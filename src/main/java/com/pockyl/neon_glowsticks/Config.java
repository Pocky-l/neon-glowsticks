package com.pockyl.neon_glowsticks;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.IntValue GLOW_SECONDS = BUILDER
            .comment("How long a cracked glowstick glows, in seconds. It dims during the last minute.")
            .translation("neon_glowsticks.configuration.glowSeconds")
            .defineInRange("glowSeconds", 600, 10, 86400);
    private static final ModConfigSpec.IntValue LIGHT_LEVEL = BUILDER
            .comment("Block light level around a thrown glowstick (torch = 14).")
            .translation("neon_glowsticks.configuration.lightLevel")
            .defineInRange("lightLevel", 13, 1, 15);
    private static final ModConfigSpec.BooleanValue LIGHT_BLOCKS = BUILDER
            .comment("Whether thrown glowsticks really light up the world (invisible light blocks that follow them).",
                    "Without it they still glow, but mobs spawn and the world stays dark.")
            .translation("neon_glowsticks.configuration.lightBlocks")
            .define("lightBlocks", true);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private static final ModConfigSpec.Builder CLIENT = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue COLORED_LIGHT = CLIENT
            .comment("Tint the blocks around a glowstick with its color.")
            .translation("neon_glowsticks.configuration.coloredLight")
            .define("coloredLight", true);
    private static final ModConfigSpec.DoubleValue COLORED_LIGHT_STRENGTH = CLIENT
            .comment("How strong the colored light is.")
            .translation("neon_glowsticks.configuration.coloredLightStrength")
            .defineInRange("coloredLightStrength", 0.6, 0.05, 1.0);
    private static final ModConfigSpec.IntValue COLORED_LIGHT_RADIUS = CLIENT
            .comment("Reach of the colored light in blocks.")
            .translation("neon_glowsticks.configuration.coloredLightRadius")
            .defineInRange("coloredLightRadius", 6, 2, 10);
    private static final ModConfigSpec.IntValue MAX_COLORED_LIGHTS = CLIENT
            .comment("At most this many glowsticks (the nearest ones) cast colored light at once.")
            .translation("neon_glowsticks.configuration.maxColoredLights")
            .defineInRange("maxColoredLights", 32, 1, 256);
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

    public static boolean lightBlocks() {
        return LIGHT_BLOCKS.get();
    }

    public static boolean coloredLight() {
        return COLORED_LIGHT.get();
    }

    public static float coloredLightStrength() {
        return COLORED_LIGHT_STRENGTH.get().floatValue();
    }

    public static int coloredLightRadius() {
        return COLORED_LIGHT_RADIUS.get();
    }

    public static int maxColoredLights() {
        return MAX_COLORED_LIGHTS.get();
    }

    public static boolean halo() {
        return HALO.get();
    }
}
