package net.minecraft.world.entity.animal.horse;

import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityAttachment;
import net.minecraft.world.entity.EntityAttachments;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.SoundType;

public class Horse extends AbstractHorse {
    private static final EntityDataAccessor<Integer> DATA_ID_TYPE_VARIANT = SynchedEntityData.defineId(Horse.class, EntityDataSerializers.INT);
    private static final EntityDimensions BABY_DIMENSIONS = EntityType.HORSE
        .getDimensions()
        .withAttachments(EntityAttachments.builder().attach(EntityAttachment.PASSENGER, 0.0F, EntityType.HORSE.getHeight() + 0.125F, 0.0F))
        .scale(0.5F);
    private static final int DEFAULT_VARIANT = 0;

    public Horse(EntityType<? extends Horse> p_30689_, Level p_30690_) {
        super(p_30689_, p_30690_);
    }

    @Override
    protected void randomizeAttributes(RandomSource p_218815_) {
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(generateMaxHealth(p_218815_::nextInt));
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(generateSpeed(p_218815_::nextDouble));
        this.getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(generateJumpStrength(p_218815_::nextDouble));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder p_335516_) {
        super.defineSynchedData(p_335516_);
        p_335516_.define(DATA_ID_TYPE_VARIANT, 0);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putInt("Variant", this.getTypeVariant());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        this.setTypeVariant(pCompound.getIntOr("Variant", 0));
    }

    private void setTypeVariant(int pTypeVariant) {
        this.entityData.set(DATA_ID_TYPE_VARIANT, pTypeVariant);
    }

    private int getTypeVariant() {
        return this.entityData.get(DATA_ID_TYPE_VARIANT);
    }

    private void setVariantAndMarkings(Variant pVariant, Markings pMarking) {
        this.setTypeVariant(pVariant.getId() & 0xFF | pMarking.getId() << 8 & 0xFF00);
    }

    public Variant getVariant() {
        return Variant.byId(this.getTypeVariant() & 0xFF);
    }

    private void setVariant(Variant pVariant) {
        this.setTypeVariant(pVariant.getId() & 0xFF | this.getTypeVariant() & -256);
    }

    @Nullable
    @Override
    public <T> T get(DataComponentType<? extends T> p_394733_) {
        return p_394733_ == DataComponents.HORSE_VARIANT ? castComponentValue((DataComponentType<T>)p_394733_, this.getVariant()) : super.get(p_394733_);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter p_394196_) {
        this.applyImplicitComponentIfPresent(p_394196_, DataComponents.HORSE_VARIANT);
        super.applyImplicitComponents(p_394196_);
    }

    @Override
    protected <T> boolean applyImplicitComponent(DataComponentType<T> p_395072_, T p_397505_) {
        if (p_395072_ == DataComponents.HORSE_VARIANT) {
            this.setVariant(castComponentValue(DataComponents.HORSE_VARIANT, p_397505_));
            return true;
        } else {
            return super.applyImplicitComponent(p_395072_, p_397505_);
        }
    }

    public Markings getMarkings() {
        return Markings.byId((this.getTypeVariant() & 0xFF00) >> 8);
    }

    @Override
    protected void playGallopSound(SoundType p_30709_) {
        super.playGallopSound(p_30709_);
        if (this.random.nextInt(10) == 0) {
            this.playSound(SoundEvents.HORSE_BREATHE, p_30709_.getVolume() * 0.6F, p_30709_.getPitch());
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.HORSE_AMBIENT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.HORSE_DEATH;
    }

    @Nullable
    @Override
    protected SoundEvent getEatingSound() {
        return SoundEvents.HORSE_EAT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource pDamageSource) {
        return SoundEvents.HORSE_HURT;
    }

    @Override
    protected SoundEvent getAngrySound() {
        return SoundEvents.HORSE_ANGRY;
    }

    @Override
    public InteractionResult mobInteract(Player pPlayer, InteractionHand pHand) {
        boolean flag = !this.isBaby() && this.isTamed() && pPlayer.isSecondaryUseActive();
        if (!this.isVehicle() && !flag) {
            ItemStack itemstack = pPlayer.getItemInHand(pHand);
            if (!itemstack.isEmpty()) {
                if (this.isFood(itemstack)) {
                    return this.fedFood(pPlayer, itemstack);
                }

                if (!this.isTamed()) {
                    this.makeMad();
                    return InteractionResult.SUCCESS;
                }
            }

            return super.mobInteract(pPlayer, pHand);
        } else {
            return super.mobInteract(pPlayer, pHand);
        }
    }

    @Override
    public boolean canMate(Animal pOtherAnimal) {
        if (pOtherAnimal == this) {
            return false;
        } else {
            return !(pOtherAnimal instanceof Donkey) && !(pOtherAnimal instanceof Horse) ? false : this.canParent() && ((AbstractHorse)pOtherAnimal).canParent();
        }
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel p_149533_, AgeableMob p_149534_) {
        if (p_149534_ instanceof Donkey) {
            Mule mule = EntityType.MULE.create(p_149533_, EntitySpawnReason.BREEDING);
            if (mule != null) {
                this.setOffspringAttributes(p_149534_, mule);
            }

            return mule;
        } else {
            Horse horse = (Horse)p_149534_;
            Horse horse1 = EntityType.HORSE.create(p_149533_, EntitySpawnReason.BREEDING);
            if (horse1 != null) {
                int i = this.random.nextInt(9);
                Variant variant;
                if (i < 4) {
                    variant = this.getVariant();
                } else if (i < 8) {
                    variant = horse.getVariant();
                } else {
                    variant = Util.getRandom(Variant.values(), this.random);
                }

                int j = this.random.nextInt(5);
                Markings markings;
                if (j < 2) {
                    markings = this.getMarkings();
                } else if (j < 4) {
                    markings = horse.getMarkings();
                } else {
                    markings = Util.getRandom(Markings.values(), this.random);
                }

                horse1.setVariantAndMarkings(variant, markings);
                this.setOffspringAttributes(p_149534_, horse1);
            }

            return horse1;
        }
    }

    @Override
    public boolean canUseSlot(EquipmentSlot p_345205_) {
        return true;
    }

    @Override
    protected void hurtArmor(DamageSource p_363728_, float p_365413_) {
        this.doHurtEquipment(p_363728_, p_365413_, new EquipmentSlot[]{EquipmentSlot.BODY});
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor p_30703_, DifficultyInstance p_30704_, EntitySpawnReason p_370066_, @Nullable SpawnGroupData p_30706_) {
        RandomSource randomsource = p_30703_.getRandom();
        Variant variant;
        if (p_30706_ instanceof Horse.HorseGroupData) {
            variant = ((Horse.HorseGroupData)p_30706_).variant;
        } else {
            variant = Util.getRandom(Variant.values(), randomsource);
            p_30706_ = new Horse.HorseGroupData(variant);
        }

        this.setVariantAndMarkings(variant, Util.getRandom(Markings.values(), randomsource));
        return super.finalizeSpawn(p_30703_, p_30704_, p_370066_, p_30706_);
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose p_329389_) {
        return this.isBaby() ? BABY_DIMENSIONS : super.getDefaultDimensions(p_329389_);
    }

    public static class HorseGroupData extends AgeableMob.AgeableMobGroupData {
        public final Variant variant;

        public HorseGroupData(Variant pVariant) {
            super(true);
            this.variant = pVariant;
        }
    }
}