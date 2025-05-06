package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class VineBlock extends Block implements net.minecraftforge.common.IForgeShearable {
    public static final MapCodec<VineBlock> CODEC = simpleCodec(VineBlock::new);
    public static final BooleanProperty UP = PipeBlock.UP;
    public static final BooleanProperty NORTH = PipeBlock.NORTH;
    public static final BooleanProperty EAST = PipeBlock.EAST;
    public static final BooleanProperty SOUTH = PipeBlock.SOUTH;
    public static final BooleanProperty WEST = PipeBlock.WEST;
    public static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = PipeBlock.PROPERTY_BY_DIRECTION
        .entrySet()
        .stream()
        .filter(p_57886_ -> p_57886_.getKey() != Direction.DOWN)
        .collect(Util.toMap());
    private final Function<BlockState, VoxelShape> shapes;

    @Override
    public MapCodec<VineBlock> codec() {
        return CODEC;
    }

    public VineBlock(BlockBehaviour.Properties p_57847_) {
        super(p_57847_);
        this.registerDefaultState(
            this.stateDefinition
                .any()
                .setValue(UP, false)
                .setValue(NORTH, false)
                .setValue(EAST, false)
                .setValue(SOUTH, false)
                .setValue(WEST, false)
        );
        this.shapes = this.makeShapes();
    }

    private Function<BlockState, VoxelShape> makeShapes() {
        Map<Direction, VoxelShape> map = Shapes.rotateAll(Block.boxZ(16.0, 0.0, 1.0));
        return this.getShapeForEachState(p_390956_ -> {
            VoxelShape voxelshape = Shapes.empty();

            for (Entry<Direction, BooleanProperty> entry : PROPERTY_BY_DIRECTION.entrySet()) {
                if (p_390956_.getValue(entry.getValue())) {
                    voxelshape = Shapes.or(voxelshape, map.get(entry.getKey()));
                }
            }

            return voxelshape.isEmpty() ? Shapes.block() : voxelshape;
        });
    }

    @Override
    protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return this.shapes.apply(pState);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState p_181239_) {
        return true;
    }

    @Override
    protected boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
        return this.hasFaces(this.getUpdatedState(pState, pLevel, pPos));
    }

    private boolean hasFaces(BlockState pState) {
        return this.countFaces(pState) > 0;
    }

    private int countFaces(BlockState pState) {
        int i = 0;

        for (BooleanProperty booleanproperty : PROPERTY_BY_DIRECTION.values()) {
            if (pState.getValue(booleanproperty)) {
                i++;
            }
        }

        return i;
    }

    private boolean canSupportAtFace(BlockGetter pLevel, BlockPos pPos, Direction pDirection) {
        if (pDirection == Direction.DOWN) {
            return false;
        } else {
            BlockPos blockpos = pPos.relative(pDirection);
            if (isAcceptableNeighbour(pLevel, blockpos, pDirection)) {
                return true;
            } else if (pDirection.getAxis() == Direction.Axis.Y) {
                return false;
            } else {
                BooleanProperty booleanproperty = PROPERTY_BY_DIRECTION.get(pDirection);
                BlockState blockstate = pLevel.getBlockState(pPos.above());
                return blockstate.is(this) && blockstate.getValue(booleanproperty);
            }
        }
    }

    public static boolean isAcceptableNeighbour(BlockGetter pBlockReader, BlockPos pNeighborPos, Direction pAttachedFace) {
        return MultifaceBlock.canAttachTo(pBlockReader, pAttachedFace, pNeighborPos, pBlockReader.getBlockState(pNeighborPos));
    }

    private BlockState getUpdatedState(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        BlockPos blockpos = pPos.above();
        if (pState.getValue(UP)) {
            pState = pState.setValue(UP, isAcceptableNeighbour(pLevel, blockpos, Direction.DOWN));
        }

        BlockState blockstate = null;

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BooleanProperty booleanproperty = getPropertyForFace(direction);
            if (pState.getValue(booleanproperty)) {
                boolean flag = this.canSupportAtFace(pLevel, pPos, direction);
                if (!flag) {
                    if (blockstate == null) {
                        blockstate = pLevel.getBlockState(blockpos);
                    }

                    flag = blockstate.is(this) && blockstate.getValue(booleanproperty);
                }

                pState = pState.setValue(booleanproperty, flag);
            }
        }

        return pState;
    }

    @Override
    protected BlockState updateShape(
        BlockState p_57875_,
        LevelReader p_367171_,
        ScheduledTickAccess p_369591_,
        BlockPos p_57879_,
        Direction p_57876_,
        BlockPos p_57880_,
        BlockState p_57877_,
        RandomSource p_364384_
    ) {
        if (p_57876_ == Direction.DOWN) {
            return super.updateShape(p_57875_, p_367171_, p_369591_, p_57879_, p_57876_, p_57880_, p_57877_, p_364384_);
        } else {
            BlockState blockstate = this.getUpdatedState(p_57875_, p_367171_, p_57879_);
            return !this.hasFaces(blockstate) ? Blocks.AIR.defaultBlockState() : blockstate;
        }
    }

    @Override
    protected void randomTick(BlockState p_222655_, ServerLevel p_222656_, BlockPos p_222657_, RandomSource p_222658_) {
        if (p_222656_.getGameRules().getBoolean(GameRules.RULE_DO_VINES_SPREAD)) {
            if (p_222656_.random.nextInt(4) == 0 && p_222656_.isAreaLoaded(p_222657_, 4)) { // Forge: check area to prevent loading unloaded chunks
                Direction direction = Direction.getRandom(p_222658_);
                BlockPos blockpos = p_222657_.above();
                if (direction.getAxis().isHorizontal() && !p_222655_.getValue(getPropertyForFace(direction))) {
                    if (this.canSpread(p_222656_, p_222657_)) {
                        BlockPos blockpos4 = p_222657_.relative(direction);
                        BlockState blockstate4 = p_222656_.getBlockState(blockpos4);
                        if (blockstate4.isAir()) {
                            Direction direction3 = direction.getClockWise();
                            Direction direction4 = direction.getCounterClockWise();
                            boolean flag = p_222655_.getValue(getPropertyForFace(direction3));
                            boolean flag1 = p_222655_.getValue(getPropertyForFace(direction4));
                            BlockPos blockpos2 = blockpos4.relative(direction3);
                            BlockPos blockpos3 = blockpos4.relative(direction4);
                            if (flag && isAcceptableNeighbour(p_222656_, blockpos2, direction3)) {
                                p_222656_.setBlock(blockpos4, this.defaultBlockState().setValue(getPropertyForFace(direction3), true), 2);
                            } else if (flag1 && isAcceptableNeighbour(p_222656_, blockpos3, direction4)) {
                                p_222656_.setBlock(blockpos4, this.defaultBlockState().setValue(getPropertyForFace(direction4), true), 2);
                            } else {
                                Direction direction1 = direction.getOpposite();
                                if (flag && p_222656_.isEmptyBlock(blockpos2) && isAcceptableNeighbour(p_222656_, p_222657_.relative(direction3), direction1)) {
                                    p_222656_.setBlock(blockpos2, this.defaultBlockState().setValue(getPropertyForFace(direction1), true), 2);
                                } else if (flag1 && p_222656_.isEmptyBlock(blockpos3) && isAcceptableNeighbour(p_222656_, p_222657_.relative(direction4), direction1)) {
                                    p_222656_.setBlock(blockpos3, this.defaultBlockState().setValue(getPropertyForFace(direction1), true), 2);
                                } else if (p_222658_.nextFloat() < 0.05 && isAcceptableNeighbour(p_222656_, blockpos4.above(), Direction.UP)) {
                                    p_222656_.setBlock(blockpos4, this.defaultBlockState().setValue(UP, true), 2);
                                }
                            }
                        } else if (isAcceptableNeighbour(p_222656_, blockpos4, direction)) {
                            p_222656_.setBlock(p_222657_, p_222655_.setValue(getPropertyForFace(direction), true), 2);
                        }
                    }
                } else {
                    if (direction == Direction.UP && p_222657_.getY() < p_222656_.getMaxY()) {
                        if (this.canSupportAtFace(p_222656_, p_222657_, direction)) {
                            p_222656_.setBlock(p_222657_, p_222655_.setValue(UP, true), 2);
                            return;
                        }

                        if (p_222656_.isEmptyBlock(blockpos)) {
                            if (!this.canSpread(p_222656_, p_222657_)) {
                                return;
                            }

                            BlockState blockstate3 = p_222655_;

                            for (Direction direction2 : Direction.Plane.HORIZONTAL) {
                                if (p_222658_.nextBoolean() || !isAcceptableNeighbour(p_222656_, blockpos.relative(direction2), direction2)) {
                                    blockstate3 = blockstate3.setValue(getPropertyForFace(direction2), false);
                                }
                            }

                            if (this.hasHorizontalConnection(blockstate3)) {
                                p_222656_.setBlock(blockpos, blockstate3, 2);
                            }

                            return;
                        }
                    }

                    if (p_222657_.getY() > p_222656_.getMinY()) {
                        BlockPos blockpos1 = p_222657_.below();
                        BlockState blockstate = p_222656_.getBlockState(blockpos1);
                        if (blockstate.isAir() || blockstate.is(this)) {
                            BlockState blockstate1 = blockstate.isAir() ? this.defaultBlockState() : blockstate;
                            BlockState blockstate2 = this.copyRandomFaces(p_222655_, blockstate1, p_222658_);
                            if (blockstate1 != blockstate2 && this.hasHorizontalConnection(blockstate2)) {
                                p_222656_.setBlock(blockpos1, blockstate2, 2);
                            }
                        }
                    }
                }
            }
        }
    }

    private BlockState copyRandomFaces(BlockState pSourceState, BlockState pSpreadState, RandomSource pRandom) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (pRandom.nextBoolean()) {
                BooleanProperty booleanproperty = getPropertyForFace(direction);
                if (pSourceState.getValue(booleanproperty)) {
                    pSpreadState = pSpreadState.setValue(booleanproperty, true);
                }
            }
        }

        return pSpreadState;
    }

    private boolean hasHorizontalConnection(BlockState pState) {
        return pState.getValue(NORTH) || pState.getValue(EAST) || pState.getValue(SOUTH) || pState.getValue(WEST);
    }

    private boolean canSpread(BlockGetter pBlockReader, BlockPos pPos) {
        int i = 4;
        Iterable<BlockPos> iterable = BlockPos.betweenClosed(
            pPos.getX() - 4,
            pPos.getY() - 1,
            pPos.getZ() - 4,
            pPos.getX() + 4,
            pPos.getY() + 1,
            pPos.getZ() + 4
        );
        int j = 5;

        for (BlockPos blockpos : iterable) {
            if (pBlockReader.getBlockState(blockpos).is(this)) {
                if (--j <= 0) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    protected boolean canBeReplaced(BlockState pState, BlockPlaceContext pUseContext) {
        BlockState blockstate = pUseContext.getLevel().getBlockState(pUseContext.getClickedPos());
        return blockstate.is(this) ? this.countFaces(blockstate) < PROPERTY_BY_DIRECTION.size() : super.canBeReplaced(pState, pUseContext);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        BlockState blockstate = pContext.getLevel().getBlockState(pContext.getClickedPos());
        boolean flag = blockstate.is(this);
        BlockState blockstate1 = flag ? blockstate : this.defaultBlockState();

        for (Direction direction : pContext.getNearestLookingDirections()) {
            if (direction != Direction.DOWN) {
                BooleanProperty booleanproperty = getPropertyForFace(direction);
                boolean flag1 = flag && blockstate.getValue(booleanproperty);
                if (!flag1 && this.canSupportAtFace(pContext.getLevel(), pContext.getClickedPos(), direction)) {
                    return blockstate1.setValue(booleanproperty, true);
                }
            }
        }

        return flag ? blockstate1 : null;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(UP, NORTH, EAST, SOUTH, WEST);
    }

    @Override
    protected BlockState rotate(BlockState pState, Rotation pRotate) {
        switch (pRotate) {
            case CLOCKWISE_180:
                return pState.setValue(NORTH, pState.getValue(SOUTH))
                    .setValue(EAST, pState.getValue(WEST))
                    .setValue(SOUTH, pState.getValue(NORTH))
                    .setValue(WEST, pState.getValue(EAST));
            case COUNTERCLOCKWISE_90:
                return pState.setValue(NORTH, pState.getValue(EAST))
                    .setValue(EAST, pState.getValue(SOUTH))
                    .setValue(SOUTH, pState.getValue(WEST))
                    .setValue(WEST, pState.getValue(NORTH));
            case CLOCKWISE_90:
                return pState.setValue(NORTH, pState.getValue(WEST))
                    .setValue(EAST, pState.getValue(NORTH))
                    .setValue(SOUTH, pState.getValue(EAST))
                    .setValue(WEST, pState.getValue(SOUTH));
            default:
                return pState;
        }
    }

    @Override
    protected BlockState mirror(BlockState pState, Mirror pMirror) {
        switch (pMirror) {
            case LEFT_RIGHT:
                return pState.setValue(NORTH, pState.getValue(SOUTH)).setValue(SOUTH, pState.getValue(NORTH));
            case FRONT_BACK:
                return pState.setValue(EAST, pState.getValue(WEST)).setValue(WEST, pState.getValue(EAST));
            default:
                return super.mirror(pState, pMirror);
        }
    }

    public static BooleanProperty getPropertyForFace(Direction pFace) {
        return PROPERTY_BY_DIRECTION.get(pFace);
    }
}
