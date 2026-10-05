package com.pockyl.neon_glowsticks.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import com.pockyl.neon_glowsticks.entity.Glowstick;
import com.pockyl.neon_glowsticks.registry.ModBlocks;

/**
 * Invisible, non-solid light source kept next to a thrown glowstick. The sticks place and move it; the block itself
 * checks every couple of seconds that a stick still claims it and removes itself otherwise, so no light is left behind
 * after a crash, a /kill or anything else that skips the stick's own clean-up.
 */
public final class GlowLightBlock extends Block implements SimpleWaterloggedBlock {
    public static final IntegerProperty LEVEL = BlockStateProperties.LEVEL;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final int CHECK_INTERVAL = 40;

    public GlowLightBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LEVEL, 15).setValue(WATERLOGGED, false));
    }

    public static int lightEmission(BlockState state) {
        return state.getValue(LEVEL);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL, WATERLOGGED);
    }

    // ------------------------------------------------------------------------------------------------
    // Placement by glowsticks
    // ------------------------------------------------------------------------------------------------

    /** Whether a glow light may occupy this block: air, a water source or an existing glow light. */
    public static boolean canHost(BlockState state) {
        return state.isAir() || state.getBlock() instanceof GlowLightBlock
                || state.is(Blocks.WATER) && state.getFluidState().isSource();
    }

    /** Puts a light of at least {@code light} at {@code pos}; a brighter one already there is left alone. */
    public static void place(Level level, BlockPos pos, int light) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof GlowLightBlock) {
            if (state.getValue(LEVEL) < light) {
                level.setBlock(pos, state.setValue(LEVEL, light), Block.UPDATE_CLIENTS);
            }
        } else if (canHost(state)) {
            boolean water = !state.isAir();
            level.setBlock(pos, ModBlocks.GLOW_LIGHT.get().defaultBlockState().setValue(LEVEL, light).setValue(WATERLOGGED, water),
                    Block.UPDATE_CLIENTS);
        }
    }

    /** Removes the light at {@code pos} unless another stick still claims it. */
    public static void release(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof GlowLightBlock && Glowstick.lightClaimedAt(level, pos) <= 0) {
            level.setBlock(pos, emptied(state), Block.UPDATE_CLIENTS);
        }
    }

    private static BlockState emptied(BlockState state) {
        return state.getValue(WATERLOGGED) ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState();
    }

    // ------------------------------------------------------------------------------------------------
    // Self check
    // ------------------------------------------------------------------------------------------------

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!oldState.is(this)) {
            level.scheduleTick(pos, this, CHECK_INTERVAL);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int light = Glowstick.lightClaimedAt(level, pos);
        if (light <= 0) {
            level.setBlock(pos, emptied(state), Block.UPDATE_CLIENTS);
            return;
        }
        if (light != state.getValue(LEVEL)) {
            level.setBlock(pos, state.setValue(LEVEL, light), Block.UPDATE_CLIENTS);
        }
        level.scheduleTick(pos, this, CHECK_INTERVAL);
    }

    // ------------------------------------------------------------------------------------------------
    // Invisible, non-solid, water-friendly
    // ------------------------------------------------------------------------------------------------

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return !state.getValue(WATERLOGGED);
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return type != PathComputationType.WATER || state.getValue(WATERLOGGED);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level, BlockPos pos,
            BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        return state;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return ItemStack.EMPTY;
    }
}
