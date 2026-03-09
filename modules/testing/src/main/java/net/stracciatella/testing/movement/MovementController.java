package net.stracciatella.testing.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/**
 * Controls the local player's movement inputs for testing.
 * Simulates keyboard inputs (forward, sprint, jump) on the client side.
 */
public class MovementController {

    /**
     * Set the player's look direction toward a target block.
     */
    public static void lookAt(LocalPlayer player, Vec3 target) {
        double dx = target.x - player.getX();
        double dy = target.y - player.getEyeY();
        double dz = target.z - player.getZ();
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.toDegrees(Math.atan2(-dx, dz)));
        float pitch = (float) (Math.toDegrees(-Math.atan2(dy, horizontalDist)));
        player.setYRot(yaw);
        player.setXRot(pitch);
    }

    /**
     * Set the player's look direction toward a target block position (center of block top).
     */
    public static void lookAtBlock(LocalPlayer player, BlockPos target) {
        lookAt(player, Vec3.atBottomCenterOf(target).add(0, 1.0, 0));
    }

    /**
     * Press forward movement key.
     */
    public static void pressForward(boolean press) {
        Minecraft.getInstance().options.keyUp.setDown(press);
    }

    /**
     * Press sprint key.
     */
    public static void pressSprint(boolean press) {
        Minecraft.getInstance().options.keySprint.setDown(press);
    }

    /**
     * Press jump key.
     */
    public static void pressJump(boolean press) {
        Minecraft.getInstance().options.keyJump.setDown(press);
    }

    /**
     * Release all movement keys.
     */
    public static void releaseAll() {
        var opts = Minecraft.getInstance().options;
        opts.keyUp.setDown(false);
        opts.keyDown.setDown(false);
        opts.keyLeft.setDown(false);
        opts.keyRight.setDown(false);
        opts.keyJump.setDown(false);
        opts.keySprint.setDown(false);
    }

    /**
     * Teleport the player to a position using a client command.
     */
    public static void teleport(LocalPlayer player, BlockPos pos) {
        player.connection.sendUnsignedCommand("tp " + player.getName().getString() + " " + pos.getX() + " " + pos.getY() + " " + pos.getZ());
    }
}
