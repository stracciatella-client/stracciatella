package net.stracciatella.fullscreen.config;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;
import me.jellysquid.mods.sodium.client.gui.options.OptionGroup;
import me.jellysquid.mods.sodium.client.gui.options.OptionImpl;
import me.jellysquid.mods.sodium.client.gui.options.OptionPage;
import me.jellysquid.mods.sodium.client.gui.options.control.ControlValueFormatter;
import me.jellysquid.mods.sodium.client.gui.options.control.SliderControl;
import me.jellysquid.mods.sodium.client.gui.options.control.TickBoxControl;
import net.minecraft.network.chat.Component;

public class SodiumCompat {
    public static final ConfigStorage configStorage = new ConfigStorage();

    public static OptionPage config() {
        List<OptionGroup> groups = new ArrayList<>();

        groups.add(OptionGroup
                .createBuilder()
                .add(OptionImpl.createBuilder(boolean.class, configStorage).setName(Component.translatable("config.borderlessmining.general.enabled")).setTooltip(Component.translatable("config.borderlessmining.general.enabled.tooltip")).setControl(TickBoxControl::new).setBinding(ConfigHandler::setEnabledPending, ConfigHandler::isEnabled).build())
                .add(OptionImpl.createBuilder(boolean.class, configStorage).setName(Component.translatable("config.borderlessmining.general.videomodeoption")).setTooltip(Component.translatable("config.borderlessmining.general.videomodeoption.tooltip")).setControl(TickBoxControl::new).setBinding((opt, value) -> opt.addToVanillaVideoSettings = value, (opt) -> opt.addToVanillaVideoSettings).build())
                .add(OptionImpl.createBuilder(boolean.class, configStorage).setName(Component.translatable("config.borderlessmining.general.enabledmac")).setTooltip(Component.translatable("config.borderlessmining.general.enabledmac.tooltip")).setControl(TickBoxControl::new).setBinding((opt, value) -> opt.enableMacOS = value, (opt) -> opt.enableMacOS).build())
                .build());

        // monitors are not listed because of the way sodium works. will implement later

        groups.add(OptionGroup
                .createBuilder()
                .add(OptionImpl.createBuilder(boolean.class, configStorage).setName(Component.translatable("config.borderlessmining.dimensions")).setTooltip(Component.empty()).setControl(TickBoxControl::new).setBinding((opt, val) -> opt.customWindowDimensions = opt.customWindowDimensions.setEnabled(true), (opt) -> opt.customWindowDimensions.enabled).build())
                .add(OptionImpl.createBuilder(boolean.class, configStorage).setName(Component.translatable("config.borderlessmining.dimensions.monitorcoordinates")).setTooltip(Component.translatable("config.borderlessmining.dimensions.monitorcoordinates.tooltip")).setControl(TickBoxControl::new).setBinding((opt, val) -> opt.customWindowDimensions = opt.customWindowDimensions.setUseMonitorCoordinates(true), (opt) -> opt.customWindowDimensions.useMonitorCoordinates).build())
                .add(OptionImpl.createBuilder(int.class, configStorage).setName(Component.translatable("config.borderlessmining.dimensions.x")).setTooltip(Component.empty()).setControl(option -> new SliderControl(option, 0, 9999, 1, ControlValueFormatter.number())).setBinding((opt, val) -> opt.customWindowDimensions = opt.customWindowDimensions.setX(val), (opt) -> opt.customWindowDimensions.x).build())
                .add(OptionImpl.createBuilder(int.class, configStorage).setName(Component.translatable("config.borderlessmining.dimensions.y")).setTooltip(Component.empty()).setControl(option -> new SliderControl(option, 0, 9999, 1, ControlValueFormatter.number())).setBinding((opt, val) -> opt.customWindowDimensions = opt.customWindowDimensions.setY(val), (opt) -> opt.customWindowDimensions.y).build())
                .add(OptionImpl.createBuilder(int.class, configStorage).setName(Component.translatable("config.borderlessmining.dimensions.width")).setTooltip(Component.empty()).setControl(option -> new SliderControl(option, 0, 9999, 1, ControlValueFormatter.number())).setBinding((opt, val) -> opt.customWindowDimensions = opt.customWindowDimensions.setWidth(val), (opt) -> opt.customWindowDimensions.width).build())
                .add(OptionImpl.createBuilder(int.class, configStorage).setName(Component.translatable("config.borderlessmining.dimensions.height")).setTooltip(Component.empty()).setControl(option -> new SliderControl(option, 0, 9999, 1, ControlValueFormatter.number())).setBinding((opt, val) -> opt.customWindowDimensions = opt.customWindowDimensions.setHeight(val), (opt) -> opt.customWindowDimensions.height).build())
                .build());

        return new OptionPage(Component.translatable("config.borderlessmining.sodium"), ImmutableList.copyOf(groups));
    }
}
