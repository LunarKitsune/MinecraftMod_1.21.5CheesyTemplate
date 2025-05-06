package net.minecraft.world.entity.monster;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;

public class Stray extends AbstractSkeleton {
    public Stray(EntityType<? extends Stray> p_33836_, Level p_33837_) {
        super(p_33836_, p_33837_);
    }

    public static boolean checkStraySpawnRules(
        EntityType<Stray> pEntityType, ServerLevelAccessor pLevel, EntitySpawnReason pSpawnReason, BlockPos pPos, RandomSource pRandom
    ) {
        BlockPos blockpos = pPos;

        do {
            blockpos = blockpos.above();
        } while (pLevel.getBlockState(blockpos).is(Blocks.POWDER_SNOW));

        return checkMonsterSpawnRules(pEntityType, pLevel, pSpawnReason, pPos, pRandom)
            && (EntitySpawnReason.isSpawner(pSpawnReason) || pLevel.canSeeSky(blockpos.below()));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.STRAY_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource pDamageSource) {
        return SoundEvents.STRAY_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.STRAY_DEATH;
    }

    @Override
    protected SoundEvent getStepSound() {
        return SoundEvents.STRAY_STEP;
    }

    @Override
    protected AbstractArrow getArrow(ItemStack p_33846_, float p_33847_, @Nullable ItemStack p_343428_) {
        AbstractArrow abstractarrow = super.getArrow(p_33846_, p_33847_, p_343428_);
        if (abstractarrow instanceof Arrow) {
            ((Arrow)abstractarrow).addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 600));
        }

        return abstractarrow;
    }
}