package net.stracciatella.testing.movement;

import net.fabricmc.fabric.api.client.gametest.v1.TestInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.stracciatella.testing.api.TestContext;

/**
 * Controls the local player's movement inputs for testing.
 * Uses the Fabric GameTest {@link TestInput} API for key simulation.
 */
public class MovementController {

    /**
     * Set the player's look direction toward a target position.
     * Must be called within {@link TestContext#runOnClient}.
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
     * Must be called within {@link TestContext#runOnClient}.
     */
    public static void lookAtBlock(LocalPlayer player, BlockPos target) {
        lookAt(player, Vec3.atBottomCenterOf(target).add(0, 1.0, 0));
    }

    /**
     * Hold the forward movement key.
     */
    public static void pressForward(TestInput input) {
        input.holdKey(options -> options.keyUp);
    }

    /**
     * Release the forward movement key.
     */
    public static void releaseForward(TestInput input) {
        input.releaseKey(options -> options.keyUp);
    }

    /**
     * Hold the sprint key.
     */
    public static void pressSprint(TestInput input) {
        input.holdKey(options -> options.keySprint);
    }

    /**
     * Release the sprint key.
     */
    public static void releaseSprint(TestInput input) {
        input.releaseKey(options -> options.keySprint);
    }

    /**
     * Hold the jump key.
     */
    public static void pressJump(TestInput input) {
        input.holdKey(options -> options.keyJump);
    }

    /**
     * Release the jump key.
     */
    public static void releaseJump(TestInput input) {
        input.releaseKey(options -> options.keyJump);
    }

    /**
     * Release all movement keys.
     */
    public static void releaseAll(TestInput input) {
        input.releaseKey(options -> options.keyUp);
        input.releaseKey(options -> options.keyDown);
        input.releaseKey(options -> options.keyLeft);
        input.releaseKey(options -> options.keyRight);
        input.releaseKey(options -> options.keyJump);
        input.releaseKey(options -> options.keySprint);
    }

    /**
     * Teleport the player to a position using a command.
     */
    public static void teleport(TestContext ctx, BlockPos pos) {
        ctx.runCommand("tp @s " + pos.getX() + " " + pos.getY() + " " + pos.getZ());
    }
}
