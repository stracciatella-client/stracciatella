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
        if (distance < 0.2) {
            currentIndex++;
            if (currentIndex >= path.size()) {
                stop();
                return;
            }
            targetPos = path.get(currentIndex);
            targetVec = Vec3.atCenterOf(targetPos);
        }

        // --- Blickrichtung anpassen ---
        lookAt(targetVec, player);

        // --- Bewegung steuern ---
        moveTowards(targetPos, player);
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

        BlockPos playerPos = player.blockPosition();
        BlockState currentBlock = minecraft.level.getBlockState(playerPos);
        BlockState targetBlock = minecraft.level.getBlockState(targetPos);
        String currentBlockName = currentBlock.getBlock().getName().getString().toLowerCase();
        String targetBlockName = targetBlock.getBlock().getName().getString().toLowerCase();

        // Leiter-Logik
        boolean onLadder = currentBlockName.contains("ladder") || currentBlockName.contains("vine");
        boolean targetIsLadder = targetBlockName.contains("ladder") || targetBlockName.contains("vine");

        // Wasser-Logik
        boolean inWater = currentBlockName.contains("water") || player.isInWater();
        boolean targetIsWater = targetBlockName.contains("water");

        // Da wir den Spieler immer in die richtige Richtung schauen lassen,
        // müssen wir nur die "Vorwärts"-Taste drücken.
        minecraft.options.keyUp.setDown(true);

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
        // Normales Springen (wenn nicht auf Leitern oder im Wasser)
        else if (targetPos.getY() > playerPos.getY()) {
            // Nur springen, wenn der Kopf frei ist
            if (!minecraft.level.getBlockState(playerPos.above(2)).isSolid()) {
                minecraft.options.keyJump.setDown(true);
                minecraft.options.keySprint.setDown(true);
            }
        }
    }

    public boolean isActive() {
        return active;
    }
}