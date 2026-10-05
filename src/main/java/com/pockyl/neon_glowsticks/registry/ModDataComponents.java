package com.pockyl.neon_glowsticks.registry;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.pockyl.neon_glowsticks.NeonGlowsticks;

public final class ModDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, NeonGlowsticks.MOD_ID);

    /** Ticks of glow left in a glowstick that was thrown and picked up again; fresh sticks do not have it. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> GLOW_LEFT = COMPONENTS.registerComponentType(
            "glow_left", builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    private ModDataComponents() {
    }

    public static void register(IEventBus modBus) {
        COMPONENTS.register(modBus);
    }
}
