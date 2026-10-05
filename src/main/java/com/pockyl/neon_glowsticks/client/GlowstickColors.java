package com.pockyl.neon_glowsticks.client;

import net.minecraft.util.FastColor;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

import com.pockyl.neon_glowsticks.item.GlowColor;
import com.pockyl.neon_glowsticks.item.GlowstickItem;
import com.pockyl.neon_glowsticks.registry.ModItems;

/**
 * All glowsticks share one grayscale texture that is tinted with the stick's color. Tint indexes (set by the asset
 * generator): 0 icon tube, 1 icon caps (untinted), 2 glowing core, 3 clear shell (a paler shade of the color).
 */
public final class GlowstickColors {
    private GlowstickColors() {
    }

    public static void register(RegisterColorHandlersEvent.Item event) {
        for (GlowColor color : GlowColor.values()) {
            event.register(GlowstickColors::color, ModItems.glowstick(color));
        }
    }

    private static int color(ItemStack stack, int tintIndex) {
        if (!(stack.getItem() instanceof GlowstickItem item)) {
            return -1;
        }
        int rgb = item.color().rgb();
        return switch (tintIndex) {
            case 0, 2 -> FastColor.ARGB32.opaque(rgb);
            case 3 -> FastColor.ARGB32.opaque(FastColor.ARGB32.lerp(0.5F, rgb, 0xFFFFFF));
            default -> -1;
        };
    }
}
