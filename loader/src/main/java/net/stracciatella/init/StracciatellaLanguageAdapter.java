package net.stracciatella.init;

import java.nio.file.Files;
import java.nio.file.Path;

import net.fabricmc.loader.api.LanguageAdapter;
import net.fabricmc.loader.api.ModContainer;
import net.stracciatella.Stracciatella;
import net.stracciatella.init.accesswidener.AccessWidenerConfig;
import net.stracciatella.init.hack.KnotClassLoaderHack;
import net.stracciatella.init.hack.classloader.ClassLoaderAccessorImpl;
import net.stracciatella.injected.ClassLoaderWrapper;
import net.stracciatella.injected.StracciatellaInjections;
import net.stracciatella.module.CommandLineModuleClasspath;
import net.stracciatella.module.Module;
import net.stracciatella.module.ModuleManager;
import net.stracciatella.module.SimpleModuleManager;
import net.stracciatella.module.StracciatellaThrowables;
import net.stracciatella.module.classloader.StracciatellaClassLoader;
import net.stracciatella.util.Provider;

@SuppressWarnings("unused")
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
                moduleManager.load(path);
            }
            var directory = stracciatella.service(Path.class, Stracciatella.WORKING_DIRECTORY);
            var modulesDirectory = directory.resolve("modules");
            Files.createDirectories(modulesDirectory);
            try (var stream = Files.newDirectoryStream(modulesDirectory)) {
                for (var file : stream) {
                    if (Files.isDirectory(file)) continue;
                    if (!file.getFileName().toString().endsWith(".jar")) {
                        stracciatella.logger().info("Skipping module file {}", file.getFileName().toString());
                        continue;
                    }
                    moduleManager.load(file);
                }
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
    public <T> T create(ModContainer mod, String value, Class<T> type) {
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
