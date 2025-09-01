package net.stracciatella.anonymousmodlist.mixins;

import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.stracciatella.anonymousmodlist.config.ConfigHandler;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public class PackageInterceptorMixin {

    // @Inject(
    //         method = "Lnet/minecraft/network/Connection;send(Lnet/minecraft/network/protocol/Packet;)V",
    //         at = @At("HEAD"),
    //         cancellable = true
    // )
    // private void onSendPacket(Packet<?> packet, CallbackInfo ci) {
    //     if (packet instanceof ServerboundCustomPayloadPacket customPayloadPacket) {
    //         // Intercept or modify the packet here.
    //         // If you want to block it entirely, call ci.cancel().
    //
    //         System.out.println("[Mixin] Intercepted a CustomPayloadC2SPacket!");
    //         System.out.println(customPayloadPacket);
    //         System.out.println(customPayloadPacket.payload());
    //         System.out.println(customPayloadPacket.payload().type());
    //         System.out.println(customPayloadPacket.type().getClass());
    //
    //
    //
    //         ci.cancel();
    //         // Example: Cancel sending if it's some channel you want to filter out
    //         // CustomPayloadC2SPacket cpPacket = (CustomPayloadC2SPacket) packet;
    //         // ResourceLocation channel = cpPacket.getChannel();
    //         // if ("your_channel".equals(channel.toString())) {
    //         //     // Cancel sending the packet
    //         //     ci.cancel();
    //         // }
    //
    //         // For now, we’ll just log it without cancelling:
    //         // ci.cancel(); // <- uncomment this if you actually want to block it
    //     }
    // }

    @Inject(method = "doSendPacket", at = @At("HEAD"), cancellable = true)
    public void method2(Packet<?> packet, @Nullable ChannelFutureListener channelFutureListener, boolean bl, CallbackInfo ci) {
        if (!ConfigHandler.getInstance().isAnonymousModlistEnabled()) {
            return;
        }
        if (packet instanceof ServerboundCustomPayloadPacket(var payload)) {
            // System.out.println("method 2  caught packet:");
            // System.out.println(customPayloadPacket);
            // System.out.println(customPayloadPacket.payload());
            // System.out.println(customPayloadPacket.payload().type());
            // System.out.println(customPayloadPacket.payload().type().getClass());
            // System.out.println("type id string: " + customPayloadPacket.type().id().toString());
            if (payload.type().id().toString().equals("minecraft:register")) {
                //todo check for more characteristics and potentially just alter the data sent instead of just cancelling
                System.out.println("confirmed correct packet");
                ci.cancel();
            }

        }
    }

    // @Inject(method = "Lnet/minecraft/network/Connection;send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;Z)V", at = @At("HEAD"), cancellable = true)
    // public void method3(Packet<?> packet, @Nullable PacketSendListener packetSendListener, boolean bl, CallbackInfo ci){
    //     if (packet instanceof ServerboundCustomPayloadPacket customPayloadPacket) {
    //         System.out.println("method 3 caught packet:");
    //         System.out.println(customPayloadPacket);
    //         System.out.println(customPayloadPacket.payload());
    //         System.out.println(customPayloadPacket.payload().type());
    //         System.out.println(customPayloadPacket.type().getClass());
    //         ci.cancel();
    //     }
    // }
}
