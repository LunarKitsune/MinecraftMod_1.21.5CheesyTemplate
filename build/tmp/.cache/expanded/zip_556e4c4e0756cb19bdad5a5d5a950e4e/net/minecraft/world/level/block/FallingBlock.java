package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.ParticleUtils;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public abstract class FallingBlock extends Block implements Fallable {
    public FallingBlock(BlockBehaviour.Properties p_53205_) {
        super(p_53205_);
    }

    @Override
    protected abstract MapCodec<? extends FallingBlock> codec();

    @Override
    protected void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
        pLevel.scheduleTick(pPos, this, this.getDelayAfterPlace());
    }

    @Override
    protected BlockState updateShape(
        BlockState p_53226_,
        LevelReader p_367091_,
        ScheduledTickAccess p_369081_,
        BlockPos p_53230_,
        Direction p_53227_,
        BlockPos p_53231_,
        BlockState p_53228_,
        RandomSource p_369211_
    ) {
        p_369081_.scheduleTick(p_53230_, this, this.getDelayAfterPlace());
        return super.updateShape(p_53226_, p_367091_, p_369081_, p_53230_, p_53227_, p_53231_, p_53228_, p_369211_);
    }

    @Override
    protected void tick(BlockState p_221124_, ServerLevel p_221125_, BlockPos p_221126_, RandomSource p_221127_) {
        if (isFree(p_221125_.getBlockState(p_221126_.below())) && p_221126_.getY() >= p_221125_.getMinY()) {
            FallingBlockEntity fallingblockentity = FallingBlockEntity.fall(p_221125_, p_221126_, p_221124_);
            this.falling(fallingblockentity);
        }
    }

    protected void falling(FallingBlockEntity pEntity) {
    }

    protected int getDelayAfterPlace() {
        return 2;
    }

    public static boolean isFree(BlockState pState) {
        return pState.isAir() || pState.is(BlockTags.FIRE) || pState.liquid() || pState.canBeReplaced();
    }

    @Override
    public void animateTick(BlockState p_221129_, Level p_221130_, BlockPos p_221131_, RandomSource p_221132_) {
        if (p_221132_.nextInt(16) == 0) {
            BlockPos blockpos = p_221131_.below();
            if (isFree(p_221130_.getBlockState(blockpos))) {
                ParticleUtils.spawnParticleBelow(p_221130_, p_221131_, p_221132_, new BlockParticleOption(ParticleTypes.FALLING_DUST, p_221129_));
            }
        }
    }

    public abstract int getDustColor(BlockState pState, BlockGetter pLevel, BlockPos pPos);
}