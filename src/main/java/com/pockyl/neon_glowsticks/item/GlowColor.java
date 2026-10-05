package com.pockyl.neon_glowsticks.item;

import net.minecraft.util.StringRepresentable;

/** The glowstick colors, one per dye; {@link #rgb} is the color of the light it casts. Keep in sync with the asset generator. */
public enum GlowColor implements StringRepresentable {
    WHITE("white", 0xF2F4FF),
    LIGHT_GRAY("light_gray", 0xC4CCDC),
    GRAY("gray", 0x8C96AA),
    // A black glowstick glows ultraviolet, like a blacklight.
    BLACK("black", 0x6A2CFF),
    BROWN("brown", 0xE08A2E),
    RED("red", 0xFF2A1E),
    ORANGE("orange", 0xFF7A12),
    YELLOW("yellow", 0xFFE12A),
    LIME("lime", 0x9BFF2A),
    GREEN("green", 0x2EE84A),
    CYAN("cyan", 0x1EE6D8),
    LIGHT_BLUE("light_blue", 0x4FC3FF),
    BLUE("blue", 0x2E62FF),
    PURPLE("purple", 0x9B3CFF),
    MAGENTA("magenta", 0xFF3CE6),
    PINK("pink", 0xFF7AB8);

    private final String name;
    private final int rgb;

    GlowColor(String name, int rgb) {
        this.name = name;
        this.rgb = rgb;
    }

    public int rgb() {
        return rgb;
    }

    public float red() {
        return (rgb >> 16 & 0xFF) / 255.0F;
    }

    public float green() {
        return (rgb >> 8 & 0xFF) / 255.0F;
    }

    public float blue() {
        return (rgb & 0xFF) / 255.0F;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    public static GlowColor byId(int id) {
        GlowColor[] values = values();
        return id >= 0 && id < values.length ? values[id] : WHITE;
    }
}
