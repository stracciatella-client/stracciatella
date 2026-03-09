package net.stracciatella.testing.command;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Executes commands on the client and provides utilities for verifying results.
 */
public class CommandExecutor {

    /**
     * Execute a command as the local player (without the leading slash).
     * Example: executeCommand("gamemode creative")
     */
    public static void executeCommand(String command) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.connection.sendCommand(command);
        }
    }

    /**
     * Send a chat message as the local player.
     */
    public static void sendChat(String message) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.connection.sendChat(message);
        }
    }
}
