package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class HugeMushroomBlock extends Block {
    public static final MapCodec<HugeMushroomBlock> CODEC = simpleCodec(HugeMushroomBlock::new);
    public static final BooleanProperty NORTH = PipeBlock.NORTH;
    public static final BooleanProperty EAST = PipeBlock.EAST;
    public static final BooleanProperty SOUTH = PipeBlock.SOUTH;
    public static final BooleanProperty WEST = PipeBlock.WEST;
    public static final BooleanProperty UP = PipeBlock.UP;
    public static final BooleanProperty DOWN = PipeBlock.DOWN;
    private static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = PipeBlock.PROPERTY_BY_DIRECTION;

    @Override
    public MapCodec<HugeMushroomBlock> codec() {
        return CODEC;
    }

    public HugeMushroomBlock(BlockBehaviour.Properties p_54136_) {
        super(p_54136_);
        this.registerDefaultState(
            this.stateDefinition
                .any()
                .setValue(NORTH, true)
                .setValue(EAST, true)
                .setValue(SOUTH, true)
                .setValue(WEST, true)
                .setValue(UP, true)
                .setValue(DOWN, true)
        );
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        BlockGetter blockgetter = pContext.getLevel();
        BlockPos blockpos = pContext.getClickedPos();
        return this.defaultBlockState()
            .setValue(DOWN, !blockgetter.getBlockState(blockpos.below()).is(this))
            .setValue(UP, !blockgetter.getBlockState(blockpos.above()).is(this))
            .setValue(NORTH, !blockgetter.getBlockState(blockpos.north()).is(this))
            .setValue(EAST, !blockgetter.getBlockState(blockpos.east()).is(this))
            .setValue(SOUTH, !blockgetter.getBlockState(blockpos.south()).is(this))
            .setValue(WEST, !blockgetter.getBlockState(blockpos.west()).is(this));
    }

    @Override
    protected BlockState updateShape(
        BlockState p_54146_,
        LevelReader p_369980_,
        ScheduledTickAccess p_367294_,
        BlockPos p_54150_,
        Direction p_54147_,
        BlockPos p_54151_,
        BlockState p_54148_,
        RandomSource p_362691_
    ) {
        return p_54148_.is(this)
            ? p_54146_.setValue(PROPERTY_BY_DIRECTION.get(p_54147_), false)
            : super.updateShape(p_54146_, p_369980_, p_367294_, p_54150_, p_54147_, p_54151_, p_54148_, p_362691_);
    }

    @Override
    protected BlockState rotate(BlockState pState, Rotation pRot) {
        return pState.setValue(PROPERTY_BY_DIRECTION.get(pRot.rotate(Direction.NORTH)), pState.getValue(NORTH))
            .setValue(PROPERTY_BY_DIRECTION.get(pRot.rotate(Direction.SOUTH)), pState.getValue(SOUTH))
            .setValue(PROPERTY_BY_DIRECTION.get(pRot.rotate(Direction.EAST)), pState.getValue(EAST))
            .setValue(PROPERTY_BY_DIRECTION.get(pRot.rotate(Direction.WEST)), pState.getValue(WEST))
            .setValue(PROPERTY_BY_DIRECTION.get(pRot.rotate(Direction.UP)), pState.getValue(UP))
            .setValue(PROPERTY_BY_DIRECTION.get(pRot.rotate(Direction.DOWN)), pState.getValue(DOWN));
    }

    @Override
    protected BlockState mirror(BlockState pState, Mirror pMirror) {
        return pState.setValue(PROPERTY_BY_DIRECTION.get(pMirror.mirror(Direction.NORTH)), pState.getValue(NORTH))
            .setValue(PROPERTY_BY_DIRECTION.get(pMirror.mirror(Direction.SOUTH)), pState.getValue(SOUTH))
            .setValue(PROPERTY_BY_DIRECTION.get(pMirror.mirror(Direction.EAST)), pState.getValue(EAST))
            .setValue(PROPERTY_BY_DIRECTION.get(pMirror.mirror(Direction.WEST)), pState.getValue(WEST))
            .setValue(PROPERTY_BY_DIRECTION.get(pMirror.mirror(Direction.UP)), pState.getValue(UP))
            .setValue(PROPERTY_BY_DIRECTION.get(pMirror.mirror(Direction.DOWN)), pState.getValue(DOWN));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(UP, DOWN, NORTH, EAST, SOUTH, WEST);
    }
}