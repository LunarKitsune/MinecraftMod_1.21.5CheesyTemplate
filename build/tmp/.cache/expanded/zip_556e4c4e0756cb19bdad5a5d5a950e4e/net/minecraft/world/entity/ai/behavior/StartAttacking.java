package net.minecraft.world.entity.ai.behavior;

import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryAccessor;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class StartAttacking {
    public static <E extends Mob> BehaviorControl<E> create(StartAttacking.TargetFinder<E> pTargetFinder) {
        return create((p_362883_, p_24212_) -> true, pTargetFinder);
    }

    public static <E extends Mob> BehaviorControl<E> create(StartAttacking.StartAttackingCondition<E> pCondition, StartAttacking.TargetFinder<E> pTargetFinder) {
        return BehaviorBuilder.create(
            p_258782_ -> p_258782_.group(p_258782_.absent(MemoryModuleType.ATTACK_TARGET), p_258782_.registered(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE))
                .apply(p_258782_, (p_258778_, p_258779_) -> (p_359048_, p_359049_, p_359050_) -> {
                    if (!pCondition.test(p_359048_, p_359049_)) {
                        return false;
                    } else {
                        Optional<? extends LivingEntity> optional = pTargetFinder.get(p_359048_, p_359049_);
                        if (optional.isEmpty()) {
                            return false;
                        } else {
                            LivingEntity livingentity = optional.get();
                            if (!p_359049_.canAttack(livingentity)) {
                                return false;
                            } else {
                                var changeTargetEvent = net.minecraftforge.event.ForgeEventFactory.onLivingChangeTargetBehavior(p_359049_, livingentity);
                                if (changeTargetEvent.isCanceled())
                                    return false;

                                p_258778_.set(changeTargetEvent.getNewTarget());
                                p_258779_.erase();
                                return true;
                            }
                        }
                    }
                })
        );
    }

    @FunctionalInterface
    public interface StartAttackingCondition<E> {
        boolean test(ServerLevel pLevel, E pMob);
    }

    @FunctionalInterface
    public interface TargetFinder<E> {
        Optional<? extends LivingEntity> get(ServerLevel pLevel, E pMob);
    }
}
