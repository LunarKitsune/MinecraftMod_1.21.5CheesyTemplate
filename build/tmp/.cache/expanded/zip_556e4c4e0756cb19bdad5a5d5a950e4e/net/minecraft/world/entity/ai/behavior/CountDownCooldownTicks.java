package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

public class CountDownCooldownTicks extends Behavior<LivingEntity> {
    private final MemoryModuleType<Integer> cooldownTicks;

    public CountDownCooldownTicks(MemoryModuleType<Integer> pCooldownTicks) {
        super(ImmutableMap.of(pCooldownTicks, MemoryStatus.VALUE_PRESENT));
        this.cooldownTicks = pCooldownTicks;
    }

    private Optional<Integer> getCooldownTickMemory(LivingEntity pEntity) {
        return pEntity.getBrain().getMemory(this.cooldownTicks);
    }

    @Override
    protected boolean timedOut(long p_147464_) {
        return false;
    }

    @Override
    protected boolean canStillUse(ServerLevel p_147468_, LivingEntity p_147469_, long p_147470_) {
        Optional<Integer> optional = this.getCooldownTickMemory(p_147469_);
        return optional.isPresent() && optional.get() > 0;
    }

    @Override
    protected void tick(ServerLevel p_147476_, LivingEntity p_147477_, long p_147478_) {
        Optional<Integer> optional = this.getCooldownTickMemory(p_147477_);
        p_147477_.getBrain().setMemory(this.cooldownTicks, optional.get() - 1);
    }

    @Override
    protected void stop(ServerLevel p_147472_, LivingEntity p_147473_, long p_147474_) {
        p_147473_.getBrain().eraseMemory(this.cooldownTicks);
    }
}