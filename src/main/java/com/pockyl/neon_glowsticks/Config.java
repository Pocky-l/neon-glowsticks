package com.pockyl.neon_glowsticks;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.IntValue GLOW_SECONDS = BUILDER
            .comment("How long a cracked glowstick glows, in seconds. It dims during the last minute.")
            .translation("neon_glowsticks.configuration.glowSeconds")
            .defineInRange("glowSeconds", 600, 10, 86400);
    private static final ModConfigSpec.IntValue MARK_SECONDS = BUILDER
            .comment("How long a mob or player hit by a thrown glowstick stays marked (outline and light in the stick's color), in seconds.",
                    "0 turns marking off.")
            .translation("neon_glowsticks.configuration.markSeconds")
            .defineInRange("markSeconds", 30, 0, 3600);

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
            .comment("At most this many glowsticks (your own held ones first, then the nearest ones) give light at once. Each moving",
                    "light re-renders the chunks around it, so lower this on weak computers.")
            .translation("neon_glowsticks.configuration.maxLights")
            .defineInRange("maxLights", 32, 1, 256);
    private static final ModConfigSpec.BooleanValue HELD_LIGHT = CLIENT
            .comment("A glowstick held in a hand lights up the area around whoever holds it: you, other players and mobs.")
            .translation("neon_glowsticks.configuration.heldLight")
            .define("heldLight", true);
    private static final ModConfigSpec.IntValue HELD_LIGHT_LEVEL = CLIENT
            .comment("Light level of a glowstick held in a hand. Held glowsticks count towards maxLights.")
            .translation("neon_glowsticks.configuration.heldLightLevel")
            .defineInRange("heldLightLevel", 11, 1, 15);
    private static final ModConfigSpec.BooleanValue MARK_LIGHT = CLIENT
            .comment("Whether mobs marked by a glowstick carry a light in its color (their outline shows either way).")
            .translation("neon_glowsticks.configuration.markLight")
            .define("markLight", true);
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

    /** Mark duration in ticks; 0 when marking is off. */
    public static int markTicks() {
        return (SPEC.isLoaded() ? MARK_SECONDS.get() : MARK_SECONDS.getDefault()) * 20;
    }

    /** For game tests; not saved to the file. */
    public static void setMarkSeconds(int seconds) {
        MARK_SECONDS.set(seconds);
    }

    public static boolean markLight() {
        return MARK_LIGHT.get();
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

    public static boolean heldLight() {
        return HELD_LIGHT.get();
    }

    public static int heldLightLevel() {
        return HELD_LIGHT_LEVEL.get();
    }

    public static boolean halo() {
        return HALO.get();
    }
}
