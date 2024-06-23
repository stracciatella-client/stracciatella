package net.stracciatella.fullscreen.mixin;

import java.util.function.Consumer;

import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import net.minecraft.network.chat.Component;
import net.stracciatella.fullscreen.config.ConfigHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(VideoSettingsScreen.class)
public abstract class FullScreenOptionMixin {
    // Modify the constructor call to add an extra option for Borderless Fullscreen
    @ModifyArgs(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/OptionInstance;<init>(Ljava/lang/String;Lnet/minecraft/client/OptionInstance$TooltipSupplier;Lnet/minecraft/client/OptionInstance$CaptionBasedToString;Lnet/minecraft/client/OptionInstance$ValueSet;Ljava/lang/Object;Ljava/util/function/Consumer;)V"))
    private void modifyOption(Args args) {
        if (!ConfigHandler.getInstance().addToVanillaVideoSettings) {
            return;
        }

        // Add one extra option at the end for Borderless Windowed
        OptionInstance.IntRange cb = args.get(3);
        int bmOption = cb.maxInclusive() + 1;
        args.set(3, new OptionInstance.IntRange(cb.minInclusive(), bmOption));

        // Modify the text getter to show Borderless Mining text
        OptionInstance.CaptionBasedToString<Integer> oldTextGetter = args.get(2);
        args.set(2, (OptionInstance.CaptionBasedToString<Integer>) (optionText, value) -> {
            if (value == bmOption) {
                return Component.translatable("text.borderlessmining.videomodename");
            }
            return oldTextGetter.toString(optionText, value);
        });

        // Change the default based on the existing option selection
        args.set(4, ConfigHandler.getInstance().isEnabledOrPending() ? bmOption : args.get(4));

        // Update BM settings when the slider is changed
        Consumer<Integer> oldConsumer = args.get(5);
        args.set(5, (Consumer<Integer>) value -> {
            if (value == bmOption) {
                ConfigHandler.getInstance().setEnabledPending(true);
                // Set the actual value to "Current"
                oldConsumer.accept(-1);
            } else {
                ConfigHandler.getInstance().setEnabledPending(false);
                oldConsumer.accept(value);
            }
        });
    }
}