package net.stracciatella.core.macro;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.*;

public class HumanizedPathfinder{

    // GEÄNDERT: World ist jetzt Level
    private final Level level;
    private final BlockPos startPos;
    private final BlockPos endPos;

    private final Random random = new Random();
    private final double imperfectionFactor;
    private final double overshootChance;

    public HumanizedPathfinder(BlockPos start, BlockPos end, double imperfectionFactor, double overshootChance) {
        // GEÄNDERT: MinecraftClient -> Minecraft, .world -> .level
        this.level = Minecraft.getInstance().level;
        this.startPos = start;
        this.endPos = end;
        this.imperfectionFactor = imperfectionFactor;
        this.overshootChance = overshootChance;
    }

    public List<BlockPos> findPath() {
        if (this.level == null) {
            return null; // Level nicht geladen
        }

        List<BlockPos> basePath = findBasePath();

        if (basePath == null || basePath.isEmpty()) {
            return null;
        }

        return postProcessPath(basePath);
    }

    private List<BlockPos> findBasePath() {
        Node startNode = new Node(startPos, null, 0, getHeuristic(startPos));
        List<Node> openSet = new ArrayList<>();
        Set<BlockPos> closedSet = new HashSet<>();
        openSet.add(startNode);

        while (!openSet.isEmpty()) {
            Node currentNode = openSet.stream().min(Comparator.comparingDouble(n -> n.fCost)).orElse(null);

            openSet.remove(currentNode);
            closedSet.add(currentNode.position);

            if (currentNode.position.equals(endPos)) {
                return reconstructPath(currentNode);
            }

            for (Node neighbor : getNeighbors(currentNode)) {
                if (closedSet.contains(neighbor.position) || !isWalkable(neighbor.position)) {
                    continue;
                }

                // GEÄNDERT: getSquaredDistance -> distSqr
                double tentativeGCost = currentNode.gCost + getDistance(currentNode.position, neighbor.position);

                Optional<Node> existingNeighbor = openSet.stream().filter(n -> n.equals(neighbor)).findFirst();
                if (existingNeighbor.isPresent()) {
                    if (tentativeGCost < existingNeighbor.get().gCost) {
                        existingNeighbor.get().gCost = tentativeGCost;
                        existingNeighbor.get().fCost = existingNeighbor.get().gCost + existingNeighbor.get().hCost;
                        existingNeighbor.get().parent = currentNode;
                    }
                } else {
                    neighbor.gCost = tentativeGCost;
                    neighbor.hCost = getHeuristic(neighbor.position);
                    neighbor.fCost = neighbor.gCost + neighbor.hCost;
                    neighbor.parent = currentNode;
                    openSet.add(neighbor);
                }
            }
        }
        return null;
    }

    /**
     * NEU: Deutlich verbesserte isWalkable-Logik für Mojang Mappings.
     * Prüft, ob ein 2 Blöcke hohes Wesen an dieser Position stehen kann.
     */
    private boolean isWalkable(BlockPos pos) {
        BlockPos groundPos = pos.below();
        BlockState groundState = level.getBlockState(groundPos);

        // Der Block darunter muss eine solide Oberfläche haben, auf der man stehen kann.
        if (groundState.getCollisionShape(level, groundPos).getFaceShape(Direction.UP).isEmpty()) {
            return false;
        }

        // Der Block auf Fußhöhe und Kopfhöhe darf nicht kollidieren (muss passierbar sein).
        return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty() &&
                level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty();
    }

    // --- Restliche Logik (größtenteils unverändert) ---

    private List<BlockPos> reconstructPath(Node endNode) {
        List<BlockPos> path = new ArrayList<>();
        Node currentNode = endNode;
        while (currentNode != null) {
            path.add(currentNode.position);
            currentNode = currentNode.parent;
        }
        Collections.reverse(path);
        return path;
    }

    private double getDistance(BlockPos a, BlockPos b) {
        // GEÄNDERT: getSquaredDistance -> distSqr
        return a.distSqr(b);
    }

    private double getHeuristic(BlockPos pos) {
        double manhattanDistance = Math.abs(pos.getX() - endPos.getX()) + Math.abs(pos.getY() - endPos.getY()) + Math.abs(pos.getZ() - endPos.getZ());
        return manhattanDistance * (1.0 + random.nextDouble() * imperfectionFactor);
    }

    private List<Node> getNeighbors(Node node) {
        List<Node> neighbors = new ArrayList<>();
        BlockPos p = node.position;
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;
                    neighbors.add(new Node(p.offset(x, y, z), null, 0, 0));
                }
            }
        }
        return neighbors;
    }

    private List<BlockPos> postProcessPath(List<BlockPos> path) {
        if (path.size() < 3 || overshootChance == 0) return path;
        List<BlockPos> newPath = new ArrayList<>();
        newPath.add(path.get(0));

        for (int i = 1; i < path.size() - 1; i++) {
            BlockPos prev = path.get(i-1);
            BlockPos current = path.get(i);
            BlockPos next = path.get(i+1);

            BlockPos dirToPrev = prev.subtract(current);
            BlockPos dirToNext = next.subtract(current);

            if (!dirToPrev.equals(dirToNext.multiply(-1)) && random.nextDouble() < overshootChance) {
                BlockPos lastPos = newPath.get(newPath.size() - 1);
                BlockPos lastDir = current.subtract(prev);
                int overshootSteps = 1 + random.nextInt(2);

                for (int j = 1; j <= overshootSteps; j++) {
                    BlockPos overshootPos = lastPos.offset(lastDir.getX() * j, lastDir.getY() * j, lastDir.getZ() * j);
                    if (isWalkable(overshootPos)) {
                        newPath.add(overshootPos);
                    } else break;
                }
                newPath.add(current);
            } else {
                newPath.add(current);
            }
        }
        newPath.add(path.get(path.size() - 1));
        return newPath;
    }

    private static class Node {
        public BlockPos position;
        public Node parent;
        public double gCost, hCost, fCost;

        public Node(BlockPos position, Node parent, double gCost, double hCost) {
            this.position = position; this.parent = parent;
            this.gCost = gCost; this.hCost = hCost;
            this.fCost = gCost + hCost;
        }
        @Override public boolean equals(Object o) { return o instanceof Node n && position.equals(n.position); }
        @Override public int hashCode() { return position.hashCode(); }
    }
}