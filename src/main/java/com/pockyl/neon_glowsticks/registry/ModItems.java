package com.pockyl.neon_glowsticks.registry;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.pockyl.neon_glowsticks.NeonGlowsticks;
import com.pockyl.neon_glowsticks.item.GlowColor;
import com.pockyl.neon_glowsticks.item.GlowstickItem;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NeonGlowsticks.MOD_ID);

    /** One glowstick per color, registered as {@code <color>_glowstick}. */
    public static final Map<GlowColor, DeferredItem<GlowstickItem>> GLOWSTICKS = registerGlowsticks();

    private ModItems() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    public static GlowstickItem glowstick(GlowColor color) {
        return GLOWSTICKS.get(color).get();
    }

    private static Map<GlowColor, DeferredItem<GlowstickItem>> registerGlowsticks() {
        Map<GlowColor, DeferredItem<GlowstickItem>> items = new EnumMap<>(GlowColor.class);
        for (GlowColor color : GlowColor.values()) {
            items.put(color, ITEMS.register(color.getSerializedName() + "_glowstick",
                    () -> new GlowstickItem(color, new Item.Properties().stacksTo(32))));
        }
        return Collections.unmodifiableMap(items);
    }
}
