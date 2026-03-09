package net.stracciatella.testing.mixin;

import net.minecraft.client.Minecraft;
import net.stracciatella.testing.runner.TestRunner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class ClientTickMixin {
    @Inject(at = @At("TAIL"), method = "tick")
    private void onClientTick(CallbackInfo ci) {
        TestRunner.instance().onClientTick();
    }
}
