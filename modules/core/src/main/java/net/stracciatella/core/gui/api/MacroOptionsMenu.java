package net.stracciatella.core.gui.api;

import net.stracciatella.core.macro.SettingsDictonary;

public class MacroOptionsMenu extends MenuOverlay {

    public MacroOptionsMenu() {
        super("Macro Options");
        super.menuItems.add(new MenuItem("Start Walking", () -> {
            System.out.println("started walking");
            SettingsDictonary.isWalking = true;
        }));

        super.menuItems.add(new MenuItem("Stop Walking", () -> {
            System.out.println("stopped walking");
            SettingsDictonary.isWalking = false;
        }));

        super.menuItems.add(new MenuItem("Set Goal position", () -> {
            System.out.println("setting goal");
            SettingsDictonary.shouldSetGoal = true;
        }));


    }

}
