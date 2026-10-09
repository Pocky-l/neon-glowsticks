package com.pockyl.neon_glowsticks.client.light;

import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.jetbrains.annotations.Nullable;

import com.pockyl.neon_glowsticks.Config;
import com.pockyl.neon_glowsticks.NeonGlowsticks;
import com.pockyl.neon_glowsticks.entity.Glowstick;
import com.pockyl.neon_glowsticks.item.GlowColor;
import com.pockyl.neon_glowsticks.item.GlowstickItem;
import com.pockyl.neon_glowsticks.light.ColorMixing;
import com.pockyl.neon_glowsticks.light.LightPriority;

import java.util.ArrayList;
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
 * <p>
 * A glowstick held in a hand is a light source too: it shines from the holder's hand and moves with the holder.
 * Sources are keyed by entity and hand, so the two hands of one holder are two lights.
 */
@EventBusSubscriber(modid = NeonGlowsticks.MOD_ID, value = Dist.CLIENT)
public final class DynamicLights {
    /** Re-run the flood fill this often, to notice placed or broken blocks. */
    private static final int REFRESH_INTERVAL = 20;
    /** At most this many sources are (re)built per tick; the rest keep their previous light for a tick. */
    private static final int MAX_BUILDS_PER_TICK = 8;
    private static final Direction[] DIRECTIONS = Direction.values();
    /** A held stick is at this fraction of the holder's height, and this many widths in front of and beside its center. */
    private static final double HAND_HEIGHT = 0.5;
    private static final double HAND_FORWARD = 0.6;
    private static final double HAND_SIDE = 0.6;
    /** How far (in blocks) the hand may leave the block a held light shines from before the light moves. */
    private static final double HELD_ORIGIN_MARGIN = 0.25;

    private static volatile LightSource[] sources = new LightSource[0];
    private static final Map<Long, LightSource> BY_KEY = new HashMap<>();
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

    /**
     * A glowstick that may give light this tick, at {@code pos}. {@code body} is set for a held stick only: the holder's
     * block at hand height, where the light comes from when the hand is inside a wall. {@code own}: held by this player.
     */
    private record Candidate(long key, Vec3 pos, @Nullable BlockPos body, int level, GlowColor color, boolean own) {
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
        float[] mix = {r / total, g / total, b / total};
        ColorMixing.additive(mix);
        float amount = Math.min(1.0F, total * Config.coloredLightStrength()) * (1.0F - 0.5F * sky / 15.0F);
        out[offset] = 1.0F - amount * (1.0F - mix[0]);
        out[offset + 1] = 1.0F - amount * (1.0F - mix[1]);
        out[offset + 2] = 1.0F - amount * (1.0F - mix[2]);
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
            BY_KEY.clear();
            sources = new LightSource[0];
        }
    }

    private static void update() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level != lastLevel) {
            BY_KEY.clear();
            sources = new LightSource[0];
            lastLevel = level;
        }
        if (level == null) {
            return;
        }
        long time = level.getGameTime();
        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
        List<Candidate> candidates = LightPriority.select(candidates(level, minecraft.player, camera), Candidate::own,
                candidate -> candidate.pos().distanceToSqr(camera), Config.maxLights());
        Map<Long, LightSource> previous = new HashMap<>(BY_KEY);
        BY_KEY.clear();
        int builds = 0;
        for (Candidate candidate : candidates) {
            LightSource old = previous.remove(candidate.key());
            BlockPos origin = candidate.body() != null ? heldOrigin(level, candidate, old) : BlockPos.containing(candidate.pos());
            boolean changed = old == null || old.origin() != origin.asLong() || old.level() != candidate.level()
                    || old.color() != candidate.color();
            boolean refresh = old != null && time - old.builtAt() >= REFRESH_INTERVAL;
            if ((changed || refresh) && (old == null || builds < MAX_BUILDS_PER_TICK)) {
                builds++;
                LightSource built = build(level, origin, candidate.level(), candidate.color(), time);
                if (changed || !built.light().equals(old.light())) {
                    markDirty(old);
                    markDirty(built);
                }
                BY_KEY.put(candidate.key(), built);
            } else {
                BY_KEY.put(candidate.key(), old);
            }
        }
        // Sticks that are gone, picked up, burnt out or put away: their light disappears.
        for (LightSource gone : previous.values()) {
            markDirty(gone);
        }
        sources = BY_KEY.values().toArray(new LightSource[0]);
    }

    /** Every glowstick that could give light now: thrown ones and, unless turned off, those held in a hand. */
    private static List<Candidate> candidates(ClientLevel level, @Nullable LocalPlayer player, Vec3 camera) {
        List<Candidate> candidates = new ArrayList<>();
        boolean held = Config.heldLight();
        for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof Glowstick stick) {
                int lightLevel = stick.lightLevel();
                if (stick.isAlive() && lightLevel > 0) {
                    candidates.add(new Candidate((long) stick.getId() << 2, stick.position(), null, lightLevel, stick.color(), false));
                }
            } else if (held && entity instanceof LivingEntity holder && holdsLight(holder, camera)) {
                addHeld(candidates, holder, InteractionHand.MAIN_HAND, holder == player);
                addHeld(candidates, holder, InteractionHand.OFF_HAND, holder == player);
            }
        }
        return candidates;
    }

    /** Holders the player cannot see, or whose glowstick is in lava, give no light. */
    private static boolean holdsLight(LivingEntity holder, Vec3 camera) {
        return holder.isAlive() && !holder.isSpectator() && !holder.isInvisible() && !holder.isInLava()
                && holder.shouldRenderAtSqrDistance(holder.distanceToSqr(camera));
    }

    private static void addHeld(List<Candidate> candidates, LivingEntity holder, InteractionHand hand, boolean own) {
        ItemStack stack = holder.getItemInHand(hand);
        if (!(stack.getItem() instanceof GlowstickItem item)) {
            return;
        }
        // Holding a stick never uses up its glow, but a picked-up one that was already dimming stays dim.
        int lightLevel = Glowstick.lightLevel(Config.heldLightLevel(), GlowstickItem.glowLeft(stack));
        if (lightLevel <= 0) {
            return;
        }
        HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? holder.getMainArm() : holder.getMainArm().getOpposite();
        long key = (long) holder.getId() << 2 | (hand == InteractionHand.MAIN_HAND ? 1 : 2);
        double handY = holder.getY() + holder.getBbHeight() * HAND_HEIGHT;
        BlockPos body = BlockPos.containing(holder.getX(), handY, holder.getZ());
        candidates.add(new Candidate(key, handPosition(holder, arm, handY), body, lightLevel, item.color(), own));
    }

    /** Roughly where the hand on {@code arm} is: a bit in front of the holder and to that side. */
    private static Vec3 handPosition(LivingEntity holder, HumanoidArm arm, double handY) {
        float yaw = holder.yBodyRot * Mth.DEG_TO_RAD;
        double sin = Mth.sin(yaw);
        double cos = Mth.cos(yaw);
        double forward = holder.getBbWidth() * HAND_FORWARD;
        double side = holder.getBbWidth() * HAND_SIDE * (arm == HumanoidArm.RIGHT ? 1 : -1);
        return new Vec3(holder.getX() - sin * forward - cos * side, handY, holder.getZ() + cos * forward - sin * side);
    }

    /**
     * The block a held stick shines from. It stays in the previous block until the hand is clearly out of it, so turning
     * around on a block edge does not rebuild the light every tick. A hand pushed into a wall shines from the holder's
     * own block instead, so the light does not leak out on the other side.
     */
    private static BlockPos heldOrigin(ClientLevel level, Candidate candidate, @Nullable LightSource old) {
        Vec3 hand = candidate.pos();
        if (old != null) {
            BlockPos previous = BlockPos.of(old.origin());
            if (new AABB(previous).inflate(HELD_ORIGIN_MARGIN).contains(hand) && !opaque(level, previous)) {
                return previous;
            }
        }
        BlockPos pos = BlockPos.containing(hand);
        return opaque(level, pos) ? candidate.body() : pos;
    }

    private static boolean opaque(ClientLevel level, BlockPos pos) {
        return level.getBlockState(pos).getLightBlock(level, pos) >= 15;
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
