package net.minecraft.world.entity.animal;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ItemBasedSteering;
import net.minecraft.world.entity.ItemSteerable;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.variant.SpawnContext;
import net.minecraft.world.entity.variant.VariantUtils;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class Pig extends Animal implements ItemSteerable {
    private static final EntityDataAccessor<Integer> DATA_BOOST_TIME = SynchedEntityData.defineId(Pig.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Holder<PigVariant>> DATA_VARIANT_ID = SynchedEntityData.defineId(Pig.class, EntityDataSerializers.PIG_VARIANT);
    private final ItemBasedSteering steering = new ItemBasedSteering(this.entityData, DATA_BOOST_TIME);

    public Pig(EntityType<? extends Pig> p_29462_, Level p_29463_) {
        super(p_29462_, p_29463_);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.25));
        this.goalSelector.addGoal(3, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(4, new TemptGoal(this, 1.2, p_332330_ -> p_332330_.is(Items.CARROT_ON_A_STICK), false));
        this.goalSelector.addGoal(4, new TemptGoal(this, 1.2, p_332514_ -> p_332514_.is(ItemTags.PIG_FOOD), false));
        this.goalSelector.addGoal(5, new FollowParentGoal(this, 1.1));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes().add(Attributes.MAX_HEALTH, 10.0).add(Attributes.MOVEMENT_SPEED, 0.25);
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return (LivingEntity)(this.isSaddled() && this.getFirstPassenger() instanceof Player player && player.isHolding(Items.CARROT_ON_A_STICK) ? player : super.getControllingPassenger());
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> pKey) {
        if (DATA_BOOST_TIME.equals(pKey) && this.level().isClientSide) {
            this.steering.onSynced();
        }

        super.onSyncedDataUpdated(pKey);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder p_335143_) {
        super.defineSynchedData(p_335143_);
        p_335143_.define(DATA_BOOST_TIME, 0);
        p_335143_.define(DATA_VARIANT_ID, VariantUtils.getDefaultOrAny(this.registryAccess(), PigVariants.DEFAULT));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        VariantUtils.writeVariant(pCompound, this.getVariant());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        VariantUtils.readVariant(pCompound, this.registryAccess(), Registries.PIG_VARIANT).ifPresent(this::setVariant);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PIG_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource pDamageSource) {
        return SoundEvents.PIG_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PIG_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pPos, BlockState pBlock) {
        this.playSound(SoundEvents.PIG_STEP, 0.15F, 1.0F);
    }

    @Override
    public InteractionResult mobInteract(Player pPlayer, InteractionHand pHand) {
        boolean flag = this.isFood(pPlayer.getItemInHand(pHand));
        if (!flag && this.isSaddled() && !this.isVehicle() && !pPlayer.isSecondaryUseActive()) {
            if (!this.level().isClientSide) {
                pPlayer.startRiding(this);
            }

            return InteractionResult.SUCCESS;
        } else {
            InteractionResult interactionresult = super.mobInteract(pPlayer, pHand);
            if (!interactionresult.consumesAction()) {
                ItemStack itemstack = pPlayer.getItemInHand(pHand);
                return (InteractionResult)(this.isEquippableInSlot(itemstack, EquipmentSlot.SADDLE)
                    ? itemstack.interactLivingEntity(pPlayer, this, pHand)
                    : InteractionResult.PASS);
            } else {
                return interactionresult;
            }
        }
    }

    @Override
    public boolean canUseSlot(EquipmentSlot p_394296_) {
        return p_394296_ != EquipmentSlot.SADDLE ? super.canUseSlot(p_394296_) : this.isAlive() && !this.isBaby();
    }

    @Override
    protected boolean canDispenserEquipIntoSlot(EquipmentSlot p_396251_) {
        return p_396251_ == EquipmentSlot.SADDLE || super.canDispenserEquipIntoSlot(p_396251_);
    }

    @Override
    protected Holder<SoundEvent> getEquipSound(EquipmentSlot p_391970_, ItemStack p_395162_, Equippable p_395329_) {
        return (Holder<SoundEvent>)(p_391970_ == EquipmentSlot.SADDLE ? SoundEvents.PIG_SADDLE : super.getEquipSound(p_391970_, p_395162_, p_395329_));
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity pLivingEntity) {
        Direction direction = this.getMotionDirection();
        if (direction.getAxis() == Direction.Axis.Y) {
            return super.getDismountLocationForPassenger(pLivingEntity);
        } else {
            int[][] aint = DismountHelper.offsetsForDirection(direction);
            BlockPos blockpos = this.blockPosition();
            BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

            for (Pose pose : pLivingEntity.getDismountPoses()) {
                AABB aabb = pLivingEntity.getLocalBoundsForPose(pose);

                for (int[] aint1 : aint) {
                    blockpos$mutableblockpos.set(blockpos.getX() + aint1[0], blockpos.getY(), blockpos.getZ() + aint1[1]);
                    double d0 = this.level().getBlockFloorHeight(blockpos$mutableblockpos);
                    if (DismountHelper.isBlockFloorValid(d0)) {
                        Vec3 vec3 = Vec3.upFromBottomCenterOf(blockpos$mutableblockpos, d0);
                        if (DismountHelper.canDismountTo(this.level(), pLivingEntity, aabb.move(vec3))) {
                            pLivingEntity.setPose(pose);
                            return vec3;
                        }
                    }
                }
            }

            return super.getDismountLocationForPassenger(pLivingEntity);
        }
    }

    @Override
    public void thunderHit(ServerLevel pLevel, LightningBolt pLightning) {
        if (pLevel.getDifficulty() != Difficulty.PEACEFUL && net.minecraftforge.event.ForgeEventFactory.canLivingConvert(this, EntityType.ZOMBIFIED_PIGLIN, (timer) -> {})) {
            ZombifiedPiglin zombifiedpiglin = this.convertTo(EntityType.ZOMBIFIED_PIGLIN, ConversionParams.single(this, false, true), p_390653_ -> {
                if (this.getMainHandItem().isEmpty()) {
                    p_390653_.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_SWORD));
                }

                p_390653_.setPersistenceRequired();
                net.minecraftforge.event.ForgeEventFactory.onLivingConvert(this, p_390653_);
            });
            if (zombifiedpiglin == null) {
                super.thunderHit(pLevel, pLightning);
            }
        } else {
            super.thunderHit(pLevel, pLightning);
        }
    }

    @Override
    protected void tickRidden(Player p_278330_, Vec3 p_278267_) {
        super.tickRidden(p_278330_, p_278267_);
        this.setRot(p_278330_.getYRot(), p_278330_.getXRot() * 0.5F);
        this.yRotO = this.yBodyRot = this.yHeadRot = this.getYRot();
        this.steering.tickBoost();
    }

    @Override
    protected Vec3 getRiddenInput(Player p_278309_, Vec3 p_275479_) {
        return new Vec3(0.0, 0.0, 1.0);
    }

    @Override
    protected float getRiddenSpeed(Player p_278258_) {
        return (float)(this.getAttributeValue(Attributes.MOVEMENT_SPEED) * 0.225 * this.steering.boostFactor());
    }

    @Override
    public boolean boost() {
        return this.steering.boost(this.getRandom());
    }

    @Nullable
    public Pig getBreedOffspring(ServerLevel p_149001_, AgeableMob p_149002_) {
        Pig pig = EntityType.PIG.create(p_149001_, EntitySpawnReason.BREEDING);
        if (pig != null && p_149002_ instanceof Pig pig1) {
            pig.setVariant(this.random.nextBoolean() ? this.getVariant() : pig1.getVariant());
        }

        return pig;
    }

    @Override
    public boolean isFood(ItemStack pStack) {
        return pStack.is(ItemTags.PIG_FOOD);
    }

    @Override
    public Vec3 getLeashOffset() {
        return new Vec3(0.0, 0.6F * this.getEyeHeight(), this.getBbWidth() * 0.4F);
    }

    private void setVariant(Holder<PigVariant> pVariant) {
        this.entityData.set(DATA_VARIANT_ID, pVariant);
    }

    public Holder<PigVariant> getVariant() {
        return this.entityData.get(DATA_VARIANT_ID);
    }

    @Nullable
    @Override
    public <T> T get(DataComponentType<? extends T> p_392167_) {
        return p_392167_ == DataComponents.PIG_VARIANT ? castComponentValue((DataComponentType<T>)p_392167_, this.getVariant()) : super.get(p_392167_);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter p_392128_) {
        this.applyImplicitComponentIfPresent(p_392128_, DataComponents.PIG_VARIANT);
        super.applyImplicitComponents(p_392128_);
    }

    @Override
    protected <T> boolean applyImplicitComponent(DataComponentType<T> p_394960_, T p_396138_) {
        if (p_394960_ == DataComponents.PIG_VARIANT) {
            this.setVariant(castComponentValue(DataComponents.PIG_VARIANT, p_396138_));
            return true;
        } else {
            return super.applyImplicitComponent(p_394960_, p_396138_);
        }
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor p_397867_, DifficultyInstance p_393732_, EntitySpawnReason p_395935_, @Nullable SpawnGroupData p_397376_) {
        PigVariants.selectVariantToSpawn(this.random, this.registryAccess(), SpawnContext.create(p_397867_, this.blockPosition())).ifPresent(this::setVariant);
        return super.finalizeSpawn(p_397867_, p_393732_, p_395935_, p_397376_);
    }
}
