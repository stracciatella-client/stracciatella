package net.stracciatella.internal.init;

import java.nio.file.Path;

import net.fabricmc.loader.api.LanguageAdapter;
import net.fabricmc.loader.api.LanguageAdapterException;
import net.fabricmc.loader.api.ModContainer;
import net.stracciatella.Stracciatella;
import net.stracciatella.internal.init.accesswidener.AccessWidenerConfig;
import net.stracciatella.internal.init.hack.KnotClassLoaderHack;
import net.stracciatella.internal.init.hack.classloader.ClassLoaderAccessorImpl;
import net.stracciatella.injected.ClassLoaderWrapper;
import net.stracciatella.injected.StracciatellaInjections;
import net.stracciatella.internal.module.CommandLineModuleClasspath;
import net.stracciatella.module.Module;
import net.stracciatella.module.ModuleManager;
import net.stracciatella.internal.module.SimpleModuleManager;
import net.stracciatella.internal.module.StracciatellaThrowables;
import net.stracciatella.internal.module.classloader.StracciatellaClassLoader;
import net.stracciatella.util.Provider;

public class StracciatellaLanguageAdapter implements LanguageAdapter {

    static {
        try {
            KnotClassLoaderHack.hack();
        } catch (Throwable t) {
            throw StracciatellaThrowables.propagate(t);
        }
        var stracciatella = Stracciatella.instance();
        stracciatella.logger().info("Initializing Stracciatella");
        LazyInitAccessors.init(stracciatella);
        stracciatella.registerProvider("access_widener_config", AccessWidenerConfig.class, Provider.of(AccessWidenerConfig::new));
        var moduleManager = (SimpleModuleManager) stracciatella.service(ModuleManager.class);

        try {
            var commandLineModuleClasspath = CommandLineModuleClasspath.fromClasspathFile(System.getProperty("stracciatellaClasspath"));
            for (var pathString : commandLineModuleClasspath.paths()) {
                var path = Path.of(pathString);
                // only loads the module up to the global point in the loading phase - here not at all, just registers the module
                moduleManager.load(path);
            }
        } catch (Throwable e) {
            throw StracciatellaThrowables.propagate(e);
        }

        try {
            moduleManager.constructRegisteredModules();
            moduleManager.changeLifeCycle(Module.LifeCycle.INITIALIZED);
        } catch (Throwable e) {
            throw StracciatellaThrowables.propagate(e);
        }
        var accessWidenerConfig = stracciatella.service(AccessWidenerConfig.class);
        accessWidenerConfig.freeze();
    }

    @Override
    public <T> T create(ModContainer mod, String value, Class<T> type) throws LanguageAdapterException {
        return null;
    }

    private static class LazyInitAccessors {
        public static void init(Stracciatella stracciatella) {
            var classLoader = stracciatella.service(StracciatellaClassLoader.class);
            var moduleManager = stracciatella.service(ModuleManager.class);
            ClassLoaderWrapper.accessor = new ClassLoaderAccessorImpl(classLoader);
            StracciatellaInjections.Holder.injections = new StracciatellaInjectionsImpl(moduleManager);
        }
    }
}
