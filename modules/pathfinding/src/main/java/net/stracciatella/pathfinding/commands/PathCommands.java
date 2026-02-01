package net.stracciatella.pathfinding.commands;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.stracciatella.pathfinding.display.PathDisplay;
import net.stracciatella.pathfinding.logic.ChunkMeshBuilder;
import net.stracciatella.pathfinding.logic.MeshManager;
import net.stracciatella.pathfinding.logic.mesh.Neighbor;

public class PathCommands {

    public void register() {

        var command = ClientCommandManager.literal("generateMesh").executes(context -> {
            long millis = System.currentTimeMillis();
            LocalPlayer sender = context.getSource().getPlayer();
            MeshManager.generateMesh(context.getSource().getWorld().getChunk(new BlockPos((int) sender.position().x, (int) sender.position().y, (int) sender.position().z)), sender);
            context.getSource().sendFeedback(Component.literal("Generated Mesh in " + (System.currentTimeMillis() - millis) + "ms"));
            return 1;
        });

        var displayConnectionsCommand = ClientCommandManager.literal("displayConnections").executes(commandContext -> {
            PathDisplay.displayNeighbors = !PathDisplay.displayNeighbors;
            return 1;
        });

        var printNeighborsCommand = ClientCommandManager.literal("printNeighborNodes").executes(commandContext -> {
            LocalPlayer sender = commandContext.getSource().getPlayer();
            var lookingAt = sender.raycastHitResult(0, sender);
            if (lookingAt.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK) {
                commandContext.getSource().sendFeedback(Component.literal("Not looking at block"));
                return 1;
            } else {
                var targetBlock = new BlockPos((int) lookingAt.getLocation().x(), (int) lookingAt.getLocation().y(), (int) lookingAt.getLocation().z());
                var node = MeshManager.meshes.get(sender).get(commandContext.getSource().getEntity().chunkPosition()).getNodes().get(targetBlock);
                if (node != null) {
                    String result = "";
                    for (Neighbor neighbor : node.getNeighbors()) {
                        result += "{" + neighbor.getNode().getBlockPos() + "} ";
                    }
                    commandContext.getSource().sendFeedback(Component.literal("Neighbors: " ));
                } else {
                    commandContext.getSource().sendFeedback(Component.literal("No mesh node found for block " + targetBlock));
                }
            }

            return 1;
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(command);
            dispatcher.register(displayConnectionsCommand);
            dispatcher.register(printNeighborsCommand);
        });
    }
}
