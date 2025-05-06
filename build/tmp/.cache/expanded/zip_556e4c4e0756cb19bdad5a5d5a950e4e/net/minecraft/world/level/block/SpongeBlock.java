package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.redstone.Orientation;

public class SpongeBlock extends Block {
    public static final MapCodec<SpongeBlock> CODEC = simpleCodec(SpongeBlock::new);
    public static final int MAX_DEPTH = 6;
    public static final int MAX_COUNT = 64;
    private static final Direction[] ALL_DIRECTIONS = Direction.values();

    @Override
    public MapCodec<SpongeBlock> codec() {
        return CODEC;
    }

    public SpongeBlock(BlockBehaviour.Properties p_56796_) {
        super(p_56796_);
    }

    @Override
    protected void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
        if (!pOldState.is(pState.getBlock())) {
            this.tryAbsorbWater(pLevel, pPos);
        }
    }

    @Override
    protected void neighborChanged(BlockState p_56801_, Level p_56802_, BlockPos p_56803_, Block p_56804_, @Nullable Orientation p_361333_, boolean p_56806_) {
        this.tryAbsorbWater(p_56802_, p_56803_);
        super.neighborChanged(p_56801_, p_56802_, p_56803_, p_56804_, p_361333_, p_56806_);
    }

    protected void tryAbsorbWater(Level pLevel, BlockPos pPos) {
        if (this.removeWaterBreadthFirstSearch(pLevel, pPos)) {
            pLevel.setBlock(pPos, Blocks.WET_SPONGE.defaultBlockState(), 2);
            pLevel.playSound(null, pPos, SoundEvents.SPONGE_ABSORB, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    private boolean removeWaterBreadthFirstSearch(Level pLevel, BlockPos pPos) {
        BlockState spongeState = pLevel.getBlockState(pPos);
        return BlockPos.breadthFirstTraversal(
                pPos,
                6,
                65,
                (p_277519_, p_277492_) -> {
                    for (Direction direction : ALL_DIRECTIONS) {
                        p_277492_.accept(p_277519_.relative(direction));
                    }
                },
                p_296944_ -> {
                    if (p_296944_.equals(pPos)) {
                        return BlockPos.TraversalNodeStatus.ACCEPT;
                    } else {
                        BlockState blockstate = pLevel.getBlockState(p_296944_);
                        FluidState fluidstate = pLevel.getFluidState(p_296944_);
                        if (!spongeState.canBeHydrated(pLevel, pPos, fluidstate, p_296944_)) {
                            return BlockPos.TraversalNodeStatus.SKIP;
                        } else if (blockstate.getBlock() instanceof BucketPickup bucketpickup
                            && !bucketpickup.pickupBlock(null, pLevel, p_296944_, blockstate).isEmpty()) {
                            return BlockPos.TraversalNodeStatus.ACCEPT;
                        } else {
                            if (blockstate.getBlock() instanceof LiquidBlock) {
                                pLevel.setBlock(p_296944_, Blocks.AIR.defaultBlockState(), 3);
                            } else {
                                if (!blockstate.is(Blocks.KELP)
                                    && !blockstate.is(Blocks.KELP_PLANT)
                                    && !blockstate.is(Blocks.SEAGRASS)
                                    && !blockstate.is(Blocks.TALL_SEAGRASS)) {
                                    return BlockPos.TraversalNodeStatus.SKIP;
                                }

                                BlockEntity blockentity = blockstate.hasBlockEntity() ? pLevel.getBlockEntity(p_296944_) : null;
                                dropResources(blockstate, pLevel, p_296944_, blockentity);
                                pLevel.setBlock(p_296944_, Blocks.AIR.defaultBlockState(), 3);
                            }

                            return BlockPos.TraversalNodeStatus.ACCEPT;
                        }
                    }
                }
            )
            > 1;
    }
}
