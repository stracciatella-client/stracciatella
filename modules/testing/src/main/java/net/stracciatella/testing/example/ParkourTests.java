package net.stracciatella.testing.example;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.stracciatella.testing.api.MinecraftTest;
import net.stracciatella.testing.api.TestContext;
import net.stracciatella.testing.api.TestSuite;
import net.stracciatella.testing.api.TickHandler;
import net.stracciatella.testing.movement.MovementController;
import net.stracciatella.testing.movement.MovementTracker;

/**
 * Example parkour test suite. Demonstrates how to test player movement
 * by walking to a target and verifying arrival.
 */
@TestSuite(name = "Parkour Tests")
public class ParkourTests implements TickHandler {
    private MovementTracker tracker;
    private TestContext activeCtx;
    private BlockPos target;
    private int phase;

    @MinecraftTest(name = "Walk to target block", timeoutTicks = 200, order = 1)
    public void walkToTarget(TestContext ctx) {
        // Teleport player to a known start, then walk forward to a target
        activeCtx = ctx;
        tracker = new MovementTracker();
        target = ctx.playerBlockPos().north(5);
        phase = 0;

        // Teleport to ensure clean start
        MovementController.teleport(ctx.player(), ctx.playerBlockPos());
    }

    @MinecraftTest(name = "Sprint jump across gap", timeoutTicks = 300, order = 2)
    public void sprintJumpAcrossGap(TestContext ctx) {
        activeCtx = ctx;
        tracker = new MovementTracker();
        // Target is 4 blocks away (a standard parkour jump)
        target = ctx.playerBlockPos().north(4);
        phase = 10; // different phase to distinguish from walk test

        MovementController.lookAtBlock(ctx.player(), target);
        MovementController.pressSprint(true);
        MovementController.pressForward(true);
    }

    @Override
    public void onTick(TestContext ctx) {
        if (activeCtx == null || activeCtx.isFinished()) return;
        if (ctx.player() == null) return;

        tracker.recordTick(ctx.player());

        // Walk test
        if (phase == 0) {
            // Wait a tick for teleport, then start walking
            if (tracker.tickCount() == 2) {
                MovementController.lookAtBlock(ctx.player(), target);
                MovementController.pressForward(true);
                phase = 1;
            }
        } else if (phase == 1) {
            if (tracker.isAtBlock(target) && tracker.isOnGround()) {
                MovementController.releaseAll();
                activeCtx.complete();
            }
            if (tracker.fellBelow(ctx.playerBlockPos().getY() - 3)) {
                MovementController.releaseAll();
                activeCtx.fail("Player fell off the path");
            }
        }

        // Sprint jump test
        if (phase == 10) {
            if (tracker.tickCount() == 5) {
                MovementController.pressJump(true);
                phase = 11;
            }
        } else if (phase == 11) {
            // Release jump after 1 tick
            if (tracker.tickCount() == 7) {
                MovementController.pressJump(false);
                phase = 12;
            }
        } else if (phase == 12) {
            if (tracker.isAtBlock(target) && tracker.isOnGround()) {
                MovementController.releaseAll();
                activeCtx.complete();
            }
            if (tracker.fellBelow(ctx.playerBlockPos().getY() - 5)) {
                MovementController.releaseAll();
                activeCtx.fail("Player fell into the gap");
            }
        }
    }
}
