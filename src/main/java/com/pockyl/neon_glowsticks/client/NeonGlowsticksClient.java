package com.pockyl.neon_glowsticks.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import com.pockyl.neon_glowsticks.NeonGlowsticks;
import com.pockyl.neon_glowsticks.client.light.MixinCheck;
import com.pockyl.neon_glowsticks.registry.ModEntities;

/** Client-only mod bus listeners; Forge loads this class only on the physical client. */
@Mod.EventBusSubscriber(modid = NeonGlowsticks.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class NeonGlowsticksClient {
    private NeonGlowsticksClient() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.GLOWSTICK.get(), GlowstickRenderer::new);
    }

    @SubscribeEvent
    public static void registerColors(RegisterColorHandlersEvent.Item event) {
        GlowstickColors.register(event);
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event) {
        MixinCheck.log();
    }
}
