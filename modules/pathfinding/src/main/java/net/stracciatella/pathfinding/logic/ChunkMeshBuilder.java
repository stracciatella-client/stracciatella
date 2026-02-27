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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
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
    private static final int MAX_DIAGONAL_SEARCH = 3;
    private static final int[][] DIRECTION_VECTORS = buildDirectionVectors(MAX_HORIZONTAL_SEARCH);

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


        for (MeshNode node : nodes) {
            List<Neighbor> neighbors = new ArrayList<>();

            // add all neighbors in all directions on same y
            for (int[] direction : DIRECTION_VECTORS) {
                int dx = direction[0];
                int dz = direction[1];
                int maxSteps = maxStepsForDirection(dx, dz, MAX_HORIZONTAL_SEARCH);
                BlockPos.MutableBlockPos targetPos = new BlockPos.MutableBlockPos(node.getX(), node.getY(), node.getZ());

                for (int i = 0; i < maxSteps; i++) {
                    targetPos.move(dx, 0, dz);

                    if (nodeMap.containsKey(targetPos) && isBlockReachable(chunk, node.getBlockPos(), targetPos)) {
                        neighbors.add(new Neighbor(nodeMap.get(targetPos), movementCost(dx, dz)));
                        break;
                    } else if (nodeMap.containsKey(targetPos)) {
                        break;
                    }
                }
            }

            // check for all neighbors on y+1
            for (int[] direction : DIRECTION_VECTORS) {
                int dx = direction[0];
                int dz = direction[1];
                int maxSteps = maxStepsForDirection(dx, dz, MAX_UP_SEARCH);
                BlockPos.MutableBlockPos targetPos = new BlockPos.MutableBlockPos(node.getX(), node.getY() + 1, node.getZ());
                for (int i = 0; i < maxSteps; i++) {
                    targetPos.move(dx, 0, dz);

                    if (nodeMap.containsKey(targetPos) && isBlockReachable(chunk, node.getBlockPos(), targetPos)) {
                        neighbors.add(new Neighbor(nodeMap.get(targetPos), movementCost(dx, dz)));
                        break;
                    } else if (nodeMap.containsKey(targetPos)) {
                        break;
                    }
                }
            }

            // temporary fix so we can only drop down up to MAX_DROP blocks at a time todo extend this logic
            for (int drop = 1; drop <= MAX_DROP; drop++) {
                for (int[] direction : DIRECTION_VECTORS) {
                    int dx = direction[0];
                    int dz = direction[1];
                    int maxSteps = maxStepsForDirection(dx, dz, MAX_DOWN_SEARCH);
                    BlockPos.MutableBlockPos targetPos = new BlockPos.MutableBlockPos(node.getX(), node.getY() - drop, node.getZ());
                    for (int i = 0; i < maxSteps; i++) {
                        targetPos.move(dx, 0, dz);

                        if (nodeMap.containsKey(targetPos) && isBlockReachable(chunk, node.getBlockPos(), targetPos)) {
                            neighbors.add(new Neighbor(nodeMap.get(targetPos), movementCost(dx, dz)));
                            break;
                        } else if (nodeMap.containsKey(targetPos)) {
                            break;
                        }
                    }
                }
            }

            node.setNeighbors(neighbors);
        }

        return newMesh;
    }


    private boolean isBlockReachable(ChunkAccess chunk, BlockPos source, BlockPos target) {
        // 1. is block a node?
        // 2. is block reachable?
        // 2.1. air between source and target?
        // todo implement rest

        int dy = source.getY() - target.getY();
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
                        if (!isColumnClear(chunk, pos)) {
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
                        if (!isColumnClear(chunk, pos)) {
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
                if (!isLineReachable(chunk, source, target)) {
                    return false;
                }
            }

        }
        // return chunk.getBlockState(pos).isAir();
        return true;
    }

    private boolean isLineReachable(ChunkAccess chunk, BlockPos source, BlockPos target) {
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
            if (!isColumnClear(chunk, pos)) {
                return false;
            }

            if (x != prevX && z != prevZ) {
                BlockPos.MutableBlockPos sideX = new BlockPos.MutableBlockPos(x, source.getY(), prevZ);
                if (!isColumnClear(chunk, sideX)) {
                    return false;
                }
                BlockPos.MutableBlockPos sideZ = new BlockPos.MutableBlockPos(prevX, source.getY(), z);
                if (!isColumnClear(chunk, sideZ)) {
                    return false;
                }
            }
        }

        return true;
    }

    private static int movementCost(int dx, int dz) {
        if (dx != 0 && dz != 0) {
            return 2;
        }
        return 1;
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

    private boolean isColumnClear(ChunkAccess chunk, BlockPos pos) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(pos.getX(), pos.getY(), pos.getZ());
        for (int i = 0; i < 3; i++) {
            cursor.move(0, 1, 0);
            if (!chunk.getBlockState(cursor).isAir()) {
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

}
