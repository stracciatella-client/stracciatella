package net.stracciatella.testing.mixin;

import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.network.chat.Component;
import net.stracciatella.testing.command.ChatInterceptor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatListener.class)
public class ChatListenerMixin {
    @Inject(at = @At("HEAD"), method = "handleSystemMessage")
    private void onSystemMessage(Component message, boolean overlay, CallbackInfo ci) {
        ChatInterceptor.instance().onMessage(message.getString());
    }
}
