package net.stracciatella.pathfinding.display;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.stracciatella.pathfinding.logic.MeshManager;
import net.stracciatella.pathfinding.logic.mesh.Neighbor;
import org.joml.Matrix4f;

public class PathDisplay {

    public static final RenderType LINES_NO_DEPTH = makeCustomLineRenderer();
    public static boolean displayNeighbors = true;

    private static RenderType makeCustomLineRenderer() {
        // 1. Base it on LINES_SNIPPET (Standard MC Lines) so we get thickness & correct uniforms
        RenderPipeline customPipeline = RenderPipeline.builder(
                        new RenderPipeline.Snippet[]{ RenderPipelines.LINES_SNIPPET }
                )
                .withLocation("pathfinding_thick_lines_no_depth") // Unique name
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST) // <--- The magic part
                .withDepthWrite(false)
                .build();

        return RenderType.create(
                "lines_no_depth",
                RenderSetup.builder(customPipeline)
                        .setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
                        .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                        .createRenderSetup()
        );
    }

    public PathDisplay() {
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            MultiBufferSource consumers = context.consumers();
            PoseStack poseStack = context.matrices();

            MeshManager.meshes.forEach((entity, meshes) -> {
                if (meshes != null) {
                    meshes.forEach((chunkCoordinate, mesh) -> {
                        if (mesh != null) {
                            mesh.getNodes().forEach(meshNode -> {
                                renderBlockOutline(poseStack, consumers, meshNode.getBlockPos());

                                if (displayNeighbors) {
                                    for (Neighbor neighbor : meshNode.getNeighbors()) {
                                        drawConnection(poseStack, consumers, meshNode.getBlockPos(), neighbor.getNode().getBlockPos());
                                    }
                                }

                            });
                        }
                    });
                }
            });
        });
    }

    public void renderBlockOutline(PoseStack poseStack, MultiBufferSource consumers, BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        VoxelShape shape = mc.level.getBlockState(pos).getShape(mc.level, pos);
        if (shape.isEmpty()) return;

        VertexConsumer buffer = consumers.getBuffer(RenderTypes.lines());

        poseStack.pushPose();
        Vec3 cameraPos = mc.gameRenderer.getMainCamera().position();
        poseStack.translate(pos.getX() - cameraPos.x, pos.getY() - cameraPos.y, pos.getZ() - cameraPos.z);

        drawBox(poseStack, buffer, shape.bounds(), 1.0F, 1.0F, 0.0F, 1.0F);
        poseStack.popPose();
    }

    private void drawConnection(PoseStack stack, MultiBufferSource consumers, BlockPos pos1, BlockPos pos2) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        // Uses our custom THICK lines renderer
        VertexConsumer buffer = consumers.getBuffer(LINES_NO_DEPTH);

        Vec3 cameraPos = mc.gameRenderer.getMainCamera().position();
        float x1 = (float) ((pos1.getX() + 0.5) - cameraPos.x);
        float y1 = (float) ((pos1.getY() + 0.5) - cameraPos.y);
        float z1 = (float) ((pos1.getZ() + 0.5) - cameraPos.z);
        float x2 = (float) ((pos2.getX() + 0.5) - cameraPos.x);
        float y2 = (float) ((pos2.getY() + 0.5) - cameraPos.y);
        float z2 = (float) ((pos2.getZ() + 0.5) - cameraPos.z);

        stack.pushPose();
        Matrix4f matrix = stack.last().pose();

        // FIX: Use 'drawStandardLine' (which adds Normals) because we are using the standard shader now.
        // This ensures the lines are visible and thick.
        drawStandardLine(buffer, matrix, x1, y1, z1, x2, y2, z2, 0.2F, 0.9F, 1.0F, 1.0F);

        stack.popPose();
    }

    // --- Drawing Helpers ---

    private void drawBox(PoseStack stack, VertexConsumer buffer, AABB box, float r, float g, float b, float a) {
        Matrix4f matrix = stack.last().pose();
        float minX = (float) box.minX; float minY = (float) box.minY; float minZ = (float) box.minZ;
        float maxX = (float) box.maxX; float maxY = (float) box.maxY; float maxZ = (float) box.maxZ;

        // (Simplified for brevity - keep your existing drawBox implementation which calls drawStandardLine)
        drawStandardLine(buffer, matrix, minX, minY, minZ, maxX, minY, minZ, r, g, b, a);
        drawStandardLine(buffer, matrix, maxX, minY, minZ, maxX, minY, maxZ, r, g, b, a);
        drawStandardLine(buffer, matrix, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a);
        drawStandardLine(buffer, matrix, minX, minY, maxZ, minX, minY, minZ, r, g, b, a);
        drawStandardLine(buffer, matrix, minX, maxY, minZ, maxX, maxY, minZ, r, g, b, a);
        drawStandardLine(buffer, matrix, maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, a);
        drawStandardLine(buffer, matrix, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a);
        drawStandardLine(buffer, matrix, minX, maxY, maxZ, minX, maxY, minZ, r, g, b, a);
        drawStandardLine(buffer, matrix, minX, minY, minZ, minX, maxY, minZ, r, g, b, a);
        drawStandardLine(buffer, matrix, maxX, minY, minZ, maxX, maxY, minZ, r, g, b, a);
        drawStandardLine(buffer, matrix, maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, a);
        drawStandardLine(buffer, matrix, minX, minY, maxZ, minX, maxY, maxZ, r, g, b, a);
    }

    /**
     * For RenderTypes.lines() (Vanilla Box)
     * Requires Normal and LineWidth.
     */
    private void drawStandardLine(VertexConsumer buffer, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float b, float a) {
        // We use normals again because LINES_SNIPPET requires them for thickness
        buffer.addVertex(matrix, x1, y1, z1)
                .setColor(r, g, b, a)
                .setNormal(0, 1, 0)
                .setLineWidth(2.0f); // Make them thicker!

        buffer.addVertex(matrix, x2, y2, z2)
                .setColor(r, g, b, a)
                .setNormal(0, 1, 0)
                .setLineWidth(2.0f);
    }

    /**
     * For LINES_NO_DEPTH (Custom Blue Lines)
     * Only accepts Position and Color.
     */
    private void drawSimpleLine(VertexConsumer buffer, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, float r, float g, float b, float a) {
        // Do NOT add normals or line width here.
        buffer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a);
        buffer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a);
    }
}