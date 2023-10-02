package net.stracciatella.mixin;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(MinecraftServer.class)
public class ExampleMixin {
    public void test(){
        MinecraftServer s = null;
        s.acceptsFailure();
    }
}