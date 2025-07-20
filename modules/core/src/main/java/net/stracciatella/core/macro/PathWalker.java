package net.stracciatella.core.macro;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class PathWalker {

    private final Minecraft minecraft;
    private List<BlockPos> path;
    private int currentIndex;
    private boolean active = false;

    public PathWalker() {
        this.minecraft = Minecraft.getInstance();
    }

    /**
     * Startet den PathWalker mit einem neuen Pfad.
     * <p/>
     *  * @param path Die Liste der Blöcke, die abgelaufen werden sollen.
     */
    public void start(List<BlockPos> path) {
        if (path == null || path.isEmpty()) {
            return;
        }
        this.path = path;
        this.currentIndex = 0;
        this.active = true;
    }

    /**
     * Stoppt die Bewegung und setzt alle Tasten frei.
     */
    public void stop() {
        this.active = false;
        if (minecraft.options != null) {
            // Stelle sicher, dass alle Bewegungstasten losgelassen werden.
            minecraft.options.keyUp.setDown(false);
            minecraft.options.keyDown.setDown(false);
            minecraft.options.keyLeft.setDown(false);
            minecraft.options.keyRight.setDown(false);
            minecraft.options.keyJump.setDown(false);
            minecraft.options.keyShift.setDown(false);
        }
    }

    /**
     * Diese Methode muss in einem Client-Tick-Event aufgerufen werden.
     */
    public void tick() {
        if (!active || minecraft.player == null || minecraft.level == null) {
            return;
        }

        // Prüfen, ob der Pfad zu Ende ist
        if (currentIndex >= path.size()) {
            stop();
            return;
        }

        BlockPos targetPos = path.get(currentIndex);
        Vec3 targetVec = Vec3.atCenterOf(targetPos);
        LocalPlayer player = minecraft.player;

        // Prüfen, ob das Ziel erreicht wurde, und zum nächsten übergehen
        Vec3 playerPos = player.position();
        double deltaX = targetVec.x() - playerPos.x();
        double deltaZ = targetVec.z() - playerPos.z();
        double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        double deltaY = targetVec.y() - playerPos.y();

        // Berechne, ob wir einen speziellen Sprung ausführen
        boolean isSpecialJump = isLongJump(player.blockPosition(), targetPos);

        // Spezielles Handling für Parkour-Sprünge: Wir müssen dem Ziel näher kommen,
        // bevor wir zum nächsten Ziel übergehen
        if (isSpecialJump) {
            // Bei Sprüngen über Lücken reicht es, wenn wir in der horizontalen Ebene nah genug sind
            if (distance < 0.5 && Math.abs(deltaY) < 1.0) {
                currentIndex++;
                if (currentIndex >= path.size()) {
                    stop();
                    return;
                }
                targetPos = path.get(currentIndex);
                targetVec = Vec3.atCenterOf(targetPos);
            }
        } else {
            // Normales Verhalten für andere Bewegungen
            if (distance < 0.2) {
                currentIndex++;
                if (currentIndex >= path.size()) {
                    stop();
                    return;
                }
                targetPos = path.get(currentIndex);
                targetVec = Vec3.atCenterOf(targetPos);
            }
        }

        // --- Blickrichtung anpassen ---
        lookAt(targetVec, player);

        // --- Bewegung steuern ---
        moveTowards(targetPos, player);
    }

    /**
     * Überprüft, ob ein Sprung über eine große Lücke ausgeführt werden muss
     */
    private boolean isLongJump(BlockPos from, BlockPos to) {
        int dx = Math.abs(to.getX() - from.getX());
        int dz = Math.abs(to.getZ() - from.getZ());
        int dy = to.getY() - from.getY(); // Positive Werte bedeuten Sprung nach oben

        // Horizontaler Sprung über 2 oder mehr Blöcke
        return (dx >= 2 || dz >= 2) || (dy >= 2);
    }

    private void lookAt(Vec3 target, LocalPlayer player) {
        Vec3 playerPos = player.getEyePosition();
        double deltaX = target.x - playerPos.x;
        double deltaZ = target.z - playerPos.z;

        // Berechne nur den Yaw (horizontale Drehung) für die Richtungsbestimmung
        float targetYaw = (float) (Math.atan2(deltaZ, deltaX) * (180.0 / Math.PI)) - 90.0F;

        // Direktes Setzen der horizontalen Blickrichtung
        player.setYRot(targetYaw);

        // Pitch (vertikale Neigung) bleibt unverändert, um das Auf- und Ab-Bewegen zu vermeiden


    }

    private void moveTowards(BlockPos targetPos, LocalPlayer player) {
        // Zuerst alle Tasten loslassen, um den Zustand zurückzusetzen
        minecraft.options.keyUp.setDown(false);
        minecraft.options.keyJump.setDown(false);
        minecraft.options.keySprint.setDown(false);

        BlockPos playerPos = player.blockPosition();
        BlockState currentBlock = minecraft.level.getBlockState(playerPos);
        BlockState targetBlock = minecraft.level.getBlockState(targetPos);
        String currentBlockName = currentBlock.getBlock().getName().getString().toLowerCase();
        String targetBlockName = targetBlock.getBlock().getName().getString().toLowerCase();

        // Berechne Distanz zwischen aktuellem Block und Zielblock
        int dx = Math.abs(targetPos.getX() - playerPos.getX());
        int dy = Math.abs(targetPos.getY() - playerPos.getY());
        int dz = Math.abs(targetPos.getZ() - playerPos.getZ());
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

        // Leiter-Logik
        boolean onLadder = currentBlockName.contains("ladder") || currentBlockName.contains("vine");
        boolean targetIsLadder = targetBlockName.contains("ladder") || targetBlockName.contains("vine");

        // Wasser-Logik
        boolean inWater = currentBlockName.contains("water") || player.isInWater();
        boolean targetIsWater = targetBlockName.contains("water");

        // Da wir den Spieler immer in die richtige Richtung schauen lassen,
        // müssen wir nur die "Vorwärts"-Taste drücken.
        minecraft.options.keyUp.setDown(true);

        // Prüfen, ob ein Sprung über eine Lücke erforderlich ist
        boolean isJumpingGap = (dx >= 2 || dz >= 2) && dy <= 1;

        // Sprung über eine Lücke
        if (isJumpingGap) {
            minecraft.options.keyJump.setDown(true);
            minecraft.options.keySprint.setDown(true);
            return; // Frühzeitig zurückkehren, um andere Bewegungslogik zu überspringen
        }

        // Auf Leitern nach oben oder unten klettern
        if (onLadder || targetIsLadder) {
            if (targetPos.getY() > playerPos.getY()) {
                // Nach oben klettern
                minecraft.options.keyJump.setDown(true);
            } else if (targetPos.getY() < playerPos.getY()) {
                // Nach unten klettern (Schleichen/Sneaking)
                minecraft.options.keyShift.setDown(true);
            }
        }
        // Im Wasser schwimmen
        else if (inWater || targetIsWater) {
            if (targetPos.getY() > playerPos.getY()) {
                // Nach oben schwimmen
                minecraft.options.keyJump.setDown(true);
            } else if (targetPos.getY() < playerPos.getY()) {
                // Nach unten schwimmen
                minecraft.options.keyShift.setDown(true);
            }
        }
        // Springen nach oben
        else if (targetPos.getY() > playerPos.getY()) {
            // Normales Springen (1 Block hoch)
            minecraft.options.keyJump.setDown(true);

            // Für 2 Blöcke hohe Sprünge: Sprint + Sprung
            if (dy >= 2) {
                minecraft.options.keySprint.setDown(true);
            }
        }
    }

    public boolean isActive() {
        return active;
    }
}