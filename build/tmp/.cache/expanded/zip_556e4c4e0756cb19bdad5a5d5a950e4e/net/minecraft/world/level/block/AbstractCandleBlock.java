package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public abstract class AbstractCandleBlock extends Block {
    public static final int LIGHT_PER_CANDLE = 3;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    @Override
    protected abstract MapCodec<? extends AbstractCandleBlock> codec();

    protected AbstractCandleBlock(BlockBehaviour.Properties p_151898_) {
        super(p_151898_);
    }

    protected abstract Iterable<Vec3> getParticleOffsets(BlockState pState);

    public static boolean isLit(BlockState pState) {
        return pState.hasProperty(LIT)
            && (pState.is(BlockTags.CANDLES) || pState.is(BlockTags.CANDLE_CAKES))
            && pState.getValue(LIT);
    }

    @Override
    protected void onProjectileHit(Level p_151905_, BlockState p_151906_, BlockHitResult p_151907_, Projectile p_151908_) {
        if (!p_151905_.isClientSide && p_151908_.isOnFire() && this.canBeLit(p_151906_)) {
            setLit(p_151905_, p_151906_, p_151907_.getBlockPos(), true);
        }
    }

    protected boolean canBeLit(BlockState pState) {
        return !pState.getValue(LIT);
    }

    @Override
    public void animateTick(BlockState p_220697_, Level p_220698_, BlockPos p_220699_, RandomSource p_220700_) {
        if (p_220697_.getValue(LIT)) {
            this.getParticleOffsets(p_220697_)
                .forEach(p_220695_ -> addParticlesAndSound(p_220698_, p_220695_.add(p_220699_.getX(), p_220699_.getY(), p_220699_.getZ()), p_220700_));
        }
    }

    private static void addParticlesAndSound(Level pLevel, Vec3 pOffset, RandomSource pRandom) {
        float f = pRandom.nextFloat();
        if (f < 0.3F) {
            pLevel.addParticle(ParticleTypes.SMOKE, pOffset.x, pOffset.y, pOffset.z, 0.0, 0.0, 0.0);
            if (f < 0.17F) {
                pLevel.playLocalSound(
                    pOffset.x + 0.5,
                    pOffset.y + 0.5,
                    pOffset.z + 0.5,
                    SoundEvents.CANDLE_AMBIENT,
                    SoundSource.BLOCKS,
                    1.0F + pRandom.nextFloat(),
                    pRandom.nextFloat() * 0.7F + 0.3F,
                    false
                );
            }
        }

        pLevel.addParticle(ParticleTypes.SMALL_FLAME, pOffset.x, pOffset.y, pOffset.z, 0.0, 0.0, 0.0);
    }

    public static void extinguish(@Nullable Player pPlayer, BlockState pState, LevelAccessor pLevel, BlockPos pPos) {
        setLit(pLevel, pState, pPos, false);
        if (pState.getBlock() instanceof AbstractCandleBlock) {
            ((AbstractCandleBlock)pState.getBlock())
                .getParticleOffsets(pState)
                .forEach(
                    p_151926_ -> pLevel.addParticle(
                        ParticleTypes.SMOKE,
                        pPos.getX() + p_151926_.x(),
                        pPos.getY() + p_151926_.y(),
                        pPos.getZ() + p_151926_.z(),
                        0.0,
                        0.1F,
                        0.0
                    )
                );
        }

        pLevel.playSound(null, pPos, SoundEvents.CANDLE_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
        pLevel.gameEvent(pPlayer, GameEvent.BLOCK_CHANGE, pPos);
    }

    private static void setLit(LevelAccessor pLevel, BlockState pState, BlockPos pPos, boolean pLit) {
        pLevel.setBlock(pPos, pState.setValue(LIT, pLit), 11);
    }

    @Override
    protected void onExplosionHit(BlockState p_310999_, ServerLevel p_368647_, BlockPos p_311846_, Explosion p_310799_, BiConsumer<ItemStack, BlockPos> p_310677_) {
        if (p_310799_.canTriggerBlocks() && p_310999_.getValue(LIT)) {
            extinguish(null, p_310999_, p_368647_, p_311846_);
        }

        super.onExplosionHit(p_310999_, p_368647_, p_311846_, p_310799_, p_310677_);
    }
}