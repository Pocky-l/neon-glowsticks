package com.pockyl.neon_glowsticks.mixin;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.pockyl.neon_glowsticks.client.light.DynamicLights;

/** Mobs, players and items near a glowstick are lit by it. */
@Mixin(EntityRenderer.class)
abstract class EntityRendererMixin {
    @Inject(method = "getBlockLightLevel", at = @At("RETURN"), cancellable = true)
    private void neon_glowsticks$dynamicLight(Entity entity, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        int light = DynamicLights.blockLight(pos);
        if (light > cir.getReturnValueI()) {
            cir.setReturnValue(light);
        }
    }
}
