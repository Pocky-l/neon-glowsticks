package com.pockyl.neon_glowsticks.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import com.pockyl.neon_glowsticks.NeonGlowsticks;

@Mod(value = NeonGlowsticks.MOD_ID, dist = Dist.CLIENT)
public final class NeonGlowsticksClient {
    public NeonGlowsticksClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
