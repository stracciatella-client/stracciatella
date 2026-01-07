package net.stracciatella.anonymousmodlist.config;

import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.stracciatella.Stracciatella;

public class SodiumCompat implements ConfigEntryPoint {
    @Override
    public void registerConfigLate(ConfigBuilder builder) {
        // @formatter:off
        builder.registerModOptions("anonymous_modlist", "Anonymous Modlist", "1.0").addPage(builder.createOptionPage()
                .setName(Component.translatable("config.anonymous-modlist.sodium"))
                .addOptionGroup(builder.createOptionGroup()
                        .setName(Component.translatable("config.anonymous-modlist.sodium"))
                        .addOption(builder.createBooleanOption(Identifier.parse(Stracciatella.STRACCIATELLA + ":anonymous_modlist"))
                                .setName(Component.translatable("config.anonymous-modlist.sodium"))
                                .setTooltip(Component.translatable("config.anonymous-modlist.tooltip"))
                                .setBinding(ConfigHandler::set, ConfigHandler::get)
                                .setStorageHandler(ConfigHandler.getInstance()::save)
                                .setDefaultValue(false)
                        )
                )
        );
        // @formatter:on
    }
}
