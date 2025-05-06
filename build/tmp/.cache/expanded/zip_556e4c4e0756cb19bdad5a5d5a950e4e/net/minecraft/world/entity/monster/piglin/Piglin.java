package net.minecraft.world.entity.monster.piglin;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Dynamic;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.CrossbowAttackMob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.InventoryCarrier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class Piglin extends AbstractPiglin implements CrossbowAttackMob, InventoryCarrier {
    private static final EntityDataAccessor<Boolean> DATA_BABY_ID = SynchedEntityData.defineId(Piglin.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_IS_CHARGING_CROSSBOW = SynchedEntityData.defineId(Piglin.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_IS_DANCING = SynchedEntityData.defineId(Piglin.class, EntityDataSerializers.BOOLEAN);
    private static final ResourceLocation SPEED_MODIFIER_BABY_ID = ResourceLocation.withDefaultNamespace("baby");
    private static final AttributeModifier SPEED_MODIFIER_BABY = new AttributeModifier(SPEED_MODIFIER_BABY_ID, 0.2F, AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    private static final int MAX_HEALTH = 16;
    private static final float MOVEMENT_SPEED_WHEN_FIGHTING = 0.35F;
    private static final int ATTACK_DAMAGE = 5;
    private static final float CHANCE_OF_WEARING_EACH_ARMOUR_ITEM = 0.1F;
    private static final int MAX_PASSENGERS_ON_ONE_HOGLIN = 3;
    private static final float PROBABILITY_OF_SPAWNING_AS_BABY = 0.2F;
    private static final EntityDimensions BABY_DIMENSIONS = EntityType.PIGLIN.getDimensions().scale(0.5F).withEyeHeight(0.97F);
    private static final double PROBABILITY_OF_SPAWNING_WITH_CROSSBOW_INSTEAD_OF_SWORD = 0.5;
    private static final boolean DEFAULT_IS_BABY = false;
    private static final boolean DEFAULT_CANNOT_HUNT = false;
    private final SimpleContainer inventory = new SimpleContainer(8);
    private boolean cannotHunt = false;
    protected static final ImmutableList<SensorType<? extends Sensor<? super Piglin>>> SENSOR_TYPES = ImmutableList.of(
        SensorType.NEAREST_LIVING_ENTITIES, SensorType.NEAREST_PLAYERS, SensorType.NEAREST_ITEMS, SensorType.HURT_BY, SensorType.PIGLIN_SPECIFIC_SENSOR
    );
    protected static final ImmutableList<MemoryModuleType<?>> MEMORY_TYPES = ImmutableList.of(
        MemoryModuleType.LOOK_TARGET,
        MemoryModuleType.DOORS_TO_CLOSE,
        MemoryModuleType.NEAREST_LIVING_ENTITIES,
        MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES,
        MemoryModuleType.NEAREST_VISIBLE_PLAYER,
        MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER,
        MemoryModuleType.NEAREST_VISIBLE_ADULT_PIGLINS,
        MemoryModuleType.NEARBY_ADULT_PIGLINS,
        MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM,
        MemoryModuleType.ITEM_PICKUP_COOLDOWN_TICKS,
        MemoryModuleType.HURT_BY,
        MemoryModuleType.HURT_BY_ENTITY,
        MemoryModuleType.WALK_TARGET,
        MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE,
        MemoryModuleType.ATTACK_TARGET,
        MemoryModuleType.ATTACK_COOLING_DOWN,
        MemoryModuleType.INTERACTION_TARGET,
        MemoryModuleType.PATH,
        MemoryModuleType.ANGRY_AT,
        MemoryModuleType.UNIVERSAL_ANGER,
        MemoryModuleType.AVOID_TARGET,
        MemoryModuleType.ADMIRING_ITEM,
        MemoryModuleType.TIME_TRYING_TO_REACH_ADMIRE_ITEM,
        MemoryModuleType.ADMIRING_DISABLED,
        MemoryModuleType.DISABLE_WALK_TO_ADMIRE_ITEM,
        MemoryModuleType.CELEBRATE_LOCATION,
        MemoryModuleType.DANCING,
        MemoryModuleType.HUNTED_RECENTLY,
        MemoryModuleType.NEAREST_VISIBLE_BABY_HOGLIN,
        MemoryModuleType.NEAREST_VISIBLE_NEMESIS,
        MemoryModuleType.NEAREST_VISIBLE_ZOMBIFIED,
        MemoryModuleType.RIDE_TARGET,
        MemoryModuleType.VISIBLE_ADULT_PIGLIN_COUNT,
        MemoryModuleType.VISIBLE_ADULT_HOGLIN_COUNT,
        MemoryModuleType.NEAREST_VISIBLE_HUNTABLE_HOGLIN,
        MemoryModuleType.NEAREST_TARGETABLE_PLAYER_NOT_WEARING_GOLD,
        MemoryModuleType.NEAREST_PLAYER_HOLDING_WANTED_ITEM,
        MemoryModuleType.ATE_RECENTLY,
        MemoryModuleType.NEAREST_REPELLENT
    );

    public Piglin(EntityType<? extends AbstractPiglin> p_34683_, Level p_34684_) {
        super(p_34683_, p_34684_);
        this.xpReward = 5;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putBoolean("IsBaby", this.isBaby());
        pCompound.putBoolean("CannotHunt", this.cannotHunt);
        this.writeInventoryToTag(pCompound, this.registryAccess());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        this.setBaby(pCompound.getBooleanOr("IsBaby", false));
        this.setCannotHunt(pCompound.getBooleanOr("CannotHunt", false));
        this.readInventoryFromTag(pCompound, this.registryAccess());
    }

    @VisibleForDebug
    @Override
    public SimpleContainer getInventory() {
        return this.inventory;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel p_343074_, DamageSource p_34697_, boolean p_34699_) {
        super.dropCustomDeathLoot(p_343074_, p_34697_, p_34699_);
        if (p_34697_.getEntity() instanceof Creeper creeper && creeper.canDropMobsSkull()) {
            ItemStack itemstack = new ItemStack(Items.PIGLIN_HEAD);
            creeper.increaseDroppedSkulls();
            this.spawnAtLocation(p_343074_, itemstack);
        }

        this.inventory.removeAllItems().forEach(p_369899_ -> this.spawnAtLocation(p_343074_, p_369899_));
    }

    protected ItemStack addToInventory(ItemStack pStack) {
        return this.inventory.addItem(pStack);
    }

    protected boolean canAddToInventory(ItemStack pStack) {
        return this.inventory.canAddItem(pStack);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder p_330559_) {
        super.defineSynchedData(p_330559_);
        p_330559_.define(DATA_BABY_ID, false);
        p_330559_.define(DATA_IS_CHARGING_CROSSBOW, false);
        p_330559_.define(DATA_IS_DANCING, false);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> pKey) {
        super.onSyncedDataUpdated(pKey);
        if (DATA_BABY_ID.equals(pKey)) {
            this.refreshDimensions();
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 16.0).add(Attributes.MOVEMENT_SPEED, 0.35F).add(Attributes.ATTACK_DAMAGE, 5.0);
    }

    public static boolean checkPiglinSpawnRules(
        EntityType<Piglin> pEntityType, LevelAccessor pLevel, EntitySpawnReason pSpawnReason, BlockPos pPos, RandomSource pRandom
    ) {
        return !pLevel.getBlockState(pPos.below()).is(Blocks.NETHER_WART_BLOCK);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor p_34717_, DifficultyInstance p_34718_, EntitySpawnReason p_367290_, @Nullable SpawnGroupData p_34720_) {
        RandomSource randomsource = p_34717_.getRandom();
        if (p_367290_ != EntitySpawnReason.STRUCTURE) {
            if (randomsource.nextFloat() < 0.2F) {
                this.setBaby(true);
            } else if (this.isAdult()) {
                this.setItemSlot(EquipmentSlot.MAINHAND, this.createSpawnWeapon());
            }
        }

        PiglinAi.initMemories(this, p_34717_.getRandom());
        this.populateDefaultEquipmentSlots(randomsource, p_34718_);
        this.populateDefaultEquipmentEnchantments(p_34717_, randomsource, p_34718_);
        return super.finalizeSpawn(p_34717_, p_34718_, p_367290_, p_34720_);
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double pDistanceToClosestPlayer) {
        return !this.isPersistenceRequired();
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource p_219189_, DifficultyInstance p_219190_) {
        if (this.isAdult()) {
            this.maybeWearArmor(EquipmentSlot.HEAD, new ItemStack(Items.GOLDEN_HELMET), p_219189_);
            this.maybeWearArmor(EquipmentSlot.CHEST, new ItemStack(Items.GOLDEN_CHESTPLATE), p_219189_);
            this.maybeWearArmor(EquipmentSlot.LEGS, new ItemStack(Items.GOLDEN_LEGGINGS), p_219189_);
            this.maybeWearArmor(EquipmentSlot.FEET, new ItemStack(Items.GOLDEN_BOOTS), p_219189_);
        }
    }

    private void maybeWearArmor(EquipmentSlot pSlot, ItemStack pStack, RandomSource pRandom) {
        if (pRandom.nextFloat() < 0.1F) {
            this.setItemSlot(pSlot, pStack);
        }
    }

    @Override
    protected Brain.Provider<Piglin> brainProvider() {
        return Brain.provider(MEMORY_TYPES, SENSOR_TYPES);
    }

    @Override
    protected Brain<?> makeBrain(Dynamic<?> pDynamic) {
        return PiglinAi.makeBrain(this, this.brainProvider().makeBrain(pDynamic));
    }

    @Override
    public Brain<Piglin> getBrain() {
        return (Brain<Piglin>)super.getBrain();
    }

    @Override
    public InteractionResult mobInteract(Player pPlayer, InteractionHand pHand) {
        InteractionResult interactionresult = super.mobInteract(pPlayer, pHand);
        if (interactionresult.consumesAction()) {
            return interactionresult;
        } else if (this.level() instanceof ServerLevel serverlevel) {
            return PiglinAi.mobInteract(serverlevel, this, pPlayer, pHand);
        } else {
            boolean flag = PiglinAi.canAdmire(this, pPlayer.getItemInHand(pHand)) && this.getArmPose() != PiglinArmPose.ADMIRING_ITEM;
            return (InteractionResult)(flag ? InteractionResult.SUCCESS : InteractionResult.PASS);
        }
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose p_330522_) {
        return this.isBaby() ? BABY_DIMENSIONS : super.getDefaultDimensions(p_330522_);
    }

    @Override
    public void setBaby(boolean pChildZombie) {
        this.getEntityData().set(DATA_BABY_ID, pChildZombie);
        if (!this.level().isClientSide) {
            AttributeInstance attributeinstance = this.getAttribute(Attributes.MOVEMENT_SPEED);
            attributeinstance.removeModifier(SPEED_MODIFIER_BABY.id());
            if (pChildZombie) {
                attributeinstance.addTransientModifier(SPEED_MODIFIER_BABY);
            }
        }
    }

    @Override
    public boolean isBaby() {
        return this.getEntityData().get(DATA_BABY_ID);
    }

    private void setCannotHunt(boolean pCannotHunt) {
        this.cannotHunt = pCannotHunt;
    }

    @Override
    protected boolean canHunt() {
        return !this.cannotHunt;
    }

    @Override
    protected void customServerAiStep(ServerLevel p_367240_) {
        ProfilerFiller profilerfiller = Profiler.get();
        profilerfiller.push("piglinBrain");
        this.getBrain().tick(p_367240_, this);
        profilerfiller.pop();
        PiglinAi.updateActivity(this);
        super.customServerAiStep(p_367240_);
    }

    @Override
    protected int getBaseExperienceReward(ServerLevel p_368633_) {
        return this.xpReward;
    }

    @Override
    protected void finishConversion(ServerLevel p_34756_) {
        PiglinAi.cancelAdmiring(p_34756_, this);
        this.inventory.removeAllItems().forEach(p_362445_ -> this.spawnAtLocation(p_34756_, p_362445_));
        super.finishConversion(p_34756_);
    }

    private ItemStack createSpawnWeapon() {
        return this.random.nextFloat() < 0.5 ? new ItemStack(Items.CROSSBOW) : new ItemStack(Items.GOLDEN_SWORD);
    }

    @Nullable
    @Override
    public TagKey<Item> getPreferredWeaponType() {
        return this.isBaby() ? null : ItemTags.PIGLIN_PREFERRED_WEAPONS;
    }

    private boolean isChargingCrossbow() {
        return this.entityData.get(DATA_IS_CHARGING_CROSSBOW);
    }

    @Override
    public void setChargingCrossbow(boolean pIsCharging) {
        this.entityData.set(DATA_IS_CHARGING_CROSSBOW, pIsCharging);
    }

    @Override
    public void onCrossbowAttackPerformed() {
        this.noActionTime = 0;
    }

    @Override
    public PiglinArmPose getArmPose() {
        if (this.isDancing()) {
            return PiglinArmPose.DANCING;
        } else if (PiglinAi.isLovedItem(this.getOffhandItem())) {
            return PiglinArmPose.ADMIRING_ITEM;
        } else if (this.isAggressive() && this.isHoldingMeleeWeapon()) {
            return PiglinArmPose.ATTACKING_WITH_MELEE_WEAPON;
        } else if (this.isChargingCrossbow()) {
            return PiglinArmPose.CROSSBOW_CHARGE;
        } else {
            return this.isHolding(is -> is.getItem() instanceof net.minecraft.world.item.CrossbowItem) && CrossbowItem.isCharged(this.getWeaponItem()) ? PiglinArmPose.CROSSBOW_HOLD : PiglinArmPose.DEFAULT;
        }
    }

    public boolean isDancing() {
        return this.entityData.get(DATA_IS_DANCING);
    }

    public void setDancing(boolean pDancing) {
        this.entityData.set(DATA_IS_DANCING, pDancing);
    }

    @Override
    public boolean hurtServer(ServerLevel p_361296_, DamageSource p_368744_, float p_364157_) {
        boolean flag = super.hurtServer(p_361296_, p_368744_, p_364157_);
        if (flag && p_368744_.getEntity() instanceof LivingEntity livingentity) {
            PiglinAi.wasHurtBy(p_361296_, this, livingentity);
        }

        return flag;
    }

    @Override
    public void performRangedAttack(LivingEntity pTarget, float pDistanceFactor) {
        this.performCrossbowAttack(this, 1.6F);
    }

    @Override
    public boolean canFireProjectileWeapon(ProjectileWeaponItem p_34715_) {
        return p_34715_ == Items.CROSSBOW;
    }

    protected void holdInMainHand(ItemStack pStack) {
        this.setItemSlotAndDropWhenKilled(EquipmentSlot.MAINHAND, pStack);
    }

    protected void holdInOffHand(ItemStack pStack) {
        if (pStack.isPiglinCurrency()) {
            this.setItemSlot(EquipmentSlot.OFFHAND, pStack);
            this.setGuaranteedDrop(EquipmentSlot.OFFHAND);
        } else {
            this.setItemSlotAndDropWhenKilled(EquipmentSlot.OFFHAND, pStack);
        }
    }

    @Override
    public boolean wantsToPickUp(ServerLevel p_369934_, ItemStack p_34777_) {
        return net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(p_369934_, this) && this.canPickUpLoot() && PiglinAi.wantsToPickup(this, p_34777_);
    }

    protected boolean canReplaceCurrentItem(ItemStack pCandidate) {
        EquipmentSlot equipmentslot = this.getEquipmentSlotForItem(pCandidate);
        ItemStack itemstack = this.getItemBySlot(equipmentslot);
        return this.canReplaceCurrentItem(pCandidate, itemstack, equipmentslot);
    }

    @Override
    protected boolean canReplaceCurrentItem(ItemStack p_34712_, ItemStack p_34713_, EquipmentSlot p_364626_) {
        if (EnchantmentHelper.has(p_34713_, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)) {
            return false;
        } else {
            TagKey<Item> tagkey = this.getPreferredWeaponType();
            boolean flag = PiglinAi.isLovedItem(p_34712_) || tagkey != null && p_34712_.is(tagkey);
            boolean flag1 = PiglinAi.isLovedItem(p_34713_) || tagkey != null && p_34713_.is(tagkey);
            if (flag && !flag1) {
                return true;
            } else {
                return !flag && flag1 ? false : super.canReplaceCurrentItem(p_34712_, p_34713_, p_364626_);
            }
        }
    }

    @Override
    protected void pickUpItem(ServerLevel p_365764_, ItemEntity p_34743_) {
        this.onItemPickup(p_34743_);
        PiglinAi.pickUpItem(p_365764_, this, p_34743_);
    }

    @Override
    public boolean startRiding(Entity pEntity, boolean pForce) {
        if (this.isBaby() && pEntity.getType() == EntityType.HOGLIN) {
            pEntity = this.getTopPassenger(pEntity, 3);
        }

        return super.startRiding(pEntity, pForce);
    }

    private Entity getTopPassenger(Entity pVehicle, int pMaxPosition) {
        List<Entity> list = pVehicle.getPassengers();
        return pMaxPosition != 1 && !list.isEmpty() ? this.getTopPassenger(list.getFirst(), pMaxPosition - 1) : pVehicle;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return this.level().isClientSide ? null : PiglinAi.getSoundForCurrentActivity(this).orElse(null);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource pDamageSource) {
        return SoundEvents.PIGLIN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PIGLIN_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pPos, BlockState pBlock) {
        this.playSound(SoundEvents.PIGLIN_STEP, 0.15F, 1.0F);
    }

    @Override
    protected void playConvertedSound() {
        this.makeSound(SoundEvents.PIGLIN_CONVERTED_TO_ZOMBIFIED);
    }
}
