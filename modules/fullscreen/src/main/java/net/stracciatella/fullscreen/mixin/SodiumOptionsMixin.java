package net.stracciatella.fullscreen.mixin;

import java.util.List;

import net.stracciatella.fullscreen.config.SodiumCompat;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = net.caffeinemc.mods.sodium.client.gui.SodiumOptionsGUI.class, remap = false)
public class SodiumOptionsMixin {
    @Shadow
    @Final
    private List<net.caffeinemc.mods.sodium.client.gui.options.OptionPage> pages;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void init(CallbackInfo ci) {

        pages.add(SodiumCompat.config());
    }
}
