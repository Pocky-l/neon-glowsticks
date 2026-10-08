package com.pockyl.neon_glowsticks.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.StringUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import com.pockyl.neon_glowsticks.Config;
import com.pockyl.neon_glowsticks.entity.Glowstick;
import com.pockyl.neon_glowsticks.registry.ModSounds;

import java.util.List;

/** A glowstick: use to crack and throw it, sneak-use to drop it at your feet. Dispensers shoot it too. */
public final class GlowstickItem extends Item {
    public static final float THROW_POWER = 0.9F;
    public static final float DROP_POWER = 0.3F;
    /** Item tag with the ticks of glow left in a stick that was thrown and picked up again; fresh sticks do not have it. */
    public static final String GLOW_LEFT_TAG = "GlowLeft";
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
        Integer left = storedGlowLeft(stack);
        return left != null ? left : Config.glowTicks();
    }

    /** The remaining glow saved on a picked-up stick, or null for a fresh one. */
    @Nullable
    public static Integer storedGlowLeft(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(GLOW_LEFT_TAG, Tag.TAG_INT) ? tag.getInt(GLOW_LEFT_TAG) : null;
    }

    public static void setGlowLeft(ItemStack stack, int ticks) {
        stack.getOrCreateTag().putInt(GLOW_LEFT_TAG, ticks);
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
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        Integer left = storedGlowLeft(stack);
        if (left != null) {
            tooltip.add(Component.translatable("item.neon_glowsticks.glowstick.glow_left", StringUtil.formatTickDuration(left))
                    .withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.translatable("item.neon_glowsticks.glowstick.glows_for",
                    StringUtil.formatTickDuration(Config.glowTicks())).withStyle(ChatFormatting.GRAY));
        }
    }
}
