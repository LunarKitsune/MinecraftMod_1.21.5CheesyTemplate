package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.MapCodec;
import java.util.Map;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class HopperBlock extends BaseEntityBlock {
    public static final MapCodec<HopperBlock> CODEC = simpleCodec(HopperBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING_HOPPER;
    public static final BooleanProperty ENABLED = BlockStateProperties.ENABLED;
    private final Function<BlockState, VoxelShape> shapes;
    private final Map<Direction, VoxelShape> interactionShapes;

    @Override
    public MapCodec<HopperBlock> codec() {
        return CODEC;
    }

    public HopperBlock(BlockBehaviour.Properties p_54039_) {
        super(p_54039_);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.DOWN).setValue(ENABLED, true));
        VoxelShape voxelshape = Block.column(12.0, 11.0, 16.0);
        this.shapes = this.makeShapes(voxelshape);
        this.interactionShapes = ImmutableMap.<Direction, VoxelShape>builderWithExpectedSize(5)
            .putAll(Shapes.rotateHorizontal(Shapes.or(voxelshape, Block.boxZ(4.0, 8.0, 10.0, 0.0, 4.0))))
            .put(Direction.DOWN, voxelshape)
            .build();
    }

    private Function<BlockState, VoxelShape> makeShapes(VoxelShape pShape) {
        VoxelShape voxelshape = Shapes.or(Block.column(16.0, 10.0, 16.0), Block.column(8.0, 4.0, 10.0));
        VoxelShape voxelshape1 = Shapes.join(voxelshape, pShape, BooleanOp.ONLY_FIRST);
        Map<Direction, VoxelShape> map = Shapes.rotateAll(Block.boxZ(4.0, 4.0, 8.0, 0.0, 8.0), new Vec3(8.0, 6.0, 8.0).scale(0.0625));
        return this.getShapeForEachState(
            p_394823_ -> Shapes.or(voxelshape1, Shapes.join(map.get(p_394823_.getValue(FACING)), Shapes.block(), BooleanOp.AND)),
            new Property[]{ENABLED}
        );
    }

    @Override
    protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return this.shapes.apply(pState);
    }

    @Override
    protected VoxelShape getInteractionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
        return this.interactionShapes.get(pState.getValue(FACING));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        Direction direction = pContext.getClickedFace().getOpposite();
        return this.defaultBlockState().setValue(FACING, direction.getAxis() == Direction.Axis.Y ? Direction.DOWN : direction).setValue(ENABLED, true);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos p_153382_, BlockState p_153383_) {
        return new HopperBlockEntity(p_153382_, p_153383_);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level p_153378_, BlockState p_153379_, BlockEntityType<T> p_153380_) {
        return p_153378_.isClientSide ? null : createTickerHelper(p_153380_, BlockEntityType.HOPPER, HopperBlockEntity::pushItemsTick);
    }

    @Override
    protected void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
        if (!pOldState.is(pState.getBlock())) {
            this.checkPoweredState(pLevel, pPos, pState);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState p_54071_, Level p_54072_, BlockPos p_54073_, Player p_54074_, BlockHitResult p_54076_) {
        if (!p_54072_.isClientSide && p_54072_.getBlockEntity(p_54073_) instanceof HopperBlockEntity hopperblockentity) {
            p_54074_.openMenu(hopperblockentity);
            p_54074_.awardStat(Stats.INSPECT_HOPPER);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    protected void neighborChanged(BlockState p_54078_, Level p_54079_, BlockPos p_54080_, Block p_54081_, @Nullable Orientation p_364751_, boolean p_54083_) {
        this.checkPoweredState(p_54079_, p_54080_, p_54078_);
    }

    private void checkPoweredState(Level pLevel, BlockPos pPos, BlockState pState) {
        boolean flag = !pLevel.hasNeighborSignal(pPos);
        if (flag != pState.getValue(ENABLED)) {
            pLevel.setBlock(pPos, pState.setValue(ENABLED, flag), 2);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState p_394577_, ServerLevel p_395407_, BlockPos p_397570_, boolean p_396660_) {
        Containers.updateNeighboursAfterDestroy(p_394577_, p_395407_, p_397570_);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState pState) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState pBlockState, Level pLevel, BlockPos pPos) {
        return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(pLevel.getBlockEntity(pPos));
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
        pBuilder.add(FACING, ENABLED);
    }

    @Override
    protected void entityInside(BlockState p_54066_, Level p_54067_, BlockPos p_54068_, Entity p_54069_, InsideBlockEffectApplier p_397073_) {
        BlockEntity blockentity = p_54067_.getBlockEntity(p_54068_);
        if (blockentity instanceof HopperBlockEntity) {
            HopperBlockEntity.entityInside(p_54067_, p_54068_, p_54066_, p_54069_, (HopperBlockEntity)blockentity);
        }
    }

    @Override
    protected boolean isPathfindable(BlockState p_54057_, PathComputationType p_54060_) {
        return false;
    }
}