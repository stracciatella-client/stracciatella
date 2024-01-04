package net.stracciatella;

import static net.stracciatella.util.Provider.*;

import java.nio.file.Path;

import net.stracciatella.internal.util.SimpleServiceProvider;
import net.stracciatella.module.LibraryStorage;
import net.stracciatella.module.ModuleManager;
import net.stracciatella.internal.module.SimpleModuleManager;
import net.stracciatella.internal.module.classloader.StracciatellaClassLoader;
import net.stracciatella.util.ServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Stracciatella implements ServiceProvider.Wrapper {
    public static final String STRACCIATELLA = "stracciatella";
    public static final String WORKING_DIRECTORY = "working_directory";
    public static final String INTERNAL_DIRECTORY = "internal_directory";
    public static final String TEMP_DIRECTORY = "temp_directory";
    public static final String CLASS_LOADER = "class_loader";
    public static final String MODULE_MANAGER = "module_manager";
    public static final String LIBRARY_STORAGE = "library_storage";

    private static final Logger LOGGER = LoggerFactory.getLogger("Stracciatella");
    private static final Stracciatella instance = new Stracciatella();
    private final ServiceProvider serviceProvider = new SimpleServiceProvider();

    private Stracciatella() {
        register(STRACCIATELLA, Stracciatella.class, this);
        registerProvider(WORKING_DIRECTORY, Path.class, of(this::findWorkingDirectory));
        registerProvider(INTERNAL_DIRECTORY, Path.class, of(() -> service(Path.class, WORKING_DIRECTORY).resolve("internal")));
        registerProvider(TEMP_DIRECTORY, Path.class, of(() -> service(Path.class, INTERNAL_DIRECTORY).resolve("temp")));
        registerProvider(CLASS_LOADER, StracciatellaClassLoader.class, of(StracciatellaClassLoader::new));
        registerProvider(MODULE_MANAGER, ModuleManager.class, of(SimpleModuleManager::new));
        registerProvider(LIBRARY_STORAGE, LibraryStorage.class, of(LibraryStorage::new));
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
