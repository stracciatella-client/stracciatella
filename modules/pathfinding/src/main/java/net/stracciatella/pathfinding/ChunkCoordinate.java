package net.stracciatella.pathfinding;

import net.minecraft.world.level.chunk.ChunkAccess;

public record ChunkCoordinate(int x, int z) {
    public ChunkCoordinate(ChunkAccess chunk) {
        this(chunk.getPos().x, chunk.getPos().z);
    }
}
