package net.minecraft.world.entity.ai.behavior;

import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryAccessor;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class StopAttackingIfTargetInvalid {
    private static final int TIMEOUT_TO_GET_WITHIN_ATTACK_RANGE = 200;

    public static <E extends Mob> BehaviorControl<E> create(StopAttackingIfTargetInvalid.TargetErasedCallback<E> pOnStopAttacking) {
        return create((p_364423_, p_147988_) -> false, pOnStopAttacking, true);
    }

    public static <E extends Mob> BehaviorControl<E> create(StopAttackingIfTargetInvalid.StopAttackCondition pCanStopAttacking) {
        return create(pCanStopAttacking, (p_363632_, p_217411_, p_217412_) -> {}, true);
    }

    public static <E extends Mob> BehaviorControl<E> create() {
        return create((p_367631_, p_147986_) -> false, (p_363605_, p_217408_, p_217409_) -> {}, true);
    }

    public static <E extends Mob> BehaviorControl<E> create(
        StopAttackingIfTargetInvalid.StopAttackCondition pCanStopAttacking, StopAttackingIfTargetInvalid.TargetErasedCallback<E> pOnStopAttacking, boolean pCanGrowTiredOfTryingToReachTarget
    ) {
        return BehaviorBuilder.create(
            p_258801_ -> p_258801_.group(p_258801_.present(MemoryModuleType.ATTACK_TARGET), p_258801_.registered(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE))
                .apply(
                    p_258801_,
                    (p_258787_, p_258788_) -> (p_359057_, p_359058_, p_359059_) -> {
                        LivingEntity livingentity = p_258801_.get(p_258787_);
                        if (p_359058_.canAttack(livingentity)
                            && (!pCanGrowTiredOfTryingToReachTarget || !isTiredOfTryingToReachTarget(p_359058_, p_258801_.tryGet(p_258788_)))
                            && livingentity.isAlive()
                            && livingentity.level() == p_359058_.level()
                            && !pCanStopAttacking.test(p_359057_, livingentity)) {
                            return true;
                        } else {
                            pOnStopAttacking.accept(p_359057_, p_359058_, livingentity);
                            p_258787_.erase();
                            return true;
                        }
                    }
                )
        );
    }

    private static boolean isTiredOfTryingToReachTarget(LivingEntity pEntity, Optional<Long> pTimeSinceInvalidTarget) {
        return pTimeSinceInvalidTarget.isPresent() && pEntity.level().getGameTime() - pTimeSinceInvalidTarget.get() > 200L;
    }

    @FunctionalInterface
    public interface StopAttackCondition {
        boolean test(ServerLevel pLevel, LivingEntity pEntity);
    }

    @FunctionalInterface
    public interface TargetErasedCallback<E> {
        void accept(ServerLevel pLevel, E pEntity, LivingEntity pTarget);
    }
}