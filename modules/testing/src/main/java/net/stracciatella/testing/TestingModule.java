package net.stracciatella.testing;

import net.stracciatella.module.Module;

public class TestingModule implements Module {
    @Task(lifeCycle = LifeCycle.STARTED)
    private void started() {
        new TestingModuleStarted();
        logger().info("Testing framework loaded. Use /stracciatella-test in-game to run tests.");
    }
}
