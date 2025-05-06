package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;

public class SculkBlock extends DropExperienceBlock implements SculkBehaviour {
    public static final MapCodec<SculkBlock> CODEC = simpleCodec(SculkBlock::new);

    @Override
    public MapCodec<SculkBlock> codec() {
        return CODEC;
    }

    public SculkBlock(BlockBehaviour.Properties p_222063_) {
        super(ConstantInt.of(1), p_222063_);
    }

    @Override
    public int attemptUseCharge(
        SculkSpreader.ChargeCursor p_222073_, LevelAccessor p_222074_, BlockPos p_222075_, RandomSource p_222076_, SculkSpreader p_222077_, boolean p_222078_
    ) {
        int i = p_222073_.getCharge();
        if (i != 0 && p_222076_.nextInt(p_222077_.chargeDecayRate()) == 0) {
            BlockPos blockpos = p_222073_.getPos();
            boolean flag = blockpos.closerThan(p_222075_, p_222077_.noGrowthRadius());
            if (!flag && canPlaceGrowth(p_222074_, blockpos)) {
                int j = p_222077_.growthSpawnCost();
                if (p_222076_.nextInt(j) < i) {
                    BlockPos blockpos1 = blockpos.above();
                    BlockState blockstate = this.getRandomGrowthState(p_222074_, blockpos1, p_222076_, p_222077_.isWorldGeneration());
                    p_222074_.setBlock(blockpos1, blockstate, 3);
                    p_222074_.playSound(null, blockpos, blockstate.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
                }

                return Math.max(0, i - j);
            } else {
                return p_222076_.nextInt(p_222077_.additionalDecayRate()) != 0 ? i : i - (flag ? 1 : getDecayPenalty(p_222077_, blockpos, p_222075_, i));
            }
        } else {
            return i;
        }
    }

    private static int getDecayPenalty(SculkSpreader pSpreader, BlockPos pCursorPos, BlockPos pRootPos, int pCharge) {
        int i = pSpreader.noGrowthRadius();
        float f = Mth.square((float)Math.sqrt(pCursorPos.distSqr(pRootPos)) - i);
        int j = Mth.square(24 - i);
        float f1 = Math.min(1.0F, f / j);
        return Math.max(1, (int)(pCharge * f1 * 0.5F));
    }

    private BlockState getRandomGrowthState(LevelAccessor pLevel, BlockPos pPos, RandomSource pRandom, boolean pIsWorldGeneration) {
        BlockState blockstate;
        if (pRandom.nextInt(11) == 0) {
            blockstate = Blocks.SCULK_SHRIEKER.defaultBlockState().setValue(SculkShriekerBlock.CAN_SUMMON, pIsWorldGeneration);
        } else {
            blockstate = Blocks.SCULK_SENSOR.defaultBlockState();
        }

        return blockstate.hasProperty(BlockStateProperties.WATERLOGGED) && !pLevel.getFluidState(pPos).isEmpty()
            ? blockstate.setValue(BlockStateProperties.WATERLOGGED, true)
            : blockstate;
    }

    private static boolean canPlaceGrowth(LevelAccessor pLevel, BlockPos pPos) {
        BlockState blockstate = pLevel.getBlockState(pPos.above());
        if (blockstate.isAir() || blockstate.is(Blocks.WATER) && blockstate.getFluidState().is(Fluids.WATER)) {
            int i = 0;

            for (BlockPos blockpos : BlockPos.betweenClosed(pPos.offset(-4, 0, -4), pPos.offset(4, 2, 4))) {
                BlockState blockstate1 = pLevel.getBlockState(blockpos);
                if (blockstate1.is(Blocks.SCULK_SENSOR) || blockstate1.is(Blocks.SCULK_SHRIEKER)) {
                    i++;
                }

                if (i > 2) {
                    return false;
                }
            }

            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean canChangeBlockStateOnSpread() {
        return false;
    }
}