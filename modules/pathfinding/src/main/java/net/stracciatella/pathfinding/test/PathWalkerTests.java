package net.stracciatella.pathfinding.test;

import net.minecraft.core.BlockPos;
import net.stracciatella.pathfinding.logic.PathWalker;
import net.stracciatella.pathfinding.logic.mesh.MeshNode;
import net.stracciatella.testing.api.MinecraftTest;
import net.stracciatella.testing.api.TestContext;
import net.stracciatella.testing.api.TestSuite;
import net.stracciatella.testing.api.TickHandler;
import net.stracciatella.testing.movement.MovementController;
import net.stracciatella.testing.movement.MovementTracker;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests for the PathWalker. Builds MeshNode paths and verifies the walker
 * reaches each destination autonomously.
 */
@TestSuite(name = "PathWalker Tests")
public class PathWalkerTests implements TickHandler {
    private MovementTracker tracker;
    private TestContext activeCtx;
    private BlockPos target;
    private int phase;
    private int waitTicks;

    @MinecraftTest(name = "PathWalker straight line walk", timeoutTicks = 400, order = 1)
    public void straightLineWalk(TestContext ctx) {
        activeCtx = ctx;
        tracker = new MovementTracker();
        phase = 0;
        waitTicks = 0;

        BlockPos start = ctx.playerBlockPos();
        target = start.north(8);

        // Teleport to ensure clean start position
        MovementController.teleport(ctx.player(), start);
        phase = 1;
    }

    @MinecraftTest(name = "PathWalker L-shaped path", timeoutTicks = 600, order = 2)
    public void lShapedPath(TestContext ctx) {
        activeCtx = ctx;
        tracker = new MovementTracker();
        phase = 0;
        waitTicks = 0;

        BlockPos start = ctx.playerBlockPos();
        BlockPos corner = start.north(5);
        target = corner.east(5);

        MovementController.teleport(ctx.player(), start);
        phase = 10;
    }

    @Override
    public void onTick(TestContext ctx) {
        if (activeCtx == null || activeCtx.isFinished()) return;
        if (ctx.player() == null) return;

        tracker.recordTick(ctx.player());
        waitTicks++;

        // --- Straight line walk ---
        if (phase == 1) {
            if (waitTicks >= 3) {
                BlockPos start = ctx.playerBlockPos();
                PathWalker.start(buildStraightPath(start, target));
                phase = 2;
            }
        } else if (phase == 2) {
            if (!PathWalker.isActive()) {
                if (tracker.isAtBlock(target)) {
                    activeCtx.complete();
                } else {
                    activeCtx.fail("PathWalker stopped but player not at target " + target
                            + ", actual: " + ctx.playerBlockPos());
                }
            }
            if (tracker.fellBelow(ctx.playerBlockPos().getY() - 3)) {
                PathWalker.stop();
                activeCtx.fail("Player fell off the path");
            }
        }

        // --- L-shaped path ---
        if (phase == 10) {
            if (waitTicks >= 3) {
                BlockPos start = ctx.playerBlockPos();
                BlockPos corner = start.north(5);

                List<MeshNode> path = new ArrayList<>();
                for (int i = 0; i <= 5; i++) {
                    BlockPos pos = start.north(i);
                    path.add(new MeshNode(pos.getX(), pos.getY(), pos.getZ()));
                }
                for (int i = 1; i <= 5; i++) {
                    BlockPos pos = corner.east(i);
                    path.add(new MeshNode(pos.getX(), pos.getY(), pos.getZ()));
                }

                PathWalker.start(path);
                phase = 11;
            }
        } else if (phase == 11) {
            if (!PathWalker.isActive()) {
                if (tracker.isAtBlock(target)) {
                    activeCtx.complete();
                } else {
                    activeCtx.fail("PathWalker stopped but player not at target " + target
                            + ", actual: " + ctx.playerBlockPos());
                }
            }
            if (tracker.fellBelow(ctx.playerBlockPos().getY() - 3)) {
                PathWalker.stop();
                activeCtx.fail("Player fell off the path");
            }
        }
    }

    private List<MeshNode> buildStraightPath(BlockPos from, BlockPos to) {
        List<MeshNode> path = new ArrayList<>();
        int dx = Integer.signum(to.getX() - from.getX());
        int dz = Integer.signum(to.getZ() - from.getZ());
        BlockPos current = from;
        while (!current.equals(to)) {
            path.add(new MeshNode(current.getX(), current.getY(), current.getZ()));
            current = current.offset(dx, 0, dz);
        }
        path.add(new MeshNode(to.getX(), to.getY(), to.getZ()));
        return path;
    }
}
