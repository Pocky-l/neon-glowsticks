package com.pockyl.neon_glowsticks.light;

/** How the colors of several glowsticks lighting the same spot combine. */
public final class ColorMixing {
    private ColorMixing() {
    }

    /**
     * Turns the weighted average of the light colors in {@code rgb} into their additive mix, in place: the brightest
     * channel is brought back to full, so red and green light make yellow instead of a dim olive.
     */
    public static void additive(float[] rgb) {
        float peak = Math.max(rgb[0], Math.max(rgb[1], rgb[2]));
        if (peak > 0.001F) {
            rgb[0] /= peak;
            rgb[1] /= peak;
            rgb[2] /= peak;
        }
    }
}
