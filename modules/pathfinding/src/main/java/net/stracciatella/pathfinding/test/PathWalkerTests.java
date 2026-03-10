package net.stracciatella.pathfinding.test;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.stracciatella.pathfinding.logic.PathWalker;
import net.stracciatella.pathfinding.logic.mesh.MeshNode;
import net.stracciatella.testing.api.MinecraftTest;
import net.stracciatella.testing.api.TestContext;
import net.stracciatella.testing.api.TestSuite;
import net.stracciatella.testing.api.TickHandler;
import net.stracciatella.testing.command.CommandExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Tests for the PathWalker. Each test places a single start block and a single
 * goal block separated by air, then runs the PathWalker to jump across.
 *
 * Phase flow:
 *   Phase 0: Teleport to course area, wait for chunks to load
 *   Phase 1: Clear area + place blocks + teleport to start, wait for player on ground
 *   Phase 2: Start PathWalker, monitor for arrival or fall
 */
@TestSuite(name = "PathWalker Tests")
public class PathWalkerTests implements TickHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("PathWalkerTests");
    private static final int CLEAR_RADIUS = 5;
    private static final double ARRIVAL_RADIUS = 0.6;

    private TestContext activeCtx;
    private BlockPos worldStart;
    private BlockPos worldEnd;
    private int phase;

    @MinecraftTest(name = "PathWalker 1-block gap", timeoutTicks = 600, order = -100, repeat = 5)
    public void gap1(TestContext ctx) {
        // 1 start block, 1 air gap, 1 goal block — heading north (-Z)
        runCourse(ctx, new BlockPos(100, 30, 100),
                new BlockPos(0, 0, 0),
                new BlockPos(0, 0, -2));
    }

    @MinecraftTest(name = "PathWalker 2-block gap", timeoutTicks = 600, order = -99, repeat = 5)
    public void gap2(TestContext ctx) {
        runCourse(ctx, new BlockPos(150, 30, 100),
                new BlockPos(0, 0, 0),
                new BlockPos(0, 0, -3));
    }

    @MinecraftTest(name = "PathWalker 3-block gap", timeoutTicks = 800, order = -98, repeat = 5)
    public void gap3(TestContext ctx) {
        runCourse(ctx, new BlockPos(200, 30, 100),
                new BlockPos(0, 0, 0),
                new BlockPos(0, 0, -4));
    }

    @MinecraftTest(name = "PathWalker 4-block gap", timeoutTicks = 800, order = -97, repeat = 5)
    public void gap4(TestContext ctx) {
        runCourse(ctx, new BlockPos(250, 30, 100),
                new BlockPos(0, 0, 0),
                new BlockPos(0, 0, -5));
    }

    private void runCourse(TestContext ctx, BlockPos origin, BlockPos relStart, BlockPos relEnd) {
        activeCtx = ctx;
        phase = 0;

        worldStart = origin.offset(relStart);
        worldEnd = origin.offset(relEnd);

        LOGGER.info("Setting up course: start={}, end={}", worldStart, worldEnd);

        // Teleport to course area to trigger chunk loading
        BlockPos center = new BlockPos(
                (worldStart.getX() + worldEnd.getX()) / 2,
                origin.getY() + 5,
                (worldStart.getZ() + worldEnd.getZ()) / 2);
        CommandExecutor.executeCommand("tp @s " + (center.getX() + 0.5) + " " + center.getY() + " " + (center.getZ() + 0.5));
    }

    @Override
    public void onTick(TestContext ctx) {
        if (activeCtx == null || activeCtx.isFinished()) return;
        if (ctx.player() == null) return;

        // Phase 0: wait for chunks to load and player to arrive near the course area
        if (phase == 0) {
            boolean chunksLoaded = ctx.level().hasChunkAt(worldStart) && ctx.level().hasChunkAt(worldEnd);
            boolean playerNearCourse = Math.abs(ctx.playerPos().y - (worldStart.getY() + 5)) < 3
                    && horizontalDistance(ctx.playerPos(), worldStart.getX() + 0.5, worldStart.getZ() + 0.5) < 20;
            if (chunksLoaded && playerNearCourse) {
                buildCourse(ctx);
                // Teleport player on top of start block (center)
                CommandExecutor.executeCommand("tp @s "
                        + (worldStart.getX() + 0.5) + " " + (worldStart.getY() + 1) + " " + (worldStart.getZ() + 0.5));
                phase = 1;
            }
            return;
        }

        // Phase 1: wait for player to be on ground near start block
        if (phase == 1) {
            boolean nearStart = horizontalDistance(ctx.playerPos(), worldStart.getX() + 0.5, worldStart.getZ() + 0.5) < 1.0;
            if (ctx.player().onGround() && nearStart) {
                LOGGER.info("Starting PathWalker, player at {}", ctx.playerBlockPos());
                List<MeshNode> path = List.of(
                        new MeshNode(worldStart.getX(), worldStart.getY(), worldStart.getZ()),
                        new MeshNode(worldEnd.getX(), worldEnd.getY(), worldEnd.getZ()));
                PathWalker.start(path);
                phase = 2;
            }
            return;
        }

        // Phase 2: monitor PathWalker
        if (phase == 2) {
            if (checkFall(ctx)) return;

            // Check arrival: on ground and within ARRIVAL_RADIUS of goal block center
            double hDist = horizontalDistance(ctx.playerPos(), worldEnd.getX() + 0.5, worldEnd.getZ() + 0.5);
            if (ctx.player().onGround() && hDist < ARRIVAL_RADIUS) {
                PathWalker.stop();
                LOGGER.info("PathWalker reached goal (distance: {}, onGround: true)", String.format("%.2f", hDist));
                activeCtx.complete();
                return;
            }

            if (!PathWalker.isActive()) {
                activeCtx.fail("PathWalker stopped but player not at goal, distance=" +
                        String.format("%.2f", hDist) + ", pos=" + ctx.playerBlockPos());
            }
        }
    }

    private void buildCourse(TestContext ctx) {
        // Clear area around start and end blocks
        int minX = Math.min(worldStart.getX(), worldEnd.getX());
        int minY = Math.min(worldStart.getY(), worldEnd.getY());
        int minZ = Math.min(worldStart.getZ(), worldEnd.getZ());
        int maxX = Math.max(worldStart.getX(), worldEnd.getX());
        int maxY = Math.max(worldStart.getY(), worldEnd.getY());
        int maxZ = Math.max(worldStart.getZ(), worldEnd.getZ());

        CommandExecutor.executeCommand("fill "
                + (minX - CLEAR_RADIUS) + " " + (minY - CLEAR_RADIUS) + " " + (minZ - CLEAR_RADIUS) + " "
                + (maxX + CLEAR_RADIUS) + " " + (maxY + CLEAR_RADIUS) + " " + (maxZ + CLEAR_RADIUS) + " air");

        // Place start and end blocks
        CommandExecutor.executeCommand("setblock " + worldStart.getX() + " " + worldStart.getY() + " " + worldStart.getZ() + " stone");
        CommandExecutor.executeCommand("setblock " + worldEnd.getX() + " " + worldEnd.getY() + " " + worldEnd.getZ() + " stone");

        LOGGER.info("Built course: 2 blocks placed, cleared ({},{},{}) to ({},{},{})",
                minX - CLEAR_RADIUS, minY - CLEAR_RADIUS, minZ - CLEAR_RADIUS,
                maxX + CLEAR_RADIUS, maxY + CLEAR_RADIUS, maxZ + CLEAR_RADIUS);
    }

    private static double horizontalDistance(Vec3 pos, double x, double z) {
        return Math.sqrt(Math.pow(pos.x - x, 2) + Math.pow(pos.z - z, 2));
    }

    private boolean checkFall(TestContext ctx) {
        double playerY = ctx.player().position().y;
        int minY = Math.min(worldStart.getY(), worldEnd.getY());
        if (playerY < minY - 2) {
            PathWalker.stop();
            activeCtx.fail("Player fell at " + ctx.playerBlockPos()
                    + " (y=" + String.format("%.2f", playerY) + ")");
            return true;
        }
        return false;
    }
}
