package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CoralPlantBlock extends BaseCoralPlantTypeBlock {
    public static final MapCodec<CoralPlantBlock> CODEC = RecordCodecBuilder.mapCodec(
        p_360427_ -> p_360427_.group(CoralBlock.DEAD_CORAL_FIELD.forGetter(p_312756_ -> p_312756_.deadBlock), propertiesCodec()).apply(p_360427_, CoralPlantBlock::new)
    );
    private final Block deadBlock;
    private static final VoxelShape SHAPE = Block.column(12.0, 0.0, 15.0);

    @Override
    public MapCodec<CoralPlantBlock> codec() {
        return CODEC;
    }

    public CoralPlantBlock(Block pDeadBlock, BlockBehaviour.Properties pProperties) {
        super(pProperties);
        this.deadBlock = pDeadBlock;
    }

    @Override
    protected void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
        this.tryScheduleDieTick(pState, pLevel, pLevel, pLevel.random, pPos);
    }

    @Override
    protected void tick(BlockState p_221030_, ServerLevel p_221031_, BlockPos p_221032_, RandomSource p_221033_) {
        if (!scanForWater(p_221030_, p_221031_, p_221032_)) {
            p_221031_.setBlock(p_221032_, this.deadBlock.defaultBlockState().setValue(WATERLOGGED, false), 2);
        }
    }

    @Override
    protected BlockState updateShape(
        BlockState p_52183_,
        LevelReader p_368530_,
        ScheduledTickAccess p_368124_,
        BlockPos p_52187_,
        Direction p_52184_,
        BlockPos p_52188_,
        BlockState p_52185_,
        RandomSource p_362387_
    ) {
        if (p_52184_ == Direction.DOWN && !p_52183_.canSurvive(p_368530_, p_52187_)) {
            return Blocks.AIR.defaultBlockState();
        } else {
            this.tryScheduleDieTick(p_52183_, p_368530_, p_368124_, p_362387_, p_52187_);
            if (p_52183_.getValue(WATERLOGGED)) {
                p_368124_.scheduleTick(p_52187_, Fluids.WATER, Fluids.WATER.getTickDelay(p_368530_));
            }

            return super.updateShape(p_52183_, p_368530_, p_368124_, p_52187_, p_52184_, p_52188_, p_52185_, p_362387_);
        }
    }

    @Override
    protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return SHAPE;
    }
}