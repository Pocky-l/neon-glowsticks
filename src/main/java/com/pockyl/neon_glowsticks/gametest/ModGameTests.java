package com.pockyl.neon_glowsticks.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import com.pockyl.neon_glowsticks.Config;
import com.pockyl.neon_glowsticks.NeonGlowsticks;
import com.pockyl.neon_glowsticks.block.GlowLightBlock;
import com.pockyl.neon_glowsticks.entity.Glowstick;
import com.pockyl.neon_glowsticks.item.GlowColor;
import com.pockyl.neon_glowsticks.item.GlowstickItem;
import com.pockyl.neon_glowsticks.registry.ModBlocks;
import com.pockyl.neon_glowsticks.registry.ModDataComponents;
import com.pockyl.neon_glowsticks.registry.ModItems;

/**
 * In-game tests, run headless by {@code gradlew runGameTestServer}.
 * Tests use the 1x1x1 {@code empty} structure and build what they need around it; every test has its own batch, so
 * sticks of one test never claim the light of another.
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

    @GameTest(template = "empty", batch = "landedStickLightsItsBlock")
    public static void landedStickLightsItsBlock(GameTestHelper helper) {
        floor(helper);
        Glowstick stick = stick(helper, GlowColor.BLUE, new Vec3(1.5, 4, 1.5));
        BlockPos light = new BlockPos(1, 2, 1);
        helper.succeedWhen(() -> {
            helper.assertTrue(stick.onGround(), "the stick fell onto the stone");
            assertLight(helper, light, Config.lightLevel());
        });
    }

    @GameTest(template = "empty", batch = "lightGoesWithTheStick")
    public static void lightGoesWithTheStick(GameTestHelper helper) {
        floor(helper);
        Glowstick stick = stick(helper, GlowColor.RED, new Vec3(1.5, 2.2, 1.5));
        BlockPos light = new BlockPos(1, 2, 1);
        helper.runAfterDelay(10, () -> {
            assertLight(helper, light, Config.lightLevel());
            stick.discard();
            helper.assertBlockPresent(Blocks.AIR, light);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", batch = "lightInWaterKeepsTheWater")
    public static void lightInWaterKeepsTheWater(GameTestHelper helper) {
        floor(helper);
        helper.setBlock(1, 2, 1, Blocks.WATER);
        Glowstick stick = stick(helper, GlowColor.GREEN, new Vec3(1.5, 2.2, 1.5));
        BlockPos light = new BlockPos(1, 2, 1);
        helper.runAfterDelay(10, () -> {
            BlockState state = helper.getBlockState(light);
            helper.assertTrue(state.is(ModBlocks.GLOW_LIGHT.get()) && state.getValue(GlowLightBlock.WATERLOGGED),
                    "a waterlogged light replaced the water");
            stick.discard();
            helper.assertTrue(helper.getBlockState(light).is(Blocks.WATER), "the water is back");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", batch = "orphanedLightRemovesItself", timeoutTicks = GlowLightBlock.CHECK_INTERVAL * 3)
    public static void orphanedLightRemovesItself(GameTestHelper helper) {
        BlockPos light = new BlockPos(1, 2, 1);
        helper.setBlock(light, ModBlocks.GLOW_LIGHT.get().defaultBlockState());
        helper.succeedWhen(() -> helper.assertBlockPresent(Blocks.AIR, light));
    }

    @GameTest(template = "empty", batch = "stickBurnsOut")
    public static void stickBurnsOut(GameTestHelper helper) {
        floor(helper);
        Glowstick stick = stick(helper, GlowColor.WHITE, new Vec3(1.5, 2.2, 1.5));
        stick.setGlowLeft(15);
        BlockPos light = new BlockPos(1, 2, 1);
        helper.runAfterDelay(5, () -> helper.assertTrue(helper.getBlockState(light).is(ModBlocks.GLOW_LIGHT.get()), "it glows at first"));
        helper.runAfterDelay(25, () -> {
            helper.assertFalse(stick.isAlive(), "the stick went out");
            helper.assertBlockPresent(Blocks.AIR, light);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", batch = "dimmingStickGivesLessLight")
    public static void dimmingStickGivesLessLight(GameTestHelper helper) {
        Glowstick stick = stick(helper, GlowColor.WHITE, new Vec3(1.5, 2.2, 1.5));
        int full = stick.lightLevel();
        stick.setGlowLeft(Glowstick.FADE_TICKS / 10);
        helper.assertTrue(stick.lightLevel() < full && stick.lightLevel() >= 1, "a nearly spent stick is dimmer but still lit");
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
        helper.assertTrue(player.getInventory().contains(stack -> stack.is(ModItems.RED_GLOWSTICK.get())
                && Integer.valueOf(500).equals(stack.get(ModDataComponents.GLOW_LEFT.get()))), "a red glowstick with 500 ticks left");
        helper.assertTrue(GlowstickItem.glowLeft(new ItemStack(ModItems.RED_GLOWSTICK.get())) == Config.glowTicks(),
                "a fresh stick glows for the full time");
        helper.succeed();
    }

    /** A stone floor block with free space above; the test area is otherwise fenced with barriers. */
    private static void floor(GameTestHelper helper) {
        helper.setBlock(1, 1, 1, Blocks.STONE);
        for (int y = 2; y <= 5; y++) {
            helper.setBlock(1, y, 1, Blocks.AIR);
        }
    }

    private static Glowstick stick(GameTestHelper helper, GlowColor color, Vec3 relative) {
        Glowstick stick = Glowstick.at(helper.getLevel(), helper.absoluteVec(relative), new ItemStack(ModItems.glowstick(color)));
        helper.getLevel().addFreshEntity(stick);
        return stick;
    }

    private static void assertLight(GameTestHelper helper, BlockPos relative, int level) {
        BlockState state = helper.getBlockState(relative);
        helper.assertTrue(state.is(ModBlocks.GLOW_LIGHT.get()), "a glow light at " + relative);
        helper.assertTrue(state.getValue(GlowLightBlock.LEVEL) == level, "light level " + level);
    }
}
