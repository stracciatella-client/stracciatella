package net.stracciatella.pathfinding.logic.mesh;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.minecraft.core.BlockPos;

public class Mesh {
    HashMap<BlockPos, MeshNode> nodes = new HashMap<>();

    public HashMap<BlockPos, MeshNode> getNodes() {
        return nodes;
    }
}
