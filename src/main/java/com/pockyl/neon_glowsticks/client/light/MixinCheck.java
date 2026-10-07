package com.pockyl.neon_glowsticks.client.light;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.neoforged.fml.ModList;

import com.pockyl.neon_glowsticks.NeonGlowsticks;

import java.lang.reflect.Method;

/**
 * The light hooks are optional (another mod may replace the same rendering code, e.g. a chunk renderer like Embeddium; Sodium gets its own hook),
 * so a missing hook never crashes the game. This logs which hooks are active, to explain a missing light or color.
 */
public final class MixinCheck {
    private MixinCheck() {
    }

    public static void log() {
        report("block light", LevelRenderer.class);
        report("block color", ModelBlockRenderer.class);
        report("entity light", EntityRenderer.class);
        if (ModList.get().isLoaded("sodium")) {
            try {
                report("Sodium block color", Class.forName("net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer"));
            } catch (ClassNotFoundException e) {
                NeonGlowsticks.LOGGER.warn("Sodium is installed, but its block renderer was not found; the light stays uncolored");
            }
        }
    }

    private static void report(String hook, Class<?> target) {
        boolean applied = false;
        for (Method method : target.getDeclaredMethods()) {
            applied |= method.getName().contains("neon_glowsticks$");
        }
        if (applied) {
            NeonGlowsticks.LOGGER.info("Glowstick {} hook is active", hook);
        } else {
            NeonGlowsticks.LOGGER.warn("Glowstick {} hook could not be applied (another mod replaces {}); that part of the light is off",
                    hook, target.getSimpleName());
        }
    }
}
