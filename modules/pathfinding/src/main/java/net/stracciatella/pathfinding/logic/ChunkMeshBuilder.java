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


        int[][] cardinalDirections = {{1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}};
        int[][] diagonalDirections = {{1, 0, 1}, {1, 0, -1}, {-1, 0, 1}, {-1, 0, -1}};

        for (MeshNode node : nodes) {
            List<Neighbor> neighbors = new ArrayList<>();

            // add all straight neighbors
            for (int[] cardinalDirection : cardinalDirections) {

                BlockPos.MutableBlockPos targetPos = new BlockPos.MutableBlockPos(node.getX(), node.getY(), node.getZ());

                for (int i = 0; i < 5; i++) {
                    targetPos.move(cardinalDirection[0], cardinalDirection[1], cardinalDirection[2]);

                    if (nodeMap.containsKey(targetPos) && isBlockReachable(chunk, node.getBlockPos(), targetPos)) {
                        neighbors.add(new Neighbor(nodeMap.get(targetPos), 1));
                        break;
                    } else if (nodeMap.containsKey(targetPos)) {
                        break;
                    }
                }

                // check for all straight neighbors on y+1
                targetPos = new BlockPos.MutableBlockPos(node.getX(), node.getY() + 1, node.getZ());
                for (int i = 0; i < 3; i++) {
                    targetPos.move(cardinalDirection[0], cardinalDirection[1], cardinalDirection[2]);

                    if (nodeMap.containsKey(targetPos) && isBlockReachable(chunk, node.getBlockPos(), targetPos)) {
                        neighbors.add(new Neighbor(nodeMap.get(targetPos), 1));
                        break;
                    } else if (nodeMap.containsKey(targetPos)) {
                        break;
                    }
                }

                // temporary fix so we can only drop down 1 block at a time todo extend this logic
                targetPos = new BlockPos.MutableBlockPos(node.getX(), node.getY() - 1, node.getZ());
                for (int i = 0; i < 5; i++) {
                    targetPos.move(cardinalDirection[0], cardinalDirection[1], cardinalDirection[2]);

                    if (nodeMap.containsKey(targetPos) && isBlockReachable(chunk, node.getBlockPos(), targetPos)) {
                        neighbors.add(new Neighbor(nodeMap.get(targetPos), 1));
                        break;
                    } else if (nodeMap.containsKey(targetPos)) {
                        break;
                    }
                }
            }

            // add all diagonal neighbors
            for (int[] diagonalDirection : diagonalDirections) {

                BlockPos.MutableBlockPos targetPos = new BlockPos.MutableBlockPos(node.getX(), node.getY(), node.getZ());

                for (int i = 0; i < 5; i++) {
                    targetPos.move(diagonalDirection[0], diagonalDirection[1], diagonalDirection[2]);

                    if (nodeMap.containsKey(targetPos) && isBlockReachable(chunk, node.getBlockPos(), targetPos)) {
                        neighbors.add(new Neighbor(nodeMap.get(targetPos), 2));
                        break;
                    } else if (nodeMap.containsKey(targetPos)) {
                        break;
                    }
                }

                // check for all diagonal neighbors on y+1
                targetPos = new BlockPos.MutableBlockPos(node.getX(), node.getY() + 1, node.getZ());
                for (int i = 0; i < 3; i++) {
                    targetPos.move(diagonalDirection[0], diagonalDirection[1], diagonalDirection[2]);

                    if (nodeMap.containsKey(targetPos) && isBlockReachable(chunk, node.getBlockPos(), targetPos)) {
                        neighbors.add(new Neighbor(nodeMap.get(targetPos), 2));
                        break;
                    } else if (nodeMap.containsKey(targetPos)) {
                        break;
                    }
                }

                // temporary fix so we can only drop down 1 block at a time todo extend this logic
                targetPos = new BlockPos.MutableBlockPos(node.getX(), node.getY() - 1, node.getZ());
                for (int i = 0; i < 5; i++) {
                    targetPos.move(diagonalDirection[0], diagonalDirection[1], diagonalDirection[2]);

                    if (nodeMap.containsKey(targetPos) && isBlockReachable(chunk, node.getBlockPos(), targetPos)) {
                        neighbors.add(new Neighbor(nodeMap.get(targetPos), 2));
                        break;
                    } else if (nodeMap.containsKey(targetPos)) {
                        break;
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

        if (source.getY() == target.getY() || source.getY() == target.getY() + 1 || source.getY() == target.getY() - 1) {
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
                if (!isDiagonalReachable(chunk, source, target)) {
                    return false;
                }
            }

        }
        // return chunk.getBlockState(pos).isAir();
        return true;
    }

    private boolean isDiagonalReachable(ChunkAccess chunk, BlockPos source, BlockPos target) {
        int dx = target.getX() - source.getX();
        int dz = target.getZ() - source.getZ();

        if (Math.abs(dx) != Math.abs(dz)) {
            return false;
        }

        int stepX = Integer.compare(dx, 0);
        int stepZ = Integer.compare(dz, 0);

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(source.getX(), source.getY(), source.getZ());
        while (pos.getX() != target.getX() || pos.getZ() != target.getZ()) {
            pos.move(stepX, 0, stepZ);

            if (!isColumnClear(chunk, pos)) {
                return false;
            }

            // Avoid cutting corners by checking the adjacent cardinal tiles
            BlockPos.MutableBlockPos sideX = new BlockPos.MutableBlockPos(pos.getX(), pos.getY(), pos.getZ() - stepZ);
            if (!isColumnClear(chunk, sideX)) {
                return false;
            }

            BlockPos.MutableBlockPos sideZ = new BlockPos.MutableBlockPos(pos.getX() - stepX, pos.getY(), pos.getZ());
            if (!isColumnClear(chunk, sideZ)) {
                return false;
            }
        }

        return true;
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
