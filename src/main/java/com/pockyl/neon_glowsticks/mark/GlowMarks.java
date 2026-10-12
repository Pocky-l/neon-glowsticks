package com.pockyl.neon_glowsticks.mark;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import com.pockyl.neon_glowsticks.Config;
import com.pockyl.neon_glowsticks.NeonGlowsticks;
import com.pockyl.neon_glowsticks.item.GlowColor;
import com.pockyl.neon_glowsticks.network.GlowMarkPayload;
import com.pockyl.neon_glowsticks.registry.ModAttachments;

/**
 * Marking mobs: a glowstick that hits a mob or player marks it for a while, so it shows an outline and carries a light
 * in the stick's color (both drawn by the clients). The server keeps the mark and tells the players who can see the mob.
 */
@EventBusSubscriber(modid = NeonGlowsticks.MOD_ID)
public final class GlowMarks {
    private GlowMarks() {
    }

    /** Marks the entity with this color for {@code markSeconds}; a new hit replaces the color and restarts the time. */
    public static void mark(LivingEntity target, GlowColor color) {
        int ticks = Config.markTicks();
        if (ticks <= 0 || target.level().isClientSide()) {
            return;
        }
        target.setData(ModAttachments.GLOW_MARK, new GlowMark(color, target.level().getGameTime() + ticks));
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(target, new GlowMarkPayload(target.getId(), color.ordinal(), ticks));
    }

    /** The entity's mark, or {@link GlowMark#NONE} when it is not marked right now. */
    public static GlowMark get(LivingEntity entity) {
        GlowMark mark = entity.hasData(ModAttachments.GLOW_MARK) ? entity.getData(ModAttachments.GLOW_MARK) : GlowMark.NONE;
        return mark.activeAt(entity.level().getGameTime()) ? mark : GlowMark.NONE;
    }

    // A player who comes close to a marked mob sees its mark too.
    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof LivingEntity target && event.getEntity() instanceof ServerPlayer player) {
            GlowMark mark = get(target);
            long left = mark.until() - target.level().getGameTime();
            if (left > 0) {
                PacketDistributor.sendToPlayer(player, new GlowMarkPayload(target.getId(), mark.color().ordinal(), (int) left));
            }
        }
    }
}
