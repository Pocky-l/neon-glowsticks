package com.pockyl.neon_glowsticks.item;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.StringUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import com.pockyl.neon_glowsticks.Config;
import com.pockyl.neon_glowsticks.entity.Glowstick;
import com.pockyl.neon_glowsticks.registry.ModDataComponents;
import com.pockyl.neon_glowsticks.registry.ModSounds;

import java.util.List;

/** A glowstick: use to crack and throw it, sneak-use to drop it at your feet. Dispensers shoot it too. */
public final class GlowstickItem extends Item implements ProjectileItem {
    public static final float THROW_POWER = 0.9F;
    public static final float DROP_POWER = 0.3F;
    private static final int COOLDOWN_TICKS = 4;

    private final GlowColor color;

    public GlowstickItem(GlowColor color, Properties properties) {
        super(properties);
        this.color = color;
    }

    public GlowColor color() {
        return color;
    }

    /** Ticks of glow the stick has: a fresh one has the full configured time. */
    public static int glowLeft(ItemStack stack) {
        Integer left = stack.get(ModDataComponents.GLOW_LEFT.get());
        return left != null ? left : Config.glowTicks();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.CRACK.get(), SoundSource.PLAYERS, 0.6F,
                0.9F + level.getRandom().nextFloat() * 0.2F);
        if (!level.isClientSide()) {
            Glowstick stick = Glowstick.thrown(level, player, stack);
            float power = player.isShiftKeyDown() ? DROP_POWER : THROW_POWER;
            stick.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, power, 1.0F);
            level.addFreshEntity(stick);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        stack.consume(1, player);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public Projectile asProjectile(Level level, Position pos, ItemStack stack, Direction direction) {
        return Glowstick.at(level, pos, stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Integer left = stack.get(ModDataComponents.GLOW_LEFT.get());
        if (left != null) {
            tooltip.add(Component.translatable("item.neon_glowsticks.glowstick.glow_left", StringUtil.formatTickDuration(left, 20))
                    .withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("item.neon_glowsticks.glowstick.glows_for",
                    StringUtil.formatTickDuration(Config.glowTicks(), 20)).withStyle(ChatFormatting.GRAY));
        }
    }
}
