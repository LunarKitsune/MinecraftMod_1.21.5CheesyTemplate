package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class MultifaceBlock extends Block implements SimpleWaterloggedBlock {
    public static final MapCodec<MultifaceBlock> CODEC = simpleCodec(MultifaceBlock::new);
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = PipeBlock.PROPERTY_BY_DIRECTION;
    protected static final Direction[] DIRECTIONS = Direction.values();
    private final Function<BlockState, VoxelShape> shapes;
    private final boolean canRotate;
    private final boolean canMirrorX;
    private final boolean canMirrorZ;

    @Override
    protected MapCodec<? extends MultifaceBlock> codec() {
        return CODEC;
    }

    public MultifaceBlock(BlockBehaviour.Properties p_153822_) {
        super(p_153822_);
        this.registerDefaultState(getDefaultMultifaceState(this.stateDefinition));
        this.shapes = this.makeShapes();
        this.canRotate = Direction.Plane.HORIZONTAL.stream().allMatch(this::isFaceSupported);
        this.canMirrorX = Direction.Plane.HORIZONTAL.stream().filter(Direction.Axis.X).filter(this::isFaceSupported).count() % 2L == 0L;
        this.canMirrorZ = Direction.Plane.HORIZONTAL.stream().filter(Direction.Axis.Z).filter(this::isFaceSupported).count() % 2L == 0L;
    }

    private Function<BlockState, VoxelShape> makeShapes() {
        Map<Direction, VoxelShape> map = Shapes.rotateAll(Block.boxZ(16.0, 0.0, 1.0));
        return this.getShapeForEachState(p_390946_ -> {
            VoxelShape voxelshape = Shapes.empty();

            for (Direction direction : DIRECTIONS) {
                if (hasFace(p_390946_, direction)) {
                    voxelshape = Shapes.or(voxelshape, map.get(direction));
                }
            }

            return voxelshape.isEmpty() ? Shapes.block() : voxelshape;
        }, new Property[]{WATERLOGGED});
    }

    public static Set<Direction> availableFaces(BlockState pState) {
        if (!(pState.getBlock() instanceof MultifaceBlock)) {
            return Set.of();
        } else {
            Set<Direction> set = EnumSet.noneOf(Direction.class);

            for (Direction direction : Direction.values()) {
                if (hasFace(pState, direction)) {
                    set.add(direction);
                }
            }

            return set;
        }
    }

    public static Set<Direction> unpack(byte pPackedDirections) {
        Set<Direction> set = EnumSet.noneOf(Direction.class);

        for (Direction direction : Direction.values()) {
            if ((pPackedDirections & (byte)(1 << direction.ordinal())) > 0) {
                set.add(direction);
            }
        }

        return set;
    }

    public static byte pack(Collection<Direction> pDirections) {
        byte b0 = 0;

        for (Direction direction : pDirections) {
            b0 = (byte)(b0 | 1 << direction.ordinal());
        }

        return b0;
    }

    protected boolean isFaceSupported(Direction pFace) {
        return true;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> p_153917_) {
        for (Direction direction : DIRECTIONS) {
            if (this.isFaceSupported(direction)) {
                p_153917_.add(getFaceProperty(direction));
            }
        }

        p_153917_.add(WATERLOGGED);
    }

    @Override
    protected BlockState updateShape(
        BlockState p_153904_,
        LevelReader p_367619_,
        ScheduledTickAccess p_360930_,
        BlockPos p_153908_,
        Direction p_153905_,
        BlockPos p_153909_,
        BlockState p_153906_,
        RandomSource p_369670_
    ) {
        if (p_153904_.getValue(WATERLOGGED)) {
            p_360930_.scheduleTick(p_153908_, Fluids.WATER, Fluids.WATER.getTickDelay(p_367619_));
        }

        if (!hasAnyFace(p_153904_)) {
            return Blocks.AIR.defaultBlockState();
        } else {
            return hasFace(p_153904_, p_153905_) && !canAttachTo(p_367619_, p_153905_, p_153909_, p_153906_)
                ? removeFace(p_153904_, getFaceProperty(p_153905_))
                : p_153904_;
        }
    }

    @Override
    protected FluidState getFluidState(BlockState p_378072_) {
        return p_378072_.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(p_378072_);
    }

    @Override
    protected VoxelShape getShape(BlockState p_153851_, BlockGetter p_153852_, BlockPos p_153853_, CollisionContext p_153854_) {
        return this.shapes.apply(p_153851_);
    }

    @Override
    protected boolean canSurvive(BlockState p_153888_, LevelReader p_153889_, BlockPos p_153890_) {
        boolean flag = false;

        for (Direction direction : DIRECTIONS) {
            if (hasFace(p_153888_, direction)) {
                if (!canAttachTo(p_153889_, p_153890_, direction)) {
                    return false;
                }

                flag = true;
            }
        }

        return flag;
    }

    @Override
    protected boolean canBeReplaced(BlockState p_153848_, BlockPlaceContext p_153849_) {
        return !p_153849_.getItemInHand().is(this.asItem()) || hasAnyVacantFace(p_153848_);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext p_153824_) {
        Level level = p_153824_.getLevel();
        BlockPos blockpos = p_153824_.getClickedPos();
        BlockState blockstate = level.getBlockState(blockpos);
        return Arrays.stream(p_153824_.getNearestLookingDirections())
            .map(p_153865_ -> this.getStateForPlacement(blockstate, level, blockpos, p_153865_))
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
    }

    public boolean isValidStateForPlacement(BlockGetter pLevel, BlockState pState, BlockPos pPos, Direction pDirection) {
        if (this.isFaceSupported(pDirection) && (!pState.is(this) || !hasFace(pState, pDirection))) {
            BlockPos blockpos = pPos.relative(pDirection);
            return canAttachTo(pLevel, pDirection, blockpos, pLevel.getBlockState(blockpos));
        } else {
            return false;
        }
    }

    @Nullable
    public BlockState getStateForPlacement(BlockState pCurrentState, BlockGetter pLevel, BlockPos pPos, Direction pLookingDirection) {
        if (!this.isValidStateForPlacement(pLevel, pCurrentState, pPos, pLookingDirection)) {
            return null;
        } else {
            BlockState blockstate;
            if (pCurrentState.is(this)) {
                blockstate = pCurrentState;
            } else if (pCurrentState.getFluidState().isSourceOfType(Fluids.WATER)) {
                blockstate = this.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true);
            } else {
                blockstate = this.defaultBlockState();
            }

            return blockstate.setValue(getFaceProperty(pLookingDirection), true);
        }
    }

    @Override
    protected BlockState rotate(BlockState p_153895_, Rotation p_153896_) {
        return !this.canRotate ? p_153895_ : this.mapDirections(p_153895_, p_153896_::rotate);
    }

    @Override
    protected BlockState mirror(BlockState p_153892_, Mirror p_153893_) {
        if (p_153893_ == Mirror.FRONT_BACK && !this.canMirrorX) {
            return p_153892_;
        } else {
            return p_153893_ == Mirror.LEFT_RIGHT && !this.canMirrorZ ? p_153892_ : this.mapDirections(p_153892_, p_153893_::mirror);
        }
    }

    private BlockState mapDirections(BlockState pState, Function<Direction, Direction> pDirectionalFunction) {
        BlockState blockstate = pState;

        for (Direction direction : DIRECTIONS) {
            if (this.isFaceSupported(direction)) {
                blockstate = blockstate.setValue(getFaceProperty(pDirectionalFunction.apply(direction)), pState.getValue(getFaceProperty(direction)));
            }
        }

        return blockstate;
    }

    public static boolean hasFace(BlockState pState, Direction pDirection) {
        BooleanProperty booleanproperty = getFaceProperty(pDirection);
        return pState.getValueOrElse(booleanproperty, false);
    }

    public static boolean canAttachTo(BlockGetter pLevel, BlockPos pPos, Direction pDirection) {
        BlockPos blockpos = pPos.relative(pDirection);
        BlockState blockstate = pLevel.getBlockState(blockpos);
        return canAttachTo(pLevel, pDirection, blockpos, blockstate);
    }

    public static boolean canAttachTo(BlockGetter pLevel, Direction pDirection, BlockPos pPos, BlockState pState) {
        return Block.isFaceFull(pState.getBlockSupportShape(pLevel, pPos), pDirection.getOpposite())
            || Block.isFaceFull(pState.getCollisionShape(pLevel, pPos), pDirection.getOpposite());
    }

    private static BlockState removeFace(BlockState pState, BooleanProperty pFaceProp) {
        BlockState blockstate = pState.setValue(pFaceProp, false);
        return hasAnyFace(blockstate) ? blockstate : Blocks.AIR.defaultBlockState();
    }

    public static BooleanProperty getFaceProperty(Direction pDirection) {
        return PROPERTY_BY_DIRECTION.get(pDirection);
    }

    private static BlockState getDefaultMultifaceState(StateDefinition<Block, BlockState> pStateDefinition) {
        BlockState blockstate = pStateDefinition.any().setValue(WATERLOGGED, false);

        for (BooleanProperty booleanproperty : PROPERTY_BY_DIRECTION.values()) {
            blockstate = blockstate.trySetValue(booleanproperty, false);
        }

        return blockstate;
    }

    protected static boolean hasAnyFace(BlockState pState) {
        for (Direction direction : DIRECTIONS) {
            if (hasFace(pState, direction)) {
                return true;
            }
        }

        return false;
    }

    private static boolean hasAnyVacantFace(BlockState pState) {
        for (Direction direction : DIRECTIONS) {
            if (!hasFace(pState, direction)) {
                return true;
            }
        }

        return false;
    }
}