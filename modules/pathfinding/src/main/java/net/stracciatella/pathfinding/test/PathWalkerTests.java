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

import java.util.ArrayList;
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

    // === Diagonal tests: jump forward+sideways (forward Z, side X) ===

    @MinecraftTest(name = "Diagonal 2x1", timeoutTicks = 800, order = -90, repeat = 3)
    public void diagonal_2x1(TestContext ctx) {
        runCourse(ctx, new BlockPos(300, 30, 100),
                new BlockPos(0, 0, 0),
                new BlockPos(1, 0, -2));
    }

    @MinecraftTest(name = "Diagonal 2x2", timeoutTicks = 800, order = -89, repeat = 3)
    public void diagonal_2x2(TestContext ctx) {
        runCourse(ctx, new BlockPos(330, 30, 100),
                new BlockPos(0, 0, 0),
                new BlockPos(2, 0, -2));
    }

    @MinecraftTest(name = "Diagonal 2x3", timeoutTicks = 800, order = -88, repeat = 3)
    public void diagonal_2x3(TestContext ctx) {
        runCourse(ctx, new BlockPos(360, 30, 100),
                new BlockPos(0, 0, 0),
                new BlockPos(3, 0, -2));
    }

    @MinecraftTest(name = "Diagonal 3x1", timeoutTicks = 800, order = -87, repeat = 3)
    public void diagonal_3x1(TestContext ctx) {
        runCourse(ctx, new BlockPos(390, 30, 100),
                new BlockPos(0, 0, 0),
                new BlockPos(1, 0, -3));
    }

    @MinecraftTest(name = "Diagonal 3x2", timeoutTicks = 800, order = -86, repeat = 3)
    public void diagonal_3x2(TestContext ctx) {
        runCourse(ctx, new BlockPos(420, 30, 100),
                new BlockPos(0, 0, 0),
                new BlockPos(2, 0, -3));
    }

    // === Chain tests: gap1 followed by gap2 in a straight line ===

    @MinecraftTest(name = "Chain 1→1", timeoutTicks = 1200, order = -80, repeat = 3)
    public void chain_1_1(TestContext ctx) { runChain(ctx, 0, 1, 1); }

    @MinecraftTest(name = "Chain 1→2", timeoutTicks = 1200, order = -80, repeat = 3)
    public void chain_1_2(TestContext ctx) { runChain(ctx, 1, 1, 2); }

    @MinecraftTest(name = "Chain 1→3", timeoutTicks = 1200, order = -80, repeat = 3)
    public void chain_1_3(TestContext ctx) { runChain(ctx, 2, 1, 3); }

    @MinecraftTest(name = "Chain 1→4", timeoutTicks = 1200, order = -80, repeat = 3)
    public void chain_1_4(TestContext ctx) { runChain(ctx, 3, 1, 4); }

    @MinecraftTest(name = "Chain 2→1", timeoutTicks = 1200, order = -79, repeat = 3)
    public void chain_2_1(TestContext ctx) { runChain(ctx, 4, 2, 1); }

    @MinecraftTest(name = "Chain 2→2", timeoutTicks = 1200, order = -79, repeat = 3)
    public void chain_2_2(TestContext ctx) { runChain(ctx, 5, 2, 2); }

    @MinecraftTest(name = "Chain 2→3", timeoutTicks = 1200, order = -79, repeat = 3)
    public void chain_2_3(TestContext ctx) { runChain(ctx, 6, 2, 3); }

    @MinecraftTest(name = "Chain 2→4", timeoutTicks = 1200, order = -79, repeat = 3)
    public void chain_2_4(TestContext ctx) { runChain(ctx, 7, 2, 4); }

    @MinecraftTest(name = "Chain 3→1", timeoutTicks = 1200, order = -78, repeat = 3)
    public void chain_3_1(TestContext ctx) { runChain(ctx, 8, 3, 1); }

    @MinecraftTest(name = "Chain 3→2", timeoutTicks = 1200, order = -78, repeat = 3)
    public void chain_3_2(TestContext ctx) { runChain(ctx, 9, 3, 2); }

    @MinecraftTest(name = "Chain 3→3", timeoutTicks = 1200, order = -78, repeat = 3)
    public void chain_3_3(TestContext ctx) { runChain(ctx, 10, 3, 3); }

    @MinecraftTest(name = "Chain 3→4", timeoutTicks = 1200, order = -78, repeat = 3)
    public void chain_3_4(TestContext ctx) { runChain(ctx, 11, 3, 4); }

    @MinecraftTest(name = "Chain 4→1", timeoutTicks = 1200, order = -77, repeat = 3)
    public void chain_4_1(TestContext ctx) { runChain(ctx, 12, 4, 1); }

    @MinecraftTest(name = "Chain 4→2", timeoutTicks = 1200, order = -77, repeat = 3)
    public void chain_4_2(TestContext ctx) { runChain(ctx, 13, 4, 2); }

    @MinecraftTest(name = "Chain 4→3", timeoutTicks = 1200, order = -77, repeat = 3)
    public void chain_4_3(TestContext ctx) { runChain(ctx, 14, 4, 3); }

    @MinecraftTest(name = "Chain 4→4", timeoutTicks = 1200, order = -77, repeat = 3)
    public void chain_4_4(TestContext ctx) { runChain(ctx, 15, 4, 4); }

    // === Corner tests: gap1 straight, then 90° left turn + gap2 ===

    @MinecraftTest(name = "Corner 1→1", timeoutTicks = 1200, order = -70, repeat = 3)
    public void corner_1_1(TestContext ctx) { runCorner(ctx, 0, 1, 1); }

    @MinecraftTest(name = "Corner 1→2", timeoutTicks = 1200, order = -70, repeat = 3)
    public void corner_1_2(TestContext ctx) { runCorner(ctx, 1, 1, 2); }

    @MinecraftTest(name = "Corner 1→3", timeoutTicks = 1200, order = -70, repeat = 3)
    public void corner_1_3(TestContext ctx) { runCorner(ctx, 2, 1, 3); }

    @MinecraftTest(name = "Corner 1→4", timeoutTicks = 1200, order = -70, repeat = 3)
    public void corner_1_4(TestContext ctx) { runCorner(ctx, 3, 1, 4); }

    @MinecraftTest(name = "Corner 2→1", timeoutTicks = 1200, order = -69, repeat = 3)
    public void corner_2_1(TestContext ctx) { runCorner(ctx, 4, 2, 1); }

    @MinecraftTest(name = "Corner 2→2", timeoutTicks = 1200, order = -69, repeat = 3)
    public void corner_2_2(TestContext ctx) { runCorner(ctx, 5, 2, 2); }

    @MinecraftTest(name = "Corner 2→3", timeoutTicks = 1200, order = -69, repeat = 3)
    public void corner_2_3(TestContext ctx) { runCorner(ctx, 6, 2, 3); }

    @MinecraftTest(name = "Corner 2→4", timeoutTicks = 1200, order = -69, repeat = 3)
    public void corner_2_4(TestContext ctx) { runCorner(ctx, 7, 2, 4); }

    @MinecraftTest(name = "Corner 3→1", timeoutTicks = 1200, order = -68, repeat = 3)
    public void corner_3_1(TestContext ctx) { runCorner(ctx, 8, 3, 1); }

    @MinecraftTest(name = "Corner 3→2", timeoutTicks = 1200, order = -68, repeat = 3)
    public void corner_3_2(TestContext ctx) { runCorner(ctx, 9, 3, 2); }

    @MinecraftTest(name = "Corner 3→3", timeoutTicks = 1200, order = -68, repeat = 3)
    public void corner_3_3(TestContext ctx) { runCorner(ctx, 10, 3, 3); }

    @MinecraftTest(name = "Corner 3→4", timeoutTicks = 1200, order = -68, repeat = 3)
    public void corner_3_4(TestContext ctx) { runCorner(ctx, 11, 3, 4); }

    @MinecraftTest(name = "Corner 4→1", timeoutTicks = 1200, order = -67, repeat = 3)
    public void corner_4_1(TestContext ctx) { runCorner(ctx, 12, 4, 1); }

    @MinecraftTest(name = "Corner 4→2", timeoutTicks = 1200, order = -67, repeat = 3)
    public void corner_4_2(TestContext ctx) { runCorner(ctx, 13, 4, 2); }

    @MinecraftTest(name = "Corner 4→3", timeoutTicks = 1200, order = -67, repeat = 3)
    public void corner_4_3(TestContext ctx) { runCorner(ctx, 14, 4, 3); }

    @MinecraftTest(name = "Corner 4→4", timeoutTicks = 1200, order = -67, repeat = 3)
    public void corner_4_4(TestContext ctx) { runCorner(ctx, 15, 4, 4); }

    private void runChain(TestContext ctx, int index, int gap1, int gap2) {
        BlockPos origin = new BlockPos(500 + index * 30, 30, 200);
        runMultiCourse(ctx, origin,
                new BlockPos(0, 0, 0),
                new BlockPos(0, 0, -(gap1 + 1)),
                new BlockPos(0, 0, -(gap1 + 1) - (gap2 + 1)));
    }

    private void runCorner(TestContext ctx, int index, int gap1, int gap2) {
        BlockPos origin = new BlockPos(500 + index * 30, 30, 500);
        runMultiCourse(ctx, origin,
                new BlockPos(0, 0, 0),
                new BlockPos(0, 0, -(gap1 + 1)),
                new BlockPos(-(gap2 + 1), 0, -(gap1 + 1)));
    }

    private void runMultiCourse(TestContext ctx, BlockPos origin, BlockPos... relPositions) {
        BlockPos[] world = new BlockPos[relPositions.length];
        for (int i = 0; i < relPositions.length; i++) {
            world[i] = origin.offset(relPositions[i]);
        }
        BlockPos worldStart = world[0];
        BlockPos worldEnd = world[world.length - 1];

        LOGGER.info("Setting up multi-course: {} waypoints, start={}, end={}", world.length, worldStart, worldEnd);

        // Phase 0: Teleport to course area and wait for chunks
        BlockPos center = new BlockPos(
                (worldStart.getX() + worldEnd.getX()) / 2,
                origin.getY() + 5,
                (worldStart.getZ() + worldEnd.getZ()) / 2);
        ctx.runCommand("tp @s " + (center.getX() + 0.5) + " " + center.getY() + " " + (center.getZ() + 0.5));

        ctx.waitFor(mc -> {
            boolean chunksLoaded = true;
            for (BlockPos pos : world) {
                if (!mc.level.hasChunkAt(pos)) { chunksLoaded = false; break; }
            }
            boolean playerNearCourse = Math.abs(mc.player.position().y - (origin.getY() + 5)) < 3
                    && horizontalDistance(mc.player.position(), center.getX() + 0.5, center.getZ() + 0.5) < 20;
            return chunksLoaded && playerNearCourse;
        });

        // Phase 1: Build course, teleport to start, wait for on-ground
        buildMultiCourse(ctx, world);
        ctx.runCommand("tp @s "
                + (worldStart.getX() + 0.5) + " " + (worldStart.getY() + 1) + " " + (worldStart.getZ() + 0.5));

        ctx.waitFor(mc -> {
            boolean nearStart = horizontalDistance(mc.player.position(),
                    worldStart.getX() + 0.5, worldStart.getZ() + 0.5) < 1.0;
            return mc.player.onGround() && nearStart;
        });

        // Phase 2: Start PathWalker and monitor
        LOGGER.info("Starting PathWalker with {} nodes", world.length);
        ctx.runOnClient(mc -> {
            List<MeshNode> path = new ArrayList<>();
            for (BlockPos pos : world) {
                path.add(new MeshNode(pos.getX(), pos.getY(), pos.getZ()));
            }
            PathWalker.start(path);
        });

        int minY = Integer.MAX_VALUE;
        for (BlockPos pos : world) {
            minY = Math.min(minY, pos.getY());
        }
        int finalMinY = minY;
        ctx.waitFor(mc -> {
            double playerY = mc.player.position().y;
            if (playerY < finalMinY - 2) {
                PathWalker.stop();
                throw new AssertionError("Player fell at " + mc.player.blockPosition()
                        + " (y=" + String.format("%.2f", playerY) + ")");
            }

            double hDist = horizontalDistance(mc.player.position(),
                    worldEnd.getX() + 0.5, worldEnd.getZ() + 0.5);
            if (mc.player.onGround() && hDist < ARRIVAL_RADIUS) {
                PathWalker.stop();
                LOGGER.info("PathWalker reached goal (distance: {}, onGround: true)",
                        String.format("%.2f", hDist));
                return true;
            }

            if (!PathWalker.isActive()) {
                throw new AssertionError("PathWalker stopped but player not at goal, distance="
                        + String.format("%.2f", hDist) + ", pos=" + mc.player.blockPosition());
            }

            return false;
        });
    }

    private void buildMultiCourse(TestContext ctx, BlockPos[] positions) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos pos : positions) {
            minX = Math.min(minX, pos.getX()); minY = Math.min(minY, pos.getY()); minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX()); maxY = Math.max(maxY, pos.getY()); maxZ = Math.max(maxZ, pos.getZ());
        }

        ctx.runCommand("fill "
                + (minX - CLEAR_RADIUS) + " " + (minY - CLEAR_RADIUS) + " " + (minZ - CLEAR_RADIUS) + " "
                + (maxX + CLEAR_RADIUS) + " " + (maxY + CLEAR_RADIUS) + " " + (maxZ + CLEAR_RADIUS) + " air");
        for (BlockPos pos : positions) {
            ctx.runCommand("setblock " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + " stone");
        }

        LOGGER.info("Built multi-course: {} blocks placed, cleared ({},{},{}) to ({},{},{})",
                positions.length,
                minX - CLEAR_RADIUS, minY - CLEAR_RADIUS, minZ - CLEAR_RADIUS,
                maxX + CLEAR_RADIUS, maxY + CLEAR_RADIUS, maxZ + CLEAR_RADIUS);
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
