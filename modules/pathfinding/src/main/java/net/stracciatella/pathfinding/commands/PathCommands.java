package net.stracciatella.pathfinding.commands;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.world.phys.BlockHitResult;
import net.stracciatella.pathfinding.ChunkCoordinate;
import net.stracciatella.pathfinding.display.PathDisplay;
import net.stracciatella.pathfinding.logic.MeshManager;
import net.stracciatella.pathfinding.logic.MeshPathfinder;
import net.stracciatella.pathfinding.logic.PathWalker;
import net.stracciatella.pathfinding.logic.mesh.MeshNode;
import net.stracciatella.pathfinding.logic.mesh.Neighbor;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import java.util.Locale;

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
            PathDisplay.setDisplayNeighbors(!PathDisplay.displayNeighbors);
            return 1;
        });

        var connectionModeCommand = ClientCommandManager.literal("path")
                .then(literal("connections")
                        .then(literal("all").executes(context -> {
                            PathDisplay.setConnectionMode(PathDisplay.ConnectionMode.ALL);
                            context.getSource().sendFeedback(Component.literal("Connection display: ALL"));
                            return 1;
                        }))
                        .then(literal("path").executes(context -> {
                            PathDisplay.setConnectionMode(PathDisplay.ConnectionMode.PATH_ONLY);
                            context.getSource().sendFeedback(Component.literal("Connection display: PATH_ONLY"));
                            return 1;
                        }))
                        .then(literal("none").executes(context -> {
                            PathDisplay.setConnectionMode(PathDisplay.ConnectionMode.NONE);
                            context.getSource().sendFeedback(Component.literal("Connection display: NONE"));
                            return 1;
                        })));

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
                    MeshNode startNode = findOrBuildNearestNode(context.getSource().getWorld(), sender, startPos);
                    MeshNode endNode = findOrBuildNearestNode(context.getSource().getWorld(), sender, endPos);
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
                }))
                .then(literal("walk").executes(context -> {
                    LocalPlayer sender = context.getSource().getPlayer();
                    if (endPos == null) {
                        context.getSource().sendFeedback(Component.literal("Set end using /path pos end"));
                        return 0;
                    }
                    startPos = sender.blockPosition();
                    MeshNode startNode = findOrBuildNearestNode(context.getSource().getWorld(), sender, startPos);
                    MeshNode endNode = findOrBuildNearestNode(context.getSource().getWorld(), sender, endPos);
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
                    PathWalker.start(path);
                    context.getSource().sendFeedback(Component.literal("Walking path with " + path.size() + " nodes"));
                    return 1;
                }))
                .then(literal("walk")
                        .then(literal("stop").executes(context -> {
                            PathWalker.stop();
                            context.getSource().sendFeedback(Component.literal("Path walking stopped"));
                            return 1;
                        })))
                .then(literal("walkdebug")
                        .then(literal("on").executes(context -> {
                            PathWalker.setDebug(true);
                            context.getSource().sendFeedback(Component.literal("Path walker debug enabled (console)"));
                            return 1;
                        }))
                        .then(literal("off").executes(context -> {
                            PathWalker.setDebug(false);
                            context.getSource().sendFeedback(Component.literal("Path walker debug disabled"));
                            return 1;
                        })))
                .then(literal("walklearn")
                        .then(literal("on").executes(context -> {
                            PathWalker.setLearning(true);
                            context.getSource().sendFeedback(Component.literal("Path walker learning enabled"));
                            return 1;
                        }))
                        .then(literal("off").executes(context -> {
                            PathWalker.setLearning(false);
                            context.getSource().sendFeedback(Component.literal("Path walker learning disabled"));
                            return 1;
                        })))
                .then(literal("walkcalibrate")
                        .then(literal("start").executes(context -> {
                            PathWalker.startCalibration();
                            context.getSource().sendFeedback(Component.literal("Path walker calibration started"));
                            return 1;
                        }))
                        .then(literal("stop").executes(context -> {
                            PathWalker.CalibrationReport report = PathWalker.stopCalibration();
                            if (report.applied()) {
                                context.getSource().sendFeedback(Component.literal("Calibration applied: " + report.summary()));
                            } else {
                                context.getSource().sendFeedback(Component.literal("Calibration incomplete: " + report.summary()));
                            }
                            return 1;
                        }))
                        .then(literal("status").executes(context -> {
                            String status = PathWalker.isCalibrationActive() ? "active" : "inactive";
                            context.getSource().sendFeedback(Component.literal("Calibration " + status + ": " + PathWalker.getCalibrationSummary()));
                            return 1;
                        })))
                .then(literal("walkconfig")
                        .then(literal("menu").executes(context -> {
                            sendWalkConfigMenu(context.getSource().getPlayer());
                            return 1;
                        }))
                        .then(literal("turn")
                                .then(argument("min", FloatArgumentType.floatArg(0.0f))
                                        .then(argument("max", FloatArgumentType.floatArg(0.0f))
                                                .executes(context -> {
                                                    float min = FloatArgumentType.getFloat(context, "min");
                                                    float max = FloatArgumentType.getFloat(context, "max");
                                                    PathWalker.setTurnRange(min, max);
                                                    context.getSource().sendFeedback(Component.literal("Turn range set to " + min + " - " + max + " deg"));
                                                    return 1;
                                                }))))
                        .then(literal("offset")
                                .then(argument("min", DoubleArgumentType.doubleArg(0.0))
                                        .then(argument("max", DoubleArgumentType.doubleArg(0.0))
                                                .executes(context -> {
                                                    double min = DoubleArgumentType.getDouble(context, "min");
                                                    double max = DoubleArgumentType.getDouble(context, "max");
                                                    PathWalker.setOffsetRange(min, max);
                                                    context.getSource().sendFeedback(Component.literal("Offset range set to " + min + " - " + max));
                                                    return 1;
                                                }))))
                        .then(literal("pause")
                                .then(argument("minMs", IntegerArgumentType.integer(0))
                                        .then(argument("maxMs", IntegerArgumentType.integer(0))
                                                .executes(context -> {
                                                    int min = IntegerArgumentType.getInteger(context, "minMs");
                                                    int max = IntegerArgumentType.getInteger(context, "maxMs");
                                                    PathWalker.setPauseRange(min, max);
                                                    context.getSource().sendFeedback(Component.literal("Pause range set to " + min + " - " + max + " ms"));
                                                    return 1;
                                                }))))
                        .then(literal("distance")
                                .then(argument("walk", DoubleArgumentType.doubleArg(0.0))
                                        .then(argument("sprint", DoubleArgumentType.doubleArg(0.0))
                                                .executes(context -> {
                                                    double walk = DoubleArgumentType.getDouble(context, "walk");
                                                    double sprint = DoubleArgumentType.getDouble(context, "sprint");
                                                    PathWalker.setDistanceThresholds(walk, sprint);
                                                    context.getSource().sendFeedback(Component.literal("Distance thresholds set to walk=" + walk + " sprint=" + sprint));
                                                    return 1;
                                                }))))
                        .then(literal("sprintchance")
                                .then(argument("min", DoubleArgumentType.doubleArg(0.0, 1.0))
                                        .then(argument("max", DoubleArgumentType.doubleArg(0.0, 1.0))
                                                .executes(context -> {
                                                    double min = DoubleArgumentType.getDouble(context, "min");
                                                    double max = DoubleArgumentType.getDouble(context, "max");
                                                    PathWalker.setSprintChanceRange(min, max);
                                                    context.getSource().sendFeedback(Component.literal("Sprint chance range set to " + min + " - " + max));
                                                    return 1;
                                                }))))
                        .then(literal("edgejump")
                                .then(argument("min", DoubleArgumentType.doubleArg(0.0, 1.0))
                                        .then(argument("max", DoubleArgumentType.doubleArg(0.0, 1.0))
                                                .executes(context -> {
                                                    double min = DoubleArgumentType.getDouble(context, "min");
                                                    double max = DoubleArgumentType.getDouble(context, "max");
                                                    PathWalker.setEdgeJumpRange(min, max);
                                                    context.getSource().sendFeedback(Component.literal("Edge jump range set to " + min + " - " + max));
                                                    return 1;
                                                }))))
                        .then(literal("turnaccel")
                                .then(argument("accel", FloatArgumentType.floatArg(0.0f))
                                        .executes(context -> {
                                            float accel = FloatArgumentType.getFloat(context, "accel");
                                            PathWalker.setTurnAccel(accel);
                                            context.getSource().sendFeedback(Component.literal("Turn acceleration set to " + accel + " deg/tick^2"));
                                            return 1;
                                        })))
                        .then(literal("turnjitter")
                                .then(argument("minMs", IntegerArgumentType.integer(0))
                                        .then(argument("maxMs", IntegerArgumentType.integer(0))
                                                .executes(context -> {
                                                    int min = IntegerArgumentType.getInteger(context, "minMs");
                                                    int max = IntegerArgumentType.getInteger(context, "maxMs");
                                                    PathWalker.setTurnJitterRange(min, max);
                                                    context.getSource().sendFeedback(Component.literal("Turn jitter range set to " + min + " - " + max + " ms"));
                                                    return 1;
                                                }))))
                        .then(literal("jumptolerance")
                                .then(argument("deg", FloatArgumentType.floatArg(0.0f))
                                        .executes(context -> {
                                            float deg = FloatArgumentType.getFloat(context, "deg");
                                            PathWalker.setJumpTolerance(deg);
                                            context.getSource().sendFeedback(Component.literal("Jump facing tolerance set to " + deg + " deg"));
                                            return 1;
                                        })))
                        .then(literal("walkturn")
                                .then(argument("deg", FloatArgumentType.floatArg(0.0f))
                                        .executes(context -> {
                                            float deg = FloatArgumentType.getFloat(context, "deg");
                                            PathWalker.setWalkTurnThreshold(deg);
                                            context.getSource().sendFeedback(Component.literal("Walk turn threshold set to " + deg + " deg"));
                                            return 1;
                                        })))
                        .then(literal("sharpturn")
                                .then(argument("deg", FloatArgumentType.floatArg(0.0f))
                                        .executes(context -> {
                                            float deg = FloatArgumentType.getFloat(context, "deg");
                                            PathWalker.setSharpTurnDeg(deg);
                                            context.getSource().sendFeedback(Component.literal("Sharp turn threshold set to " + deg + " deg"));
                                            return 1;
                                        })))
                        .then(literal("turnprep")
                                .then(argument("distance", FloatArgumentType.floatArg(0.0f))
                                        .executes(context -> {
                                            float distance = FloatArgumentType.getFloat(context, "distance");
                                            PathWalker.setTurnPrepDistance(distance);
                                            context.getSource().sendFeedback(Component.literal("Turn prep distance set to " + distance));
                                            return 1;
                                        })))
                        .then(literal("turnstop")
                                .then(argument("deg", FloatArgumentType.floatArg(0.0f))
                                        .executes(context -> {
                                            float deg = FloatArgumentType.getFloat(context, "deg");
                                            PathWalker.setTurnStopThreshold(deg);
                                            context.getSource().sendFeedback(Component.literal("Turn stop threshold set to " + deg + " deg"));
                                            return 1;
                                        })))
                        .then(literal("turnpause")
                                .then(argument("minMs", IntegerArgumentType.integer(0))
                                        .then(argument("maxMs", IntegerArgumentType.integer(0))
                                                .executes(context -> {
                                                    int min = IntegerArgumentType.getInteger(context, "minMs");
                                                    int max = IntegerArgumentType.getInteger(context, "maxMs");
                                                    PathWalker.setTurnPauseRange(min, max);
                                                    context.getSource().sendFeedback(Component.literal("Turn pause range set to " + min + " - " + max + " ms"));
                                                    return 1;
                                                }))))
                        .then(literal("edgejumpscale")
                                .then(argument("value", DoubleArgumentType.doubleArg(0.0))
                                        .executes(context -> {
                                            double value = DoubleArgumentType.getDouble(context, "value");
                                            PathWalker.setEdgeJumpScale(value);
                                            context.getSource().sendFeedback(Component.literal("Edge jump scale set to " + value));
                                            return 1;
                                        })))
                        .then(literal("edgejumpshort")
                                .then(argument("min", DoubleArgumentType.doubleArg(0.0, 1.0))
                                        .then(argument("max", DoubleArgumentType.doubleArg(0.0, 1.0))
                                                .executes(context -> {
                                                    double min = DoubleArgumentType.getDouble(context, "min");
                                                    double max = DoubleArgumentType.getDouble(context, "max");
                                                    PathWalker.setEdgeJumpShortRange(min, max);
                                                    context.getSource().sendFeedback(Component.literal("Edge jump short range set to " + min + " - " + max));
                                                    return 1;
                                                }))))
                        .then(literal("edgejumpmid")
                                .then(argument("min", DoubleArgumentType.doubleArg(0.0, 1.0))
                                        .then(argument("max", DoubleArgumentType.doubleArg(0.0, 1.0))
                                                .executes(context -> {
                                                    double min = DoubleArgumentType.getDouble(context, "min");
                                                    double max = DoubleArgumentType.getDouble(context, "max");
                                                    PathWalker.setEdgeJumpMidRange(min, max);
                                                    context.getSource().sendFeedback(Component.literal("Edge jump mid range set to " + min + " - " + max));
                                                    return 1;
                                                }))))
                        .then(literal("edgejumpsprintbias")
                                .then(argument("value", DoubleArgumentType.doubleArg(0.0, 1.0))
                                        .executes(context -> {
                                            double value = DoubleArgumentType.getDouble(context, "value");
                                            PathWalker.setEdgeJumpSprintBias(value);
                                            context.getSource().sendFeedback(Component.literal("Edge jump sprint bias set to " + value));
                                            return 1;
                                        })))
                        .then(literal("edgejumpfwdairbias")
                                .then(argument("value", DoubleArgumentType.doubleArg(0.0, 1.0))
                                        .executes(context -> {
                                            double value = DoubleArgumentType.getDouble(context, "value");
                                            PathWalker.setEdgeJumpForwardAirBias(value);
                                            context.getSource().sendFeedback(Component.literal("Edge jump forward-air bias set to " + value));
                                            return 1;
                                        })))
                        .then(literal("edgejumpfwdairmin")
                                .then(argument("value", DoubleArgumentType.doubleArg(0.0, 1.0))
                                        .executes(context -> {
                                            double value = DoubleArgumentType.getDouble(context, "value");
                                            PathWalker.setEdgeJumpForwardAirMin(value);
                                            context.getSource().sendFeedback(Component.literal("Edge jump forward-air min set to " + value));
                                            return 1;
                                        })))
                        .then(literal("edgejumphold")
                                .then(argument("value", DoubleArgumentType.doubleArg(0.0))
                                        .executes(context -> {
                                            double value = DoubleArgumentType.getDouble(context, "value");
                                            PathWalker.setEdgeJumpHoldDistance(value);
                                            context.getSource().sendFeedback(Component.literal("Edge jump hold distance set to " + value));
                                            return 1;
                                        })))
                        .then(literal("edgejumpholdedge")
                                .then(argument("value", DoubleArgumentType.doubleArg(0.0, 0.5))
                                        .executes(context -> {
                                            double value = DoubleArgumentType.getDouble(context, "value");
                                            PathWalker.setEdgeJumpHoldEdge(value);
                                            context.getSource().sendFeedback(Component.literal("Edge jump hold edge set to " + fmt(value)));
                                            return 1;
                                        })))
                        .then(literal("edgejumptrigger")
                                .then(argument("value", DoubleArgumentType.doubleArg(0.0))
                                        .executes(context -> {
                                            double value = DoubleArgumentType.getDouble(context, "value");
                                            PathWalker.setEdgeJumpTriggerDistance(value);
                                            context.getSource().sendFeedback(Component.literal("Edge jump trigger distance set to " + value));
                                            return 1;
                                        })))
                        .then(literal("edgejumptriggeredge")
                                .then(argument("value", DoubleArgumentType.doubleArg(0.0, 0.5))
                                        .executes(context -> {
                                            double value = DoubleArgumentType.getDouble(context, "value");
                                            PathWalker.setEdgeJumpTriggerEdge(value);
                                            context.getSource().sendFeedback(Component.literal("Edge jump trigger edge set to " + fmt(value)));
                                            return 1;
                                        })))
                        .then(literal("alignhold")
                                .then(argument("ms", IntegerArgumentType.integer(0))
                                        .executes(context -> {
                                            int ms = IntegerArgumentType.getInteger(context, "ms");
                                            PathWalker.setAlignmentHoldMs(ms);
                                            context.getSource().sendFeedback(Component.literal("Alignment hold set to " + ms + " ms"));
                                            return 1;
                                        })))
                        .then(literal("aligndeadzone")
                                .then(argument("deg", FloatArgumentType.floatArg(0.0f))
                                        .executes(context -> {
                                            float deg = FloatArgumentType.getFloat(context, "deg");
                                            PathWalker.setAlignmentDeadzoneDeg(deg);
                                            context.getSource().sendFeedback(Component.literal("Alignment deadzone set to " + deg + " deg"));
                                            return 1;
                                        })))
                        .then(literal("walkturnmax")
                                .then(argument("deg", FloatArgumentType.floatArg(0.0f))
                                        .executes(context -> {
                                            float deg = FloatArgumentType.getFloat(context, "deg");
                                            PathWalker.setWalkTurnMaxDeg(deg);
                                            context.getSource().sendFeedback(Component.literal("Walk turn max set to " + deg + " deg"));
                                            return 1;
                                        })))
                        .then(literal("jumpcooldown")
                                .then(argument("ticks", IntegerArgumentType.integer(0))
                                        .executes(context -> {
                                            int ticks = IntegerArgumentType.getInteger(context, "ticks");
                                            PathWalker.setJumpCooldownTicks(ticks);
                                            context.getSource().sendFeedback(Component.literal("Jump cooldown set to " + ticks + " ticks"));
                                            return 1;
                                        })))
                        .then(literal("pitchjitter")
                                .then(argument("min", FloatArgumentType.floatArg(0.0f))
                                        .then(argument("max", FloatArgumentType.floatArg(0.0f))
                                                .executes(context -> {
                                                    float min = FloatArgumentType.getFloat(context, "min");
                                                    float max = FloatArgumentType.getFloat(context, "max");
                                                    PathWalker.setPitchJitterRange(min, max);
                                                    context.getSource().sendFeedback(Component.literal("Pitch jitter range set to " + min + " - " + max + " deg"));
                                                    return 1;
                                                }))))
                        .then(literal("jumpaimyaw")
                                .then(argument("min", FloatArgumentType.floatArg(0.0f))
                                        .then(argument("max", FloatArgumentType.floatArg(0.0f))
                                                .executes(context -> {
                                                    float min = FloatArgumentType.getFloat(context, "min");
                                                    float max = FloatArgumentType.getFloat(context, "max");
                                                    PathWalker.setJumpAimYawRange(min, max);
                                                    context.getSource().sendFeedback(Component.literal("Jump aim yaw range set to " + min + " - " + max + " deg"));
                                                    return 1;
                                                }))))
                        .then(literal("jumpsimticks")
                                .then(argument("ticks", IntegerArgumentType.integer(5))
                                        .executes(context -> {
                                            int ticks = IntegerArgumentType.getInteger(context, "ticks");
                                            PathWalker.setJumpSimTicks(ticks);
                                            context.getSource().sendFeedback(Component.literal("Jump sim ticks set to " + ticks));
                                            return 1;
                                        })))
                        .then(literal("jumplanding")
                                .then(argument("margin", DoubleArgumentType.doubleArg(0.0))
                                        .executes(context -> {
                                            double margin = DoubleArgumentType.getDouble(context, "margin");
                                            PathWalker.setJumpLandingMargin(margin);
                                            context.getSource().sendFeedback(Component.literal("Jump landing margin set to " + fmt(margin)));
                                            return 1;
                                        }))));

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(command);
            dispatcher.register(displayConnectionsCommand);
            dispatcher.register(printNeighborsCommand);
            dispatcher.register(pathPosStart);
            dispatcher.register(connectionModeCommand);
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

    private static MeshNode findOrBuildNearestNode(net.minecraft.world.level.Level level, LocalPlayer player, BlockPos pos) {
        ChunkCoordinate chunkCoordinate = new ChunkCoordinate(pos.getX() >> 4, pos.getZ() >> 4);
        var meshesForPlayer = MeshManager.meshes.get(player);
        if (meshesForPlayer == null || !meshesForPlayer.containsKey(chunkCoordinate)) {
            MeshManager.generateMesh(level.getChunk(pos), player);
        }
        var mesh = MeshManager.meshes.get(player).get(chunkCoordinate);
        if (mesh == null) {
            return null;
        }
        MeshNode exact = mesh.getNodes().get(pos);
        if (exact != null) {
            return exact;
        }
        MeshNode nearest = null;
        double bestDist = Double.MAX_VALUE;
        for (MeshNode node : mesh.getNodes().values()) {
            double dx = node.getX() - pos.getX();
            double dy = node.getY() - pos.getY();
            double dz = node.getZ() - pos.getZ();
            double dist = dx * dx + dy * dy + dz * dz;
            if (dist < bestDist) {
                bestDist = dist;
                nearest = node;
            }
        }
        return nearest;
    }

    private static void sendWalkConfigMenu(LocalPlayer player) {
        if (player == null) {
            return;
        }
        sendMenuHeader(player, "Path Walker Config");
        sendMenuLine(player, rangeLine("turn", PathWalker.CONFIG.turnMinDeg, PathWalker.CONFIG.turnMaxDeg, 0.5, "/path walkconfig turn", 0.0, Double.POSITIVE_INFINITY));
        sendMenuLine(player, rangeLine("offset", PathWalker.CONFIG.offsetMin, PathWalker.CONFIG.offsetMax, 0.05, "/path walkconfig offset", 0.0, Double.POSITIVE_INFINITY));
        sendMenuLine(player, rangeLineInt("pause", PathWalker.CONFIG.pauseMinMs, PathWalker.CONFIG.pauseMaxMs, 50, "/path walkconfig pause", 0));
        sendMenuLine(player, rangeLine("distance", PathWalker.CONFIG.walkDistance, PathWalker.CONFIG.sprintDistance, 0.25, "/path walkconfig distance", 0.0, Double.POSITIVE_INFINITY));
        sendMenuLine(player, rangeLine("sprintchance", PathWalker.CONFIG.sprintChanceMin, PathWalker.CONFIG.sprintChanceMax, 0.05, "/path walkconfig sprintchance", 0.0, 1.0));
        sendMenuLine(player, rangeLine("edgejump", PathWalker.CONFIG.edgeJumpMin, PathWalker.CONFIG.edgeJumpMax, 0.01, "/path walkconfig edgejump", 0.0, 1.0));
        sendMenuLine(player, valueLine("turnaccel", PathWalker.CONFIG.turnAccel, 0.1, "/path walkconfig turnaccel", 0.0, Double.POSITIVE_INFINITY));
        sendMenuLine(player, rangeLineInt("turnjitter", PathWalker.CONFIG.turnJitterMinMs, PathWalker.CONFIG.turnJitterMaxMs, 20, "/path walkconfig turnjitter", 0));
        sendMenuLine(player, valueLine("jumptolerance", PathWalker.CONFIG.jumpFacingToleranceDeg, 1.0, "/path walkconfig jumptolerance", 0.0, Double.POSITIVE_INFINITY));
        sendMenuLine(player, valueLine("walkturn", PathWalker.CONFIG.walkTurnThresholdDeg, 1.0, "/path walkconfig walkturn", 0.0, Double.POSITIVE_INFINITY));
        sendMenuLine(player, valueLine("sharpturn", PathWalker.CONFIG.sharpTurnDeg, 2.0, "/path walkconfig sharpturn", 0.0, Double.POSITIVE_INFINITY));
        sendMenuLine(player, valueLine("turnprep", PathWalker.CONFIG.turnPrepDistance, 0.1, "/path walkconfig turnprep", 0.0, Double.POSITIVE_INFINITY));
        sendMenuLine(player, valueLine("turnstop", PathWalker.CONFIG.turnStopThresholdDeg, 1.0, "/path walkconfig turnstop", 0.0, Double.POSITIVE_INFINITY));
        sendMenuLine(player, rangeLineInt("turnpause", PathWalker.CONFIG.turnPauseMinMs, PathWalker.CONFIG.turnPauseMaxMs, 20, "/path walkconfig turnpause", 0));
        sendMenuLine(player, valueLine("edgejumpscale", PathWalker.CONFIG.edgeJumpScale, 0.005, "/path walkconfig edgejumpscale", 0.0, Double.POSITIVE_INFINITY));
        sendMenuLine(player, rangeLine("edgejumpshort", PathWalker.CONFIG.edgeJumpShortMin, PathWalker.CONFIG.edgeJumpShortMax, 0.01, "/path walkconfig edgejumpshort", 0.0, 1.0));
        sendMenuLine(player, rangeLine("edgejumpmid", PathWalker.CONFIG.edgeJumpMidMin, PathWalker.CONFIG.edgeJumpMidMax, 0.01, "/path walkconfig edgejumpmid", 0.0, 1.0));
        sendMenuLine(player, valueLine("edgejumpsprintbias", PathWalker.CONFIG.edgeJumpSprintBias, 0.005, "/path walkconfig edgejumpsprintbias", 0.0, 1.0));
        sendMenuLine(player, valueLine("edgejumpfwdairbias", PathWalker.CONFIG.edgeJumpForwardAirBias, 0.01, "/path walkconfig edgejumpfwdairbias", 0.0, 1.0));
        sendMenuLine(player, valueLine("edgejumpfwdairmin", PathWalker.CONFIG.edgeJumpForwardAirMin, 0.01, "/path walkconfig edgejumpfwdairmin", 0.0, 1.0));
        sendMenuLine(player, valueLine("edgejumphold", PathWalker.CONFIG.edgeJumpHoldDistance, 0.1, "/path walkconfig edgejumphold", 0.0, Double.POSITIVE_INFINITY));
        sendMenuLine(player, valueLine("edgejumpholdedge", PathWalker.CONFIG.edgeJumpHoldEdge, 0.01, "/path walkconfig edgejumpholdedge", 0.0, 0.5));
        sendMenuLine(player, valueLine("edgejumptrigger", PathWalker.CONFIG.edgeJumpTriggerDistance, 0.1, "/path walkconfig edgejumptrigger", 0.0, Double.POSITIVE_INFINITY));
        sendMenuLine(player, valueLine("edgejumptriggeredge", PathWalker.CONFIG.edgeJumpTriggerEdge, 0.01, "/path walkconfig edgejumptriggeredge", 0.0, 0.5));
        sendMenuLine(player, valueLineInt("alignhold", PathWalker.CONFIG.alignmentHoldMs, 50, "/path walkconfig alignhold", 0));
        sendMenuLine(player, valueLine("aligndeadzone", PathWalker.CONFIG.alignmentDeadzoneDeg, 0.5, "/path walkconfig aligndeadzone", 0.0, Double.POSITIVE_INFINITY));
        sendMenuLine(player, valueLine("walkturnmax", PathWalker.CONFIG.walkTurnMaxDeg, 2.0, "/path walkconfig walkturnmax", 0.0, Double.POSITIVE_INFINITY));
        sendMenuLine(player, valueLineInt("jumpcooldown", PathWalker.CONFIG.jumpCooldownTicks, 1, "/path walkconfig jumpcooldown", 0));
        sendMenuLine(player, rangeLine("pitchjitter", PathWalker.CONFIG.pitchJitterMinDeg, PathWalker.CONFIG.pitchJitterMaxDeg, 0.1, "/path walkconfig pitchjitter", 0.0, Double.POSITIVE_INFINITY));
        sendMenuLine(player, rangeLine("jumpaimyaw", PathWalker.CONFIG.jumpAimYawMinDeg, PathWalker.CONFIG.jumpAimYawMaxDeg, 0.5, "/path walkconfig jumpaimyaw", 0.0, Double.POSITIVE_INFINITY));
        sendMenuLine(player, valueLineInt("jumpsimticks", PathWalker.CONFIG.jumpSimTicks, 1, "/path walkconfig jumpsimticks", 5));
        sendMenuLine(player, valueLine("jumplanding", PathWalker.CONFIG.jumpLandingMargin, 0.05, "/path walkconfig jumplanding", 0.0, Double.POSITIVE_INFINITY));
    }

    private static void sendMenuHeader(LocalPlayer player, String title) {
        player.displayClientMessage(Component.literal("=== " + title + " ===").withStyle(ChatFormatting.GOLD), false);
    }

    private static void sendMenuLine(LocalPlayer player, Component line) {
        player.displayClientMessage(line, false);
    }

    private static Component rangeLine(String label, double min, double max, double step, String commandPrefix, double clampMin, double clampMax) {
        double decMin = clamp(min - step, clampMin, clampMax);
        double incMin = clamp(min + step, clampMin, clampMax);
        double decMax = clamp(max - step, clampMin, clampMax);
        double incMax = clamp(max + step, clampMin, clampMax);
        String minStr = fmt(min);
        String maxStr = fmt(max);
        return Component.literal(label + " min=" + minStr + " max=" + maxStr + " ")
                .append(button("-min", commandPrefix + " " + fmt(decMin) + " " + fmt(max), "Decrease min"))
                .append(Component.literal(" "))
                .append(button("+min", commandPrefix + " " + fmt(incMin) + " " + fmt(max), "Increase min"))
                .append(Component.literal(" "))
                .append(button("-max", commandPrefix + " " + fmt(min) + " " + fmt(decMax), "Decrease max"))
                .append(Component.literal(" "))
                .append(button("+max", commandPrefix + " " + fmt(min) + " " + fmt(incMax), "Increase max"));
    }

    private static Component rangeLineInt(String label, int min, int max, int step, String commandPrefix, int clampMin) {
        int decMin = Math.max(clampMin, min - step);
        int incMin = Math.max(clampMin, min + step);
        int decMax = Math.max(clampMin, max - step);
        int incMax = Math.max(clampMin, max + step);
        return Component.literal(label + " min=" + min + " max=" + max + " ")
                .append(button("-min", commandPrefix + " " + decMin + " " + max, "Decrease min"))
                .append(Component.literal(" "))
                .append(button("+min", commandPrefix + " " + incMin + " " + max, "Increase min"))
                .append(Component.literal(" "))
                .append(button("-max", commandPrefix + " " + min + " " + decMax, "Decrease max"))
                .append(Component.literal(" "))
                .append(button("+max", commandPrefix + " " + min + " " + incMax, "Increase max"));
    }

    private static Component valueLine(String label, double value, double step, String commandPrefix, double clampMin, double clampMax) {
        double dec = clamp(value - step, clampMin, clampMax);
        double inc = clamp(value + step, clampMin, clampMax);
        return Component.literal(label + " = " + fmt(value) + " ")
                .append(button("-", commandPrefix + " " + fmt(dec), "Decrease"))
                .append(Component.literal(" "))
                .append(button("+", commandPrefix + " " + fmt(inc), "Increase"));
    }

    private static Component valueLineInt(String label, int value, int step, String commandPrefix, int clampMin) {
        int dec = Math.max(clampMin, value - step);
        int inc = Math.max(clampMin, value + step);
        return Component.literal(label + " = " + value + " ")
                .append(button("-", commandPrefix + " " + dec, "Decrease"))
                .append(Component.literal(" "))
                .append(button("+", commandPrefix + " " + inc, "Increase"));
    }

    private static Component button(String label, String command, String hover) {
        return Component.literal("[" + label + "]").withStyle(style ->
                style.withColor(ChatFormatting.AQUA)
                        .withClickEvent(new ClickEvent.RunCommand(command))
                        .withHoverEvent(new HoverEvent.ShowText(Component.literal(hover + " (" + command + ")")))
        );
    }

    private static String fmt(double value) {
        return String.format(Locale.US, "%.3f", value);
    }

    private static double clamp(double value, double min, double max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }
}
