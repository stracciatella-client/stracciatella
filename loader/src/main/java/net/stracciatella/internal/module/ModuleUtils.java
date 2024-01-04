package net.stracciatella.internal.module;

import static net.stracciatella.internal.module.SimpleModuleManager.*;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;

import net.stracciatella.Stracciatella;
import net.stracciatella.module.LibraryStorage;
import net.stracciatella.module.dependency.MavenModuleDependency;
import net.stracciatella.module.dependency.ModuleDependencyNotFoundException;
import net.stracciatella.module.dependency.ModuleModuleDependency;

class ModuleUtils {

    static void addDependencies(SimpleModuleManager moduleManager, SimpleModuleEntry entry) {
        var dependencies = entry.moduleConfiguration().dependencies();
        var mavenDependencies = new ArrayList<MavenModuleDependency>();
        var moduleDependencies = new ArrayList<ModuleModuleDependency>();
        for (var dependency : dependencies) {
            if (dependency instanceof MavenModuleDependency maven) {
                mavenDependencies.add(maven);
            } else if (dependency instanceof ModuleModuleDependency module) {
                moduleDependencies.add(module);
            } else {
                throw new UnsupportedOperationException("Stracciatella does not support dependencies of type " + dependency.getClass().getName());
            }
        }
        var libraryStorage = Stracciatella.instance().service(LibraryStorage.class);
        var downloadTasks = new ArrayList<Runnable>();
        for (var maven : mavenDependencies) {
            if (!libraryStorage.contains(maven)) {
                downloadTasks.add(() -> {
                    try {
                        var url = maven.resolve();
                        LOGGER.info("Download " + url);
                        var connection = url.openConnection();
                        connection.connect();
                        var in = connection.getInputStream();
                        libraryStorage.store(maven, in);
                    } catch (Throwable e) {
                        LOGGER.error("Failed to download maven dependency " + maven, e);
                    }
                });
            }
        }
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var futures = downloadTasks.stream().map(executor::submit).toList();
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (Throwable e) {
                    LOGGER.error("Failed to wait for a download task", e);
                }
            }
        }
        for (var maven : mavenDependencies) {
            var path = libraryStorage.path(maven);
            if (!Files.exists(path)) throw new ModuleDependencyNotFoundException("Failed to find dependency " + maven);
        }
        for (var module : moduleDependencies) {
            addModuleDependency(moduleManager, entry, module);
        }
    }

    private static void addModuleDependency(SimpleModuleManager moduleManager, SimpleModuleEntry entry, ModuleModuleDependency dependency) {
        var dependencyEntry = moduleManager.module(dependency.name());
        if (dependencyEntry == null) {
            throw new ModuleDependencyNotFoundException("Module dependency " + dependency + " not found!");
        }
        entry.dependencies().add(dependencyEntry);
        dependencyEntry.dependants().add(entry);
    }

    static void walk(SimpleModuleEntry entry, Function<SimpleModuleEntry, Iterable<SimpleModuleEntry>> converter, ModuleWalker action) throws Throwable {
        for (var e : converter.apply(entry)) {
            walk(e, converter, action);
        }
        action.walk(entry);
    }

    static void walkDependants(SimpleModuleEntry entry, ModuleWalker action) throws Throwable {
        walk(entry, SimpleModuleEntry::dependants, action);
    }

    static void walkDependencies(SimpleModuleEntry entry, ModuleWalker action) throws Throwable {
        walk(entry, SimpleModuleEntry::dependencies, action);
    }

    static void walkDependencies(Collection<SimpleModuleEntry> collection, ModuleWalker action) throws Throwable {
        var copy = new HashSet<>(collection);
        while (!copy.isEmpty()) {
            walkDependencies(copy.iterator().next(), module -> {
                if (!copy.remove(module)) return;
                action.walk(module);
            });
        }
    }

    interface ModuleWalker {
        void walk(SimpleModuleEntry entry) throws Throwable;
    }
}
