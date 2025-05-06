package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

public class CoralFanBlock extends BaseCoralFanBlock {
    public static final MapCodec<CoralFanBlock> CODEC = RecordCodecBuilder.mapCodec(
        p_360426_ -> p_360426_.group(CoralBlock.DEAD_CORAL_FIELD.forGetter(p_311032_ -> p_311032_.deadBlock), propertiesCodec()).apply(p_360426_, CoralFanBlock::new)
    );
    private final Block deadBlock;

    @Override
    public MapCodec<CoralFanBlock> codec() {
        return CODEC;
    }

    public CoralFanBlock(Block pDeadBlock, BlockBehaviour.Properties pProperties) {
        super(pProperties);
        this.deadBlock = pDeadBlock;
    }

    @Override
    protected void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
        this.tryScheduleDieTick(pState, pLevel, pLevel, pLevel.random, pPos);
    }

    @Override
    protected void tick(BlockState p_221025_, ServerLevel p_221026_, BlockPos p_221027_, RandomSource p_221028_) {
        if (!scanForWater(p_221025_, p_221026_, p_221027_)) {
            p_221026_.setBlock(p_221027_, this.deadBlock.defaultBlockState().setValue(WATERLOGGED, false), 2);
        }
    }

    @Override
    protected BlockState updateShape(
        BlockState p_52159_,
        LevelReader p_363934_,
        ScheduledTickAccess p_363811_,
        BlockPos p_52163_,
        Direction p_52160_,
        BlockPos p_52164_,
        BlockState p_52161_,
        RandomSource p_363437_
    ) {
        if (p_52160_ == Direction.DOWN && !p_52159_.canSurvive(p_363934_, p_52163_)) {
            return Blocks.AIR.defaultBlockState();
        } else {
            this.tryScheduleDieTick(p_52159_, p_363934_, p_363811_, p_363437_, p_52163_);
            if (p_52159_.getValue(WATERLOGGED)) {
                p_363811_.scheduleTick(p_52163_, Fluids.WATER, Fluids.WATER.getTickDelay(p_363934_));
            }

            return super.updateShape(p_52159_, p_363934_, p_363811_, p_52163_, p_52160_, p_52164_, p_52161_, p_363437_);
        }
    }
}