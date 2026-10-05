package com.pockyl.neon_glowsticks.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

import com.pockyl.neon_glowsticks.Config;
import com.pockyl.neon_glowsticks.NeonGlowsticks;
import com.pockyl.neon_glowsticks.entity.Glowstick;
import com.pockyl.neon_glowsticks.item.GlowColor;
import com.pockyl.neon_glowsticks.registry.ModItems;

import java.util.EnumMap;
import java.util.Map;

/** Draws the stick's 3D item model with its tumbling orientation, plus a soft additive halo in its color. */
public final class GlowstickRenderer extends EntityRenderer<Glowstick> {
    private static final ResourceLocation HALO = NeonGlowsticks.id("textures/entity/glow_halo.png");
    private static final float HALO_SIZE = 0.55F;
    private static final float HALO_STRENGTH = 0.55F;

    private final ItemRenderer itemRenderer;
    private final Map<GlowColor, ItemStack> stacks = new EnumMap<>(GlowColor.class);

    public GlowstickRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(Glowstick stick, float entityYaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        GlowColor color = stick.color();
        pose.pushPose();
        pose.translate(0, stick.getBbHeight() / 2, 0);

        pose.pushPose();
        pose.mulPose(stick.getOrientation(partialTick));
        ItemStack stack = stacks.computeIfAbsent(color, c -> new ItemStack(ModItems.glowstick(c)));
        itemRenderer.renderStatic(stack, ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY, pose, buffers, stick.level(),
                stick.getId());
        pose.popPose();

        if (Config.halo()) {
            float glow = HALO_STRENGTH * (0.25F + 0.75F * stick.brightness());
            pose.mulPose(entityRenderDispatcher.cameraOrientation());
            halo(pose, buffers.getBuffer(RenderType.eyes(HALO)), HALO_SIZE * (0.7F + 0.3F * stick.brightness()),
                    color.red() * glow, color.green() * glow, color.blue() * glow);
        }
        pose.popPose();
        super.render(stick, entityYaw, partialTick, pose, buffers, light);
    }

    /** A camera-facing square; drawn additively, so the color's brightness is its strength. */
    private static void halo(PoseStack pose, VertexConsumer consumer, float size, float r, float g, float b) {
        PoseStack.Pose last = pose.last();
        Matrix4f matrix = last.pose();
        vertex(consumer, last, matrix, -size, -size, 0, 1, r, g, b);
        vertex(consumer, last, matrix, size, -size, 1, 1, r, g, b);
        vertex(consumer, last, matrix, size, size, 1, 0, r, g, b);
        vertex(consumer, last, matrix, -size, size, 0, 0, r, g, b);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose last, Matrix4f matrix, float x, float y, float u, float v,
            float r, float g, float b) {
        consumer.addVertex(matrix, x, y, 0)
                .setColor(r, g, b, 1.0F)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(last, 0, 1, 0);
    }

    @Override
    public ResourceLocation getTextureLocation(Glowstick stick) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
