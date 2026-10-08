package com.pockyl.neon_glowsticks.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import com.pockyl.neon_glowsticks.NeonGlowsticks;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, NeonGlowsticks.MOD_ID);

    /** The stick is cracked to start glowing and thrown. */
    public static final RegistryObject<SoundEvent> CRACK = sound("glowstick.crack");
    public static final RegistryObject<SoundEvent> BOUNCE = sound("glowstick.bounce");
    public static final RegistryObject<SoundEvent> FIZZLE = sound("glowstick.fizzle");

    private ModSounds() {
    }

    public static void register(IEventBus modBus) {
        SOUNDS.register(modBus);
    }

    private static RegistryObject<SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(NeonGlowsticks.id(name)));
    }
}
