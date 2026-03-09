package net.stracciatella.pathfinding;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
// FAKT: Hier liegen jetzt die Standard-Instanzen (lines, solid, etc.)

// FAKT: Das ist die Klasse für das Objekt selbst
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.stracciatella.module.Module;
import net.stracciatella.pathfinding.commands.PathCommands;
import net.stracciatella.pathfinding.display.PathDisplay;
import net.stracciatella.pathfinding.logic.PathWalker;
import net.stracciatella.pathfinding.test.PathWalkerTests;
import net.stracciatella.testing.runner.TestRunner;
import org.joml.Matrix4f;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class PathfindingModule implements Module {

    @Task(lifeCycle = LifeCycle.STARTED)
    public void init() {
        PathWalker.loadConfig();
        PathDisplay.loadConfig();
        PathDisplay display = new PathDisplay();
        PathCommands commands = new PathCommands();
        commands.register();
        ClientTickEvents.END_CLIENT_TICK.register(PathWalker::tick);
        TestRunner.instance().registerSuite(PathWalkerTests.class);
    }


}
