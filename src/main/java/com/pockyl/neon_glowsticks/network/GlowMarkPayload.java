package com.pockyl.neon_glowsticks.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import com.pockyl.neon_glowsticks.NeonGlowsticks;

/** Server to client: an entity is marked by a glowstick of color {@code color} for {@code ticks}. */
public record GlowMarkPayload(int entityId, int color, int ticks) implements CustomPacketPayload {
    public static final Type<GlowMarkPayload> TYPE = new Type<>(NeonGlowsticks.id("glow_mark"));

    public static final StreamCodec<ByteBuf, GlowMarkPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, GlowMarkPayload::entityId,
            ByteBufCodecs.VAR_INT, GlowMarkPayload::color,
            ByteBufCodecs.VAR_INT, GlowMarkPayload::ticks,
            GlowMarkPayload::new);

    @Override
    public Type<GlowMarkPayload> type() {
        return TYPE;
    }
}
