package net.minecraft.world.entity.animal.wolf;

import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Crackiness;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.BegGoal;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NonTameRandomTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.ai.goal.target.ResetUniversalAngerTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Turtle;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Llama;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.variant.SpawnContext;
import net.minecraft.world.entity.variant.VariantUtils;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;

public class Wolf extends TamableAnimal implements NeutralMob, net.minecraftforge.common.IForgeShearable {
    private static final EntityDataAccessor<Boolean> DATA_INTERESTED_ID = SynchedEntityData.defineId(Wolf.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_COLLAR_COLOR = SynchedEntityData.defineId(Wolf.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_REMAINING_ANGER_TIME = SynchedEntityData.defineId(Wolf.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Holder<WolfVariant>> DATA_VARIANT_ID = SynchedEntityData.defineId(Wolf.class, EntityDataSerializers.WOLF_VARIANT);
    private static final EntityDataAccessor<Holder<WolfSoundVariant>> DATA_SOUND_VARIANT_ID = SynchedEntityData.defineId(Wolf.class, EntityDataSerializers.WOLF_SOUND_VARIANT);
    public static final TargetingConditions.Selector PREY_SELECTOR = (p_395238_, p_395683_) -> {
        EntityType<?> entitytype = p_395238_.getType();
        return entitytype == EntityType.SHEEP || entitytype == EntityType.RABBIT || entitytype == EntityType.FOX;
    };
    private static final float START_HEALTH = 8.0F;
    private static final float TAME_HEALTH = 40.0F;
    private static final float ARMOR_REPAIR_UNIT = 0.125F;
    public static final float DEFAULT_TAIL_ANGLE = (float) (Math.PI / 5);
    private static final DyeColor DEFAULT_COLLAR_COLOR = DyeColor.RED;
    private float interestedAngle;
    private float interestedAngleO;
    private boolean isWet;
    private boolean isShaking;
    private float shakeAnim;
    private float shakeAnimO;
    private static final UniformInt PERSISTENT_ANGER_TIME = TimeUtil.rangeOfSeconds(20, 39);
    @Nullable
    private UUID persistentAngerTarget;

    public Wolf(EntityType<? extends Wolf> p_391992_, Level p_395120_) {
        super(p_391992_, p_395120_);
        this.setTame(false, false);
        this.setPathfindingMalus(PathType.POWDER_SNOW, -1.0F);
        this.setPathfindingMalus(PathType.DANGER_POWDER_SNOW, -1.0F);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(1, new TamableAnimal.TamableAnimalPanicGoal(1.5, DamageTypeTags.PANIC_ENVIRONMENTAL_CAUSES));
        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(3, new Wolf.WolfAvoidEntityGoal<>(this, Llama.class, 24.0F, 1.5, 1.5));
        this.goalSelector.addGoal(4, new LeapAtTargetGoal(this, 0.4F));
        this.goalSelector.addGoal(5, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(6, new FollowOwnerGoal(this, 1.0, 10.0F, 2.0F));
        this.goalSelector.addGoal(7, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(9, new BegGoal(this, 8.0F));
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this).setAlertOthers());
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::isAngryAt));
        this.targetSelector.addGoal(5, new NonTameRandomTargetGoal<>(this, Animal.class, false, PREY_SELECTOR));
        this.targetSelector.addGoal(6, new NonTameRandomTargetGoal<>(this, Turtle.class, false, Turtle.BABY_ON_LAND_SELECTOR));
        this.targetSelector.addGoal(7, new NearestAttackableTargetGoal<>(this, AbstractSkeleton.class, false));
        this.targetSelector.addGoal(8, new ResetUniversalAngerTargetGoal<>(this, true));
    }

    public ResourceLocation getTexture() {
        WolfVariant wolfvariant = this.getVariant().value();
        if (this.isTame()) {
            return wolfvariant.assetInfo().tame().texturePath();
        } else {
            return this.isAngry() ? wolfvariant.assetInfo().angry().texturePath() : wolfvariant.assetInfo().wild().texturePath();
        }
    }

    private Holder<WolfVariant> getVariant() {
        return this.entityData.get(DATA_VARIANT_ID);
    }

    private void setVariant(Holder<WolfVariant> pVariant) {
        this.entityData.set(DATA_VARIANT_ID, pVariant);
    }

    private Holder<WolfSoundVariant> getSoundVariant() {
        return this.entityData.get(DATA_SOUND_VARIANT_ID);
    }

    private void setSoundVariant(Holder<WolfSoundVariant> pSoundVariant) {
        this.entityData.set(DATA_SOUND_VARIANT_ID, pSoundVariant);
    }

    @Nullable
    @Override
    public <T> T get(DataComponentType<? extends T> p_394251_) {
        if (p_394251_ == DataComponents.WOLF_VARIANT) {
            return castComponentValue((DataComponentType<T>)p_394251_, this.getVariant());
        } else if (p_394251_ == DataComponents.WOLF_SOUND_VARIANT) {
            return castComponentValue((DataComponentType<T>)p_394251_, this.getSoundVariant());
        } else {
            return p_394251_ == DataComponents.WOLF_COLLAR ? castComponentValue((DataComponentType<T>)p_394251_, this.getCollarColor()) : super.get(p_394251_);
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter p_391752_) {
        this.applyImplicitComponentIfPresent(p_391752_, DataComponents.WOLF_VARIANT);
        this.applyImplicitComponentIfPresent(p_391752_, DataComponents.WOLF_SOUND_VARIANT);
        this.applyImplicitComponentIfPresent(p_391752_, DataComponents.WOLF_COLLAR);
        super.applyImplicitComponents(p_391752_);
    }

    @Override
    protected <T> boolean applyImplicitComponent(DataComponentType<T> p_396557_, T p_395330_) {
        if (p_396557_ == DataComponents.WOLF_VARIANT) {
            this.setVariant(castComponentValue(DataComponents.WOLF_VARIANT, p_395330_));
            return true;
        } else if (p_396557_ == DataComponents.WOLF_SOUND_VARIANT) {
            this.setSoundVariant(castComponentValue(DataComponents.WOLF_SOUND_VARIANT, p_395330_));
            return true;
        } else if (p_396557_ == DataComponents.WOLF_COLLAR) {
            this.setCollarColor(castComponentValue(DataComponents.WOLF_COLLAR, p_395330_));
            return true;
        } else {
            return super.applyImplicitComponent(p_396557_, p_395330_);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes().add(Attributes.MOVEMENT_SPEED, 0.3F).add(Attributes.MAX_HEALTH, 8.0).add(Attributes.ATTACK_DAMAGE, 4.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder p_394106_) {
        super.defineSynchedData(p_394106_);
        Registry<WolfSoundVariant> registry = this.registryAccess().lookupOrThrow(Registries.WOLF_SOUND_VARIANT);
        p_394106_.define(DATA_VARIANT_ID, VariantUtils.getDefaultOrAny(this.registryAccess(), WolfVariants.DEFAULT));
        p_394106_.define(DATA_SOUND_VARIANT_ID, registry.get(WolfSoundVariants.CLASSIC).or(registry::getAny).orElseThrow());
        p_394106_.define(DATA_INTERESTED_ID, false);
        p_394106_.define(DATA_COLLAR_COLOR, DEFAULT_COLLAR_COLOR.getId());
        p_394106_.define(DATA_REMAINING_ANGER_TIME, 0);
    }

    @Override
    protected void playStepSound(BlockPos p_394772_, BlockState p_395010_) {
        this.playSound(SoundEvents.WOLF_STEP, 0.15F, 1.0F);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag p_391862_) {
        super.addAdditionalSaveData(p_391862_);
        p_391862_.store("CollarColor", DyeColor.LEGACY_ID_CODEC, this.getCollarColor());
        VariantUtils.writeVariant(p_391862_, this.getVariant());
        this.addPersistentAngerSaveData(p_391862_);
        this.getSoundVariant()
            .unwrapKey()
            .ifPresent(p_395012_ -> p_391862_.store("sound_variant", ResourceKey.codec(Registries.WOLF_SOUND_VARIANT), (ResourceKey<WolfSoundVariant>)p_395012_));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag p_391903_) {
        super.readAdditionalSaveData(p_391903_);
        VariantUtils.readVariant(p_391903_, this.registryAccess(), Registries.WOLF_VARIANT).ifPresent(this::setVariant);
        this.setCollarColor(p_391903_.read("CollarColor", DyeColor.LEGACY_ID_CODEC).orElse(DEFAULT_COLLAR_COLOR));
        this.readPersistentAngerSaveData(this.level(), p_391903_);
        p_391903_.read("sound_variant", ResourceKey.codec(Registries.WOLF_SOUND_VARIANT))
            .flatMap(p_393179_ -> this.registryAccess().lookupOrThrow(Registries.WOLF_SOUND_VARIANT).get((ResourceKey<WolfSoundVariant>)p_393179_))
            .ifPresent(this::setSoundVariant);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor p_396732_, DifficultyInstance p_393098_, EntitySpawnReason p_396359_, @Nullable SpawnGroupData p_397026_) {
        if (p_397026_ instanceof Wolf.WolfPackData wolf$wolfpackdata) {
            this.setVariant(wolf$wolfpackdata.type);
        } else {
            Optional<? extends Holder<WolfVariant>> optional = WolfVariants.selectVariantToSpawn(
                this.random, this.registryAccess(), SpawnContext.create(p_396732_, this.blockPosition())
            );
            if (optional.isPresent()) {
                this.setVariant((Holder<WolfVariant>)optional.get());
                p_397026_ = new Wolf.WolfPackData((Holder<WolfVariant>)optional.get());
            }
        }

        this.setSoundVariant(WolfSoundVariants.pickRandomSoundVariant(this.registryAccess(), this.random));
        return super.finalizeSpawn(p_396732_, p_393098_, p_396359_, p_397026_);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        if (this.isAngry()) {
            return this.getSoundVariant().value().growlSound().value();
        } else if (this.random.nextInt(3) == 0) {
            return this.isTame() && this.getHealth() < 20.0F
                ? this.getSoundVariant().value().whineSound().value()
                : this.getSoundVariant().value().pantSound().value();
        } else {
            return this.getSoundVariant().value().ambientSound().value();
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource p_395818_) {
        return this.canArmorAbsorb(p_395818_) ? SoundEvents.WOLF_ARMOR_DAMAGE : this.getSoundVariant().value().hurtSound().value();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return this.getSoundVariant().value().deathSound().value();
    }

    @Override
    protected float getSoundVolume() {
        return 0.4F;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide && this.isWet && !this.isShaking && !this.isPathFinding() && this.onGround()) {
            this.isShaking = true;
            this.shakeAnim = 0.0F;
            this.shakeAnimO = 0.0F;
            this.level().broadcastEntityEvent(this, (byte)8);
        }

        if (!this.level().isClientSide) {
            this.updatePersistentAnger((ServerLevel)this.level(), true);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.isAlive()) {
            this.interestedAngleO = this.interestedAngle;
            if (this.isInterested()) {
                this.interestedAngle = this.interestedAngle + (1.0F - this.interestedAngle) * 0.4F;
            } else {
                this.interestedAngle = this.interestedAngle + (0.0F - this.interestedAngle) * 0.4F;
            }

            if (this.isInWaterOrRain()) {
                this.isWet = true;
                if (this.isShaking && !this.level().isClientSide) {
                    this.level().broadcastEntityEvent(this, (byte)56);
                    this.cancelShake();
                }
            } else if ((this.isWet || this.isShaking) && this.isShaking) {
                if (this.shakeAnim == 0.0F) {
                    this.playSound(SoundEvents.WOLF_SHAKE, this.getSoundVolume(), (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
                    this.gameEvent(GameEvent.ENTITY_ACTION);
                }

                this.shakeAnimO = this.shakeAnim;
                this.shakeAnim += 0.05F;
                if (this.shakeAnimO >= 2.0F) {
                    this.isWet = false;
                    this.isShaking = false;
                    this.shakeAnimO = 0.0F;
                    this.shakeAnim = 0.0F;
                }

                if (this.shakeAnim > 0.4F) {
                    float f = (float)this.getY();
                    int i = (int)(Mth.sin((this.shakeAnim - 0.4F) * (float) Math.PI) * 7.0F);
                    Vec3 vec3 = this.getDeltaMovement();

                    for (int j = 0; j < i; j++) {
                        float f1 = (this.random.nextFloat() * 2.0F - 1.0F) * this.getBbWidth() * 0.5F;
                        float f2 = (this.random.nextFloat() * 2.0F - 1.0F) * this.getBbWidth() * 0.5F;
                        this.level()
                            .addParticle(ParticleTypes.SPLASH, this.getX() + f1, f + 0.8F, this.getZ() + f2, vec3.x, vec3.y, vec3.z);
                    }
                }
            }
        }
    }

    private void cancelShake() {
        this.isShaking = false;
        this.shakeAnim = 0.0F;
        this.shakeAnimO = 0.0F;
    }

    @Override
    public void die(DamageSource p_392806_) {
        this.isWet = false;
        this.isShaking = false;
        this.shakeAnimO = 0.0F;
        this.shakeAnim = 0.0F;
        super.die(p_392806_);
    }

    public float getWetShade(float pPartialTick) {
        return !this.isWet ? 1.0F : Math.min(0.75F + Mth.lerp(pPartialTick, this.shakeAnimO, this.shakeAnim) / 2.0F * 0.25F, 1.0F);
    }

    public float getShakeAnim(float pPartialTick) {
        return Mth.lerp(pPartialTick, this.shakeAnimO, this.shakeAnim);
    }

    public float getHeadRollAngle(float pPartialTick) {
        return Mth.lerp(pPartialTick, this.interestedAngleO, this.interestedAngle) * 0.15F * (float) Math.PI;
    }

    @Override
    public int getMaxHeadXRot() {
        return this.isInSittingPose() ? 20 : super.getMaxHeadXRot();
    }

    @Override
    public boolean hurtServer(ServerLevel p_395568_, DamageSource p_396024_, float p_394833_) {
        if (this.isInvulnerableTo(p_395568_, p_396024_)) {
            return false;
        } else {
            this.setOrderedToSit(false);
            return super.hurtServer(p_395568_, p_396024_, p_394833_);
        }
    }

    @Override
    protected void actuallyHurt(ServerLevel p_395118_, DamageSource p_393775_, float p_396523_) {
        if (!this.canArmorAbsorb(p_393775_)) {
            super.actuallyHurt(p_395118_, p_393775_, p_396523_);
        } else {
            ItemStack itemstack = this.getBodyArmorItem();
            int i = itemstack.getDamageValue();
            int j = itemstack.getMaxDamage();
            itemstack.hurtAndBreak(Mth.ceil(p_396523_), this, EquipmentSlot.BODY);
            if (Crackiness.WOLF_ARMOR.byDamage(i, j) != Crackiness.WOLF_ARMOR.byDamage(this.getBodyArmorItem())) {
                this.playSound(SoundEvents.WOLF_ARMOR_CRACK);
                p_395118_.sendParticles(
                    new ItemParticleOption(ParticleTypes.ITEM, Items.ARMADILLO_SCUTE.getDefaultInstance()),
                    this.getX(),
                    this.getY() + 1.0,
                    this.getZ(),
                    20,
                    0.2,
                    0.1,
                    0.2,
                    0.1
                );
            }
        }
    }

    private boolean canArmorAbsorb(DamageSource pDamageSource) {
        return this.getBodyArmorItem().is(Items.WOLF_ARMOR) && !pDamageSource.is(DamageTypeTags.BYPASSES_WOLF_ARMOR);
    }

    @Override
    protected void applyTamingSideEffects() {
        if (this.isTame()) {
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(40.0);
            this.setHealth(40.0F);
        } else {
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(8.0);
        }
    }

    @Override
    protected void hurtArmor(DamageSource p_395075_, float p_391183_) {
        this.doHurtEquipment(p_395075_, p_391183_, new EquipmentSlot[]{EquipmentSlot.BODY});
    }

    @Override
    public InteractionResult mobInteract(Player p_391202_, InteractionHand p_391687_) {
        ItemStack itemstack = p_391202_.getItemInHand(p_391687_);
        Item item = itemstack.getItem();
        if (this.isTame()) {
            if (this.isFood(itemstack) && this.getHealth() < this.getMaxHealth()) {
                this.usePlayerItem(p_391202_, p_391687_, itemstack);
                FoodProperties foodproperties = itemstack.get(DataComponents.FOOD);
                float f = foodproperties != null ? foodproperties.nutrition() : 1.0F;
                this.heal(2.0F * f);
                return InteractionResult.SUCCESS;
            }

            if (!(item instanceof DyeItem dyeitem && this.isOwnedBy(p_391202_))) {
                if (this.isEquippableInSlot(itemstack, EquipmentSlot.BODY) && !this.isWearingBodyArmor() && this.isOwnedBy(p_391202_) && !this.isBaby()) {
                    this.setBodyArmorItem(itemstack.copyWithCount(1));
                    itemstack.consume(1, p_391202_);
                    return InteractionResult.SUCCESS;
                }

                if (!itemstack.canPerformAction(net.minecraftforge.common.ToolActions.SHEARS_HARVEST)
                    || !this.isOwnedBy(p_391202_)
                    || !this.isWearingBodyArmor()
                    || EnchantmentHelper.has(this.getBodyArmorItem(), EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE) && !p_391202_.isCreative()) {
                    if (this.isInSittingPose() && this.isWearingBodyArmor() && this.isOwnedBy(p_391202_) && this.getBodyArmorItem().isDamaged() && this.getBodyArmorItem().isValidRepairItem(itemstack)
                        )
                     {
                        itemstack.shrink(1);
                        this.playSound(SoundEvents.WOLF_ARMOR_REPAIR);
                        ItemStack itemstack2 = this.getBodyArmorItem();
                        int i = (int)(itemstack2.getMaxDamage() * 0.125F);
                        itemstack2.setDamageValue(Math.max(0, itemstack2.getDamageValue() - i));
                        return InteractionResult.SUCCESS;
                    }

                    InteractionResult interactionresult = super.mobInteract(p_391202_, p_391687_);
                    if (!interactionresult.consumesAction() && this.isOwnedBy(p_391202_)) {
                        this.setOrderedToSit(!this.isOrderedToSit());
                        this.jumping = false;
                        this.navigation.stop();
                        this.setTarget(null);
                        return InteractionResult.SUCCESS.withoutItem();
                    }

                    return interactionresult;
                }

                // Forge: moved to onSheared
                if (true) return InteractionResult.PASS;
                itemstack.hurtAndBreak(1, p_391202_, getSlotForHand(p_391687_));
                this.playSound(SoundEvents.ARMOR_UNEQUIP_WOLF);
                ItemStack itemstack1 = this.getBodyArmorItem();
                this.setBodyArmorItem(ItemStack.EMPTY);
                if (this.level() instanceof ServerLevel serverlevel) {
                    this.spawnAtLocation(serverlevel, itemstack1);
                }

                return InteractionResult.SUCCESS;
            }

            DyeColor dyecolor = dyeitem.getDyeColor();
            if (dyecolor != this.getCollarColor()) {
                this.setCollarColor(dyecolor);
                itemstack.consume(1, p_391202_);
                return InteractionResult.SUCCESS;
            }
        } else if (!this.level().isClientSide && itemstack.is(Items.BONE) && !this.isAngry()) {
            itemstack.consume(1, p_391202_);
            this.tryToTame(p_391202_);
            return InteractionResult.SUCCESS_SERVER;
        }

        return super.mobInteract(p_391202_, p_391687_);
    }

    private void tryToTame(Player pPlayer) {
        if (this.random.nextInt(3) == 0 && !net.minecraftforge.event.ForgeEventFactory.onAnimalTame(this, pPlayer)) {
            this.tame(pPlayer);
            this.navigation.stop();
            this.setTarget(null);
            this.setOrderedToSit(true);
            this.level().broadcastEntityEvent(this, (byte)7);
        } else {
            this.level().broadcastEntityEvent(this, (byte)6);
        }
    }

    @Override
    public void handleEntityEvent(byte p_391571_) {
        if (p_391571_ == 8) {
            this.isShaking = true;
            this.shakeAnim = 0.0F;
            this.shakeAnimO = 0.0F;
        } else if (p_391571_ == 56) {
            this.cancelShake();
        } else {
            super.handleEntityEvent(p_391571_);
        }
    }

    public float getTailAngle() {
        if (this.isAngry()) {
            return 1.5393804F;
        } else if (this.isTame()) {
            float f = this.getMaxHealth();
            float f1 = (f - this.getHealth()) / f;
            return (0.55F - f1 * 0.4F) * (float) Math.PI;
        } else {
            return (float) (Math.PI / 5);
        }
    }

    @Override
    public boolean isFood(ItemStack p_393879_) {
        return p_393879_.is(ItemTags.WOLF_FOOD);
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 8;
    }

    @Override
    public int getRemainingPersistentAngerTime() {
        return this.entityData.get(DATA_REMAINING_ANGER_TIME);
    }

    @Override
    public void setRemainingPersistentAngerTime(int p_396897_) {
        this.entityData.set(DATA_REMAINING_ANGER_TIME, p_396897_);
    }

    @Override
    public void startPersistentAngerTimer() {
        this.setRemainingPersistentAngerTime(PERSISTENT_ANGER_TIME.sample(this.random));
    }

    @Nullable
    @Override
    public UUID getPersistentAngerTarget() {
        return this.persistentAngerTarget;
    }

    @Override
    public void setPersistentAngerTarget(@Nullable UUID p_393236_) {
        this.persistentAngerTarget = p_393236_;
    }

    public DyeColor getCollarColor() {
        return DyeColor.byId(this.entityData.get(DATA_COLLAR_COLOR));
    }

    private void setCollarColor(DyeColor pColor) {
        this.entityData.set(DATA_COLLAR_COLOR, pColor.getId());
    }

    @Nullable
    public Wolf getBreedOffspring(ServerLevel p_394063_, AgeableMob p_393185_) {
        Wolf wolf = EntityType.WOLF.create(p_394063_, EntitySpawnReason.BREEDING);
        if (wolf != null && p_393185_ instanceof Wolf wolf1) {
            if (this.random.nextBoolean()) {
                wolf.setVariant(this.getVariant());
            } else {
                wolf.setVariant(wolf1.getVariant());
            }

            if (this.isTame()) {
                wolf.setOwnerReference(this.getOwnerReference());
                wolf.setTame(true, true);
                DyeColor dyecolor = this.getCollarColor();
                DyeColor dyecolor1 = wolf1.getCollarColor();
                wolf.setCollarColor(DyeColor.getMixedColor(p_394063_, dyecolor, dyecolor1));
            }

            wolf.setSoundVariant(WolfSoundVariants.pickRandomSoundVariant(this.registryAccess(), this.random));
        }

        return wolf;
    }

    public void setIsInterested(boolean pIsInterested) {
        this.entityData.set(DATA_INTERESTED_ID, pIsInterested);
    }

    @Override
    public boolean canMate(Animal p_394472_) {
        if (p_394472_ == this) {
            return false;
        } else if (!this.isTame()) {
            return false;
        } else if (!(p_394472_ instanceof Wolf wolf)) {
            return false;
        } else if (!wolf.isTame()) {
            return false;
        } else {
            return wolf.isInSittingPose() ? false : this.isInLove() && wolf.isInLove();
        }
    }

    public boolean isInterested() {
        return this.entityData.get(DATA_INTERESTED_ID);
    }

    @Override
    public boolean wantsToAttack(LivingEntity p_395043_, LivingEntity p_394450_) {
        if (p_395043_ instanceof Creeper || p_395043_ instanceof Ghast || p_395043_ instanceof ArmorStand) {
            return false;
        } else if (p_395043_ instanceof Wolf wolf) {
            return !wolf.isTame() || wolf.getOwner() != p_394450_;
        } else if (p_395043_ instanceof Player player && p_394450_ instanceof Player player1 && !player1.canHarmPlayer(player)) {
            return false;
        } else {
            return p_395043_ instanceof AbstractHorse abstracthorse && abstracthorse.isTamed()
                ? false
                : !(p_395043_ instanceof TamableAnimal tamableanimal && tamableanimal.isTame());
        }
    }

    @Override
    public boolean canBeLeashed() {
        return !this.isAngry();
    }

    @Override
    public Vec3 getLeashOffset() {
        return new Vec3(0.0, 0.6F * this.getEyeHeight(), this.getBbWidth() * 0.4F);
    }

    public static boolean checkWolfSpawnRules(
        EntityType<Wolf> pEntityType, LevelAccessor pLevel, EntitySpawnReason pSpawnReason, BlockPos pPos, RandomSource pRandom
    ) {
        return pLevel.getBlockState(pPos.below()).is(BlockTags.WOLVES_SPAWNABLE_ON) && isBrightEnoughToSpawn(pLevel, pPos);
    }

    class WolfAvoidEntityGoal<T extends LivingEntity> extends AvoidEntityGoal<T> {
        private final Wolf wolf;

        public WolfAvoidEntityGoal(final Wolf pWolf, final Class<T> pAvoidClass, final float pMaxDistance, final double pWalkSpeedModifier, final double pSprintSpeedModifier) {
            super(pWolf, pAvoidClass, pMaxDistance, pWalkSpeedModifier, pSprintSpeedModifier);
            this.wolf = pWolf;
        }

        @Override
        public boolean canUse() {
            return super.canUse() && this.toAvoid instanceof Llama ? !this.wolf.isTame() && this.avoidLlama((Llama)this.toAvoid) : false;
        }

        private boolean avoidLlama(Llama pLlama) {
            return pLlama.getStrength() >= Wolf.this.random.nextInt(5);
        }

        @Override
        public void start() {
            Wolf.this.setTarget(null);
            super.start();
        }

        @Override
        public void tick() {
            Wolf.this.setTarget(null);
            super.tick();
        }
    }

    public static class WolfPackData extends AgeableMob.AgeableMobGroupData {
        public final Holder<WolfVariant> type;

        public WolfPackData(Holder<WolfVariant> pType) {
            super(false);
            this.type = pType;
        }
    }

    @Override
    public boolean isShearable(ItemStack item, Level world, BlockPos pos) {
        return this.isWearingBodyArmor();
    }

    @Override
    public java.util.List<ItemStack> onSheared(@org.jetbrains.annotations.Nullable Player player, @org.jetbrains.annotations.NotNull ItemStack item, Level world, BlockPos pos, int fortune) {
        if (!this.isWearingBodyArmor() || (player != null && !this.isOwnedBy(player)) ||
            (EnchantmentHelper.has(this.getBodyArmorItem(), EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE) && (player == null || !player.isCreative()))
        )
            return java.util.Collections.emptyList();

        this.playSound(SoundEvents.ARMOR_UNEQUIP_WOLF);
        var armor = this.getBodyArmorItem();
        this.setBodyArmorItem(ItemStack.EMPTY);
        return java.util.List.of(armor);
    }
}
