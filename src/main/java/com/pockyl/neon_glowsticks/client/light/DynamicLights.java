package com.pockyl.neon_glowsticks.client.light;

import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import com.pockyl.neon_glowsticks.Config;
import com.pockyl.neon_glowsticks.NeonGlowsticks;
import com.pockyl.neon_glowsticks.entity.Glowstick;
import com.pockyl.neon_glowsticks.item.GlowColor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Client-side colored dynamic light of glowsticks.
 * <p>
 * Every glowstick is a light source whose light spreads through the world like vanilla block light (a flood fill that
 * walls stop), computed on the client only. The light is fed into the world rendering in two places:
 * <ul>
 *     <li>brightness — the block light of every position is raised to the glowstick's light, so blocks, block
 *     entities and mobs around it are lit;</li>
 *     <li>color — when chunk sections are meshed, each block vertex is tinted towards the colors of the lights reaching
 *     it, so the light blends smoothly with vanilla shading.</li>
 * </ul>
 * Whenever a source moves to another block, changes its strength or the blocks around it change, the chunk sections
 * it reaches are re-meshed. Chunk meshing runs on worker threads, so the sources are published as an immutable
 * snapshot.
 */
@EventBusSubscriber(modid = NeonGlowsticks.MOD_ID, value = Dist.CLIENT)
public final class DynamicLights {
    /** Re-run the flood fill this often, to notice placed or broken blocks. */
    private static final int REFRESH_INTERVAL = 20;
    /** At most this many sources are (re)built per tick; the rest keep their previous light for a tick. */
    private static final int MAX_BUILDS_PER_TICK = 8;
    private static final Direction[] DIRECTIONS = Direction.values();

    private static volatile LightSource[] sources = new LightSource[0];
    private static final Map<Integer, LightSource> BY_ENTITY = new HashMap<>();
    private static ClientLevel lastLevel;
    private static boolean failed;

    private DynamicLights() {
    }

    /** One glowstick's light: the flood-filled light level of every block it reaches. */
    record LightSource(long origin, int level, GlowColor color, Long2ByteOpenHashMap light, long builtAt,
            int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

        int lightAt(long pos) {
            return light.get(pos);
        }

        boolean reaches(int x, int y, int z) {
            return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
        }
    }

    // ------------------------------------------------------------------------------------------------
    // Queries (any thread)
    // ------------------------------------------------------------------------------------------------

    /** Brightest glowstick light at a block position, 0 if none. */
    public static int blockLight(BlockPos pos) {
        LightSource[] current = sources;
        if (current.length == 0) {
            return 0;
        }
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        long key = BlockPos.asLong(x, y, z);
        int light = 0;
        for (LightSource source : current) {
            if (source.reaches(x, y, z)) {
                light = Math.max(light, source.lightAt(key));
            }
        }
        return light;
    }

    /** Whether any glowstick light reaches the block at this position (or right next to it). */
    public static boolean anyNear(BlockPos pos) {
        LightSource[] current = sources;
        for (LightSource source : current) {
            if (pos.getX() >= source.minX() - 1 && pos.getX() <= source.maxX() + 1
                    && pos.getY() >= source.minY() - 1 && pos.getY() <= source.maxY() + 1
                    && pos.getZ() >= source.minZ() - 1 && pos.getZ() <= source.maxZ() + 1) {
                return true;
            }
        }
        return false;
    }

    /**
     * Writes the color multiplier of a vertex into {@code out} (r, g, b): white when no glowstick light reaches it,
     * towards the lights' colors where they are strong. The light is sampled in the four blocks the vertex touches on
     * the side its face looks at, like vanilla smooth lighting.
     *
     * @param x   vertex position in the world
     * @param sky sky light of the vertex (0-15); colored light is weaker where daylight can reach
     */
    public static void tint(float x, float y, float z, Direction face, int sky, float[] out, int offset) {
        out[offset] = 1.0F;
        out[offset + 1] = 1.0F;
        out[offset + 2] = 1.0F;
        LightSource[] current = sources;
        if (current.length == 0 || !Config.coloredLight()) {
            return;
        }
        // A point half a block in front of the face; the 4 blocks around it in the face's plane.
        float px = x + face.getStepX() * 0.5F;
        float py = y + face.getStepY() * 0.5F;
        float pz = z + face.getStepZ() * 0.5F;
        Direction.Axis axis = face.getAxis();
        float total = 0;
        float r = 0;
        float g = 0;
        float b = 0;
        for (LightSource source : current) {
            int sum = 0;
            int count = 0;
            for (int i = 0; i < 4; i++) {
                float du = (i & 1) == 0 ? -0.5F : 0.5F;
                float dv = (i & 2) == 0 ? -0.5F : 0.5F;
                int bx = floor(px + (axis == Direction.Axis.X ? 0 : du));
                int by = floor(py + (axis == Direction.Axis.Y ? 0 : axis == Direction.Axis.X ? du : dv));
                int bz = floor(pz + (axis == Direction.Axis.Z ? 0 : dv));
                if (!source.reaches(bx, by, bz)) {
                    continue;
                }
                int light = source.lightAt(BlockPos.asLong(bx, by, bz));
                if (light > 0) {
                    sum += light;
                    count++;
                }
            }
            if (count == 0) {
                continue;
            }
            float strength = sum / (float) count / 15.0F;
            total += strength;
            r += strength * source.color().red();
            g += strength * source.color().green();
            b += strength * source.color().blue();
        }
        if (total < 0.01F) {
            return;
        }
        float amount = Math.min(1.0F, total * Config.coloredLightStrength()) * (1.0F - 0.5F * sky / 15.0F);
        out[offset] = 1.0F - amount * (1.0F - r / total);
        out[offset + 1] = 1.0F - amount * (1.0F - g / total);
        out[offset + 2] = 1.0F - amount * (1.0F - b / total);
    }

    private static int floor(float value) {
        int i = (int) value;
        return value < i ? i - 1 : i;
    }

    // ------------------------------------------------------------------------------------------------
    // Updates (client thread)
    // ------------------------------------------------------------------------------------------------

    // Never let the light break the game: on any error the lights are dropped and the problem is logged once.
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        try {
            update();
        } catch (RuntimeException e) {
            if (!failed) {
                failed = true;
                NeonGlowsticks.LOGGER.error("Glowstick lights failed to update; they are turned off for now", e);
            }
            BY_ENTITY.clear();
            sources = new LightSource[0];
        }
    }

    private static void update() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level != lastLevel) {
            BY_ENTITY.clear();
            sources = new LightSource[0];
            lastLevel = level;
        }
        if (level == null) {
            return;
        }
        long time = level.getGameTime();
        Map<Integer, LightSource> previous = new HashMap<>(BY_ENTITY);
        BY_ENTITY.clear();
        int builds = 0;
        for (Glowstick stick : nearestSticks(level, minecraft.gameRenderer.getMainCamera().getPosition())) {
            int lightLevel = stick.lightLevel();
            if (lightLevel <= 0) {
                continue;
            }
            LightSource old = previous.remove(stick.getId());
            long origin = stick.blockPosition().asLong();
            boolean changed = old == null || old.origin() != origin || old.level() != lightLevel || old.color() != stick.color();
            boolean refresh = old != null && time - old.builtAt() >= REFRESH_INTERVAL;
            if ((changed || refresh) && (old == null || builds < MAX_BUILDS_PER_TICK)) {
                builds++;
                LightSource built = build(level, stick.blockPosition(), lightLevel, stick.color(), time);
                if (changed || !built.light().equals(old.light())) {
                    markDirty(old);
                    markDirty(built);
                }
                BY_ENTITY.put(stick.getId(), built);
            } else {
                BY_ENTITY.put(stick.getId(), old);
            }
        }
        // Sticks that are gone, picked up or burnt out: their light disappears.
        for (LightSource gone : previous.values()) {
            markDirty(gone);
        }
        sources = BY_ENTITY.values().toArray(new LightSource[0]);
    }

    private static List<Glowstick> nearestSticks(ClientLevel level, Vec3 camera) {
        List<Glowstick> sticks = new ArrayList<>();
        for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof Glowstick stick && stick.isAlive()) {
                sticks.add(stick);
            }
        }
        sticks.sort(Comparator.comparingDouble(stick -> stick.position().distanceToSqr(camera)));
        int max = Config.maxLights();
        return sticks.size() > max ? sticks.subList(0, max) : sticks;
    }

    /** Spreads the light like vanilla block light: one level less per block, blocked by opaque blocks. */
    private static LightSource build(ClientLevel level, BlockPos origin, int lightLevel, GlowColor color, long time) {
        Long2ByteOpenHashMap light = new Long2ByteOpenHashMap();
        LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        light.put(origin.asLong(), (byte) lightLevel);
        queue.enqueue(origin.asLong());
        while (!queue.isEmpty()) {
            long current = queue.dequeueLong();
            int value = light.get(current);
            if (value <= 1) {
                continue;
            }
            for (Direction direction : DIRECTIONS) {
                pos.set(current).move(direction);
                long next = pos.asLong();
                BlockState state = level.getBlockState(pos);
                int spread = value - Math.max(1, state.getLightBlock(level, pos));
                if (spread > light.get(next)) {
                    light.put(next, (byte) spread);
                    queue.enqueue(next);
                }
            }
        }
        int reach = lightLevel;
        return new LightSource(origin.asLong(), lightLevel, color, light, time,
                origin.getX() - reach, origin.getY() - reach, origin.getZ() - reach,
                origin.getX() + reach, origin.getY() + reach, origin.getZ() + reach);
    }

    /** Re-meshes every chunk section the light reaches (plus one block, for smooth lighting at the edges). */
    private static void markDirty(LightSource source) {
        if (source == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        int minX = SectionPos.blockToSectionCoord(source.minX() - 1);
        int minY = SectionPos.blockToSectionCoord(source.minY() - 1);
        int minZ = SectionPos.blockToSectionCoord(source.minZ() - 1);
        int maxX = SectionPos.blockToSectionCoord(source.maxX() + 1);
        int maxY = SectionPos.blockToSectionCoord(source.maxY() + 1);
        int maxZ = SectionPos.blockToSectionCoord(source.maxZ() + 1);
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    minecraft.levelRenderer.setSectionDirty(x, y, z);
                }
            }
        }
    }
}
