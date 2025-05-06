package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BubbleColumnBlock extends Block implements BucketPickup {
    public static final MapCodec<BubbleColumnBlock> CODEC = simpleCodec(BubbleColumnBlock::new);
    public static final BooleanProperty DRAG_DOWN = BlockStateProperties.DRAG;
    private static final int CHECK_PERIOD = 5;

    @Override
    public MapCodec<BubbleColumnBlock> codec() {
        return CODEC;
    }

    public BubbleColumnBlock(BlockBehaviour.Properties p_50959_) {
        super(p_50959_);
        this.registerDefaultState(this.stateDefinition.any().setValue(DRAG_DOWN, true));
    }

    @Override
    protected void entityInside(BlockState p_50976_, Level p_50977_, BlockPos p_50978_, Entity p_50979_, InsideBlockEffectApplier p_395598_) {
        BlockState blockstate = p_50977_.getBlockState(p_50978_.above());
        boolean flag = blockstate.getCollisionShape(p_50977_, p_50978_).isEmpty() && blockstate.getFluidState().isEmpty();
        if (flag) {
            p_50979_.onAboveBubbleColumn(p_50976_.getValue(DRAG_DOWN), p_50978_);
        } else {
            p_50979_.onInsideBubbleColumn(p_50976_.getValue(DRAG_DOWN));
        }
    }

    @Override
    protected void tick(BlockState p_220888_, ServerLevel p_220889_, BlockPos p_220890_, RandomSource p_220891_) {
        updateColumn(p_220889_, p_220890_, p_220888_, p_220889_.getBlockState(p_220890_.below()));
    }

    @Override
    protected FluidState getFluidState(BlockState pState) {
        return Fluids.WATER.getSource(false);
    }

    public static void updateColumn(LevelAccessor pLevel, BlockPos pPos, BlockState pState) {
        updateColumn(pLevel, pPos, pLevel.getBlockState(pPos), pState);
    }

    public static void updateColumn(LevelAccessor pLevel, BlockPos pPos, BlockState pFluid, BlockState pState) {
        if (canExistIn(pFluid)) {
            BlockState blockstate = getColumnState(pState);
            pLevel.setBlock(pPos, blockstate, 2);
            BlockPos.MutableBlockPos blockpos$mutableblockpos = pPos.mutable().move(Direction.UP);

            while (canExistIn(pLevel.getBlockState(blockpos$mutableblockpos))) {
                if (!pLevel.setBlock(blockpos$mutableblockpos, blockstate, 2)) {
                    return;
                }

                blockpos$mutableblockpos.move(Direction.UP);
            }
        }
    }

    private static boolean canExistIn(BlockState pBlockState) {
        return pBlockState.is(Blocks.BUBBLE_COLUMN)
            || pBlockState.is(Blocks.WATER) && pBlockState.getFluidState().getAmount() >= 8 && pBlockState.getFluidState().isSource();
    }

    private static BlockState getColumnState(BlockState pBlockState) {
        if (pBlockState.is(Blocks.BUBBLE_COLUMN)) {
            return pBlockState;
        } else if (pBlockState.is(Blocks.SOUL_SAND)) {
            return Blocks.BUBBLE_COLUMN.defaultBlockState().setValue(DRAG_DOWN, false);
        } else {
            return pBlockState.is(Blocks.MAGMA_BLOCK) ? Blocks.BUBBLE_COLUMN.defaultBlockState().setValue(DRAG_DOWN, true) : Blocks.WATER.defaultBlockState();
        }
    }

    @Override
    public void animateTick(BlockState p_220893_, Level p_220894_, BlockPos p_220895_, RandomSource p_220896_) {
        double d0 = p_220895_.getX();
        double d1 = p_220895_.getY();
        double d2 = p_220895_.getZ();
        if (p_220893_.getValue(DRAG_DOWN)) {
            p_220894_.addAlwaysVisibleParticle(ParticleTypes.CURRENT_DOWN, d0 + 0.5, d1 + 0.8, d2, 0.0, 0.0, 0.0);
            if (p_220896_.nextInt(200) == 0) {
                p_220894_.playLocalSound(
                    d0, d1, d2, SoundEvents.BUBBLE_COLUMN_WHIRLPOOL_AMBIENT, SoundSource.BLOCKS, 0.2F + p_220896_.nextFloat() * 0.2F, 0.9F + p_220896_.nextFloat() * 0.15F, false
                );
            }
        } else {
            p_220894_.addAlwaysVisibleParticle(ParticleTypes.BUBBLE_COLUMN_UP, d0 + 0.5, d1, d2 + 0.5, 0.0, 0.04, 0.0);
            p_220894_.addAlwaysVisibleParticle(ParticleTypes.BUBBLE_COLUMN_UP, d0 + p_220896_.nextFloat(), d1 + p_220896_.nextFloat(), d2 + p_220896_.nextFloat(), 0.0, 0.04, 0.0);
            if (p_220896_.nextInt(200) == 0) {
                p_220894_.playLocalSound(
                    d0, d1, d2, SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundSource.BLOCKS, 0.2F + p_220896_.nextFloat() * 0.2F, 0.9F + p_220896_.nextFloat() * 0.15F, false
                );
            }
        }
    }

    @Override
    protected BlockState updateShape(
        BlockState p_50990_,
        LevelReader p_366024_,
        ScheduledTickAccess p_365544_,
        BlockPos p_50994_,
        Direction p_50991_,
        BlockPos p_50995_,
        BlockState p_50992_,
        RandomSource p_363879_
    ) {
        p_365544_.scheduleTick(p_50994_, Fluids.WATER, Fluids.WATER.getTickDelay(p_366024_));
        if (!p_50990_.canSurvive(p_366024_, p_50994_)
            || p_50991_ == Direction.DOWN
            || p_50991_ == Direction.UP && !p_50992_.is(Blocks.BUBBLE_COLUMN) && canExistIn(p_50992_)) {
            p_365544_.scheduleTick(p_50994_, this, 5);
        }

        return super.updateShape(p_50990_, p_366024_, p_365544_, p_50994_, p_50991_, p_50995_, p_50992_, p_363879_);
    }

    @Override
    protected boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
        BlockState blockstate = pLevel.getBlockState(pPos.below());
        return blockstate.is(Blocks.BUBBLE_COLUMN) || blockstate.is(Blocks.MAGMA_BLOCK) || blockstate.is(Blocks.SOUL_SAND);
    }

    @Override
    protected VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return Shapes.empty();
    }

    @Override
    protected RenderShape getRenderShape(BlockState pState) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(DRAG_DOWN);
    }

    @Override
    public ItemStack pickupBlock(@Nullable LivingEntity p_392392_, LevelAccessor p_152712_, BlockPos p_152713_, BlockState p_152714_) {
        p_152712_.setBlock(p_152713_, Blocks.AIR.defaultBlockState(), 11);
        return new ItemStack(Items.WATER_BUCKET);
    }

    @Override
    public Optional<SoundEvent> getPickupSound() {
        return Fluids.WATER.getPickupSound();
    }
}