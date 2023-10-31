package net.stracciatella.core;

import net.stracciatella.core.state.CoreModuleInit;
import net.stracciatella.core.state.CoreModuleMixins;
import net.stracciatella.core.state.CoreModulePreLaunch;
import net.stracciatella.core.state.CoreModuleStarted;
import net.stracciatella.module.Module;

public class CoreModule implements Module {
    @Task(lifeCycle = LifeCycle.INITIALIZED)
    private void init() {
        new CoreModuleInit();
    }

    @Task(lifeCycle = LifeCycle.MIXINS)
    private void loaded() {
        // var resourcePackDirectory = Minecraft.getInstance().resourcePackDirectory;
        // var profileFuture = Minecraft.getInstance().profileFuture;
        // var virtualScreen = Minecraft.getInstance().virtualScreen;

        new CoreModuleMixins();
    }

    @Task(lifeCycle = LifeCycle.PRE_LAUNCH)
    private void preLaunch() {
        new CoreModulePreLaunch();
    }

    @Task(lifeCycle = LifeCycle.STARTED)
    private void started() {
        new CoreModuleStarted();
    }
}
