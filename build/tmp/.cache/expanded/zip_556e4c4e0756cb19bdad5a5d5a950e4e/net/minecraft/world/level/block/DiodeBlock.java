package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.ExperimentalRedstoneUtils;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.ticks.TickPriority;

public abstract class DiodeBlock extends HorizontalDirectionalBlock {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    private static final VoxelShape SHAPE = Block.column(16.0, 0.0, 2.0);

    protected DiodeBlock(BlockBehaviour.Properties p_52499_) {
        super(p_52499_);
    }

    @Override
    protected abstract MapCodec<? extends DiodeBlock> codec();

    @Override
    protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
        BlockPos blockpos = pPos.below();
        return this.canSurviveOn(pLevel, blockpos, pLevel.getBlockState(blockpos));
    }

    protected boolean canSurviveOn(LevelReader pLevel, BlockPos pPos, BlockState pState) {
        return pState.isFaceSturdy(pLevel, pPos, Direction.UP, SupportType.RIGID);
    }

    @Override
    protected void tick(BlockState p_221065_, ServerLevel p_221066_, BlockPos p_221067_, RandomSource p_221068_) {
        if (!this.isLocked(p_221066_, p_221067_, p_221065_)) {
            boolean flag = p_221065_.getValue(POWERED);
            boolean flag1 = this.shouldTurnOn(p_221066_, p_221067_, p_221065_);
            if (flag && !flag1) {
                p_221066_.setBlock(p_221067_, p_221065_.setValue(POWERED, false), 2);
            } else if (!flag) {
                p_221066_.setBlock(p_221067_, p_221065_.setValue(POWERED, true), 2);
                if (!flag1) {
                    p_221066_.scheduleTick(p_221067_, this, this.getDelay(p_221065_), TickPriority.VERY_HIGH);
                }
            }
        }
    }

    @Override
    protected int getDirectSignal(BlockState pBlockState, BlockGetter pBlockAccess, BlockPos pPos, Direction pSide) {
        return pBlockState.getSignal(pBlockAccess, pPos, pSide);
    }

    @Override
    protected int getSignal(BlockState pBlockState, BlockGetter pBlockAccess, BlockPos pPos, Direction pSide) {
        if (!pBlockState.getValue(POWERED)) {
            return 0;
        } else {
            return pBlockState.getValue(FACING) == pSide ? this.getOutputSignal(pBlockAccess, pPos, pBlockState) : 0;
        }
    }

    @Override
    protected void neighborChanged(BlockState p_52525_, Level p_52526_, BlockPos p_52527_, Block p_52528_, @Nullable Orientation p_363702_, boolean p_52530_) {
        if (p_52525_.canSurvive(p_52526_, p_52527_)) {
            this.checkTickOnNeighbor(p_52526_, p_52527_, p_52525_);
        } else {
            BlockEntity blockentity = p_52525_.hasBlockEntity() ? p_52526_.getBlockEntity(p_52527_) : null;
            dropResources(p_52525_, p_52526_, p_52527_, blockentity);
            p_52526_.removeBlock(p_52527_, false);

            for (Direction direction : Direction.values()) {
                p_52526_.updateNeighborsAt(p_52527_.relative(direction), this);
            }
        }
    }

    protected void checkTickOnNeighbor(Level pLevel, BlockPos pPos, BlockState pState) {
        if (!this.isLocked(pLevel, pPos, pState)) {
            boolean flag = pState.getValue(POWERED);
            boolean flag1 = this.shouldTurnOn(pLevel, pPos, pState);
            if (flag != flag1 && !pLevel.getBlockTicks().willTickThisTick(pPos, this)) {
                TickPriority tickpriority = TickPriority.HIGH;
                if (this.shouldPrioritize(pLevel, pPos, pState)) {
                    tickpriority = TickPriority.EXTREMELY_HIGH;
                } else if (flag) {
                    tickpriority = TickPriority.VERY_HIGH;
                }

                pLevel.scheduleTick(pPos, this, this.getDelay(pState), tickpriority);
            }
        }
    }

    public boolean isLocked(LevelReader pLevel, BlockPos pPos, BlockState pState) {
        return false;
    }

    protected boolean shouldTurnOn(Level pLevel, BlockPos pPos, BlockState pState) {
        return this.getInputSignal(pLevel, pPos, pState) > 0;
    }

    protected int getInputSignal(Level pLevel, BlockPos pPos, BlockState pState) {
        Direction direction = pState.getValue(FACING);
        BlockPos blockpos = pPos.relative(direction);
        int i = pLevel.getSignal(blockpos, direction);
        if (i >= 15) {
            return i;
        } else {
            BlockState blockstate = pLevel.getBlockState(blockpos);
            return Math.max(i, blockstate.is(Blocks.REDSTONE_WIRE) ? blockstate.getValue(RedStoneWireBlock.POWER) : 0);
        }
    }

    protected int getAlternateSignal(SignalGetter pLevel, BlockPos pPos, BlockState pState) {
        Direction direction = pState.getValue(FACING);
        Direction direction1 = direction.getClockWise();
        Direction direction2 = direction.getCounterClockWise();
        boolean flag = this.sideInputDiodesOnly();
        return Math.max(
            pLevel.getControlInputSignal(pPos.relative(direction1), direction1, flag), pLevel.getControlInputSignal(pPos.relative(direction2), direction2, flag)
        );
    }

    @Override
    protected boolean isSignalSource(BlockState pState) {
        return true;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        return this.defaultBlockState().setValue(FACING, pContext.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level pLevel, BlockPos pPos, BlockState pState, LivingEntity pPlacer, ItemStack pStack) {
        if (this.shouldTurnOn(pLevel, pPos, pState)) {
            pLevel.scheduleTick(pPos, this, 1);
        }
    }

    @Override
    protected void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
        this.updateNeighborsInFront(pLevel, pPos, pState);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState p_397978_, ServerLevel p_391595_, BlockPos p_392425_, boolean p_397263_) {
        if (!p_397263_) {
            this.updateNeighborsInFront(p_391595_, p_392425_, p_397978_);
        }
    }

    protected void updateNeighborsInFront(Level pLevel, BlockPos pPos, BlockState pState) {
        Direction direction = pState.getValue(FACING);
        BlockPos blockpos = pPos.relative(direction.getOpposite());
        Orientation orientation = ExperimentalRedstoneUtils.initialOrientation(pLevel, direction.getOpposite(), Direction.UP);
        if (net.minecraftforge.event.ForgeEventFactory.onNeighborNotify(pLevel, pPos, pLevel.getBlockState(pPos), java.util.EnumSet.of(direction.getOpposite()), false)) {
            return;
        }
        pLevel.neighborChanged(blockpos, this, orientation);
        pLevel.updateNeighborsAtExceptFromFacing(blockpos, this, direction, orientation);
    }

    protected boolean sideInputDiodesOnly() {
        return false;
    }

    protected int getOutputSignal(BlockGetter pLevel, BlockPos pPos, BlockState pState) {
        return 15;
    }

    public static boolean isDiode(BlockState pState) {
        return pState.getBlock() instanceof DiodeBlock;
    }

    public boolean shouldPrioritize(BlockGetter pLevel, BlockPos pPos, BlockState pState) {
        Direction direction = pState.getValue(FACING).getOpposite();
        BlockState blockstate = pLevel.getBlockState(pPos.relative(direction));
        return isDiode(blockstate) && blockstate.getValue(FACING) != direction;
    }

    protected abstract int getDelay(BlockState pState);
}
