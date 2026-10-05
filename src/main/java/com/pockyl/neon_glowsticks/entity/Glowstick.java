package com.pockyl.neon_glowsticks.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.pockyl.neon_glowsticks.Config;
import com.pockyl.neon_glowsticks.block.GlowLightBlock;
import com.pockyl.neon_glowsticks.item.GlowColor;
import com.pockyl.neon_glowsticks.item.GlowstickItem;
import com.pockyl.neon_glowsticks.registry.ModDataComponents;
import com.pockyl.neon_glowsticks.registry.ModEntities;
import com.pockyl.neon_glowsticks.registry.ModItems;
import com.pockyl.neon_glowsticks.registry.ModSounds;

import java.util.Objects;

/**
 * A cracked glowstick in the world. It flies, bounces off blocks and mobs, rolls and comes to rest lying on one of its
 * flat sides, keeps an invisible light block next to itself and burns out after a while. Players pick it up with
 * right-click and knock it around with left-click.
 * <p>
 * The physics run on both sides, so the client sees a smooth simulation instead of interpolated server positions; the
 * tumbling orientation exists only on the client.
 */
public final class Glowstick extends Projectile {
    /** The glow dims over this many final ticks. */
    public static final int FADE_TICKS = 1200;
    private static final double GRAVITY = 0.05;
    private static final double WATER_GRAVITY = 0.012;
    private static final double AIR_DRAG = 0.99;
    private static final double WATER_DRAG = 0.85;
    private static final double GROUND_FRICTION = 0.82;
    private static final double RESTITUTION = 0.45;
    /** Bounces slower than this along an axis just stop instead of jittering. */
    private static final double MIN_BOUNCE = 0.09;
    private static final double BOUNCE_SOUND_IMPACT = 0.12;
    private static final int ENTITY_HIT_COOLDOWN = 10;
    private static final int GLOW_SYNC_INTERVAL = 20;
    private static final int LIGHT_REFRESH_INTERVAL = 10;

    private static final EntityDataAccessor<Byte> COLOR = SynchedEntityData.defineId(Glowstick.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> GLOW_LEFT = SynchedEntityData.defineId(Glowstick.class, EntityDataSerializers.INT);

    private int glowLeft = Integer.MAX_VALUE;
    /** Server only: where this stick's light block is, if any. */
    @Nullable
    private BlockPos lightPos;
    private int lastLight;
    private int lastHitEntity = -1;
    private int lastHitTick;

    // Client-only rigid body: orientation (the stick's long axis is the body's Y axis) and angular velocity in radians per
    // tick, both in world space. The previous orientation is kept for interpolation.
    private final Quaternionf orientation = new Quaternionf();
    private final Quaternionf orientationO = new Quaternionf();
    private final Vector3f spin = new Vector3f();
    private boolean poseStarted;
    /** Client-only: remaining offset to the server position, applied gradually instead of snapping. */
    private Vec3 correction = Vec3.ZERO;

    public Glowstick(EntityType<? extends Glowstick> type, Level level) {
        super(type, level);
    }

    /** A stick thrown by an entity, taking color and remaining glow from the item. */
    public static Glowstick thrown(Level level, LivingEntity thrower, ItemStack stack) {
        Glowstick stick = new Glowstick(ModEntities.GLOWSTICK.get(), level);
        stick.setOwner(thrower);
        stick.setPos(thrower.getX(), thrower.getEyeY() - 0.15, thrower.getZ());
        stick.fillFrom(stack);
        return stick;
    }

    /** A stick placed at a position, e.g. shot by a dispenser. */
    public static Glowstick at(Level level, Position pos, ItemStack stack) {
        Glowstick stick = new Glowstick(ModEntities.GLOWSTICK.get(), level);
        stick.setPos(pos.x(), pos.y(), pos.z());
        stick.fillFrom(stack);
        return stick;
    }

    private void fillFrom(ItemStack stack) {
        GlowColor color = stack.getItem() instanceof GlowstickItem item ? item.color() : GlowColor.WHITE;
        entityData.set(COLOR, (byte) color.ordinal());
        setGlowLeft(GlowstickItem.glowLeft(stack));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(COLOR, (byte) GlowColor.WHITE.ordinal());
        builder.define(GLOW_LEFT, Integer.MAX_VALUE);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (GLOW_LEFT.equals(key)) {
            glowLeft = entityData.get(GLOW_LEFT);
        }
    }

    public GlowColor color() {
        return GlowColor.byId(entityData.get(COLOR));
    }

    public int glowLeft() {
        return glowLeft;
    }

    public void setGlowLeft(int ticks) {
        glowLeft = ticks;
        entityData.set(GLOW_LEFT, ticks);
    }

    /** 1 while fresh, dimming to 0 over the last {@link #FADE_TICKS}. */
    public float brightness() {
        return Mth.clamp(glowLeft / (float) FADE_TICKS, 0.0F, 1.0F);
    }

    /** Block light this stick gives; dims with {@link #brightness()} but stays at least 1 until it is out. */
    public int lightLevel() {
        if (glowLeft <= 0) {
            return 0;
        }
        return Math.max(1, Math.round(Config.lightLevel() * (0.4F + 0.6F * brightness())));
    }

    @Nullable
    public BlockPos lightPos() {
        return lightPos;
    }

    /** Brightest light claimed for {@code pos} by living sticks nearby, 0 if none claims it. */
    public static int lightClaimedAt(Level level, BlockPos pos) {
        int light = 0;
        for (Glowstick stick : level.getEntitiesOfClass(Glowstick.class, new AABB(pos).inflate(2), Entity::isAlive)) {
            if (pos.equals(stick.lightPos)) {
                light = Math.max(light, stick.lightLevel());
            }
        }
        return light;
    }

    /** The item this stick turns back into when picked up, remembering how much glow is left. */
    public ItemStack toItem() {
        ItemStack stack = new ItemStack(ModItems.glowstick(color()));
        if (glowLeft < Config.glowTicks()) {
            stack.set(ModDataComponents.GLOW_LEFT.get(), Math.max(1, glowLeft));
        }
        return stack;
    }

    // ------------------------------------------------------------------------------------------------
    // Tick
    // ------------------------------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() && correction.lengthSqr() > 1.0E-6) {
            Vec3 step = correction.scale(0.12);
            setPos(position().add(step));
            correction = correction.subtract(step);
        }
        double impact = tickPhysics();
        if (level().isClientSide()) {
            glowLeft--;
            tickPose(impact);
            return;
        }
        if (impact > BOUNCE_SOUND_IMPACT) {
            playSound(ModSounds.BOUNCE.get(), (float) Math.min(1.0, impact) * 0.7F, 0.9F + random.nextFloat() * 0.4F);
        }
        if (isInLava()) {
            fizzle();
            return;
        }
        glowLeft--;
        if (glowLeft <= 0) {
            fizzle();
            return;
        }
        if (glowLeft % GLOW_SYNC_INTERVAL == 0) {
            entityData.set(GLOW_LEFT, glowLeft);
        }
        updateLight();
    }

    /** Moves the stick for one tick; returns how hard it hit something. */
    private double tickPhysics() {
        hitEntities(getDeltaMovement());
        Vec3 before = position();
        Vec3 velocity = getDeltaMovement();
        boolean water = isInWater();
        if (!isNoGravity()) {
            velocity = velocity.add(0, water ? -WATER_GRAVITY : -GRAVITY, 0);
        }
        move(MoverType.SELF, velocity);
        Vec3 moved = position().subtract(before);
        Vec3 bounced = new Vec3(
                bounceAxis(velocity.x, moved.x),
                bounceAxis(velocity.y, moved.y),
                bounceAxis(velocity.z, moved.z));
        double impact = velocity.subtract(bounced).length();
        if (onGround()) {
            bounced = new Vec3(bounced.x * GROUND_FRICTION, bounced.y, bounced.z * GROUND_FRICTION);
        }
        bounced = bounced.scale(water ? WATER_DRAG : AIR_DRAG);
        setDeltaMovement(bounced.lengthSqr() < 1.0E-6 ? Vec3.ZERO : bounced);
        return impact;
    }

    /** Reflects the velocity along an axis where movement was blocked; small bounces die out. */
    private static double bounceAxis(double intended, double actual) {
        if (Math.abs(intended - actual) < 1.0E-4) {
            return intended;
        }
        double reflected = -intended * RESTITUTION;
        return Math.abs(reflected) < MIN_BOUNCE ? 0 : reflected;
    }

    /** Bounces off each mob in the path, at most once in a while. */
    private void hitEntities(Vec3 velocity) {
        if (velocity.lengthSqr() < 0.04) {
            return;
        }
        Vec3 from = position();
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(level(), this, from, from.add(velocity),
                getBoundingBox().expandTowards(velocity).inflate(0.2), this::canHitEntity);
        if (hit == null || !(hit.getEntity() instanceof LivingEntity target)) {
            return;
        }
        if (target.getId() == lastHitEntity && tickCount - lastHitTick < ENTITY_HIT_COOLDOWN) {
            return;
        }
        lastHitEntity = target.getId();
        lastHitTick = tickCount;
        if (!level().isClientSide()) {
            playSound(ModSounds.BOUNCE.get(), 0.6F, 1.0F);
        }
        setDeltaMovement(velocity.scale(-0.3).add(0, 0.15, 0));
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && !(target instanceof Glowstick);
    }

    // The client runs the same simulation. Server positions arrive a few ticks late, so a fast stick is always a bit
    // "behind" in them; differences explained by that delay are ignored, real divergence is blended in over several
    // ticks, and only a large one snaps.
    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        Vec3 offset = new Vec3(x, y, z).subtract(position());
        double tolerance = 0.25 + getDeltaMovement().length() * 3.0;
        if (offset.length() > 4.0) {
            correction = Vec3.ZERO;
            super.lerpTo(x, y, z, yRot, xRot, steps);
        } else if (offset.length() > tolerance) {
            correction = offset;
        }
    }

    // Server velocity updates are just as stale; only take them when something pushed the stick.
    @Override
    public void lerpMotion(double x, double y, double z) {
        if (new Vec3(x, y, z).distanceTo(getDeltaMovement()) > 0.3) {
            super.lerpMotion(x, y, z);
        }
    }

    // ------------------------------------------------------------------------------------------------
    // Light
    // ------------------------------------------------------------------------------------------------

    private void updateLight() {
        int light = Config.lightBlocks() ? lightLevel() : 0;
        BlockPos target = light > 0 ? findLightPos() : null;
        if (!Objects.equals(target, lightPos)) {
            BlockPos old = lightPos;
            // Claim the new spot first, so releasing the old one sees the current state.
            lightPos = target;
            if (old != null) {
                GlowLightBlock.release(level(), old);
            }
            if (target != null) {
                GlowLightBlock.place(level(), target, light);
            }
        } else if (target != null && (light != lastLight || tickCount % LIGHT_REFRESH_INTERVAL == 0)) {
            GlowLightBlock.place(level(), target, light);
        }
        lastLight = light;
    }

    /** The block the stick is in, or the one above when that one is taken (e.g. by grass or snow layers). */
    @Nullable
    private BlockPos findLightPos() {
        BlockPos pos = BlockPos.containing(getX(), getY() + 0.1, getZ());
        if (GlowLightBlock.canHost(level().getBlockState(pos))) {
            return pos;
        }
        BlockPos above = pos.above();
        return GlowLightBlock.canHost(level().getBlockState(above)) ? above : null;
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        // Unloaded sticks keep their light: they come back with the chunk.
        if (!level().isClientSide() && reason.shouldDestroy() && lightPos != null) {
            GlowLightBlock.release(level(), lightPos);
        }
    }

    /** Goes out with a puff of smoke. */
    private void fizzle() {
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 0.1, getZ(), 6, 0.12, 0.04, 0.12, 0.01);
        }
        playSound(ModSounds.FIZZLE.get(), 0.5F, 0.9F + random.nextFloat() * 0.3F);
        discard();
    }

    // ------------------------------------------------------------------------------------------------
    // Interaction
    // ------------------------------------------------------------------------------------------------

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!level().isClientSide() && isAlive()) {
            ItemStack stack = toItem();
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.3F,
                    1.4F + random.nextFloat() * 0.4F);
            discard();
        }
        return InteractionResult.sidedSuccess(level().isClientSide());
    }

    /** Hits knock the stick around; fire puts it out. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerableTo(source) || isRemoved()) {
            return false;
        }
        if (source.is(DamageTypeTags.IS_FIRE)) {
            if (!level().isClientSide()) {
                fizzle();
            }
            return true;
        }
        Entity attacker = source.getDirectEntity();
        if (attacker == null) {
            return false;
        }
        Vec3 push = attacker instanceof LivingEntity living ? living.getLookAngle() : position().subtract(attacker.position()).normalize();
        setDeltaMovement(getDeltaMovement().add(push.x * 0.5, 0.25, push.z * 0.5));
        hasImpulse = true;
        hurtMarked = true;
        if (!level().isClientSide()) {
            playSound(ModSounds.BOUNCE.get(), 0.7F, 1.2F);
        }
        return true;
    }

    // ------------------------------------------------------------------------------------------------
    // Client pose
    // ------------------------------------------------------------------------------------------------

    /** Half the stick's thickness: the rolling radius. */
    private static final float RADIUS = 0.094F;
    private static final float AIR_ANGULAR_DRAG = 0.99F;
    private static final float GROUND_ANGULAR_DRAG = 0.7F;
    private static final float SETTLE_TORQUE = 0.15F;
    private static final float MAX_AIR_SPIN = 0.7F;
    private static final float MAX_GROUND_SPIN = 0.9F;
    /** The stick's flat sides; its ends (body Y) are too small to rest on. */
    private static final Vector3f[] SIDES = {new Vector3f(1, 0, 0), new Vector3f(-1, 0, 0), new Vector3f(0, 0, 1), new Vector3f(0, 0, -1)};

    /**
     * Rigid-body tumbling: a throw starts an end-over-end spin, impacts add torque that tips the stick over in the
     * direction of travel, on the ground it rolls when moving across its length and falls onto its nearest flat side.
     */
    private void tickPose(double impact) {
        orientationO.set(orientation);
        Vec3 velocity = getDeltaMovement();
        Vector3f tumbleAxis = tumbleAxis(velocity);
        if (!poseStarted) {
            poseStarted = true;
            startPose(velocity, tumbleAxis);
            orientationO.set(orientation);
        }
        if (impact > 0.1) {
            spin.add(new Vector3f(tumbleAxis).mul((float) Math.min(impact, 1.0) * 0.5F)).add(randomVector(0.08F));
        }
        if (onGround()) {
            settleOntoSide();
            spin.mul(GROUND_ANGULAR_DRAG);
            roll(velocity);
            clampLength(spin, MAX_GROUND_SPIN);
        } else {
            spin.mul(isInWater() ? 0.9F : AIR_ANGULAR_DRAG);
            clampLength(spin, MAX_AIR_SPIN);
        }
        float angle = spin.length();
        if (angle > 1.0E-5F) {
            orientation.premul(new Quaternionf().rotationAxis(angle, spin.x / angle, spin.y / angle, spin.z / angle)).normalize();
        }
    }

    /** A thrown stick starts upright and flips end over end; one that appears at rest already lies flat. */
    private void startPose(Vec3 velocity, Vector3f tumbleAxis) {
        float yaw = random.nextFloat() * Mth.TWO_PI;
        if (velocity.lengthSqr() > 0.01) {
            orientation.rotationY(yaw).premul(new Quaternionf().rotationAxis(-0.6F, tumbleAxis.x, tumbleAxis.y, tumbleAxis.z));
            spin.set(tumbleAxis).mul(0.45F).add(randomVector(0.05F));
        } else {
            orientation.rotationY(yaw).rotateZ(Mth.HALF_PI);
        }
    }

    /** Axis around which moving with this velocity tips the stick over (top towards the motion). */
    private static Vector3f tumbleAxis(Vec3 velocity) {
        Vector3f horizontal = new Vector3f((float) velocity.x, 0, (float) velocity.z);
        if (horizontal.lengthSquared() < 1.0E-6F) {
            return new Vector3f(1, 0, 0);
        }
        return new Vector3f(0, 1, 0).cross(horizontal).normalize();
    }

    /** Gravity on a stick lying on the ground: rotate so that the flat side closest to "up" lines up with it. */
    private void settleOntoSide() {
        Vector3f up = new Vector3f(0, 1, 0);
        Vector3f best = null;
        float bestDot = -2;
        for (Vector3f side : SIDES) {
            Vector3f world = orientation.transform(new Vector3f(side));
            float dot = world.dot(up);
            if (dot > bestDot) {
                bestDot = dot;
                best = world;
            }
        }
        Vector3f torque = best.cross(up, new Vector3f());
        float sin = torque.length();
        if (sin > 1.0E-4F) {
            float tilt = (float) Math.acos(Mth.clamp(bestDot, -1.0F, 1.0F));
            spin.add(torque.mul(tilt * SETTLE_TORQUE / sin));
        }
    }

    /** Moving across its length on the ground, the stick rolls instead of sliding. */
    private void roll(Vec3 velocity) {
        Vector3f axis = orientation.transform(new Vector3f(0, 1, 0));
        // Rolling without slipping on a floor: angular velocity = (up x velocity) / radius.
        float target = new Vector3f((float) velocity.z, 0, (float) -velocity.x).dot(axis) / RADIUS;
        float current = spin.dot(axis);
        spin.add(new Vector3f(axis).mul((target - current) * 0.6F));
    }

    private Vector3f randomVector(float scale) {
        return new Vector3f(random.nextFloat() - 0.5F, random.nextFloat() - 0.5F, random.nextFloat() - 0.5F).mul(2 * scale);
    }

    private static void clampLength(Vector3f vector, float max) {
        float length = vector.length();
        if (length > max) {
            vector.mul(max / length);
        }
    }

    /** Interpolated orientation in world space; the stick's long axis is its Y axis. */
    public Quaternionf getOrientation(float partialTick) {
        return orientationO.slerp(orientation, partialTick, new Quaternionf());
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        double range = 96.0 * getViewScale();
        return distance < range * range;
    }

    /** Includes the halo around the stick. */
    @Override
    public AABB getBoundingBoxForCulling() {
        return super.getBoundingBoxForCulling().inflate(0.6);
    }

    // ------------------------------------------------------------------------------------------------
    // Saving
    // ------------------------------------------------------------------------------------------------

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Color", color().getSerializedName());
        tag.putInt("GlowLeft", glowLeft);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        for (GlowColor color : GlowColor.values()) {
            if (color.getSerializedName().equals(tag.getString("Color"))) {
                entityData.set(COLOR, (byte) color.ordinal());
            }
        }
        setGlowLeft(tag.contains("GlowLeft") ? tag.getInt("GlowLeft") : Config.glowTicks());
    }
}
