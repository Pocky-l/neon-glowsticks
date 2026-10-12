package com.pockyl.neon_glowsticks.registry;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import com.pockyl.neon_glowsticks.NeonGlowsticks;
import com.pockyl.neon_glowsticks.mark.GlowMark;

import java.util.function.Supplier;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, NeonGlowsticks.MOD_ID);

    /** Server only and not saved: a glowstick mark lasts seconds. */
    public static final Supplier<AttachmentType<GlowMark>> GLOW_MARK = ATTACHMENTS.register("glow_mark",
            () -> AttachmentType.builder(() -> GlowMark.NONE).build());

    private ModAttachments() {
    }

    public static void register(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
    }
}
