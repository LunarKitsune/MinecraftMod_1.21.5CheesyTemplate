package net.minecraft.world.effect;

import java.util.function.ToIntFunction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

class InfestedMobEffect extends MobEffect {
    private final float chanceToSpawn;
    private final ToIntFunction<RandomSource> spawnedCount;

    protected InfestedMobEffect(MobEffectCategory pCategory, int pColor, float pChanceToSpawn, ToIntFunction<RandomSource> pSpawnedCount) {
        super(pCategory, pColor, ParticleTypes.INFESTED);
        this.chanceToSpawn = pChanceToSpawn;
        this.spawnedCount = pSpawnedCount;
    }

    @Override
    public void onMobHurt(ServerLevel p_364179_, LivingEntity p_334146_, int p_328888_, DamageSource p_330722_, float p_331740_) {
        if (p_334146_.getRandom().nextFloat() <= this.chanceToSpawn) {
            int i = this.spawnedCount.applyAsInt(p_334146_.getRandom());

            for (int j = 0; j < i; j++) {
                this.spawnSilverfish(p_364179_, p_334146_, p_334146_.getX(), p_334146_.getY() + p_334146_.getBbHeight() / 2.0, p_334146_.getZ());
            }
        }
    }

    private void spawnSilverfish(ServerLevel pLevel, LivingEntity pEntity, double pX, double pY, double pZ) {
        Silverfish silverfish = EntityType.SILVERFISH.create(pLevel, EntitySpawnReason.TRIGGERED);
        if (silverfish != null) {
            RandomSource randomsource = pEntity.getRandom();
            float f = (float) (Math.PI / 2);
            float f1 = Mth.randomBetween(randomsource, (float) (-Math.PI / 2), (float) (Math.PI / 2));
            Vector3f vector3f = pEntity.getLookAngle().toVector3f().mul(0.3F).mul(1.0F, 1.5F, 1.0F).rotateY(f1);
            silverfish.snapTo(pX, pY, pZ, pLevel.getRandom().nextFloat() * 360.0F, 0.0F);
            silverfish.setDeltaMovement(new Vec3(vector3f));
            pLevel.addFreshEntity(silverfish);
            silverfish.playSound(SoundEvents.SILVERFISH_HURT);
        }
    }
}