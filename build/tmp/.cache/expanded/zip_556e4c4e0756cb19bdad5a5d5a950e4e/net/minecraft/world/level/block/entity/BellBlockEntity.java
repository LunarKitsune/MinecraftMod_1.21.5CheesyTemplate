package net.minecraft.world.level.block.entity;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.apache.commons.lang3.mutable.MutableInt;

public class BellBlockEntity extends BlockEntity {
    private static final int DURATION = 50;
    private static final int GLOW_DURATION = 60;
    private static final int MIN_TICKS_BETWEEN_SEARCHES = 60;
    private static final int MAX_RESONATION_TICKS = 40;
    private static final int TICKS_BEFORE_RESONATION = 5;
    private static final int SEARCH_RADIUS = 48;
    private static final int HEAR_BELL_RADIUS = 32;
    private static final int HIGHLIGHT_RAIDERS_RADIUS = 48;
    private long lastRingTimestamp;
    public int ticks;
    public boolean shaking;
    public Direction clickDirection;
    private List<LivingEntity> nearbyEntities;
    private boolean resonating;
    private int resonationTicks;

    public BellBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(BlockEntityType.BELL, pPos, pBlockState);
    }

    @Override
    public boolean triggerEvent(int pId, int pType) {
        if (pId == 1) {
            this.updateEntities();
            this.resonationTicks = 0;
            this.clickDirection = Direction.from3DDataValue(pType);
            this.ticks = 0;
            this.shaking = true;
            return true;
        } else {
            return super.triggerEvent(pId, pType);
        }
    }

    private static void tick(
        Level pLevel, BlockPos pPos, BlockState pState, BellBlockEntity pBlockEntity, BellBlockEntity.ResonationEndAction pResonationEndAction
    ) {
        if (pBlockEntity.shaking) {
            pBlockEntity.ticks++;
        }

        if (pBlockEntity.ticks >= 50) {
            pBlockEntity.shaking = false;
            pBlockEntity.ticks = 0;
        }

        if (pBlockEntity.ticks >= 5 && pBlockEntity.resonationTicks == 0 && areRaidersNearby(pPos, pBlockEntity.nearbyEntities)) {
            pBlockEntity.resonating = true;
            pLevel.playSound(null, pPos, SoundEvents.BELL_RESONATE, SoundSource.BLOCKS, 1.0F, 1.0F);
        }

        if (pBlockEntity.resonating) {
            if (pBlockEntity.resonationTicks < 40) {
                pBlockEntity.resonationTicks++;
            } else {
                pResonationEndAction.run(pLevel, pPos, pBlockEntity.nearbyEntities);
                pBlockEntity.resonating = false;
            }
        }
    }

    public static void clientTick(Level pLevel, BlockPos pPos, BlockState pState, BellBlockEntity pBlockEntity) {
        tick(pLevel, pPos, pState, pBlockEntity, BellBlockEntity::showBellParticles);
    }

    public static void serverTick(Level pLevel, BlockPos pPos, BlockState pState, BellBlockEntity pBlockEntity) {
        tick(pLevel, pPos, pState, pBlockEntity, BellBlockEntity::makeRaidersGlow);
    }

    public void onHit(Direction pDirection) {
        BlockPos blockpos = this.getBlockPos();
        this.clickDirection = pDirection;
        if (this.shaking) {
            this.ticks = 0;
        } else {
            this.shaking = true;
        }

        this.level.blockEvent(blockpos, this.getBlockState().getBlock(), 1, pDirection.get3DDataValue());
    }

    private void updateEntities() {
        BlockPos blockpos = this.getBlockPos();
        if (this.level.getGameTime() > this.lastRingTimestamp + 60L || this.nearbyEntities == null) {
            this.lastRingTimestamp = this.level.getGameTime();
            AABB aabb = new AABB(blockpos).inflate(48.0);
            this.nearbyEntities = this.level.getEntitiesOfClass(LivingEntity.class, aabb);
        }

        if (!this.level.isClientSide) {
            for (LivingEntity livingentity : this.nearbyEntities) {
                if (livingentity.isAlive() && !livingentity.isRemoved() && blockpos.closerToCenterThan(livingentity.position(), 32.0)) {
                    livingentity.getBrain().setMemory(MemoryModuleType.HEARD_BELL_TIME, this.level.getGameTime());
                }
            }
        }
    }

    private static boolean areRaidersNearby(BlockPos pPos, List<LivingEntity> pRaiders) {
        for (LivingEntity livingentity : pRaiders) {
            if (livingentity.isAlive()
                && !livingentity.isRemoved()
                && pPos.closerToCenterThan(livingentity.position(), 32.0)
                && livingentity.getType().is(EntityTypeTags.RAIDERS)) {
                return true;
            }
        }

        return false;
    }

    private static void makeRaidersGlow(Level pLevel, BlockPos pPos, List<LivingEntity> pRaiders) {
        pRaiders.stream().filter(p_155219_ -> isRaiderWithinRange(pPos, p_155219_)).forEach(BellBlockEntity::glow);
    }

    private static void showBellParticles(Level pLevel, BlockPos pPos, List<LivingEntity> pRaiders) {
        MutableInt mutableint = new MutableInt(16700985);
        int i = (int)pRaiders.stream().filter(p_375333_ -> pPos.closerToCenterThan(p_375333_.position(), 48.0)).count();
        pRaiders.stream()
            .filter(p_155213_ -> isRaiderWithinRange(pPos, p_155213_))
            .forEach(
                p_327289_ -> {
                    float f = 1.0F;
                    double d0 = Math.sqrt(
                        (p_327289_.getX() - pPos.getX()) * (p_327289_.getX() - pPos.getX())
                            + (p_327289_.getZ() - pPos.getZ()) * (p_327289_.getZ() - pPos.getZ())
                    );
                    double d1 = pPos.getX() + 0.5F + 1.0 / d0 * (p_327289_.getX() - pPos.getX());
                    double d2 = pPos.getZ() + 0.5F + 1.0 / d0 * (p_327289_.getZ() - pPos.getZ());
                    int j = Mth.clamp((i - 21) / -2, 3, 15);

                    for (int k = 0; k < j; k++) {
                        int l = mutableint.addAndGet(5);
                        pLevel.addParticle(ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, l), d1, pPos.getY() + 0.5F, d2, 0.0, 0.0, 0.0);
                    }
                }
            );
    }

    private static boolean isRaiderWithinRange(BlockPos pPos, LivingEntity pRaider) {
        return pRaider.isAlive()
            && !pRaider.isRemoved()
            && pPos.closerToCenterThan(pRaider.position(), 48.0)
            && pRaider.getType().is(EntityTypeTags.RAIDERS);
    }

    private static void glow(LivingEntity pEntity) {
        pEntity.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60));
    }

    @FunctionalInterface
    interface ResonationEndAction {
        void run(Level pLevel, BlockPos pPos, List<LivingEntity> pRaiders);
    }
}