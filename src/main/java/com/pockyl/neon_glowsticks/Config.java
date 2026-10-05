package com.pockyl.neon_glowsticks;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.IntValue GLOW_SECONDS = BUILDER
            .comment("How long a cracked glowstick glows, in seconds. It dims during the last minute.")
            .translation("neon_glowsticks.configuration.glowSeconds")
            .defineInRange("glowSeconds", 600, 10, 86400);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private static final ModConfigSpec.Builder CLIENT = new ModConfigSpec.Builder();

    private static final ModConfigSpec.IntValue LIGHT_LEVEL = CLIENT
            .comment("Light level of a glowstick (a torch gives 14). The light is client-side: it does not stop mobs from spawning.")
            .translation("neon_glowsticks.configuration.lightLevel")
            .defineInRange("lightLevel", 13, 1, 15);
    private static final ModConfigSpec.BooleanValue COLORED_LIGHT = CLIENT
            .comment("Tint the light of glowsticks with their color.")
            .translation("neon_glowsticks.configuration.coloredLight")
            .define("coloredLight", true);
    private static final ModConfigSpec.DoubleValue COLORED_LIGHT_STRENGTH = CLIENT
            .comment("How strongly the light is tinted.")
            .translation("neon_glowsticks.configuration.coloredLightStrength")
            .defineInRange("coloredLightStrength", 1.0, 0.05, 1.5);
    private static final ModConfigSpec.IntValue MAX_LIGHTS = CLIENT
            .comment("At most this many glowsticks (the nearest ones) give light at once. Each moving light re-renders the chunks",
                    "around it, so lower this on weak computers.")
            .translation("neon_glowsticks.configuration.maxLights")
            .defineInRange("maxLights", 32, 1, 256);
    private static final ModConfigSpec.BooleanValue HALO = CLIENT
            .comment("Draw a soft glow around glowsticks.")
            .translation("neon_glowsticks.configuration.halo")
            .define("halo", true);

    public static final ModConfigSpec CLIENT_SPEC = CLIENT.build();

    private Config() {
    }

    public static int glowTicks() {
        return GLOW_SECONDS.get() * 20;
    }

    public static int lightLevel() {
        return LIGHT_LEVEL.get();
    }

    public static boolean coloredLight() {
        return COLORED_LIGHT.get();
    }

    public static float coloredLightStrength() {
        return COLORED_LIGHT_STRENGTH.get().floatValue();
    }

    public static int maxLights() {
        return MAX_LIGHTS.get();
    }

    public static boolean halo() {
        return HALO.get();
    }
}
