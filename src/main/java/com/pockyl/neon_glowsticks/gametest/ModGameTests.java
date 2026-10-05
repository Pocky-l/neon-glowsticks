package com.pockyl.neon_glowsticks.gametest;

import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import com.pockyl.neon_glowsticks.Config;
import com.pockyl.neon_glowsticks.NeonGlowsticks;
import com.pockyl.neon_glowsticks.entity.Glowstick;
import com.pockyl.neon_glowsticks.item.GlowColor;
import com.pockyl.neon_glowsticks.item.GlowstickItem;
import com.pockyl.neon_glowsticks.registry.ModDataComponents;
import com.pockyl.neon_glowsticks.registry.ModItems;

/**
 * In-game tests, run headless by {@code gradlew runGameTestServer}.
 * Tests use the 1x1x1 {@code empty} structure and build what they need around it; every test has its own batch.
 * The light itself is client-side and is not covered here.
 */
@GameTestHolder(NeonGlowsticks.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ModGameTests {
    private ModGameTests() {
    }

    @GameTest(template = "empty", batch = "modLoads")
    public static void modLoads(GameTestHelper helper) {
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "thrownStickComesToRest")
    public static void thrownStickComesToRest(GameTestHelper helper) {
        floor(helper);
        Glowstick stick = stick(helper, GlowColor.BLUE, new Vec3(1.5, 4, 1.5));
        helper.succeedWhen(() -> {
            helper.assertTrue(stick.onGround(), "the stick fell onto the stone");
            helper.assertTrue(stick.getDeltaMovement().lengthSqr() < 1.0E-6, "and lies still");
            helper.assertTrue(Math.abs(stick.getY() - helper.absoluteVec(new Vec3(0, 2, 0)).y) < 0.01, "on top of the floor");
        });
    }

    @GameTest(template = "empty", batch = "stickBurnsOut")
    public static void stickBurnsOut(GameTestHelper helper) {
        floor(helper);
        Glowstick stick = stick(helper, GlowColor.WHITE, new Vec3(1.5, 2.2, 1.5));
        stick.setGlowLeft(15);
        // The test area's entities may start ticking a few ticks late, so only the outcome is checked.
        helper.succeedWhen(() -> helper.assertFalse(stick.isAlive(), "the stick went out"));
    }

    @GameTest(template = "empty", batch = "stickDimsAtTheEnd")
    public static void stickDimsAtTheEnd(GameTestHelper helper) {
        Glowstick stick = stick(helper, GlowColor.WHITE, new Vec3(1.5, 2.2, 1.5));
        helper.assertTrue(stick.brightness() == 1.0F, "a fresh stick is fully bright");
        stick.setGlowLeft(Glowstick.FADE_TICKS / 10);
        helper.assertTrue(stick.brightness() < 0.2F && stick.brightness() > 0, "a nearly spent stick is dim but still lit");
        stick.discard();
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "pickingUpKeepsTheRemainingGlow")
    public static void pickingUpKeepsTheRemainingGlow(GameTestHelper helper) {
        floor(helper);
        Glowstick stick = stick(helper, GlowColor.RED, new Vec3(1.5, 2.2, 1.5));
        stick.setGlowLeft(500);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        stick.interact(player, InteractionHand.MAIN_HAND);

        helper.assertFalse(stick.isAlive(), "the stick was picked up");
        helper.assertTrue(player.getInventory().contains(stack -> stack.is(ModItems.glowstick(GlowColor.RED))
                && Integer.valueOf(500).equals(stack.get(ModDataComponents.GLOW_LEFT.get()))), "a red glowstick with 500 ticks left");
        helper.assertTrue(GlowstickItem.glowLeft(new ItemStack(ModItems.glowstick(GlowColor.RED))) == Config.glowTicks(),
                "a fresh stick glows for the full time");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "hittingKnocksTheStickAway")
    public static void hittingKnocksTheStickAway(GameTestHelper helper) {
        floor(helper);
        Glowstick stick = stick(helper, GlowColor.GREEN, new Vec3(1.5, 2.2, 1.5));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(stick.hurt(player.damageSources().playerAttack(player), 1.0F), "the hit lands");
            helper.assertTrue(stick.getDeltaMovement().y > 0.1, "the stick is knocked up");
            stick.discard();
            helper.succeed();
        });
    }

    /** A stone floor block with free space above; the test area is otherwise fenced with barriers. */
    private static void floor(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, Blocks.STONE);
        for (int y = 2; y <= 5; y++) {
            helper.setBlock(1, y, 1, Blocks.AIR);
        }
    }

    private static Glowstick stick(GameTestHelper helper, GlowColor color, Vec3 relative) {
        Vec3 pos = helper.absoluteVec(relative);
        // Test areas are not always in entity-ticking chunks; forcing the chunk makes the stick tick.
        helper.getLevel().setChunkForced(SectionPos.blockToSectionCoord(pos.x), SectionPos.blockToSectionCoord(pos.z), true);
        Glowstick stick = Glowstick.at(helper.getLevel(), pos, new ItemStack(ModItems.glowstick(color)));
        helper.getLevel().addFreshEntity(stick);
        return stick;
    }
}
