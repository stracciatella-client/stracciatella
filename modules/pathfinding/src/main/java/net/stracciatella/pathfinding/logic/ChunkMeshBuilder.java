package net.stracciatella.pathfinding.logic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.stracciatella.pathfinding.ChunkCoordinate;
import net.stracciatella.pathfinding.display.PathDisplay;
import net.stracciatella.pathfinding.logic.mesh.IMeshProvider;
import net.stracciatella.pathfinding.logic.mesh.Mesh;
import net.stracciatella.pathfinding.logic.mesh.MeshNode;
import net.stracciatella.pathfinding.logic.mesh.Neighbor;


public class ChunkMeshBuilder {

    private static final int MAX_HORIZONTAL_SEARCH = 5;
    private static final int MAX_UP_SEARCH = 3;
    private static final int MAX_DOWN_SEARCH = 5;
    private static final int MAX_DROP = 3;
    private static final int MAX_DIAGONAL_SEARCH = 5;
    private static final int[][] DIRECTION_VECTORS = buildDirectionVectors(1);

    public Mesh generatePathfindingMesh(ChunkAccess chunk, Entity entity) {
        Mesh newMesh = new Mesh();
        // Temporäre Map für schnellen Zugriff beim Verknüpfen der Nachbarn
        Map<BlockPos, MeshNode> nodeMap = new HashMap<>();
        List<MeshNode> nodes = new ArrayList<>();

        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();

        int maxY = calculateMaxChunkY(chunk);
        int minY = calculateMinChunkY(chunk);

        BlockPos.MutableBlockPos baseBlock = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos oneAbove = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos twoAbove = new BlockPos.MutableBlockPos();

        // 1. SCHRITT: KNOTEN ERSTELLEN
        // Wir iterieren durch den Chunk (0-15 x, 0-15 z)
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                // Wir iterieren von oben nach unten oder unten nach oben
                for (int y = minY; y < maxY - 2; y++) {
                    baseBlock.set(minX + x, y, minZ + z);
                    oneAbove.set(minX + x, y + 1, minZ + z);
                    twoAbove.set(minX + x, y + 2, minZ + z);

                    BlockState baseBlockState = chunk.getBlockState(baseBlock);
                    if (baseBlockState.entityCanStandOn(Minecraft.getInstance().level, baseBlock.immutable(), entity)) {
                        BlockState oneAboveState = chunk.getBlockState(oneAbove);
                        BlockState twoAboveState = chunk.getBlockState(twoAbove);

                        if (oneAboveState.isAir() && twoAboveState.isAir()) {

                            // Node erstellen mit globalen Koordinaten
                            MeshNode node = new MeshNode(baseBlock.getX(), baseBlock.getY(), baseBlock.getZ());
                            nodes.add(node);
                            newMesh.getNodes().put(baseBlock.immutable(), node);

                            // In Map speichern (immutable Key für HashMap wichtig)
                            nodeMap.put(baseBlock.immutable(), node);

                        }
                    }

                }
            }
        }


        rebuildNeighbors(chunk::getBlockState, nodeMap, nodes);

        return newMesh;
    }

    public void reconnectBorderNodes(Level level, ChunkCoordinate chunkA, Mesh meshA, ChunkCoordinate chunkB, Mesh meshB) {
        if (level == null || meshA == null || meshB == null) {
            return;
        }
        int dx = chunkB.x() - chunkA.x();
        int dz = chunkB.z() - chunkA.z();
        if (dx == 0 && dz == 0) {
            return;
        }
        if (Math.abs(dx) > 1 || Math.abs(dz) > 1) {
            return;
        }

        int borderRange = Math.max(MAX_HORIZONTAL_SEARCH, Math.max(MAX_UP_SEARCH, MAX_DOWN_SEARCH));
        List<MeshNode> borderA = collectBorderNodes(meshA, chunkA, dx, dz, borderRange);
        List<MeshNode> borderB = collectBorderNodes(meshB, chunkB, -dx, -dz, borderRange);
        if (borderA.isEmpty() && borderB.isEmpty()) {
            return;
        }

        Map<BlockPos, MeshNode> combined = new HashMap<>();
        combined.putAll(meshA.getNodes());
        combined.putAll(meshB.getNodes());

        if (!borderA.isEmpty()) {
            rebuildNeighbors(level::getBlockState, combined, borderA);
        }
        if (!borderB.isEmpty()) {
            rebuildNeighbors(level::getBlockState, combined, borderB);
        }
    }

    private List<MeshNode> collectBorderNodes(Mesh mesh, ChunkCoordinate chunk, int dx, int dz, int borderRange) {
        int minX = chunk.x() * 16;
        int maxX = minX + 15;
        int minZ = chunk.z() * 16;
        int maxZ = minZ + 15;

        List<MeshNode> result = new ArrayList<>();
        for (MeshNode node : mesh.getNodes().values()) {
            if (isWithinBorder(node, minX, maxX, minZ, maxZ, dx, dz, borderRange)) {
                result.add(node);
            }
        }
        return result;
    }

    private boolean isWithinBorder(MeshNode node, int minX, int maxX, int minZ, int maxZ, int dx, int dz, int borderRange) {
        boolean inX = true;
        boolean inZ = true;
        if (dx > 0) {
            inX = node.getX() >= maxX - borderRange;
        } else if (dx < 0) {
            inX = node.getX() <= minX + borderRange;
        }
        if (dz > 0) {
            inZ = node.getZ() >= maxZ - borderRange;
        } else if (dz < 0) {
            inZ = node.getZ() <= minZ + borderRange;
        }
        return inX && inZ;
    }

    private void rebuildNeighbors(BlockStateLookup lookup, Map<BlockPos, MeshNode> nodeMap, List<MeshNode> nodes) {
        for (MeshNode node : nodes) {
            List<Neighbor> neighbors = new ArrayList<>();

            // Check all positions within search radius (brute force approach for diagonal jumps)
            int searchRadius = Math.max(MAX_HORIZONTAL_SEARCH, Math.max(MAX_UP_SEARCH, MAX_DOWN_SEARCH));

            for (int dy = -MAX_DROP; dy <= MAX_UP_SEARCH; dy++) {
                for (int dx = -searchRadius; dx <= searchRadius; dx++) {
                    for (int dz = -searchRadius; dz <= searchRadius; dz++) {
                        if (dx == 0 && dz == 0 && dy == 0) continue;

                        BlockPos.MutableBlockPos targetPos = new BlockPos.MutableBlockPos(
                            node.getX() + dx, node.getY() + dy, node.getZ() + dz);

                        MeshNode candidate = nodeMap.get(targetPos);
                        if (candidate != null) {
                            if (isBlockReachable(lookup, node.getBlockPos(), targetPos)) {
                                neighbors.add(new Neighbor(candidate, movementCost(dx, dz, dy)));
                            }
                        }
                    }
                }
            }

            node.setNeighbors(neighbors);
        }
    }


    private boolean isBlockReachable(BlockStateLookup lookup, BlockPos source, BlockPos target) {
        // 1. is block a node?
        // 2. is block reachable?
        // 2.1. air between source and target?
        // todo implement rest

        // Check horizontal distance constraints for diagonal jumps
        int dx = Math.abs(target.getX() - source.getX());
        int dz = Math.abs(target.getZ() - source.getZ());
        int dy = source.getY() - target.getY();

        // Players can only jump up 1 block in Minecraft
        if (dy < -1) {
            return false;
        }

        // For diagonal movements, enforce realistic jump distance limits
        if (dx > 0 && dz > 0) {
            double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

            // For same level or step up, max diagonal distance is ~4.0 blocks (sprint jump)
            if (dy >= -1 && horizontalDistance > 4.0) {
                return false;
            }

            // For drops, allow slightly more horizontal distance but still limited
            if (dy > 0 && horizontalDistance > 4.5) {
                return false;
            }
        }

        if (dy == 0 || dy == -1 || (dy >= 1 && dy <= MAX_DROP)) {
            // close height
            //      air
            // air  air    air
            // air  air    air
            // solid  .... solid

            if (source.getX() == target.getX() || source.getZ() == target.getZ()) {
                // if its on the same axis we can easily check the blocks in a straight line

                if (source.getX() == target.getX()) {
                    BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(source.getX(), source.getY(), source.getZ());
                    boolean countUp = source.getZ() < target.getZ();
                    while (pos.getZ() != target.getZ()) {
                        if (!isColumnClear(lookup, pos)) {
                            return false;
                        }
                        if (countUp) {
                            pos.move(0, 0, 1);
                        } else {
                            pos.move(0, 0, -1);
                        }
                    }
                } else {
                    BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(source.getX(), source.getY(), source.getZ());
                    boolean countUp = source.getX() < target.getX();
                    while (pos.getX() != target.getX()) {
                        if (!isColumnClear(lookup, pos)) {
                            return false;
                        }
                        if (countUp) {
                            pos.move(1, 0, 0);
                        } else {
                            pos.move(-1, 0, 0);
                        }
                    }
                }

            } else {
                if (!isLineReachable(lookup, source, target)) {
                    return false;
                }
            }

        }
        // return chunk.getBlockState(pos).isAir();
        return true;
    }

    private boolean isLineReachable(BlockStateLookup lookup, BlockPos source, BlockPos target) {
        int x0 = source.getX();
        int z0 = source.getZ();
        int x1 = target.getX();
        int z1 = target.getZ();

        int dx = Math.abs(x1 - x0);
        int dz = Math.abs(z1 - z0);
        int sx = x0 < x1 ? 1 : -1;
        int sz = z0 < z1 ? 1 : -1;

        int err = dx - dz;
        int x = x0;
        int z = z0;

        while (x != x1 || z != z1) {
            int prevX = x;
            int prevZ = z;
            int e2 = 2 * err;
            if (e2 > -dz) {
                err -= dz;
                x += sx;
            }
            if (e2 < dx) {
                err += dx;
                z += sz;
            }

            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, source.getY(), z);
            if (!isColumnClear(lookup, pos)) {
                return false;
            }

            if (x != prevX && z != prevZ) {
                BlockPos.MutableBlockPos sideX = new BlockPos.MutableBlockPos(x, source.getY(), prevZ);
                if (!isColumnClear(lookup, sideX)) {
                    return false;
                }
                BlockPos.MutableBlockPos sideZ = new BlockPos.MutableBlockPos(prevX, source.getY(), z);
                if (!isColumnClear(lookup, sideZ)) {
                    return false;
                }
            }
        }

        return true;
    }

    private static int movementCost(int dx, int dz, int dy) {
        int gap = Math.max(Math.abs(dx), Math.abs(dz));

        // Base cost scales steeply with horizontal gap so A* strongly prefers
        // walking (gap=1) over short jumps (gap=2-3) over long parkour (gap=4+).
        int base;
        if (gap <= 1)      base = 10;
        else if (gap == 2) base = 22;
        else if (gap == 3) base = 40;
        else if (gap == 4) base = 65;
        else               base = 100;

        // dy > 0 means target is higher (requires jumping up — harder).
        // dy < 0 means target is lower (drop — easier than jumping up).
        int heightCost = dy > 0 ? dy * 5 : Math.abs(dy) * 2;

        int diagonal = (dx != 0 && dz != 0) ? 3 : 0;

        return base + heightCost + diagonal;
    }

    private static int[][] buildDirectionVectors(int maxComponent) {
        List<int[]> directions = new ArrayList<>();
        for (int dx = -maxComponent; dx <= maxComponent; dx++) {
            for (int dz = -maxComponent; dz <= maxComponent; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                int g = gcd(Math.abs(dx), Math.abs(dz));
                if (g == 1) {
                    directions.add(new int[]{dx, dz});
                }
            }
        }
        return directions.toArray(new int[0][0]);
    }

    private static int gcd(int a, int b) {
        if (a == 0) {
            return b;
        }
        while (b != 0) {
            int t = a % b;
            a = b;
            b = t;
        }
        return a;
    }

    private static int maxStepsForDirection(int dx, int dz, int baseMax) {
        if (dx == 0 || dz == 0) {
            return baseMax;
        }
        return Math.min(baseMax, MAX_DIAGONAL_SEARCH);
    }

    private boolean isColumnClear(BlockStateLookup lookup, BlockPos pos) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(pos.getX(), pos.getY(), pos.getZ());
        for (int i = 0; i < 3; i++) {
            cursor.move(0, 1, 0);
            if (!lookup.getBlockState(cursor).isAir()) {
                return false;
            }
        }
        return true;
    }


    private int calculateMinChunkY(ChunkAccess chunk) {
        int i = 0;
        while (chunk.isInsideBuildHeight(i)) {
            i = i - 1;
        }
        return i + 1;
    }


    private int calculateMaxChunkY(ChunkAccess chunk) {
        int i = 0;
        while (chunk.isInsideBuildHeight(i)) {
            i++;
        }
        return i - 1;
    }

    private interface BlockStateLookup {
        BlockState getBlockState(BlockPos pos);
    }

}
