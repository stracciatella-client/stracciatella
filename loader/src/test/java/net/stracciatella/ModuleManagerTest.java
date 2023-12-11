package net.stracciatella;

import static net.stracciatella.module.Module.LifeCycle.*;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;

import net.stracciatella.module.CommandLineModuleClasspath;
import net.stracciatella.module.Module;
import net.stracciatella.module.ModuleEntry;
import net.stracciatella.module.ModuleManager;
import net.stracciatella.module.SimpleModuleManager;
import net.stracciatella.module.classloader.StracciatellaClassLoader;
import org.junit.jupiter.api.Test;

public class ModuleManagerTest {
    private static void assertLifeCycle(Module.LifeCycle lifeCycle, ModuleEntry... modules) {
        for (var module : modules) {
            assertEquals(module.lifeCycle(), lifeCycle);
        }
    }

    @Test
    void test() throws Throwable {
        var moduleManager = (SimpleModuleManager) Stracciatella.instance().service(ModuleManager.class);
        var classLoader = Stracciatella.instance().service(StracciatellaClassLoader.class);
        Thread.currentThread().setContextClassLoader(classLoader);
        var classpath = CommandLineModuleClasspath.fromClasspath(System.getProperty("stracciatellaClasspath"));
        for (var pathString : classpath.paths()) {
            moduleManager.load(Path.of(pathString));
        }
        var test1module = moduleManager.module("test1module");
        assertNotNull(test1module);
        var test2module = moduleManager.module("test2module");
        assertNotNull(test2module);
        var test3module = moduleManager.module("test3module");
        assertNotNull(test3module);

        moduleManager.constructRegisteredModules();
        moduleManager.changeLifeCycle(INITIALIZED);
        assertLifeCycle(INITIALIZED, test1module, test2module, test3module);
        moduleManager.changeLifeCycle(MIXINS);
        assertLifeCycle(MIXINS, test1module, test2module, test3module);
        moduleManager.changeLifeCycle(PRE_LAUNCH);
        assertLifeCycle(PRE_LAUNCH, test1module, test2module, test3module);
        moduleManager.changeLifeCycle(STARTED);
        assertLifeCycle(STARTED, test1module, test2module, test3module);

        moduleManager.reload(test1module);
        test1module = moduleManager.module("test1module");
        assertNotNull(test1module);
        test2module = moduleManager.module("test2module");
        assertNotNull(test2module);

        moduleManager.unload(test1module);
        assertLifeCycle(STOPPED, test1module);
        assertLifeCycle(STOPPED, test2module);
        assertLifeCycle(STARTED, test3module);
        moduleManager.unload(test3module);
        assertLifeCycle(STOPPED, test3module);

        moduleManager.joinWorkers();
    }
}
