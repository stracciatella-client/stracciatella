package net.stracciatella;

import java.nio.file.Path;

import net.stracciatella.internal.util.SimpleServiceProvider;
import net.stracciatella.module.LibraryStorage;
import net.stracciatella.module.ModuleManager;
import net.stracciatella.module.SimpleModuleManager;
import net.stracciatella.module.classloader.StracciatellaClassLoader;
import net.stracciatella.util.Provider;
import net.stracciatella.util.ServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Stracciatella implements ServiceProvider.Wrapper {
    public static final String STRACCIATELLA = "stracciatella";
    public static final String WORKING_DIRECTORY = "working_directory";
    public static final String CLASS_LOADER = "class_loader";
    public static final String MODULE_MANAGER = "module_manager";
    public static final String LIBRARY_STORAGE = "library_storage";

    private static final Logger LOGGER = LoggerFactory.getLogger("Stracciatella");
    private static final Stracciatella instance = new Stracciatella();
    private final ServiceProvider serviceProvider = new SimpleServiceProvider();

    private Stracciatella() {
        register(STRACCIATELLA, Stracciatella.class, this);
        registerProvider(WORKING_DIRECTORY, Path.class, Provider.of(this::findWorkingDirectory));
        registerProvider(CLASS_LOADER, StracciatellaClassLoader.class, Provider.of(StracciatellaClassLoader::new));
        registerProvider(MODULE_MANAGER, ModuleManager.class, Provider.of(SimpleModuleManager::new));
        registerProvider(LIBRARY_STORAGE, LibraryStorage.class, Provider.of(LibraryStorage::new));
    }

    public static Stracciatella instance() {
        return instance;
    }

    @Override
    public ServiceProvider serviceProvider() {
        return serviceProvider;
    }

    public Logger logger() {
        return LOGGER;
    }

    private Path findWorkingDirectory() {
        return Path.of("stracciatella");
    }
}
