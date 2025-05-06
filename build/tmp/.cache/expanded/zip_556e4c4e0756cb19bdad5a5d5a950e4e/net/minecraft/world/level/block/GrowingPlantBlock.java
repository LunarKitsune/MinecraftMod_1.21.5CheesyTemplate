package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class GrowingPlantBlock extends Block {
    protected final Direction growthDirection;
    protected final boolean scheduleFluidTicks;
    protected final VoxelShape shape;

    protected GrowingPlantBlock(BlockBehaviour.Properties pProperties, Direction pGrowthDirection, VoxelShape pShape, boolean pScheduleFluidTicks) {
        super(pProperties);
        this.growthDirection = pGrowthDirection;
        this.shape = pShape;
        this.scheduleFluidTicks = pScheduleFluidTicks;
    }

    @Override
    protected abstract MapCodec<? extends GrowingPlantBlock> codec();

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        BlockState blockstate = pContext.getLevel().getBlockState(pContext.getClickedPos().relative(this.growthDirection));
        return !blockstate.is(this.getHeadBlock()) && !blockstate.is(this.getBodyBlock())
            ? this.getStateForPlacement(pContext.getLevel().random)
            : this.getBodyBlock().defaultBlockState();
    }

    public BlockState getStateForPlacement(RandomSource pRandom) {
        return this.defaultBlockState();
    }

    @Override
    protected boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
        BlockPos blockpos = pPos.relative(this.growthDirection.getOpposite());
        BlockState blockstate = pLevel.getBlockState(blockpos);
        return !this.canAttachTo(blockstate)
            ? false
            : blockstate.is(this.getHeadBlock()) || blockstate.is(this.getBodyBlock()) || blockstate.isFaceSturdy(pLevel, blockpos, this.growthDirection);
    }

    @Override
    protected void tick(BlockState p_221280_, ServerLevel p_221281_, BlockPos p_221282_, RandomSource p_221283_) {
        if (!p_221280_.canSurvive(p_221281_, p_221282_)) {
            p_221281_.destroyBlock(p_221282_, true);
        }
    }

    protected boolean canAttachTo(BlockState pState) {
        return true;
    }

    @Override
    protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return this.shape;
    }

    protected abstract GrowingPlantHeadBlock getHeadBlock();

    protected abstract Block getBodyBlock();
}