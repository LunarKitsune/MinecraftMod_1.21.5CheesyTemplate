package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class GrowingPlantHeadBlock extends GrowingPlantBlock implements BonemealableBlock {
    public static final IntegerProperty AGE = BlockStateProperties.AGE_25;
    public static final int MAX_AGE = 25;
    private final double growPerTickProbability;

    protected GrowingPlantHeadBlock(BlockBehaviour.Properties pProperties, Direction pGrowthDirection, VoxelShape pShape, boolean pScheduleFluidTicks, double pGrowPerTickProbability) {
        super(pProperties, pGrowthDirection, pShape, pScheduleFluidTicks);
        this.growPerTickProbability = pGrowPerTickProbability;
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
    }

    @Override
    protected abstract MapCodec<? extends GrowingPlantHeadBlock> codec();

    @Override
    public BlockState getStateForPlacement(RandomSource p_364827_) {
        return this.defaultBlockState().setValue(AGE, p_364827_.nextInt(25));
    }

    @Override
    protected boolean isRandomlyTicking(BlockState pState) {
        return pState.getValue(AGE) < 25;
    }

    @Override
    protected void randomTick(BlockState p_221350_, ServerLevel p_221351_, BlockPos p_221352_, RandomSource p_221353_) {
        var vanilla = p_221353_.nextDouble() < this.growPerTickProbability;
        var target = p_221352_.relative(this.growthDirection);
        if (p_221350_.getValue(AGE) < 25 && net.minecraftforge.common.ForgeHooks.onCropsGrowPre(p_221351_, target, p_221351_.getBlockState(target), vanilla)) {
            BlockPos blockpos = p_221352_.relative(this.growthDirection);
            if (this.canGrowInto(p_221351_.getBlockState(blockpos))) {
                p_221351_.setBlockAndUpdate(blockpos, this.getGrowIntoState(p_221350_, p_221351_.random));
                net.minecraftforge.common.ForgeHooks.onCropsGrowPost(p_221351_, blockpos, p_221351_.getBlockState(blockpos));
            }
        }
    }

    protected BlockState getGrowIntoState(BlockState pState, RandomSource pRandom) {
        return pState.cycle(AGE);
    }

    public BlockState getMaxAgeState(BlockState pState) {
        return pState.setValue(AGE, 25);
    }

    public boolean isMaxAge(BlockState pState) {
        return pState.getValue(AGE) == 25;
    }

    protected BlockState updateBodyAfterConvertedFromHead(BlockState pHead, BlockState pBody) {
        return pBody;
    }

    @Override
    protected BlockState updateShape(
        BlockState p_53951_,
        LevelReader p_366005_,
        ScheduledTickAccess p_361719_,
        BlockPos p_53955_,
        Direction p_53952_,
        BlockPos p_53956_,
        BlockState p_53953_,
        RandomSource p_364682_
    ) {
        if (p_53952_ == this.growthDirection.getOpposite()) {
            if (!p_53951_.canSurvive(p_366005_, p_53955_)) {
                p_361719_.scheduleTick(p_53955_, this, 1);
            } else {
                BlockState blockstate = p_366005_.getBlockState(p_53955_.relative(this.growthDirection));
                if (blockstate.is(this) || blockstate.is(this.getBodyBlock())) {
                    return this.updateBodyAfterConvertedFromHead(p_53951_, this.getBodyBlock().defaultBlockState());
                }
            }
        }

        if (p_53952_ != this.growthDirection || !p_53953_.is(this) && !p_53953_.is(this.getBodyBlock())) {
            if (this.scheduleFluidTicks) {
                p_361719_.scheduleTick(p_53955_, Fluids.WATER, Fluids.WATER.getTickDelay(p_366005_));
            }

            return super.updateShape(p_53951_, p_366005_, p_361719_, p_53955_, p_53952_, p_53956_, p_53953_, p_364682_);
        } else {
            return this.updateBodyAfterConvertedFromHead(p_53951_, this.getBodyBlock().defaultBlockState());
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(AGE);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader p_255931_, BlockPos p_256046_, BlockState p_256550_) {
        return this.canGrowInto(p_255931_.getBlockState(p_256046_.relative(this.growthDirection)));
    }

    @Override
    public boolean isBonemealSuccess(Level p_221343_, RandomSource p_221344_, BlockPos p_221345_, BlockState p_221346_) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel p_221337_, RandomSource p_221338_, BlockPos p_221339_, BlockState p_221340_) {
        BlockPos blockpos = p_221339_.relative(this.growthDirection);
        int i = Math.min(p_221340_.getValue(AGE) + 1, 25);
        int j = this.getBlocksToGrowWhenBonemealed(p_221338_);

        for (int k = 0; k < j && this.canGrowInto(p_221337_.getBlockState(blockpos)); k++) {
            p_221337_.setBlockAndUpdate(blockpos, p_221340_.setValue(AGE, i));
            blockpos = blockpos.relative(this.growthDirection);
            i = Math.min(i + 1, 25);
        }
    }

    protected abstract int getBlocksToGrowWhenBonemealed(RandomSource pRandom);

    protected abstract boolean canGrowInto(BlockState pState);

    @Override
    protected GrowingPlantHeadBlock getHeadBlock() {
        return this;
    }
}
