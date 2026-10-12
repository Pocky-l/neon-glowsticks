package com.pockyl.neon_glowsticks.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.pockyl.neon_glowsticks.client.ClientGlowMarks;
import com.pockyl.neon_glowsticks.item.GlowColor;

/** The outline of a marked mob has the glowstick's color instead of the team color. Applied on the client only. */
@Mixin(Entity.class)
abstract class EntityMixin {
    @Inject(method = "getTeamColor", at = @At("RETURN"), cancellable = true)
    private void neon_glowsticks$markColor(CallbackInfoReturnable<Integer> cir) {
        GlowColor color = ClientGlowMarks.color((Entity) (Object) this);
        if (color != null) {
            cir.setReturnValue(color.rgb());
        }
    }
}
