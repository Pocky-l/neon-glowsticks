package com.pockyl.neon_glowsticks;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import org.slf4j.Logger;

import com.pockyl.neon_glowsticks.item.GlowColor;
import com.pockyl.neon_glowsticks.registry.ModDataComponents;
import com.pockyl.neon_glowsticks.registry.ModEntities;
import com.pockyl.neon_glowsticks.registry.ModItems;
import com.pockyl.neon_glowsticks.registry.ModSounds;
import com.pockyl.neon_glowsticks.registry.PockyModsTab;

@Mod(NeonGlowsticks.MOD_ID)
public final class NeonGlowsticks {
    public static final String MOD_ID = "neon_glowsticks";
    public static final Logger LOGGER = LogUtils.getLogger();

    public NeonGlowsticks(IEventBus modBus, ModContainer container) {
        ModItems.register(modBus);
        ModEntities.register(modBus);
        ModDataComponents.register(modBus);
        ModSounds.register(modBus);
        PockyModsTab.register(modBus, () -> new ItemStack(ModItems.glowstick(GlowColor.BLUE)), output -> {
            for (GlowColor color : GlowColor.values()) {
                output.accept(ModItems.glowstick(color));
            }
        });
        modBus.addListener(NeonGlowsticks::addToVanillaTabs);
        modBus.addListener(NeonGlowsticks::commonSetup);

        container.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_SPEC);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    private static void addToVanillaTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            for (GlowColor color : GlowColor.values()) {
                event.accept(ModItems.glowstick(color));
            }
        }
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            for (GlowColor color : GlowColor.values()) {
                DispenserBlock.registerProjectileBehavior(ModItems.glowstick(color));
            }
        });
    }
}
