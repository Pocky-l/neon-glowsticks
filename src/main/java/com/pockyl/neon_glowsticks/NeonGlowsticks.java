package com.pockyl.neon_glowsticks;

import com.mojang.logging.LogUtils;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.AbstractProjectileDispenseBehavior;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

import com.pockyl.neon_glowsticks.entity.Glowstick;
import com.pockyl.neon_glowsticks.item.GlowColor;
import com.pockyl.neon_glowsticks.registry.ModEntities;
import com.pockyl.neon_glowsticks.registry.ModItems;
import com.pockyl.neon_glowsticks.registry.ModSounds;
import com.pockyl.neon_glowsticks.registry.PockyModsTab;

@Mod(NeonGlowsticks.MOD_ID)
public final class NeonGlowsticks {
    public static final String MOD_ID = "neon_glowsticks";
    public static final Logger LOGGER = LogUtils.getLogger();

    // The no-argument constructor with FMLJavaModLoadingContext.get() is the one every Forge 47 and NeoForge 1.20.1 build accepts.
    public NeonGlowsticks() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.register(modBus);
        ModEntities.register(modBus);
        ModSounds.register(modBus);
        PockyModsTab.register(modBus, () -> new ItemStack(ModItems.glowstick(GlowColor.BLUE)), output -> {
            for (GlowColor color : GlowColor.values()) {
                output.accept(ModItems.glowstick(color));
            }
        });
        modBus.addListener(NeonGlowsticks::addToVanillaTabs);
        modBus.addListener(NeonGlowsticks::commonSetup);

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_SPEC);
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
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
                DispenserBlock.registerBehavior(ModItems.glowstick(color), new AbstractProjectileDispenseBehavior() {
                    @Override
                    protected Projectile getProjectile(Level level, Position pos, ItemStack stack) {
                        return Glowstick.at(level, pos, stack);
                    }
                });
            }
        });
    }
}
