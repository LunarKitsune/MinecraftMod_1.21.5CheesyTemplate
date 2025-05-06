package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.vehicle.AbstractBoat;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class HoneyBlock extends HalfTransparentBlock {
    public static final MapCodec<HoneyBlock> CODEC = simpleCodec(HoneyBlock::new);
    private static final double SLIDE_STARTS_WHEN_VERTICAL_SPEED_IS_AT_LEAST = 0.13;
    private static final double MIN_FALL_SPEED_TO_BE_CONSIDERED_SLIDING = 0.08;
    private static final double THROTTLE_SLIDE_SPEED_TO = 0.05;
    private static final int SLIDE_ADVANCEMENT_CHECK_INTERVAL = 20;
    private static final VoxelShape SHAPE = Block.column(14.0, 0.0, 15.0);

    @Override
    public MapCodec<HoneyBlock> codec() {
        return CODEC;
    }

    public HoneyBlock(BlockBehaviour.Properties p_53985_) {
        super(p_53985_);
    }

    private static boolean doesEntityDoHoneyBlockSlideEffects(Entity pEntity) {
        return pEntity instanceof LivingEntity || pEntity instanceof AbstractMinecart || pEntity instanceof PrimedTnt || pEntity instanceof AbstractBoat;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
        return SHAPE;
    }

    @Override
    public void fallOn(Level p_153372_, BlockState p_153373_, BlockPos p_153374_, Entity p_153375_, double p_394083_) {
        p_153375_.playSound(SoundEvents.HONEY_BLOCK_SLIDE, 1.0F, 1.0F);
        if (!p_153372_.isClientSide) {
            p_153372_.broadcastEntityEvent(p_153375_, (byte)54);
        }

        if (p_153375_.causeFallDamage(p_394083_, 0.2F, p_153372_.damageSources().fall())) {
            p_153375_.playSound(this.soundType.getFallSound(), this.soundType.getVolume() * 0.5F, this.soundType.getPitch() * 0.75F);
        }
    }

    @Override
    protected void entityInside(BlockState p_54003_, Level p_54004_, BlockPos p_54005_, Entity p_54006_, InsideBlockEffectApplier p_395723_) {
        if (this.isSlidingDown(p_54005_, p_54006_)) {
            this.maybeDoSlideAchievement(p_54006_, p_54005_);
            this.doSlideMovement(p_54006_);
            this.maybeDoSlideEffects(p_54004_, p_54006_);
        }

        super.entityInside(p_54003_, p_54004_, p_54005_, p_54006_, p_395723_);
    }

    private static double getOldDeltaY(double pDeltaY) {
        return pDeltaY / 0.98F + 0.08;
    }

    private static double getNewDeltaY(double pDeltaY) {
        return (pDeltaY - 0.08) * 0.98F;
    }

    private boolean isSlidingDown(BlockPos pPos, Entity pEntity) {
        if (pEntity.onGround()) {
            return false;
        } else if (pEntity.getY() > pPos.getY() + 0.9375 - 1.0E-7) {
            return false;
        } else if (getOldDeltaY(pEntity.getDeltaMovement().y) >= -0.08) {
            return false;
        } else {
            double d0 = Math.abs(pPos.getX() + 0.5 - pEntity.getX());
            double d1 = Math.abs(pPos.getZ() + 0.5 - pEntity.getZ());
            double d2 = 0.4375 + pEntity.getBbWidth() / 2.0F;
            return d0 + 1.0E-7 > d2 || d1 + 1.0E-7 > d2;
        }
    }

    private void maybeDoSlideAchievement(Entity pEntity, BlockPos pPos) {
        if (pEntity instanceof ServerPlayer && pEntity.level().getGameTime() % 20L == 0L) {
            CriteriaTriggers.HONEY_BLOCK_SLIDE.trigger((ServerPlayer)pEntity, pEntity.level().getBlockState(pPos));
        }
    }

    private void doSlideMovement(Entity pEntity) {
        Vec3 vec3 = pEntity.getDeltaMovement();
        if (getOldDeltaY(pEntity.getDeltaMovement().y) < -0.13) {
            double d0 = -0.05 / getOldDeltaY(pEntity.getDeltaMovement().y);
            pEntity.setDeltaMovement(new Vec3(vec3.x * d0, getNewDeltaY(-0.05), vec3.z * d0));
        } else {
            pEntity.setDeltaMovement(new Vec3(vec3.x, getNewDeltaY(-0.05), vec3.z));
        }

        pEntity.resetFallDistance();
    }

    private void maybeDoSlideEffects(Level pLevel, Entity pEntity) {
        if (doesEntityDoHoneyBlockSlideEffects(pEntity)) {
            if (pLevel.random.nextInt(5) == 0) {
                pEntity.playSound(SoundEvents.HONEY_BLOCK_SLIDE, 1.0F, 1.0F);
            }

            if (!pLevel.isClientSide && pLevel.random.nextInt(5) == 0) {
                pLevel.broadcastEntityEvent(pEntity, (byte)53);
            }
        }
    }

    public static void showSlideParticles(Entity pEntity) {
        showParticles(pEntity, 5);
    }

    public static void showJumpParticles(Entity pEntity) {
        showParticles(pEntity, 10);
    }

    private static void showParticles(Entity pEntity, int pParticleCount) {
        if (pEntity.level().isClientSide) {
            BlockState blockstate = Blocks.HONEY_BLOCK.defaultBlockState();

            for (int i = 0; i < pParticleCount; i++) {
                pEntity.level()
                    .addParticle(
                        new BlockParticleOption(ParticleTypes.BLOCK, blockstate),
                        pEntity.getX(),
                        pEntity.getY(),
                        pEntity.getZ(),
                        0.0,
                        0.0,
                        0.0
                    );
            }
        }
    }
}