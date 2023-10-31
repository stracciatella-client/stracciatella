package net.stracciatella.init;

import java.nio.file.Path;

import net.fabricmc.loader.api.LanguageAdapter;
import net.fabricmc.loader.api.LanguageAdapterException;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.stracciatella.Stracciatella;
import net.stracciatella.module.CommandLineModuleClasspath;
import net.stracciatella.module.Module;
import net.stracciatella.module.ModuleManager;
import net.stracciatella.module.ModulesClassLoader;
import net.stracciatella.module.SimpleModuleManager;
import net.stracciatella.util.Provider;

public class StracciatellaLanguageAdapter implements LanguageAdapter {

    static {
        var stracciatella = Stracciatella.instance();
        stracciatella.logger().info("Initializing Stracciatella");
        stracciatella.registerProvider("access_widener_config", AccessWidenerConfig.class, Provider.of(AccessWidenerConfig::new));
        var moduleManager = (SimpleModuleManager) stracciatella.service(ModuleManager.class);

        try {
            var commandLineModuleClasspath = CommandLineModuleClasspath.fromClasspathFile(System.getProperty("stracciatellaClasspath"));
            for (var pathString : commandLineModuleClasspath.paths()) {
                var path = Path.of(pathString);
                moduleManager.load(path);
            }
        } catch (Throwable e) {
            throw new AssertionError(e);
        }

        Thread.currentThread().setContextClassLoader(stracciatella.service(ModulesClassLoader.class));

        try {
            moduleManager.constructRegisteredModules();
            moduleManager.changeLifeCycle(Module.LifeCycle.INITIALIZED);
        } catch (Throwable e) {
            if (e instanceof RuntimeException runtimeException) throw runtimeException;
            if (e instanceof Error error) throw error;
            throw new AssertionError(e);
        }
        var accessWidenerConfig = stracciatella.service(AccessWidenerConfig.class);
        accessWidenerConfig.freeze();
    }

    @Override
    public <T> T create(ModContainer mod, String value, Class<T> type) throws LanguageAdapterException {
        System.out.println("Language adapter create");
        return null;
    }
}
