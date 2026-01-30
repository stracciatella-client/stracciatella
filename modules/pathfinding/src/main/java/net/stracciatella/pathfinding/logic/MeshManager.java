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
            // todo adjacent and diagonal as well
        });
    }

    public static void generateMesh(ChunkAccess chunk, Entity entity) {

        Mesh mesh = meshBuilder.generatePathfindingMesh(chunk, entity);
        // todo adjacent and diagonal as well
        if (!meshes.containsKey(entity)) {
            meshes.put(entity, new HashMap<>());
        }

        meshes.get(entity).put(new ChunkCoordinate(chunk), mesh);

    }

}


