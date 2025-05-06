package net.minecraft.world.entity.monster.breeze;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Unit;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.LongJumpUtil;
import net.minecraft.world.entity.ai.behavior.Swim;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class LongJump extends Behavior<Breeze> {
    private static final int REQUIRED_AIR_BLOCKS_ABOVE = 4;
    private static final int JUMP_COOLDOWN_TICKS = 10;
    private static final int JUMP_COOLDOWN_WHEN_HURT_TICKS = 2;
    private static final int INHALING_DURATION_TICKS = Math.round(10.0F);
    private static final float DEFAULT_FOLLOW_RANGE = 24.0F;
    private static final float DEFAULT_MAX_JUMP_VELOCITY = 1.4F;
    private static final float MAX_JUMP_VELOCITY_MULTIPLIER = 0.058333334F;
    private static final ObjectArrayList<Integer> ALLOWED_ANGLES = new ObjectArrayList<>(Lists.newArrayList(40, 55, 60, 75, 80));

    @VisibleForTesting
    public LongJump() {
        super(
            Map.of(
                MemoryModuleType.ATTACK_TARGET,
                MemoryStatus.VALUE_PRESENT,
                MemoryModuleType.BREEZE_JUMP_COOLDOWN,
                MemoryStatus.VALUE_ABSENT,
                MemoryModuleType.BREEZE_JUMP_INHALING,
                MemoryStatus.REGISTERED,
                MemoryModuleType.BREEZE_JUMP_TARGET,
                MemoryStatus.REGISTERED,
                MemoryModuleType.BREEZE_SHOOT,
                MemoryStatus.VALUE_ABSENT,
                MemoryModuleType.WALK_TARGET,
                MemoryStatus.VALUE_ABSENT,
                MemoryModuleType.BREEZE_LEAVING_WATER,
                MemoryStatus.REGISTERED
            ),
            200
        );
    }

    public static boolean canRun(ServerLevel pLevel, Breeze pBreeze) {
        if (!pBreeze.onGround() && !pBreeze.isInWater()) {
            return false;
        } else if (Swim.shouldSwim(pBreeze)) {
            return false;
        } else if (pBreeze.getBrain().checkMemory(MemoryModuleType.BREEZE_JUMP_TARGET, MemoryStatus.VALUE_PRESENT)) {
            return true;
        } else {
            LivingEntity livingentity = pBreeze.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(null);
            if (livingentity == null) {
                return false;
            } else if (outOfAggroRange(pBreeze, livingentity)) {
                pBreeze.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
                return false;
            } else if (tooCloseForJump(pBreeze, livingentity)) {
                return false;
            } else if (!canJumpFromCurrentPosition(pLevel, pBreeze)) {
                return false;
            } else {
                BlockPos blockpos = snapToSurface(pBreeze, BreezeUtil.randomPointBehindTarget(livingentity, pBreeze.getRandom()));
                if (blockpos == null) {
                    return false;
                } else {
                    BlockState blockstate = pLevel.getBlockState(blockpos.below());
                    if (pBreeze.getType().isBlockDangerous(blockstate)) {
                        return false;
                    } else if (!BreezeUtil.hasLineOfSight(pBreeze, blockpos.getCenter()) && !BreezeUtil.hasLineOfSight(pBreeze, blockpos.above(4).getCenter())) {
                        return false;
                    } else {
                        pBreeze.getBrain().setMemory(MemoryModuleType.BREEZE_JUMP_TARGET, blockpos);
                        return true;
                    }
                }
            }
        }
    }

    protected boolean checkExtraStartConditions(ServerLevel p_312411_, Breeze p_309539_) {
        return canRun(p_312411_, p_309539_);
    }

    protected boolean canStillUse(ServerLevel p_310673_, Breeze p_311330_, long p_310051_) {
        return p_311330_.getPose() != Pose.STANDING && !p_311330_.getBrain().hasMemoryValue(MemoryModuleType.BREEZE_JUMP_COOLDOWN);
    }

    protected void start(ServerLevel p_310741_, Breeze p_312948_, long p_311377_) {
        if (p_312948_.getBrain().checkMemory(MemoryModuleType.BREEZE_JUMP_INHALING, MemoryStatus.VALUE_ABSENT)) {
            p_312948_.getBrain().setMemoryWithExpiry(MemoryModuleType.BREEZE_JUMP_INHALING, Unit.INSTANCE, INHALING_DURATION_TICKS);
        }

        p_312948_.setPose(Pose.INHALING);
        p_310741_.playSound(null, p_312948_, SoundEvents.BREEZE_CHARGE, SoundSource.HOSTILE, 1.0F, 1.0F);
        p_312948_.getBrain()
            .getMemory(MemoryModuleType.BREEZE_JUMP_TARGET)
            .ifPresent(p_311106_ -> p_312948_.lookAt(EntityAnchorArgument.Anchor.EYES, p_311106_.getCenter()));
    }

    protected void tick(ServerLevel p_312629_, Breeze p_310204_, long p_313176_) {
        boolean flag = p_310204_.isInWater();
        if (!flag && p_310204_.getBrain().checkMemory(MemoryModuleType.BREEZE_LEAVING_WATER, MemoryStatus.VALUE_PRESENT)) {
            p_310204_.getBrain().eraseMemory(MemoryModuleType.BREEZE_LEAVING_WATER);
        }

        if (isFinishedInhaling(p_310204_)) {
            Vec3 vec3 = p_310204_.getBrain()
                .getMemory(MemoryModuleType.BREEZE_JUMP_TARGET)
                .flatMap(p_375147_ -> calculateOptimalJumpVector(p_310204_, p_310204_.getRandom(), Vec3.atBottomCenterOf(p_375147_)))
                .orElse(null);
            if (vec3 == null) {
                p_310204_.setPose(Pose.STANDING);
                return;
            }

            if (flag) {
                p_310204_.getBrain().setMemory(MemoryModuleType.BREEZE_LEAVING_WATER, Unit.INSTANCE);
            }

            p_310204_.playSound(SoundEvents.BREEZE_JUMP, 1.0F, 1.0F);
            p_310204_.setPose(Pose.LONG_JUMPING);
            p_310204_.setYRot(p_310204_.yBodyRot);
            p_310204_.setDiscardFriction(true);
            p_310204_.setDeltaMovement(vec3);
        } else if (isFinishedJumping(p_310204_)) {
            p_310204_.playSound(SoundEvents.BREEZE_LAND, 1.0F, 1.0F);
            p_310204_.setPose(Pose.STANDING);
            p_310204_.setDiscardFriction(false);
            boolean flag1 = p_310204_.getBrain().hasMemoryValue(MemoryModuleType.HURT_BY);
            p_310204_.getBrain().setMemoryWithExpiry(MemoryModuleType.BREEZE_JUMP_COOLDOWN, Unit.INSTANCE, flag1 ? 2L : 10L);
            p_310204_.getBrain().setMemoryWithExpiry(MemoryModuleType.BREEZE_SHOOT, Unit.INSTANCE, 100L);
        }
    }

    protected void stop(ServerLevel p_309511_, Breeze p_311681_, long p_312980_) {
        if (p_311681_.getPose() == Pose.LONG_JUMPING || p_311681_.getPose() == Pose.INHALING) {
            p_311681_.setPose(Pose.STANDING);
        }

        p_311681_.getBrain().eraseMemory(MemoryModuleType.BREEZE_JUMP_TARGET);
        p_311681_.getBrain().eraseMemory(MemoryModuleType.BREEZE_JUMP_INHALING);
        p_311681_.getBrain().eraseMemory(MemoryModuleType.BREEZE_LEAVING_WATER);
    }

    private static boolean isFinishedInhaling(Breeze pBreeze) {
        return pBreeze.getBrain().getMemory(MemoryModuleType.BREEZE_JUMP_INHALING).isEmpty() && pBreeze.getPose() == Pose.INHALING;
    }

    private static boolean isFinishedJumping(Breeze pBreeze) {
        boolean flag = pBreeze.getPose() == Pose.LONG_JUMPING;
        boolean flag1 = pBreeze.onGround();
        boolean flag2 = pBreeze.isInWater() && pBreeze.getBrain().checkMemory(MemoryModuleType.BREEZE_LEAVING_WATER, MemoryStatus.VALUE_ABSENT);
        return flag && (flag1 || flag2);
    }

    @Nullable
    private static BlockPos snapToSurface(LivingEntity pOwner, Vec3 pTargetPos) {
        ClipContext clipcontext = new ClipContext(
            pTargetPos, pTargetPos.relative(Direction.DOWN, 10.0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, pOwner
        );
        HitResult hitresult = pOwner.level().clip(clipcontext);
        if (hitresult.getType() == HitResult.Type.BLOCK) {
            return BlockPos.containing(hitresult.getLocation()).above();
        } else {
            ClipContext clipcontext1 = new ClipContext(
                pTargetPos, pTargetPos.relative(Direction.UP, 10.0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, pOwner
            );
            HitResult hitresult1 = pOwner.level().clip(clipcontext1);
            return hitresult1.getType() == HitResult.Type.BLOCK ? BlockPos.containing(hitresult1.getLocation()).above() : null;
        }
    }

    private static boolean outOfAggroRange(Breeze pBreeze, LivingEntity pTarget) {
        return !pTarget.closerThan(pBreeze, pBreeze.getAttributeValue(Attributes.FOLLOW_RANGE));
    }

    private static boolean tooCloseForJump(Breeze pBreeze, LivingEntity pTarget) {
        return pTarget.distanceTo(pBreeze) - 4.0F <= 0.0F;
    }

    private static boolean canJumpFromCurrentPosition(ServerLevel pLevel, Breeze pBreeze) {
        BlockPos blockpos = pBreeze.blockPosition();
        if (pLevel.getBlockState(blockpos).is(Blocks.HONEY_BLOCK)) {
            return false;
        } else {
            for (int i = 1; i <= 4; i++) {
                BlockPos blockpos1 = blockpos.relative(Direction.UP, i);
                if (!pLevel.getBlockState(blockpos1).isAir() && !pLevel.getFluidState(blockpos1).is(FluidTags.WATER)) {
                    return false;
                }
            }

            return true;
        }
    }

    private static Optional<Vec3> calculateOptimalJumpVector(Breeze pBreeze, RandomSource pRandom, Vec3 pTarget) {
        for (int i : Util.shuffledCopy(ALLOWED_ANGLES, pRandom)) {
            float f = 0.058333334F * (float)pBreeze.getAttributeValue(Attributes.FOLLOW_RANGE);
            Optional<Vec3> optional = LongJumpUtil.calculateJumpVectorForAngle(pBreeze, pTarget, f, i, false);
            if (optional.isPresent()) {
                if (pBreeze.hasEffect(MobEffects.JUMP_BOOST)) {
                    double d0 = optional.get().normalize().y * pBreeze.getJumpBoostPower();
                    return optional.map(p_359268_ -> p_359268_.add(0.0, d0, 0.0));
                }

                return optional;
            }
        }

        return Optional.empty();
    }
}