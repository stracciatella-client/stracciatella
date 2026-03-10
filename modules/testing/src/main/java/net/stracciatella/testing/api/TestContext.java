package net.stracciatella.testing.api;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Provides blocking test utilities that run on a dedicated test thread.
 * Use {@link #waitTick()}, {@link #waitFor(Predicate)}, and {@link #runOnClient}
 * to synchronize with the render thread.
 */
public class TestContext {
    private final CountDownLatch[] tickLatch = {new CountDownLatch(1)};
    private int remainingTicks;

    public TestContext() {
        this.remainingTicks = Integer.MAX_VALUE;
    }

    /**
     * Set the remaining tick budget for the current test.
     */
    public void setTimeoutTicks(int ticks) {
        this.remainingTicks = ticks;
    }

    /**
     * Called by the tick mixin on the render thread each client tick.
     * Signals the test thread that a tick has completed.
     */
    public void onClientTick() {
        tickLatch[0].countDown();
    }

    /**
     * Wait one client tick. Blocks the test thread until the next tick completes.
     */
    public void waitTick() {
        try {
            tickLatch[0].await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Test thread interrupted", e);
        }
        tickLatch[0] = new CountDownLatch(1);
        remainingTicks--;
    }

    /**
     * Wait the given number of client ticks.
     */
    public void waitTicks(int ticks) {
        for (int i = 0; i < ticks; i++) {
            waitTick();
        }
    }

    /**
     * Wait until the predicate returns true, checking each tick.
     * Uses the remaining tick budget as timeout. Throws on timeout.
     *
     * @return the number of ticks waited
     */
    public int waitFor(Predicate<Minecraft> predicate) {
        return waitFor(predicate, Math.max(1, remainingTicks));
    }

    /**
     * Wait until the predicate returns true, with a specific timeout.
     * The predicate runs on the render thread.
     *
     * @return the number of ticks waited
     */
    public int waitFor(Predicate<Minecraft> predicate, int timeout) {
        for (int i = 0; i < timeout; i++) {
            waitTick();
            boolean result = computeOnClient(predicate::test);
            if (result) {
                remainingTicks -= i + 1;
                return i + 1;
            }
        }
        throw new AssertionError("Timed out after " + timeout + " ticks");
    }

    /**
     * Run an action on the render thread and wait for it to complete.
     */
    public void runOnClient(Runnable action) {
        Minecraft mc = Minecraft.getInstance();
        CompletableFuture<Void> future = new CompletableFuture<>();
        mc.execute(() -> {
            try {
                action.run();
                future.complete(null);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        try {
            future.get();
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof RuntimeException re) throw re;
            if (cause instanceof Error err) throw err;
            throw new RuntimeException(cause);
        }
    }

    /**
     * Run an action on the render thread with access to the Minecraft instance.
     */
    public void runOnClient(java.util.function.Consumer<Minecraft> action) {
        runOnClient(() -> action.accept(Minecraft.getInstance()));
    }

    /**
     * Compute a value on the render thread.
     */
    public <T> T computeOnClient(Function<Minecraft, T> function) {
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        Minecraft mc = Minecraft.getInstance();
        CompletableFuture<Void> future = new CompletableFuture<>();
        mc.execute(() -> {
            try {
                result.set(function.apply(mc));
                future.complete(null);
            } catch (Throwable t) {
                error.set(t);
                future.complete(null);
            }
        });
        try {
            future.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (error.get() != null) {
            Throwable t = error.get();
            if (t instanceof RuntimeException re) throw re;
            if (t instanceof Error err) throw err;
            throw new RuntimeException(t);
        }
        return result.get();
    }

    // --- Convenience accessors (run on render thread) ---

    public Minecraft minecraft() {
        return Minecraft.getInstance();
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
        runOnClient(mc -> {
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
