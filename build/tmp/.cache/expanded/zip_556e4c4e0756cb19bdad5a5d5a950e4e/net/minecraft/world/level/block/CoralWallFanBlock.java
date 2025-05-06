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

public class CoralWallFanBlock extends BaseCoralWallFanBlock {
    public static final MapCodec<CoralWallFanBlock> CODEC = RecordCodecBuilder.mapCodec(
        p_360428_ -> p_360428_.group(CoralBlock.DEAD_CORAL_FIELD.forGetter(p_311712_ -> p_311712_.deadBlock), propertiesCodec()).apply(p_360428_, CoralWallFanBlock::new)
    );
    private final Block deadBlock;

    @Override
    public MapCodec<CoralWallFanBlock> codec() {
        return CODEC;
    }

    public CoralWallFanBlock(Block pDeadBlock, BlockBehaviour.Properties pProperties) {
        super(pProperties);
        this.deadBlock = pDeadBlock;
    }

    @Override
    protected void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
        this.tryScheduleDieTick(pState, pLevel, pLevel, pLevel.random, pPos);
    }

    @Override
    protected void tick(BlockState p_221035_, ServerLevel p_221036_, BlockPos p_221037_, RandomSource p_221038_) {
        if (!scanForWater(p_221035_, p_221036_, p_221037_)) {
            p_221036_.setBlock(p_221037_, this.deadBlock.defaultBlockState().setValue(WATERLOGGED, false).setValue(FACING, p_221035_.getValue(FACING)), 2);
        }
    }

    @Override
    protected BlockState updateShape(
        BlockState p_52210_,
        LevelReader p_367217_,
        ScheduledTickAccess p_362720_,
        BlockPos p_52214_,
        Direction p_52211_,
        BlockPos p_52215_,
        BlockState p_52212_,
        RandomSource p_364592_
    ) {
        if (p_52211_.getOpposite() == p_52210_.getValue(FACING) && !p_52210_.canSurvive(p_367217_, p_52214_)) {
            return Blocks.AIR.defaultBlockState();
        } else {
            if (p_52210_.getValue(WATERLOGGED)) {
                p_362720_.scheduleTick(p_52214_, Fluids.WATER, Fluids.WATER.getTickDelay(p_367217_));
            }

            this.tryScheduleDieTick(p_52210_, p_367217_, p_362720_, p_364592_, p_52214_);
            return super.updateShape(p_52210_, p_367217_, p_362720_, p_52214_, p_52211_, p_52215_, p_52212_, p_364592_);
        }
    }
}