package com.pockyl.neon_glowsticks.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import com.pockyl.neon_glowsticks.Config;
import com.pockyl.neon_glowsticks.NeonGlowsticks;
import com.pockyl.neon_glowsticks.block.GlowLightBlock;
import com.pockyl.neon_glowsticks.entity.Glowstick;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Colored light. Minecraft's light engine only knows one color, so the real (white) light comes from the invisible
 * light block, and the color is painted on top: every block face a glowstick can see gets an additive quad whose
 * vertices fade with distance and angle to the stick, like a point light.
 * <p>
 * Which faces a stick can see is found with ray casts and cached until the stick moves to another block (or every
 * couple of seconds, to catch block changes); the falloff is computed every frame from the interpolated position, so a
 * flying stick lights its surroundings smoothly.
 */
@EventBusSubscriber(modid = NeonGlowsticks.MOD_ID, value = Dist.CLIENT)
public final class SurfaceGlow {
    private static final RenderType GLOW = RenderType.create(NeonGlowsticks.MOD_ID + "_surface_glow", DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS, 1536, false, false, RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setTransparencyState(RenderStateShard.LIGHTNING_TRANSPARENCY)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setLayeringState(RenderStateShard.POLYGON_OFFSET_LAYERING)
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false));
    private static final int REBUILD_INTERVAL = 40;
    private static final int MAX_REBUILDS_PER_TICK = 6;
    private static final double MAX_DISTANCE = 72.0;
    /** Lifts the quads off the surface; the polygon offset does the rest. */
    private static final float SURFACE_OFFSET = 0.004F;
    /** Floats per face: 4 corners (xyz), normal (xyz), visibility of each corner, sky light. */
    private static final int STRIDE = 12 + 3 + 4 + 1;

    private static final Map<Integer, Lit> LIT = new HashMap<>();

    private SurfaceGlow() {
    }

    /** The faces lit by one stick. */
    private static final class Lit {
        final BlockPos center;
        final long builtAt;
        final float[] faces;
        final int count;

        Lit(BlockPos center, long builtAt, float[] faces, int count) {
            this.center = center;
            this.builtAt = builtAt;
            this.faces = faces;
            this.count = count;
        }
    }

    // ------------------------------------------------------------------------------------------------
    // Cache
    // ------------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || !Config.coloredLight()) {
            LIT.clear();
            return;
        }
        List<Glowstick> sticks = nearbySticks(level, minecraft.gameRenderer.getMainCamera().getPosition());
        Set<Integer> alive = new HashSet<>();
        int rebuilds = 0;
        long time = level.getGameTime();
        int radius = Config.coloredLightRadius();
        for (Glowstick stick : sticks) {
            alive.add(stick.getId());
            Lit lit = LIT.get(stick.getId());
            BlockPos center = stick.blockPosition();
            boolean stale = lit == null || !lit.center.equals(center) || time - lit.builtAt >= REBUILD_INTERVAL;
            // Sticks without any cache go first; the rest wait for a free slot.
            if (stale && (lit == null || rebuilds < MAX_REBUILDS_PER_TICK)) {
                LIT.put(stick.getId(), build(level, lightOrigin(stick.position(), stick), center, radius, time));
                rebuilds++;
            }
        }
        LIT.keySet().retainAll(alive);
    }

    /** The nearest glowsticks that cast colored light, closest first. */
    private static List<Glowstick> nearbySticks(ClientLevel level, Vec3 camera) {
        List<Glowstick> sticks = new ArrayList<>();
        for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof Glowstick stick && stick.isAlive() && stick.position().distanceToSqr(camera) < MAX_DISTANCE * MAX_DISTANCE) {
                sticks.add(stick);
            }
        }
        sticks.sort(Comparator.comparingDouble(stick -> stick.position().distanceToSqr(camera)));
        int max = Config.maxColoredLights();
        return sticks.size() > max ? sticks.subList(0, max) : sticks;
    }

    private static Vec3 lightOrigin(Vec3 position, Glowstick stick) {
        return position.add(0, stick.getBbHeight() / 2 + 0.05, 0);
    }

    /** Collects the faces around {@code light} that face it and that it can see. */
    private static Lit build(ClientLevel level, Vec3 light, BlockPos center, int radius, long time) {
        float[] faces = new float[STRIDE * 256];
        int count = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos neighborPos = new BlockPos.MutableBlockPos();
        int reach = radius + 1;
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dy = -reach; dy <= reach; dy++) {
                for (int dz = -reach; dz <= reach; dz++) {
                    if (dx * dx + dy * dy + dz * dz > reach * reach) {
                        continue;
                    }
                    pos.setWithOffset(center, dx, dy, dz);
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir() || state.getBlock() instanceof GlowLightBlock || state.getRenderShape() == RenderShape.INVISIBLE) {
                        continue;
                    }
                    VoxelShape shape = state.getShape(level, pos);
                    if (shape.isEmpty()) {
                        continue;
                    }
                    AABB box = shape.bounds();
                    for (Direction side : Direction.values()) {
                        if (faces.length < (count + 1) * STRIDE) {
                            faces = Arrays.copyOf(faces, faces.length * 2);
                        }
                        if (addFace(level, light, pos, box, side, neighborPos, faces, count * STRIDE)) {
                            count++;
                        }
                    }
                }
            }
        }
        return new Lit(center.immutable(), time, faces, count);
    }

    private static boolean addFace(ClientLevel level, Vec3 light, BlockPos pos, AABB box, Direction side,
            BlockPos.MutableBlockPos neighborPos, float[] out, int offset) {
        Direction.Axis axis = side.getAxis();
        boolean positive = side.getAxisDirection() == Direction.AxisDirection.POSITIVE;
        double plane = positive ? box.max(axis) : box.min(axis);
        boolean onBoundary = positive ? plane > 0.9999 : plane < 0.0001;
        neighborPos.setWithOffset(pos, side);
        if (onBoundary) {
            BlockState neighbor = level.getBlockState(neighborPos);
            if (neighbor.isSolidRender(level, neighborPos)) {
                return false;
            }
        }
        float nx = side.getStepX();
        float ny = side.getStepY();
        float nz = side.getStepZ();
        // The two in-plane axes of this face.
        Direction.Axis uAxis = axis == Direction.Axis.X ? Direction.Axis.Y : Direction.Axis.X;
        Direction.Axis vAxis = axis == Direction.Axis.Z ? Direction.Axis.Y : Direction.Axis.Z;
        double[][] corners = {
                {box.min(uAxis), box.min(vAxis)}, {box.max(uAxis), box.min(vAxis)},
                {box.max(uAxis), box.max(vAxis)}, {box.min(uAxis), box.max(vAxis)}};
        Vec3 faceCenter = point(pos, axis, plane, uAxis, (box.min(uAxis) + box.max(uAxis)) / 2, vAxis, (box.min(vAxis) + box.max(vAxis)) / 2);
        Vec3 toLight = light.subtract(faceCenter);
        if (toLight.x * nx + toLight.y * ny + toLight.z * nz <= 0.01) {
            return false;
        }
        boolean anyVisible = false;
        for (int i = 0; i < 4; i++) {
            Vec3 corner = point(pos, axis, plane, uAxis, corners[i][0], vAxis, corners[i][1]);
            Vec3 lifted = corner.add(nx * SURFACE_OFFSET, ny * SURFACE_OFFSET, nz * SURFACE_OFFSET);
            out[offset + i * 3] = (float) lifted.x;
            out[offset + i * 3 + 1] = (float) lifted.y;
            out[offset + i * 3 + 2] = (float) lifted.z;
            // Probe slightly inside the face and off its surface, so the face's own block does not block the ray.
            Vec3 probe = corner.add(faceCenter.subtract(corner).scale(0.15)).add(nx * 0.05, ny * 0.05, nz * 0.05);
            boolean visible = canSee(level, light, probe);
            out[offset + 15 + i] = visible ? 1.0F : 0.0F;
            anyVisible |= visible;
        }
        if (!anyVisible) {
            return false;
        }
        out[offset + 12] = nx;
        out[offset + 13] = ny;
        out[offset + 14] = nz;
        out[offset + 19] = level.getBrightness(LightLayer.SKY, onBoundary ? neighborPos : pos);
        return true;
    }

    private static Vec3 point(BlockPos pos, Direction.Axis axis, double plane, Direction.Axis uAxis, double u, Direction.Axis vAxis,
            double v) {
        double[] xyz = new double[3];
        xyz[axis.ordinal()] = plane;
        xyz[uAxis.ordinal()] = u;
        xyz[vAxis.ordinal()] = v;
        return new Vec3(pos.getX() + xyz[0], pos.getY() + xyz[1], pos.getZ() + xyz[2]);
    }

    private static boolean canSee(ClientLevel level, Vec3 from, Vec3 to) {
        return level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()))
                .getType() == HitResult.Type.MISS;
    }

    // ------------------------------------------------------------------------------------------------
    // Rendering
    // ------------------------------------------------------------------------------------------------

    // Drawn after the opaque and cutout blocks, before entities and translucent blocks: water and glass in front of a
    // lit wall correctly cover its glow, and the quads write no depth, so nothing else is affected.
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS || LIT.isEmpty() || !Config.coloredLight()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }
        Vec3 camera = event.getCamera().getPosition();
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float strength = Config.coloredLightStrength();
        float radius = Config.coloredLightRadius();
        int skyDarken = level.getSkyDarken();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer consumer = buffers.getBuffer(GLOW);
        Matrix4f matrix = event.getPoseStack().last().pose();
        for (Map.Entry<Integer, Lit> entry : LIT.entrySet()) {
            if (!(level.getEntity(entry.getKey()) instanceof Glowstick stick)) {
                continue;
            }
            Vec3 light = lightOrigin(stick.getPosition(partialTick), stick);
            float distanceFade = (float) Mth.clamp(1.5 - light.distanceTo(camera) / MAX_DISTANCE * 1.5, 0.0, 1.0);
            float power = strength * (0.2F + 0.8F * stick.brightness()) * distanceFade;
            if (power <= 0.01F) {
                continue;
            }
            draw(consumer, matrix, entry.getValue(), light, camera, radius, power, skyDarken, stick);
        }
        buffers.endBatch(GLOW);
    }

    private static void draw(VertexConsumer consumer, Matrix4f matrix, Lit lit, Vec3 light, Vec3 camera, float radius, float power,
            int skyDarken, Glowstick stick) {
        float r = stick.color().red();
        float g = stick.color().green();
        float b = stick.color().blue();
        float lx = (float) light.x;
        float ly = (float) light.y;
        float lz = (float) light.z;
        float[] f = lit.faces;
        for (int face = 0; face < lit.count; face++) {
            int o = face * STRIDE;
            // Daylight washes the colored light out, like a real glowstick in the sun.
            float daylight = Math.max(0, f[o + 19] - skyDarken) / 15.0F;
            float facePower = power * (1.0F - 0.85F * daylight);
            if (facePower <= 0.01F) {
                continue;
            }
            float cx = (f[o] + f[o + 6]) / 2 - lx;
            float cy = (f[o + 1] + f[o + 7]) / 2 - ly;
            float cz = (f[o + 2] + f[o + 8]) / 2 - lz;
            float centerDistance = Mth.sqrt(cx * cx + cy * cy + cz * cz);
            if (centerDistance > radius + 1.0F) {
                continue;
            }
            // Faces right next to the stick get a finer grid, so the bright spot under it is round instead of a square.
            int grid = centerDistance < 1.5F ? 4 : centerDistance < 3.0F ? 2 : 1;
            emitFace(consumer, matrix, f, o, grid, lx, ly, lz, camera, radius, facePower, r, g, b);
        }
    }

    private static void emitFace(VertexConsumer consumer, Matrix4f matrix, float[] f, int o, int grid, float lx, float ly, float lz,
            Vec3 camera, float radius, float power, float r, float g, float b) {
        int size = grid + 1;
        float[] xs = new float[size * size];
        float[] ys = new float[size * size];
        float[] zs = new float[size * size];
        float[] alpha = new float[size * size];
        float nx = f[o + 12];
        float ny = f[o + 13];
        float nz = f[o + 14];
        boolean lit = false;
        for (int j = 0; j < size; j++) {
            float t = j / (float) grid;
            for (int i = 0; i < size; i++) {
                float s = i / (float) grid;
                // Bilinear over corners 0-1-2-3 (going around the face).
                float w0 = (1 - s) * (1 - t);
                float w1 = s * (1 - t);
                float w2 = s * t;
                float w3 = (1 - s) * t;
                int k = j * size + i;
                xs[k] = w0 * f[o] + w1 * f[o + 3] + w2 * f[o + 6] + w3 * f[o + 9];
                ys[k] = w0 * f[o + 1] + w1 * f[o + 4] + w2 * f[o + 7] + w3 * f[o + 10];
                zs[k] = w0 * f[o + 2] + w1 * f[o + 5] + w2 * f[o + 8] + w3 * f[o + 11];
                float visibility = w0 * f[o + 15] + w1 * f[o + 16] + w2 * f[o + 17] + w3 * f[o + 18];
                float dx = lx - xs[k];
                float dy = ly - ys[k];
                float dz = lz - zs[k];
                float distance = Math.max(Mth.sqrt(dx * dx + dy * dy + dz * dz), 1.0E-3F);
                float falloff = Mth.clamp(1.0F - distance / radius, 0.0F, 1.0F);
                float facing = Math.max(0.0F, (dx * nx + dy * ny + dz * nz) / distance);
                alpha[k] = Math.min(1.0F, power * falloff * falloff * (0.3F + 0.7F * facing) * visibility);
                lit |= alpha[k] > 0.004F;
            }
        }
        if (!lit) {
            return;
        }
        float camX = (float) camera.x;
        float camY = (float) camera.y;
        float camZ = (float) camera.z;
        for (int j = 0; j < grid; j++) {
            for (int i = 0; i < grid; i++) {
                int k0 = j * size + i;
                int k1 = k0 + 1;
                int k2 = k1 + size;
                int k3 = k0 + size;
                for (int k : new int[] {k0, k1, k2, k3}) {
                    consumer.addVertex(matrix, xs[k] - camX, ys[k] - camY, zs[k] - camZ).setColor(r, g, b, alpha[k]);
                }
            }
        }
    }
}
