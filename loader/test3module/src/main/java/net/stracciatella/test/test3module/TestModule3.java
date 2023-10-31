package net.stracciatella.test.test3module;

import net.stracciatella.module.Module;

public class TestModule3 implements Module {
    private static TestModule3 instance;

    public TestModule3() {
        instance = this;
    }
}
