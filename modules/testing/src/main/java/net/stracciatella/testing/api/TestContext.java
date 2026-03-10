package net.stracciatella.testing.api;

import net.fabricmc.fabric.api.client.gametest.v1.TestInput;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Wrapper around {@link ClientGameTestContext} providing convenience methods
 * for writing Minecraft integration tests.
 * <p>
 * Tests run on the gametest thread. Use {@link #runOnClient} or {@link #computeOnClient}
 * to access game state on the render thread. {@link #waitTick()}, {@link #waitTicks(int)},
 * and {@link #waitFor(Predicate)} block the gametest thread until conditions are met.
 */
public class TestContext {
    private final ClientGameTestContext gameTest;
    private int remainingTicks;

    public TestContext(ClientGameTestContext gameTest) {
        this.gameTest = gameTest;
        this.remainingTicks = Integer.MAX_VALUE;
    }

    /**
     * Set the remaining tick budget for the current test.
     */
    public void setTimeoutTicks(int ticks) {
        this.remainingTicks = ticks;
    }

    /**
     * Get the underlying Fabric gametest context.
     */
    public ClientGameTestContext gameTest() {
        return gameTest;
    }

    /**
     * Get the test input handler for simulating keyboard/mouse input.
     */
    public TestInput input() {
        return gameTest.getInput();
    }

    // --- Waiting ---

    /**
     * Wait one client tick.
     */
    public void waitTick() {
        gameTest.waitTick();
        remainingTicks--;
    }

    /**
     * Wait the given number of client ticks.
     */
    public void waitTicks(int ticks) {
        gameTest.waitTicks(ticks);
        remainingTicks -= ticks;
    }

    /**
     * Wait until the predicate returns true, checking each tick.
     * Uses the remaining tick budget from the test's timeout.
     *
     * @return the number of ticks waited
     */
    public int waitFor(Predicate<Minecraft> predicate) {
        int timeout = Math.max(1, remainingTicks);
        int waited = gameTest.waitFor(predicate, timeout);
        remainingTicks -= waited;
        return waited;
    }

    /**
     * Wait until the predicate returns true, with a specific timeout.
     *
     * @return the number of ticks waited
     */
    public int waitFor(Predicate<Minecraft> predicate, int timeout) {
        int waited = gameTest.waitFor(predicate, timeout);
        remainingTicks -= waited;
        return waited;
    }

    // --- Client thread access ---

    /**
     * Run an action on the render thread and wait for it to complete.
     */
    public void runOnClient(Runnable action) {
        gameTest.runOnClient(mc -> action.run());
    }

    /**
     * Run an action on the render thread with access to the Minecraft instance.
     */
    public void runOnClient(java.util.function.Consumer<Minecraft> action) {
        gameTest.runOnClient(action::accept);
    }

    /**
     * Compute a value on the render thread.
     */
    public <T> T computeOnClient(Function<Minecraft, T> function) {
        return gameTest.computeOnClient(function::apply);
    }

    // --- Convenience accessors (run on render thread) ---

    public Minecraft minecraft() {
        return computeOnClient(mc -> mc);
    }

    public LocalPlayer player() {
        return computeOnClient(mc -> mc.player);
    }

    public Level level() {
        return computeOnClient(mc -> mc.player.level());
    }

    public Vec3 playerPos() {
        return computeOnClient(mc -> mc.player.position());
    }

    public BlockPos playerBlockPos() {
        return computeOnClient(mc -> mc.player.blockPosition());
    }

    // --- Command execution ---

    /**
     * Execute a command as the local player (without the leading slash).
     * Waits one tick after sending for the command to process.
     */
    public void runCommand(String command) {
        gameTest.runOnClient(mc -> {
            if (mc.player != null) {
                mc.player.connection.sendCommand(command);
            }
        });
        waitTick();
    }

    // --- Assertions ---

    /**
     * Fail the test immediately with the given reason.
     */
    public void fail(String reason) {
        throw new AssertionError(reason);
    }
}
