package net.stracciatella.core.macro;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.List;

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
     * @param path Die Liste der Blöcke, die abgelaufen werden sollen.
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

        // Da wir den Spieler immer in die richtige Richtung schauen lassen,
        // müssen wir nur die "Vorwärts"-Taste drücken.
        minecraft.options.keyUp.setDown(true);

        // Springen, wenn der nächste Block höher ist als der, auf dem wir stehen.
        if (targetPos.getY() > player.blockPosition().getY()) {
            // Nur springen, wenn der Kopf frei ist
            if (!minecraft.level.getBlockState(player.blockPosition().above(2)).isSolid()) {
                minecraft.options.keyJump.setDown(true);
            }
        }
    }

    public boolean isActive() {
        return active;
    }
}