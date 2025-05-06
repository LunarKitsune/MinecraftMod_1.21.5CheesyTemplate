package net.minecraft.world.level.material;

import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.InsideBlockEffectType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

public abstract class LavaFluid extends FlowingFluid {
    public static final float MIN_LEVEL_CUTOFF = 0.44444445F;

    @Override
    public Fluid getFlowing() {
        return Fluids.FLOWING_LAVA;
    }

    @Override
    public Fluid getSource() {
        return Fluids.LAVA;
    }

    @Override
    public Item getBucket() {
        return Items.LAVA_BUCKET;
    }

    @Override
    public void animateTick(Level p_230567_, BlockPos p_230568_, FluidState p_230569_, RandomSource p_230570_) {
        BlockPos blockpos = p_230568_.above();
        if (p_230567_.getBlockState(blockpos).isAir() && !p_230567_.getBlockState(blockpos).isSolidRender()) {
            if (p_230570_.nextInt(100) == 0) {
                double d0 = p_230568_.getX() + p_230570_.nextDouble();
                double d1 = p_230568_.getY() + 1.0;
                double d2 = p_230568_.getZ() + p_230570_.nextDouble();
                p_230567_.addParticle(ParticleTypes.LAVA, d0, d1, d2, 0.0, 0.0, 0.0);
                p_230567_.playLocalSound(
                    d0, d1, d2, SoundEvents.LAVA_POP, SoundSource.AMBIENT, 0.2F + p_230570_.nextFloat() * 0.2F, 0.9F + p_230570_.nextFloat() * 0.15F, false
                );
            }

            if (p_230570_.nextInt(200) == 0) {
                p_230567_.playLocalSound(
                    p_230568_.getX(),
                    p_230568_.getY(),
                    p_230568_.getZ(),
                    SoundEvents.LAVA_AMBIENT,
                    SoundSource.AMBIENT,
                    0.2F + p_230570_.nextFloat() * 0.2F,
                    0.9F + p_230570_.nextFloat() * 0.15F,
                    false
                );
            }
        }
    }

    @Override
    public void randomTick(ServerLevel p_367000_, BlockPos p_230573_, FluidState p_230574_, RandomSource p_230575_) {
        if (p_367000_.getGameRules().getBoolean(GameRules.RULE_DOFIRETICK)) {
            if (p_367000_.getGameRules().getBoolean(GameRules.RULE_ALLOWFIRETICKAWAYFROMPLAYERS) || p_367000_.anyPlayerCloseEnoughForSpawning(p_230573_)) {
                int i = p_230575_.nextInt(3);
                if (i > 0) {
                    BlockPos blockpos = p_230573_;

                    for (int j = 0; j < i; j++) {
                        blockpos = blockpos.offset(p_230575_.nextInt(3) - 1, 1, p_230575_.nextInt(3) - 1);
                        if (!p_367000_.isLoaded(blockpos)) {
                            return;
                        }

                        BlockState blockstate = p_367000_.getBlockState(blockpos);
                        if (blockstate.isAir()) {
                            if (this.hasFlammableNeighbours(p_367000_, blockpos)) {
                                p_367000_.setBlockAndUpdate(blockpos, net.minecraftforge.event.ForgeEventFactory.fireFluidPlaceBlockEvent(p_367000_, blockpos, p_230573_, Blocks.FIRE.defaultBlockState()));
                                return;
                            }
                        } else if (blockstate.blocksMotion()) {
                            return;
                        }
                    }
                } else {
                    for (int k = 0; k < 3; k++) {
                        BlockPos blockpos1 = p_230573_.offset(p_230575_.nextInt(3) - 1, 0, p_230575_.nextInt(3) - 1);
                        if (!p_367000_.isLoaded(blockpos1)) {
                            return;
                        }

                        if (p_367000_.isEmptyBlock(blockpos1.above()) && this.isFlammable(p_367000_, blockpos1, Direction.UP)) {
                            p_367000_.setBlockAndUpdate(blockpos1.above(), net.minecraftforge.event.ForgeEventFactory.fireFluidPlaceBlockEvent(p_367000_, blockpos1.above(), p_230573_, Blocks.FIRE.defaultBlockState()));
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void entityInside(Level p_397670_, BlockPos p_395605_, Entity p_396547_, InsideBlockEffectApplier p_395270_) {
        p_395270_.apply(InsideBlockEffectType.LAVA_IGNITE);
        p_395270_.runAfter(InsideBlockEffectType.LAVA_IGNITE, Entity::lavaHurt);
    }

    private boolean hasFlammableNeighbours(LevelReader pLevel, BlockPos pPos) {
        for (Direction direction : Direction.values()) {
            if (this.isFlammable(pLevel, pPos.relative(direction), direction.getOpposite())) {
                return true;
            }
        }

        return false;
    }

    /** @deprecated Forge: use {@link LavaFluid#isFlammable(LevelReader,BlockPos,Direction)} instead */
    private boolean isFlammable(LevelReader pLevel, BlockPos pPos) {
        return pLevel.isInsideBuildHeight(pPos.getY()) && !pLevel.hasChunkAt(pPos) ? false : pLevel.getBlockState(pPos).ignitedByLava();
    }

    private boolean isFlammable(LevelReader pLevel, BlockPos pPos, Direction face) {
        return pLevel.isInsideBuildHeight(pPos.getY()) && !pLevel.hasChunkAt(pPos)
            ? false
            : pLevel.getBlockState(pPos).ignitedByLava() && pLevel.getBlockState(pPos).isFlammable(pLevel, pPos, face);
     }

    @Nullable
    @Override
    public ParticleOptions getDripParticle() {
        return ParticleTypes.DRIPPING_LAVA;
    }

    @Override
    protected void beforeDestroyingBlock(LevelAccessor pLevel, BlockPos pPos, BlockState pState) {
        this.fizz(pLevel, pPos);
    }

    @Override
    public int getSlopeFindDistance(LevelReader pLevel) {
        return pLevel.dimensionType().ultraWarm() ? 4 : 2;
    }

    @Override
    public BlockState createLegacyBlock(FluidState pState) {
        return Blocks.LAVA.defaultBlockState().setValue(LiquidBlock.LEVEL, getLegacyLevel(pState));
    }

    @Override
    public boolean isSame(Fluid pFluid) {
        return pFluid == Fluids.LAVA || pFluid == Fluids.FLOWING_LAVA;
    }

    @Override
    public int getDropOff(LevelReader pLevel) {
        return pLevel.dimensionType().ultraWarm() ? 1 : 2;
    }

    @Override
    public boolean canBeReplacedWith(FluidState pFluidState, BlockGetter pBlockReader, BlockPos pPos, Fluid pFluid, Direction pDirection) {
        return pFluidState.getHeight(pBlockReader, pPos) >= 0.44444445F && pFluid.is(FluidTags.WATER);
    }

    @Override
    public int getTickDelay(LevelReader p_76226_) {
        return p_76226_.dimensionType().ultraWarm() ? 10 : 30;
    }

    @Override
    public int getSpreadDelay(Level p_76203_, BlockPos p_76204_, FluidState p_76205_, FluidState p_76206_) {
        int i = this.getTickDelay(p_76203_);
        if (!p_76205_.isEmpty()
            && !p_76206_.isEmpty()
            && !p_76205_.getValue(FALLING)
            && !p_76206_.getValue(FALLING)
            && p_76206_.getHeight(p_76203_, p_76204_) > p_76205_.getHeight(p_76203_, p_76204_)
            && p_76203_.getRandom().nextInt(4) != 0) {
            i *= 4;
        }

        return i;
    }

    private void fizz(LevelAccessor pLevel, BlockPos pPos) {
        pLevel.levelEvent(1501, pPos, 0);
    }

    @Override
    protected boolean canConvertToSource(ServerLevel p_362658_) {
        return p_362658_.getGameRules().getBoolean(GameRules.RULE_LAVA_SOURCE_CONVERSION);
    }

    @Override
    protected void spreadTo(LevelAccessor pLevel, BlockPos pPos, BlockState pBlockState, Direction pDirection, FluidState pFluidState) {
        if (pDirection == Direction.DOWN) {
            FluidState fluidstate = pLevel.getFluidState(pPos);
            if (this.is(FluidTags.LAVA) && fluidstate.is(FluidTags.WATER)) {
                if (pBlockState.getBlock() instanceof LiquidBlock) {
                    pLevel.setBlock(pPos, net.minecraftforge.event.ForgeEventFactory.fireFluidPlaceBlockEvent(pLevel, pPos, pPos, Blocks.STONE.defaultBlockState()), 3);

                }

                this.fizz(pLevel, pPos);
                return;
            }
        }

        super.spreadTo(pLevel, pPos, pBlockState, pDirection, pFluidState);
    }

    @Override
    protected boolean isRandomlyTicking() {
        return true;
    }

    @Override
    protected float getExplosionResistance() {
        return 100.0F;
    }

    @Override
    public Optional<SoundEvent> getPickupSound() {
        return Optional.of(SoundEvents.BUCKET_FILL_LAVA);
    }

    public static class Flowing extends LavaFluid {
        @Override
        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> pBuilder) {
            super.createFluidStateDefinition(pBuilder);
            pBuilder.add(LEVEL);
        }

        @Override
        public int getAmount(FluidState pState) {
            return pState.getValue(LEVEL);
        }

        @Override
        public boolean isSource(FluidState pState) {
            return false;
        }
    }

    public static class Source extends LavaFluid {
        @Override
        public int getAmount(FluidState pState) {
            return 8;
        }

        @Override
        public boolean isSource(FluidState pState) {
            return true;
        }
    }
}
