package com.pockyl.neon_glowsticks.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.jetbrains.annotations.Nullable;

import com.pockyl.neon_glowsticks.NeonGlowsticks;
import com.pockyl.neon_glowsticks.item.GlowColor;
import com.pockyl.neon_glowsticks.network.GlowMarkPayload;

import java.util.HashMap;
import java.util.Map;

/**
 * The glowstick marks this client knows of, by entity id. The outline and its color come from the mixins on
 * {@code Minecraft} and {@code Entity}, the light from {@code DynamicLights}; all of them run on the client thread.
 */
@EventBusSubscriber(modid = NeonGlowsticks.MOD_ID, value = Dist.CLIENT)
public final class ClientGlowMarks {
    private static final Map<Integer, Mark> MARKS = new HashMap<>();
    @Nullable
    private static ClientLevel lastLevel;

    private ClientGlowMarks() {
    }

    private record Mark(GlowColor color, long until) {
    }

    public static void handle(GlowMarkPayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            MARKS.put(payload.entityId(), new Mark(GlowColor.byId(payload.color()), level.getGameTime() + payload.ticks()));
        }
    }

    /** Color of the entity's mark, or null when it is not marked. */
    @Nullable
    public static GlowColor color(Entity entity) {
        if (MARKS.isEmpty() || entity.level() == null || !entity.level().isClientSide()) {
            return null;
        }
        Mark mark = MARKS.get(entity.getId());
        return mark != null && entity.level().getGameTime() < mark.until() ? mark.color() : null;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != lastLevel) {
            MARKS.clear();
            lastLevel = level;
        }
        if (level != null && !MARKS.isEmpty()) {
            long time = level.getGameTime();
            MARKS.values().removeIf(mark -> time >= mark.until());
        }
    }
}
