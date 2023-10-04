package net.stracciatella;

import java.io.IOException;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.launch.MixinBootstrap;

public class Stracciatella implements ModInitializer {
    // This logger is used to write text to the console and the log file.
    // It is considered best practice to use your mod id as the logger's name.
    // That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger("stracciatella");

    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        try {
            var bytes = FabricLauncherBase.getLauncher().getClassByteArray("net.stracciatella.init.ClassLoadingTest", true);
            System.out.println(bytes.length); // 319
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        LOGGER.warn("Hello Fabric world!");
    }
}