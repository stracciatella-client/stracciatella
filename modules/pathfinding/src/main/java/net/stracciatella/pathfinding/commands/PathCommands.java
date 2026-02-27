package net.stracciatella.pathfinding.commands;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.stracciatella.pathfinding.ChunkCoordinate;
import net.stracciatella.pathfinding.display.PathDisplay;
import net.stracciatella.pathfinding.logic.MeshManager;
import net.stracciatella.pathfinding.logic.MeshPathfinder;
import net.stracciatella.pathfinding.logic.mesh.MeshNode;
import net.stracciatella.pathfinding.logic.mesh.Neighbor;

public class PathCommands {

    private static BlockPos startPos;
    private static BlockPos endPos;

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
                var targetBlock = ((BlockHitResult) lookingAt).getBlockPos();
                var chunkPos = commandContext.getSource().getEntity().chunkPosition();
                var node = MeshManager.meshes.get(sender).get(new ChunkCoordinate(chunkPos.x, chunkPos.z)).getNodes().get(targetBlock);
                if (node != null) {
                    String result = "";
                    for (Neighbor neighbor : node.getNeighbors()) {
                        result += "{" + neighbor.getNode().getBlockPos() + "} ";
                    }
                    commandContext.getSource().sendFeedback(Component.literal("Neighbors: " + result));
                } else {
                    commandContext.getSource().sendFeedback(Component.literal("No mesh node found for block " + targetBlock));
                }
            }

            return 1;
        });

        var pathPosStart = ClientCommandManager.literal("path")
                .then(literal("pos")
                        .then(literal("start").executes(context -> {
                            LocalPlayer sender = context.getSource().getPlayer();
                            BlockPos lookedPos = getLookedBlockPos(sender);
                            if (lookedPos == null) {
                                context.getSource().sendFeedback(Component.literal("Not looking at block"));
                                return 0;
                            }
                            startPos = lookedPos;
                            context.getSource().sendFeedback(Component.literal("Path start set to " + startPos));
                            return 1;
                        }))
                        .then(literal("end").executes(context -> {
                            LocalPlayer sender = context.getSource().getPlayer();
                            BlockPos lookedPos = getLookedBlockPos(sender);
                            if (lookedPos == null) {
                                context.getSource().sendFeedback(Component.literal("Not looking at block"));
                                return 0;
                            }
                            endPos = lookedPos;
                            context.getSource().sendFeedback(Component.literal("Path end set to " + endPos));
                            return 1;
                        })))
                .then(literal("find").executes(context -> {
                    LocalPlayer sender = context.getSource().getPlayer();
                    if (startPos == null || endPos == null) {
                        context.getSource().sendFeedback(Component.literal("Set both start and end using /path pos start|end"));
                        return 0;
                    }
                    MeshNode startNode = findNode(sender, startPos);
                    MeshNode endNode = findNode(sender, endPos);
                    if (startNode == null || endNode == null) {
                        context.getSource().sendFeedback(Component.literal("No mesh node found for start or end"));
                        return 0;
                    }

                    MeshPathfinder pathfinder = new MeshPathfinder();
                    var path = pathfinder.findPath(startNode, endNode);
                    if (path.isEmpty()) {
                        PathDisplay.clearHighlightedPath();
                        context.getSource().sendFeedback(Component.literal("No path found"));
                        return 0;
                    }

                    PathDisplay.setHighlightedPath(path);
                    context.getSource().sendFeedback(Component.literal("Path found with " + path.size() + " nodes"));
                    return 1;
                }));

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(command);
            dispatcher.register(displayConnectionsCommand);
            dispatcher.register(printNeighborsCommand);
            dispatcher.register(pathPosStart);
        });
    }

    private static BlockPos getLookedBlockPos(LocalPlayer player) {
        var lookingAt = player.raycastHitResult(0, player);
        if (lookingAt.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK) {
            return null;
        }
        return ((BlockHitResult) lookingAt).getBlockPos();
    }

    private static MeshNode findNode(LocalPlayer player, BlockPos pos) {
        var meshesForPlayer = MeshManager.meshes.get(player);
        if (meshesForPlayer == null) {
            return null;
        }
        ChunkCoordinate chunkCoordinate = new ChunkCoordinate(pos.getX() >> 4, pos.getZ() >> 4);
        var mesh = meshesForPlayer.get(chunkCoordinate);
        if (mesh == null) {
            return null;
        }
        return mesh.getNodes().get(pos);
    }
}
