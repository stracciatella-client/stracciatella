package net.stracciatella.test.test2module;

import de.dasbabypixel.annotations.Api;
import de.dasbabypixel.util.Graph;
import net.stracciatella.module.Module;

public class TestModule2 implements Module {
    public TestModule2() {
        // if (TestModule1.instance().id != 1) throw new IllegalStateException("ID not 1");
    }

    @Api
    @Task(lifeCycle = LifeCycle.INITIALIZED)
    void init() {
        // if (TestModule1.instance().id != 2) throw new IllegalStateException("ID not 1");
        var graph = Graph.<String, Integer>linkedGraph();
        var test = graph.newNode("test");
        var test2 = graph.newNode("test2");
        graph.newConnection(test, test2, 100);
        graph.newConnection(test2, test, 50);
    }
}
