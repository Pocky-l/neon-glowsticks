package com.pockyl.neon_glowsticks.mixin.embeddium;

import me.jellysquid.mods.sodium.client.model.light.data.QuadLightData;
import me.jellysquid.mods.sodium.client.model.quad.BakedQuadView;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderContext;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.Material;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.pockyl.neon_glowsticks.client.light.DynamicLights;

/**
 * Embeddium meshes chunks with its own block renderer, which skips {@code ModelBlockRendererMixin}: this colors its quads
 * the same way as vanilla meshing. The brightness needs no hook, Embeddium reads it through {@code LevelRenderer.getLightColor}.
 * Only applied when Embeddium is installed ({@link EmbeddiumMixinPlugin}).
 */
@Mixin(value = BlockRenderer.class, remap = false)
abstract class EmbeddiumBlockRendererMixin {
    @Shadow
    @Final
    private ChunkVertexEncoder.Vertex[] vertices;

    // One renderer per chunk-build thread, so plain fields are enough.
    @Unique
    private final BlockPos.MutableBlockPos neon_glowsticks$offset = new BlockPos.MutableBlockPos();
    @Unique
    private final float[] neon_glowsticks$tints = new float[12];
    @Unique
    private boolean neon_glowsticks$near;

    @Inject(method = "renderModel", at = @At("HEAD"))
    private void neon_glowsticks$startBlock(BlockRenderContext ctx, ChunkBuildBuffers buffers, CallbackInfo ci) {
        // Vertex positions are relative to the chunk section, the block's corner is at origin: this turns them into world ones.
        BlockPos pos = ctx.pos();
        Vector3fc origin = ctx.origin();
        neon_glowsticks$offset.set(pos.getX() - (int) origin.x(), pos.getY() - (int) origin.y(), pos.getZ() - (int) origin.z());
        neon_glowsticks$near = DynamicLights.anyNear(pos);
    }

    // Runs once the vertices of a quad are filled in, before they go to the mesh (opaque and translucent alike).
    @Inject(method = "writeGeometry", at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/render/chunk/vertex/builder/"
            + "ChunkMeshBufferBuilder;push([Lme/jellysquid/mods/sodium/client/render/chunk/vertex/format/ChunkVertexEncoder$Vertex;"
            + "Lme/jellysquid/mods/sodium/client/render/chunk/terrain/material/Material;)V"))
    private void neon_glowsticks$tint(BlockRenderContext ctx, ChunkModelBuilder builder, Vec3 offset, Material material, BakedQuadView quad,
            int[] colors, QuadLightData light, CallbackInfo ci) {
        if (!neon_glowsticks$near) {
            return;
        }
        Direction face = neon_glowsticks$direction(quad.getNormalFace());
        float[] tints = neon_glowsticks$tints;
        for (int i = 0; i < 4; i++) {
            ChunkVertexEncoder.Vertex vertex = vertices[i];
            DynamicLights.tint(vertex.x + neon_glowsticks$offset.getX(), vertex.y + neon_glowsticks$offset.getY(),
                    vertex.z + neon_glowsticks$offset.getZ(), face, vertex.light >>> 20 & 0xF, tints, i * 3);
            // ABGR: red in the lowest byte.
            int color = vertex.color;
            int red = (int) ((color & 0xFF) * tints[i * 3]);
            int green = (int) ((color >>> 8 & 0xFF) * tints[i * 3 + 1]);
            int blue = (int) ((color >>> 16 & 0xFF) * tints[i * 3 + 2]);
            vertex.color = color & 0xFF000000 | blue << 16 | green << 8 | red;
        }
    }

    @Unique
    private static Direction neon_glowsticks$direction(ModelQuadFacing facing) {
        return switch (facing) {
            case POS_X -> Direction.EAST;
            case NEG_X -> Direction.WEST;
            case POS_Y -> Direction.UP;
            case NEG_Y -> Direction.DOWN;
            case POS_Z -> Direction.SOUTH;
            case NEG_Z -> Direction.NORTH;
            default -> Direction.UP;
        };
    }
}
