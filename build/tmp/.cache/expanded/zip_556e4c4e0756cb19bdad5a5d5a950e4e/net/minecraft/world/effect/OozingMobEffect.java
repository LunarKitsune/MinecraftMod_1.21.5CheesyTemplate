package net.minecraft.world.effect;

import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;

class OozingMobEffect extends MobEffect {
    private static final int RADIUS_TO_CHECK_SLIMES = 2;
    public static final int SLIME_SIZE = 2;
    private final ToIntFunction<RandomSource> spawnedCount;

    protected OozingMobEffect(MobEffectCategory pCategory, int pColor, ToIntFunction<RandomSource> pSpawnedCount) {
        super(pCategory, pColor, ParticleTypes.ITEM_SLIME);
        this.spawnedCount = pSpawnedCount;
    }

    @VisibleForTesting
    protected static int numberOfSlimesToSpawn(int pMaxEntityCramming, OozingMobEffect.NearbySlimes pNearbySlimes, int pSpawnCount) {
        return pMaxEntityCramming < 1 ? pSpawnCount : Mth.clamp(0, pMaxEntityCramming - pNearbySlimes.count(pMaxEntityCramming), pSpawnCount);
    }

    @Override
    public void onMobRemoved(ServerLevel p_362223_, LivingEntity p_329549_, int p_329953_, Entity.RemovalReason p_332875_) {
        if (p_332875_ == Entity.RemovalReason.KILLED) {
            int i = this.spawnedCount.applyAsInt(p_329549_.getRandom());
            int j = p_362223_.getGameRules().getInt(GameRules.RULE_MAX_ENTITY_CRAMMING);
            int k = numberOfSlimesToSpawn(j, OozingMobEffect.NearbySlimes.closeTo(p_329549_), i);

            for (int l = 0; l < k; l++) {
                this.spawnSlimeOffspring(p_329549_.level(), p_329549_.getX(), p_329549_.getY() + 0.5, p_329549_.getZ());
            }
        }
    }

    private void spawnSlimeOffspring(Level pLevel, double pX, double pY, double pZ) {
        Slime slime = EntityType.SLIME.create(pLevel, EntitySpawnReason.TRIGGERED);
        if (slime != null) {
            slime.setSize(2, true);
            slime.snapTo(pX, pY, pZ, pLevel.getRandom().nextFloat() * 360.0F, 0.0F);
            pLevel.addFreshEntity(slime);
        }
    }

    @FunctionalInterface
    protected interface NearbySlimes {
        int count(int pMaxEntityCramming);

        static OozingMobEffect.NearbySlimes closeTo(LivingEntity pEntity) {
            return p_374929_ -> {
                List<Slime> list = new ArrayList<>();
                pEntity.level().getEntities(EntityType.SLIME, pEntity.getBoundingBox().inflate(2.0), p_344894_ -> p_344894_ != pEntity, list, p_374929_);
                return list.size();
            };
        }
    }
}