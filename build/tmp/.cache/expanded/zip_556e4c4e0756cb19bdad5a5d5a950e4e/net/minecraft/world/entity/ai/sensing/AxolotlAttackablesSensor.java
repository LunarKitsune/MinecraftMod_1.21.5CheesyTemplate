package net.minecraft.world.entity.ai.sensing;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class AxolotlAttackablesSensor extends NearestVisibleLivingEntitySensor {
    public static final float TARGET_DETECTION_DISTANCE = 8.0F;

    @Override
    protected boolean isMatchingEntity(ServerLevel p_369264_, LivingEntity p_148266_, LivingEntity p_148267_) {
        return this.isClose(p_148266_, p_148267_)
            && p_148267_.isInWater()
            && (this.isHostileTarget(p_148267_) || this.isHuntTarget(p_148266_, p_148267_))
            && Sensor.isEntityAttackable(p_369264_, p_148266_, p_148267_);
    }

    private boolean isHuntTarget(LivingEntity pAttacker, LivingEntity pTarget) {
        return !pAttacker.getBrain().hasMemoryValue(MemoryModuleType.HAS_HUNTING_COOLDOWN) && pTarget.getType().is(EntityTypeTags.AXOLOTL_HUNT_TARGETS);
    }

    private boolean isHostileTarget(LivingEntity pTarget) {
        return pTarget.getType().is(EntityTypeTags.AXOLOTL_ALWAYS_HOSTILES);
    }

    private boolean isClose(LivingEntity pAttacker, LivingEntity pTarget) {
        return pTarget.distanceToSqr(pAttacker) <= 64.0;
    }

    @Override
    protected MemoryModuleType<LivingEntity> getMemory() {
        return MemoryModuleType.NEAREST_ATTACKABLE;
    }
}