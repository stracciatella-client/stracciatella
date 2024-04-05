package net.stracciatella.fullscreen.util;

import org.spongepowered.asm.mixin.Unique;

public interface WindowHooks {
    @Unique
    void stracciatella$apply();
}
