package net.stracciatella.testing.api;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Provides access to the Minecraft client state during a test.
 * Passed to every {@link MinecraftTest} method.
 */
public class TestContext {
    private final Minecraft minecraft;
    private final Runnable completeCallback;
    private final java.util.function.Consumer<String> failCallback;
    private boolean finished;

    public TestContext(Minecraft minecraft, Runnable completeCallback, java.util.function.Consumer<String> failCallback) {
        this.minecraft = minecraft;
        this.completeCallback = completeCallback;
        this.failCallback = failCallback;
    }

    public Minecraft minecraft() {
        return minecraft;
    }

    public LocalPlayer player() {
        return minecraft.player;
    }

    public Level level() {
        return minecraft.player.level();
    }

    public Vec3 playerPos() {
        return minecraft.player.position();
    }

    public BlockPos playerBlockPos() {
        return minecraft.player.blockPosition();
    }

    /**
     * Mark this test as successfully completed.
     */
    public void complete() {
        if (!finished) {
            finished = true;
            completeCallback.run();
        }
    }

    /**
     * Mark this test as failed with a message.
     */
    public void fail(String reason) {
        if (!finished) {
            finished = true;
            failCallback.accept(reason);
        }
    }

    public boolean isFinished() {
        return finished;
    }
}
