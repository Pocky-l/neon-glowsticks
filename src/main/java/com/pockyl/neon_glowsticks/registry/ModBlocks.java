package com.pockyl.neon_glowsticks.registry;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.pockyl.neon_glowsticks.NeonGlowsticks;
import com.pockyl.neon_glowsticks.block.GlowLightBlock;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(NeonGlowsticks.MOD_ID);

    /** Invisible light source that follows a thrown glowstick; it has no item. */
    public static final DeferredBlock<GlowLightBlock> GLOW_LIGHT = BLOCKS.register("glow_light",
            () -> new GlowLightBlock(BlockBehaviour.Properties.of()
                    .replaceable()
                    .noCollission()
                    .noOcclusion()
                    .noLootTable()
                    .instabreak()
                    .pushReaction(PushReaction.DESTROY)
                    .lightLevel(GlowLightBlock::lightEmission)));

    private ModBlocks() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
    }
}
