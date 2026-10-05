package com.pockyl.neon_glowsticks.item;

import net.minecraft.util.StringRepresentable;

/** The glowstick colors; {@link #rgb} is the color of the light it casts. */
public enum GlowColor implements StringRepresentable {
    RED("red", 0xFF2A1E),
    GREEN("green", 0x4CFF3C),
    BLUE("blue", 0x2E62FF),
    WHITE("white", 0xF2F4FF);

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
