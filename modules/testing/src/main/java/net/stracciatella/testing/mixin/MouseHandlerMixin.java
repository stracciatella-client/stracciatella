package net.stracciatella.testing.mixin;

import net.minecraft.client.MouseHandler;
import net.stracciatella.testing.runner.TestRunner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
    @Inject(at = @At("HEAD"), method = "turnPlayer", cancellable = true)
    private void onTurnPlayer(CallbackInfo ci) {
        if (TestRunner.instance().isRunning()) {
            ci.cancel();
        }
    }

    @Inject(at = @At("HEAD"), method = "grabMouse", cancellable = true)
    private void onGrabMouse(CallbackInfo ci) {
        if (TestRunner.instance().isRunning()) {
            ci.cancel();
        }
    }
}
