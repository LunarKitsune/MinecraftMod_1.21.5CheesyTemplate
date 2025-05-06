package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
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
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SoulSandBlock extends Block {
    public static final MapCodec<SoulSandBlock> CODEC = simpleCodec(SoulSandBlock::new);
    private static final VoxelShape SHAPE = Block.column(16.0, 0.0, 14.0);
    private static final int BUBBLE_COLUMN_CHECK_DELAY = 20;

    @Override
    public MapCodec<SoulSandBlock> codec() {
        return CODEC;
    }

    public SoulSandBlock(BlockBehaviour.Properties p_56672_) {
        super(p_56672_);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState pState, BlockGetter pReader, BlockPos pPos) {
        return Shapes.block();
    }

    @Override
    protected VoxelShape getVisualShape(BlockState pState, BlockGetter pReader, BlockPos pPos, CollisionContext pContext) {
        return Shapes.block();
    }

    @Override
    protected void tick(BlockState p_222457_, ServerLevel p_222458_, BlockPos p_222459_, RandomSource p_222460_) {
        BubbleColumnBlock.updateColumn(p_222458_, p_222459_.above(), p_222457_);
    }

    @Override
    protected BlockState updateShape(
        BlockState p_56689_,
        LevelReader p_370196_,
        ScheduledTickAccess p_363093_,
        BlockPos p_56693_,
        Direction p_56690_,
        BlockPos p_56694_,
        BlockState p_56691_,
        RandomSource p_365298_
    ) {
        if (p_56690_ == Direction.UP && p_56691_.is(Blocks.WATER)) {
            p_363093_.scheduleTick(p_56693_, this, 20);
        }

        return super.updateShape(p_56689_, p_370196_, p_363093_, p_56693_, p_56690_, p_56694_, p_56691_, p_365298_);
    }

    @Override
    protected void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
        pLevel.scheduleTick(pPos, this, 20);
    }

    @Override
    protected boolean isPathfindable(BlockState p_56679_, PathComputationType p_56682_) {
        return false;
    }

    @Override
    protected float getShadeBrightness(BlockState p_222462_, BlockGetter p_222463_, BlockPos p_222464_) {
        return 0.2F;
    }
}