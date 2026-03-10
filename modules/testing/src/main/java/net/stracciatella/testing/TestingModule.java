package net.stracciatella.testing;

import net.stracciatella.module.Module;
import net.stracciatella.testing.example.CommandTests;
import net.stracciatella.testing.runner.TestRunner;

public class TestingModule implements Module {
    @Task(lifeCycle = LifeCycle.STARTED)
    private void started() {
        TestRunner.instance().registerSuite(CommandTests.class);
        new TestingModuleStarted();
        logger().info("Testing framework loaded. Use /stracciatella-test in-game to run tests.");
    }
}
