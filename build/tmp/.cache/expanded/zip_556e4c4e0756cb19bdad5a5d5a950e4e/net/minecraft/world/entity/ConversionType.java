package net.minecraft.world.entity;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.Scoreboard;

public enum ConversionType {
    SINGLE(true) {
        @Override
        void convert(Mob p_362402_, Mob p_366485_, ConversionParams p_364039_) {
            Entity entity = p_362402_.getFirstPassenger();
            p_366485_.copyPosition(p_362402_);
            p_366485_.setDeltaMovement(p_362402_.getDeltaMovement());
            if (entity != null) {
                entity.stopRiding();
                entity.boardingCooldown = 0;

                for (Entity entity1 : p_366485_.getPassengers()) {
                    entity1.stopRiding();
                    entity1.remove(Entity.RemovalReason.DISCARDED);
                }

                entity.startRiding(p_366485_);
            }

            Entity entity2 = p_362402_.getVehicle();
            if (entity2 != null) {
                p_362402_.stopRiding();
                p_366485_.startRiding(entity2);
            }

            if (p_364039_.keepEquipment()) {
                for (EquipmentSlot equipmentslot : EquipmentSlot.VALUES) {
                    ItemStack itemstack = p_362402_.getItemBySlot(equipmentslot);
                    if (!itemstack.isEmpty()) {
                        p_366485_.setItemSlot(equipmentslot, itemstack.copyAndClear());
                        p_366485_.setDropChance(equipmentslot, p_362402_.getDropChances().byEquipment(equipmentslot));
                    }
                }
            }

            p_366485_.fallDistance = p_362402_.fallDistance;
            p_366485_.setSharedFlag(7, p_362402_.isFallFlying());
            p_366485_.lastHurtByPlayerMemoryTime = p_362402_.lastHurtByPlayerMemoryTime;
            p_366485_.hurtTime = p_362402_.hurtTime;
            p_366485_.yBodyRot = p_362402_.yBodyRot;
            p_366485_.setOnGround(p_362402_.onGround());
            p_362402_.getSleepingPos().ifPresent(p_366485_::setSleepingPos);
            Entity entity3 = p_362402_.getLeashHolder();
            if (entity3 != null) {
                p_366485_.setLeashedTo(entity3, true);
            }

            this.convertCommon(p_362402_, p_366485_, p_364039_);
        }
    },
    SPLIT_ON_DEATH(false) {
        @Override
        void convert(Mob p_362122_, Mob p_361715_, ConversionParams p_364524_) {
            Entity entity = p_362122_.getFirstPassenger();
            if (entity != null) {
                entity.stopRiding();
            }

            Entity entity1 = p_362122_.getLeashHolder();
            if (entity1 != null) {
                p_362122_.dropLeash();
            }

            this.convertCommon(p_362122_, p_361715_, p_364524_);
        }
    };

    private final boolean discardAfterConversion;

    ConversionType(final boolean pDiscardAfterConversion) {
        this.discardAfterConversion = pDiscardAfterConversion;
    }

    public boolean shouldDiscardAfterConversion() {
        return this.discardAfterConversion;
    }

    abstract void convert(Mob pOldMob, Mob pNewMob, ConversionParams pConversionParams);

    void convertCommon(Mob pOldMob, Mob pNewMob, ConversionParams pConversionParams) {
        pNewMob.setAbsorptionAmount(pOldMob.getAbsorptionAmount());

        for (MobEffectInstance mobeffectinstance : pOldMob.getActiveEffects()) {
            pNewMob.addEffect(new MobEffectInstance(mobeffectinstance));
        }

        if (pOldMob.isBaby()) {
            pNewMob.setBaby(true);
        }

        if (pOldMob instanceof AgeableMob ageablemob && pNewMob instanceof AgeableMob ageablemob1) {
            ageablemob1.setAge(ageablemob.getAge());
            ageablemob1.forcedAge = ageablemob.forcedAge;
            ageablemob1.forcedAgeTimer = ageablemob.forcedAgeTimer;
        }

        Brain<?> brain = pOldMob.getBrain();
        Brain<?> brain1 = pNewMob.getBrain();
        if (brain.checkMemory(MemoryModuleType.ANGRY_AT, MemoryStatus.REGISTERED) && brain.hasMemoryValue(MemoryModuleType.ANGRY_AT)) {
            brain1.setMemory(MemoryModuleType.ANGRY_AT, brain.getMemory(MemoryModuleType.ANGRY_AT));
        }

        if (pConversionParams.preserveCanPickUpLoot()) {
            pNewMob.setCanPickUpLoot(pOldMob.canPickUpLoot());
        }

        pNewMob.setLeftHanded(pOldMob.isLeftHanded());
        pNewMob.setNoAi(pOldMob.isNoAi());
        if (pOldMob.isPersistenceRequired()) {
            pNewMob.setPersistenceRequired();
        }

        if (pOldMob.hasCustomName()) {
            pNewMob.setCustomName(pOldMob.getCustomName());
            pNewMob.setCustomNameVisible(pOldMob.isCustomNameVisible());
        }

        pNewMob.setSharedFlagOnFire(pOldMob.isOnFire());
        pNewMob.setInvulnerable(pOldMob.isInvulnerable());
        pNewMob.setNoGravity(pOldMob.isNoGravity());
        pNewMob.setPortalCooldown(pOldMob.getPortalCooldown());
        pNewMob.setSilent(pOldMob.isSilent());
        pOldMob.getTags().forEach(pNewMob::addTag);
        if (pConversionParams.team() != null) {
            Scoreboard scoreboard = pNewMob.level().getScoreboard();
            scoreboard.addPlayerToTeam(pNewMob.getStringUUID(), pConversionParams.team());
            if (pOldMob.getTeam() != null && pOldMob.getTeam() == pConversionParams.team()) {
                scoreboard.removePlayerFromTeam(pOldMob.getStringUUID(), pOldMob.getTeam());
            }
        }

        if (pOldMob instanceof Zombie zombie1 && zombie1.canBreakDoors() && pNewMob instanceof Zombie zombie) {
            zombie.setCanBreakDoors(true);
        }
    }
}