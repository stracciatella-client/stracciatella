package net.stracciatella.test.test1module;

import net.stracciatella.module.Module;

public class TestModule1 implements Module {
    public static TestModule1 instance;
    public int id;

    public TestModule1() {
        instance = this;
        id = 1;
    }

    public static TestModule1 instance() {
        return instance;
    }

    @Task(lifeCycle = LifeCycle.INITIALIZED)
    private void init() {
        id = 2;

    }
}
