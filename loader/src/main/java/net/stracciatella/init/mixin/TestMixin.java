package net.stracciatella.init.mixin;

import java.nio.file.Path;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.chat.ChatListener;
import net.stracciatella.Stracciatella;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class TestMixin {
    @Final
    @Shadow
    private Path resourcePackDirectory;

    @Shadow
    public abstract ChatListener getChatListener();

    @Inject(method = "run", at = @At("HEAD"))
    public void test(CallbackInfo ci) {
        System.out.println("TEST MIXIN APPLIED-----------");
        System.out.println("TEST MIXIN APPLIED-----------");
        System.out.println("TEST MIXIN APPLIED-----------");
        System.out.println("TEST MIXIN APPLIED-----------");

        try {
            System.out.println(Class.forName("net.stracciatella.FindMe"));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        // try {
        //     RegisteredMixins.test.invokeExact();
        //     // var cls = Class.forName("net.stracciatella.FindMe", true, Stracciatella.instance().service(ModulesClassLoader.class));
        // } catch (Throwable e) {
        //     e.printStackTrace();
        // }
    }
}
