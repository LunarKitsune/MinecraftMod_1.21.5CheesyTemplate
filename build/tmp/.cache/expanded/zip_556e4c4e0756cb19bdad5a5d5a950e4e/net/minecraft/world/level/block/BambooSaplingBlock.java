package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BambooLeaves;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BambooSaplingBlock extends Block implements BonemealableBlock {
    public static final MapCodec<BambooSaplingBlock> CODEC = simpleCodec(BambooSaplingBlock::new);
    private static final VoxelShape SHAPE = Block.column(8.0, 0.0, 12.0);

    @Override
    public MapCodec<BambooSaplingBlock> codec() {
        return CODEC;
    }

    public BambooSaplingBlock(BlockBehaviour.Properties p_48957_) {
        super(p_48957_);
    }

    @Override
    protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return SHAPE.move(pState.getOffset(pPos));
    }

    @Override
    protected void randomTick(BlockState p_220753_, ServerLevel p_220754_, BlockPos p_220755_, RandomSource p_220756_) {
        if (p_220756_.nextInt(3) == 0 && p_220754_.isEmptyBlock(p_220755_.above()) && p_220754_.getRawBrightness(p_220755_.above(), 0) >= 9) {
            this.growBamboo(p_220754_, p_220755_);
        }
    }

    @Override
    protected boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
        return pLevel.getBlockState(pPos.below()).is(BlockTags.BAMBOO_PLANTABLE_ON);
    }

    @Override
    protected BlockState updateShape(
        BlockState p_48990_,
        LevelReader p_366200_,
        ScheduledTickAccess p_365833_,
        BlockPos p_48994_,
        Direction p_48991_,
        BlockPos p_48995_,
        BlockState p_48992_,
        RandomSource p_365774_
    ) {
        if (!p_48990_.canSurvive(p_366200_, p_48994_)) {
            return Blocks.AIR.defaultBlockState();
        } else {
            return p_48991_ == Direction.UP && p_48992_.is(Blocks.BAMBOO)
                ? Blocks.BAMBOO.defaultBlockState()
                : super.updateShape(p_48990_, p_366200_, p_365833_, p_48994_, p_48991_, p_48995_, p_48992_, p_365774_);
        }
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader p_312659_, BlockPos p_48965_, BlockState p_48966_, boolean p_376584_) {
        return new ItemStack(Items.BAMBOO);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader p_256136_, BlockPos p_256527_, BlockState p_255620_) {
        return p_256136_.getBlockState(p_256527_.above()).isAir();
    }

    @Override
    public boolean isBonemealSuccess(Level p_220748_, RandomSource p_220749_, BlockPos p_220750_, BlockState p_220751_) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel p_220743_, RandomSource p_220744_, BlockPos p_220745_, BlockState p_220746_) {
        this.growBamboo(p_220743_, p_220745_);
    }

    protected void growBamboo(Level pLevel, BlockPos pState) {
        pLevel.setBlock(pState.above(), Blocks.BAMBOO.defaultBlockState().setValue(BambooStalkBlock.LEAVES, BambooLeaves.SMALL), 3);
    }
}