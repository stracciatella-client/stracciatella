package net.stracciatella.pathfinding.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.Level;
import net.stracciatella.pathfinding.display.PathDisplay;
import net.stracciatella.pathfinding.logic.ChunkMeshBuilder;
import net.stracciatella.pathfinding.logic.mesh.IMeshProvider;
import net.stracciatella.pathfinding.logic.mesh.Mesh;
import net.stracciatella.pathfinding.logic.mesh.MeshNode;
import net.stracciatella.pathfinding.logic.mesh.Neighbor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.Console;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin {

    private static final Logger log = LoggerFactory.getLogger(LevelChunkMixin.class);

    @Inject(method = "<init>(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/ChunkPos;)V", at = @At("TAIL"))
    private void onChunkLoad(net.minecraft.world.level.Level level, net.minecraft.world.level.ChunkPos pos, CallbackInfo ci) {

    }

    @Inject(method = "setBlockState", at = @At("RETURN"))
    private void onBlockUpdate(BlockPos pos, BlockState state, int i, CallbackInfoReturnable<BlockState> cir) {
        // Re-generate or update a specific slice of the mesh when a block changes

    }

    @Shadow
    public abstract Level getLevel();

    @Shadow
    public abstract BlockState getBlockState(BlockPos pos);

    @Unique
    private Mesh pathfindingMesh;




}