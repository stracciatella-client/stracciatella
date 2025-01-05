package net.stracciatella.anonymousmodlist.mixins;

import java.util.List;

import net.caffeinemc.mods.sodium.client.gui.options.OptionPage;
import net.stracciatella.anonymousmodlist.config.SodiumCompat;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = net.caffeinemc.mods.sodium.client.gui.SodiumOptionsGUI.class, remap = false)
public class SodiumOptionMixin {
   
    @Shadow
    @Final
    private List<OptionPage> pages;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void init(CallbackInfo ci) {
        for (int i = 0; i < 100; i++) {
            System.out.println("asfgbazsfhgasfgzasdfa");
        }
        pages.add(SodiumCompat.config());
    }
}
