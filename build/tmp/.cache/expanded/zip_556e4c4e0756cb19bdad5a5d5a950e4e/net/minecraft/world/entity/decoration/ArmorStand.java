package net.minecraft.world.entity.decoration;

import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Rotations;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ArmorStand extends LivingEntity {
    public static final int WOBBLE_TIME = 5;
    private static final boolean ENABLE_ARMS = true;
    public static final Rotations DEFAULT_HEAD_POSE = new Rotations(0.0F, 0.0F, 0.0F);
    public static final Rotations DEFAULT_BODY_POSE = new Rotations(0.0F, 0.0F, 0.0F);
    public static final Rotations DEFAULT_LEFT_ARM_POSE = new Rotations(-10.0F, 0.0F, -10.0F);
    public static final Rotations DEFAULT_RIGHT_ARM_POSE = new Rotations(-15.0F, 0.0F, 10.0F);
    public static final Rotations DEFAULT_LEFT_LEG_POSE = new Rotations(-1.0F, 0.0F, -1.0F);
    public static final Rotations DEFAULT_RIGHT_LEG_POSE = new Rotations(1.0F, 0.0F, 1.0F);
    private static final EntityDimensions MARKER_DIMENSIONS = EntityDimensions.fixed(0.0F, 0.0F);
    private static final EntityDimensions BABY_DIMENSIONS = EntityType.ARMOR_STAND.getDimensions().scale(0.5F).withEyeHeight(0.9875F);
    private static final double FEET_OFFSET = 0.1;
    private static final double CHEST_OFFSET = 0.9;
    private static final double LEGS_OFFSET = 0.4;
    private static final double HEAD_OFFSET = 1.6;
    public static final int DISABLE_TAKING_OFFSET = 8;
    public static final int DISABLE_PUTTING_OFFSET = 16;
    public static final int CLIENT_FLAG_SMALL = 1;
    public static final int CLIENT_FLAG_SHOW_ARMS = 4;
    public static final int CLIENT_FLAG_NO_BASEPLATE = 8;
    public static final int CLIENT_FLAG_MARKER = 16;
    public static final EntityDataAccessor<Byte> DATA_CLIENT_FLAGS = SynchedEntityData.defineId(ArmorStand.class, EntityDataSerializers.BYTE);
    public static final EntityDataAccessor<Rotations> DATA_HEAD_POSE = SynchedEntityData.defineId(ArmorStand.class, EntityDataSerializers.ROTATIONS);
    public static final EntityDataAccessor<Rotations> DATA_BODY_POSE = SynchedEntityData.defineId(ArmorStand.class, EntityDataSerializers.ROTATIONS);
    public static final EntityDataAccessor<Rotations> DATA_LEFT_ARM_POSE = SynchedEntityData.defineId(ArmorStand.class, EntityDataSerializers.ROTATIONS);
    public static final EntityDataAccessor<Rotations> DATA_RIGHT_ARM_POSE = SynchedEntityData.defineId(ArmorStand.class, EntityDataSerializers.ROTATIONS);
    public static final EntityDataAccessor<Rotations> DATA_LEFT_LEG_POSE = SynchedEntityData.defineId(ArmorStand.class, EntityDataSerializers.ROTATIONS);
    public static final EntityDataAccessor<Rotations> DATA_RIGHT_LEG_POSE = SynchedEntityData.defineId(ArmorStand.class, EntityDataSerializers.ROTATIONS);
    private static final Predicate<Entity> RIDABLE_MINECARTS = p_359230_ -> p_359230_ instanceof AbstractMinecart abstractminecart && abstractminecart.isRideable();
    private static final boolean DEFAULT_INVISIBLE = false;
    private static final int DEFAULT_DISABLED_SLOTS = 0;
    private static final boolean DEFAULT_SMALL = false;
    private static final boolean DEFAULT_SHOW_ARMS = false;
    private static final boolean DEFAULT_NO_BASE_PLATE = false;
    private static final boolean DEFAULT_MARKER = false;
    private boolean invisible = false;
    public long lastHit;
    private int disabledSlots = 0;
    private Rotations headPose = DEFAULT_HEAD_POSE;
    private Rotations bodyPose = DEFAULT_BODY_POSE;
    private Rotations leftArmPose = DEFAULT_LEFT_ARM_POSE;
    private Rotations rightArmPose = DEFAULT_RIGHT_ARM_POSE;
    private Rotations leftLegPose = DEFAULT_LEFT_LEG_POSE;
    private Rotations rightLegPose = DEFAULT_RIGHT_LEG_POSE;

    public ArmorStand(EntityType<? extends ArmorStand> p_31553_, Level p_31554_) {
        super(p_31553_, p_31554_);
    }

    public ArmorStand(Level pLevel, double pX, double pY, double pZ) {
        this(EntityType.ARMOR_STAND, pLevel);
        this.setPos(pX, pY, pZ);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createLivingAttributes().add(Attributes.STEP_HEIGHT, 0.0);
    }

    @Override
    public void refreshDimensions() {
        double d0 = this.getX();
        double d1 = this.getY();
        double d2 = this.getZ();
        super.refreshDimensions();
        this.setPos(d0, d1, d2);
    }

    private boolean hasPhysics() {
        return !this.isMarker() && !this.isNoGravity();
    }

    @Override
    public boolean isEffectiveAi() {
        return super.isEffectiveAi() && this.hasPhysics();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder p_328656_) {
        super.defineSynchedData(p_328656_);
        p_328656_.define(DATA_CLIENT_FLAGS, (byte)0);
        p_328656_.define(DATA_HEAD_POSE, DEFAULT_HEAD_POSE);
        p_328656_.define(DATA_BODY_POSE, DEFAULT_BODY_POSE);
        p_328656_.define(DATA_LEFT_ARM_POSE, DEFAULT_LEFT_ARM_POSE);
        p_328656_.define(DATA_RIGHT_ARM_POSE, DEFAULT_RIGHT_ARM_POSE);
        p_328656_.define(DATA_LEFT_LEG_POSE, DEFAULT_LEFT_LEG_POSE);
        p_328656_.define(DATA_RIGHT_LEG_POSE, DEFAULT_RIGHT_LEG_POSE);
    }

    @Override
    public boolean canUseSlot(EquipmentSlot p_332073_) {
        return p_332073_ != EquipmentSlot.BODY && p_332073_ != EquipmentSlot.SADDLE && !this.isDisabled(p_332073_);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putBoolean("Invisible", this.isInvisible());
        pCompound.putBoolean("Small", this.isSmall());
        pCompound.putBoolean("ShowArms", this.showArms());
        pCompound.putInt("DisabledSlots", this.disabledSlots);
        pCompound.putBoolean("NoBasePlate", !this.showBasePlate());
        if (this.isMarker()) {
            pCompound.putBoolean("Marker", this.isMarker());
        }

        pCompound.put("Pose", this.writePose());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        this.setInvisible(pCompound.getBooleanOr("Invisible", false));
        this.setSmall(pCompound.getBooleanOr("Small", false));
        this.setShowArms(pCompound.getBooleanOr("ShowArms", false));
        this.disabledSlots = pCompound.getIntOr("DisabledSlots", 0);
        this.setNoBasePlate(pCompound.getBooleanOr("NoBasePlate", false));
        this.setMarker(pCompound.getBooleanOr("Marker", false));
        this.noPhysics = !this.hasPhysics();
        this.readPose(pCompound.getCompoundOrEmpty("Pose"));
    }

    private void readPose(CompoundTag pCompound) {
        this.setHeadPose(pCompound.read("Head", Rotations.CODEC).orElse(DEFAULT_HEAD_POSE));
        this.setBodyPose(pCompound.read("Body", Rotations.CODEC).orElse(DEFAULT_BODY_POSE));
        this.setLeftArmPose(pCompound.read("LeftArm", Rotations.CODEC).orElse(DEFAULT_LEFT_ARM_POSE));
        this.setRightArmPose(pCompound.read("RightArm", Rotations.CODEC).orElse(DEFAULT_RIGHT_ARM_POSE));
        this.setLeftLegPose(pCompound.read("LeftLeg", Rotations.CODEC).orElse(DEFAULT_LEFT_LEG_POSE));
        this.setRightLegPose(pCompound.read("RightLeg", Rotations.CODEC).orElse(DEFAULT_RIGHT_LEG_POSE));
    }

    private CompoundTag writePose() {
        CompoundTag compoundtag = new CompoundTag();
        if (!DEFAULT_HEAD_POSE.equals(this.headPose)) {
            compoundtag.store("Head", Rotations.CODEC, this.headPose);
        }

        if (!DEFAULT_BODY_POSE.equals(this.bodyPose)) {
            compoundtag.store("Body", Rotations.CODEC, this.bodyPose);
        }

        if (!DEFAULT_LEFT_ARM_POSE.equals(this.leftArmPose)) {
            compoundtag.store("LeftArm", Rotations.CODEC, this.leftArmPose);
        }

        if (!DEFAULT_RIGHT_ARM_POSE.equals(this.rightArmPose)) {
            compoundtag.store("RightArm", Rotations.CODEC, this.rightArmPose);
        }

        if (!DEFAULT_LEFT_LEG_POSE.equals(this.leftLegPose)) {
            compoundtag.store("LeftLeg", Rotations.CODEC, this.leftLegPose);
        }

        if (!DEFAULT_RIGHT_LEG_POSE.equals(this.rightLegPose)) {
            compoundtag.store("RightLeg", Rotations.CODEC, this.rightLegPose);
        }

        return compoundtag;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity pEntity) {
    }

    @Override
    protected void pushEntities() {
        for (Entity entity : this.level().getEntities(this, this.getBoundingBox(), RIDABLE_MINECARTS)) {
            if (this.distanceToSqr(entity) <= 0.2) {
                entity.push(this);
            }
        }
    }

    @Override
    public InteractionResult interactAt(Player pPlayer, Vec3 pVec, InteractionHand pHand) {
        ItemStack itemstack = pPlayer.getItemInHand(pHand);
        if (this.isMarker() || itemstack.is(Items.NAME_TAG)) {
            return InteractionResult.PASS;
        } else if (pPlayer.isSpectator()) {
            return InteractionResult.SUCCESS;
        } else if (pPlayer.level().isClientSide) {
            return InteractionResult.SUCCESS_SERVER;
        } else {
            EquipmentSlot equipmentslot = this.getEquipmentSlotForItem(itemstack);
            if (itemstack.isEmpty()) {
                EquipmentSlot equipmentslot1 = this.getClickedSlot(pVec);
                EquipmentSlot equipmentslot2 = this.isDisabled(equipmentslot1) ? equipmentslot : equipmentslot1;
                if (this.hasItemInSlot(equipmentslot2) && this.swapItem(pPlayer, equipmentslot2, itemstack, pHand)) {
                    return InteractionResult.SUCCESS_SERVER;
                }
            } else {
                if (this.isDisabled(equipmentslot)) {
                    return InteractionResult.FAIL;
                }

                if (equipmentslot.getType() == EquipmentSlot.Type.HAND && !this.showArms()) {
                    return InteractionResult.FAIL;
                }

                if (this.swapItem(pPlayer, equipmentslot, itemstack, pHand)) {
                    return InteractionResult.SUCCESS_SERVER;
                }
            }

            return InteractionResult.PASS;
        }
    }

    private EquipmentSlot getClickedSlot(Vec3 pVector) {
        EquipmentSlot equipmentslot = EquipmentSlot.MAINHAND;
        boolean flag = this.isSmall();
        double d0 = pVector.y / (this.getScale() * this.getAgeScale());
        EquipmentSlot equipmentslot1 = EquipmentSlot.FEET;
        if (d0 >= 0.1 && d0 < 0.1 + (flag ? 0.8 : 0.45) && this.hasItemInSlot(equipmentslot1)) {
            equipmentslot = EquipmentSlot.FEET;
        } else if (d0 >= 0.9 + (flag ? 0.3 : 0.0) && d0 < 0.9 + (flag ? 1.0 : 0.7) && this.hasItemInSlot(EquipmentSlot.CHEST)) {
            equipmentslot = EquipmentSlot.CHEST;
        } else if (d0 >= 0.4 && d0 < 0.4 + (flag ? 1.0 : 0.8) && this.hasItemInSlot(EquipmentSlot.LEGS)) {
            equipmentslot = EquipmentSlot.LEGS;
        } else if (d0 >= 1.6 && this.hasItemInSlot(EquipmentSlot.HEAD)) {
            equipmentslot = EquipmentSlot.HEAD;
        } else if (!this.hasItemInSlot(EquipmentSlot.MAINHAND) && this.hasItemInSlot(EquipmentSlot.OFFHAND)) {
            equipmentslot = EquipmentSlot.OFFHAND;
        }

        return equipmentslot;
    }

    private boolean isDisabled(EquipmentSlot pSlot) {
        return (this.disabledSlots & 1 << pSlot.getFilterBit(0)) != 0 || pSlot.getType() == EquipmentSlot.Type.HAND && !this.showArms();
    }

    private boolean swapItem(Player pPlayer, EquipmentSlot pSlot, ItemStack pStack, InteractionHand pHand) {
        ItemStack itemstack = this.getItemBySlot(pSlot);
        if (!itemstack.isEmpty() && (this.disabledSlots & 1 << pSlot.getFilterBit(8)) != 0) {
            return false;
        } else if (itemstack.isEmpty() && (this.disabledSlots & 1 << pSlot.getFilterBit(16)) != 0) {
            return false;
        } else if (pPlayer.hasInfiniteMaterials() && itemstack.isEmpty() && !pStack.isEmpty()) {
            this.setItemSlot(pSlot, pStack.copyWithCount(1));
            return true;
        } else if (pStack.isEmpty() || pStack.getCount() <= 1) {
            this.setItemSlot(pSlot, pStack);
            pPlayer.setItemInHand(pHand, itemstack);
            return true;
        } else if (!itemstack.isEmpty()) {
            return false;
        } else {
            this.setItemSlot(pSlot, pStack.split(1));
            return true;
        }
    }

    @Override
    public boolean hurtServer(ServerLevel p_362531_, DamageSource p_364009_, float p_366450_) {
        if (this.isRemoved()) {
            return false;
        } else if (!p_362531_.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING) && p_364009_.getEntity() instanceof Mob) {
            return false;
        } else if (p_364009_.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            this.kill(p_362531_);
            return false;
        } else if (this.isInvulnerableTo(p_362531_, p_364009_) || this.invisible || this.isMarker()) {
            return false;
        } else if (p_364009_.is(DamageTypeTags.IS_EXPLOSION)) {
            this.brokenByAnything(p_362531_, p_364009_);
            this.kill(p_362531_);
            return false;
        } else if (p_364009_.is(DamageTypeTags.IGNITES_ARMOR_STANDS)) {
            if (this.isOnFire()) {
                this.causeDamage(p_362531_, p_364009_, 0.15F);
            } else {
                this.igniteForSeconds(5.0F);
            }

            return false;
        } else if (p_364009_.is(DamageTypeTags.BURNS_ARMOR_STANDS) && this.getHealth() > 0.5F) {
            this.causeDamage(p_362531_, p_364009_, 4.0F);
            return false;
        } else {
            boolean flag = p_364009_.is(DamageTypeTags.CAN_BREAK_ARMOR_STAND);
            boolean flag1 = p_364009_.is(DamageTypeTags.ALWAYS_KILLS_ARMOR_STANDS);
            if (!flag && !flag1) {
                return false;
            } else if (p_364009_.getEntity() instanceof Player player && !player.getAbilities().mayBuild) {
                return false;
            } else if (p_364009_.isCreativePlayer()) {
                this.playBrokenSound();
                this.showBreakingParticles();
                this.kill(p_362531_);
                return true;
            } else {
                long i = p_362531_.getGameTime();
                if (i - this.lastHit > 5L && !flag1) {
                    p_362531_.broadcastEntityEvent(this, (byte)32);
                    this.gameEvent(GameEvent.ENTITY_DAMAGE, p_364009_.getEntity());
                    this.lastHit = i;
                } else {
                    this.brokenByPlayer(p_362531_, p_364009_);
                    this.showBreakingParticles();
                    this.kill(p_362531_);
                }

                return true;
            }
        }
    }

    @Override
    public void handleEntityEvent(byte p_31568_) {
        if (p_31568_ == 32) {
            if (this.level().isClientSide) {
                this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), SoundEvents.ARMOR_STAND_HIT, this.getSoundSource(), 0.3F, 1.0F, false);
                this.lastHit = this.level().getGameTime();
            }
        } else {
            super.handleEntityEvent(p_31568_);
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double pDistance) {
        double d0 = this.getBoundingBox().getSize() * 4.0;
        if (Double.isNaN(d0) || d0 == 0.0) {
            d0 = 4.0;
        }

        d0 *= 64.0;
        return pDistance < d0 * d0;
    }

    private void showBreakingParticles() {
        if (this.level() instanceof ServerLevel) {
            ((ServerLevel)this.level())
                .sendParticles(
                    new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OAK_PLANKS.defaultBlockState()),
                    this.getX(),
                    this.getY(0.6666666666666666),
                    this.getZ(),
                    10,
                    this.getBbWidth() / 4.0F,
                    this.getBbHeight() / 4.0F,
                    this.getBbWidth() / 4.0F,
                    0.05
                );
        }
    }

    private void causeDamage(ServerLevel pLevel, DamageSource pDamageSource, float pDamageAmount) {
        float f = this.getHealth();
        f -= pDamageAmount;
        if (f <= 0.5F) {
            this.brokenByAnything(pLevel, pDamageSource);
            this.kill(pLevel);
        } else {
            this.setHealth(f);
            this.gameEvent(GameEvent.ENTITY_DAMAGE, pDamageSource.getEntity());
        }
    }

    private void brokenByPlayer(ServerLevel pLevel, DamageSource pDamageSource) {
        ItemStack itemstack = new ItemStack(Items.ARMOR_STAND);
        itemstack.set(DataComponents.CUSTOM_NAME, this.getCustomName());
        Block.popResource(this.level(), this.blockPosition(), itemstack);
        this.brokenByAnything(pLevel, pDamageSource);
    }

    private void brokenByAnything(ServerLevel pLevel, DamageSource pDamageSource) {
        this.playBrokenSound();
        this.dropAllDeathLoot(pLevel, pDamageSource);

        for (EquipmentSlot equipmentslot : EquipmentSlot.VALUES) {
            ItemStack itemstack = this.equipment.set(equipmentslot, ItemStack.EMPTY);
            if (!itemstack.isEmpty()) {
                Block.popResource(this.level(), this.blockPosition().above(), itemstack);
            }
        }
    }

    private void playBrokenSound() {
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ARMOR_STAND_BREAK, this.getSoundSource(), 1.0F, 1.0F);
    }

    @Override
    protected void tickHeadTurn(float p_31644_) {
        this.yBodyRotO = this.yRotO;
        this.yBodyRot = this.getYRot();
    }

    @Override
    public void travel(Vec3 pTravelVector) {
        if (this.hasPhysics()) {
            super.travel(pTravelVector);
        }
    }

    @Override
    public void setYBodyRot(float pOffset) {
        this.yBodyRotO = this.yRotO = pOffset;
        this.yHeadRotO = this.yHeadRot = pOffset;
    }

    @Override
    public void setYHeadRot(float pRotation) {
        this.yBodyRotO = this.yRotO = pRotation;
        this.yHeadRotO = this.yHeadRot = pRotation;
    }

    @Override
    public void tick() {
        super.tick();
        Rotations rotations = this.entityData.get(DATA_HEAD_POSE);
        if (!this.headPose.equals(rotations)) {
            this.setHeadPose(rotations);
        }

        Rotations rotations1 = this.entityData.get(DATA_BODY_POSE);
        if (!this.bodyPose.equals(rotations1)) {
            this.setBodyPose(rotations1);
        }

        Rotations rotations2 = this.entityData.get(DATA_LEFT_ARM_POSE);
        if (!this.leftArmPose.equals(rotations2)) {
            this.setLeftArmPose(rotations2);
        }

        Rotations rotations3 = this.entityData.get(DATA_RIGHT_ARM_POSE);
        if (!this.rightArmPose.equals(rotations3)) {
            this.setRightArmPose(rotations3);
        }

        Rotations rotations4 = this.entityData.get(DATA_LEFT_LEG_POSE);
        if (!this.leftLegPose.equals(rotations4)) {
            this.setLeftLegPose(rotations4);
        }

        Rotations rotations5 = this.entityData.get(DATA_RIGHT_LEG_POSE);
        if (!this.rightLegPose.equals(rotations5)) {
            this.setRightLegPose(rotations5);
        }
    }

    @Override
    protected void updateInvisibilityStatus() {
        this.setInvisible(this.invisible);
    }

    @Override
    public void setInvisible(boolean pInvisible) {
        this.invisible = pInvisible;
        super.setInvisible(pInvisible);
    }

    @Override
    public boolean isBaby() {
        return this.isSmall();
    }

    @Override
    public void kill(ServerLevel p_361567_) {
        this.remove(Entity.RemovalReason.KILLED);
        this.gameEvent(GameEvent.ENTITY_DIE);
    }

    @Override
    public boolean ignoreExplosion(Explosion p_310221_) {
        return p_310221_.shouldAffectBlocklikeEntities() ? this.isInvisible() : true;
    }

    @Override
    public PushReaction getPistonPushReaction() {
        return this.isMarker() ? PushReaction.IGNORE : super.getPistonPushReaction();
    }

    @Override
    public boolean isIgnoringBlockTriggers() {
        return this.isMarker();
    }

    private void setSmall(boolean pSmall) {
        this.entityData.set(DATA_CLIENT_FLAGS, this.setBit(this.entityData.get(DATA_CLIENT_FLAGS), 1, pSmall));
    }

    public boolean isSmall() {
        return (this.entityData.get(DATA_CLIENT_FLAGS) & 1) != 0;
    }

    public void setShowArms(boolean pShowArms) {
        this.entityData.set(DATA_CLIENT_FLAGS, this.setBit(this.entityData.get(DATA_CLIENT_FLAGS), 4, pShowArms));
    }

    public boolean showArms() {
        return (this.entityData.get(DATA_CLIENT_FLAGS) & 4) != 0;
    }

    public void setNoBasePlate(boolean pNoBasePlate) {
        this.entityData.set(DATA_CLIENT_FLAGS, this.setBit(this.entityData.get(DATA_CLIENT_FLAGS), 8, pNoBasePlate));
    }

    public boolean showBasePlate() {
        return (this.entityData.get(DATA_CLIENT_FLAGS) & 8) == 0;
    }

    private void setMarker(boolean pMarker) {
        this.entityData.set(DATA_CLIENT_FLAGS, this.setBit(this.entityData.get(DATA_CLIENT_FLAGS), 16, pMarker));
    }

    public boolean isMarker() {
        return (this.entityData.get(DATA_CLIENT_FLAGS) & 16) != 0;
    }

    private byte setBit(byte pOldBit, int pOffset, boolean pValue) {
        if (pValue) {
            pOldBit = (byte)(pOldBit | pOffset);
        } else {
            pOldBit = (byte)(pOldBit & ~pOffset);
        }

        return pOldBit;
    }

    public void setHeadPose(Rotations pHeadPose) {
        this.headPose = pHeadPose;
        this.entityData.set(DATA_HEAD_POSE, pHeadPose);
    }

    public void setBodyPose(Rotations pBodyPose) {
        this.bodyPose = pBodyPose;
        this.entityData.set(DATA_BODY_POSE, pBodyPose);
    }

    public void setLeftArmPose(Rotations pLeftArmPose) {
        this.leftArmPose = pLeftArmPose;
        this.entityData.set(DATA_LEFT_ARM_POSE, pLeftArmPose);
    }

    public void setRightArmPose(Rotations pRightArmPose) {
        this.rightArmPose = pRightArmPose;
        this.entityData.set(DATA_RIGHT_ARM_POSE, pRightArmPose);
    }

    public void setLeftLegPose(Rotations pLeftLegPose) {
        this.leftLegPose = pLeftLegPose;
        this.entityData.set(DATA_LEFT_LEG_POSE, pLeftLegPose);
    }

    public void setRightLegPose(Rotations pRightLegPose) {
        this.rightLegPose = pRightLegPose;
        this.entityData.set(DATA_RIGHT_LEG_POSE, pRightLegPose);
    }

    public Rotations getHeadPose() {
        return this.headPose;
    }

    public Rotations getBodyPose() {
        return this.bodyPose;
    }

    public Rotations getLeftArmPose() {
        return this.leftArmPose;
    }

    public Rotations getRightArmPose() {
        return this.rightArmPose;
    }

    public Rotations getLeftLegPose() {
        return this.leftLegPose;
    }

    public Rotations getRightLegPose() {
        return this.rightLegPose;
    }

    @Override
    public boolean isPickable() {
        return super.isPickable() && !this.isMarker();
    }

    @Override
    public boolean skipAttackInteraction(Entity pEntity) {
        return pEntity instanceof Player player && !this.level().mayInteract(player, this.blockPosition());
    }

    @Override
    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }

    @Override
    public LivingEntity.Fallsounds getFallSounds() {
        return new LivingEntity.Fallsounds(SoundEvents.ARMOR_STAND_FALL, SoundEvents.ARMOR_STAND_FALL);
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource pDamageSource) {
        return SoundEvents.ARMOR_STAND_HIT;
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ARMOR_STAND_BREAK;
    }

    @Override
    public void thunderHit(ServerLevel pLevel, LightningBolt pLightning) {
    }

    @Override
    public boolean isAffectedByPotions() {
        return false;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> pKey) {
        if (DATA_CLIENT_FLAGS.equals(pKey)) {
            this.refreshDimensions();
            this.blocksBuilding = !this.isMarker();
        }

        super.onSyncedDataUpdated(pKey);
    }

    @Override
    public boolean attackable() {
        return false;
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose p_31587_) {
        return this.getDimensionsMarker(this.isMarker());
    }

    private EntityDimensions getDimensionsMarker(boolean pIsMarker) {
        if (pIsMarker) {
            return MARKER_DIMENSIONS;
        } else {
            return this.isBaby() ? BABY_DIMENSIONS : this.getType().getDimensions();
        }
    }

    @Override
    public Vec3 getLightProbePosition(float pPartialTicks) {
        if (this.isMarker()) {
            AABB aabb = this.getDimensionsMarker(false).makeBoundingBox(this.position());
            BlockPos blockpos = this.blockPosition();
            int i = Integer.MIN_VALUE;

            for (BlockPos blockpos1 : BlockPos.betweenClosed(
                BlockPos.containing(aabb.minX, aabb.minY, aabb.minZ), BlockPos.containing(aabb.maxX, aabb.maxY, aabb.maxZ)
            )) {
                int j = Math.max(this.level().getBrightness(LightLayer.BLOCK, blockpos1), this.level().getBrightness(LightLayer.SKY, blockpos1));
                if (j == 15) {
                    return Vec3.atCenterOf(blockpos1);
                }

                if (j > i) {
                    i = j;
                    blockpos = blockpos1.immutable();
                }
            }

            return Vec3.atCenterOf(blockpos);
        } else {
            return super.getLightProbePosition(pPartialTicks);
        }
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(Items.ARMOR_STAND);
    }

    @Override
    public boolean canBeSeenByAnyone() {
        return !this.isInvisible() && !this.isMarker();
    }
}