package net.stracciatella.init;

import java.util.LinkedHashSet;

import net.stracciatella.injected.StracciatellaInjections;
import net.stracciatella.module.ModuleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigSource;
import org.spongepowered.asm.mixin.transformer.Config;

public record StracciatellaInjectionsImpl(ModuleManager moduleManager) implements StracciatellaInjections {
    private static final Logger LOGGER = LoggerFactory.getLogger(StracciatellaInjectionsImpl.class);

    @Override
    public void initializeMixins() {
        LOGGER.info("Initializing mixins");
        var originalConfigs = new LinkedHashSet<>(Mixins.getConfigs());
        // Mixins.getConfigs().clear();
        for (var entry : moduleManager.modules()) {
            var config = entry.moduleConfiguration();
            for (var mixinFileName : config.mixins()) {
                var mixinConfig = Config.create(mixinFileName, (IMixinConfigSource) null);
                Mixins.getConfigs().add(mixinConfig);
            }
        }
        // Mixins.getConfigs().addAll(originalConfigs);
        // Mixins.getConfigs().forEach(config -> {
        //     System.out.println(config);
        // });
    }
}
