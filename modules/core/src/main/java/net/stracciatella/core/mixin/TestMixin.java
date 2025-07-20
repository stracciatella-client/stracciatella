package net.stracciatella.core.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.stracciatella.core.gui.api.MacroOptionsMenu;
import net.stracciatella.core.macro.HumanizedPathfinder;
import net.stracciatella.core.macro.PathWalker;
import net.stracciatella.core.macro.SettingsDictonary;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public class TestMixin {

    PathWalker pathWalker = new PathWalker();

    @Inject(at = @At("HEAD"), method = "tick")
    private void run(CallbackInfo ci) {

        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = (LocalPlayer) (Object) this;

        openMacroOptionsMenu(client);

        if (SettingsDictonary.shouldSetGoal) {
            SettingsDictonary.shouldSetGoal = false;
            SettingsDictonary.goalPosition = player.blockPosition();
            System.out.println("setting goal to " + SettingsDictonary.goalPosition.toString());
        }

        if (SettingsDictonary.isWalking) {
            if (!pathWalker.isActive()) {
                pathWalker.start(new HumanizedPathfinder(player.blockPosition(), SettingsDictonary.goalPosition, 0, 0).findPath());
            }
            pathWalker.tick();
        } else {
            if (pathWalker.isActive()) {
                pathWalker.stop();
            }
        }





        // // Direkte Bewegungssimulation
        // if (shouldAutoMove()) {
        //
        //     player.input. = true;  // W-Taste simulieren
        //     player.input.down = false; // S-Taste
        //     player.input.left = false; // A-Taste
        //     player.input.right = false; // D-Taste
        // }
        //
        // if (client.player != null && shouldSimulateInventory()) {
        //     // Inventar-Slot wechseln
        //     client.player.getInventory().selected = targetSlot;
        //
        //     // Rechtsklick simulieren (Item verwenden)
        //     if (client.gameMode != null) {
        //         client.gameMode.useItem(client.player, client.level, InteractionHand.MAIN_HAND);
        //     }
        // }


    }

    private void openMacroOptionsMenu(Minecraft client) {
        boolean currentRightShiftState = GLFW.glfwGetKey(client.getWindow().getWindow(),
                GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;


        if (currentRightShiftState) {
            // Rechte Shift-Taste wurde gedrückt
            if (client.screen == null) { // Nur öffnen wenn kein anderes GUI offen ist
                client.setScreen(new MacroOptionsMenu());
            }
        }
    }
}
