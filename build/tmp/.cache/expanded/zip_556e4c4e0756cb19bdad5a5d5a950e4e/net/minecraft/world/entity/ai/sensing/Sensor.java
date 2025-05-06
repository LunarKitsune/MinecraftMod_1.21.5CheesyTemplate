package net.minecraft.world.entity.ai.sensing;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;

public abstract class Sensor<E extends LivingEntity> {
    private static final RandomSource RANDOM = RandomSource.createThreadSafe();
    private static final int DEFAULT_SCAN_RATE = 20;
    private static final int DEFAULT_TARGETING_RANGE = 16;
    private static final TargetingConditions TARGET_CONDITIONS = TargetingConditions.forNonCombat().range(16.0);
    private static final TargetingConditions TARGET_CONDITIONS_IGNORE_INVISIBILITY_TESTING = TargetingConditions.forNonCombat().range(16.0).ignoreInvisibilityTesting();
    private static final TargetingConditions ATTACK_TARGET_CONDITIONS = TargetingConditions.forCombat().range(16.0);
    private static final TargetingConditions ATTACK_TARGET_CONDITIONS_IGNORE_INVISIBILITY_TESTING = TargetingConditions.forCombat().range(16.0).ignoreInvisibilityTesting();
    private static final TargetingConditions ATTACK_TARGET_CONDITIONS_IGNORE_LINE_OF_SIGHT = TargetingConditions.forCombat().range(16.0).ignoreLineOfSight();
    private static final TargetingConditions ATTACK_TARGET_CONDITIONS_IGNORE_INVISIBILITY_AND_LINE_OF_SIGHT = TargetingConditions.forCombat().range(16.0).ignoreLineOfSight().ignoreInvisibilityTesting();
    private final int scanRate;
    private long timeToTick;

    public Sensor(int pScanRate) {
        this.scanRate = pScanRate;
        this.timeToTick = RANDOM.nextInt(pScanRate);
    }

    public Sensor() {
        this(20);
    }

    public final void tick(ServerLevel pLevel, E pEntity) {
        if (--this.timeToTick <= 0L) {
            this.timeToTick = this.scanRate;
            this.updateTargetingConditionRanges(pEntity);
            this.doTick(pLevel, pEntity);
        }
    }

    private void updateTargetingConditionRanges(E pEntity) {
        double d0 = pEntity.getAttributeValue(Attributes.FOLLOW_RANGE);
        TARGET_CONDITIONS.range(d0);
        TARGET_CONDITIONS_IGNORE_INVISIBILITY_TESTING.range(d0);
        ATTACK_TARGET_CONDITIONS.range(d0);
        ATTACK_TARGET_CONDITIONS_IGNORE_INVISIBILITY_TESTING.range(d0);
        ATTACK_TARGET_CONDITIONS_IGNORE_LINE_OF_SIGHT.range(d0);
        ATTACK_TARGET_CONDITIONS_IGNORE_INVISIBILITY_AND_LINE_OF_SIGHT.range(d0);
    }

    protected abstract void doTick(ServerLevel pLevel, E pEntity);

    public abstract Set<MemoryModuleType<?>> requires();

    public static boolean isEntityTargetable(ServerLevel pLevel, LivingEntity pEntity, LivingEntity pTarget) {
        return pEntity.getBrain().isMemoryValue(MemoryModuleType.ATTACK_TARGET, pTarget)
            ? TARGET_CONDITIONS_IGNORE_INVISIBILITY_TESTING.test(pLevel, pEntity, pTarget)
            : TARGET_CONDITIONS.test(pLevel, pEntity, pTarget);
    }

    public static boolean isEntityAttackable(ServerLevel pLevel, LivingEntity pEntity, LivingEntity pTarget) {
        return pEntity.getBrain().isMemoryValue(MemoryModuleType.ATTACK_TARGET, pTarget)
            ? ATTACK_TARGET_CONDITIONS_IGNORE_INVISIBILITY_TESTING.test(pLevel, pEntity, pTarget)
            : ATTACK_TARGET_CONDITIONS.test(pLevel, pEntity, pTarget);
    }

    public static BiPredicate<ServerLevel, LivingEntity> wasEntityAttackableLastNTicks(LivingEntity pEntity, int pTicks) {
        return rememberPositives(pTicks, (p_366099_, p_365289_) -> isEntityAttackable(p_366099_, pEntity, p_365289_));
    }

    public static boolean isEntityAttackableIgnoringLineOfSight(ServerLevel pLevel, LivingEntity pEntity, LivingEntity pTarget) {
        return pEntity.getBrain().isMemoryValue(MemoryModuleType.ATTACK_TARGET, pTarget)
            ? ATTACK_TARGET_CONDITIONS_IGNORE_INVISIBILITY_AND_LINE_OF_SIGHT.test(pLevel, pEntity, pTarget)
            : ATTACK_TARGET_CONDITIONS_IGNORE_LINE_OF_SIGHT.test(pLevel, pEntity, pTarget);
    }

    static <T, U> BiPredicate<T, U> rememberPositives(int pTicks, BiPredicate<T, U> pPredicate) {
        AtomicInteger atomicinteger = new AtomicInteger(0);
        return (p_367981_, p_361364_) -> {
            if (pPredicate.test(p_367981_, p_361364_)) {
                atomicinteger.set(pTicks);
                return true;
            } else {
                return atomicinteger.decrementAndGet() >= 0;
            }
        };
    }
}