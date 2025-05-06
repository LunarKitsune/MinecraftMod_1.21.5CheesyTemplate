package net.minecraft.world.level.block;

import com.google.common.base.MoreObjects;
import com.mojang.serialization.MapCodec;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.redstone.ExperimentalRedstoneUtils;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class TripWireHookBlock extends Block {
    public static final MapCodec<TripWireHookBlock> CODEC = simpleCodec(TripWireHookBlock::new);
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty ATTACHED = BlockStateProperties.ATTACHED;
    protected static final int WIRE_DIST_MIN = 1;
    protected static final int WIRE_DIST_MAX = 42;
    private static final int RECHECK_PERIOD = 10;
    private static final Map<Direction, VoxelShape> SHAPES = Shapes.rotateHorizontal(Block.boxZ(6.0, 0.0, 10.0, 10.0, 16.0));

    @Override
    public MapCodec<TripWireHookBlock> codec() {
        return CODEC;
    }

    public TripWireHookBlock(BlockBehaviour.Properties p_57676_) {
        super(p_57676_);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, false).setValue(ATTACHED, false));
    }

    @Override
    protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return SHAPES.get(pState.getValue(FACING));
    }

    @Override
    protected boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
        Direction direction = pState.getValue(FACING);
        BlockPos blockpos = pPos.relative(direction.getOpposite());
        BlockState blockstate = pLevel.getBlockState(blockpos);
        return direction.getAxis().isHorizontal() && blockstate.isFaceSturdy(pLevel, blockpos, direction);
    }

    @Override
    protected BlockState updateShape(
        BlockState p_57731_,
        LevelReader p_368766_,
        ScheduledTickAccess p_365650_,
        BlockPos p_57735_,
        Direction p_57732_,
        BlockPos p_57736_,
        BlockState p_57733_,
        RandomSource p_361546_
    ) {
        return p_57732_.getOpposite() == p_57731_.getValue(FACING) && !p_57731_.canSurvive(p_368766_, p_57735_)
            ? Blocks.AIR.defaultBlockState()
            : super.updateShape(p_57731_, p_368766_, p_365650_, p_57735_, p_57732_, p_57736_, p_57733_, p_361546_);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        BlockState blockstate = this.defaultBlockState().setValue(POWERED, false).setValue(ATTACHED, false);
        LevelReader levelreader = pContext.getLevel();
        BlockPos blockpos = pContext.getClickedPos();
        Direction[] adirection = pContext.getNearestLookingDirections();

        for (Direction direction : adirection) {
            if (direction.getAxis().isHorizontal()) {
                Direction direction1 = direction.getOpposite();
                blockstate = blockstate.setValue(FACING, direction1);
                if (blockstate.canSurvive(levelreader, blockpos)) {
                    return blockstate;
                }
            }
        }

        return null;
    }

    @Override
    public void setPlacedBy(Level pLevel, BlockPos pPos, BlockState pState, LivingEntity pPlacer, ItemStack pStack) {
        calculateState(pLevel, pPos, pState, false, false, -1, null);
    }

    public static void calculateState(
        Level pLevel, BlockPos pPos, BlockState pHookState, boolean pAttaching, boolean pShouldNotifyNeighbours, int pSearchRange, @Nullable BlockState pState
    ) {
        Optional<Direction> optional = pHookState.getOptionalValue(FACING);
        if (optional.isPresent()) {
            Direction direction = optional.get();
            boolean flag = pHookState.getOptionalValue(ATTACHED).orElse(false);
            boolean flag1 = pHookState.getOptionalValue(POWERED).orElse(false);
            Block block = pHookState.getBlock();
            boolean flag2 = !pAttaching;
            boolean flag3 = false;
            int i = 0;
            BlockState[] ablockstate = new BlockState[42];

            for (int j = 1; j < 42; j++) {
                BlockPos blockpos = pPos.relative(direction, j);
                BlockState blockstate = pLevel.getBlockState(blockpos);
                if (blockstate.is(Blocks.TRIPWIRE_HOOK)) {
                    if (blockstate.getValue(FACING) == direction.getOpposite()) {
                        i = j;
                    }
                    break;
                }

                if (!blockstate.is(Blocks.TRIPWIRE) && j != pSearchRange) {
                    ablockstate[j] = null;
                    flag2 = false;
                } else {
                    if (j == pSearchRange) {
                        blockstate = MoreObjects.firstNonNull(pState, blockstate);
                    }

                    boolean flag4 = !blockstate.getValue(TripWireBlock.DISARMED);
                    boolean flag5 = blockstate.getValue(TripWireBlock.POWERED);
                    flag3 |= flag4 && flag5;
                    ablockstate[j] = blockstate;
                    if (j == pSearchRange) {
                        pLevel.scheduleTick(pPos, block, 10);
                        flag2 &= flag4;
                    }
                }
            }

            flag2 &= i > 1;
            flag3 &= flag2;
            BlockState blockstate1 = block.defaultBlockState().trySetValue(ATTACHED, flag2).trySetValue(POWERED, flag3);
            if (i > 0) {
                BlockPos blockpos1 = pPos.relative(direction, i);
                Direction direction1 = direction.getOpposite();
                pLevel.setBlock(blockpos1, blockstate1.setValue(FACING, direction1), 3);
                notifyNeighbors(block, pLevel, blockpos1, direction1);
                emitState(pLevel, blockpos1, flag2, flag3, flag, flag1);
            }

            emitState(pLevel, pPos, flag2, flag3, flag, flag1);
            if (!pAttaching) {
                pLevel.setBlock(pPos, blockstate1.setValue(FACING, direction), 3);
                if (pShouldNotifyNeighbours) {
                    notifyNeighbors(block, pLevel, pPos, direction);
                }
            }

            if (flag != flag2) {
                for (int k = 1; k < i; k++) {
                    BlockPos blockpos2 = pPos.relative(direction, k);
                    BlockState blockstate2 = ablockstate[k];
                    if (blockstate2 != null) {
                        BlockState blockstate3 = pLevel.getBlockState(blockpos2);
                        if (blockstate3.is(Blocks.TRIPWIRE) || blockstate3.is(Blocks.TRIPWIRE_HOOK)) {
                            pLevel.setBlock(blockpos2, blockstate2.trySetValue(ATTACHED, flag2), 3);
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void tick(BlockState p_222610_, ServerLevel p_222611_, BlockPos p_222612_, RandomSource p_222613_) {
        calculateState(p_222611_, p_222612_, p_222610_, false, true, -1, null);
    }

    private static void emitState(Level pLevel, BlockPos pPos, boolean pAttached, boolean pPowered, boolean pWasAttached, boolean pWasPowered) {
        if (pPowered && !pWasPowered) {
            pLevel.playSound(null, pPos, SoundEvents.TRIPWIRE_CLICK_ON, SoundSource.BLOCKS, 0.4F, 0.6F);
            pLevel.gameEvent(null, GameEvent.BLOCK_ACTIVATE, pPos);
        } else if (!pPowered && pWasPowered) {
            pLevel.playSound(null, pPos, SoundEvents.TRIPWIRE_CLICK_OFF, SoundSource.BLOCKS, 0.4F, 0.5F);
            pLevel.gameEvent(null, GameEvent.BLOCK_DEACTIVATE, pPos);
        } else if (pAttached && !pWasAttached) {
            pLevel.playSound(null, pPos, SoundEvents.TRIPWIRE_ATTACH, SoundSource.BLOCKS, 0.4F, 0.7F);
            pLevel.gameEvent(null, GameEvent.BLOCK_ATTACH, pPos);
        } else if (!pAttached && pWasAttached) {
            pLevel.playSound(null, pPos, SoundEvents.TRIPWIRE_DETACH, SoundSource.BLOCKS, 0.4F, 1.2F / (pLevel.random.nextFloat() * 0.2F + 0.9F));
            pLevel.gameEvent(null, GameEvent.BLOCK_DETACH, pPos);
        }
    }

    private static void notifyNeighbors(Block pBlock, Level pLevel, BlockPos pPos, Direction pDirection) {
        Direction direction = pDirection.getOpposite();
        Orientation orientation = ExperimentalRedstoneUtils.initialOrientation(pLevel, direction, Direction.UP);
        pLevel.updateNeighborsAt(pPos, pBlock, orientation);
        pLevel.updateNeighborsAt(pPos.relative(direction), pBlock, orientation);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState p_394362_, ServerLevel p_392228_, BlockPos p_392478_, boolean p_391414_) {
        if (!p_391414_) {
            boolean flag = p_394362_.getValue(ATTACHED);
            boolean flag1 = p_394362_.getValue(POWERED);
            if (flag || flag1) {
                calculateState(p_392228_, p_392478_, p_394362_, true, false, -1, null);
            }

            if (flag1) {
                notifyNeighbors(this, p_392228_, p_392478_, p_394362_.getValue(FACING));
            }
        }
    }

    @Override
    protected int getSignal(BlockState pBlockState, BlockGetter pBlockAccess, BlockPos pPos, Direction pSide) {
        return pBlockState.getValue(POWERED) ? 15 : 0;
    }

    @Override
    protected int getDirectSignal(BlockState pBlockState, BlockGetter pBlockAccess, BlockPos pPos, Direction pSide) {
        if (!pBlockState.getValue(POWERED)) {
            return 0;
        } else {
            return pBlockState.getValue(FACING) == pSide ? 15 : 0;
        }
    }

    @Override
    protected boolean isSignalSource(BlockState pState) {
        return true;
    }

    @Override
    protected BlockState rotate(BlockState pState, Rotation pRotation) {
        return pState.setValue(FACING, pRotation.rotate(pState.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState pState, Mirror pMirror) {
        return pState.rotate(pMirror.getRotation(pState.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACING, POWERED, ATTACHED);
    }
}