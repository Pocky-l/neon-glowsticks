package com.pockyl.neon_glowsticks.client;

import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;

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
            case 0, 2 -> 0xFF000000 | rgb;
            case 3 -> 0xFF000000 | halfwayToWhite(rgb);
            default -> -1;
        };
    }

    private static int halfwayToWhite(int rgb) {
        return halfwayTo255(rgb >> 16 & 0xFF) << 16 | halfwayTo255(rgb >> 8 & 0xFF) << 8 | halfwayTo255(rgb & 0xFF);
    }

    private static int halfwayTo255(int channel) {
        return channel + Mth.floor((255 - channel) * 0.5F);
    }
}
