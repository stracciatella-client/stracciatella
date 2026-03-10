package net.stracciatella.pathfinding.test;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.stracciatella.pathfinding.logic.PathWalker;
import net.stracciatella.pathfinding.logic.mesh.MeshNode;
import net.stracciatella.testing.api.MinecraftTest;
import net.stracciatella.testing.api.TestContext;
import net.stracciatella.testing.api.TestSuite;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Tests for the PathWalker. Each test places a single start block and a single
 * goal block separated by air, then runs the PathWalker to jump across.
 * <p>
 * Uses the blocking gametest model: each test method runs synchronously,
 * using {@code ctx.waitFor()} to wait for conditions instead of tick callbacks.
 */
@TestSuite(name = "PathWalker Tests")
public class PathWalkerTests {
    private static final Logger LOGGER = LoggerFactory.getLogger("PathWalkerTests");
    private static final int CLEAR_RADIUS = 5;
    private static final double ARRIVAL_RADIUS = 0.6;

    @MinecraftTest(name = "PathWalker 1-block gap", timeoutTicks = 600, order = -100, repeat = 5)
    public void gap1(TestContext ctx) {
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
        BlockPos worldStart = origin.offset(relStart);
        BlockPos worldEnd = origin.offset(relEnd);

        LOGGER.info("Setting up course: start={}, end={}", worldStart, worldEnd);

        // Phase 0: Teleport to course area and wait for chunks
        BlockPos center = new BlockPos(
                (worldStart.getX() + worldEnd.getX()) / 2,
                origin.getY() + 5,
                (worldStart.getZ() + worldEnd.getZ()) / 2);
        ctx.runCommand("tp @s " + (center.getX() + 0.5) + " " + center.getY() + " " + (center.getZ() + 0.5));

        ctx.waitFor(mc -> {
            boolean chunksLoaded = mc.level.hasChunkAt(worldStart) && mc.level.hasChunkAt(worldEnd);
            boolean playerNearCourse = Math.abs(mc.player.position().y - (worldStart.getY() + 5)) < 3
                    && horizontalDistance(mc.player.position(), worldStart.getX() + 0.5, worldStart.getZ() + 0.5) < 20;
            return chunksLoaded && playerNearCourse;
        });

        // Phase 1: Build course, teleport to start, wait for on-ground
        buildCourse(ctx, worldStart, worldEnd);
        ctx.runCommand("tp @s "
                + (worldStart.getX() + 0.5) + " " + (worldStart.getY() + 1) + " " + (worldStart.getZ() + 0.5));

        ctx.waitFor(mc -> {
            boolean nearStart = horizontalDistance(mc.player.position(),
                    worldStart.getX() + 0.5, worldStart.getZ() + 0.5) < 1.0;
            return mc.player.onGround() && nearStart;
        });

        // Phase 2: Start PathWalker and monitor
        LOGGER.info("Starting PathWalker");
        ctx.runOnClient(mc -> {
            List<MeshNode> path = List.of(
                    new MeshNode(worldStart.getX(), worldStart.getY(), worldStart.getZ()),
                    new MeshNode(worldEnd.getX(), worldEnd.getY(), worldEnd.getZ()));
            PathWalker.start(path);
        });

        int minY = Math.min(worldStart.getY(), worldEnd.getY());
        ctx.waitFor(mc -> {
            // Check fall
            double playerY = mc.player.position().y;
            if (playerY < minY - 2) {
                PathWalker.stop();
                throw new AssertionError("Player fell at " + mc.player.blockPosition()
                        + " (y=" + String.format("%.2f", playerY) + ")");
            }

            // Check arrival
            double hDist = horizontalDistance(mc.player.position(),
                    worldEnd.getX() + 0.5, worldEnd.getZ() + 0.5);
            if (mc.player.onGround() && hDist < ARRIVAL_RADIUS) {
                PathWalker.stop();
                LOGGER.info("PathWalker reached goal (distance: {}, onGround: true)",
                        String.format("%.2f", hDist));
                return true;
            }

            // Check PathWalker stopped unexpectedly
            if (!PathWalker.isActive()) {
                throw new AssertionError("PathWalker stopped but player not at goal, distance="
                        + String.format("%.2f", hDist) + ", pos=" + mc.player.blockPosition());
            }

            return false;
        });
    }

    private void buildCourse(TestContext ctx, BlockPos worldStart, BlockPos worldEnd) {
        int minX = Math.min(worldStart.getX(), worldEnd.getX());
        int minY = Math.min(worldStart.getY(), worldEnd.getY());
        int minZ = Math.min(worldStart.getZ(), worldEnd.getZ());
        int maxX = Math.max(worldStart.getX(), worldEnd.getX());
        int maxY = Math.max(worldStart.getY(), worldEnd.getY());
        int maxZ = Math.max(worldStart.getZ(), worldEnd.getZ());

        ctx.runCommand("fill "
                + (minX - CLEAR_RADIUS) + " " + (minY - CLEAR_RADIUS) + " " + (minZ - CLEAR_RADIUS) + " "
                + (maxX + CLEAR_RADIUS) + " " + (maxY + CLEAR_RADIUS) + " " + (maxZ + CLEAR_RADIUS) + " air");
        ctx.runCommand("setblock " + worldStart.getX() + " " + worldStart.getY() + " " + worldStart.getZ() + " stone");
        ctx.runCommand("setblock " + worldEnd.getX() + " " + worldEnd.getY() + " " + worldEnd.getZ() + " stone");

        LOGGER.info("Built course: 2 blocks placed, cleared ({},{},{}) to ({},{},{})",
                minX - CLEAR_RADIUS, minY - CLEAR_RADIUS, minZ - CLEAR_RADIUS,
                maxX + CLEAR_RADIUS, maxY + CLEAR_RADIUS, maxZ + CLEAR_RADIUS);
    }

    private static double horizontalDistance(Vec3 pos, double x, double z) {
        return Math.sqrt(Math.pow(pos.x - x, 2) + Math.pow(pos.z - z, 2));
    }
}
