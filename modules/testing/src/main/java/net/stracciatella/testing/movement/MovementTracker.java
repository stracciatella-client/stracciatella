package net.stracciatella.testing.movement;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks player movement over time. Use this in tick handlers to record
 * positions and verify movement behavior.
 */
public class MovementTracker {
    private final List<Vec3> positions = new ArrayList<>();
    private final List<Boolean> onGroundHistory = new ArrayList<>();

    public void recordTick(LocalPlayer player) {
        positions.add(player.position());
        onGroundHistory.add(player.onGround());
    }

    public Vec3 startPos() {
        return positions.isEmpty() ? Vec3.ZERO : positions.get(0);
    }

    public Vec3 latestPos() {
        return positions.isEmpty() ? Vec3.ZERO : positions.get(positions.size() - 1);
    }

    public int tickCount() {
        return positions.size();
    }

    public boolean isOnGround() {
        return !onGroundHistory.isEmpty() && onGroundHistory.get(onGroundHistory.size() - 1);
    }

    /**
     * Check if the player is currently at the given block position (feet position).
     */
    public boolean isAtBlock(BlockPos target) {
        Vec3 pos = latestPos();
        return BlockPos.containing(pos.x, pos.y, pos.z).equals(target);
    }

    /**
     * Check if the player is within a given distance of a target position.
     */
    public boolean isWithin(Vec3 target, double distance) {
        return latestPos().distanceTo(target) <= distance;
    }

    /**
     * Check if the player ever fell below a given Y level.
     */
    public boolean fellBelow(double y) {
        return positions.stream().anyMatch(pos -> pos.y < y);
    }

    /**
     * Check if the player's Y ever decreased (indicating a fall).
     */
    public boolean didFall() {
        for (int i = 1; i < positions.size(); i++) {
            if (positions.get(i).y < positions.get(i - 1).y - 0.01 && !onGroundHistory.get(i)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the total horizontal distance traveled.
     */
    public double totalHorizontalDistance() {
        double total = 0;
        for (int i = 1; i < positions.size(); i++) {
            Vec3 prev = positions.get(i - 1);
            Vec3 curr = positions.get(i);
            total += Math.sqrt(Math.pow(curr.x - prev.x, 2) + Math.pow(curr.z - prev.z, 2));
        }
        return total;
    }

    public List<Vec3> positions() {
        return List.copyOf(positions);
    }

    public void clear() {
        positions.clear();
        onGroundHistory.clear();
    }
}
