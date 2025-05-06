package net.minecraft.world.entity.ai.targeting;

import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

public class TargetingConditions {
    public static final TargetingConditions DEFAULT = forCombat();
    private static final double MIN_VISIBILITY_DISTANCE_FOR_INVISIBLE_TARGET = 2.0;
    private final boolean isCombat;
    private double range = -1.0;
    private boolean checkLineOfSight = true;
    private boolean testInvisible = true;
    @Nullable
    private TargetingConditions.Selector selector;

    private TargetingConditions(boolean pIsCombat) {
        this.isCombat = pIsCombat;
    }

    public static TargetingConditions forCombat() {
        return new TargetingConditions(true);
    }

    public static TargetingConditions forNonCombat() {
        return new TargetingConditions(false);
    }

    public TargetingConditions copy() {
        TargetingConditions targetingconditions = this.isCombat ? forCombat() : forNonCombat();
        targetingconditions.range = this.range;
        targetingconditions.checkLineOfSight = this.checkLineOfSight;
        targetingconditions.testInvisible = this.testInvisible;
        targetingconditions.selector = this.selector;
        return targetingconditions;
    }

    public TargetingConditions range(double pDistance) {
        this.range = pDistance;
        return this;
    }

    public TargetingConditions ignoreLineOfSight() {
        this.checkLineOfSight = false;
        return this;
    }

    public TargetingConditions ignoreInvisibilityTesting() {
        this.testInvisible = false;
        return this;
    }

    public TargetingConditions selector(@Nullable TargetingConditions.Selector pSelector) {
        this.selector = pSelector;
        return this;
    }

    public boolean test(ServerLevel pLevel, @Nullable LivingEntity pEntity, LivingEntity pTarget) {
        if (pEntity == pTarget) {
            return false;
        } else if (!pTarget.canBeSeenByAnyone()) {
            return false;
        } else if (this.selector != null && !this.selector.test(pTarget, pLevel)) {
            return false;
        } else {
            if (pEntity == null) {
                if (this.isCombat && (!pTarget.canBeSeenAsEnemy() || pLevel.getDifficulty() == Difficulty.PEACEFUL)) {
                    return false;
                }
            } else {
                if (this.isCombat && (!pEntity.canAttack(pTarget) || !pEntity.canAttackType(pTarget.getType()) || pEntity.isAlliedTo(pTarget))) {
                    return false;
                }

                if (this.range > 0.0) {
                    double d0 = this.testInvisible ? pTarget.getVisibilityPercent(pEntity) : 1.0;
                    double d1 = Math.max(this.range * d0, 2.0);
                    double d2 = pEntity.distanceToSqr(pTarget.getX(), pTarget.getY(), pTarget.getZ());
                    if (d2 > d1 * d1) {
                        return false;
                    }
                }

                if (this.checkLineOfSight && pEntity instanceof Mob mob && !mob.getSensing().hasLineOfSight(pTarget)) {
                    return false;
                }
            }

            return true;
        }
    }

    @FunctionalInterface
    public interface Selector {
        boolean test(LivingEntity pEntity, ServerLevel pLevel);
    }
}