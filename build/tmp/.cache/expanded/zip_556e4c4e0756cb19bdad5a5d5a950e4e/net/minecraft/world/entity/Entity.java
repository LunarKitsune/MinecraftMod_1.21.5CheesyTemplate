package net.minecraft.world.entity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.floats.FloatArraySet;
import it.unimi.dsi.fastutil.floats.FloatArrays;
import it.unimi.dsi.fastutil.floats.FloatSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.Object2DoubleArrayMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.BlockUtil;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.network.protocol.game.VecDeltaCodec;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SyncedDataHolder;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.Nameable;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.entity.vehicle.AbstractBoat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.HoneyBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.entity.EntityInLevelCallback;
import net.minecraft.world.level.gameevent.DynamicGameEventListener;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Team;
import org.jetbrains.annotations.Contract;

public abstract class Entity extends net.minecraftforge.common.capabilities.CapabilityProvider<Entity> implements SyncedDataHolder, Nameable, EntityAccess, ScoreHolder, DataComponentGetter, net.minecraftforge.common.extensions.IForgeEntity {
    public static final String ID_TAG = "id";
    public static final String PASSENGERS_TAG = "Passengers";
    private static final String DATA_TAG = "data";
    protected static final AtomicInteger ENTITY_COUNTER = new AtomicInteger();
    public static final int CONTENTS_SLOT_INDEX = 0;
    public static final int BOARDING_COOLDOWN = 60;
    public static final int TOTAL_AIR_SUPPLY = 300;
    public static final int MAX_ENTITY_TAG_COUNT = 1024;
    private static final Codec<List<String>> TAG_LIST_CODEC = Codec.STRING.sizeLimitedListOf(1024);
    public static final float DELTA_AFFECTED_BY_BLOCKS_BELOW_0_2 = 0.2F;
    public static final double DELTA_AFFECTED_BY_BLOCKS_BELOW_0_5 = 0.500001;
    public static final double DELTA_AFFECTED_BY_BLOCKS_BELOW_1_0 = 0.999999;
    public static final int BASE_TICKS_REQUIRED_TO_FREEZE = 140;
    public static final int FREEZE_HURT_FREQUENCY = 40;
    public static final int BASE_SAFE_FALL_DISTANCE = 3;
    private static final ImmutableList<Direction.Axis> YXZ_AXIS_ORDER = ImmutableList.of(Direction.Axis.Y, Direction.Axis.X, Direction.Axis.Z);
    private static final ImmutableList<Direction.Axis> YZX_AXIS_ORDER = ImmutableList.of(Direction.Axis.Y, Direction.Axis.Z, Direction.Axis.X);
    private static final AABB INITIAL_AABB = new AABB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
    private static final double WATER_FLOW_SCALE = 0.014;
    private static final double LAVA_FAST_FLOW_SCALE = 0.007;
    private static final double LAVA_SLOW_FLOW_SCALE = 0.0023333333333333335;
    public static final String UUID_TAG = "UUID";
    private static double viewScale = 1.0;
    @Deprecated // Forge: Use the getter to allow overriding in mods
    private final EntityType<?> type;
    private int id = ENTITY_COUNTER.incrementAndGet();
    public boolean blocksBuilding;
    private ImmutableList<Entity> passengers = ImmutableList.of();
    protected int boardingCooldown;
    @Nullable
    private Entity vehicle;
    private Level level;
    public double xo;
    public double yo;
    public double zo;
    private Vec3 position;
    private BlockPos blockPosition;
    private ChunkPos chunkPosition;
    private Vec3 deltaMovement = Vec3.ZERO;
    private float yRot;
    private float xRot;
    public float yRotO;
    public float xRotO;
    private AABB bb = INITIAL_AABB;
    private boolean onGround;
    public boolean horizontalCollision;
    public boolean verticalCollision;
    public boolean verticalCollisionBelow;
    public boolean minorHorizontalCollision;
    public boolean hurtMarked;
    protected Vec3 stuckSpeedMultiplier = Vec3.ZERO;
    @Nullable
    private Entity.RemovalReason removalReason;
    public static final float DEFAULT_BB_WIDTH = 0.6F;
    public static final float DEFAULT_BB_HEIGHT = 1.8F;
    public float moveDist;
    public float flyDist;
    public double fallDistance;
    private float nextStep = 1.0F;
    public double xOld;
    public double yOld;
    public double zOld;
    public boolean noPhysics;
    protected final RandomSource random = RandomSource.create();
    public int tickCount;
    private int remainingFireTicks = -this.getFireImmuneTicks();
    protected boolean wasTouchingWater;
    @Deprecated // Forge: Use forgeFluidTypeHeight instead
    protected Object2DoubleMap<TagKey<Fluid>> fluidHeight = new Object2DoubleArrayMap<>(2);
    protected boolean wasEyeInWater;
    @Deprecated // Forge: Use forgeFluidTypeOnEyes instead
    private final Set<TagKey<Fluid>> fluidOnEyes = new HashSet<>();
    public int invulnerableTime;
    protected boolean firstTick = true;
    protected final SynchedEntityData entityData;
    protected static final EntityDataAccessor<Byte> DATA_SHARED_FLAGS_ID = SynchedEntityData.defineId(Entity.class, EntityDataSerializers.BYTE);
    protected static final int FLAG_ONFIRE = 0;
    private static final int FLAG_SHIFT_KEY_DOWN = 1;
    private static final int FLAG_SPRINTING = 3;
    private static final int FLAG_SWIMMING = 4;
    private static final int FLAG_INVISIBLE = 5;
    protected static final int FLAG_GLOWING = 6;
    protected static final int FLAG_FALL_FLYING = 7;
    private static final EntityDataAccessor<Integer> DATA_AIR_SUPPLY_ID = SynchedEntityData.defineId(Entity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<Component>> DATA_CUSTOM_NAME = SynchedEntityData.defineId(Entity.class, EntityDataSerializers.OPTIONAL_COMPONENT);
    private static final EntityDataAccessor<Boolean> DATA_CUSTOM_NAME_VISIBLE = SynchedEntityData.defineId(Entity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_SILENT = SynchedEntityData.defineId(Entity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_NO_GRAVITY = SynchedEntityData.defineId(Entity.class, EntityDataSerializers.BOOLEAN);
    protected static final EntityDataAccessor<Pose> DATA_POSE = SynchedEntityData.defineId(Entity.class, EntityDataSerializers.POSE);
    private static final EntityDataAccessor<Integer> DATA_TICKS_FROZEN = SynchedEntityData.defineId(Entity.class, EntityDataSerializers.INT);
    private EntityInLevelCallback levelCallback = EntityInLevelCallback.NULL;
    private final VecDeltaCodec packetPositionCodec = new VecDeltaCodec();
    public boolean hasImpulse;
    @Nullable
    public PortalProcessor portalProcess;
    private int portalCooldown;
    private boolean invulnerable;
    protected UUID uuid = Mth.createInsecureUUID(this.random);
    protected String stringUUID = this.uuid.toString();
    private boolean hasGlowingTag;
    private final Set<String> tags = Sets.newHashSet();
    private final double[] pistonDeltas = new double[]{0.0, 0.0, 0.0};
    private long pistonDeltasGameTime;
    private EntityDimensions dimensions;
    private float eyeHeight;
    public boolean isInPowderSnow;
    public boolean wasInPowderSnow;
    public Optional<BlockPos> mainSupportingBlockPos = Optional.empty();
    private boolean onGroundNoBlocks = false;
    private float crystalSoundIntensity;
    private int lastCrystalSoundPlayTick;
    private boolean hasVisualFire;
    @Nullable
    private BlockState inBlockState = null;
    private final List<List<Entity.Movement>> movementThisTick = new ObjectArrayList<>();
    private final List<Entity.Movement> finalMovementsThisTick = new ObjectArrayList<>();
    private final LongSet visitedBlocks = new LongOpenHashSet();
    private final InsideBlockEffectApplier.StepBasedCollector insideEffectCollector = new InsideBlockEffectApplier.StepBasedCollector();
    private CustomData customData = CustomData.EMPTY;

    public Entity(EntityType<?> pEntityType, Level pLevel) {
        super(Entity.class);
        this.type = pEntityType;
        this.level = pLevel;
        this.dimensions = pEntityType.getDimensions();
        this.position = Vec3.ZERO;
        this.blockPosition = BlockPos.ZERO;
        this.chunkPosition = ChunkPos.ZERO;
        SynchedEntityData.Builder synchedentitydata$builder = new SynchedEntityData.Builder(this);
        synchedentitydata$builder.define(DATA_SHARED_FLAGS_ID, (byte)0);
        synchedentitydata$builder.define(DATA_AIR_SUPPLY_ID, this.getMaxAirSupply());
        synchedentitydata$builder.define(DATA_CUSTOM_NAME_VISIBLE, false);
        synchedentitydata$builder.define(DATA_CUSTOM_NAME, Optional.empty());
        synchedentitydata$builder.define(DATA_SILENT, false);
        synchedentitydata$builder.define(DATA_NO_GRAVITY, false);
        synchedentitydata$builder.define(DATA_POSE, Pose.STANDING);
        synchedentitydata$builder.define(DATA_TICKS_FROZEN, 0);
        this.defineSynchedData(synchedentitydata$builder);
        this.entityData = synchedentitydata$builder.build();
        this.setPos(0.0, 0.0, 0.0);
        this.eyeHeight = this.dimensions.eyeHeight();
        net.minecraftforge.event.ForgeEventFactory.onEntityConstructing(this);
        this.gatherCapabilities();
    }

    public boolean isColliding(BlockPos pPos, BlockState pState) {
        VoxelShape voxelshape = pState.getCollisionShape(this.level(), pPos, CollisionContext.of(this)).move(pPos);
        return Shapes.joinIsNotEmpty(voxelshape, Shapes.create(this.getBoundingBox()), BooleanOp.AND);
    }

    public int getTeamColor() {
        Team team = this.getTeam();
        return team != null && team.getColor().getColor() != null ? team.getColor().getColor() : 16777215;
    }

    public boolean isSpectator() {
        return false;
    }

    public final void unRide() {
        if (this.isVehicle()) {
            this.ejectPassengers();
        }

        if (this.isPassenger()) {
            this.stopRiding();
        }
    }

    public void syncPacketPositionCodec(double pX, double pY, double pZ) {
        this.packetPositionCodec.setBase(new Vec3(pX, pY, pZ));
    }

    public VecDeltaCodec getPositionCodec() {
        return this.packetPositionCodec;
    }

    public EntityType<?> getType() {
        return this.type;
    }

    @Override
    public int getId() {
        return this.id;
    }

    public void setId(int pId) {
        this.id = pId;
    }

    public Set<String> getTags() {
        return this.tags;
    }

    public boolean addTag(String pTag) {
        return this.tags.size() >= 1024 ? false : this.tags.add(pTag);
    }

    public boolean removeTag(String pTag) {
        return this.tags.remove(pTag);
    }

    public void kill(ServerLevel pLevel) {
        this.remove(Entity.RemovalReason.KILLED);
        this.gameEvent(GameEvent.ENTITY_DIE);
    }

    public final void discard() {
        this.remove(Entity.RemovalReason.DISCARDED);
    }

    protected abstract void defineSynchedData(SynchedEntityData.Builder pBuilder);

    public SynchedEntityData getEntityData() {
        return this.entityData;
    }

    @Override
    public boolean equals(Object pObject) {
        return pObject instanceof Entity ? ((Entity)pObject).id == this.id : false;
    }

    @Override
    public int hashCode() {
        return this.id;
    }

    public void remove(Entity.RemovalReason pReason) {
        this.setRemoved(pReason);
        this.invalidateCaps();
    }

    public void onClientRemoval() {
    }

    public void onRemoval(Entity.RemovalReason pReason) {
    }

    public void setPose(Pose pPose) {
        this.entityData.set(DATA_POSE, pPose);
    }

    public Pose getPose() {
        return this.entityData.get(DATA_POSE);
    }

    public boolean hasPose(Pose pPose) {
        return this.getPose() == pPose;
    }

    public boolean closerThan(Entity pEntity, double pDistance) {
        return this.position().closerThan(pEntity.position(), pDistance);
    }

    public boolean closerThan(Entity pEntity, double pHorizontalDistance, double pVerticalDistance) {
        double d0 = pEntity.getX() - this.getX();
        double d1 = pEntity.getY() - this.getY();
        double d2 = pEntity.getZ() - this.getZ();
        return Mth.lengthSquared(d0, d2) < Mth.square(pHorizontalDistance) && Mth.square(d1) < Mth.square(pVerticalDistance);
    }

    protected void setRot(float pYRot, float pXRot) {
        this.setYRot(pYRot % 360.0F);
        this.setXRot(pXRot % 360.0F);
    }

    public final void setPos(Vec3 pPos) {
        this.setPos(pPos.x(), pPos.y(), pPos.z());
    }

    public void setPos(double pX, double pY, double pZ) {
        this.setPosRaw(pX, pY, pZ);
        this.setBoundingBox(this.makeBoundingBox());
    }

    protected final AABB makeBoundingBox() {
        return this.makeBoundingBox(this.position);
    }

    protected AABB makeBoundingBox(Vec3 pPosition) {
        return this.dimensions.makeBoundingBox(pPosition);
    }

    protected void reapplyPosition() {
        this.setPos(this.position.x, this.position.y, this.position.z);
    }

    public void turn(double pYRot, double pXRot) {
        float f = (float)pXRot * 0.15F;
        float f1 = (float)pYRot * 0.15F;
        this.setXRot(this.getXRot() + f);
        this.setYRot(this.getYRot() + f1);
        this.setXRot(Mth.clamp(this.getXRot(), -90.0F, 90.0F));
        this.xRotO += f;
        this.yRotO += f1;
        this.xRotO = Mth.clamp(this.xRotO, -90.0F, 90.0F);
        if (this.vehicle != null) {
            this.vehicle.onPassengerTurned(this);
        }
    }

    public void tick() {
        this.baseTick();
    }

    public void baseTick() {
        ProfilerFiller profilerfiller = Profiler.get();
        profilerfiller.push("entityBaseTick");
        this.inBlockState = null;
        if (this.isPassenger() && this.getVehicle().isRemoved()) {
            this.stopRiding();
        }

        if (this.boardingCooldown > 0) {
            this.boardingCooldown--;
        }

        this.handlePortal();
        if (this.canSpawnSprintParticle()) {
            this.spawnSprintParticle();
        }

        this.wasInPowderSnow = this.isInPowderSnow;
        this.isInPowderSnow = false;
        this.updateInWaterStateAndDoFluidPushing();
        this.updateFluidOnEyes();
        this.updateSwimming();
        if (this.level() instanceof ServerLevel serverlevel) {
            if (this.remainingFireTicks > 0) {
                if (this.fireImmune()) {
                    this.setRemainingFireTicks(this.remainingFireTicks - 4);
                    if (this.remainingFireTicks < 0) {
                        this.clearFire();
                    }
                } else {
                    if (this.remainingFireTicks % 20 == 0 && !this.isInLava()) {
                        this.hurtServer(serverlevel, this.damageSources().onFire(), 1.0F);
                    }

                    this.setRemainingFireTicks(this.remainingFireTicks - 1);
                }
            }
        } else {
            this.clearFire();
        }

        if (this.isInLava()) {
            this.fallDistance *= this.getFluidFallDistanceModifier(net.minecraftforge.common.ForgeMod.LAVA_TYPE.get());
        }

        this.checkBelowWorld();
        if (!this.level().isClientSide) {
            this.setSharedFlagOnFire(this.remainingFireTicks > 0);
        }

        this.firstTick = false;
        if (this.level() instanceof ServerLevel serverlevel1 && this instanceof Leashable) {
            Leashable.tickLeash(serverlevel1, (Entity & Leashable)this);
        }

        profilerfiller.pop();
    }

    public void setSharedFlagOnFire(boolean pIsOnFire) {
        this.setSharedFlag(0, pIsOnFire || this.hasVisualFire);
    }

    public void checkBelowWorld() {
        if (this.getY() < this.level().getMinY() - 64) {
            this.onBelowWorld();
        }
    }

    public void setPortalCooldown() {
        this.portalCooldown = this.getDimensionChangingDelay();
    }

    public void setPortalCooldown(int pPortalCooldown) {
        this.portalCooldown = pPortalCooldown;
    }

    public int getPortalCooldown() {
        return this.portalCooldown;
    }

    public boolean isOnPortalCooldown() {
        return this.portalCooldown > 0;
    }

    protected void processPortalCooldown() {
        if (this.isOnPortalCooldown()) {
            this.portalCooldown--;
        }
    }

    public void lavaIgnite() {
        if (!this.fireImmune()) {
            this.igniteForSeconds(15.0F);
        }
    }

    public void lavaHurt() {
        if (!this.fireImmune()) {
            if (this.level() instanceof ServerLevel serverlevel
                && this.hurtServer(serverlevel, this.damageSources().lava(), 4.0F)
                && this.shouldPlayLavaHurtSound()
                && !this.isSilent()) {
                serverlevel.playSound(
                    null,
                    this.getX(),
                    this.getY(),
                    this.getZ(),
                    SoundEvents.GENERIC_BURN,
                    this.getSoundSource(),
                    0.4F,
                    2.0F + this.random.nextFloat() * 0.4F
                );
            }
        }
    }

    protected boolean shouldPlayLavaHurtSound() {
        return true;
    }

    public final void igniteForSeconds(float pSeconds) {
        this.igniteForTicks(Mth.floor(pSeconds * 20.0F));
    }

    public void igniteForTicks(int pTicks) {
        if (this.remainingFireTicks < pTicks) {
            this.setRemainingFireTicks(pTicks);
        }

        this.clearFreeze();
    }

    public void setRemainingFireTicks(int pRemainingFireTicks) {
        this.remainingFireTicks = pRemainingFireTicks;
    }

    public int getRemainingFireTicks() {
        return this.remainingFireTicks;
    }

    public void clearFire() {
        this.setRemainingFireTicks(0);
    }

    protected void onBelowWorld() {
        this.discard();
    }

    public boolean isFree(double pX, double pY, double pZ) {
        return this.isFree(this.getBoundingBox().move(pX, pY, pZ));
    }

    private boolean isFree(AABB pBox) {
        return this.level().noCollision(this, pBox) && !this.level().containsAnyLiquid(pBox);
    }

    public void setOnGround(boolean pOnGround) {
        this.onGround = pOnGround;
        this.checkSupportingBlock(pOnGround, null);
    }

    public void setOnGroundWithMovement(boolean pOnGround, Vec3 pMovement) {
        this.setOnGroundWithMovement(pOnGround, this.horizontalCollision, pMovement);
    }

    public void setOnGroundWithMovement(boolean pOnGround, boolean pHorizontalCollision, Vec3 pMovement) {
        this.onGround = pOnGround;
        this.horizontalCollision = pHorizontalCollision;
        this.checkSupportingBlock(pOnGround, pMovement);
    }

    public boolean isSupportedBy(BlockPos pPos) {
        return this.mainSupportingBlockPos.isPresent() && this.mainSupportingBlockPos.get().equals(pPos);
    }

    protected void checkSupportingBlock(boolean pOnGround, @Nullable Vec3 pMovement) {
        if (pOnGround) {
            AABB aabb = this.getBoundingBox();
            AABB aabb1 = new AABB(aabb.minX, aabb.minY - 1.0E-6, aabb.minZ, aabb.maxX, aabb.minY, aabb.maxZ);
            Optional<BlockPos> optional = this.level.findSupportingBlock(this, aabb1);
            if (optional.isPresent() || this.onGroundNoBlocks) {
                this.mainSupportingBlockPos = optional;
            } else if (pMovement != null) {
                AABB aabb2 = aabb1.move(-pMovement.x, 0.0, -pMovement.z);
                optional = this.level.findSupportingBlock(this, aabb2);
                this.mainSupportingBlockPos = optional;
            }

            this.onGroundNoBlocks = optional.isEmpty();
        } else {
            this.onGroundNoBlocks = false;
            if (this.mainSupportingBlockPos.isPresent()) {
                this.mainSupportingBlockPos = Optional.empty();
            }
        }
    }

    public boolean onGround() {
        return this.onGround;
    }

    public void move(MoverType pType, Vec3 pMovement) {
        if (this.noPhysics) {
            this.setPos(this.getX() + pMovement.x, this.getY() + pMovement.y, this.getZ() + pMovement.z);
        } else {
            if (pType == MoverType.PISTON) {
                pMovement = this.limitPistonMovement(pMovement);
                if (pMovement.equals(Vec3.ZERO)) {
                    return;
                }
            }

            ProfilerFiller profilerfiller = Profiler.get();
            profilerfiller.push("move");
            if (this.stuckSpeedMultiplier.lengthSqr() > 1.0E-7) {
                pMovement = pMovement.multiply(this.stuckSpeedMultiplier);
                this.stuckSpeedMultiplier = Vec3.ZERO;
                this.setDeltaMovement(Vec3.ZERO);
            }

            pMovement = this.maybeBackOffFromEdge(pMovement, pType);
            Vec3 vec3 = this.collide(pMovement);
            double d0 = vec3.lengthSqr();
            if (d0 > 1.0E-7 || pMovement.lengthSqr() - d0 < 1.0E-7) {
                if (this.fallDistance != 0.0 && d0 >= 1.0) {
                    BlockHitResult blockhitresult = this.level()
                        .clip(
                            new ClipContext(
                                this.position(), this.position().add(vec3), ClipContext.Block.FALLDAMAGE_RESETTING, ClipContext.Fluid.WATER, this
                            )
                        );
                    if (blockhitresult.getType() != HitResult.Type.MISS) {
                        this.resetFallDistance();
                    }
                }

                Vec3 vec32 = this.position();
                List<Entity.Movement> list = new ObjectArrayList<>();

                for (Direction.Axis direction$axis : axisStepOrder(vec3)) {
                    double d1 = vec3.get(direction$axis);
                    if (d1 != 0.0) {
                        Vec3 vec31 = vec32.relative(direction$axis.getPositive(), d1);
                        list.add(new Entity.Movement(vec32, vec31));
                        vec32 = vec31;
                    }
                }

                this.movementThisTick.add(list);
                this.setPos(vec32);
            }

            profilerfiller.pop();
            profilerfiller.push("rest");
            boolean flag = !Mth.equal(pMovement.x, vec3.x);
            boolean flag1 = !Mth.equal(pMovement.z, vec3.z);
            this.horizontalCollision = flag || flag1;
            if (Math.abs(pMovement.y) > 0.0 || this.isLocalInstanceAuthoritative()) {
                this.verticalCollision = pMovement.y != vec3.y;
                this.verticalCollisionBelow = this.verticalCollision && pMovement.y < 0.0;
                this.setOnGroundWithMovement(this.verticalCollisionBelow, this.horizontalCollision, vec3);
            }

            if (this.horizontalCollision) {
                this.minorHorizontalCollision = this.isHorizontalCollisionMinor(vec3);
            } else {
                this.minorHorizontalCollision = false;
            }

            BlockPos blockpos = this.getOnPosLegacy();
            BlockState blockstate = this.level().getBlockState(blockpos);
            if (this.isLocalInstanceAuthoritative()) {
                this.checkFallDamage(vec3.y, this.onGround(), blockstate, blockpos);
            }

            if (this.isRemoved()) {
                profilerfiller.pop();
            } else {
                if (this.horizontalCollision) {
                    Vec3 vec33 = this.getDeltaMovement();
                    this.setDeltaMovement(flag ? 0.0 : vec33.x, vec33.y, flag1 ? 0.0 : vec33.z);
                }

                if (this.canSimulateMovement()) {
                    Block block = blockstate.getBlock();
                    if (pMovement.y != vec3.y) {
                        block.updateEntityMovementAfterFallOn(this.level(), this);
                    }
                }

                if (!this.level().isClientSide() || this.isLocalInstanceAuthoritative()) {
                    Entity.MovementEmission entity$movementemission = this.getMovementEmission();
                    if (entity$movementemission.emitsAnything() && !this.isPassenger()) {
                        this.applyMovementEmissionAndPlaySound(entity$movementemission, vec3, blockpos, blockstate);
                    }
                }

                float f = this.getBlockSpeedFactor();
                this.setDeltaMovement(this.getDeltaMovement().multiply(f, 1.0, f));
                profilerfiller.pop();
            }
        }
    }

    private void applyMovementEmissionAndPlaySound(Entity.MovementEmission pMovementEmission, Vec3 pMovement, BlockPos pPos, BlockState pState) {
        float f = 0.6F;
        float f1 = (float)(pMovement.length() * 0.6F);
        float f2 = (float)(pMovement.horizontalDistance() * 0.6F);
        BlockPos blockpos = this.getOnPos();
        BlockState blockstate = this.level().getBlockState(blockpos);
        boolean flag = this.isStateClimbable(blockstate);
        this.moveDist += flag ? f1 : f2;
        this.flyDist += f1;
        if (this.moveDist > this.nextStep && !blockstate.isAir()) {
            boolean flag1 = blockpos.equals(pPos);
            boolean flag2 = this.vibrationAndSoundEffectsFromBlock(pPos, pState, pMovementEmission.emitsSounds(), flag1, pMovement);
            if (!flag1) {
                flag2 |= this.vibrationAndSoundEffectsFromBlock(blockpos, blockstate, false, pMovementEmission.emitsEvents(), pMovement);
            }

            if (flag2) {
                this.nextStep = this.nextStep();
            } else if (this.isInWater()) {
                this.nextStep = this.nextStep();
                if (pMovementEmission.emitsSounds()) {
                    this.waterSwimSound();
                }

                if (pMovementEmission.emitsEvents()) {
                    this.gameEvent(GameEvent.SWIM);
                }
            }
        } else if (blockstate.isAir()) {
            this.processFlappingMovement();
        }
    }

    protected void applyEffectsFromBlocks() {
        this.finalMovementsThisTick.clear();
        this.movementThisTick.forEach(this.finalMovementsThisTick::addAll);
        this.movementThisTick.clear();
        if (this.finalMovementsThisTick.isEmpty()) {
            this.finalMovementsThisTick.add(new Entity.Movement(this.oldPosition(), this.position()));
        } else if (this.finalMovementsThisTick.getLast().to.distanceToSqr(this.position()) > 9.9999994E-11F) {
            this.finalMovementsThisTick.add(new Entity.Movement(this.finalMovementsThisTick.getLast().to, this.position()));
        }

        this.applyEffectsFromBlocks(this.finalMovementsThisTick);
    }

    public void removeLatestMovementRecordingBatch() {
        if (!this.movementThisTick.isEmpty()) {
            this.movementThisTick.removeLast();
        }
    }

    public void applyEffectsFromBlocks(Vec3 pOldPosition, Vec3 pPosition) {
        this.applyEffectsFromBlocks(List.of(new Entity.Movement(pOldPosition, pPosition)));
    }

    private void applyEffectsFromBlocks(List<Entity.Movement> pMovements) {
        if (this.isAffectedByBlocks()) {
            if (this.onGround()) {
                BlockPos blockpos = this.getOnPosLegacy();
                BlockState blockstate = this.level().getBlockState(blockpos);
                blockstate.getBlock().stepOn(this.level(), blockpos, blockstate, this);
            }

            boolean flag = this.isOnFire();
            boolean flag1 = this.isFreezing();
            this.checkInsideBlocks(pMovements, this.insideEffectCollector);
            this.insideEffectCollector.applyAndClear(this);
            if (this.isInRain()) {
                this.clearFire();
            }

            if (flag && !this.isOnFire() || flag1 && !this.isFreezing()) {
                this.playEntityOnFireExtinguishedSound();
            }

            if (flag && !this.isOnFire() && this.remainingFireTicks <= 0) {
                this.setRemainingFireTicks(-this.getFireImmuneTicks());
            }
        }
    }

    protected boolean isAffectedByBlocks() {
        return !this.isRemoved() && !this.noPhysics;
    }

    private boolean isStateClimbable(BlockState pState) {
        return pState.is(BlockTags.CLIMBABLE) || pState.is(Blocks.POWDER_SNOW);
    }

    private boolean vibrationAndSoundEffectsFromBlock(BlockPos pPos, BlockState pState, boolean pPlayStepSound, boolean pBroadcastGameEvent, Vec3 pEntityPos) {
        if (pState.isAir()) {
            return false;
        } else {
            boolean flag = this.isStateClimbable(pState);
            if ((this.onGround() || flag || this.isCrouching() && pEntityPos.y == 0.0 || this.isOnRails()) && !this.isSwimming()) {
                if (pPlayStepSound) {
                    this.walkingStepSound(pPos, pState);
                }

                if (pBroadcastGameEvent) {
                    this.level().gameEvent(GameEvent.STEP, this.position(), GameEvent.Context.of(this, pState));
                }

                return true;
            } else {
                return false;
            }
        }
    }

    protected boolean isHorizontalCollisionMinor(Vec3 pDeltaMovement) {
        return false;
    }

    protected void playEntityOnFireExtinguishedSound() {
        if (!this.level.isClientSide()) {
            this.level()
                .playSound(
                    null,
                    this.getX(),
                    this.getY(),
                    this.getZ(),
                    SoundEvents.GENERIC_EXTINGUISH_FIRE,
                    this.getSoundSource(),
                    0.7F,
                    1.6F + (this.random.nextFloat() - this.random.nextFloat()) * 0.4F
                );
        }
    }

    public void extinguishFire() {
        if (this.isOnFire()) {
            this.playEntityOnFireExtinguishedSound();
        }

        this.clearFire();
    }

    protected void processFlappingMovement() {
        if (this.isFlapping()) {
            this.onFlap();
            if (this.getMovementEmission().emitsEvents()) {
                this.gameEvent(GameEvent.FLAP);
            }
        }
    }

    @Deprecated
    public BlockPos getOnPosLegacy() {
        return this.getOnPos(0.2F);
    }

    public BlockPos getBlockPosBelowThatAffectsMyMovement() {
        return this.getOnPos(0.500001F);
    }

    public BlockPos getOnPos() {
        return this.getOnPos(1.0E-5F);
    }

    protected BlockPos getOnPos(float pYOffset) {
        if (this.mainSupportingBlockPos.isPresent()) {
            BlockPos blockpos = this.mainSupportingBlockPos.get();
            if (!(pYOffset > 1.0E-5F)) {
                return blockpos;
            } else {
                BlockState blockstate = this.level().getBlockState(blockpos);
                return (!((double)pYOffset <= 0.5) || !blockstate.collisionExtendsVertically(this.level(), blockpos, this))
                    ? blockpos.atY(Mth.floor(this.position.y - pYOffset))
                    : blockpos;
            }
        } else {
            int i = Mth.floor(this.position.x);
            int j = Mth.floor(this.position.y - pYOffset);
            int k = Mth.floor(this.position.z);
            return new BlockPos(i, j, k);
        }
    }

    protected float getBlockJumpFactor() {
        float f = this.level().getBlockState(this.blockPosition()).getBlock().getJumpFactor();
        float f1 = this.level().getBlockState(this.getBlockPosBelowThatAffectsMyMovement()).getBlock().getJumpFactor();
        return f == 1.0 ? f1 : f;
    }

    protected float getBlockSpeedFactor() {
        BlockState blockstate = this.level().getBlockState(this.blockPosition());
        float f = blockstate.getBlock().getSpeedFactor();
        if (!blockstate.is(Blocks.WATER) && !blockstate.is(Blocks.BUBBLE_COLUMN)) {
            return f == 1.0 ? this.level().getBlockState(this.getBlockPosBelowThatAffectsMyMovement()).getBlock().getSpeedFactor() : f;
        } else {
            return f;
        }
    }

    protected Vec3 maybeBackOffFromEdge(Vec3 pVec, MoverType pMover) {
        return pVec;
    }

    protected Vec3 limitPistonMovement(Vec3 pPos) {
        if (pPos.lengthSqr() <= 1.0E-7) {
            return pPos;
        } else {
            long i = this.level().getGameTime();
            if (i != this.pistonDeltasGameTime) {
                Arrays.fill(this.pistonDeltas, 0.0);
                this.pistonDeltasGameTime = i;
            }

            if (pPos.x != 0.0) {
                double d2 = this.applyPistonMovementRestriction(Direction.Axis.X, pPos.x);
                return Math.abs(d2) <= 1.0E-5F ? Vec3.ZERO : new Vec3(d2, 0.0, 0.0);
            } else if (pPos.y != 0.0) {
                double d1 = this.applyPistonMovementRestriction(Direction.Axis.Y, pPos.y);
                return Math.abs(d1) <= 1.0E-5F ? Vec3.ZERO : new Vec3(0.0, d1, 0.0);
            } else if (pPos.z != 0.0) {
                double d0 = this.applyPistonMovementRestriction(Direction.Axis.Z, pPos.z);
                return Math.abs(d0) <= 1.0E-5F ? Vec3.ZERO : new Vec3(0.0, 0.0, d0);
            } else {
                return Vec3.ZERO;
            }
        }
    }

    private double applyPistonMovementRestriction(Direction.Axis pAxis, double pDistance) {
        int i = pAxis.ordinal();
        double d0 = Mth.clamp(pDistance + this.pistonDeltas[i], -0.51, 0.51);
        pDistance = d0 - this.pistonDeltas[i];
        this.pistonDeltas[i] = d0;
        return pDistance;
    }

    private Vec3 collide(Vec3 pVec) {
        AABB aabb = this.getBoundingBox();
        List<VoxelShape> list = this.level().getEntityCollisions(this, aabb.expandTowards(pVec));
        Vec3 vec3 = pVec.lengthSqr() == 0.0 ? pVec : collideBoundingBox(this, pVec, aabb, this.level(), list);
        boolean flag = pVec.x != vec3.x;
        boolean flag1 = pVec.y != vec3.y;
        boolean flag2 = pVec.z != vec3.z;
        boolean flag3 = flag1 && pVec.y < 0.0;
        if (this.maxUpStep() > 0.0F && (flag3 || this.onGround()) && (flag || flag2)) {
            AABB aabb1 = flag3 ? aabb.move(0.0, vec3.y, 0.0) : aabb;
            AABB aabb2 = aabb1.expandTowards(pVec.x, this.maxUpStep(), pVec.z);
            if (!flag3) {
                aabb2 = aabb2.expandTowards(0.0, -1.0E-5F, 0.0);
            }

            List<VoxelShape> list1 = collectColliders(this, this.level, list, aabb2);
            float f = (float)vec3.y;
            float[] afloat = collectCandidateStepUpHeights(aabb1, list1, this.maxUpStep(), f);

            for (float f1 : afloat) {
                Vec3 vec31 = collideWithShapes(new Vec3(pVec.x, f1, pVec.z), aabb1, list1);
                if (vec31.horizontalDistanceSqr() > vec3.horizontalDistanceSqr()) {
                    double d0 = aabb.minY - aabb1.minY;
                    return vec31.subtract(0.0, d0, 0.0);
                }
            }
        }

        return vec3;
    }

    private static float[] collectCandidateStepUpHeights(AABB pBox, List<VoxelShape> pColliders, float pDeltaY, float pMaxUpStep) {
        FloatSet floatset = new FloatArraySet(4);

        for (VoxelShape voxelshape : pColliders) {
            for (double d0 : voxelshape.getCoords(Direction.Axis.Y)) {
                float f = (float)(d0 - pBox.minY);
                if (!(f < 0.0F) && f != pMaxUpStep) {
                    if (f > pDeltaY) {
                        break;
                    }

                    floatset.add(f);
                }
            }
        }

        float[] afloat = floatset.toFloatArray();
        FloatArrays.unstableSort(afloat);
        return afloat;
    }

    public static Vec3 collideBoundingBox(@Nullable Entity pEntity, Vec3 pVec, AABB pCollisionBox, Level pLevel, List<VoxelShape> pPotentialHits) {
        List<VoxelShape> list = collectColliders(pEntity, pLevel, pPotentialHits, pCollisionBox.expandTowards(pVec));
        return collideWithShapes(pVec, pCollisionBox, list);
    }

    private static List<VoxelShape> collectColliders(@Nullable Entity pEntity, Level pLevel, List<VoxelShape> pCollisions, AABB pBoundingBox) {
        Builder<VoxelShape> builder = ImmutableList.builderWithExpectedSize(pCollisions.size() + 1);
        if (!pCollisions.isEmpty()) {
            builder.addAll(pCollisions);
        }

        WorldBorder worldborder = pLevel.getWorldBorder();
        boolean flag = pEntity != null && worldborder.isInsideCloseToBorder(pEntity, pBoundingBox);
        if (flag) {
            builder.add(worldborder.getCollisionShape());
        }

        builder.addAll(pLevel.getBlockCollisions(pEntity, pBoundingBox));
        return builder.build();
    }

    private static Vec3 collideWithShapes(Vec3 pDeltaMovement, AABB pEntityBB, List<VoxelShape> pShapes) {
        if (pShapes.isEmpty()) {
            return pDeltaMovement;
        } else {
            Vec3 vec3 = Vec3.ZERO;

            for (Direction.Axis direction$axis : axisStepOrder(pDeltaMovement)) {
                double d0 = pDeltaMovement.get(direction$axis);
                if (d0 != 0.0) {
                    double d1 = Shapes.collide(direction$axis, pEntityBB.move(vec3), pShapes, d0);
                    vec3 = vec3.with(direction$axis, d1);
                }
            }

            return vec3;
        }
    }

    private static Iterable<Direction.Axis> axisStepOrder(Vec3 pDeltaMovement) {
        return Math.abs(pDeltaMovement.x) < Math.abs(pDeltaMovement.z) ? YZX_AXIS_ORDER : YXZ_AXIS_ORDER;
    }

    protected float nextStep() {
        return (int)this.moveDist + 1;
    }

    protected SoundEvent getSwimSound() {
        return SoundEvents.GENERIC_SWIM;
    }

    protected SoundEvent getSwimSplashSound() {
        return SoundEvents.GENERIC_SPLASH;
    }

    protected SoundEvent getSwimHighSpeedSplashSound() {
        return SoundEvents.GENERIC_SPLASH;
    }

    private void checkInsideBlocks(List<Entity.Movement> pMovements, InsideBlockEffectApplier.StepBasedCollector pStepBasedCollector) {
        if (this.isAffectedByBlocks()) {
            LongSet longset = this.visitedBlocks;

            for (Entity.Movement entity$movement : pMovements) {
                Vec3 vec3 = entity$movement.from();
                Vec3 vec31 = entity$movement.to();
                AABB aabb = this.makeBoundingBox(vec31).deflate(1.0E-5F);
                BlockGetter.forEachBlockIntersectedBetween(
                    vec3,
                    vec31,
                    aabb,
                    (p_390481_, p_390482_) -> {
                        if (this.isAlive()) {
                            BlockState blockstate = this.level().getBlockState(p_390481_);
                            if (!blockstate.isAir()) {
                                if (longset.add(p_390481_.asLong())) {
                                    VoxelShape voxelshape = blockstate.getEntityInsideCollisionShape(this.level(), p_390481_, this);
                                    boolean flag = voxelshape == Shapes.block()
                                        || this.collidedWithShapeMovingFrom(vec3, vec31, voxelshape.move(new Vec3(p_390481_)).toAabbs());
                                    if (flag) {
                                        try {
                                            pStepBasedCollector.advanceStep(p_390482_);
                                            blockstate.entityInside(this.level(), p_390481_, this, pStepBasedCollector);
                                            this.onInsideBlock(blockstate);
                                        } catch (Throwable throwable) {
                                            CrashReport crashreport = CrashReport.forThrowable(throwable, "Colliding entity with block");
                                            CrashReportCategory crashreportcategory = crashreport.addCategory("Block being collided with");
                                            CrashReportCategory.populateBlockDetails(crashreportcategory, this.level(), p_390481_, blockstate);
                                            CrashReportCategory crashreportcategory1 = crashreport.addCategory("Entity being checked for collision");
                                            this.fillCrashReportCategory(crashreportcategory1);
                                            throw new ReportedException(crashreport);
                                        }
                                    }

                                    boolean flag1 = this.collidedWithFluid(blockstate.getFluidState(), p_390481_, vec3, vec31);
                                    if (flag1) {
                                        pStepBasedCollector.advanceStep(p_390482_);
                                        blockstate.getFluidState().entityInside(this.level(), p_390481_, this, pStepBasedCollector);
                                    }
                                }
                            }
                        }
                    }
                );
            }

            longset.clear();
        }
    }

    private boolean collidedWithFluid(FluidState pFluid, BlockPos pPos, Vec3 pFrom, Vec3 pTo) {
        AABB aabb = pFluid.getAABB(this.level(), pPos);
        return aabb != null && this.collidedWithShapeMovingFrom(pFrom, pTo, List.of(aabb));
    }

    private boolean collidedWithShapeMovingFrom(Vec3 pFrom, Vec3 pTo, List<AABB> pBoxes) {
        AABB aabb = this.makeBoundingBox(pFrom);
        Vec3 vec3 = pTo.subtract(pFrom);
        return aabb.collidedAlongVector(vec3, pBoxes);
    }

    protected void onInsideBlock(BlockState pState) {
    }

    public BlockPos adjustSpawnLocation(ServerLevel pLevel, BlockPos pPos) {
        BlockPos blockpos = pLevel.getSharedSpawnPos();
        Vec3 vec3 = blockpos.getCenter();
        int i = pLevel.getChunkAt(blockpos).getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, blockpos.getX(), blockpos.getZ()) + 1;
        return BlockPos.containing(vec3.x, i, vec3.z);
    }

    public void gameEvent(Holder<GameEvent> pGameEvent, @Nullable Entity pEntity) {
        this.level().gameEvent(pEntity, pGameEvent, this.position);
    }

    public void gameEvent(Holder<GameEvent> pGameEvent) {
        this.gameEvent(pGameEvent, this);
    }

    private void walkingStepSound(BlockPos pPos, BlockState pState) {
        this.playStepSound(pPos, pState);
        if (this.shouldPlayAmethystStepSound(pState)) {
            this.playAmethystStepSound();
        }
    }

    protected void waterSwimSound() {
        Entity entity = Objects.requireNonNullElse(this.getControllingPassenger(), this);
        float f = entity == this ? 0.35F : 0.4F;
        Vec3 vec3 = entity.getDeltaMovement();
        float f1 = Math.min(
            1.0F, (float)Math.sqrt(vec3.x * vec3.x * 0.2F + vec3.y * vec3.y + vec3.z * vec3.z * 0.2F) * f
        );
        this.playSwimSound(f1);
    }

    protected BlockPos getPrimaryStepSoundBlockPos(BlockPos pPos) {
        BlockPos blockpos = pPos.above();
        BlockState blockstate = this.level().getBlockState(blockpos);
        return !blockstate.is(BlockTags.INSIDE_STEP_SOUND_BLOCKS) && !blockstate.is(BlockTags.COMBINATION_STEP_SOUND_BLOCKS) ? pPos : blockpos;
    }

    protected void playCombinationStepSounds(BlockState pPrimaryState, BlockState pSecondaryState, BlockPos primaryPos, BlockPos secondaryPos) {
        SoundType soundtype = pPrimaryState.getSoundType(this.level(), primaryPos, this);
        this.playSound(soundtype.getStepSound(), soundtype.getVolume() * 0.15F, soundtype.getPitch());
        this.playMuffledStepSound(pSecondaryState, secondaryPos);
    }

    protected void playMuffledStepSound(BlockState pState, BlockPos pos) {
        SoundType soundtype = pState.getSoundType(this.level(), pos, this);
        this.playSound(soundtype.getStepSound(), soundtype.getVolume() * 0.05F, soundtype.getPitch() * 0.8F);
    }

    protected void playStepSound(BlockPos pPos, BlockState pState) {
        SoundType soundtype = pState.getSoundType(this.level(), pPos, this);
        this.playSound(soundtype.getStepSound(), soundtype.getVolume() * 0.15F, soundtype.getPitch());
    }

    private boolean shouldPlayAmethystStepSound(BlockState pState) {
        return pState.is(BlockTags.CRYSTAL_SOUND_BLOCKS) && this.tickCount >= this.lastCrystalSoundPlayTick + 20;
    }

    private void playAmethystStepSound() {
        this.crystalSoundIntensity = this.crystalSoundIntensity * (float)Math.pow(0.997, this.tickCount - this.lastCrystalSoundPlayTick);
        this.crystalSoundIntensity = Math.min(1.0F, this.crystalSoundIntensity + 0.07F);
        float f = 0.5F + this.crystalSoundIntensity * this.random.nextFloat() * 1.2F;
        float f1 = 0.1F + this.crystalSoundIntensity * 1.2F;
        this.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, f1, f);
        this.lastCrystalSoundPlayTick = this.tickCount;
    }

    protected void playSwimSound(float pVolume) {
        this.playSound(this.getSwimSound(), pVolume, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.4F);
    }

    protected void onFlap() {
    }

    protected boolean isFlapping() {
        return false;
    }

    public void playSound(SoundEvent pSound, float pVolume, float pPitch) {
        if (!this.isSilent()) {
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(), pSound, this.getSoundSource(), pVolume, pPitch);
        }
    }

    public void playSound(SoundEvent pSound) {
        if (!this.isSilent()) {
            this.playSound(pSound, 1.0F, 1.0F);
        }
    }

    public boolean isSilent() {
        return this.entityData.get(DATA_SILENT);
    }

    public void setSilent(boolean pIsSilent) {
        this.entityData.set(DATA_SILENT, pIsSilent);
    }

    public boolean isNoGravity() {
        return this.entityData.get(DATA_NO_GRAVITY);
    }

    public void setNoGravity(boolean pNoGravity) {
        this.entityData.set(DATA_NO_GRAVITY, pNoGravity);
    }

    protected double getDefaultGravity() {
        return 0.0;
    }

    public final double getGravity() {
        return this.isNoGravity() ? 0.0 : this.getDefaultGravity();
    }

    protected void applyGravity() {
        double d0 = this.getGravity();
        if (d0 != 0.0) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, -d0, 0.0));
        }
    }

    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.ALL;
    }

    public boolean dampensVibrations() {
        return false;
    }

    public final void doCheckFallDamage(double pX, double pY, double pZ, boolean pOnGround) {
        if (!this.touchingUnloadedChunk()) {
            this.checkSupportingBlock(pOnGround, new Vec3(pX, pY, pZ));
            BlockPos blockpos = this.getOnPosLegacy();
            BlockState blockstate = this.level().getBlockState(blockpos);
            this.checkFallDamage(pY, pOnGround, blockstate, blockpos);
        }
    }

    protected void checkFallDamage(double pY, boolean pOnGround, BlockState pState, BlockPos pPos) {
        if (!this.isInWater() && pY < 0.0) {
            this.fallDistance -= (float)pY;
        }

        if (pOnGround) {
            if (this.fallDistance > 0.0) {
                pState.getBlock().fallOn(this.level(), pState, pPos, this, this.fallDistance);
                this.level()
                    .gameEvent(
                        GameEvent.HIT_GROUND,
                        this.position,
                        GameEvent.Context.of(this, this.mainSupportingBlockPos.<BlockState>map(p_286200_ -> this.level().getBlockState(p_286200_)).orElse(pState))
                    );
            }

            this.resetFallDistance();
        }
    }

    public boolean fireImmune() {
        return this.getType().fireImmune();
    }

    public boolean causeFallDamage(double pFallDistance, float pDamageMultiplier, DamageSource pDamageSource) {
        if (this.type.is(EntityTypeTags.FALL_DAMAGE_IMMUNE)) {
            return false;
        } else {
            this.propagateFallToPassengers(pFallDistance, pDamageMultiplier, pDamageSource);
            return false;
        }
    }

    protected void propagateFallToPassengers(double pFallDistance, float pDamageMultiplier, DamageSource pDamageSource) {
        if (this.isVehicle()) {
            for (Entity entity : this.getPassengers()) {
                entity.causeFallDamage(pFallDistance, pDamageMultiplier, pDamageSource);
            }
        }
    }

    public boolean isInWater() {
        return this.wasTouchingWater;
    }

    public boolean isInWaterOrSwimmable() {
        return this.isInWater() || isInFluidType((fluidType, height) -> canSwimInFluidType(fluidType));
    }

    boolean isInRain() {
        BlockPos blockpos = this.blockPosition();
        return this.level().isRainingAt(blockpos)
            || this.level().isRainingAt(BlockPos.containing(blockpos.getX(), this.getBoundingBox().maxY, blockpos.getZ()));
    }

    public boolean isInWaterOrRain() {
        return this.isInWater() || this.isInRain();
    }

    public boolean isInLiquid() {
        return this.isInWater() || this.isInLava();
    }

    public boolean isUnderWater() {
        return this.wasEyeInWater && this.isInWater();
    }

    public void updateSwimming() {
        if (this.isSwimming()) {
            this.setSwimming(this.isSprinting() && this.isInWaterOrSwimmable() && !this.isPassenger());
        } else {
            this.setSwimming(this.isSprinting() && (this.isUnderWater() || this.canStartSwimming()) && !this.isPassenger());
        }
    }

    protected boolean updateInWaterStateAndDoFluidPushing() {
        this.fluidHeight.clear();
        this.forgeFluidTypeHeight.clear();
        this.updateInWaterStateAndDoWaterCurrentPushing();
        if (!(this.getVehicle() instanceof AbstractBoat)) {
           this.fallDistance *= this.forgeFluidTypeHeight.object2DoubleEntrySet().stream().filter(e -> !e.getKey().isAir() && !e.getKey().isVanilla()).map(e -> this.getFluidFallDistanceModifier(e.getKey())).min(Float::compare).orElse(1F);
           if (this.isInFluidType((fluidType, height) -> !fluidType.isAir() && !fluidType.isVanilla() && this.canFluidExtinguish(fluidType))) this.clearFire();
        }
        return this.isInFluidType();
    }

    void updateInWaterStateAndDoWaterCurrentPushing() {
        java.util.function.BooleanSupplier updateFluidHeight = () -> this.updateFluidHeightAndDoFluidPushing(FluidTags.WATER, 0.014D);
        if (this.getVehicle() instanceof AbstractBoat abstractboat && !abstractboat.isUnderWater()) {
            updateFluidHeight = () -> {
                this.updateFluidHeightAndDoFluidPushing(state -> this.shouldUpdateFluidWhileBoating(state, abstractboat));
                return false;
            };
        }
        if (updateFluidHeight.getAsBoolean()) {
            if (!this.wasTouchingWater && !this.firstTick) {
                this.doWaterSplashEffect();
            }

            this.resetFallDistance();
            this.wasTouchingWater = true;
        } else {
            this.wasTouchingWater = false;
        }
    }

    private void updateFluidOnEyes() {
        this.wasEyeInWater = this.isEyeInFluid(FluidTags.WATER);
        this.fluidOnEyes.clear();
        this.forgeFluidTypeOnEyes = net.minecraftforge.common.ForgeMod.EMPTY_TYPE.get();
        double d0 = this.getEyeY();
        if (!(
            this.getVehicle() instanceof AbstractBoat abstractboat
                && !abstractboat.isUnderWater()
                && abstractboat.getBoundingBox().maxY >= d0
                && abstractboat.getBoundingBox().minY <= d0
        )) {
            BlockPos blockpos = BlockPos.containing(this.getX(), d0, this.getZ());
            FluidState fluidstate = this.level().getFluidState(blockpos);
            double d1 = blockpos.getY() + fluidstate.getHeight(this.level(), blockpos);
            if (d1 > d0) {
                this.forgeFluidTypeOnEyes = fluidstate.getFluidType();
            }
        }
    }

    protected void doWaterSplashEffect() {
        Entity entity = Objects.requireNonNullElse(this.getControllingPassenger(), this);
        float f = entity == this ? 0.2F : 0.9F;
        Vec3 vec3 = entity.getDeltaMovement();
        float f1 = Math.min(
            1.0F, (float)Math.sqrt(vec3.x * vec3.x * 0.2F + vec3.y * vec3.y + vec3.z * vec3.z * 0.2F) * f
        );
        if (f1 < 0.25F) {
            this.playSound(this.getSwimSplashSound(), f1, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.4F);
        } else {
            this.playSound(this.getSwimHighSpeedSplashSound(), f1, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.4F);
        }

        float f2 = Mth.floor(this.getY());

        for (int i = 0; i < 1.0F + this.dimensions.width() * 20.0F; i++) {
            double d0 = (this.random.nextDouble() * 2.0 - 1.0) * this.dimensions.width();
            double d1 = (this.random.nextDouble() * 2.0 - 1.0) * this.dimensions.width();
            this.level()
                .addParticle(
                    ParticleTypes.BUBBLE,
                    this.getX() + d0,
                    f2 + 1.0F,
                    this.getZ() + d1,
                    vec3.x,
                    vec3.y - this.random.nextDouble() * 0.2F,
                    vec3.z
                );
        }

        for (int j = 0; j < 1.0F + this.dimensions.width() * 20.0F; j++) {
            double d2 = (this.random.nextDouble() * 2.0 - 1.0) * this.dimensions.width();
            double d3 = (this.random.nextDouble() * 2.0 - 1.0) * this.dimensions.width();
            this.level().addParticle(ParticleTypes.SPLASH, this.getX() + d2, f2 + 1.0F, this.getZ() + d3, vec3.x, vec3.y, vec3.z);
        }

        this.gameEvent(GameEvent.SPLASH);
    }

    @Deprecated
    protected BlockState getBlockStateOnLegacy() {
        return this.level().getBlockState(this.getOnPosLegacy());
    }

    public BlockState getBlockStateOn() {
        return this.level().getBlockState(this.getOnPos());
    }

    public boolean canSpawnSprintParticle() {
        return this.isSprinting() && !this.isInWater() && !this.isSpectator() && !this.isCrouching() && !this.isInLava() && this.isAlive() && !this.isInFluidType();
    }

    protected void spawnSprintParticle() {
        BlockPos blockpos = this.getOnPosLegacy();
        BlockState blockstate = this.level().getBlockState(blockpos);
        if (!blockstate.addRunningEffects(level, blockpos, this))
        if (blockstate.getRenderShape() != RenderShape.INVISIBLE) {
            Vec3 vec3 = this.getDeltaMovement();
            BlockPos blockpos1 = this.blockPosition();
            double d0 = this.getX() + (this.random.nextDouble() - 0.5) * this.dimensions.width();
            double d1 = this.getZ() + (this.random.nextDouble() - 0.5) * this.dimensions.width();
            if (blockpos1.getX() != blockpos.getX()) {
                d0 = Mth.clamp(d0, blockpos.getX(), blockpos.getX() + 1.0);
            }

            if (blockpos1.getZ() != blockpos.getZ()) {
                d1 = Mth.clamp(d1, blockpos.getZ(), blockpos.getZ() + 1.0);
            }

            this.level()
                .addParticle(
                    new BlockParticleOption(ParticleTypes.BLOCK, blockstate).setPos(blockpos),
                    d0,
                    this.getY() + 0.1,
                    d1,
                    vec3.x * -4.0,
                    1.5,
                    vec3.z * -4.0
                );
        }
    }

    @Deprecated // Forge: Use isEyeInFluidType instead
    public boolean isEyeInFluid(TagKey<Fluid> pFluidTag) {
        if (pFluidTag == FluidTags.WATER) return this.isEyeInFluidType(net.minecraftforge.common.ForgeMod.WATER_TYPE.get());
        else if (pFluidTag == FluidTags.LAVA) return this.isEyeInFluidType(net.minecraftforge.common.ForgeMod.LAVA_TYPE.get());
        return this.fluidOnEyes.contains(pFluidTag);
    }

    public boolean isInLava() {
        return !this.firstTick && this.forgeFluidTypeHeight.getDouble(net.minecraftforge.common.ForgeMod.LAVA_TYPE.get()) > 0.0;
    }

    public void moveRelative(float pAmount, Vec3 pRelative) {
        Vec3 vec3 = getInputVector(pRelative, pAmount, this.getYRot());
        this.setDeltaMovement(this.getDeltaMovement().add(vec3));
    }

    protected static Vec3 getInputVector(Vec3 pRelative, float pMotionScaler, float pFacing) {
        double d0 = pRelative.lengthSqr();
        if (d0 < 1.0E-7) {
            return Vec3.ZERO;
        } else {
            Vec3 vec3 = (d0 > 1.0 ? pRelative.normalize() : pRelative).scale(pMotionScaler);
            float f = Mth.sin(pFacing * (float) (Math.PI / 180.0));
            float f1 = Mth.cos(pFacing * (float) (Math.PI / 180.0));
            return new Vec3(vec3.x * f1 - vec3.z * f, vec3.y, vec3.z * f1 + vec3.x * f);
        }
    }

    @Deprecated
    public float getLightLevelDependentMagicValue() {
        return this.level().hasChunkAt(this.getBlockX(), this.getBlockZ())
            ? this.level().getLightLevelDependentMagicValue(BlockPos.containing(this.getX(), this.getEyeY(), this.getZ()))
            : 0.0F;
    }

    public void absSnapTo(double pX, double pY, double pZ, float pYRot, float pXRot) {
        this.absSnapTo(pX, pY, pZ);
        this.absSnapRotationTo(pYRot, pXRot);
    }

    public void absSnapRotationTo(float pYRot, float pXRot) {
        this.setYRot(pYRot % 360.0F);
        this.setXRot(Mth.clamp(pXRot, -90.0F, 90.0F) % 360.0F);
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }

    public void absSnapTo(double pX, double pY, double pZ) {
        double d0 = Mth.clamp(pX, -3.0E7, 3.0E7);
        double d1 = Mth.clamp(pZ, -3.0E7, 3.0E7);
        this.xo = d0;
        this.yo = pY;
        this.zo = d1;
        this.setPos(d0, pY, d1);
    }

    public void snapTo(Vec3 pPos) {
        this.snapTo(pPos.x, pPos.y, pPos.z);
    }

    public void snapTo(double pX, double pY, double pZ) {
        this.snapTo(pX, pY, pZ, this.getYRot(), this.getXRot());
    }

    public void snapTo(BlockPos pPos, float pYRot, float pXRot) {
        this.snapTo(pPos.getBottomCenter(), pYRot, pXRot);
    }

    public void snapTo(Vec3 pPos, float pYRot, float pXRot) {
        this.snapTo(pPos.x, pPos.y, pPos.z, pYRot, pXRot);
    }

    public void snapTo(double pX, double pY, double pZ, float pYRot, float pXRot) {
        this.setPosRaw(pX, pY, pZ);
        this.setYRot(pYRot);
        this.setXRot(pXRot);
        this.setOldPosAndRot();
        this.reapplyPosition();
    }

    public final void setOldPosAndRot() {
        this.setOldPos();
        this.setOldRot();
    }

    public final void setOldPosAndRot(Vec3 pPos, float pYRot, float pXRot) {
        this.setOldPos(pPos);
        this.setOldRot(pYRot, pXRot);
    }

    protected void setOldPos() {
        this.setOldPos(this.position);
    }

    public void setOldRot() {
        this.setOldRot(this.getYRot(), this.getXRot());
    }

    private void setOldPos(Vec3 pPos) {
        this.xo = this.xOld = pPos.x;
        this.yo = this.yOld = pPos.y;
        this.zo = this.zOld = pPos.z;
    }

    private void setOldRot(float pYRot, float pXRot) {
        this.yRotO = pYRot;
        this.xRotO = pXRot;
    }

    public final Vec3 oldPosition() {
        return new Vec3(this.xOld, this.yOld, this.zOld);
    }

    public float distanceTo(Entity pEntity) {
        float f = (float)(this.getX() - pEntity.getX());
        float f1 = (float)(this.getY() - pEntity.getY());
        float f2 = (float)(this.getZ() - pEntity.getZ());
        return Mth.sqrt(f * f + f1 * f1 + f2 * f2);
    }

    public double distanceToSqr(double pX, double pY, double pZ) {
        double d0 = this.getX() - pX;
        double d1 = this.getY() - pY;
        double d2 = this.getZ() - pZ;
        return d0 * d0 + d1 * d1 + d2 * d2;
    }

    public double distanceToSqr(Entity pEntity) {
        return this.distanceToSqr(pEntity.position());
    }

    public double distanceToSqr(Vec3 pVec) {
        double d0 = this.getX() - pVec.x;
        double d1 = this.getY() - pVec.y;
        double d2 = this.getZ() - pVec.z;
        return d0 * d0 + d1 * d1 + d2 * d2;
    }

    public void playerTouch(Player pPlayer) {
    }

    public void push(Entity pEntity) {
        if (!this.isPassengerOfSameVehicle(pEntity)) {
            if (!pEntity.noPhysics && !this.noPhysics) {
                double d0 = pEntity.getX() - this.getX();
                double d1 = pEntity.getZ() - this.getZ();
                double d2 = Mth.absMax(d0, d1);
                if (d2 >= 0.01F) {
                    d2 = Math.sqrt(d2);
                    d0 /= d2;
                    d1 /= d2;
                    double d3 = 1.0 / d2;
                    if (d3 > 1.0) {
                        d3 = 1.0;
                    }

                    d0 *= d3;
                    d1 *= d3;
                    d0 *= 0.05F;
                    d1 *= 0.05F;
                    if (!this.isVehicle() && this.isPushable()) {
                        this.push(-d0, 0.0, -d1);
                    }

                    if (!pEntity.isVehicle() && pEntity.isPushable()) {
                        pEntity.push(d0, 0.0, d1);
                    }
                }
            }
        }
    }

    public void push(Vec3 pVector) {
        this.push(pVector.x, pVector.y, pVector.z);
    }

    public void push(double pX, double pY, double pZ) {
        this.setDeltaMovement(this.getDeltaMovement().add(pX, pY, pZ));
        this.hasImpulse = true;
    }

    protected void markHurt() {
        this.hurtMarked = true;
    }

    @Deprecated
    public final void hurt(DamageSource pDamageSource, float pAmount) {
        if (this.level instanceof ServerLevel serverlevel) {
            this.hurtServer(serverlevel, pDamageSource, pAmount);
        }
    }

    @Deprecated
    public final boolean hurtOrSimulate(DamageSource pDamageSource, float pAmount) {
        return this.level instanceof ServerLevel serverlevel ? this.hurtServer(serverlevel, pDamageSource, pAmount) : this.hurtClient(pDamageSource);
    }

    public abstract boolean hurtServer(ServerLevel pLevel, DamageSource pDamageSource, float pAmount);

    public boolean hurtClient(DamageSource pDamageSource) {
        return false;
    }

    public final Vec3 getViewVector(float pPartialTicks) {
        return this.calculateViewVector(this.getViewXRot(pPartialTicks), this.getViewYRot(pPartialTicks));
    }

    public Direction getNearestViewDirection() {
        return Direction.getApproximateNearest(this.getViewVector(1.0F));
    }

    public float getViewXRot(float pPartialTicks) {
        return this.getXRot(pPartialTicks);
    }

    public float getViewYRot(float pPartialTick) {
        return this.getYRot(pPartialTick);
    }

    public float getXRot(float pPartialTick) {
        return pPartialTick == 1.0F ? this.getXRot() : Mth.lerp(pPartialTick, this.xRotO, this.getXRot());
    }

    public float getYRot(float pPartialTick) {
        return pPartialTick == 1.0F ? this.getYRot() : Mth.rotLerp(pPartialTick, this.yRotO, this.getYRot());
    }

    public final Vec3 calculateViewVector(float pXRot, float pYRot) {
        float f = pXRot * (float) (Math.PI / 180.0);
        float f1 = -pYRot * (float) (Math.PI / 180.0);
        float f2 = Mth.cos(f1);
        float f3 = Mth.sin(f1);
        float f4 = Mth.cos(f);
        float f5 = Mth.sin(f);
        return new Vec3(f3 * f4, -f5, f2 * f4);
    }

    public final Vec3 getUpVector(float pPartialTick) {
        return this.calculateUpVector(this.getViewXRot(pPartialTick), this.getViewYRot(pPartialTick));
    }

    protected final Vec3 calculateUpVector(float pXRot, float pYRot) {
        return this.calculateViewVector(pXRot - 90.0F, pYRot);
    }

    public final Vec3 getEyePosition() {
        return new Vec3(this.getX(), this.getEyeY(), this.getZ());
    }

    public final Vec3 getEyePosition(float pPartialTick) {
        double d0 = Mth.lerp(pPartialTick, this.xo, this.getX());
        double d1 = Mth.lerp(pPartialTick, this.yo, this.getY()) + this.getEyeHeight();
        double d2 = Mth.lerp(pPartialTick, this.zo, this.getZ());
        return new Vec3(d0, d1, d2);
    }

    public Vec3 getLightProbePosition(float pPartialTicks) {
        return this.getEyePosition(pPartialTicks);
    }

    public final Vec3 getPosition(float pPartialTicks) {
        double d0 = Mth.lerp(pPartialTicks, this.xo, this.getX());
        double d1 = Mth.lerp(pPartialTicks, this.yo, this.getY());
        double d2 = Mth.lerp(pPartialTicks, this.zo, this.getZ());
        return new Vec3(d0, d1, d2);
    }

    public HitResult pick(double pHitDistance, float pPartialTicks, boolean pHitFluids) {
        Vec3 vec3 = this.getEyePosition(pPartialTicks);
        Vec3 vec31 = this.getViewVector(pPartialTicks);
        Vec3 vec32 = vec3.add(vec31.x * pHitDistance, vec31.y * pHitDistance, vec31.z * pHitDistance);
        return this.level()
            .clip(new ClipContext(vec3, vec32, ClipContext.Block.OUTLINE, pHitFluids ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE, this));
    }

    public boolean canBeHitByProjectile() {
        return this.isAlive() && this.isPickable();
    }

    public boolean isPickable() {
        return false;
    }

    public boolean isPushable() {
        return false;
    }

    public void awardKillScore(Entity pEntity, DamageSource pDamageSource) {
        if (pEntity instanceof ServerPlayer) {
            CriteriaTriggers.ENTITY_KILLED_PLAYER.trigger((ServerPlayer)pEntity, this, pDamageSource);
        }
    }

    public boolean shouldRender(double pX, double pY, double pZ) {
        double d0 = this.getX() - pX;
        double d1 = this.getY() - pY;
        double d2 = this.getZ() - pZ;
        double d3 = d0 * d0 + d1 * d1 + d2 * d2;
        return this.shouldRenderAtSqrDistance(d3);
    }

    public boolean shouldRenderAtSqrDistance(double pDistance) {
        double d0 = this.getBoundingBox().getSize();
        if (Double.isNaN(d0)) {
            d0 = 1.0;
        }

        d0 *= 64.0 * viewScale;
        return pDistance < d0 * d0;
    }

    public boolean saveAsPassenger(CompoundTag pCompound) {
        if (this.removalReason != null && !this.removalReason.shouldSave()) {
            return false;
        } else {
            String s = this.getEncodeId();
            if (s == null) {
                return false;
            } else {
                pCompound.putString("id", s);
                this.saveWithoutId(pCompound);
                return true;
            }
        }
    }

    public boolean save(CompoundTag pCompound) {
        return this.isPassenger() ? false : this.saveAsPassenger(pCompound);
    }

    public CompoundTag saveWithoutId(CompoundTag pCompound) {
        try {
            if (this.vehicle != null) {
                pCompound.store("Pos", Vec3.CODEC, new Vec3(this.vehicle.getX(), this.getY(), this.vehicle.getZ()));
            } else {
                pCompound.store("Pos", Vec3.CODEC, this.position());
            }

            pCompound.store("Motion", Vec3.CODEC, this.getDeltaMovement());
            pCompound.store("Rotation", Vec2.CODEC, new Vec2(this.getYRot(), this.getXRot()));
            pCompound.putDouble("fall_distance", this.fallDistance);
            pCompound.putShort("Fire", (short)this.remainingFireTicks);
            pCompound.putShort("Air", (short)this.getAirSupply());
            pCompound.putBoolean("OnGround", this.onGround());
            pCompound.putBoolean("Invulnerable", this.invulnerable);
            pCompound.putInt("PortalCooldown", this.portalCooldown);
            pCompound.store("UUID", UUIDUtil.CODEC, this.getUUID());
            Component component = this.getCustomName();
            if (component != null) {
                RegistryOps<Tag> registryops = this.registryAccess().createSerializationContext(NbtOps.INSTANCE);
                pCompound.store("CustomName", ComponentSerialization.CODEC, registryops, component);
            }

            if (this.isCustomNameVisible()) {
                pCompound.putBoolean("CustomNameVisible", this.isCustomNameVisible());
            }

            if (this.isSilent()) {
                pCompound.putBoolean("Silent", this.isSilent());
            }

            if (this.isNoGravity()) {
                pCompound.putBoolean("NoGravity", this.isNoGravity());
            }

            if (this.hasGlowingTag) {
                pCompound.putBoolean("Glowing", true);
            }

            int i = this.getTicksFrozen();
            if (i > 0) {
                pCompound.putInt("TicksFrozen", this.getTicksFrozen());
            }

            if (this.hasVisualFire) {
                pCompound.putBoolean("HasVisualFire", this.hasVisualFire);
            }

            pCompound.putBoolean("CanUpdate", canUpdate);
            CompoundTag caps = serializeCaps(this.registryAccess());
            if (caps != null) pCompound.put("ForgeCaps", caps);
            if (persistentData != null) pCompound.put("ForgeData", persistentData.copy());

            if (!this.tags.isEmpty()) {
                pCompound.store("Tags", TAG_LIST_CODEC, List.copyOf(this.tags));
            }

            if (!this.customData.isEmpty()) {
                pCompound.store("data", CustomData.CODEC, this.customData);
            }

            this.addAdditionalSaveData(pCompound);
            if (this.isVehicle()) {
                ListTag listtag = new ListTag();

                for (Entity entity : this.getPassengers()) {
                    CompoundTag compoundtag = new CompoundTag();
                    if (entity.saveAsPassenger(compoundtag)) {
                        listtag.add(compoundtag);
                    }
                }

                if (!listtag.isEmpty()) {
                    pCompound.put("Passengers", listtag);
                }
            }

            return pCompound;
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Saving entity NBT");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Entity being saved");
            this.fillCrashReportCategory(crashreportcategory);
            throw new ReportedException(crashreport);
        }
    }

    public void load(CompoundTag pCompound) {
        try {
            Vec3 vec3 = pCompound.read("Pos", Vec3.CODEC).orElse(Vec3.ZERO);
            Vec3 vec31 = pCompound.read("Motion", Vec3.CODEC).orElse(Vec3.ZERO);
            Vec2 vec2 = pCompound.read("Rotation", Vec2.CODEC).orElse(Vec2.ZERO);
            this.setDeltaMovement(
                Math.abs(vec31.x) > 10.0 ? 0.0 : vec31.x,
                Math.abs(vec31.y) > 10.0 ? 0.0 : vec31.y,
                Math.abs(vec31.z) > 10.0 ? 0.0 : vec31.z
            );
            this.hasImpulse = true;
            double d0 = 3.0000512E7;
            this.setPosRaw(
                Mth.clamp(vec3.x, -3.0000512E7, 3.0000512E7),
                Mth.clamp(vec3.y, -2.0E7, 2.0E7),
                Mth.clamp(vec3.z, -3.0000512E7, 3.0000512E7)
            );
            this.setYRot(vec2.x);
            this.setXRot(vec2.y);
            this.setOldPosAndRot();
            this.setYHeadRot(this.getYRot());
            this.setYBodyRot(this.getYRot());
            this.fallDistance = pCompound.getDoubleOr("fall_distance", 0.0);
            this.remainingFireTicks = pCompound.getShortOr("Fire", (short)0);
            this.setAirSupply(pCompound.getIntOr("Air", this.getMaxAirSupply()));
            this.onGround = pCompound.getBooleanOr("OnGround", false);
            this.invulnerable = pCompound.getBooleanOr("Invulnerable", false);
            this.portalCooldown = pCompound.getIntOr("PortalCooldown", 0);
            pCompound.read("UUID", UUIDUtil.CODEC).ifPresent(p_390483_ -> {
                this.uuid = p_390483_;
                this.stringUUID = this.uuid.toString();
            });
            if (!Double.isFinite(this.getX()) || !Double.isFinite(this.getY()) || !Double.isFinite(this.getZ())) {
                throw new IllegalStateException("Entity has invalid position");
            } else if (Double.isFinite(this.getYRot()) && Double.isFinite(this.getXRot())) {
                this.reapplyPosition();
                this.setRot(this.getYRot(), this.getXRot());
                RegistryOps<Tag> registryops = this.registryAccess().createSerializationContext(NbtOps.INSTANCE);
                this.setCustomName(pCompound.read("CustomName", ComponentSerialization.CODEC, registryops).orElse(null));
                this.setCustomNameVisible(pCompound.getBooleanOr("CustomNameVisible", false));
                this.setSilent(pCompound.getBooleanOr("Silent", false));
                this.setNoGravity(pCompound.getBooleanOr("NoGravity", false));
                this.setGlowingTag(pCompound.getBooleanOr("Glowing", false));
                this.setTicksFrozen(pCompound.getIntOr("TicksFrozen", 0));
                this.hasVisualFire = pCompound.getBooleanOr("HasVisualFire", false);
                pCompound.getCompound("ForgeData").ifPresent(data -> this.persistentData = data);
                pCompound.getBoolean("CanUpdate").ifPresent(this::canUpdate);
                pCompound.getCompound("ForgeCaps").ifPresent(caps -> this.deserializeCaps(this.registryAccess(), caps));
                this.customData = pCompound.read("data", CustomData.CODEC).orElse(CustomData.EMPTY);
                this.tags.clear();
                pCompound.read("Tags", TAG_LIST_CODEC).ifPresent(this.tags::addAll);
                this.readAdditionalSaveData(pCompound);
                if (this.repositionEntityAfterLoad()) {
                    this.reapplyPosition();
                }
            } else {
                throw new IllegalStateException("Entity has invalid rotation");
            }
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Loading entity NBT");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Entity being loaded");
            this.fillCrashReportCategory(crashreportcategory);
            throw new ReportedException(crashreport);
        }
    }

    protected boolean repositionEntityAfterLoad() {
        return true;
    }

    @Nullable
    public final String getEncodeId() {
        EntityType<?> entitytype = this.getType();
        ResourceLocation resourcelocation = EntityType.getKey(entitytype);
        return entitytype.canSerialize() && resourcelocation != null ? resourcelocation.toString() : null;
    }

    protected abstract void readAdditionalSaveData(CompoundTag pTag);

    protected abstract void addAdditionalSaveData(CompoundTag pTag);

    @Nullable
    public ItemEntity spawnAtLocation(ServerLevel pLevel, ItemLike pItem) {
        return this.spawnAtLocation(pLevel, pItem, 0);
    }

    @Nullable
    public ItemEntity spawnAtLocation(ServerLevel pLevel, ItemLike pItem, int pYOffset) {
        return this.spawnAtLocation(pLevel, new ItemStack(pItem), pYOffset);
    }

    @Nullable
    public ItemEntity spawnAtLocation(ServerLevel pLevel, ItemStack pStack) {
        return this.spawnAtLocation(pLevel, pStack, 0.0F);
    }

    @Nullable
    public ItemEntity spawnAtLocation(ServerLevel pLevel, ItemStack pStack, float pYOffset) {
        if (pStack.isEmpty()) {
            return null;
        } else {
            ItemEntity itementity = new ItemEntity(pLevel, this.getX(), this.getY() + pYOffset, this.getZ(), pStack);
            itementity.setDefaultPickUpDelay();
            if (captureDrops() != null) captureDrops().add(itementity);
            else
            pLevel.addFreshEntity(itementity);
            return itementity;
        }
    }

    public boolean isAlive() {
        return !this.isRemoved();
    }

    public boolean isInWall() {
        if (this.noPhysics) {
            return false;
        } else {
            float f = this.dimensions.width() * 0.8F;
            AABB aabb = AABB.ofSize(this.getEyePosition(), f, 1.0E-6, f);
            return BlockPos.betweenClosedStream(aabb)
                .anyMatch(
                    p_390485_ -> {
                        BlockState blockstate = this.level().getBlockState(p_390485_);
                        return !blockstate.isAir()
                            && blockstate.isSuffocating(this.level(), p_390485_)
                            && Shapes.joinIsNotEmpty(blockstate.getCollisionShape(this.level(), p_390485_).move(p_390485_), Shapes.create(aabb), BooleanOp.AND);
                    }
                );
        }
    }

    public InteractionResult interact(Player pPlayer, InteractionHand pHand) {
        if (this.isAlive() && this instanceof Leashable leashable) {
            if (leashable.getLeashHolder() == pPlayer) {
                if (!this.level().isClientSide()) {
                    if (pPlayer.hasInfiniteMaterials()) {
                        leashable.removeLeash();
                    } else {
                        leashable.dropLeash();
                    }

                    this.gameEvent(GameEvent.ENTITY_INTERACT, pPlayer);
                }

                return InteractionResult.SUCCESS.withoutItem();
            }

            ItemStack itemstack = pPlayer.getItemInHand(pHand);
            if (itemstack.is(Items.LEAD) && leashable.canHaveALeashAttachedToIt()) {
                if (!this.level().isClientSide()) {
                    leashable.setLeashedTo(pPlayer, true);
                }

                itemstack.shrink(1);
                return InteractionResult.SUCCESS;
            }
        }

        return InteractionResult.PASS;
    }

    public boolean canCollideWith(Entity pEntity) {
        return pEntity.canBeCollidedWith() && !this.isPassengerOfSameVehicle(pEntity);
    }

    public boolean canBeCollidedWith() {
        return false;
    }

    public void rideTick() {
        this.setDeltaMovement(Vec3.ZERO);
        if (canUpdate())
        this.tick();
        if (this.isPassenger()) {
            this.getVehicle().positionRider(this);
        }
    }

    public final void positionRider(Entity pPassenger) {
        if (this.hasPassenger(pPassenger)) {
            this.positionRider(pPassenger, Entity::setPos);
        }
    }

    protected void positionRider(Entity pPassenger, Entity.MoveFunction pCallback) {
        Vec3 vec3 = this.getPassengerRidingPosition(pPassenger);
        Vec3 vec31 = pPassenger.getVehicleAttachmentPoint(this);
        pCallback.accept(pPassenger, vec3.x - vec31.x, vec3.y - vec31.y, vec3.z - vec31.z);
    }

    public void onPassengerTurned(Entity pEntityToUpdate) {
    }

    public Vec3 getVehicleAttachmentPoint(Entity pEntity) {
        return this.getAttachments().get(EntityAttachment.VEHICLE, 0, this.yRot);
    }

    public Vec3 getPassengerRidingPosition(Entity pEntity) {
        return this.position().add(this.getPassengerAttachmentPoint(pEntity, this.dimensions, 1.0F));
    }

    protected Vec3 getPassengerAttachmentPoint(Entity pEntity, EntityDimensions pDimensions, float pPartialTick) {
        return getDefaultPassengerAttachmentPoint(this, pEntity, pDimensions.attachments());
    }

    protected static Vec3 getDefaultPassengerAttachmentPoint(Entity pVehicle, Entity pPassenger, EntityAttachments pAttachments) {
        int i = pVehicle.getPassengers().indexOf(pPassenger);
        return pAttachments.getClamped(EntityAttachment.PASSENGER, i, pVehicle.yRot);
    }

    public boolean startRiding(Entity pVehicle) {
        return this.startRiding(pVehicle, false);
    }

    public boolean showVehicleHealth() {
        return this instanceof LivingEntity;
    }

    public boolean startRiding(Entity pVehicle, boolean pForce) {
        if (pVehicle == this.vehicle) {
            return false;
        } else if (!pVehicle.couldAcceptPassenger()) {
            return false;
        } else if (!this.level().isClientSide() && !pVehicle.type.canSerialize()) {
            return false;
        } else {
            for (Entity entity = pVehicle; entity.vehicle != null; entity = entity.vehicle) {
                if (entity.vehicle == this) {
                    return false;
                }
            }

            if (!net.minecraftforge.event.ForgeEventFactory.canMountEntity(this, pVehicle, true)) return false;
            if (pForce || this.canRide(pVehicle) && pVehicle.canAddPassenger(this)) {
                if (this.isPassenger()) {
                    this.stopRiding();
                }

                this.setPose(Pose.STANDING);
                this.vehicle = pVehicle;
                this.vehicle.addPassenger(this);
                pVehicle.getIndirectPassengersStream()
                    .filter(p_185984_ -> p_185984_ instanceof ServerPlayer)
                    .forEach(p_185982_ -> CriteriaTriggers.START_RIDING_TRIGGER.trigger((ServerPlayer)p_185982_));
                return true;
            } else {
                return false;
            }
        }
    }

    protected boolean canRide(Entity pVehicle) {
        return !this.isShiftKeyDown() && this.boardingCooldown <= 0;
    }

    public void ejectPassengers() {
        for (int i = this.passengers.size() - 1; i >= 0; i--) {
            this.passengers.get(i).stopRiding();
        }
    }

    public void removeVehicle() {
        if (this.vehicle != null) {
            Entity entity = this.vehicle;
            if (!net.minecraftforge.event.ForgeEventFactory.canMountEntity(this, entity, false)) return;
            this.vehicle = null;
            entity.removePassenger(this);
        }
    }

    public void stopRiding() {
        this.removeVehicle();
    }

    protected void addPassenger(Entity pPassenger) {
        if (pPassenger.getVehicle() != this) {
            throw new IllegalStateException("Use x.startRiding(y), not y.addPassenger(x)");
        } else {
            if (this.passengers.isEmpty()) {
                this.passengers = ImmutableList.of(pPassenger);
            } else {
                List<Entity> list = Lists.newArrayList(this.passengers);
                if (!this.level().isClientSide && pPassenger instanceof Player && !(this.getFirstPassenger() instanceof Player)) {
                    list.add(0, pPassenger);
                } else {
                    list.add(pPassenger);
                }

                this.passengers = ImmutableList.copyOf(list);
            }

            this.gameEvent(GameEvent.ENTITY_MOUNT, pPassenger);
        }
    }

    protected void removePassenger(Entity pPassenger) {
        if (pPassenger.getVehicle() == this) {
            throw new IllegalStateException("Use x.stopRiding(y), not y.removePassenger(x)");
        } else {
            if (this.passengers.size() == 1 && this.passengers.get(0) == pPassenger) {
                this.passengers = ImmutableList.of();
            } else {
                this.passengers = this.passengers.stream().filter(p_344072_ -> p_344072_ != pPassenger).collect(ImmutableList.toImmutableList());
            }

            pPassenger.boardingCooldown = 60;
            this.gameEvent(GameEvent.ENTITY_DISMOUNT, pPassenger);
        }
    }

    protected boolean canAddPassenger(Entity pPassenger) {
        return this.passengers.isEmpty();
    }

    /** @deprecated Forge: Use {@link #canBeRiddenUnderFluidType(net.minecraftforge.fluids.FluidType, Entity) rider sensitive version} */
    @Deprecated
    protected boolean couldAcceptPassenger() {
        return true;
    }

    public final boolean isInterpolating() {
        return this.getInterpolation() != null && this.getInterpolation().hasActiveInterpolation();
    }

    public final void moveOrInterpolateTo(Vec3 pPos, float pYRot, float pXRot) {
        InterpolationHandler interpolationhandler = this.getInterpolation();
        if (interpolationhandler != null) {
            interpolationhandler.interpolateTo(pPos, pYRot, pXRot);
        } else {
            this.setPos(pPos);
            this.setRot(pYRot, pXRot);
        }
    }

    @Nullable
    public InterpolationHandler getInterpolation() {
        return null;
    }

    public void lerpHeadTo(float pYaw, int pPitch) {
        this.setYHeadRot(pYaw);
    }

    public float getPickRadius() {
        return 0.0F;
    }

    public Vec3 getLookAngle() {
        return this.calculateViewVector(this.getXRot(), this.getYRot());
    }

    public Vec3 getHandHoldingItemAngle(Item pItem) {
        if (!(this instanceof Player player)) {
            return Vec3.ZERO;
        } else {
            boolean flag = player.getOffhandItem().is(pItem) && !player.getMainHandItem().is(pItem);
            HumanoidArm humanoidarm = flag ? player.getMainArm().getOpposite() : player.getMainArm();
            return this.calculateViewVector(0.0F, this.getYRot() + (humanoidarm == HumanoidArm.RIGHT ? 80 : -80)).scale(0.5);
        }
    }

    public Vec2 getRotationVector() {
        return new Vec2(this.getXRot(), this.getYRot());
    }

    public Vec3 getForward() {
        return Vec3.directionFromRotation(this.getRotationVector());
    }

    public void setAsInsidePortal(Portal pPortal, BlockPos pPos) {
        if (this.isOnPortalCooldown()) {
            this.setPortalCooldown();
        } else {
            if (this.portalProcess == null || !this.portalProcess.isSamePortal(pPortal)) {
                this.portalProcess = new PortalProcessor(pPortal, pPos.immutable());
            } else if (!this.portalProcess.isInsidePortalThisTick()) {
                this.portalProcess.updateEntryPosition(pPos.immutable());
                this.portalProcess.setAsInsidePortalThisTick(true);
            }
        }
    }

    protected void handlePortal() {
        if (this.level() instanceof ServerLevel serverlevel) {
            this.processPortalCooldown();
            if (this.portalProcess != null) {
                if (this.portalProcess.processPortalTeleportation(serverlevel, this, this.canUsePortal(false))) {
                    ProfilerFiller profilerfiller = Profiler.get();
                    profilerfiller.push("portal");
                    this.setPortalCooldown();
                    TeleportTransition teleporttransition = this.portalProcess.getPortalDestination(serverlevel, this);
                    if (teleporttransition != null) {
                        ServerLevel serverlevel1 = teleporttransition.newLevel();
                        if (serverlevel.getServer().isLevelEnabled(serverlevel1)
                            && (serverlevel1.dimension() == serverlevel.dimension() || this.canTeleport(serverlevel, serverlevel1))) {
                            this.teleport(teleporttransition);
                        }
                    }

                    profilerfiller.pop();
                } else if (this.portalProcess.hasExpired()) {
                    this.portalProcess = null;
                }
            }
        }
    }

    public int getDimensionChangingDelay() {
        Entity entity = this.getFirstPassenger();
        return entity instanceof ServerPlayer ? entity.getDimensionChangingDelay() : 300;
    }

    public void lerpMotion(double pX, double pY, double pZ) {
        this.setDeltaMovement(pX, pY, pZ);
    }

    public void handleDamageEvent(DamageSource pDamageSource) {
    }

    public void handleEntityEvent(byte pId) {
        switch (pId) {
            case 53:
                HoneyBlock.showSlideParticles(this);
        }
    }

    public void animateHurt(float pYaw) {
    }

    public boolean isOnFire() {
        boolean flag = this.level() != null && this.level().isClientSide;
        return !this.fireImmune() && (this.remainingFireTicks > 0 || flag && this.getSharedFlag(0));
    }

    public boolean isPassenger() {
        return this.getVehicle() != null;
    }

    public boolean isVehicle() {
        return !this.passengers.isEmpty();
    }

    public boolean dismountsUnderwater() {
        return this.getType().is(EntityTypeTags.DISMOUNTS_UNDERWATER);
    }

    public boolean canControlVehicle() {
        return !this.getType().is(EntityTypeTags.NON_CONTROLLING_RIDER);
    }

    public void setShiftKeyDown(boolean pKeyDown) {
        this.setSharedFlag(1, pKeyDown);
    }

    public boolean isShiftKeyDown() {
        return this.getSharedFlag(1);
    }

    public boolean isSteppingCarefully() {
        return this.isShiftKeyDown();
    }

    public boolean isSuppressingBounce() {
        return this.isShiftKeyDown();
    }

    public boolean isDiscrete() {
        return this.isShiftKeyDown();
    }

    public boolean isDescending() {
        return this.isShiftKeyDown();
    }

    public boolean isCrouching() {
        return this.hasPose(Pose.CROUCHING);
    }

    public boolean isSprinting() {
        return this.getSharedFlag(3);
    }

    public void setSprinting(boolean pSprinting) {
        this.setSharedFlag(3, pSprinting);
    }

    public boolean isSwimming() {
        return this.getSharedFlag(4);
    }

    public boolean isVisuallySwimming() {
        return this.hasPose(Pose.SWIMMING);
    }

    public boolean isVisuallyCrawling() {
        return this.isVisuallySwimming() && !this.isInWaterOrSwimmable();
    }

    public void setSwimming(boolean pSwimming) {
        this.setSharedFlag(4, pSwimming);
    }

    public final boolean hasGlowingTag() {
        return this.hasGlowingTag;
    }

    public final void setGlowingTag(boolean pHasGlowingTag) {
        this.hasGlowingTag = pHasGlowingTag;
        this.setSharedFlag(6, this.isCurrentlyGlowing());
    }

    public boolean isCurrentlyGlowing() {
        return this.level().isClientSide() ? this.getSharedFlag(6) : this.hasGlowingTag;
    }

    public boolean isInvisible() {
        return this.getSharedFlag(5);
    }

    public boolean isInvisibleTo(Player pPlayer) {
        if (pPlayer.isSpectator()) {
            return false;
        } else {
            Team team = this.getTeam();
            return team != null && pPlayer != null && pPlayer.getTeam() == team && team.canSeeFriendlyInvisibles() ? false : this.isInvisible();
        }
    }

    public boolean isOnRails() {
        return false;
    }

    public void updateDynamicGameEventListener(BiConsumer<DynamicGameEventListener<?>, ServerLevel> pListenerConsumer) {
    }

    @Nullable
    public PlayerTeam getTeam() {
        return this.level().getScoreboard().getPlayersTeam(this.getScoreboardName());
    }

    public final boolean isAlliedTo(@Nullable Entity pEntity) {
        return pEntity == null ? false : this == pEntity || this.considersEntityAsAlly(pEntity) || pEntity.considersEntityAsAlly(this);
    }

    protected boolean considersEntityAsAlly(Entity pEntity) {
        return this.isAlliedTo(pEntity.getTeam());
    }

    public boolean isAlliedTo(@Nullable Team pTeam) {
        return this.getTeam() != null ? this.getTeam().isAlliedTo(pTeam) : false;
    }

    public void setInvisible(boolean pInvisible) {
        this.setSharedFlag(5, pInvisible);
    }

    protected boolean getSharedFlag(int pFlag) {
        return (this.entityData.get(DATA_SHARED_FLAGS_ID) & 1 << pFlag) != 0;
    }

    protected void setSharedFlag(int pFlag, boolean pSet) {
        byte b0 = this.entityData.get(DATA_SHARED_FLAGS_ID);
        if (pSet) {
            this.entityData.set(DATA_SHARED_FLAGS_ID, (byte)(b0 | 1 << pFlag));
        } else {
            this.entityData.set(DATA_SHARED_FLAGS_ID, (byte)(b0 & ~(1 << pFlag)));
        }
    }

    public int getMaxAirSupply() {
        return 300;
    }

    public int getAirSupply() {
        return this.entityData.get(DATA_AIR_SUPPLY_ID);
    }

    public void setAirSupply(int pAir) {
        this.entityData.set(DATA_AIR_SUPPLY_ID, pAir);
    }

    public void clearFreeze() {
        this.setTicksFrozen(0);
    }

    public int getTicksFrozen() {
        return this.entityData.get(DATA_TICKS_FROZEN);
    }

    public void setTicksFrozen(int pTicksFrozen) {
        this.entityData.set(DATA_TICKS_FROZEN, pTicksFrozen);
    }

    public float getPercentFrozen() {
        int i = this.getTicksRequiredToFreeze();
        return (float)Math.min(this.getTicksFrozen(), i) / i;
    }

    public boolean isFullyFrozen() {
        return this.getTicksFrozen() >= this.getTicksRequiredToFreeze();
    }

    public int getTicksRequiredToFreeze() {
        return 140;
    }

    public void thunderHit(ServerLevel pLevel, LightningBolt pLightning) {
        this.setRemainingFireTicks(this.remainingFireTicks + 1);
        if (this.remainingFireTicks == 0) {
            this.igniteForSeconds(8.0F);
        }

        this.hurtServer(pLevel, this.damageSources().lightningBolt(), pLightning.getDamage());
    }

    public void onAboveBubbleColumn(boolean pDownwards, BlockPos pPos) {
        handleOnAboveBubbleColumn(this, pDownwards, pPos);
    }

    protected static void handleOnAboveBubbleColumn(Entity pEntity, boolean pDownwards, BlockPos pPos) {
        Vec3 vec3 = pEntity.getDeltaMovement();
        double d0;
        if (pDownwards) {
            d0 = Math.max(-0.9, vec3.y - 0.03);
        } else {
            d0 = Math.min(1.8, vec3.y + 0.1);
        }

        pEntity.setDeltaMovement(vec3.x, d0, vec3.z);
        sendBubbleColumnParticles(pEntity.level, pPos);
    }

    protected static void sendBubbleColumnParticles(Level pLevel, BlockPos pPos) {
        if (pLevel instanceof ServerLevel serverlevel) {
            for (int i = 0; i < 2; i++) {
                serverlevel.sendParticles(
                    ParticleTypes.SPLASH,
                    pPos.getX() + pLevel.random.nextDouble(),
                    pPos.getY() + 1,
                    pPos.getZ() + pLevel.random.nextDouble(),
                    1,
                    0.0,
                    0.0,
                    0.0,
                    1.0
                );
                serverlevel.sendParticles(
                    ParticleTypes.BUBBLE,
                    pPos.getX() + pLevel.random.nextDouble(),
                    pPos.getY() + 1,
                    pPos.getZ() + pLevel.random.nextDouble(),
                    1,
                    0.0,
                    0.01,
                    0.0,
                    0.2
                );
            }
        }
    }

    public void onInsideBubbleColumn(boolean pDownwards) {
        handleOnInsideBubbleColumn(this, pDownwards);
    }

    protected static void handleOnInsideBubbleColumn(Entity pEntity, boolean pDownwards) {
        Vec3 vec3 = pEntity.getDeltaMovement();
        double d0;
        if (pDownwards) {
            d0 = Math.max(-0.3, vec3.y - 0.03);
        } else {
            d0 = Math.min(0.7, vec3.y + 0.06);
        }

        pEntity.setDeltaMovement(vec3.x, d0, vec3.z);
        pEntity.resetFallDistance();
    }

    public boolean killedEntity(ServerLevel pLevel, LivingEntity pEntity) {
        return true;
    }

    public void checkSlowFallDistance() {
        if (this.getDeltaMovement().y() > -0.5 && this.fallDistance > 1.0) {
            this.fallDistance = 1.0;
        }
    }

    public void resetFallDistance() {
        this.fallDistance = 0.0;
    }

    protected void moveTowardsClosestSpace(double pX, double pY, double pZ) {
        BlockPos blockpos = BlockPos.containing(pX, pY, pZ);
        Vec3 vec3 = new Vec3(pX - blockpos.getX(), pY - blockpos.getY(), pZ - blockpos.getZ());
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();
        Direction direction = Direction.UP;
        double d0 = Double.MAX_VALUE;

        for (Direction direction1 : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.UP}) {
            blockpos$mutableblockpos.setWithOffset(blockpos, direction1);
            if (!this.level().getBlockState(blockpos$mutableblockpos).isCollisionShapeFullBlock(this.level(), blockpos$mutableblockpos)) {
                double d1 = vec3.get(direction1.getAxis());
                double d2 = direction1.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1.0 - d1 : d1;
                if (d2 < d0) {
                    d0 = d2;
                    direction = direction1;
                }
            }
        }

        float f = this.random.nextFloat() * 0.2F + 0.1F;
        float f1 = direction.getAxisDirection().getStep();
        Vec3 vec31 = this.getDeltaMovement().scale(0.75);
        if (direction.getAxis() == Direction.Axis.X) {
            this.setDeltaMovement(f1 * f, vec31.y, vec31.z);
        } else if (direction.getAxis() == Direction.Axis.Y) {
            this.setDeltaMovement(vec31.x, f1 * f, vec31.z);
        } else if (direction.getAxis() == Direction.Axis.Z) {
            this.setDeltaMovement(vec31.x, vec31.y, f1 * f);
        }
    }

    public void makeStuckInBlock(BlockState pState, Vec3 pMotionMultiplier) {
        this.resetFallDistance();
        this.stuckSpeedMultiplier = pMotionMultiplier;
    }

    private static Component removeAction(Component pName) {
        MutableComponent mutablecomponent = pName.plainCopy().setStyle(pName.getStyle().withClickEvent(null));

        for (Component component : pName.getSiblings()) {
            mutablecomponent.append(removeAction(component));
        }

        return mutablecomponent;
    }

    @Override
    public Component getName() {
        Component component = this.getCustomName();
        return component != null ? removeAction(component) : this.getTypeName();
    }

    protected Component getTypeName() {
        return this.getType().getDescription(); // Forge: Use getter to allow overriding by mods;
    }

    public boolean is(Entity pEntity) {
        return this == pEntity;
    }

    public float getYHeadRot() {
        return 0.0F;
    }

    public void setYHeadRot(float pYHeadRot) {
    }

    public void setYBodyRot(float pYBodyRot) {
    }

    public boolean isAttackable() {
        return true;
    }

    public boolean skipAttackInteraction(Entity pEntity) {
        return false;
    }

    @Override
    public String toString() {
        String s = this.level() == null ? "~NULL~" : this.level().toString();
        return this.removalReason != null
            ? String.format(
                Locale.ROOT,
                "%s['%s'/%d, l='%s', x=%.2f, y=%.2f, z=%.2f, removed=%s]",
                this.getClass().getSimpleName(),
                this.getName().getString(),
                this.id,
                s,
                this.getX(),
                this.getY(),
                this.getZ(),
                this.removalReason
            )
            : String.format(
                Locale.ROOT,
                "%s['%s'/%d, l='%s', x=%.2f, y=%.2f, z=%.2f]",
                this.getClass().getSimpleName(),
                this.getName().getString(),
                this.id,
                s,
                this.getX(),
                this.getY(),
                this.getZ()
            );
    }

    protected final boolean isInvulnerableToBase(DamageSource pDamageSource) {
        return this.isRemoved()
            || this.invulnerable && !pDamageSource.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && !pDamageSource.isCreativePlayer()
            || pDamageSource.is(DamageTypeTags.IS_FIRE) && this.fireImmune()
            || pDamageSource.is(DamageTypeTags.IS_FALL) && this.getType().is(EntityTypeTags.FALL_DAMAGE_IMMUNE);
    }

    public boolean isInvulnerable() {
        return this.invulnerable;
    }

    public void setInvulnerable(boolean pIsInvulnerable) {
        this.invulnerable = pIsInvulnerable;
    }

    public void copyPosition(Entity pEntity) {
        this.snapTo(pEntity.getX(), pEntity.getY(), pEntity.getZ(), pEntity.getYRot(), pEntity.getXRot());
    }

    public void restoreFrom(Entity pEntity) {
        CompoundTag compoundtag = pEntity.saveWithoutId(new CompoundTag());
        compoundtag.remove("Dimension");
        this.load(compoundtag);
        this.portalCooldown = pEntity.portalCooldown;
        this.portalProcess = pEntity.portalProcess;
    }

    @Nullable
    public Entity teleport(TeleportTransition pTeleportTransition) {
        if (this.level() instanceof ServerLevel serverlevel && !this.isRemoved()) {
            ServerLevel serverlevel1 = pTeleportTransition.newLevel();
            boolean flag = serverlevel1.dimension() != serverlevel.dimension();
            if (!pTeleportTransition.asPassenger()) {
                this.stopRiding();
            }

            return flag ? this.teleportCrossDimension(serverlevel1, pTeleportTransition) : this.teleportSameDimension(serverlevel, pTeleportTransition);
        } else {
            return null;
        }
    }

    private Entity teleportSameDimension(ServerLevel pLevel, TeleportTransition pTeleportTransition) {
        for (Entity entity : this.getPassengers()) {
            entity.teleport(this.calculatePassengerTransition(pTeleportTransition, entity));
        }

        ProfilerFiller profilerfiller = Profiler.get();
        profilerfiller.push("teleportSameDimension");
        this.teleportSetPosition(PositionMoveRotation.of(pTeleportTransition), pTeleportTransition.relatives());
        if (!pTeleportTransition.asPassenger()) {
            this.sendTeleportTransitionToRidingPlayers(pTeleportTransition);
        }

        pTeleportTransition.postTeleportTransition().onTransition(this);
        profilerfiller.pop();
        return this;
    }

    private Entity teleportCrossDimension(ServerLevel pLevel, TeleportTransition pTeleportTransition) {
        List<Entity> list = this.getPassengers();
        List<Entity> list1 = new ArrayList<>(list.size());
        this.ejectPassengers();

        for (Entity entity : list) {
            Entity entity1 = entity.teleport(this.calculatePassengerTransition(pTeleportTransition, entity));
            if (entity1 != null) {
                list1.add(entity1);
            }
        }

        ProfilerFiller profilerfiller = Profiler.get();
        profilerfiller.push("teleportCrossDimension");
        Entity entity3 = this.getType().create(pLevel, EntitySpawnReason.DIMENSION_TRAVEL);
        if (entity3 == null) {
            profilerfiller.pop();
            return null;
        } else {
            entity3.restoreFrom(this);
            this.removeAfterChangingDimensions();
            entity3.teleportSetPosition(PositionMoveRotation.of(pTeleportTransition), pTeleportTransition.relatives());
            pLevel.addDuringTeleport(entity3);

            for (Entity entity2 : list1) {
                entity2.startRiding(entity3, true);
            }

            pLevel.resetEmptyTime();
            pTeleportTransition.postTeleportTransition().onTransition(entity3);
            profilerfiller.pop();
            return entity3;
        }
    }

    private TeleportTransition calculatePassengerTransition(TeleportTransition pTeleportTransition, Entity pEntity) {
        float f = pTeleportTransition.yRot() + (pTeleportTransition.relatives().contains(Relative.Y_ROT) ? 0.0F : pEntity.getYRot() - this.getYRot());
        float f1 = pTeleportTransition.xRot() + (pTeleportTransition.relatives().contains(Relative.X_ROT) ? 0.0F : pEntity.getXRot() - this.getXRot());
        Vec3 vec3 = pEntity.position().subtract(this.position());
        Vec3 vec31 = pTeleportTransition.position()
            .add(
                pTeleportTransition.relatives().contains(Relative.X) ? 0.0 : vec3.x(),
                pTeleportTransition.relatives().contains(Relative.Y) ? 0.0 : vec3.y(),
                pTeleportTransition.relatives().contains(Relative.Z) ? 0.0 : vec3.z()
            );
        return pTeleportTransition.withPosition(vec31).withRotation(f, f1).transitionAsPassenger();
    }

    private void sendTeleportTransitionToRidingPlayers(TeleportTransition pTeleportTransition) {
        Entity entity = this.getControllingPassenger();

        for (Entity entity1 : this.getIndirectPassengers()) {
            if (entity1 instanceof ServerPlayer serverplayer) {
                if (entity != null && serverplayer.getId() == entity.getId()) {
                    serverplayer.connection
                        .send(
                            ClientboundTeleportEntityPacket.teleport(
                                this.getId(), PositionMoveRotation.of(pTeleportTransition), pTeleportTransition.relatives(), this.onGround
                            )
                        );
                } else {
                    serverplayer.connection
                        .send(ClientboundTeleportEntityPacket.teleport(this.getId(), PositionMoveRotation.of(this), Set.of(), this.onGround));
                }
            }
        }
    }

    public void teleportSetPosition(PositionMoveRotation pPositionMovementRotation, Set<Relative> pRelatives) {
        PositionMoveRotation positionmoverotation = PositionMoveRotation.of(this);
        PositionMoveRotation positionmoverotation1 = PositionMoveRotation.calculateAbsolute(positionmoverotation, pPositionMovementRotation, pRelatives);
        this.setPosRaw(positionmoverotation1.position().x, positionmoverotation1.position().y, positionmoverotation1.position().z);
        this.setYRot(positionmoverotation1.yRot());
        this.setYHeadRot(positionmoverotation1.yRot());
        this.setXRot(positionmoverotation1.xRot());
        this.reapplyPosition();
        this.setOldPosAndRot();
        this.setDeltaMovement(positionmoverotation1.deltaMovement());
        this.movementThisTick.clear();
    }

    public void forceSetRotation(float pYRot, float pXRot) {
        this.setYRot(pYRot);
        this.setYHeadRot(pYRot);
        this.setXRot(pXRot);
        this.setOldRot();
    }

    public void placePortalTicket(BlockPos pPos) {
        if (this.level() instanceof ServerLevel serverlevel) {
            serverlevel.getChunkSource().addTicketWithRadius(TicketType.PORTAL, new ChunkPos(pPos), 3);
        }
    }

    protected void removeAfterChangingDimensions() {
        this.setRemoved(Entity.RemovalReason.CHANGED_DIMENSION);
        if (this instanceof Leashable leashable) {
            leashable.removeLeash();
        }
    }

    public Vec3 getRelativePortalPosition(Direction.Axis pAxis, BlockUtil.FoundRectangle pPortal) {
        return PortalShape.getRelativePosition(pPortal, pAxis, this.position(), this.getDimensions(this.getPose()));
    }

    public boolean canUsePortal(boolean pAllowPassengers) {
        return (pAllowPassengers || !this.isPassenger()) && this.isAlive();
    }

    public boolean canTeleport(Level pFromLevel, Level pToLevel) {
        if (pFromLevel.dimension() == Level.END && pToLevel.dimension() == Level.OVERWORLD) {
            for (Entity entity : this.getPassengers()) {
                if (entity instanceof ServerPlayer serverplayer && !serverplayer.seenCredits) {
                    return false;
                }
            }
        }

        return true;
    }

    public float getBlockExplosionResistance(Explosion pExplosion, BlockGetter pLevel, BlockPos pPos, BlockState pBlockState, FluidState pFluidState, float pExplosionPower) {
        return pExplosionPower;
    }

    public boolean shouldBlockExplode(Explosion pExplosion, BlockGetter pLevel, BlockPos pPos, BlockState pBlockState, float pExplosionPower) {
        return true;
    }

    public int getMaxFallDistance() {
        return 3;
    }

    public boolean isIgnoringBlockTriggers() {
        return false;
    }

    public void fillCrashReportCategory(CrashReportCategory pCategory) {
        pCategory.setDetail("Entity Type", () -> EntityType.getKey(this.getType()) + " (" + this.getClass().getCanonicalName() + ")");
        pCategory.setDetail("Entity ID", this.id);
        pCategory.setDetail("Entity Name", () -> this.getName().getString());
        pCategory.setDetail("Entity's Exact location", String.format(Locale.ROOT, "%.2f, %.2f, %.2f", this.getX(), this.getY(), this.getZ()));
        pCategory.setDetail(
            "Entity's Block location",
            CrashReportCategory.formatLocation(this.level(), Mth.floor(this.getX()), Mth.floor(this.getY()), Mth.floor(this.getZ()))
        );
        Vec3 vec3 = this.getDeltaMovement();
        pCategory.setDetail("Entity's Momentum", String.format(Locale.ROOT, "%.2f, %.2f, %.2f", vec3.x, vec3.y, vec3.z));
        pCategory.setDetail("Entity's Passengers", () -> this.getPassengers().toString());
        pCategory.setDetail("Entity's Vehicle", () -> String.valueOf(this.getVehicle()));
    }

    public boolean displayFireAnimation() {
        return this.isOnFire() && !this.isSpectator();
    }

    public void setUUID(UUID pUniqueId) {
        this.uuid = pUniqueId;
        this.stringUUID = this.uuid.toString();
    }

    @Override
    public UUID getUUID() {
        return this.uuid;
    }

    public String getStringUUID() {
        return this.stringUUID;
    }

    @Override
    public String getScoreboardName() {
        return this.stringUUID;
    }

    @Deprecated // Forge: Use FluidType sensitive version
    public boolean isPushedByFluid() {
        return true;
    }

    public static double getViewScale() {
        return viewScale;
    }

    public static void setViewScale(double pRenderDistWeight) {
        viewScale = pRenderDistWeight;
    }

    @Override
    public Component getDisplayName() {
        return PlayerTeam.formatNameForTeam(this.getTeam(), this.getName()).withStyle(p_185975_ -> p_185975_.withHoverEvent(this.createHoverEvent()).withInsertion(this.getStringUUID()));
    }

    public void setCustomName(@Nullable Component pName) {
        this.entityData.set(DATA_CUSTOM_NAME, Optional.ofNullable(pName));
    }

    @Nullable
    @Override
    public Component getCustomName() {
        return this.entityData.get(DATA_CUSTOM_NAME).orElse(null);
    }

    @Override
    public boolean hasCustomName() {
        return this.entityData.get(DATA_CUSTOM_NAME).isPresent();
    }

    public void setCustomNameVisible(boolean pAlwaysRenderNameTag) {
        this.entityData.set(DATA_CUSTOM_NAME_VISIBLE, pAlwaysRenderNameTag);
    }

    public boolean isCustomNameVisible() {
        return this.entityData.get(DATA_CUSTOM_NAME_VISIBLE);
    }

    public boolean teleportTo(
        ServerLevel pLevel,
        double pX,
        double pY,
        double pZ,
        Set<Relative> pRelativeMovements,
        float pYaw,
        float pPitch,
        boolean pSetCamera
    ) {
        Entity entity = this.teleport(
            new TeleportTransition(
                pLevel, new Vec3(pX, pY, pZ), Vec3.ZERO, pYaw, pPitch, pRelativeMovements, TeleportTransition.DO_NOTHING
            )
        );
        return entity != null;
    }

    public void dismountTo(double pX, double pY, double pZ) {
        this.teleportTo(pX, pY, pZ);
    }

    public void teleportTo(double pX, double pY, double pZ) {
        if (this.level() instanceof ServerLevel) {
            this.snapTo(pX, pY, pZ, this.getYRot(), this.getXRot());
            this.teleportPassengers();
        }
    }

    private void teleportPassengers() {
        this.getSelfAndPassengers().forEach(p_185977_ -> {
            for (Entity entity : p_185977_.passengers) {
                p_185977_.positionRider(entity, Entity::snapTo);
            }
        });
    }

    public void teleportRelative(double pDx, double pDy, double pDz) {
        this.teleportTo(this.getX() + pDx, this.getY() + pDy, this.getZ() + pDz);
    }

    public boolean shouldShowName() {
        return this.isCustomNameVisible();
    }

    @Override
    public void onSyncedDataUpdated(List<SynchedEntityData.DataValue<?>> pDataValues) {
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> pKey) {
        if (DATA_POSE.equals(pKey)) {
            this.refreshDimensions();
        }
    }

    @Deprecated
    protected void fixupDimensions() {
        Pose pose = this.getPose();
        EntityDimensions entitydimensions = this.getDimensions(pose);
        this.dimensions = entitydimensions;
        this.eyeHeight = entitydimensions.eyeHeight();
    }

    public void refreshDimensions() {
        EntityDimensions entitydimensions = this.dimensions;
        Pose pose = this.getPose();
        EntityDimensions entitydimensions1 = this.getDimensions(pose);
        this.dimensions = entitydimensions1;
        this.eyeHeight = entitydimensions1.eyeHeight();
        this.reapplyPosition();
        boolean flag = entitydimensions1.width() <= 4.0F && entitydimensions1.height() <= 4.0F;
        if (!this.level.isClientSide
            && !this.firstTick
            && !this.noPhysics
            && flag
            && (entitydimensions1.width() > entitydimensions.width() || entitydimensions1.height() > entitydimensions.height())
            && !(this instanceof Player)) {
            this.fudgePositionAfterSizeChange(entitydimensions);
        }
    }

    public boolean fudgePositionAfterSizeChange(EntityDimensions pDimensions) {
        EntityDimensions entitydimensions = this.getDimensions(this.getPose());
        Vec3 vec3 = this.position().add(0.0, pDimensions.height() / 2.0, 0.0);
        double d0 = Math.max(0.0F, entitydimensions.width() - pDimensions.width()) + 1.0E-6;
        double d1 = Math.max(0.0F, entitydimensions.height() - pDimensions.height()) + 1.0E-6;
        VoxelShape voxelshape = Shapes.create(AABB.ofSize(vec3, d0, d1, d0));
        Optional<Vec3> optional = this.level
            .findFreePosition(this, voxelshape, vec3, entitydimensions.width(), entitydimensions.height(), entitydimensions.width());
        if (optional.isPresent()) {
            this.setPos(optional.get().add(0.0, -entitydimensions.height() / 2.0, 0.0));
            return true;
        } else {
            if (entitydimensions.width() > pDimensions.width() && entitydimensions.height() > pDimensions.height()) {
                VoxelShape voxelshape1 = Shapes.create(AABB.ofSize(vec3, d0, 1.0E-6, d0));
                Optional<Vec3> optional1 = this.level
                    .findFreePosition(this, voxelshape1, vec3, entitydimensions.width(), pDimensions.height(), entitydimensions.width());
                if (optional1.isPresent()) {
                    this.setPos(optional1.get().add(0.0, -pDimensions.height() / 2.0 + 1.0E-6, 0.0));
                    return true;
                }
            }

            return false;
        }
    }

    public Direction getDirection() {
        return Direction.fromYRot(this.getYRot());
    }

    public Direction getMotionDirection() {
        return this.getDirection();
    }

    protected HoverEvent createHoverEvent() {
        return new HoverEvent.ShowEntity(new HoverEvent.EntityTooltipInfo(this.getType(), this.getUUID(), this.getName()));
    }

    public boolean broadcastToPlayer(ServerPlayer pPlayer) {
        return true;
    }

    @Override
    public final AABB getBoundingBox() {
        return this.bb;
    }

    public final void setBoundingBox(AABB pBb) {
        this.bb = pBb;
    }

    public final float getEyeHeight(Pose pPose) {
        return this.getDimensions(pPose).eyeHeight();
    }

    public final float getEyeHeight() {
        return this.eyeHeight;
    }

    public Vec3 getLeashOffset(float pPartialTick) {
        return this.getLeashOffset();
    }

    protected Vec3 getLeashOffset() {
        return new Vec3(0.0, this.getEyeHeight(), this.getBbWidth() * 0.4F);
    }

    public SlotAccess getSlot(int pSlot) {
        return SlotAccess.NULL;
    }

    public Level getCommandSenderWorld() {
        return this.level();
    }

    @Nullable
    public MinecraftServer getServer() {
        return this.level().getServer();
    }

    public InteractionResult interactAt(Player pPlayer, Vec3 pVec, InteractionHand pHand) {
        return InteractionResult.PASS;
    }

    public boolean ignoreExplosion(Explosion pExplosion) {
        return false;
    }

    public void startSeenByPlayer(ServerPlayer pServerPlayer) {
    }

    public void stopSeenByPlayer(ServerPlayer pServerPlayer) {
    }

    public float rotate(Rotation pTransformRotation) {
        float f = Mth.wrapDegrees(this.getYRot());
        switch (pTransformRotation) {
            case CLOCKWISE_180:
                return f + 180.0F;
            case COUNTERCLOCKWISE_90:
                return f + 270.0F;
            case CLOCKWISE_90:
                return f + 90.0F;
            default:
                return f;
        }
    }

    public float mirror(Mirror pTransformMirror) {
        float f = Mth.wrapDegrees(this.getYRot());
        switch (pTransformMirror) {
            case FRONT_BACK:
                return -f;
            case LEFT_RIGHT:
                return 180.0F - f;
            default:
                return f;
        }
    }

    public ProjectileDeflection deflection(Projectile pProjectile) {
        return this.getType().is(EntityTypeTags.DEFLECTS_PROJECTILES) ? ProjectileDeflection.REVERSE : ProjectileDeflection.NONE;
    }

    @Nullable
    public LivingEntity getControllingPassenger() {
        return null;
    }

    public final boolean hasControllingPassenger() {
        return this.getControllingPassenger() != null;
    }

    public final List<Entity> getPassengers() {
        return this.passengers;
    }

    @Nullable
    public Entity getFirstPassenger() {
        return this.passengers.isEmpty() ? null : this.passengers.get(0);
    }

    public boolean hasPassenger(Entity pEntity) {
        return this.passengers.contains(pEntity);
    }

    public boolean hasPassenger(Predicate<Entity> pPredicate) {
        for (Entity entity : this.passengers) {
            if (pPredicate.test(entity)) {
                return true;
            }
        }

        return false;
    }

    private Stream<Entity> getIndirectPassengersStream() {
        return this.passengers.stream().flatMap(Entity::getSelfAndPassengers);
    }

    @Override
    public Stream<Entity> getSelfAndPassengers() {
        return Stream.concat(Stream.of(this), this.getIndirectPassengersStream());
    }

    @Override
    public Stream<Entity> getPassengersAndSelf() {
        return Stream.concat(this.passengers.stream().flatMap(Entity::getPassengersAndSelf), Stream.of(this));
    }

    public Iterable<Entity> getIndirectPassengers() {
        return () -> this.getIndirectPassengersStream().iterator();
    }

    public int countPlayerPassengers() {
        return (int)this.getIndirectPassengersStream().filter(p_185943_ -> p_185943_ instanceof Player).count();
    }

    public boolean hasExactlyOnePlayerPassenger() {
        return this.countPlayerPassengers() == 1;
    }

    public Entity getRootVehicle() {
        Entity entity = this;

        while (entity.isPassenger()) {
            entity = entity.getVehicle();
        }

        return entity;
    }

    public boolean isPassengerOfSameVehicle(Entity pEntity) {
        return this.getRootVehicle() == pEntity.getRootVehicle();
    }

    public boolean hasIndirectPassenger(Entity pEntity) {
        if (!pEntity.isPassenger()) {
            return false;
        } else {
            Entity entity = pEntity.getVehicle();
            return entity == this ? true : this.hasIndirectPassenger(entity);
        }
    }

    public final boolean isLocalInstanceAuthoritative() {
        return this.level.isClientSide() ? this.isLocalClientAuthoritative() : !this.isClientAuthoritative();
    }

    protected boolean isLocalClientAuthoritative() {
        LivingEntity livingentity = this.getControllingPassenger();
        return livingentity != null && livingentity.isLocalClientAuthoritative();
    }

    public boolean isClientAuthoritative() {
        LivingEntity livingentity = this.getControllingPassenger();
        return livingentity != null && livingentity.isClientAuthoritative();
    }

    public boolean canSimulateMovement() {
        return this.isLocalInstanceAuthoritative();
    }

    public boolean isEffectiveAi() {
        return this.isLocalInstanceAuthoritative();
    }

    protected static Vec3 getCollisionHorizontalEscapeVector(double pVehicleWidth, double pPassengerWidth, float pYRot) {
        double d0 = (pVehicleWidth + pPassengerWidth + 1.0E-5F) / 2.0;
        float f = -Mth.sin(pYRot * (float) (Math.PI / 180.0));
        float f1 = Mth.cos(pYRot * (float) (Math.PI / 180.0));
        float f2 = Math.max(Math.abs(f), Math.abs(f1));
        return new Vec3(f * d0 / f2, 0.0, f1 * d0 / f2);
    }

    public Vec3 getDismountLocationForPassenger(LivingEntity pPassenger) {
        return new Vec3(this.getX(), this.getBoundingBox().maxY, this.getZ());
    }

    @Nullable
    public Entity getVehicle() {
        return this.vehicle;
    }

    @Nullable
    public Entity getControlledVehicle() {
        return this.vehicle != null && this.vehicle.getControllingPassenger() == this ? this.vehicle : null;
    }

    public PushReaction getPistonPushReaction() {
        return PushReaction.NORMAL;
    }

    public SoundSource getSoundSource() {
        return SoundSource.NEUTRAL;
    }

    protected int getFireImmuneTicks() {
        return 1;
    }

    public CommandSourceStack createCommandSourceStackForNameResolution(ServerLevel pLevel) {
        return new CommandSourceStack(
            CommandSource.NULL, this.position(), this.getRotationVector(), pLevel, 0, this.getName().getString(), this.getDisplayName(), pLevel.getServer(), this
        );
    }

    public void lookAt(EntityAnchorArgument.Anchor pAnchor, Vec3 pTarget) {
        Vec3 vec3 = pAnchor.apply(this);
        double d0 = pTarget.x - vec3.x;
        double d1 = pTarget.y - vec3.y;
        double d2 = pTarget.z - vec3.z;
        double d3 = Math.sqrt(d0 * d0 + d2 * d2);
        this.setXRot(Mth.wrapDegrees((float)(-(Mth.atan2(d1, d3) * 180.0F / (float)Math.PI))));
        this.setYRot(Mth.wrapDegrees((float)(Mth.atan2(d2, d0) * 180.0F / (float)Math.PI) - 90.0F));
        this.setYHeadRot(this.getYRot());
        this.xRotO = this.getXRot();
        this.yRotO = this.getYRot();
    }

    public float getPreciseBodyRotation(float pPartialTick) {
        return Mth.lerp(pPartialTick, this.yRotO, this.yRot);
    }

    @Deprecated // Forge: Use predicate version instead, only for vanilla Tags
    public boolean updateFluidHeightAndDoFluidPushing(TagKey<Fluid> pFluidTag, double pMotionScale) {
        this.updateFluidHeightAndDoFluidPushing(com.google.common.base.Predicates.alwaysTrue());
        if(pFluidTag == FluidTags.WATER) return this.isInFluidType(net.minecraftforge.common.ForgeMod.WATER_TYPE.get());
        else if (pFluidTag == FluidTags.LAVA) return this.isInFluidType(net.minecraftforge.common.ForgeMod.LAVA_TYPE.get());
        else return false;
     }

     public void updateFluidHeightAndDoFluidPushing(Predicate<FluidState> shouldUpdate) {
        if (this.touchingUnloadedChunk()) {
            return;
        } else {
            AABB aabb = this.getBoundingBox().deflate(0.001);
            int i = Mth.floor(aabb.minX);
            int j = Mth.ceil(aabb.maxX);
            int k = Mth.floor(aabb.minY);
            int l = Mth.ceil(aabb.maxY);
            int i1 = Mth.floor(aabb.minZ);
            int j1 = Mth.ceil(aabb.maxZ);
            double d0 = 0.0;
            boolean flag = this.isPushedByFluid();
            boolean flag1 = false;
            Vec3 vec3 = Vec3.ZERO;
            int k1 = 0;
            BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();
            var interimCalcs = new it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap<net.minecraftforge.fluids.FluidType, org.apache.commons.lang3.tuple.MutableTriple<Double, Vec3, Integer>>(net.minecraftforge.fluids.FluidType.SIZE.get() - 1);

            for (int l1 = i; l1 < j; l1++) {
                for (int i2 = k; i2 < l; i2++) {
                    for (int j2 = i1; j2 < j1; j2++) {
                        blockpos$mutableblockpos.set(l1, i2, j2);
                        FluidState fluidstate = this.level().getFluidState(blockpos$mutableblockpos);
                        net.minecraftforge.fluids.FluidType fluidType = fluidstate.getFluidType();
                        if (!fluidType.isAir() && shouldUpdate.test(fluidstate)) {
                            double d1 = i2 + fluidstate.getHeight(this.level(), blockpos$mutableblockpos);
                            if (d1 >= aabb.minY) {
                                flag1 = true;
                                var interim = interimCalcs.computeIfAbsent(fluidType, t -> org.apache.commons.lang3.tuple.MutableTriple.of(0.0D, Vec3.ZERO, 0));
                                interim.setLeft(Math.max(d1 - aabb.minY, interim.getLeft()));
                                if (this.isPushedByFluid(fluidType)) {
                                    Vec3 vec31 = fluidstate.getFlow(this.level(), blockpos$mutableblockpos);
                                    if (interim.getLeft() < 0.4D) {
                                       vec31 = vec31.scale(interim.getLeft());
                                    }

                                    interim.setMiddle(interim.getMiddle().add(vec31));
                                    interim.setRight(interim.getRight() + 1);
                                }
                            }
                        }
                    }
                }
            }

            interimCalcs.forEach((fluidType, interim) -> {
            if (interim.getMiddle().length() > 0.0D) {
                if (interim.getRight() > 0) {
                    interim.setMiddle(interim.getMiddle().scale(1.0D / (double)interim.getRight()));
                }

                if (!(this instanceof Player)) {
                    interim.setMiddle(interim.getMiddle().normalize());
                }

                Vec3 vec32 = this.getDeltaMovement();
                interim.setMiddle(interim.getMiddle().scale(this.getFluidMotionScale(fluidType)));
                double d2 = 0.003;
                if (Math.abs(vec32.x) < 0.003 && Math.abs(vec32.z) < 0.003 && interim.getMiddle().length() < 0.0045000000000000005) {
                    interim.setMiddle(interim.getMiddle().normalize().scale(0.0045000000000000005));
                }

                this.setDeltaMovement(this.getDeltaMovement().add(interim.getMiddle()));
            }

            this.setFluidTypeHeight(fluidType, interim.getLeft());
            });
        }
    }

    public boolean touchingUnloadedChunk() {
        AABB aabb = this.getBoundingBox().inflate(1.0);
        int i = Mth.floor(aabb.minX);
        int j = Mth.ceil(aabb.maxX);
        int k = Mth.floor(aabb.minZ);
        int l = Mth.ceil(aabb.maxZ);
        return !this.level().hasChunksAt(i, k, j, l);
    }

    @Deprecated // Forge: Use getFluidTypeHeight instead
    public double getFluidHeight(TagKey<Fluid> pFluidTag) {
        if (pFluidTag == FluidTags.WATER) return getFluidTypeHeight(net.minecraftforge.common.ForgeMod.WATER_TYPE.get());
        else if (pFluidTag == FluidTags.LAVA) return getFluidTypeHeight(net.minecraftforge.common.ForgeMod.LAVA_TYPE.get());
        return this.fluidHeight.getDouble(pFluidTag);
    }

    public double getFluidJumpThreshold() {
        return this.getEyeHeight() < 0.4 ? 0.0 : 0.4;
    }

    public final float getBbWidth() {
        return this.dimensions.width();
    }

    public final float getBbHeight() {
        return this.dimensions.height();
    }

    private boolean hasExtraSpawnData = this instanceof net.minecraftforge.entity.IEntityAdditionalSpawnData;
    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity pEntity) {
        if (hasExtraSpawnData) return net.minecraftforge.common.ForgeHooks.getEntitySpawnPacket(this);
        return new ClientboundAddEntityPacket(this, pEntity);
    }

    public EntityDimensions getDimensions(Pose pPose) {
        return this.type.getDimensions();
    }

    public final EntityAttachments getAttachments() {
        return this.dimensions.attachments();
    }

    public Vec3 position() {
        return this.position;
    }

    public Vec3 trackingPosition() {
        return this.position();
    }

    @Override
    public BlockPos blockPosition() {
        return this.blockPosition;
    }

    public BlockState getInBlockState() {
        if (this.inBlockState == null) {
            this.inBlockState = this.level().getBlockState(this.blockPosition());
        }

        return this.inBlockState;
    }

    public ChunkPos chunkPosition() {
        return this.chunkPosition;
    }

    public Vec3 getDeltaMovement() {
        return this.deltaMovement;
    }

    public void setDeltaMovement(Vec3 pDeltaMovement) {
        this.deltaMovement = pDeltaMovement;
    }

    public void addDeltaMovement(Vec3 pAddend) {
        this.setDeltaMovement(this.getDeltaMovement().add(pAddend));
    }

    public void setDeltaMovement(double pX, double pY, double pZ) {
        this.setDeltaMovement(new Vec3(pX, pY, pZ));
    }

    public final int getBlockX() {
        return this.blockPosition.getX();
    }

    public final double getX() {
        return this.position.x;
    }

    public double getX(double pScale) {
        return this.position.x + this.getBbWidth() * pScale;
    }

    public double getRandomX(double pScale) {
        return this.getX((2.0 * this.random.nextDouble() - 1.0) * pScale);
    }

    public final int getBlockY() {
        return this.blockPosition.getY();
    }

    public final double getY() {
        return this.position.y;
    }

    public double getY(double pScale) {
        return this.position.y + this.getBbHeight() * pScale;
    }

    public double getRandomY() {
        return this.getY(this.random.nextDouble());
    }

    public double getEyeY() {
        return this.position.y + this.eyeHeight;
    }

    public final int getBlockZ() {
        return this.blockPosition.getZ();
    }

    public final double getZ() {
        return this.position.z;
    }

    public double getZ(double pScale) {
        return this.position.z + this.getBbWidth() * pScale;
    }

    public double getRandomZ(double pScale) {
        return this.getZ((2.0 * this.random.nextDouble() - 1.0) * pScale);
    }

    public final void setPosRaw(double pX, double pY, double pZ) {
        if (this.position.x != pX || this.position.y != pY || this.position.z != pZ) {
            this.position = new Vec3(pX, pY, pZ);
            int i = Mth.floor(pX);
            int j = Mth.floor(pY);
            int k = Mth.floor(pZ);
            if (i != this.blockPosition.getX() || j != this.blockPosition.getY() || k != this.blockPosition.getZ()) {
                this.blockPosition = new BlockPos(i, j, k);
                this.inBlockState = null;
                if (SectionPos.blockToSectionCoord(i) != this.chunkPosition.x || SectionPos.blockToSectionCoord(k) != this.chunkPosition.z) {
                    this.chunkPosition = new ChunkPos(this.blockPosition);
                }
            }

            this.levelCallback.onMove();
        }

        // Forge - ensure target chunk is loaded.
        if (this.isAddedToWorld() && !this.level.isClientSide && !this.isRemoved()) {
            this.level.getChunk((int) Math.floor(pX) >> 4, (int) Math.floor(pZ) >> 4);
        }
    }

    public void checkDespawn() {
    }

    public Vec3 getRopeHoldPosition(float pPartialTicks) {
        return this.getPosition(pPartialTicks).add(0.0, this.eyeHeight * 0.7, 0.0);
    }

    public void recreateFromPacket(ClientboundAddEntityPacket pPacket) {
        int i = pPacket.getId();
        double d0 = pPacket.getX();
        double d1 = pPacket.getY();
        double d2 = pPacket.getZ();
        this.syncPacketPositionCodec(d0, d1, d2);
        this.snapTo(d0, d1, d2, pPacket.getYRot(), pPacket.getXRot());
        this.setId(i);
        this.setUUID(pPacket.getUUID());
        Vec3 vec3 = new Vec3(pPacket.getXa(), pPacket.getYa(), pPacket.getZa());
        this.setDeltaMovement(vec3);
    }

    @Nullable
    public ItemStack getPickResult() {
        return null;
    }

    public void setIsInPowderSnow(boolean pIsInPowderSnow) {
        this.isInPowderSnow = pIsInPowderSnow;
    }

    public boolean canFreeze() {
        return !this.getType().is(EntityTypeTags.FREEZE_IMMUNE_ENTITY_TYPES);
    }

    public boolean isFreezing() {
        return this.getTicksFrozen() > 0;
    }

    public float getYRot() {
        return this.yRot;
    }

    public float getVisualRotationYInDegrees() {
        return this.getYRot();
    }

    public void setYRot(float pYRot) {
        if (!Float.isFinite(pYRot)) {
            Util.logAndPauseIfInIde("Invalid entity rotation: " + pYRot + ", discarding.");
        } else {
            this.yRot = pYRot;
        }
    }

    public float getXRot() {
        return this.xRot;
    }

    public void setXRot(float pXRot) {
        if (!Float.isFinite(pXRot)) {
            Util.logAndPauseIfInIde("Invalid entity rotation: " + pXRot + ", discarding.");
        } else {
            this.xRot = Math.clamp(pXRot % 360.0F, -90.0F, 90.0F);
        }
    }

    public boolean canSprint() {
        return false;
    }

    public float maxUpStep() {
        return 0.0F;
    }

    public void onExplosionHit(@Nullable Entity pEntity) {
    }

    @Override
    public final boolean isRemoved() {
        return this.removalReason != null;
    }

    @Nullable
    public Entity.RemovalReason getRemovalReason() {
        return this.removalReason;
    }

    @Override
    public final void setRemoved(Entity.RemovalReason p_146876_) {
        if (this.removalReason == null) {
            this.removalReason = p_146876_;
        }

        if (this.removalReason.shouldDestroy()) {
            this.stopRiding();
        }

        this.getPassengers().forEach(Entity::stopRiding);
        this.levelCallback.onRemove(p_146876_);
        this.onRemoval(p_146876_);
    }

    protected void unsetRemoved() {
        this.removalReason = null;
    }

    @Override
    public void setLevelCallback(EntityInLevelCallback p_146849_) {
        this.levelCallback = p_146849_;
    }

    @Override
    public boolean shouldBeSaved() {
        if (this.removalReason != null && !this.removalReason.shouldSave()) {
            return false;
        } else {
            return this.isPassenger() ? false : !this.isVehicle() || !this.hasExactlyOnePlayerPassenger();
        }
    }

    @Override
    public boolean isAlwaysTicking() {
        return false;
    }

    public boolean mayInteract(ServerLevel pLevel, BlockPos pPos) {
        return true;
    }

    public Level level() {
        return this.level;
    }

    protected void setLevel(Level pLevel) {
        this.level = pLevel;
    }

    public DamageSources damageSources() {
        return this.level().damageSources();
    }

    public RegistryAccess registryAccess() {
        return this.level().registryAccess();
    }

    protected void lerpPositionAndRotationStep(int pSteps, double pTargetX, double pTargetY, double pTargetZ, double pTargetYRot, double pTargetXRot) {
        double d0 = 1.0 / pSteps;
        double d1 = Mth.lerp(d0, this.getX(), pTargetX);
        double d2 = Mth.lerp(d0, this.getY(), pTargetY);
        double d3 = Mth.lerp(d0, this.getZ(), pTargetZ);
        float f = (float)Mth.rotLerp(d0, this.getYRot(), pTargetYRot);
        float f1 = (float)Mth.lerp(d0, this.getXRot(), pTargetXRot);
        this.setPos(d1, d2, d3);
        this.setRot(f, f1);
    }

    private boolean canUpdate = true;
    @Override
    public void canUpdate(boolean value) {
       this.canUpdate = value;
    }

    @Override
    public boolean canUpdate() {
       return this.canUpdate;
    }

    private java.util.Collection<ItemEntity> captureDrops = null;
    @Override
    public java.util.Collection<ItemEntity> captureDrops() {
       return captureDrops;
    }

    @Override
    public java.util.Collection<ItemEntity> captureDrops(java.util.Collection<ItemEntity> value) {
       java.util.Collection<ItemEntity> ret = captureDrops;
       this.captureDrops = value;
       return ret;
    }

    private CompoundTag persistentData;
    @Override
    public CompoundTag getPersistentData() {
       if (persistentData == null)
          persistentData = new CompoundTag();
       return persistentData;
    }

    @Override
    public boolean canTrample(ServerLevel level, BlockState state, BlockPos pos, double fallDistance) {
       return level.random.nextFloat() < fallDistance - 0.5F
           && this instanceof LivingEntity
           && (this instanceof Player || net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(level, this))
           && this.getBbWidth() * this.getBbWidth() * this.getBbHeight() > 0.512F;
    }

    /**
     * Internal use for keeping track of entities that are tracked by a world, to
     * allow guarantees that entity position changes will force a chunk load, avoiding
     * potential issues with entity desyncing and bad chunk data.
     */
    private boolean isAddedToWorld;

    @Override
    public final boolean isAddedToWorld() { return this.isAddedToWorld; }

    @Override
    public void onAddedToWorld() { this.isAddedToWorld = true; }

    @Override
    public void onRemovedFromWorld() { this.isAddedToWorld = false; }

    @Override
    public void revive() {
        this.unsetRemoved();
        this.reviveCaps();
    }

    protected Object2DoubleMap<net.minecraftforge.fluids.FluidType> forgeFluidTypeHeight = new Object2DoubleArrayMap<>(net.minecraftforge.fluids.FluidType.SIZE.get());
    private net.minecraftforge.fluids.FluidType forgeFluidTypeOnEyes = net.minecraftforge.common.ForgeMod.EMPTY_TYPE.get();
    protected final void setFluidTypeHeight(net.minecraftforge.fluids.FluidType type, double height) {
        this.forgeFluidTypeHeight.put(type, height);
    }

    @Override
    public final double getFluidTypeHeight(net.minecraftforge.fluids.FluidType type) {
        return this.forgeFluidTypeHeight.getDouble(type);
    }

    @Override
    public final boolean isInFluidType(java.util.function.BiPredicate<net.minecraftforge.fluids.FluidType, Double> predicate, boolean forAllTypes) {
       return forAllTypes ? this.forgeFluidTypeHeight.object2DoubleEntrySet().stream().allMatch(e -> predicate.test(e.getKey(), e.getDoubleValue()))
                          : this.forgeFluidTypeHeight.object2DoubleEntrySet().stream().anyMatch(e -> predicate.test(e.getKey(), e.getDoubleValue()));
    }

    @Override
    public final boolean isInFluidType() {
       return this.forgeFluidTypeHeight.size() > 0;
    }

    @Override
    public final net.minecraftforge.fluids.FluidType getEyeInFluidType() {
        return forgeFluidTypeOnEyes;
    }

    @Override
    public net.minecraftforge.fluids.FluidType getMaxHeightFluidType() {
        return this.forgeFluidTypeHeight.object2DoubleEntrySet().stream().max(java.util.Comparator.comparingDouble(Object2DoubleMap.Entry::getDoubleValue)).map(Object2DoubleMap.Entry::getKey).orElseGet(net.minecraftforge.common.ForgeMod.EMPTY_TYPE);
    }

    public RandomSource getRandom() {
        return this.random;
    }

    public Vec3 getKnownMovement() {
        return this.getControllingPassenger() instanceof Player player && this.isAlive() ? player.getKnownMovement() : this.getDeltaMovement();
    }

    @Nullable
    public ItemStack getWeaponItem() {
        return null;
    }

    public Optional<ResourceKey<LootTable>> getLootTable() {
        return this.type.getDefaultLootTable();
    }

    protected void applyImplicitComponents(DataComponentGetter pComponentGetter) {
        this.applyImplicitComponentIfPresent(pComponentGetter, DataComponents.CUSTOM_NAME);
        this.applyImplicitComponentIfPresent(pComponentGetter, DataComponents.CUSTOM_DATA);
    }

    public final void applyComponentsFromItemStack(ItemStack pStack) {
        this.applyImplicitComponents(pStack.getComponents());
    }

    @Nullable
    @Override
    public <T> T get(DataComponentType<? extends T> p_392678_) {
        if (p_392678_ == DataComponents.CUSTOM_NAME) {
            return castComponentValue((DataComponentType<T>)p_392678_, this.getCustomName());
        } else {
            return p_392678_ == DataComponents.CUSTOM_DATA ? castComponentValue((DataComponentType<T>)p_392678_, this.customData) : null;
        }
    }

    @Nullable
    @Contract("_,!null->!null;_,_->_")
    protected static <T> T castComponentValue(DataComponentType<T> pComponentType, @Nullable Object pValue) {
        return (T)pValue;
    }

    public <T> void setComponent(DataComponentType<T> pComponent, T pValue) {
        this.applyImplicitComponent(pComponent, pValue);
    }

    protected <T> boolean applyImplicitComponent(DataComponentType<T> pComponent, T pValue) {
        if (pComponent == DataComponents.CUSTOM_NAME) {
            this.setCustomName(castComponentValue(DataComponents.CUSTOM_NAME, pValue));
            return true;
        } else if (pComponent == DataComponents.CUSTOM_DATA) {
            this.customData = castComponentValue(DataComponents.CUSTOM_DATA, pValue);
            return true;
        } else {
            return false;
        }
    }

    protected <T> boolean applyImplicitComponentIfPresent(DataComponentGetter pComponentGetter, DataComponentType<T> pComponent) {
        T t = pComponentGetter.get(pComponent);
        return t != null ? this.applyImplicitComponent(pComponent, t) : false;
    }

    @FunctionalInterface
    public interface MoveFunction {
        void accept(Entity pEntity, double pX, double pY, double pZ);
    }

    record Movement(Vec3 from, Vec3 to) {
    }

    public static enum MovementEmission {
        NONE(false, false),
        SOUNDS(true, false),
        EVENTS(false, true),
        ALL(true, true);

        final boolean sounds;
        final boolean events;

        private MovementEmission(final boolean pSounds, final boolean pEvents) {
            this.sounds = pSounds;
            this.events = pEvents;
        }

        public boolean emitsAnything() {
            return this.events || this.sounds;
        }

        public boolean emitsEvents() {
            return this.events;
        }

        public boolean emitsSounds() {
            return this.sounds;
        }
    }

    public static enum RemovalReason {
        KILLED(true, false),
        DISCARDED(true, false),
        UNLOADED_TO_CHUNK(false, true),
        UNLOADED_WITH_PLAYER(false, false),
        CHANGED_DIMENSION(false, false);

        private final boolean destroy;
        private final boolean save;

        private RemovalReason(final boolean pDestroy, final boolean pSave) {
            this.destroy = pDestroy;
            this.save = pSave;
        }

        public boolean shouldDestroy() {
            return this.destroy;
        }

        public boolean shouldSave() {
            return this.save;
        }
    }
}
