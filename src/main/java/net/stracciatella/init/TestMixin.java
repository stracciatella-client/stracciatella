package net.stracciatella.init;

import org.spongepowered.asm.mixin.Mixin;

@Mixin(ClassLoadingTest.class)
public class TestMixin implements ToInject {

}
