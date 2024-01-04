package net.stracciatella.internal.init;

import net.stracciatella.injected.StracciatellaInjections;
import net.stracciatella.module.ModuleManager;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.transformer.Config;

public record StracciatellaInjectionsImpl(ModuleManager moduleManager) implements StracciatellaInjections {
    @Override
    public void initializeMixins() {
        for (var entry : moduleManager.modules()) {
            var config = entry.moduleConfiguration();
            for (var mixinFileName : config.mixins()) {
                var mixinConfig = Config.create(mixinFileName);
                Mixins.getConfigs().add(mixinConfig);
            }
        }
    }
}
