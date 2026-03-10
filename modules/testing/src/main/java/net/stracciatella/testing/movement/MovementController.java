package net.stracciatella.testing.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.stracciatella.testing.api.TestContext;

/**
 * Controls the local player's movement inputs for testing.
 * Simulates keyboard inputs (forward, sprint, jump) on the client side.
 */
public class MovementController {

    /**
     * Set the player's look direction toward a target position.
     * Must be called on the render thread (e.g. inside {@link TestContext#runOnClient}).
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
     * Must be called on the render thread.
     */
    public static void lookAtBlock(LocalPlayer player, BlockPos target) {
        lookAt(player, Vec3.atBottomCenterOf(target).add(0, 1.0, 0));
    }

    /**
     * Press forward movement key. Must be called on the render thread.
     */
    public static void pressForward(boolean press) {
        Minecraft.getInstance().options.keyUp.setDown(press);
    }

    /**
     * Press sprint key. Must be called on the render thread.
     */
    public static void pressSprint(boolean press) {
        Minecraft.getInstance().options.keySprint.setDown(press);
    }

    /**
     * Press jump key. Must be called on the render thread.
     */
    public static void pressJump(boolean press) {
        Minecraft.getInstance().options.keyJump.setDown(press);
    }

    /**
     * Release all movement keys. Must be called on the render thread.
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
     * Teleport the player to a position using a command.
     */
    public static void teleport(TestContext ctx, BlockPos pos) {
        ctx.runCommand("tp @s " + pos.getX() + " " + pos.getY() + " " + pos.getZ());
    }
}
