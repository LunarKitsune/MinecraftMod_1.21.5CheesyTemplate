package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SeaPickleBlock extends VegetationBlock implements BonemealableBlock, SimpleWaterloggedBlock {
    public static final MapCodec<SeaPickleBlock> CODEC = simpleCodec(SeaPickleBlock::new);
    public static final int MAX_PICKLES = 4;
    public static final IntegerProperty PICKLES = BlockStateProperties.PICKLES;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final VoxelShape SHAPE_ONE = Block.column(4.0, 0.0, 6.0);
    private static final VoxelShape SHAPE_TWO = Block.column(10.0, 0.0, 6.0);
    private static final VoxelShape SHAPE_THREE = Block.column(12.0, 0.0, 6.0);
    private static final VoxelShape SHAPE_FOUR = Block.column(12.0, 0.0, 7.0);

    @Override
    public MapCodec<SeaPickleBlock> codec() {
        return CODEC;
    }

    public SeaPickleBlock(BlockBehaviour.Properties p_56082_) {
        super(p_56082_);
        this.registerDefaultState(this.stateDefinition.any().setValue(PICKLES, 1).setValue(WATERLOGGED, true));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        BlockState blockstate = pContext.getLevel().getBlockState(pContext.getClickedPos());
        if (blockstate.is(this)) {
            return blockstate.setValue(PICKLES, Math.min(4, blockstate.getValue(PICKLES) + 1));
        } else {
            FluidState fluidstate = pContext.getLevel().getFluidState(pContext.getClickedPos());
            boolean flag = fluidstate.getType() == Fluids.WATER;
            return super.getStateForPlacement(pContext).setValue(WATERLOGGED, flag);
        }
    }

    public static boolean isDead(BlockState pState) {
        return !pState.getValue(WATERLOGGED);
    }

    @Override
    protected boolean mayPlaceOn(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        return !pState.getCollisionShape(pLevel, pPos).getFaceShape(Direction.UP).isEmpty() || pState.isFaceSturdy(pLevel, pPos, Direction.UP);
    }

    @Override
    protected boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
        BlockPos blockpos = pPos.below();
        return this.mayPlaceOn(pLevel.getBlockState(blockpos), pLevel, blockpos);
    }

    @Override
    protected BlockState updateShape(
        BlockState p_56113_,
        LevelReader p_365386_,
        ScheduledTickAccess p_362722_,
        BlockPos p_56117_,
        Direction p_56114_,
        BlockPos p_56118_,
        BlockState p_56115_,
        RandomSource p_366416_
    ) {
        if (!p_56113_.canSurvive(p_365386_, p_56117_)) {
            return Blocks.AIR.defaultBlockState();
        } else {
            if (p_56113_.getValue(WATERLOGGED)) {
                p_362722_.scheduleTick(p_56117_, Fluids.WATER, Fluids.WATER.getTickDelay(p_365386_));
            }

            return super.updateShape(p_56113_, p_365386_, p_362722_, p_56117_, p_56114_, p_56118_, p_56115_, p_366416_);
        }
    }

    @Override
    protected boolean canBeReplaced(BlockState pState, BlockPlaceContext pUseContext) {
        return !pUseContext.isSecondaryUseActive() && pUseContext.getItemInHand().is(this.asItem()) && pState.getValue(PICKLES) < 4
            ? true
            : super.canBeReplaced(pState, pUseContext);
    }

    @Override
    protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return switch (pState.getValue(PICKLES)) {
            case 2 -> SHAPE_TWO;
            case 3 -> SHAPE_THREE;
            case 4 -> SHAPE_FOUR;
            default -> SHAPE_ONE;
        };
    }

    @Override
    protected FluidState getFluidState(BlockState pState) {
        return pState.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(pState);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(PICKLES, WATERLOGGED);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader p_255984_, BlockPos p_56092_, BlockState p_56093_) {
        return !isDead(p_56093_) && p_255984_.getBlockState(p_56092_.below()).is(BlockTags.CORAL_BLOCKS);
    }

    @Override
    public boolean isBonemealSuccess(Level p_222418_, RandomSource p_222419_, BlockPos p_222420_, BlockState p_222421_) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel p_222413_, RandomSource p_222414_, BlockPos p_222415_, BlockState p_222416_) {
        int i = 5;
        int j = 1;
        int k = 2;
        int l = 0;
        int i1 = p_222415_.getX() - 2;
        int j1 = 0;

        for (int k1 = 0; k1 < 5; k1++) {
            for (int l1 = 0; l1 < j; l1++) {
                int i2 = 2 + p_222415_.getY() - 1;

                for (int j2 = i2 - 2; j2 < i2; j2++) {
                    BlockPos blockpos = new BlockPos(i1 + k1, j2, p_222415_.getZ() - j1 + l1);
                    if (blockpos != p_222415_ && p_222414_.nextInt(6) == 0 && p_222413_.getBlockState(blockpos).is(Blocks.WATER)) {
                        BlockState blockstate = p_222413_.getBlockState(blockpos.below());
                        if (blockstate.is(BlockTags.CORAL_BLOCKS)) {
                            p_222413_.setBlock(blockpos, Blocks.SEA_PICKLE.defaultBlockState().setValue(PICKLES, p_222414_.nextInt(4) + 1), 3);
                        }
                    }
                }
            }

            if (l < 2) {
                j += 2;
                j1++;
            } else {
                j -= 2;
                j1--;
            }

            l++;
        }

        p_222413_.setBlock(p_222415_, p_222416_.setValue(PICKLES, 4), 2);
    }

    @Override
    protected boolean isPathfindable(BlockState p_56104_, PathComputationType p_56107_) {
        return false;
    }
}