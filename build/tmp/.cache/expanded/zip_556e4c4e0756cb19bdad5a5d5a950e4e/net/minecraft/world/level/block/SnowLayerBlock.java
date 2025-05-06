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
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SnowLayerBlock extends Block {
    public static final MapCodec<SnowLayerBlock> CODEC = simpleCodec(SnowLayerBlock::new);
    public static final int MAX_HEIGHT = 8;
    public static final IntegerProperty LAYERS = BlockStateProperties.LAYERS;
    private static final VoxelShape[] SHAPES = Block.boxes(8, p_393422_ -> Block.column(16.0, 0.0, p_393422_ * 2));
    public static final int HEIGHT_IMPASSABLE = 5;

    @Override
    public MapCodec<SnowLayerBlock> codec() {
        return CODEC;
    }

    public SnowLayerBlock(BlockBehaviour.Properties p_56585_) {
        super(p_56585_);
        this.registerDefaultState(this.stateDefinition.any().setValue(LAYERS, 1));
    }

    @Override
    protected boolean isPathfindable(BlockState p_56592_, PathComputationType p_56595_) {
        return p_56595_ == PathComputationType.LAND ? p_56592_.getValue(LAYERS) < 5 : false;
    }

    @Override
    protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return SHAPES[pState.getValue(LAYERS)];
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return SHAPES[pState.getValue(LAYERS) - 1];
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState pState, BlockGetter pReader, BlockPos pPos) {
        return SHAPES[pState.getValue(LAYERS)];
    }

    @Override
    protected VoxelShape getVisualShape(BlockState pState, BlockGetter pReader, BlockPos pPos, CollisionContext pContext) {
        return SHAPES[pState.getValue(LAYERS)];
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState pState) {
        return true;
    }

    @Override
    protected float getShadeBrightness(BlockState p_222453_, BlockGetter p_222454_, BlockPos p_222455_) {
        return p_222453_.getValue(LAYERS) == 8 ? 0.2F : 1.0F;
    }

    @Override
    protected boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
        BlockState blockstate = pLevel.getBlockState(pPos.below());
        if (blockstate.is(BlockTags.SNOW_LAYER_CANNOT_SURVIVE_ON)) {
            return false;
        } else {
            return blockstate.is(BlockTags.SNOW_LAYER_CAN_SURVIVE_ON)
                ? true
                : Block.isFaceFull(blockstate.getCollisionShape(pLevel, pPos.below()), Direction.UP)
                    || blockstate.is(this) && blockstate.getValue(LAYERS) == 8;
        }
    }

    @Override
    protected BlockState updateShape(
        BlockState p_56606_,
        LevelReader p_360741_,
        ScheduledTickAccess p_366735_,
        BlockPos p_56610_,
        Direction p_56607_,
        BlockPos p_56611_,
        BlockState p_56608_,
        RandomSource p_361947_
    ) {
        return !p_56606_.canSurvive(p_360741_, p_56610_)
            ? Blocks.AIR.defaultBlockState()
            : super.updateShape(p_56606_, p_360741_, p_366735_, p_56610_, p_56607_, p_56611_, p_56608_, p_361947_);
    }

    @Override
    protected void randomTick(BlockState p_222448_, ServerLevel p_222449_, BlockPos p_222450_, RandomSource p_222451_) {
        if (p_222449_.getBrightness(LightLayer.BLOCK, p_222450_) > 11) {
            dropResources(p_222448_, p_222449_, p_222450_);
            p_222449_.removeBlock(p_222450_, false);
        }
    }

    @Override
    protected boolean canBeReplaced(BlockState pState, BlockPlaceContext pUseContext) {
        int i = pState.getValue(LAYERS);
        if (!pUseContext.getItemInHand().is(this.asItem()) || i >= 8) {
            return i == 1;
        } else {
            return pUseContext.replacingClickedOnBlock() ? pUseContext.getClickedFace() == Direction.UP : true;
        }
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        BlockState blockstate = pContext.getLevel().getBlockState(pContext.getClickedPos());
        if (blockstate.is(this)) {
            int i = blockstate.getValue(LAYERS);
            return blockstate.setValue(LAYERS, Math.min(8, i + 1));
        } else {
            return super.getStateForPlacement(pContext);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(LAYERS);
    }
}