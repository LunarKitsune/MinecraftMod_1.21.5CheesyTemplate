package net.minecraft.world.entity.monster.hoglin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;

public interface HoglinBase {
    int ATTACK_ANIMATION_DURATION = 10;
    float PROBABILITY_OF_SPAWNING_AS_BABY = 0.2F;

    int getAttackAnimationRemainingTicks();

    static boolean hurtAndThrowTarget(ServerLevel pLevel, LivingEntity pEntity, LivingEntity pTarget) {
        float f1 = (float)pEntity.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float f;
        if (!pEntity.isBaby() && (int)f1 > 0) {
            f = f1 / 2.0F + pLevel.random.nextInt((int)f1);
        } else {
            f = f1;
        }

        DamageSource damagesource = pEntity.damageSources().mobAttack(pEntity);
        boolean flag = pTarget.hurtServer(pLevel, damagesource, f);
        if (flag) {
            EnchantmentHelper.doPostAttackEffects(pLevel, pTarget, damagesource);
            if (!pEntity.isBaby()) {
                throwTarget(pEntity, pTarget);
            }
        }

        return flag;
    }

    static void throwTarget(LivingEntity pHoglin, LivingEntity pTarget) {
        double d0 = pHoglin.getAttributeValue(Attributes.ATTACK_KNOCKBACK);
        double d1 = pTarget.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
        double d2 = d0 - d1;
        if (!(d2 <= 0.0)) {
            double d3 = pTarget.getX() - pHoglin.getX();
            double d4 = pTarget.getZ() - pHoglin.getZ();
            float f = pHoglin.level().random.nextInt(21) - 10;
            double d5 = d2 * (pHoglin.level().random.nextFloat() * 0.5F + 0.2F);
            Vec3 vec3 = new Vec3(d3, 0.0, d4).normalize().scale(d5).yRot(f);
            double d6 = d2 * pHoglin.level().random.nextFloat() * 0.5;
            pTarget.push(vec3.x, d6, vec3.z);
            pTarget.hurtMarked = true;
        }
    }
}