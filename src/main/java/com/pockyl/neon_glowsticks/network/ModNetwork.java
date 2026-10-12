package com.pockyl.neon_glowsticks.network;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import com.pockyl.neon_glowsticks.NeonGlowsticks;
import com.pockyl.neon_glowsticks.client.ClientGlowMarks;

@EventBusSubscriber(modid = NeonGlowsticks.MOD_ID)
public final class ModNetwork {
    private static final String VERSION = "1";

    private ModNetwork() {
    }

    // The client handler is only called, and its class only loaded, on a client.
    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VERSION);
        registrar.playToClient(GlowMarkPayload.TYPE, GlowMarkPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientGlowMarks.handle(payload)));
    }
}
