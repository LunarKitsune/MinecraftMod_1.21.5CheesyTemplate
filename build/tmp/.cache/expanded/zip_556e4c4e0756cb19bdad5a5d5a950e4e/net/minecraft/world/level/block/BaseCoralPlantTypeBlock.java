package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class BaseCoralPlantTypeBlock extends Block implements SimpleWaterloggedBlock {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final VoxelShape SHAPE = Block.column(12.0, 0.0, 4.0);

    protected BaseCoralPlantTypeBlock(BlockBehaviour.Properties p_49161_) {
        super(p_49161_);
        this.registerDefaultState(this.stateDefinition.any().setValue(WATERLOGGED, true));
    }

    @Override
    protected abstract MapCodec<? extends BaseCoralPlantTypeBlock> codec();

    protected void tryScheduleDieTick(BlockState pState, BlockGetter pLevel, ScheduledTickAccess pScheduledTickAccess, RandomSource pRandom, BlockPos pPos) {
        if (!scanForWater(pState, pLevel, pPos)) {
            pScheduledTickAccess.scheduleTick(pPos, this, 60 + pRandom.nextInt(40));
        }
    }

    protected static boolean scanForWater(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        if (pState.getValue(WATERLOGGED)) {
            return true;
        } else {
            for (Direction direction : Direction.values()) {
                if (pLevel.getFluidState(pPos.relative(direction)).is(FluidTags.WATER)) {
                    return true;
                }
            }

            return false;
        }
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        FluidState fluidstate = pContext.getLevel().getFluidState(pContext.getClickedPos());
        return this.defaultBlockState().setValue(WATERLOGGED, fluidstate.is(FluidTags.WATER) && fluidstate.getAmount() == 8);
    }

    @Override
    protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return SHAPE;
    }

    @Override
    protected BlockState updateShape(
        BlockState p_49173_,
        LevelReader p_363497_,
        ScheduledTickAccess p_366125_,
        BlockPos p_49177_,
        Direction p_49174_,
        BlockPos p_49178_,
        BlockState p_49175_,
        RandomSource p_365580_
    ) {
        if (p_49173_.getValue(WATERLOGGED)) {
            p_366125_.scheduleTick(p_49177_, Fluids.WATER, Fluids.WATER.getTickDelay(p_363497_));
        }

        return p_49174_ == Direction.DOWN && !this.canSurvive(p_49173_, p_363497_, p_49177_)
            ? Blocks.AIR.defaultBlockState()
            : super.updateShape(p_49173_, p_363497_, p_366125_, p_49177_, p_49174_, p_49178_, p_49175_, p_365580_);
    }

    @Override
    protected boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
        BlockPos blockpos = pPos.below();
        return pLevel.getBlockState(blockpos).isFaceSturdy(pLevel, blockpos, Direction.UP);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(WATERLOGGED);
    }

    @Override
    protected FluidState getFluidState(BlockState pState) {
        return pState.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(pState);
    }
}