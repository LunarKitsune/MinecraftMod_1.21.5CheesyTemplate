package net.minecraft.world.level.redstone;

import com.google.common.collect.Sets;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.state.BlockState;

public class DefaultRedstoneWireEvaluator extends RedstoneWireEvaluator {
    public DefaultRedstoneWireEvaluator(RedStoneWireBlock p_369991_) {
        super(p_369991_);
    }

    @Override
    public void updatePowerStrength(Level p_362765_, BlockPos p_364703_, BlockState p_367621_, @Nullable Orientation p_363846_, boolean p_362932_) {
        int i = this.calculateTargetStrength(p_362765_, p_364703_);
        if (p_367621_.getValue(RedStoneWireBlock.POWER) != i) {
            if (p_362765_.getBlockState(p_364703_) == p_367621_) {
                p_362765_.setBlock(p_364703_, p_367621_.setValue(RedStoneWireBlock.POWER, i), 2);
            }

            Set<BlockPos> set = Sets.newHashSet();
            set.add(p_364703_);

            for (Direction direction : Direction.values()) {
                set.add(p_364703_.relative(direction));
            }

            for (BlockPos blockpos : set) {
                p_362765_.updateNeighborsAt(blockpos, this.wireBlock);
            }
        }
    }

    private int calculateTargetStrength(Level pLevel, BlockPos pPos) {
        int i = this.getBlockSignal(pLevel, pPos);
        return i == 15 ? i : Math.max(i, this.getIncomingWireSignal(pLevel, pPos));
    }
}