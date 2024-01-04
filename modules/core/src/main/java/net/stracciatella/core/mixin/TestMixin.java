package net.stracciatella.core.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class TestMixin {
    @Inject(at = @At("HEAD"), method = "<clinit>")
    private static void run(CallbackInfo ci) {
        System.err.println("Initialize Minecraft");
    }
}
