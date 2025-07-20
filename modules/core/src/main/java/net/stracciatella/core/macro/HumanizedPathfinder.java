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

    // Aktueller Knoten für fortgeschrittene Pfadfindungslogik
    private Node currentNode;

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
            this.currentNode = currentNode;

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
     * Erweiterte isWalkable-Logik für Mojang Mappings.
     * Prüft, ob ein 2 Blöcke hohes Wesen an dieser Position stehen, klettern oder schwimmen kann.
     * Unterstützt jetzt auch Sprünge über Lücken.
     */
    private boolean isWalkable(BlockPos pos) {
        BlockState currentState = level.getBlockState(pos);
        BlockState aboveState = level.getBlockState(pos.above());
        BlockPos groundPos = pos.below();
        BlockState groundState = level.getBlockState(groundPos);

        // Prüfen, ob es eine Leiter ist
        if (currentState.getBlock().getName().getString().toLowerCase().contains("ladder") ||
            currentState.getBlock().getName().getString().toLowerCase().contains("vine")) {
            // Bei Leitern: Der Spieler kann klettern
            return aboveState.getCollisionShape(level, pos.above()).isEmpty() || 
                   aboveState.getBlock().getName().getString().toLowerCase().contains("ladder") ||
                   aboveState.getBlock().getName().getString().toLowerCase().contains("vine");
        }

        // Prüfen, ob es Wasser ist (Schwimmen)
        if (currentState.getBlock().getName().getString().toLowerCase().contains("water")) {
            // Bei Wasser: Der Spieler kann schwimmen
            return aboveState.getCollisionShape(level, pos.above()).isEmpty() ||
                   aboveState.getBlock().getName().getString().toLowerCase().contains("water");
        }

        // Standardprüfung für normales Gehen oder Springen
        // Für Sprünge: Der Block kann in der Luft sein, solange der Spieler
        // dorthin springen kann (wird in checkAndAddJumpNode geprüft)
        if (groundState.getCollisionShape(level, groundPos).getFaceShape(Direction.UP).isEmpty() && 
            !groundState.getBlock().getName().getString().toLowerCase().contains("water")) {
            // Wir erlauben leere Blöcke darunter für Sprünge über Lücken,
            // aber solche Knoten werden nur von checkAndAddJumpNode hinzugefügt
            if (currentNode != null && currentNode.parent != null) {
                BlockPos parentPos = currentNode.parent.position;
                // Wenn die Distanz zum Elternknoten groß ist, handelt es sich um einen Sprung
                int dx = Math.abs(parentPos.getX() - pos.getX());
                int dz = Math.abs(parentPos.getZ() - pos.getZ());
                if (dx >= 2 || dz >= 2) {
                    // Erlauben, wenn es ein Sprung über eine Lücke ist
                    return currentState.getCollisionShape(level, pos).isEmpty() &&
                           aboveState.getCollisionShape(level, pos.above()).isEmpty();
                }
            }
            return false;
        }

        // Der Block auf Fußhöhe und Kopfhöhe darf nicht kollidieren (muss passierbar sein).
        return currentState.getCollisionShape(level, pos).isEmpty() &&
               aboveState.getCollisionShape(level, pos.above()).isEmpty();
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
        // Grundlegende Distanz
        double baseDistance = a.distSqr(b);

        // Größere Kosten für spezielle Sprünge hinzufügen
        int dx = Math.abs(a.getX() - b.getX());
        int dy = Math.abs(a.getY() - b.getY());
        int dz = Math.abs(a.getZ() - b.getZ());

        // Horizontaler Sprung über mehrere Blöcke (erhöhter Aufwand)
        if (dy == 0 && (dx >= 2 || dz >= 2)) {
            baseDistance *= 1.5; // Sprünge über mehrere Blöcke sind aufwändiger
        }

        // Sprung nach oben (erhöhter Aufwand)
        if (dy > 0 && b.getY() > a.getY()) {
            baseDistance *= (1.0 + dy * 0.5); // Je höher der Sprung, desto aufwändiger
        }

        return baseDistance;
    }

    private double getHeuristic(BlockPos pos) {
        double manhattanDistance = Math.abs(pos.getX() - endPos.getX()) + Math.abs(pos.getY() - endPos.getY()) + Math.abs(pos.getZ() - endPos.getZ());
        return manhattanDistance * (1.0 + random.nextDouble() * imperfectionFactor);
    }

    private List<Node> getNeighbors(Node node) {
        List<Node> neighbors = new ArrayList<>();
        BlockPos p = node.position;

        // Normale Nachbarn (1 Block Bewegung in jede Richtung)
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;
                    neighbors.add(new Node(p.offset(x, y, z), null, 0, 0));
                }
            }
        }

        // Spezielle Sprungbewegungen hinzufügen
        addJumpNodes(neighbors, p);

        return neighbors;
    }

    /**
     * Fügt spezielle Sprungbewegungen zu den Nachbarn hinzu
     * - Sprünge über 2-3 Blöcke breite Lücken
     * - Sprünge nach oben (1-2 Blöcke hoch)
     */
    private void addJumpNodes(List<Node> neighbors, BlockPos pos) {
        // Horizontal-Sprünge (2-3 Blöcke)
        for (int d = 2; d <= 3; d++) {
            // X-Achse (positiv und negativ)
            checkAndAddJumpNode(neighbors, pos, new BlockPos(pos.getX() + d, pos.getY(), pos.getZ()));
            checkAndAddJumpNode(neighbors, pos, new BlockPos(pos.getX() - d, pos.getY(), pos.getZ()));

            // Z-Achse (positiv und negativ)
            checkAndAddJumpNode(neighbors, pos, new BlockPos(pos.getX(), pos.getY(), pos.getZ() + d));
            checkAndAddJumpNode(neighbors, pos, new BlockPos(pos.getX(), pos.getY(), pos.getZ() - d));

            // Diagonale Sprünge (nur für d=2, bei d=3 zu weit)
            if (d == 2) {
                checkAndAddJumpNode(neighbors, pos, new BlockPos(pos.getX() + d, pos.getY(), pos.getZ() + d));
                checkAndAddJumpNode(neighbors, pos, new BlockPos(pos.getX() + d, pos.getY(), pos.getZ() - d));
                checkAndAddJumpNode(neighbors, pos, new BlockPos(pos.getX() - d, pos.getY(), pos.getZ() + d));
                checkAndAddJumpNode(neighbors, pos, new BlockPos(pos.getX() - d, pos.getY(), pos.getZ() - d));
            }
        }

        // Vertikale Sprünge (1-2 Blöcke nach oben)
        for (int y = 1; y <= 2; y++) {
            // Direkter Sprung nach oben
            checkAndAddJumpNode(neighbors, pos, new BlockPos(pos.getX(), pos.getY() + y, pos.getZ()));

            // Diagonale Sprünge nach oben
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && z == 0) continue; // Überspringen des direkten Sprungs nach oben
                    checkAndAddJumpNode(neighbors, pos, new BlockPos(pos.getX() + x, pos.getY() + y, pos.getZ() + z));
                }
            }
        }
    }

    /**
     * Prüft, ob ein Sprung zum Ziel möglich ist und fügt ihn gegebenenfalls zu den Nachbarn hinzu
     */
    private void checkAndAddJumpNode(List<Node> neighbors, BlockPos from, BlockPos to) {
        // Prüfen, ob der Zielblock begehbar ist
        if (!isWalkable(to)) {
            return;
        }

        // Bei horizontalen Sprüngen: Überprüfen, ob der Pfad dazwischen frei ist
        if (to.getY() == from.getY()) {
            int dx = to.getX() - from.getX();
            int dz = to.getZ() - from.getZ();

            // Für Sprünge über 2-3 Blöcke muss der Luftraum frei sein
            for (int i = 1; i < Math.max(Math.abs(dx), Math.abs(dz)); i++) {
                int x = from.getX() + (dx != 0 ? (dx > 0 ? i : -i) : 0);
                int z = from.getZ() + (dz != 0 ? (dz > 0 ? i : -i) : 0);

                // Überprüfen, ob der Luftraum frei ist (inkl. Kopfraum)
                BlockPos midPos = new BlockPos(x, from.getY(), z);
                if (!level.getBlockState(midPos).getCollisionShape(level, midPos).isEmpty() ||
                    !level.getBlockState(midPos.above()).getCollisionShape(level, midPos.above()).isEmpty()) {
                    return;
                }
            }
        }

        neighbors.add(new Node(to, null, 0, 0));
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