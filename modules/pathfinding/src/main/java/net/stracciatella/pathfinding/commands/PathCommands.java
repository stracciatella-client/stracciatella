package net.stracciatella.pathfinding.commands;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.stracciatella.pathfinding.logic.ChunkMeshBuilder;
import net.stracciatella.pathfinding.logic.MeshManager;

public class PathCommands {

    public void register() {

        var command = ClientCommandManager.literal("generateMesh").executes(context -> {
            long millis = System.currentTimeMillis();
            LocalPlayer sender = context.getSource().getPlayer();
            MeshManager.generateMesh(context.getSource().getWorld().getChunk(new BlockPos((int) sender.position().x, (int) sender.position().y, (int) sender.position().z)), sender);
            context.getSource().sendFeedback(Component.literal("Generated Mesh in " + (System.currentTimeMillis() - millis) + "ms"));
            return 1;
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(command);
        });
    }
}
