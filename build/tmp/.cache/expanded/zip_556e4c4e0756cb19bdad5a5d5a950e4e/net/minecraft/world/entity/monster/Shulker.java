package net.minecraft.world.entity.monster;

import java.util.EnumSet;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class Shulker extends AbstractGolem implements Enemy {
    private static final ResourceLocation COVERED_ARMOR_MODIFIER_ID = ResourceLocation.withDefaultNamespace("covered");
    private static final AttributeModifier COVERED_ARMOR_MODIFIER = new AttributeModifier(COVERED_ARMOR_MODIFIER_ID, 20.0, AttributeModifier.Operation.ADD_VALUE);
    protected static final EntityDataAccessor<Direction> DATA_ATTACH_FACE_ID = SynchedEntityData.defineId(Shulker.class, EntityDataSerializers.DIRECTION);
    protected static final EntityDataAccessor<Byte> DATA_PEEK_ID = SynchedEntityData.defineId(Shulker.class, EntityDataSerializers.BYTE);
    protected static final EntityDataAccessor<Byte> DATA_COLOR_ID = SynchedEntityData.defineId(Shulker.class, EntityDataSerializers.BYTE);
    private static final int TELEPORT_STEPS = 6;
    private static final byte NO_COLOR = 16;
    private static final byte DEFAULT_COLOR = 16;
    private static final int MAX_TELEPORT_DISTANCE = 8;
    private static final int OTHER_SHULKER_SCAN_RADIUS = 8;
    private static final int OTHER_SHULKER_LIMIT = 5;
    private static final float PEEK_PER_TICK = 0.05F;
    private static final byte DEFAULT_PEEK = 0;
    private static final Direction DEFAULT_ATTACH_FACE = Direction.DOWN;
    static final Vector3f FORWARD = Util.make(() -> {
        Vec3i vec3i = Direction.SOUTH.getUnitVec3i();
        return new Vector3f(vec3i.getX(), vec3i.getY(), vec3i.getZ());
    });
    private static final float MAX_SCALE = 3.0F;
    private float currentPeekAmountO;
    private float currentPeekAmount;
    @Nullable
    private BlockPos clientOldAttachPosition;
    private int clientSideTeleportInterpolation;
    private static final float MAX_LID_OPEN = 1.0F;

    public Shulker(EntityType<? extends Shulker> p_33404_, Level p_33405_) {
        super(p_33404_, p_33405_);
        this.xpReward = 5;
        this.lookControl = new Shulker.ShulkerLookControl(this);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 8.0F, 0.02F, true));
        this.goalSelector.addGoal(4, new Shulker.ShulkerAttackGoal());
        this.goalSelector.addGoal(7, new Shulker.ShulkerPeekGoal());
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this, this.getClass()).setAlertOthers());
        this.targetSelector.addGoal(2, new Shulker.ShulkerNearestAttackGoal(this));
        this.targetSelector.addGoal(3, new Shulker.ShulkerDefenseAttackGoal(this));
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.NONE;
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.HOSTILE;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SHULKER_AMBIENT;
    }

    @Override
    public void playAmbientSound() {
        if (!this.isClosed()) {
            super.playAmbientSound();
        }
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SHULKER_DEATH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource pDamageSource) {
        return this.isClosed() ? SoundEvents.SHULKER_HURT_CLOSED : SoundEvents.SHULKER_HURT;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder p_335590_) {
        super.defineSynchedData(p_335590_);
        p_335590_.define(DATA_ATTACH_FACE_ID, DEFAULT_ATTACH_FACE);
        p_335590_.define(DATA_PEEK_ID, (byte)0);
        p_335590_.define(DATA_COLOR_ID, (byte)16);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 30.0);
    }

    @Override
    protected BodyRotationControl createBodyControl() {
        return new Shulker.ShulkerBodyRotationControl(this);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        this.setAttachFace(pCompound.read("AttachFace", Direction.LEGACY_ID_CODEC).orElse(DEFAULT_ATTACH_FACE));
        this.entityData.set(DATA_PEEK_ID, pCompound.getByteOr("Peek", (byte)0));
        this.entityData.set(DATA_COLOR_ID, pCompound.getByteOr("Color", (byte)16));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.store("AttachFace", Direction.LEGACY_ID_CODEC, this.getAttachFace());
        pCompound.putByte("Peek", this.entityData.get(DATA_PEEK_ID));
        pCompound.putByte("Color", this.entityData.get(DATA_COLOR_ID));
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide && !this.isPassenger() && !this.canStayAt(this.blockPosition(), this.getAttachFace())) {
            this.findNewAttachment();
        }

        if (this.updatePeekAmount()) {
            this.onPeekAmountChange();
        }

        if (this.level().isClientSide) {
            if (this.clientSideTeleportInterpolation > 0) {
                this.clientSideTeleportInterpolation--;
            } else {
                this.clientOldAttachPosition = null;
            }
        }
    }

    private void findNewAttachment() {
        Direction direction = this.findAttachableSurface(this.blockPosition());
        if (direction != null) {
            this.setAttachFace(direction);
        } else {
            this.teleportSomewhere();
        }
    }

    @Override
    protected AABB makeBoundingBox(Vec3 p_378289_) {
        float f = getPhysicalPeek(this.currentPeekAmount);
        Direction direction = this.getAttachFace().getOpposite();
        return getProgressAabb(this.getScale(), direction, f, p_378289_);
    }

    private static float getPhysicalPeek(float pPeek) {
        return 0.5F - Mth.sin((0.5F + pPeek) * (float) Math.PI) * 0.5F;
    }

    private boolean updatePeekAmount() {
        this.currentPeekAmountO = this.currentPeekAmount;
        float f = this.getRawPeekAmount() * 0.01F;
        if (this.currentPeekAmount == f) {
            return false;
        } else {
            if (this.currentPeekAmount > f) {
                this.currentPeekAmount = Mth.clamp(this.currentPeekAmount - 0.05F, f, 1.0F);
            } else {
                this.currentPeekAmount = Mth.clamp(this.currentPeekAmount + 0.05F, 0.0F, f);
            }

            return true;
        }
    }

    private void onPeekAmountChange() {
        this.reapplyPosition();
        float f = getPhysicalPeek(this.currentPeekAmount);
        float f1 = getPhysicalPeek(this.currentPeekAmountO);
        Direction direction = this.getAttachFace().getOpposite();
        float f2 = (f - f1) * this.getScale();
        if (!(f2 <= 0.0F)) {
            for (Entity entity : this.level()
                .getEntities(
                    this, getProgressDeltaAabb(this.getScale(), direction, f1, f, this.position()), EntitySelector.NO_SPECTATORS.and(p_149771_ -> !p_149771_.isPassengerOfSameVehicle(this))
                )) {
                if (!(entity instanceof Shulker) && !entity.noPhysics) {
                    entity.move(MoverType.SHULKER, new Vec3(f2 * direction.getStepX(), f2 * direction.getStepY(), f2 * direction.getStepZ()));
                }
            }
        }
    }

    public static AABB getProgressAabb(float pScale, Direction pExpansionDirection, float pPeek, Vec3 pPosition) {
        return getProgressDeltaAabb(pScale, pExpansionDirection, -1.0F, pPeek, pPosition);
    }

    public static AABB getProgressDeltaAabb(float pScale, Direction pExpansionDirection, float pCurrentPeek, float pOldPeek, Vec3 pPosition) {
        AABB aabb = new AABB(-pScale * 0.5, 0.0, -pScale * 0.5, pScale * 0.5, pScale, pScale * 0.5);
        double d0 = Math.max(pCurrentPeek, pOldPeek);
        double d1 = Math.min(pCurrentPeek, pOldPeek);
        AABB aabb1 = aabb.expandTowards(pExpansionDirection.getStepX() * d0 * pScale, pExpansionDirection.getStepY() * d0 * pScale, pExpansionDirection.getStepZ() * d0 * pScale)
            .contract(
                -pExpansionDirection.getStepX() * (1.0 + d1) * pScale,
                -pExpansionDirection.getStepY() * (1.0 + d1) * pScale,
                -pExpansionDirection.getStepZ() * (1.0 + d1) * pScale
            );
        return aabb1.move(pPosition.x, pPosition.y, pPosition.z);
    }

    @Override
    public boolean startRiding(Entity p_149773_, boolean p_149774_) {
        if (this.level().isClientSide()) {
            this.clientOldAttachPosition = null;
            this.clientSideTeleportInterpolation = 0;
        }

        this.setAttachFace(Direction.DOWN);
        return super.startRiding(p_149773_, p_149774_);
    }

    @Override
    public void stopRiding() {
        super.stopRiding();
        if (this.level().isClientSide) {
            this.clientOldAttachPosition = this.blockPosition();
        }

        this.yBodyRotO = 0.0F;
        this.yBodyRot = 0.0F;
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor p_149780_, DifficultyInstance p_149781_, EntitySpawnReason p_365010_, @Nullable SpawnGroupData p_149783_) {
        this.setYRot(0.0F);
        this.yHeadRot = this.getYRot();
        this.setOldPosAndRot();
        return super.finalizeSpawn(p_149780_, p_149781_, p_365010_, p_149783_);
    }

    @Override
    public void move(MoverType pType, Vec3 pPos) {
        if (pType == MoverType.SHULKER_BOX) {
            this.teleportSomewhere();
        } else {
            super.move(pType, pPos);
        }
    }

    @Override
    public Vec3 getDeltaMovement() {
        return Vec3.ZERO;
    }

    @Override
    public void setDeltaMovement(Vec3 p_149804_) {
    }

    @Override
    public void setPos(double pX, double pY, double pZ) {
        BlockPos blockpos = this.blockPosition();
        if (this.isPassenger()) {
            super.setPos(pX, pY, pZ);
        } else {
            super.setPos(Mth.floor(pX) + 0.5, Mth.floor(pY + 0.5), Mth.floor(pZ) + 0.5);
        }

        if (this.tickCount != 0) {
            BlockPos blockpos1 = this.blockPosition();
            if (!blockpos1.equals(blockpos)) {
                this.entityData.set(DATA_PEEK_ID, (byte)0);
                this.hasImpulse = true;
                if (this.level().isClientSide && !this.isPassenger() && !blockpos1.equals(this.clientOldAttachPosition)) {
                    this.clientOldAttachPosition = blockpos;
                    this.clientSideTeleportInterpolation = 6;
                    this.xOld = this.getX();
                    this.yOld = this.getY();
                    this.zOld = this.getZ();
                }
            }
        }
    }

    @Nullable
    protected Direction findAttachableSurface(BlockPos pPos) {
        for (Direction direction : Direction.values()) {
            if (this.canStayAt(pPos, direction)) {
                return direction;
            }
        }

        return null;
    }

    boolean canStayAt(BlockPos pPos, Direction pFacing) {
        if (this.isPositionBlocked(pPos)) {
            return false;
        } else {
            Direction direction = pFacing.getOpposite();
            if (!this.level().loadedAndEntityCanStandOnFace(pPos.relative(pFacing), this, direction)) {
                return false;
            } else {
                AABB aabb = getProgressAabb(this.getScale(), direction, 1.0F, pPos.getBottomCenter()).deflate(1.0E-6);
                return this.level().noCollision(this, aabb);
            }
        }
    }

    private boolean isPositionBlocked(BlockPos pPos) {
        BlockState blockstate = this.level().getBlockState(pPos);
        if (blockstate.isAir()) {
            return false;
        } else {
            boolean flag = blockstate.is(Blocks.MOVING_PISTON) && pPos.equals(this.blockPosition());
            return !flag;
        }
    }

    protected boolean teleportSomewhere() {
        if (!this.isNoAi() && this.isAlive()) {
            BlockPos blockpos = this.blockPosition();

            for (int i = 0; i < 5; i++) {
                BlockPos blockpos1 = blockpos.offset(
                    Mth.randomBetweenInclusive(this.random, -8, 8), Mth.randomBetweenInclusive(this.random, -8, 8), Mth.randomBetweenInclusive(this.random, -8, 8)
                );
                if (blockpos1.getY() > this.level().getMinY()
                    && this.level().isEmptyBlock(blockpos1)
                    && this.level().getWorldBorder().isWithinBounds(blockpos1)
                    && this.level().noCollision(this, new AABB(blockpos1).deflate(1.0E-6))) {
                    Direction direction = this.findAttachableSurface(blockpos1);
                    if (direction != null) {
                        net.minecraftforge.event.entity.EntityTeleportEvent.EnderEntity event = net.minecraftforge.event.ForgeEventFactory.onEnderTeleport(this, blockpos1.getX(), blockpos1.getY(), blockpos1.getZ());
                        if (event.isCanceled()) direction = null;
                        blockpos1 = BlockPos.containing(event.getTargetX(), event.getTargetY(), event.getTargetZ());
                    }

                    if (direction != null) {
                        this.unRide();
                        this.setAttachFace(direction);
                        this.playSound(SoundEvents.SHULKER_TELEPORT, 1.0F, 1.0F);
                        this.setPos(blockpos1.getX() + 0.5, blockpos1.getY(), blockpos1.getZ() + 0.5);
                        this.level().gameEvent(GameEvent.TELEPORT, blockpos, GameEvent.Context.of(this));
                        this.entityData.set(DATA_PEEK_ID, (byte)0);
                        this.setTarget(null);
                        return true;
                    }
                }
            }

            return false;
        } else {
            return false;
        }
    }

    @Override
    public InterpolationHandler getInterpolation() {
        return null;
    }

    @Override
    public boolean hurtServer(ServerLevel p_366136_, DamageSource p_366119_, float p_367361_) {
        if (this.isClosed()) {
            Entity entity = p_366119_.getDirectEntity();
            if (entity instanceof AbstractArrow) {
                return false;
            }
        }

        if (!super.hurtServer(p_366136_, p_366119_, p_367361_)) {
            return false;
        } else {
            if (this.getHealth() < this.getMaxHealth() * 0.5 && this.random.nextInt(4) == 0) {
                this.teleportSomewhere();
            } else if (p_366119_.is(DamageTypeTags.IS_PROJECTILE)) {
                Entity entity1 = p_366119_.getDirectEntity();
                if (entity1 != null && entity1.getType() == EntityType.SHULKER_BULLET) {
                    this.hitByShulkerBullet();
                }
            }

            return true;
        }
    }

    private boolean isClosed() {
        return this.getRawPeekAmount() == 0;
    }

    private void hitByShulkerBullet() {
        Vec3 vec3 = this.position();
        AABB aabb = this.getBoundingBox();
        if (!this.isClosed() && this.teleportSomewhere()) {
            int i = this.level().getEntities(EntityType.SHULKER, aabb.inflate(8.0), Entity::isAlive).size();
            float f = (i - 1) / 5.0F;
            if (!(this.level().random.nextFloat() < f)) {
                Shulker shulker = EntityType.SHULKER.create(this.level(), EntitySpawnReason.BREEDING);
                if (shulker != null) {
                    shulker.setVariant(this.getVariant());
                    shulker.snapTo(vec3);
                    this.level().addFreshEntity(shulker);
                }
            }
        }
    }

    @Override
    public boolean canBeCollidedWith() {
        return this.isAlive();
    }

    public Direction getAttachFace() {
        return this.entityData.get(DATA_ATTACH_FACE_ID);
    }

    private void setAttachFace(Direction pAttachFace) {
        this.entityData.set(DATA_ATTACH_FACE_ID, pAttachFace);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> pKey) {
        if (DATA_ATTACH_FACE_ID.equals(pKey)) {
            this.setBoundingBox(this.makeBoundingBox());
        }

        super.onSyncedDataUpdated(pKey);
    }

    private int getRawPeekAmount() {
        return this.entityData.get(DATA_PEEK_ID);
    }

    void setRawPeekAmount(int pPeekAmount) {
        if (!this.level().isClientSide) {
            this.getAttribute(Attributes.ARMOR).removeModifier(COVERED_ARMOR_MODIFIER_ID);
            if (pPeekAmount == 0) {
                this.getAttribute(Attributes.ARMOR).addPermanentModifier(COVERED_ARMOR_MODIFIER);
                this.playSound(SoundEvents.SHULKER_CLOSE, 1.0F, 1.0F);
                this.gameEvent(GameEvent.CONTAINER_CLOSE);
            } else {
                this.playSound(SoundEvents.SHULKER_OPEN, 1.0F, 1.0F);
                this.gameEvent(GameEvent.CONTAINER_OPEN);
            }
        }

        this.entityData.set(DATA_PEEK_ID, (byte)pPeekAmount);
    }

    public float getClientPeekAmount(float pPartialTick) {
        return Mth.lerp(pPartialTick, this.currentPeekAmountO, this.currentPeekAmount);
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket p_219067_) {
        super.recreateFromPacket(p_219067_);
        this.yBodyRot = 0.0F;
        this.yBodyRotO = 0.0F;
    }

    @Override
    public int getMaxHeadXRot() {
        return 180;
    }

    @Override
    public int getMaxHeadYRot() {
        return 180;
    }

    @Override
    public void push(Entity pEntity) {
    }

    @Nullable
    public Vec3 getRenderPosition(float pPartialTick) {
        if (this.clientOldAttachPosition != null && this.clientSideTeleportInterpolation > 0) {
            double d0 = (this.clientSideTeleportInterpolation - pPartialTick) / 6.0;
            d0 *= d0;
            d0 *= this.getScale();
            BlockPos blockpos = this.blockPosition();
            double d1 = (blockpos.getX() - this.clientOldAttachPosition.getX()) * d0;
            double d2 = (blockpos.getY() - this.clientOldAttachPosition.getY()) * d0;
            double d3 = (blockpos.getZ() - this.clientOldAttachPosition.getZ()) * d0;
            return new Vec3(-d1, -d2, -d3);
        } else {
            return null;
        }
    }

    @Override
    protected float sanitizeScale(float p_332844_) {
        return Math.min(p_332844_, 3.0F);
    }

    private void setVariant(Optional<DyeColor> pVariant) {
        this.entityData.set(DATA_COLOR_ID, pVariant.<Byte>map(p_262566_ -> (byte)p_262566_.getId()).orElse((byte)16));
    }

    public Optional<DyeColor> getVariant() {
        return Optional.ofNullable(this.getColor());
    }

    @Nullable
    public DyeColor getColor() {
        byte b0 = this.entityData.get(DATA_COLOR_ID);
        return b0 != 16 && b0 <= 15 ? DyeColor.byId(b0) : null;
    }

    @Nullable
    @Override
    public <T> T get(DataComponentType<? extends T> p_395942_) {
        return p_395942_ == DataComponents.SHULKER_COLOR ? castComponentValue((DataComponentType<T>)p_395942_, this.getColor()) : super.get(p_395942_);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter p_392524_) {
        this.applyImplicitComponentIfPresent(p_392524_, DataComponents.SHULKER_COLOR);
        super.applyImplicitComponents(p_392524_);
    }

    @Override
    protected <T> boolean applyImplicitComponent(DataComponentType<T> p_393862_, T p_393891_) {
        if (p_393862_ == DataComponents.SHULKER_COLOR) {
            this.setVariant(Optional.of(castComponentValue(DataComponents.SHULKER_COLOR, p_393891_)));
            return true;
        } else {
            return super.applyImplicitComponent(p_393862_, p_393891_);
        }
    }

    class ShulkerAttackGoal extends Goal {
        private int attackTime;

        public ShulkerAttackGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity livingentity = Shulker.this.getTarget();
            return livingentity != null && livingentity.isAlive() ? Shulker.this.level().getDifficulty() != Difficulty.PEACEFUL : false;
        }

        @Override
        public void start() {
            this.attackTime = 20;
            Shulker.this.setRawPeekAmount(100);
        }

        @Override
        public void stop() {
            Shulker.this.setRawPeekAmount(0);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (Shulker.this.level().getDifficulty() != Difficulty.PEACEFUL) {
                this.attackTime--;
                LivingEntity livingentity = Shulker.this.getTarget();
                if (livingentity != null) {
                    Shulker.this.getLookControl().setLookAt(livingentity, 180.0F, 180.0F);
                    double d0 = Shulker.this.distanceToSqr(livingentity);
                    if (d0 < 400.0) {
                        if (this.attackTime <= 0) {
                            this.attackTime = 20 + Shulker.this.random.nextInt(10) * 20 / 2;
                            Shulker.this.level()
                                .addFreshEntity(new ShulkerBullet(Shulker.this.level(), Shulker.this, livingentity, Shulker.this.getAttachFace().getAxis()));
                            Shulker.this.playSound(
                                SoundEvents.SHULKER_SHOOT, 2.0F, (Shulker.this.random.nextFloat() - Shulker.this.random.nextFloat()) * 0.2F + 1.0F
                            );
                        }
                    } else {
                        Shulker.this.setTarget(null);
                    }

                    super.tick();
                }
            }
        }
    }

    static class ShulkerBodyRotationControl extends BodyRotationControl {
        public ShulkerBodyRotationControl(Mob p_149816_) {
            super(p_149816_);
        }

        @Override
        public void clientTick() {
        }
    }

    static class ShulkerDefenseAttackGoal extends NearestAttackableTargetGoal<LivingEntity> {
        public ShulkerDefenseAttackGoal(Shulker pShulker) {
            super(pShulker, LivingEntity.class, 10, true, false, (p_33501_, p_367887_) -> p_33501_ instanceof Enemy);
        }

        @Override
        public boolean canUse() {
            return this.mob.getTeam() == null ? false : super.canUse();
        }

        @Override
        protected AABB getTargetSearchArea(double pTargetDistance) {
            Direction direction = ((Shulker)this.mob).getAttachFace();
            if (direction.getAxis() == Direction.Axis.X) {
                return this.mob.getBoundingBox().inflate(4.0, pTargetDistance, pTargetDistance);
            } else {
                return direction.getAxis() == Direction.Axis.Z
                    ? this.mob.getBoundingBox().inflate(pTargetDistance, pTargetDistance, 4.0)
                    : this.mob.getBoundingBox().inflate(pTargetDistance, 4.0, pTargetDistance);
            }
        }
    }

    class ShulkerLookControl extends LookControl {
        public ShulkerLookControl(final Mob pMob) {
            super(pMob);
        }

        @Override
        protected void clampHeadRotationToBody() {
        }

        @Override
        protected Optional<Float> getYRotD() {
            Direction direction = Shulker.this.getAttachFace().getOpposite();
            Vector3f vector3f = direction.getRotation().transform(new Vector3f(Shulker.FORWARD));
            Vec3i vec3i = direction.getUnitVec3i();
            Vector3f vector3f1 = new Vector3f(vec3i.getX(), vec3i.getY(), vec3i.getZ());
            vector3f1.cross(vector3f);
            double d0 = this.wantedX - this.mob.getX();
            double d1 = this.wantedY - this.mob.getEyeY();
            double d2 = this.wantedZ - this.mob.getZ();
            Vector3f vector3f2 = new Vector3f((float)d0, (float)d1, (float)d2);
            float f = vector3f1.dot(vector3f2);
            float f1 = vector3f.dot(vector3f2);
            return !(Math.abs(f) > 1.0E-5F) && !(Math.abs(f1) > 1.0E-5F)
                ? Optional.empty()
                : Optional.of((float)(Mth.atan2(-f, f1) * 180.0F / (float)Math.PI));
        }

        @Override
        protected Optional<Float> getXRotD() {
            return Optional.of(0.0F);
        }
    }

    class ShulkerNearestAttackGoal extends NearestAttackableTargetGoal<Player> {
        public ShulkerNearestAttackGoal(final Shulker pShulker) {
            super(pShulker, Player.class, true);
        }

        @Override
        public boolean canUse() {
            return Shulker.this.level().getDifficulty() == Difficulty.PEACEFUL ? false : super.canUse();
        }

        @Override
        protected AABB getTargetSearchArea(double pTargetDistance) {
            Direction direction = ((Shulker)this.mob).getAttachFace();
            if (direction.getAxis() == Direction.Axis.X) {
                return this.mob.getBoundingBox().inflate(4.0, pTargetDistance, pTargetDistance);
            } else {
                return direction.getAxis() == Direction.Axis.Z
                    ? this.mob.getBoundingBox().inflate(pTargetDistance, pTargetDistance, 4.0)
                    : this.mob.getBoundingBox().inflate(pTargetDistance, 4.0, pTargetDistance);
            }
        }
    }

    class ShulkerPeekGoal extends Goal {
        private int peekTime;

        @Override
        public boolean canUse() {
            return Shulker.this.getTarget() == null
                && Shulker.this.random.nextInt(reducedTickDelay(40)) == 0
                && Shulker.this.canStayAt(Shulker.this.blockPosition(), Shulker.this.getAttachFace());
        }

        @Override
        public boolean canContinueToUse() {
            return Shulker.this.getTarget() == null && this.peekTime > 0;
        }

        @Override
        public void start() {
            this.peekTime = this.adjustedTickDelay(20 * (1 + Shulker.this.random.nextInt(3)));
            Shulker.this.setRawPeekAmount(30);
        }

        @Override
        public void stop() {
            if (Shulker.this.getTarget() == null) {
                Shulker.this.setRawPeekAmount(0);
            }
        }

        @Override
        public void tick() {
            this.peekTime--;
        }
    }
}
