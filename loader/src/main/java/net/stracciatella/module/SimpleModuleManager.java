package net.stracciatella.module;

import static net.stracciatella.module.Module.LifeCycle.*;
import static net.stracciatella.module.ModuleUtils.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.accesswidener.AccessWidenerReader;
import net.stracciatella.Stracciatella;
import net.stracciatella.init.AccessWidenerConfig;
import net.stracciatella.module.Module.LifeCycle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleModuleManager implements ModuleManager {

    static final Logger LOGGER = LoggerFactory.getLogger("ModuleManager");
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Map<String, SimpleModuleEntry> registeredModules = new HashMap<>();
    private final Map<Module, SimpleModuleEntry> modules = new WeakHashMap<>();
    private final ModuleLifeCycleUtils lifeCycleUtils = new ModuleLifeCycleUtils();
    private final Collection<Thread> workers = ConcurrentHashMap.newKeySet();
    private LifeCycle globalLifeCycle = REGISTERED;

    @Override
    public synchronized @NotNull Collection<SimpleModuleEntry> modules() {
        return Collections.unmodifiableCollection(registeredModules.values());
    }

    @Override
    public synchronized @Nullable SimpleModuleEntry module(@NotNull String moduleId) {
        return registeredModules.get(moduleId);
    }

    @Override
    public synchronized @NotNull SimpleModuleEntry module(@NotNull Module module) {
        var entry = modules.get(module);
        if (entry == null) throw new IllegalStateException("Module not registered in ModuleManager: " + module.getClass().getName() + ". Probably not correctly shut down.");
        return entry;
    }

    @Override
    public synchronized @NotNull SimpleModuleEntry load(@NotNull Path file) throws Throwable {
        var entry = register(file);
        updateLifeCycleToGlobal(entry);
        return entry;
    }

    private void updateLifeCycleToGlobal(SimpleModuleEntry entry) {
        while (entry.lifeCycle() != globalLifeCycle) {
            var newLifeCycle = switch (entry.lifeCycle()) {
                case REGISTERED -> INITIALIZED;
                case INITIALIZED -> MIXINS;
                case MIXINS -> PRE_LAUNCH;
                case PRE_LAUNCH -> STARTED;
                default -> throw new IllegalStateException("Bad state");
            };
            changeLifeCycle(entry, newLifeCycle);
        }
    }

    private SimpleModuleEntry register(Path file) throws IOException {
        if (globalLifeCycle == STOPPING || globalLifeCycle == STOPPED) throw new IllegalStateException("Can't load module after ModuleManager has shut down");
        var fileSystem = FileSystems.newFileSystem(file);
        var moduleJsonPath = fileSystem.getPath("stracciatella.module.json");
        if (!Files.exists(moduleJsonPath)) {
            throw new FileNotFoundException("stracciatella.module.json not found for file " + file.toAbsolutePath().normalize());
        }

        var moduleConfiguration = loadConfiguration(moduleJsonPath);

        var entry = new SimpleModuleEntry(moduleConfiguration, fileSystem, file);
        entry.lifeCycle(REGISTERED);
        registeredModules.put(moduleConfiguration.id(), entry);
        return entry;
    }

    @Override
    public synchronized void reload(@NotNull ModuleEntry module) throws Throwable {
        var unloadedEntries = new ArrayList<SimpleModuleEntry>();
        // first unload all required modules
        ModuleUtils.walkDependants((SimpleModuleEntry) module, e -> {
            unloadedEntries.add(e);
            unload(e);
        });
        var files = new ArrayList<Path>();
        // collect all the files where the modules came from
        for (var entry : unloadedEntries) files.add(entry.file());
        var entries = new ArrayList<SimpleModuleEntry>();
        // load all the modules
        for (var file : files) entries.add(register(file));
        constructModules(entries);
        var lifeCycle = REGISTERED;
        while (lifeCycle != globalLifeCycle) {
            lifeCycle = switch (lifeCycle) {
                case REGISTERED -> INITIALIZED;
                case INITIALIZED -> MIXINS;
                case MIXINS -> PRE_LAUNCH;
                case PRE_LAUNCH -> STARTED;
                default -> throw new IllegalStateException("Bad state");
            };
            var finalLifeCycle = lifeCycle;
            walkDependencies(entries, e -> changeLifeCycle(e, finalLifeCycle));
        }
    }

    @Override
    public synchronized void unload(@NotNull ModuleEntry module) throws Throwable {
        var entry = (SimpleModuleEntry) module;
        ModuleUtils.walkDependants(entry, this::unload);
    }

    private void unload(SimpleModuleEntry entry) throws IOException {
        changeLifeCycle(entry, STOPPING);
        changeLifeCycle(entry, STOPPED);
        for (var dependency : entry.dependencies()) {
            dependency.dependants().remove(entry);
            entry.classLoader().dependencyLoaders().remove(dependency.classLoader());
        }
        var classLoader = entry.classLoader();
        Stracciatella.instance().service(ModulesClassLoader.class).moduleLoaders().remove(classLoader);
        entry.dependencies().clear();
        if (!entry.dependants().isEmpty()) {
            LOGGER.error("There are still dependant modules when unloading module " + entry.moduleConfiguration().name() + ": " + entry.dependants().size());
            for (var dependant : entry.dependants()) LOGGER.error(" - " + dependant.moduleConfiguration().name());
        }
        if (!registeredModules.remove(entry.moduleConfiguration().id(), entry)) LOGGER.error("Failed to remove module " + entry.moduleConfiguration().name() + " from registered modules");
        entry.module(null);
        entry.classLoader(null);
        for (var fileSystem : classLoader.fileSystems()) fileSystem.close();
        MemoryLeakDetector.start(this, entry.moduleConfiguration().name(), classLoader.loadedClasses());
    }

    public synchronized void constructRegisteredModules() throws Throwable {
        constructModules(registeredModules.values());
    }

    private void constructModules(Collection<SimpleModuleEntry> entries) throws Throwable {
        for (var entry : entries) {
            addDependencies(this, entry);
        }
        walkDependencies(entries, this::setupClassLoader);
        walkDependencies(entries, this::constructModule);
    }

    public synchronized void changeLifeCycle(@NotNull LifeCycle lifeCycle) {
        if (!globalLifeCycle.canChangeTo(lifeCycle)) throw new IllegalStateException("Can't change global LifeCycle from " + globalLifeCycle + " to " + lifeCycle);
        for (var entry : registeredModules.values()) {
            if (!entry.lifeCycle().canChangeTo(lifeCycle)) continue;
            changeLifeCycle(entry, lifeCycle);
        }
        globalLifeCycle = lifeCycle;
    }

    public synchronized void changeLifeCycle(@NotNull ModuleEntry entry, @NotNull LifeCycle lifeCycle) {
        lifeCycleUtils.changeLifeCycle((SimpleModuleEntry) entry, lifeCycle);
    }

    private void setupClassLoader(SimpleModuleEntry entry) throws Throwable {
        if (entry.lifeCycle() != REGISTERED) throw new IllegalStateException();
        var parentLoader = Stracciatella.instance().service(ModulesClassLoader.class);
        var classLoader = new SimpleModuleClassLoader(parentLoader);
        classLoader.fileSystems().add(entry.fileSystem());
        var libraryStorage = Stracciatella.instance().service(LibraryStorage.class);

        for (var dependency : entry.dependencies()) {
            classLoader.dependencyLoaders().add(dependency.classLoader());
        }
        for (var dependency : entry.moduleConfiguration().dependencies()) {
            if (!(dependency instanceof MavenModuleDependency maven)) continue;
            var path = libraryStorage.path(maven);
            classLoader.fileSystems().add(FileSystems.newFileSystem(path));
        }

        entry.classLoader(classLoader);
    }

    private void constructModule(SimpleModuleEntry entry) throws Throwable {
        if (entry.lifeCycle() != REGISTERED) throw new IllegalStateException();
        LOGGER.info("Loading module " + entry.moduleConfiguration().name());

        var parentLoader = Stracciatella.instance().service(ModulesClassLoader.class);
        parentLoader.moduleLoaders().add(entry.classLoader());

        { // load access widener
            var reader = new AccessWidenerReader(Stracciatella.instance().service(AccessWidenerConfig.class));
            for (var accessWidenerPath : entry.moduleConfiguration().accessWideners()) {
                var in = entry.classLoader().getResourceAsStream(accessWidenerPath);
                if (in == null) throw new IllegalStateException("Access widener " + accessWidenerPath + " in module " + entry.moduleConfiguration().name() + " not found.");
                reader.read(in.readAllBytes());
                in.close();
            }
        }

        { // create instance
            var raw = Class.forName(entry.moduleConfiguration().main(), true, entry.classLoader());
            if (!Module.class.isAssignableFrom(raw)) throw new ModuleException("Main class must implement Module");
            var cls = raw.asSubclass(Module.class);
            try {
                var constructor = cls.getConstructor();
                var instance = constructor.newInstance();
                entry.module(instance);
                modules.put(instance, entry);
            } catch (NoSuchMethodException exception) {
                throw new ModuleException("Main class " + cls.getName() + " must have a public empty constructor");
            }
        }
    }

    public Collection<Thread> workers() {
        return workers;
    }

    public void joinWorkers() throws InterruptedException {
        while (!workers.isEmpty()) {
            for (var worker : workers) {
                worker.join();
            }
        }
    }

    private ModuleConfiguration loadConfiguration(Path path) throws IOException {
        var json = gson.fromJson(Files.readString(path), JsonObject.class);
        return ModuleConfiguration.parse(json);
    }
}
