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
import org.spongepowered.asm.mixin.Unique;

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
                            newMesh.getNodes().add(node);

                            // In Map speichern (immutable Key für HashMap wichtig)
                            nodeMap.put(baseBlock.immutable(), node);

                        }
                    }

                }
            }
        }

        // 2. SCHRITT: NACHBARN VERKNÜPFEN
        // Wir gehen alle erstellten Nodes durch und schauen, ob sie Nachbarn haben
        int[][] directions = {{1, 0, 0}, {-1, 0, 0}, // Ost, West
                {0, 0, 1}, {0, 0, -1},  // Süd, Nord
                {1, 0, 1}, {1, 0, -1}, {-1, 0, 1}, {-1, 0, -1}
                // Optional: Diagonalen oder Sprünge (y+1) hier hinzufügen
        };
        int[][] cardinalDirections = {{1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}};

        for (MeshNode node : nodes) {
            List<Neighbor> neighbors = new ArrayList<>();
            // for (int[] dir : directions) {
            //     int nx = node.getX() + dir[0];
            //     int ny = node.getY() + dir[1];
            //     int nz = node.getZ() + dir[2];
            //
            //     BlockPos targetPos = new BlockPos(nx, ny, nz);
            //
            //     // Prüfen, ob an der Zielposition ein Node existiert
            //     if (nodeMap.containsKey(targetPos) && isBlockReachable(chunk, node.getBlockPos(), targetPos)) {
            //         MeshNode neighborNode = nodeMap.get(targetPos);
            //         // Kosten: 1 für gerade Bewegung.
            //         neighbors.add(new Neighbor(neighborNode, 1));
            //     } else {
            //         // ERWEITERTE LOGIK: Treppen / Sprünge
            //         // Prüfen wir y+1 (Springen) oder y-1 (Fallen)
            //         BlockPos jumpPos = targetPos.above();
            //         BlockPos fallPos = targetPos.below();
            //
            //         // todo add more complex cost logic here and expand supported jumps
            //         if (nodeMap.containsKey(jumpPos) && isBlockReachable(chunk, node.getBlockPos(), targetPos)) {
            //             neighbors.add(new Neighbor(nodeMap.get(jumpPos), 2)); // Höhere Kosten für Sprung
            //         } else if (nodeMap.containsKey(fallPos)) {
            //             neighbors.add(new Neighbor(nodeMap.get(fallPos), 1));
            //         } else {
            //             // todo add longer jump logic here
            //         }
            //     }
            // }

            //add all straight neighbors
            for (int[] cardinalDirection : cardinalDirections) {

                BlockPos.MutableBlockPos targetPos = new BlockPos.MutableBlockPos(node.getX(), node.getY(), node.getZ());

                for (int i = 0; i < 4; i++) {
                    targetPos.move(cardinalDirection[0], cardinalDirection[1], cardinalDirection[2]);

                    if (nodeMap.containsKey(targetPos) && isBlockReachable(chunk, node.getBlockPos(), targetPos)) {
                        neighbors.add(new Neighbor(nodeMap.get(targetPos), 1));
                        break;
                    } else if (nodeMap.containsKey(targetPos)) {
                        break;
                    }
                }

                //check for all straight neighbors on y+1
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
                //todo check for straight falls
            }


            node.setNeighbors(neighbors);
        }

        return newMesh;
    }

    private boolean isBlockReachable(ChunkAccess chunk, BlockPos source, BlockPos target) {
        // 1. is block a node?
        // 2. is block reachable?
        // 2.1. air between source and target?

        if (source.getY() == target.getY() || source.getY() == target.getY() + 1 || source.getY() == target.getY() - 1) {
            // close height
            //      air
            // air  air    air
            // air  air    air
            // solid  .... solid

            if (source.getX() == target.getX() || source.getZ() == target.getZ()) {
                // if its on the same axis we can easily check the blocks in a straight line

                if (source.getX() == target.getX()) {
                    // BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(source.getX(), source.getY(), source.getZ());
                    // while (pos.getZ() != target.getZ()) {
                    //     // check all 3 blocks above to see if they are air
                    //     for (int i = 0; i < 3; i++) {
                    //         pos.move(0, 1, 0);
                    //         if (!chunk.getBlockState(pos).isAir()) {
                    //             return false;
                    //         }
                    //     }
                    // }
                } else {

                }

            } else {
                // todo
                // check for all relevant block on the line
            }

        }
        // return chunk.getBlockState(pos).isAir();
        // todo move movement logic here
        return true;
    }

    @Unique
    private int calculateMinChunkY(ChunkAccess chunk) {
        int i = 0;
        while (chunk.isInsideBuildHeight(i)) {
            i = i - 1;
        }
        return i + 1;
    }

    @Unique
    private int calculateMaxChunkY(ChunkAccess chunk) {
        int i = 0;
        while (chunk.isInsideBuildHeight(i)) {
            i++;
        }
        return i - 1;
    }

}
