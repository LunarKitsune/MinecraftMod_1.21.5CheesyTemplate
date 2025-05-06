package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.serialization.MapCodec;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.WallSide;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class WallBlock extends Block implements SimpleWaterloggedBlock {
    public static final MapCodec<WallBlock> CODEC = simpleCodec(WallBlock::new);
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final EnumProperty<WallSide> EAST = BlockStateProperties.EAST_WALL;
    public static final EnumProperty<WallSide> NORTH = BlockStateProperties.NORTH_WALL;
    public static final EnumProperty<WallSide> SOUTH = BlockStateProperties.SOUTH_WALL;
    public static final EnumProperty<WallSide> WEST = BlockStateProperties.WEST_WALL;
    public static final Map<Direction, EnumProperty<WallSide>> PROPERTY_BY_DIRECTION = ImmutableMap.copyOf(
        Maps.newEnumMap(Map.of(Direction.NORTH, NORTH, Direction.EAST, EAST, Direction.SOUTH, SOUTH, Direction.WEST, WEST))
    );
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private final Function<BlockState, VoxelShape> shapes;
    private final Function<BlockState, VoxelShape> collisionShapes;
    private static final VoxelShape TEST_SHAPE_POST = Block.column(2.0, 0.0, 16.0);
    private static final Map<Direction, VoxelShape> TEST_SHAPES_WALL = Shapes.rotateHorizontal(Block.boxZ(2.0, 16.0, 0.0, 9.0));

    @Override
    public MapCodec<WallBlock> codec() {
        return CODEC;
    }

    public WallBlock(BlockBehaviour.Properties p_57964_) {
        super(p_57964_);
        this.registerDefaultState(
            this.stateDefinition
                .any()
                .setValue(UP, true)
                .setValue(NORTH, WallSide.NONE)
                .setValue(EAST, WallSide.NONE)
                .setValue(SOUTH, WallSide.NONE)
                .setValue(WEST, WallSide.NONE)
                .setValue(WATERLOGGED, false)
        );
        this.shapes = this.makeShapes(16.0F, 14.0F);
        this.collisionShapes = this.makeShapes(24.0F, 24.0F);
    }

    private Function<BlockState, VoxelShape> makeShapes(float pHeight, float pWidth) {
        VoxelShape voxelshape = Block.column(8.0, 0.0, pHeight);
        int i = 6;
        Map<Direction, VoxelShape> map = Shapes.rotateHorizontal(Block.boxZ(6.0, 0.0, pWidth, 0.0, 11.0));
        Map<Direction, VoxelShape> map1 = Shapes.rotateHorizontal(Block.boxZ(6.0, 0.0, pHeight, 0.0, 11.0));
        return this.getShapeForEachState(p_394482_ -> {
            VoxelShape voxelshape1 = p_394482_.getValue(UP) ? voxelshape : Shapes.empty();

            for (Entry<Direction, EnumProperty<WallSide>> entry : PROPERTY_BY_DIRECTION.entrySet()) {
                voxelshape1 = Shapes.or(voxelshape1, switch ((WallSide)p_394482_.getValue(entry.getValue())) {
                    case NONE -> Shapes.empty();
                    case LOW -> (VoxelShape)map.get(entry.getKey());
                    case TALL -> (VoxelShape)map1.get(entry.getKey());
                });
            }

            return voxelshape1;
        }, new Property[]{WATERLOGGED});
    }

    @Override
    protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return this.shapes.apply(pState);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return this.collisionShapes.apply(pState);
    }

    @Override
    protected boolean isPathfindable(BlockState p_57996_, PathComputationType p_57999_) {
        return false;
    }

    private boolean connectsTo(BlockState pState, boolean pSideSolid, Direction pDirection) {
        Block block = pState.getBlock();
        boolean flag = block instanceof FenceGateBlock && FenceGateBlock.connectsToDirection(pState, pDirection);
        return pState.is(BlockTags.WALLS) || !isExceptionForConnection(pState) && pSideSolid || block instanceof IronBarsBlock || flag;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        LevelReader levelreader = pContext.getLevel();
        BlockPos blockpos = pContext.getClickedPos();
        FluidState fluidstate = pContext.getLevel().getFluidState(pContext.getClickedPos());
        BlockPos blockpos1 = blockpos.north();
        BlockPos blockpos2 = blockpos.east();
        BlockPos blockpos3 = blockpos.south();
        BlockPos blockpos4 = blockpos.west();
        BlockPos blockpos5 = blockpos.above();
        BlockState blockstate = levelreader.getBlockState(blockpos1);
        BlockState blockstate1 = levelreader.getBlockState(blockpos2);
        BlockState blockstate2 = levelreader.getBlockState(blockpos3);
        BlockState blockstate3 = levelreader.getBlockState(blockpos4);
        BlockState blockstate4 = levelreader.getBlockState(blockpos5);
        boolean flag = this.connectsTo(blockstate, blockstate.isFaceSturdy(levelreader, blockpos1, Direction.SOUTH), Direction.SOUTH);
        boolean flag1 = this.connectsTo(blockstate1, blockstate1.isFaceSturdy(levelreader, blockpos2, Direction.WEST), Direction.WEST);
        boolean flag2 = this.connectsTo(blockstate2, blockstate2.isFaceSturdy(levelreader, blockpos3, Direction.NORTH), Direction.NORTH);
        boolean flag3 = this.connectsTo(blockstate3, blockstate3.isFaceSturdy(levelreader, blockpos4, Direction.EAST), Direction.EAST);
        BlockState blockstate5 = this.defaultBlockState().setValue(WATERLOGGED, fluidstate.getType() == Fluids.WATER);
        return this.updateShape(levelreader, blockstate5, blockpos5, blockstate4, flag, flag1, flag2, flag3);
    }

    @Override
    protected BlockState updateShape(
        BlockState p_58014_,
        LevelReader p_363038_,
        ScheduledTickAccess p_368345_,
        BlockPos p_58018_,
        Direction p_58015_,
        BlockPos p_58019_,
        BlockState p_58016_,
        RandomSource p_364621_
    ) {
        if (p_58014_.getValue(WATERLOGGED)) {
            p_368345_.scheduleTick(p_58018_, Fluids.WATER, Fluids.WATER.getTickDelay(p_363038_));
        }

        if (p_58015_ == Direction.DOWN) {
            return super.updateShape(p_58014_, p_363038_, p_368345_, p_58018_, p_58015_, p_58019_, p_58016_, p_364621_);
        } else {
            return p_58015_ == Direction.UP
                ? this.topUpdate(p_363038_, p_58014_, p_58019_, p_58016_)
                : this.sideUpdate(p_363038_, p_58018_, p_58014_, p_58019_, p_58016_, p_58015_);
        }
    }

    private static boolean isConnected(BlockState pState, Property<WallSide> pHeightProperty) {
        return pState.getValue(pHeightProperty) != WallSide.NONE;
    }

    private static boolean isCovered(VoxelShape pFirstShape, VoxelShape pSecondShape) {
        return !Shapes.joinIsNotEmpty(pSecondShape, pFirstShape, BooleanOp.ONLY_FIRST);
    }

    private BlockState topUpdate(LevelReader pLevel, BlockState pState, BlockPos pPos, BlockState pSecondState) {
        boolean flag = isConnected(pState, NORTH);
        boolean flag1 = isConnected(pState, EAST);
        boolean flag2 = isConnected(pState, SOUTH);
        boolean flag3 = isConnected(pState, WEST);
        return this.updateShape(pLevel, pState, pPos, pSecondState, flag, flag1, flag2, flag3);
    }

    private BlockState sideUpdate(LevelReader pLevel, BlockPos pFirstPos, BlockState pFirstState, BlockPos pSecondPos, BlockState pSecondState, Direction pDir) {
        Direction direction = pDir.getOpposite();
        boolean flag = pDir == Direction.NORTH
            ? this.connectsTo(pSecondState, pSecondState.isFaceSturdy(pLevel, pSecondPos, direction), direction)
            : isConnected(pFirstState, NORTH);
        boolean flag1 = pDir == Direction.EAST
            ? this.connectsTo(pSecondState, pSecondState.isFaceSturdy(pLevel, pSecondPos, direction), direction)
            : isConnected(pFirstState, EAST);
        boolean flag2 = pDir == Direction.SOUTH
            ? this.connectsTo(pSecondState, pSecondState.isFaceSturdy(pLevel, pSecondPos, direction), direction)
            : isConnected(pFirstState, SOUTH);
        boolean flag3 = pDir == Direction.WEST
            ? this.connectsTo(pSecondState, pSecondState.isFaceSturdy(pLevel, pSecondPos, direction), direction)
            : isConnected(pFirstState, WEST);
        BlockPos blockpos = pFirstPos.above();
        BlockState blockstate = pLevel.getBlockState(blockpos);
        return this.updateShape(pLevel, pFirstState, blockpos, blockstate, flag, flag1, flag2, flag3);
    }

    private BlockState updateShape(
        LevelReader pLevel,
        BlockState pState,
        BlockPos pPos,
        BlockState pNeighbour,
        boolean pNorthConnection,
        boolean pEastConnection,
        boolean pSouthConnection,
        boolean pWestConnection
    ) {
        VoxelShape voxelshape = pNeighbour.getCollisionShape(pLevel, pPos).getFaceShape(Direction.DOWN);
        BlockState blockstate = this.updateSides(pState, pNorthConnection, pEastConnection, pSouthConnection, pWestConnection, voxelshape);
        return blockstate.setValue(UP, this.shouldRaisePost(blockstate, pNeighbour, voxelshape));
    }

    private boolean shouldRaisePost(BlockState pState, BlockState pNeighbour, VoxelShape pShape) {
        boolean flag = pNeighbour.getBlock() instanceof WallBlock && pNeighbour.getValue(UP);
        if (flag) {
            return true;
        } else {
            WallSide wallside = pState.getValue(NORTH);
            WallSide wallside1 = pState.getValue(SOUTH);
            WallSide wallside2 = pState.getValue(EAST);
            WallSide wallside3 = pState.getValue(WEST);
            boolean flag1 = wallside1 == WallSide.NONE;
            boolean flag2 = wallside3 == WallSide.NONE;
            boolean flag3 = wallside2 == WallSide.NONE;
            boolean flag4 = wallside == WallSide.NONE;
            boolean flag5 = flag4 && flag1 && flag2 && flag3 || flag4 != flag1 || flag2 != flag3;
            if (flag5) {
                return true;
            } else {
                boolean flag6 = wallside == WallSide.TALL && wallside1 == WallSide.TALL || wallside2 == WallSide.TALL && wallside3 == WallSide.TALL;
                return flag6 ? false : pNeighbour.is(BlockTags.WALL_POST_OVERRIDE) || isCovered(pShape, TEST_SHAPE_POST);
            }
        }
    }

    private BlockState updateSides(BlockState pState, boolean pNorthConnection, boolean pEastConnection, boolean pSouthConnection, boolean pWestConnection, VoxelShape pWallShape) {
        return pState.setValue(NORTH, this.makeWallState(pNorthConnection, pWallShape, TEST_SHAPES_WALL.get(Direction.NORTH)))
            .setValue(EAST, this.makeWallState(pEastConnection, pWallShape, TEST_SHAPES_WALL.get(Direction.EAST)))
            .setValue(SOUTH, this.makeWallState(pSouthConnection, pWallShape, TEST_SHAPES_WALL.get(Direction.SOUTH)))
            .setValue(WEST, this.makeWallState(pWestConnection, pWallShape, TEST_SHAPES_WALL.get(Direction.WEST)));
    }

    private WallSide makeWallState(boolean pAllowConnection, VoxelShape pShape, VoxelShape pNeighbourShape) {
        if (pAllowConnection) {
            return isCovered(pShape, pNeighbourShape) ? WallSide.TALL : WallSide.LOW;
        } else {
            return WallSide.NONE;
        }
    }

    @Override
    protected FluidState getFluidState(BlockState pState) {
        return pState.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(pState);
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState p_58046_) {
        return !p_58046_.getValue(WATERLOGGED);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(UP, NORTH, EAST, WEST, SOUTH, WATERLOGGED);
    }

    @Override
    protected BlockState rotate(BlockState pState, Rotation pRotation) {
        switch (pRotation) {
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
}