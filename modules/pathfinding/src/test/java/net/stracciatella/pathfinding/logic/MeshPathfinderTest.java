package net.stracciatella.pathfinding.logic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.stracciatella.pathfinding.logic.mesh.MeshNode;
import net.stracciatella.pathfinding.logic.mesh.Neighbor;
import org.junit.jupiter.api.Test;

public class MeshPathfinderTest {
    @Test
    public void testPathfindingWithVisualization() {
        // 1. SETUP: Create a 10x10 Grid Mesh
        // Map key is "x,z" string for easy lookup during graph building
        Map<String, MeshNode> grid = new HashMap<>();
        int width = 10;
        int depth = 10;

        // Create Nodes
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                // Assuming you added a constructor: new MeshNode(x, y, z)
                // If not, use setters here.
                MeshNode node = new MeshNode(x, 0, z);
                grid.put(x + "," + z, node);
            }
        }

        // Create "Walls" by removing specific nodes from the grid
        // This forces the pathfinder to go around.
        // Wall at x=5, from z=0 to z=7
        for (int z = 0; z <= 7; z++) {
            grid.remove("5," + z);
        }

        //manually added walls:
        Set<TestPos> walls = Set.of(new TestPos(4, 4), new TestPos(3, 4), new TestPos(2, 4));

        walls.forEach(pos -> grid.remove(pos.x + "," + pos.y));

        // Link Neighbors (Grid style: Up, Down, Left, Right)
        for (MeshNode node : grid.values()) {
            List<Neighbor> neighbors = new ArrayList<>();
            int[][] directions = {{0, 1}, {0, -1}, {1, 0}, {-1, 0}};

            for (int[] dir : directions) {
                int nx = node.getX() + dir[0];
                int nz = node.getZ() + dir[1];
                MeshNode neighborNode = grid.get(nx + "," + nz);

                if (neighborNode != null) {
                    // Assuming constructor: new Neighbor(node, cost)
                    // standard movement cost = 10
                    neighbors.add(new Neighbor(neighborNode, 10));
                }
            }
            // Assuming setter: node.setNeighbors(neighbors)
            node.setNeighbors(neighbors);
        }

        // 2. RUN: Pathfinding
        MeshNode start = grid.get("1,1");
        MeshNode end = grid.get("8,1");

        System.out.println("Finding path from (1,1) to (8,1)...");
        System.out.println("A wall exists at x=5 (z=0 to z=7)\n");

        MeshPathfinder pathfinder = new MeshPathfinder();
        List<MeshNode> path = pathfinder.findPath(start, end);

        // 3. VISUALIZE
        printGrid(width, depth, grid, path, start, end);

        // Assertions (JUnit)
        assert !path.isEmpty() : "Path should not be empty";
        assert path.get(0).equals(start) : "Path must start at Start Node";
        assert path.get(path.size() - 1).equals(end) : "Path must end at End Node";
    }

    private void printGrid(int width, int depth, Map<String, MeshNode> grid, List<MeshNode> path, MeshNode start, MeshNode end) {
        System.out.println("   --- MAP VISUALIZATION ---");
        System.out.print("  ");
        for (int x = 0; x < width; x++) System.out.print(x + " ");
        System.out.println();

        for (int z = 0; z < depth; z++) {
            System.out.print(z + " ");
            for (int x = 0; x < width; x++) {
                MeshNode node = grid.get(x + "," + z);

                if (node == null) {
                    System.out.print("# "); // Wall / Void
                } else if (node.equals(start)) {
                    System.out.print("S "); // Start
                } else if (node.equals(end)) {
                    System.out.print("E "); // End
                } else if (path.contains(node)) {
                    System.out.print("* "); // Path
                } else {
                    System.out.print(". "); // Empty Node
                }
            }
            System.out.println();
        }
        System.out.println("\nLegend: [S]tart, [E]nd, [*] Path, [.] Node, [#] Wall");
    }

    record TestPos(int x, int y) {}
}
