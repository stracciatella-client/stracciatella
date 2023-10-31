package net.stracciatella.init;

import net.fabricmc.api.ModInitializer;
import net.stracciatella.Stracciatella;
import net.stracciatella.module.Module;
import net.stracciatella.module.ModuleManager;
import net.stracciatella.module.SimpleModuleManager;

public class StracciatellaInitializer implements ModInitializer {

    @Override
    public void onInitialize() {
        ((SimpleModuleManager) Stracciatella.instance().service(ModuleManager.class)).changeLifeCycle(Module.LifeCycle.STARTED);
    }
}