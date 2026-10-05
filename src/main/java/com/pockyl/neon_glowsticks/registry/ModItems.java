package com.pockyl.neon_glowsticks.registry;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.pockyl.neon_glowsticks.NeonGlowsticks;
import com.pockyl.neon_glowsticks.item.GlowColor;
import com.pockyl.neon_glowsticks.item.GlowstickItem;

import java.util.EnumMap;
import java.util.Map;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NeonGlowsticks.MOD_ID);

    // Declared before the items: their initializers fill it.
    private static final Map<GlowColor, DeferredItem<GlowstickItem>> BY_COLOR = new EnumMap<>(GlowColor.class);

    public static final DeferredItem<GlowstickItem> RED_GLOWSTICK = glowstickItem(GlowColor.RED);
    public static final DeferredItem<GlowstickItem> GREEN_GLOWSTICK = glowstickItem(GlowColor.GREEN);
    public static final DeferredItem<GlowstickItem> BLUE_GLOWSTICK = glowstickItem(GlowColor.BLUE);
    public static final DeferredItem<GlowstickItem> WHITE_GLOWSTICK = glowstickItem(GlowColor.WHITE);

    private ModItems() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }

    public static GlowstickItem glowstick(GlowColor color) {
        return BY_COLOR.get(color).get();
    }

    private static DeferredItem<GlowstickItem> glowstickItem(GlowColor color) {
        DeferredItem<GlowstickItem> item = ITEMS.register(color.getSerializedName() + "_glowstick",
                () -> new GlowstickItem(color, new Item.Properties().stacksTo(32)));
        BY_COLOR.put(color, item);
        return item;
    }
}
