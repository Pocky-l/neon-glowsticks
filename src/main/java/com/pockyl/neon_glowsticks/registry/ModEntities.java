package com.pockyl.neon_glowsticks.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import com.pockyl.neon_glowsticks.NeonGlowsticks;
import com.pockyl.neon_glowsticks.entity.Glowstick;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, NeonGlowsticks.MOD_ID);

    // The height equals the stick's thickness, so a stick lying on the ground rests exactly on it.
    public static final RegistryObject<EntityType<Glowstick>> GLOWSTICK = ENTITIES.register("glowstick",
            () -> EntityType.Builder.<Glowstick>of(Glowstick::new, MobCategory.MISC)
                    .sized(0.25F, 0.1875F)
                    .clientTrackingRange(10)
                    .updateInterval(10)
                    .build(NeonGlowsticks.id("glowstick").toString()));

    private ModEntities() {
    }

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
    }
}
