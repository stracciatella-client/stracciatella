package net.stracciatella.fullscreen.config;

import net.caffeinemc.mods.sodium.api.config.ConfigEntryPoint;
import net.caffeinemc.mods.sodium.api.config.structure.ConfigBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class SodiumCompat implements ConfigEntryPoint {
    @Override
    public void registerConfigLate(ConfigBuilder builder) {
        var page = builder.createOptionPage();
        var generalGroup = builder.createOptionGroup();

        // @formatter:off
        generalGroup.setName(Component.translatable("config.borderlessmining.general"));
        generalGroup.addOption(builder.createBooleanOption(Identifier.parse("borderlessmining:enabled")).setName(Component.translatable("config.borderlessmining.general.enabled")).setTooltip(Component.translatable("config.borderlessmining.general.enabled.tooltip")).setStorageHandler(ConfigHandler.getInstance()::save).setDefaultValue(true).setBinding(ConfigHandler.getInstance()::setEnabledPending, ConfigHandler.getInstance()::isEnabled));

        builder.registerModOptions("borderlessmining", "Borderless Mining", "1.0").addPage(page.setName(Component.translatable("config.borderlessmining.title"))
                .addOptionGroup(generalGroup)
        );
        // @formatter:on
    }
    // public static final ConfigStorage configStorage = new ConfigStorage();
    //
    // public static OptionPage config() {
    //     List<OptionGroup> groups = new ArrayList<>();
    //
    //     // monitors are not listed because of the way sodium works. will implement later
    //
    //     groups.add(OptionGroup
    //             .createBuilder()
    //             .add(OptionImpl.createBuilder(boolean.class, configStorage).setName(Component.translatable("config.borderlessmining.dimensions")).setTooltip(Component.empty()).setControl(TickBoxControl::new).setBinding((opt, val) -> opt.customWindowDimensions = opt.customWindowDimensions.setEnabled(true), (opt) -> opt.customWindowDimensions.enabled).build())
    //             .add(OptionImpl.createBuilder(boolean.class, configStorage).setName(Component.translatable("config.borderlessmining.dimensions.monitorcoordinates")).setTooltip(Component.translatable("config.borderlessmining.dimensions.monitorcoordinates.tooltip")).setControl(TickBoxControl::new).setBinding((opt, val) -> opt.customWindowDimensions = opt.customWindowDimensions.setUseMonitorCoordinates(true), (opt) -> opt.customWindowDimensions.useMonitorCoordinates).build())
    //             .add(OptionImpl.createBuilder(int.class, configStorage).setName(Component.translatable("config.borderlessmining.dimensions.x")).setTooltip(Component.empty()).setControl(option -> new SliderControl(option, 0, 9999, 1, ControlValueFormatter.number())).setBinding((opt, val) -> opt.customWindowDimensions = opt.customWindowDimensions.setX(val), (opt) -> opt.customWindowDimensions.x).build())
    //             .add(OptionImpl.createBuilder(int.class, configStorage).setName(Component.translatable("config.borderlessmining.dimensions.y")).setTooltip(Component.empty()).setControl(option -> new SliderControl(option, 0, 9999, 1, ControlValueFormatter.number())).setBinding((opt, val) -> opt.customWindowDimensions = opt.customWindowDimensions.setY(val), (opt) -> opt.customWindowDimensions.y).build())
    //             .add(OptionImpl.createBuilder(int.class, configStorage).setName(Component.translatable("config.borderlessmining.dimensions.width")).setTooltip(Component.empty()).setControl(option -> new SliderControl(option, 0, 9999, 1, ControlValueFormatter.number())).setBinding((opt, val) -> opt.customWindowDimensions = opt.customWindowDimensions.setWidth(val), (opt) -> opt.customWindowDimensions.width).build())
    //             .add(OptionImpl.createBuilder(int.class, configStorage).setName(Component.translatable("config.borderlessmining.dimensions.height")).setTooltip(Component.empty()).setControl(option -> new SliderControl(option, 0, 9999, 1, ControlValueFormatter.number())).setBinding((opt, val) -> opt.customWindowDimensions = opt.customWindowDimensions.setHeight(val), (opt) -> opt.customWindowDimensions.height).build())
    //             .build());
    //
    //     return new OptionPage(Component.translatable("config.borderlessmining.sodium"), ImmutableList.copyOf(groups));
    // }
}
