package net.stracciatella.anonymousmodlist;

import net.stracciatella.module.Module;

public class AnonymousModlistModule implements Module {
    @Task(lifeCycle = LifeCycle.MIXINS)
    public void registerMixins() {
    }
}
