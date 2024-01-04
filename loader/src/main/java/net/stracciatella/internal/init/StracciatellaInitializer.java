package net.stracciatella.internal.init;

import java.util.Arrays;

import net.fabricmc.api.ModInitializer;
import net.minecraft.client.Minecraft;
import net.stracciatella.Stracciatella;
import net.stracciatella.module.Module;
import net.stracciatella.module.ModuleManager;
import net.stracciatella.internal.module.SimpleModuleManager;

public class StracciatellaInitializer implements ModInitializer {

    @Override
    public void onInitialize() {
        ((SimpleModuleManager) Stracciatella.instance().service(ModuleManager.class)).changeLifeCycle(Module.LifeCycle.STARTED);

        System.out.println(Arrays.stream(Minecraft.class.getInterfaces()).map(Class::getName).toList());
    }
}