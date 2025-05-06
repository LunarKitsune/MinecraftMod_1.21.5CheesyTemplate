package net.minecraft.world.entity.ai.goal.target;

import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.raid.Raider;

public class NearestAttackableWitchTargetGoal<T extends LivingEntity> extends NearestAttackableTargetGoal<T> {
    private boolean canAttack = true;

    public NearestAttackableWitchTargetGoal(
        Raider pRaider, Class<T> pTargetType, int pInterval, boolean pMustSee, boolean pMustReach, @Nullable TargetingConditions.Selector pSelector
    ) {
        super(pRaider, pTargetType, pInterval, pMustSee, pMustReach, pSelector);
    }

    public void setCanAttack(boolean pCanAttack) {
        this.canAttack = pCanAttack;
    }

    @Override
    public boolean canUse() {
        return this.canAttack && super.canUse();
    }
}