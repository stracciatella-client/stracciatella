package net.stracciatella.fullscreen.mixin;

import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.OptionsSubScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.VideoSettingsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * This Mixin fixes a bug in Keyboard that causes video mode changes to not be applied when pressing Esc.
 * See: https://bugs.mojang.com/browse/MC-175437
 */
@Mixin(VideoSettingsScreen.class)
public class VideoModeFixMixin extends OptionsSubScreen {
    private VideoModeFixMixin(Screen parent, Options gameOptions, Component title) {
        super(parent, gameOptions, title);
    }

    @Inject(at = @At("HEAD"), method = "removed()V")
    public void screenRemoved(CallbackInfo ci) {
        if (this.minecraft != null) {
            this.minecraft.getWindow().changeFullscreenVideoMode();
        }
    }
}