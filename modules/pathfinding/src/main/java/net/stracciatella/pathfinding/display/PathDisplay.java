package net.stracciatella.pathfinding.display;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.stracciatella.pathfinding.logic.MeshManager;
import org.joml.Matrix4f;

public class PathDisplay {



    public PathDisplay() {
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            MeshManager.meshes.forEach((entity, meshes) -> {
                if (meshes != null) {
                    meshes.forEach((chunkCoordinate, mesh) -> {
                        if (mesh != null) {
                            mesh.getNodes().forEach(meshNode -> {
                                renderBlockOutline(context.matrices(), meshNode.getBlockPos());
                            });
                        }
                    });
                }
            });
        });
    }


    public void renderBlockOutline(PoseStack poseStack, BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.level == null) {
            return;
        }

        VoxelShape shape = mc.level.getBlockState(pos).getShape(mc.level, pos);
        if (shape.isEmpty()) {
            return;
        }

        VertexConsumer buffer = mc.renderBuffers().bufferSource().getBuffer(RenderTypes.lines());

        poseStack.pushPose();

        Vec3 cameraPos = mc.gameRenderer.getMainCamera().position();

        poseStack.translate(
                (double) pos.getX() - cameraPos.x,
                (double) pos.getY() - cameraPos.y,
                (double) pos.getZ() - cameraPos.z
        );

        drawBox(poseStack, buffer, shape.bounds(), 1.0F, 1.0F, 0.0F, 1.0F);

        poseStack.popPose();
    }

    private void drawBox(PoseStack stack, VertexConsumer buffer, AABB box, float r, float g, float b, float a) {
        Matrix4f matrix = stack.last().pose();

        float minX = (float) box.minX;
        float minY = (float) box.minY;
        float minZ = (float) box.minZ;
        float maxX = (float) box.maxX;
        float maxY = (float) box.maxY;
        float maxZ = (float) box.maxZ;

        // Unten
        drawLine(buffer, matrix, minX, minY, minZ, maxX, minY, minZ, r, g, b, a);
        drawLine(buffer, matrix, maxX, minY, minZ, maxX, minY, maxZ, r, g, b, a);
        drawLine(buffer, matrix, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a);
        drawLine(buffer, matrix, minX, minY, maxZ, minX, minY, minZ, r, g, b, a);

        // Oben
        drawLine(buffer, matrix, minX, maxY, minZ, maxX, maxY, minZ, r, g, b, a);
        drawLine(buffer, matrix, maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, a);
        drawLine(buffer, matrix, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a);
        drawLine(buffer, matrix, minX, maxY, maxZ, minX, maxY, minZ, r, g, b, a);

        // Vertikale
        drawLine(buffer, matrix, minX, minY, minZ, minX, maxY, minZ, r, g, b, a);
        drawLine(buffer, matrix, maxX, minY, minZ, maxX, maxY, minZ, r, g, b, a);
        drawLine(buffer, matrix, maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, a);
        drawLine(buffer, matrix, minX, minY, maxZ, minX, maxY, maxZ, r, g, b, a);
    }

    private void drawLine(VertexConsumer buffer, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float b, float a) {
        buffer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setNormal(0, 1, 0).setLineWidth(2);
        buffer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setNormal(0, 1, 0).setLineWidth(2);
    }

}
