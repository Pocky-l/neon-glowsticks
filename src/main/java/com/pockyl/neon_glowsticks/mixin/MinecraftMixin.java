package com.pockyl.neon_glowsticks.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.pockyl.neon_glowsticks.client.ClientGlowMarks;

/** Mobs marked by a glowstick show an outline through walls, like the Glowing effect, but drawn by the client only. */
@Mixin(Minecraft.class)
abstract class MinecraftMixin {
    @Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
    private void neon_glowsticks$markOutline(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (ClientGlowMarks.color(entity) != null) {
            cir.setReturnValue(true);
        }
    }
}
