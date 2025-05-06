package net.minecraft.world.level.block;

import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public interface SegmentableBlock {
    int MIN_SEGMENT = 1;
    int MAX_SEGMENT = 4;
    IntegerProperty AMOUNT = BlockStateProperties.SEGMENT_AMOUNT;

    default Function<BlockState, VoxelShape> getShapeCalculator(EnumProperty<Direction> pDirectionProperty, IntegerProperty pAmountProperty) {
        Map<Direction, VoxelShape> map = Shapes.rotateHorizontal(Block.box(0.0, 0.0, 0.0, 8.0, this.getShapeHeight(), 8.0));
        return p_392138_ -> {
            VoxelShape voxelshape = Shapes.empty();
            Direction direction = p_392138_.getValue(pDirectionProperty);
            int i = p_392138_.getValue(pAmountProperty);

            for (int j = 0; j < i; j++) {
                voxelshape = Shapes.or(voxelshape, map.get(direction));
                direction = direction.getCounterClockWise();
            }

            return voxelshape.singleEncompassing();
        };
    }

    default IntegerProperty getSegmentAmountProperty() {
        return AMOUNT;
    }

    default double getShapeHeight() {
        return 1.0;
    }

    default boolean canBeReplaced(BlockState pState, BlockPlaceContext pContext, IntegerProperty pAmountProperty) {
        return !pContext.isSecondaryUseActive() && pContext.getItemInHand().is(pState.getBlock().asItem()) && pState.getValue(pAmountProperty) < 4;
    }

    default BlockState getStateForPlacement(BlockPlaceContext pContext, Block pBlock, IntegerProperty pAmountProperty, EnumProperty<Direction> pDirectionProperty) {
        BlockState blockstate = pContext.getLevel().getBlockState(pContext.getClickedPos());
        return blockstate.is(pBlock)
            ? blockstate.setValue(pAmountProperty, Math.min(4, blockstate.getValue(pAmountProperty) + 1))
            : pBlock.defaultBlockState().setValue(pDirectionProperty, pContext.getHorizontalDirection().getOpposite());
    }
}