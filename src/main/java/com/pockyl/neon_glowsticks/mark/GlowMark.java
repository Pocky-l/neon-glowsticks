package com.pockyl.neon_glowsticks.mark;

import com.pockyl.neon_glowsticks.item.GlowColor;

/** A mob marked by a glowstick hit: it glows in the stick's color until game time {@code until}. */
public record GlowMark(GlowColor color, long until) {
    public static final GlowMark NONE = new GlowMark(GlowColor.WHITE, 0);

    public boolean activeAt(long gameTime) {
        return gameTime < until;
    }
}
