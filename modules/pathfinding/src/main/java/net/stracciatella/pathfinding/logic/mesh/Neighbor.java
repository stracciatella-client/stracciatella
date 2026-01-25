package net.stracciatella.pathfinding.logic.mesh;

public class Neighbor {
    MeshNode node;
    int cost;

    public Neighbor(MeshNode node, int cost) {
        this.node = node;
        this.cost = cost;
    }

    public int getCost() {
        return cost;
    }

    public MeshNode getNode() {
        return node;
    }
}
