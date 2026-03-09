package net.stracciatella.testing.mixin;

import net.minecraft.client.gui.screens.TitleScreen;
import net.stracciatella.testing.world.AutoTestWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class TitleScreenMixin {

    @Inject(at = @At("TAIL"), method = "init")
    private void onInit(CallbackInfo ci) {
        if (!"true".equals(System.getProperty("stracciatella.testing.autorun"))) return;
        AutoTestWorld.joinOrCreate();
    }
}
