package net.stracciatella.pathfinding.logic.mesh;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.commands.arguments.coordinates.WorldCoordinate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.DensityFunctions;

public class MeshNode {
    List<Neighbor> neighbors = new ArrayList<>();
    int x;
    int y;
    int z;

    public MeshNode(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public List<Neighbor> getNeighbors() {
        return neighbors;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public void setNeighbors(List<Neighbor> neighbors) {
        this.neighbors = neighbors;
    }

    public BlockPos getBlockPos() {
        return new BlockPos(x, y, z);
    }
}
