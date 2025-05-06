package net.minecraft.world.level.redstone;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;

public abstract class RedstoneWireEvaluator {
    protected final RedStoneWireBlock wireBlock;

    protected RedstoneWireEvaluator(RedStoneWireBlock pWireBlock) {
        this.wireBlock = pWireBlock;
    }

    public abstract void updatePowerStrength(Level pLevel, BlockPos pPos, BlockState pState, @Nullable Orientation pOrientation, boolean pUpdateShape);

    protected int getBlockSignal(Level pLevel, BlockPos pPos) {
        return this.wireBlock.getBlockSignal(pLevel, pPos);
    }

    protected int getWireSignal(BlockPos pPos, BlockState pState) {
        return pState.is(this.wireBlock) ? pState.getValue(RedStoneWireBlock.POWER) : 0;
    }

    protected int getIncomingWireSignal(Level pLevel, BlockPos pPos) {
        int i = 0;

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos blockpos = pPos.relative(direction);
            BlockState blockstate = pLevel.getBlockState(blockpos);
            i = Math.max(i, this.getWireSignal(blockpos, blockstate));
            BlockPos blockpos1 = pPos.above();
            if (blockstate.isRedstoneConductor(pLevel, blockpos) && !pLevel.getBlockState(blockpos1).isRedstoneConductor(pLevel, blockpos1)) {
                BlockPos blockpos3 = blockpos.above();
                i = Math.max(i, this.getWireSignal(blockpos3, pLevel.getBlockState(blockpos3)));
            } else if (!blockstate.isRedstoneConductor(pLevel, blockpos)) {
                BlockPos blockpos2 = blockpos.below();
                i = Math.max(i, this.getWireSignal(blockpos2, pLevel.getBlockState(blockpos2)));
            }
        }

        return Math.max(0, i - 1);
    }
}