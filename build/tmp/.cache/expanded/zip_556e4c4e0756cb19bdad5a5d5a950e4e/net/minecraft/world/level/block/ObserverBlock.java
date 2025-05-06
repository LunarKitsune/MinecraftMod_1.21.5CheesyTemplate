package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.level.redstone.ExperimentalRedstoneUtils;
import net.minecraft.world.level.redstone.Orientation;

public class ObserverBlock extends DirectionalBlock {
    public static final MapCodec<ObserverBlock> CODEC = simpleCodec(ObserverBlock::new);
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    @Override
    public MapCodec<ObserverBlock> codec() {
        return CODEC;
    }

    public ObserverBlock(BlockBehaviour.Properties p_55085_) {
        super(p_55085_);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.SOUTH).setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACING, POWERED);
    }

    @Override
    protected BlockState rotate(BlockState pState, Rotation pRot) {
        return pState.setValue(FACING, pRot.rotate(pState.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState pState, Mirror pMirror) {
        return pState.rotate(pMirror.getRotation(pState.getValue(FACING)));
    }

    @Override
    protected void tick(BlockState p_221840_, ServerLevel p_221841_, BlockPos p_221842_, RandomSource p_221843_) {
        if (p_221840_.getValue(POWERED)) {
            p_221841_.setBlock(p_221842_, p_221840_.setValue(POWERED, false), 2);
        } else {
            p_221841_.setBlock(p_221842_, p_221840_.setValue(POWERED, true), 2);
            p_221841_.scheduleTick(p_221842_, this, 2);
        }

        this.updateNeighborsInFront(p_221841_, p_221842_, p_221840_);
    }

    @Override
    protected BlockState updateShape(
        BlockState p_55118_,
        LevelReader p_362436_,
        ScheduledTickAccess p_366728_,
        BlockPos p_55122_,
        Direction p_55119_,
        BlockPos p_55123_,
        BlockState p_55120_,
        RandomSource p_368192_
    ) {
        if (p_55118_.getValue(FACING) == p_55119_ && !p_55118_.getValue(POWERED)) {
            this.startSignal(p_362436_, p_366728_, p_55122_);
        }

        return super.updateShape(p_55118_, p_362436_, p_366728_, p_55122_, p_55119_, p_55123_, p_55120_, p_368192_);
    }

    private void startSignal(LevelReader pLevel, ScheduledTickAccess pScheduledTickAccess, BlockPos pPos) {
        if (!pLevel.isClientSide() && !pScheduledTickAccess.getBlockTicks().hasScheduledTick(pPos, this)) {
            pScheduledTickAccess.scheduleTick(pPos, this, 2);
        }
    }

    protected void updateNeighborsInFront(Level pLevel, BlockPos pPos, BlockState pState) {
        Direction direction = pState.getValue(FACING);
        BlockPos blockpos = pPos.relative(direction.getOpposite());
        Orientation orientation = ExperimentalRedstoneUtils.initialOrientation(pLevel, direction.getOpposite(), null);
        pLevel.neighborChanged(blockpos, this, orientation);
        pLevel.updateNeighborsAtExceptFromFacing(blockpos, this, direction, orientation);
    }

    @Override
    protected boolean isSignalSource(BlockState pState) {
        return true;
    }

    @Override
    protected int getDirectSignal(BlockState pBlockState, BlockGetter pBlockAccess, BlockPos pPos, Direction pSide) {
        return pBlockState.getSignal(pBlockAccess, pPos, pSide);
    }

    @Override
    protected int getSignal(BlockState pBlockState, BlockGetter pBlockAccess, BlockPos pPos, Direction pSide) {
        return pBlockState.getValue(POWERED) && pBlockState.getValue(FACING) == pSide ? 15 : 0;
    }

    @Override
    protected void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
        if (!pState.is(pOldState.getBlock())) {
            if (!pLevel.isClientSide() && pState.getValue(POWERED) && !pLevel.getBlockTicks().hasScheduledTick(pPos, this)) {
                BlockState blockstate = pState.setValue(POWERED, false);
                pLevel.setBlock(pPos, blockstate, 18);
                this.updateNeighborsInFront(pLevel, pPos, blockstate);
            }
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState p_394748_, ServerLevel p_398030_, BlockPos p_392130_, boolean p_394233_) {
        if (p_394748_.getValue(POWERED) && p_398030_.getBlockTicks().hasScheduledTick(p_392130_, this)) {
            this.updateNeighborsInFront(p_398030_, p_392130_, p_394748_.setValue(POWERED, false));
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        return this.defaultBlockState().setValue(FACING, pContext.getNearestLookingDirection().getOpposite().getOpposite());
    }
}