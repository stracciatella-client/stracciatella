package net.stracciatella;

import net.stracciatella.init.AccessWidenerConfig;
import net.stracciatella.module.LibraryStorage;
import net.stracciatella.module.ModuleManager;
import net.stracciatella.module.ModulesClassLoader;
import net.stracciatella.module.SimpleModuleManager;
import net.stracciatella.util.Provider;
import net.stracciatella.util.ServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Stracciatella implements ServiceProvider.Wrapper {
    private static final Logger LOGGER = LoggerFactory.getLogger("Stracciatella");
    private static final Stracciatella instance = new Stracciatella();
    private final ServiceProvider serviceProvider = new SimpleServiceProvider();

    private Stracciatella() {
        register("stracciatella", Stracciatella.class, this);
        registerProvider("modules_class_loader", ModulesClassLoader.class, Provider.of(ModulesClassLoader::new));
        registerProvider("module_manager", ModuleManager.class, Provider.of(SimpleModuleManager::new));
        registerProvider("library_storage", LibraryStorage.class, Provider.of(LibraryStorage::new));
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
}
