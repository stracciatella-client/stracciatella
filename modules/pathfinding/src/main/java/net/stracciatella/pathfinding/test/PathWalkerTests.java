package net.stracciatella.pathfinding.test;

import net.minecraft.core.BlockPos;
import net.stracciatella.pathfinding.logic.PathWalker;
import net.stracciatella.pathfinding.logic.mesh.MeshNode;
import net.stracciatella.testing.api.MinecraftTest;
import net.stracciatella.testing.api.TestContext;
import net.stracciatella.testing.api.TestSuite;
import net.stracciatella.testing.api.TickHandler;
import net.stracciatella.testing.command.CommandExecutor;
import net.stracciatella.testing.movement.MovementTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests for the PathWalker. Each test defines a parkour course as relative block
 * positions, which are placed in the air at a unique world location. The test
 * clears a 5-block radius around all blocks, places the solid blocks, teleports
 * the player to the start, and runs the PathWalker to the end.
 *
 * Phase flow:
 *   Phase 0: Teleport to course center (loads chunks), wait 5 ticks
 *   Phase 1: Clear area + place blocks + teleport to start, wait 10 ticks
 *   Phase 2: Start PathWalker, monitor for arrival or fall
 *
 * Order numbers are high (100+) to avoid interleaving with other test suites.
 */
@TestSuite(name = "PathWalker Tests")
public class PathWalkerTests implements TickHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("PathWalkerTests");
    private static final int CLEAR_RADIUS = 5;

    private MovementTracker tracker;
    private TestContext activeCtx;
    private BlockPos worldStart;
    private BlockPos worldEnd;
    private List<BlockPos> worldBlocks;
    private List<MeshNode> path;
    private int phase;
    private int waitTicks;

    // Each test uses a different X offset. Y=30 puts courses well above the superflat ground.
    // All positions in the test methods are relative (0,0,0 is the start block).
    // Platforms are 3 blocks wide (x-1, x, x+1) for safety; the path follows the center.

    @MinecraftTest(name = "PathWalker 1-block gap", timeoutTicks = 600, order = -100)
    public void gap1(TestContext ctx) {
        // 3 walk blocks, 1 air gap, 3 walk blocks — heading north (-Z)
        runCourse(ctx, new BlockPos(100, 30, 100),
                new BlockPos(0, 0, 0),   // start
                new BlockPos(0, 0, -6),  // end
                new BlockPos[]{
                        // platform before gap (3 wide)
                        bp(-1, 0, 0), bp(0, 0, 0), bp(1, 0, 0),
                        bp(-1, 0, -1), bp(0, 0, -1), bp(1, 0, -1),
                        bp(-1, 0, -2), bp(0, 0, -2), bp(1, 0, -2),
                        // 1 air gap at z=-3
                        // platform after gap (3 wide)
                        bp(-1, 0, -4), bp(0, 0, -4), bp(1, 0, -4),
                        bp(-1, 0, -5), bp(0, 0, -5), bp(1, 0, -5),
                        bp(-1, 0, -6), bp(0, 0, -6), bp(1, 0, -6),
                },
                // path follows center (x=0)
                new BlockPos[]{
                        bp(0, 0, 0), bp(0, 0, -1), bp(0, 0, -2),
                        bp(0, 0, -4), bp(0, 0, -5), bp(0, 0, -6),
                });
    }

    @MinecraftTest(name = "PathWalker 2-block gap", timeoutTicks = 600, order = -99)
    public void gap2(TestContext ctx) {
        runCourse(ctx, new BlockPos(150, 30, 100),
                new BlockPos(0, 0, 0),
                new BlockPos(0, 0, -7),
                new BlockPos[]{
                        bp(-1, 0, 0), bp(0, 0, 0), bp(1, 0, 0),
                        bp(-1, 0, -1), bp(0, 0, -1), bp(1, 0, -1),
                        bp(-1, 0, -2), bp(0, 0, -2), bp(1, 0, -2),
                        // 2 air gap at z=-3, z=-4
                        bp(-1, 0, -5), bp(0, 0, -5), bp(1, 0, -5),
                        bp(-1, 0, -6), bp(0, 0, -6), bp(1, 0, -6),
                        bp(-1, 0, -7), bp(0, 0, -7), bp(1, 0, -7),
                },
                new BlockPos[]{
                        bp(0, 0, 0), bp(0, 0, -1), bp(0, 0, -2),
                        bp(0, 0, -5), bp(0, 0, -6), bp(0, 0, -7),
                });
    }

    @MinecraftTest(name = "PathWalker 3-block gap", timeoutTicks = 800, order = -98)
    public void gap3(TestContext ctx) {
        runCourse(ctx, new BlockPos(200, 30, 100),
                new BlockPos(0, 0, 0),
                new BlockPos(0, 0, -8),
                new BlockPos[]{
                        bp(-1, 0, 0), bp(0, 0, 0), bp(1, 0, 0),
                        bp(-1, 0, -1), bp(0, 0, -1), bp(1, 0, -1),
                        bp(-1, 0, -2), bp(0, 0, -2), bp(1, 0, -2),
                        // 3 air gap at z=-3, z=-4, z=-5
                        bp(-1, 0, -6), bp(0, 0, -6), bp(1, 0, -6),
                        bp(-1, 0, -7), bp(0, 0, -7), bp(1, 0, -7),
                        bp(-1, 0, -8), bp(0, 0, -8), bp(1, 0, -8),
                },
                new BlockPos[]{
                        bp(0, 0, 0), bp(0, 0, -1), bp(0, 0, -2),
                        bp(0, 0, -6), bp(0, 0, -7), bp(0, 0, -8),
                });
    }

    /**
     * Sets up and runs a parkour course test.
     *
     * @param ctx        test context
     * @param origin     world position where relative (0,0,0) maps to
     * @param relStart   relative start position (player teleports on top of this block)
     * @param relEnd     relative end position (PathWalker target)
     * @param relBlocks  relative positions of all solid blocks to place
     * @param relPath    relative positions of the PathWalker path nodes (center line)
     */
    private void runCourse(TestContext ctx, BlockPos origin, BlockPos relStart, BlockPos relEnd,
                           BlockPos[] relBlocks, BlockPos[] relPath) {
        activeCtx = ctx;
        tracker = new MovementTracker();
        waitTicks = 0;
        phase = 0;

        worldStart = origin.offset(relStart);
        worldEnd = origin.offset(relEnd);

        worldBlocks = new ArrayList<>();
        for (BlockPos rel : relBlocks) {
            worldBlocks.add(origin.offset(rel));
        }

        path = new ArrayList<>();
        for (BlockPos rel : relPath) {
            BlockPos world = origin.offset(rel);
            path.add(new MeshNode(world.getX(), world.getY(), world.getZ()));
        }

        LOGGER.info("Setting up course at {} with {} blocks, path length {}",
                origin, worldBlocks.size(), path.size());

        // Teleport to course center to load chunks
        BlockPos center = origin.offset(relEnd.getX() / 2, 0, relEnd.getZ() / 2);
        CommandExecutor.executeCommand("tp @s " + center.getX() + " " + (origin.getY() + 5) + " " + center.getZ());
    }

    @Override
    public void onTick(TestContext ctx) {
        if (activeCtx == null || activeCtx.isFinished()) return;
        if (ctx.player() == null) return;

        tracker.recordTick(ctx.player());
        waitTicks++;

        // Phase 0: wait for chunks to load
        if (phase == 0 && waitTicks >= 5) {
            buildCourse();
            // Teleport player on top of start block
            CommandExecutor.executeCommand("tp @s "
                    + worldStart.getX() + " " + (worldStart.getY() + 1) + " " + worldStart.getZ());
            waitTicks = 0;
            phase = 1;
        }

        // Phase 1: wait for player to settle on the block
        else if (phase == 1 && waitTicks >= 10) {
            LOGGER.info("Starting PathWalker, player at {}", ctx.playerBlockPos());
            PathWalker.start(path);
            phase = 2;
        }

        // Phase 2: monitor PathWalker
        else if (phase == 2) {
            if (checkFall(ctx)) return;
            if (!PathWalker.isActive()) {
                // Check arrival — target is at feet level (block above the solid end block)
                BlockPos feetTarget = worldEnd.above();
                if (tracker.isAtBlock(feetTarget)) {
                    LOGGER.info("PathWalker reached target {}", feetTarget);
                    activeCtx.complete();
                } else {
                    activeCtx.fail("PathWalker stopped but player not at target " + feetTarget
                            + ", actual: " + ctx.playerBlockPos());
                }
            }
        }
    }

    /** Clears a 5-block radius around all course blocks, then places the solid blocks. */
    private void buildCourse() {
        // Find bounding box of all blocks
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos b : worldBlocks) {
            minX = Math.min(minX, b.getX());
            minY = Math.min(minY, b.getY());
            minZ = Math.min(minZ, b.getZ());
            maxX = Math.max(maxX, b.getX());
            maxY = Math.max(maxY, b.getY());
            maxZ = Math.max(maxZ, b.getZ());
        }

        // Clear area: bounding box expanded by CLEAR_RADIUS in all directions
        fill(minX - CLEAR_RADIUS, minY - CLEAR_RADIUS, minZ - CLEAR_RADIUS,
                maxX + CLEAR_RADIUS, maxY + CLEAR_RADIUS, maxZ + CLEAR_RADIUS, "air");

        // Place solid blocks
        for (BlockPos b : worldBlocks) {
            CommandExecutor.executeCommand("setblock " + b.getX() + " " + b.getY() + " " + b.getZ() + " stone");
        }

        LOGGER.info("Built course: {} blocks placed, cleared ({},{},{}) to ({},{},{})",
                worldBlocks.size(),
                minX - CLEAR_RADIUS, minY - CLEAR_RADIUS, minZ - CLEAR_RADIUS,
                maxX + CLEAR_RADIUS, maxY + CLEAR_RADIUS, maxZ + CLEAR_RADIUS);
    }

    private boolean checkFall(TestContext ctx) {
        double playerY = ctx.player().position().y;
        int minY = worldBlocks.stream().mapToInt(BlockPos::getY).min().orElse(0);
        if (playerY < minY - 2) {
            PathWalker.stop();
            activeCtx.fail("Player fell at " + ctx.playerBlockPos()
                    + " (y=" + String.format("%.2f", playerY) + ")");
            return true;
        }
        return false;
    }

    private static void fill(int x1, int y1, int z1, int x2, int y2, int z2, String block) {
        CommandExecutor.executeCommand("fill " + x1 + " " + y1 + " " + z1
                + " " + x2 + " " + y2 + " " + z2 + " " + block);
    }

    private static BlockPos bp(int x, int y, int z) {
        return new BlockPos(x, y, z);
    }
}
