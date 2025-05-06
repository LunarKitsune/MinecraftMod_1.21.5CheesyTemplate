package net.minecraft.world.entity.animal.horse;

import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;

public class Donkey extends AbstractChestedHorse {
    public Donkey(EntityType<? extends Donkey> p_30672_, Level p_30673_) {
        super(p_30672_, p_30673_);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.DONKEY_AMBIENT;
    }

    @Override
    protected SoundEvent getAngrySound() {
        return SoundEvents.DONKEY_ANGRY;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.DONKEY_DEATH;
    }

    @Nullable
    @Override
    protected SoundEvent getEatingSound() {
        return SoundEvents.DONKEY_EAT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource pDamageSource) {
        return SoundEvents.DONKEY_HURT;
    }

    @Override
    public boolean canMate(Animal pOtherAnimal) {
        if (pOtherAnimal == this) {
            return false;
        } else {
            return !(pOtherAnimal instanceof Donkey) && !(pOtherAnimal instanceof Horse) ? false : this.canParent() && ((AbstractHorse)pOtherAnimal).canParent();
        }
    }

    @Override
    protected void playJumpSound() {
        this.playSound(SoundEvents.DONKEY_JUMP, 0.4F, 1.0F);
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel p_149530_, AgeableMob p_149531_) {
        EntityType<? extends AbstractHorse> entitytype = p_149531_ instanceof Horse ? EntityType.MULE : EntityType.DONKEY;
        AbstractHorse abstracthorse = entitytype.create(p_149530_, EntitySpawnReason.BREEDING);
        if (abstracthorse != null) {
            this.setOffspringAttributes(p_149531_, abstracthorse);
        }

        return abstracthorse;
    }
}