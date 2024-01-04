package net.stracciatella.core.state;

import net.minecraft.client.Minecraft;

public class CoreModuleStarted {
    public CoreModuleStarted() {
        System.out.println(Minecraft.getInstance().resourcePackDirectory);
        System.out.println(Minecraft.getInstance().profileFuture);
        System.out.println(Minecraft.getInstance().virtualScreen);
    }
}
