package net.stracciatella.pathfinding.logic;

import java.util.HashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.stracciatella.pathfinding.ChunkCoordinate;
import net.stracciatella.pathfinding.logic.mesh.Mesh;

public class MeshManager {

    public static HashMap<Entity, HashMap<ChunkCoordinate, Mesh>> meshes = new HashMap<>();
    static ChunkMeshBuilder meshBuilder = new ChunkMeshBuilder();

    public static void invalidateMesh(ChunkCoordinate chunkCoordinate) {
        meshes.forEach((entity, meshes) -> {
            Mesh mesh = meshBuilder.generatePathfindingMesh(Minecraft.getInstance().level.getChunk(chunkCoordinate.x(), chunkCoordinate.z()), entity);
            meshes.put(chunkCoordinate, mesh);
            connectAdjacentMeshes(entity, chunkCoordinate);
        });
    }

    public static void generateMesh(ChunkAccess chunk, Entity entity) {

        Mesh mesh = meshBuilder.generatePathfindingMesh(chunk, entity);
        if (!meshes.containsKey(entity)) {
            meshes.put(entity, new HashMap<>());
        }

        ChunkCoordinate chunkCoordinate = new ChunkCoordinate(chunk);
        meshes.get(entity).put(chunkCoordinate, mesh);
        connectAdjacentMeshes(entity, chunkCoordinate);

    }

    private static void connectAdjacentMeshes(Entity entity, ChunkCoordinate chunkCoordinate) {
        var meshesForEntity = meshes.get(entity);
        if (meshesForEntity == null) {
            return;
        }
        Mesh center = meshesForEntity.get(chunkCoordinate);
        if (center == null) {
            return;
        }
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                ChunkCoordinate neighborCoord = new ChunkCoordinate(chunkCoordinate.x() + dx, chunkCoordinate.z() + dz);
                Mesh neighbor = meshesForEntity.get(neighborCoord);
                if (neighbor != null) {
                    meshBuilder.reconnectBorderNodes(level, chunkCoordinate, center, neighborCoord, neighbor);
                }
            }
        }
    }

}


