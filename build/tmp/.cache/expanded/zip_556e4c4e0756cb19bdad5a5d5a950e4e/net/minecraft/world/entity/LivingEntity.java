package net.minecraft.world.entity;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.doubles.DoubleDoubleImmutablePair;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.BlockUtil;
import net.minecraft.Util;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.item.component.DeathProtection;
import net.minecraft.world.item.component.Weapon;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.effects.EnchantmentLocationBasedEffect;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HoneyBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.PowderSnowBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.jetbrains.annotations.Contract;
import org.slf4j.Logger;

/**
 * @return null or the {@linkplain LivingEntity} it was ignited by
 */
public abstract class LivingEntity extends Entity implements Attackable, net.minecraftforge.common.extensions.IForgeLivingEntity {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TAG_ACTIVE_EFFECTS = "active_effects";
    private static final ResourceLocation SPEED_MODIFIER_POWDER_SNOW_ID = ResourceLocation.withDefaultNamespace("powder_snow");
    private static final ResourceLocation SPRINTING_MODIFIER_ID = ResourceLocation.withDefaultNamespace("sprinting");
    private static final AttributeModifier SPEED_MODIFIER_SPRINTING = new AttributeModifier(SPRINTING_MODIFIER_ID, 0.3F, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    public static final int EQUIPMENT_SLOT_OFFSET = 98;
    public static final int ARMOR_SLOT_OFFSET = 100;
    public static final int BODY_ARMOR_OFFSET = 105;
    public static final int SADDLE_OFFSET = 106;
    public static final int SWING_DURATION = 6;
    public static final int PLAYER_HURT_EXPERIENCE_TIME = 100;
    private static final int DAMAGE_SOURCE_TIMEOUT = 40;
    public static final double MIN_MOVEMENT_DISTANCE = 0.003;
    public static final double DEFAULT_BASE_GRAVITY = 0.08;
    public static final int DEATH_DURATION = 20;
    protected static final float INPUT_FRICTION = 0.98F;
    private static final int TICKS_PER_ELYTRA_FREE_FALL_EVENT = 10;
    private static final int FREE_FALL_EVENTS_PER_ELYTRA_BREAK = 2;
    public static final float BASE_JUMP_POWER = 0.42F;
    private static final double MAX_LINE_OF_SIGHT_TEST_RANGE = 128.0;
    protected static final int LIVING_ENTITY_FLAG_IS_USING = 1;
    protected static final int LIVING_ENTITY_FLAG_OFF_HAND = 2;
    protected static final int LIVING_ENTITY_FLAG_SPIN_ATTACK = 4;
    protected static final EntityDataAccessor<Byte> DATA_LIVING_ENTITY_FLAGS = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Float> DATA_HEALTH_ID = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<List<ParticleOptions>> DATA_EFFECT_PARTICLES = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.PARTICLES);
    private static final EntityDataAccessor<Boolean> DATA_EFFECT_AMBIENCE_ID = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_ARROW_COUNT_ID = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_STINGER_COUNT_ID = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Optional<BlockPos>> SLEEPING_POS_ID = SynchedEntityData.defineId(LivingEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final int PARTICLE_FREQUENCY_WHEN_INVISIBLE = 15;
    protected static final EntityDimensions SLEEPING_DIMENSIONS = EntityDimensions.fixed(0.2F, 0.2F).withEyeHeight(0.2F);
    public static final float EXTRA_RENDER_CULLING_SIZE_WITH_BIG_HAT = 0.5F;
    public static final float DEFAULT_BABY_SCALE = 0.5F;
    public static final String ATTRIBUTES_FIELD = "attributes";
    /** Forge: Use a variant that calls {@link ItemStack#isMonsterDisguise(Player, net.minecraft.world.entity.monster.Monster)} and {@link net.minecraftforge.event.entity.living.MonsterDisguiseEvent} */
    @Deprecated
    public static final Predicate<LivingEntity> PLAYER_NOT_WEARING_DISGUISE_ITEM = p_390518_ -> {
        if (p_390518_ instanceof Player player) {
            ItemStack itemstack = player.getItemBySlot(EquipmentSlot.HEAD);
            return !itemstack.is(ItemTags.GAZE_DISGUISE_EQUIPMENT);
        } else {
            return true;
        }
    };
    private final AttributeMap attributes;
    private final CombatTracker combatTracker = new CombatTracker(this);
    private final Map<Holder<MobEffect>, MobEffectInstance> activeEffects = Maps.newHashMap();
    private final Map<EquipmentSlot, ItemStack> lastEquipmentItems = Util.makeEnumMap(EquipmentSlot.class, p_390519_ -> ItemStack.EMPTY);
    public boolean swinging;
    private boolean discardFriction = false;
    public InteractionHand swingingArm;
    public int swingTime;
    public int removeArrowTime;
    public int removeStingerTime;
    public int hurtTime;
    public int hurtDuration;
    public int deathTime;
    public float oAttackAnim;
    public float attackAnim;
    protected int attackStrengthTicker;
    public final WalkAnimationState walkAnimation = new WalkAnimationState();
    public final int invulnerableDuration = 20;
    public float yBodyRot;
    public float yBodyRotO;
    public float yHeadRot;
    public float yHeadRotO;
    public final ElytraAnimationState elytraAnimationState = new ElytraAnimationState(this);
    @Nullable
    protected EntityReference<Player> lastHurtByPlayer;
    protected int lastHurtByPlayerMemoryTime;
    protected boolean dead;
    protected int noActionTime;
    protected float lastHurt;
    protected boolean jumping;
    public float xxa;
    public float yya;
    public float zza;
    protected InterpolationHandler interpolation = new InterpolationHandler(this);
    protected double lerpYHeadRot;
    protected int lerpHeadSteps;
    private boolean effectsDirty = true;
    @Nullable
    private EntityReference<LivingEntity> lastHurtByMob;
    private int lastHurtByMobTimestamp;
    @Nullable
    private LivingEntity lastHurtMob;
    private int lastHurtMobTimestamp;
    private float speed;
    private int noJumpDelay;
    private float absorptionAmount;
    protected ItemStack useItem = ItemStack.EMPTY;
    protected int useItemRemaining;
    protected int fallFlyTicks;
    private BlockPos lastPos;
    private Optional<BlockPos> lastClimbablePos = Optional.empty();
    @Nullable
    private DamageSource lastDamageSource;
    private long lastDamageStamp;
    protected int autoSpinAttackTicks;
    protected float autoSpinAttackDmg;
    @Nullable
    protected ItemStack autoSpinAttackItemStack;
    private float swimAmount;
    private float swimAmountO;
    protected Brain<?> brain;
    private boolean skipDropExperience;
    private final EnumMap<EquipmentSlot, Reference2ObjectMap<Enchantment, Set<EnchantmentLocationBasedEffect>>> activeLocationDependentEnchantments = new EnumMap<>(EquipmentSlot.class);
    protected final EntityEquipment equipment;

    protected LivingEntity(EntityType<? extends LivingEntity> p_20966_, Level p_20967_) {
        super(p_20966_, p_20967_);
        this.attributes = new AttributeMap(DefaultAttributes.getSupplier(p_20966_));
        this.setHealth(this.getMaxHealth());
        this.equipment = this.createEquipment();
        this.blocksBuilding = true;
        this.reapplyPosition();
        this.setYRot((float)(Math.random() * (float) (Math.PI * 2)));
        this.yHeadRot = this.getYRot();
        NbtOps nbtops = NbtOps.INSTANCE;
        var dyn = new Dynamic<>(nbtops, nbtops.createMap(ImmutableMap.of(nbtops.createString("memories"), nbtops.emptyMap())));
        this.brain = net.minecraftforge.common.ForgeHooks.onLivingMakeBrain(this, this.makeBrain(dyn), dyn);
    }

    @Contract(
        pure = true
    )
    protected EntityEquipment createEquipment() {
        return new EntityEquipment();
    }

    public Brain<?> getBrain() {
        return this.brain;
    }

    protected Brain.Provider<?> brainProvider() {
        return Brain.provider(ImmutableList.of(), ImmutableList.of());
    }

    protected Brain<?> makeBrain(Dynamic<?> pDynamic) {
        return this.brainProvider().makeBrain(pDynamic);
    }

    @Override
    public void kill(ServerLevel p_367431_) {
        this.hurtServer(p_367431_, this.damageSources().genericKill(), Float.MAX_VALUE);
    }

    public boolean canAttackType(EntityType<?> pEntityType) {
        return true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder p_329703_) {
        p_329703_.define(DATA_LIVING_ENTITY_FLAGS, (byte)0);
        p_329703_.define(DATA_EFFECT_PARTICLES, List.of());
        p_329703_.define(DATA_EFFECT_AMBIENCE_ID, false);
        p_329703_.define(DATA_ARROW_COUNT_ID, 0);
        p_329703_.define(DATA_STINGER_COUNT_ID, 0);
        p_329703_.define(DATA_HEALTH_ID, 1.0F);
        p_329703_.define(SLEEPING_POS_ID, Optional.empty());
    }

    public static AttributeSupplier.Builder createLivingAttributes() {
        return AttributeSupplier.builder()
            .add(Attributes.MAX_HEALTH)
            .add(Attributes.KNOCKBACK_RESISTANCE)
            .add(Attributes.MOVEMENT_SPEED)
            .add(Attributes.ARMOR)
            .add(Attributes.ARMOR_TOUGHNESS)
            .add(Attributes.MAX_ABSORPTION)
            .add(Attributes.STEP_HEIGHT)
            .add(Attributes.SCALE)
            .add(Attributes.GRAVITY)
            .add(Attributes.SAFE_FALL_DISTANCE)
            .add(Attributes.FALL_DAMAGE_MULTIPLIER)
            .add(Attributes.JUMP_STRENGTH)
            .add(Attributes.OXYGEN_BONUS)
            .add(Attributes.BURNING_TIME)
            .add(Attributes.EXPLOSION_KNOCKBACK_RESISTANCE)
            .add(Attributes.WATER_MOVEMENT_EFFICIENCY)
            .add(Attributes.MOVEMENT_EFFICIENCY)
            .add(Attributes.ATTACK_KNOCKBACK)
            .add(Attributes.JUMP_STRENGTH);
    }

    @Override
    protected void checkFallDamage(double pY, boolean pOnGround, BlockState pState, BlockPos pPos) {
        if (!this.isInWater()) {
            this.updateInWaterStateAndDoWaterCurrentPushing();
        }

        if (this.level() instanceof ServerLevel serverlevel && pOnGround && this.fallDistance > 0.0) {
            this.onChangedBlock(serverlevel, pPos);
            double d6 = Math.max(0, Mth.floor(this.calculateFallPower(this.fallDistance)));
            if (d6 > 0.0 && !pState.isAir()) {
                double d0 = this.getX();
                double d1 = this.getY();
                double d2 = this.getZ();
                BlockPos blockpos = this.blockPosition();
                if (pPos.getX() != blockpos.getX() || pPos.getZ() != blockpos.getZ()) {
                    double d3 = d0 - pPos.getX() - 0.5;
                    double d4 = d2 - pPos.getZ() - 0.5;
                    double d5 = Math.max(Math.abs(d3), Math.abs(d4));
                    d0 = pPos.getX() + 0.5 + d3 / d5 * 0.5;
                    d2 = pPos.getZ() + 0.5 + d4 / d5 * 0.5;
                }

                double d7 = Math.min(0.2F + d6 / 15.0, 2.5);
                int i = (int)(150.0 * d7);
                if (!pState.addLandingEffects((ServerLevel) this.level(), pPos, pState, this, i))
                ((ServerLevel)this.level()).sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, pState).setPos(pPos), d0, d1, d2, i, 0.0, 0.0, 0.0, 0.15F);
            }
        }

        super.checkFallDamage(pY, pOnGround, pState, pPos);
        if (pOnGround) {
            this.lastClimbablePos = Optional.empty();
        }
    }

    @Deprecated //FORGE: Use canDrownInFluidType instead
    public final boolean canBreatheUnderwater() {
        return this.getType().is(EntityTypeTags.CAN_BREATHE_UNDER_WATER);
    }

    public float getSwimAmount(float pPartialTicks) {
        return Mth.lerp(pPartialTicks, this.swimAmountO, this.swimAmount);
    }

    public boolean hasLandedInLiquid() {
        return this.getDeltaMovement().y() < 1.0E-5F && this.isInLiquid();
    }

    @Override
    public void baseTick() {
        this.oAttackAnim = this.attackAnim;
        if (this.firstTick) {
            this.getSleepingPos().ifPresent(this::setPosToBed);
        }

        if (this.level() instanceof ServerLevel serverlevel) {
            EnchantmentHelper.tickEffects(serverlevel, this);
        }

        super.baseTick();
        ProfilerFiller profilerfiller = Profiler.get();
        profilerfiller.push("livingEntityBaseTick");
        if (this.fireImmune() || this.level().isClientSide) {
            this.clearFire();
        }

        if (this.isAlive() && this.level() instanceof ServerLevel serverlevel1) {
            boolean flag = this instanceof Player;
            if (this.isInWall()) {
                this.hurtServer(serverlevel1, this.damageSources().inWall(), 1.0F);
            } else if (flag && !serverlevel1.getWorldBorder().isWithinBounds(this.getBoundingBox())) {
                double d0 = serverlevel1.getWorldBorder().getDistanceToBorder(this) + serverlevel1.getWorldBorder().getDamageSafeZone();
                if (d0 < 0.0) {
                    double d1 = serverlevel1.getWorldBorder().getDamagePerBlock();
                    if (d1 > 0.0) {
                        this.hurtServer(serverlevel1, this.damageSources().outOfBorder(), Math.max(1, Mth.floor(-d0 * d1)));
                    }
                }
            }


            int airSupply = this.getAirSupply();
            net.minecraftforge.common.ForgeHooks.onLivingBreathe(this, airSupply - decreaseAirSupply(airSupply), increaseAirSupply(airSupply) - airSupply);
            if (false) // Forge: Handled in ForgeHooks#onLivingBreathe(LivingEntity, int, int)
            if (this.isEyeInFluid(FluidTags.WATER)
                && !serverlevel1.getBlockState(BlockPos.containing(this.getX(), this.getEyeY(), this.getZ())).is(Blocks.BUBBLE_COLUMN)) {
                boolean flag1 = !this.canBreatheUnderwater() && !MobEffectUtil.hasWaterBreathing(this) && (!flag || !((Player)this).getAbilities().invulnerable);
                if (flag1) {
                    this.setAirSupply(this.decreaseAirSupply(this.getAirSupply()));
                    if (this.getAirSupply() == -20) {
                        this.setAirSupply(0);
                        serverlevel1.broadcastEntityEvent(this, (byte)67);
                        this.hurtServer(serverlevel1, this.damageSources().drown(), 2.0F);
                    }
                } else if (this.getAirSupply() < this.getMaxAirSupply()) {
                    this.setAirSupply(this.increaseAirSupply(this.getAirSupply()));
                }

                if (this.isPassenger() && this.getVehicle() != null && this.getVehicle().dismountsUnderwater()) {
                    this.stopRiding();
                }
            } else if (this.getAirSupply() < this.getMaxAirSupply()) {
                this.setAirSupply(this.increaseAirSupply(this.getAirSupply()));
            }

            BlockPos blockpos = this.blockPosition();
            if (!Objects.equal(this.lastPos, blockpos)) {
                this.lastPos = blockpos;
                this.onChangedBlock(serverlevel1, blockpos);
            }
        }

        if (this.hurtTime > 0) {
            this.hurtTime--;
        }

        if (this.invulnerableTime > 0 && !(this instanceof ServerPlayer)) {
            this.invulnerableTime--;
        }

        if (this.isDeadOrDying() && this.level().shouldTickDeath(this)) {
            this.tickDeath();
        }

        if (this.lastHurtByPlayerMemoryTime > 0) {
            this.lastHurtByPlayerMemoryTime--;
        } else {
            this.lastHurtByPlayer = null;
        }

        if (this.lastHurtMob != null && !this.lastHurtMob.isAlive()) {
            this.lastHurtMob = null;
        }

        LivingEntity livingentity = this.getLastHurtByMob();
        if (livingentity != null) {
            if (!livingentity.isAlive()) {
                this.setLastHurtByMob(null);
            } else if (this.tickCount - this.lastHurtByMobTimestamp > 100) {
                this.setLastHurtByMob(null);
            }
        }

        this.tickEffects();
        this.yHeadRotO = this.yHeadRot;
        this.yBodyRotO = this.yBodyRot;
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
        profilerfiller.pop();
    }

    @Override
    protected float getBlockSpeedFactor() {
        return Mth.lerp((float)this.getAttributeValue(Attributes.MOVEMENT_EFFICIENCY), super.getBlockSpeedFactor(), 1.0F);
    }

    public float getLuck() {
        return 0.0F;
    }

    protected void removeFrost() {
        AttributeInstance attributeinstance = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attributeinstance != null) {
            if (attributeinstance.getModifier(SPEED_MODIFIER_POWDER_SNOW_ID) != null) {
                attributeinstance.removeModifier(SPEED_MODIFIER_POWDER_SNOW_ID);
            }
        }
    }

    protected void tryAddFrost() {
        if (!this.getBlockStateOnLegacy().isAir()) {
            int i = this.getTicksFrozen();
            if (i > 0) {
                AttributeInstance attributeinstance = this.getAttribute(Attributes.MOVEMENT_SPEED);
                if (attributeinstance == null) {
                    return;
                }

                float f = -0.05F * this.getPercentFrozen();
                attributeinstance.addTransientModifier(new AttributeModifier(SPEED_MODIFIER_POWDER_SNOW_ID, f, AttributeModifier.Operation.ADD_VALUE));
            }
        }
    }

    protected void onChangedBlock(ServerLevel pLevel, BlockPos pPos) {
        EnchantmentHelper.runLocationChangedEffects(pLevel, this);
    }

    public boolean isBaby() {
        return false;
    }

    public float getAgeScale() {
        return this.isBaby() ? 0.5F : 1.0F;
    }

    public final float getScale() {
        AttributeMap attributemap = this.getAttributes();
        return attributemap == null ? 1.0F : this.sanitizeScale((float)attributemap.getValue(Attributes.SCALE));
    }

    protected float sanitizeScale(float pScale) {
        return pScale;
    }

    public boolean isAffectedByFluids() {
        return true;
    }

    protected void tickDeath() {
        this.deathTime++;
        if (this.deathTime >= 20 && !this.level().isClientSide() && !this.isRemoved()) {
            this.level().broadcastEntityEvent(this, (byte)60);
            this.remove(Entity.RemovalReason.KILLED);
        }
    }

    public boolean shouldDropExperience() {
        return !this.isBaby();
    }

    protected boolean shouldDropLoot() {
        return !this.isBaby();
    }

    protected int decreaseAirSupply(int pCurrentAir) {
        AttributeInstance attributeinstance = this.getAttribute(Attributes.OXYGEN_BONUS);
        double d0;
        if (attributeinstance != null) {
            d0 = attributeinstance.getValue();
        } else {
            d0 = 0.0;
        }

        return d0 > 0.0 && this.random.nextDouble() >= 1.0 / (d0 + 1.0) ? pCurrentAir : pCurrentAir - 1;
    }

    protected int increaseAirSupply(int pCurrentAir) {
        return Math.min(pCurrentAir + 4, this.getMaxAirSupply());
    }

    public final int getExperienceReward(ServerLevel pLevel, @Nullable Entity pKiller) {
        return EnchantmentHelper.processMobExperience(pLevel, pKiller, this, this.getBaseExperienceReward(pLevel));
    }

    protected int getBaseExperienceReward(ServerLevel pLevel) {
        return 0;
    }

    protected boolean isAlwaysExperienceDropper() {
        return false;
    }

    @Nullable
    public LivingEntity getLastHurtByMob() {
        return EntityReference.get(this.lastHurtByMob, this.level(), LivingEntity.class);
    }

    @Nullable
    public Player getLastHurtByPlayer() {
        return EntityReference.get(this.lastHurtByPlayer, this.level(), Player.class);
    }

    @Override
    public LivingEntity getLastAttacker() {
        return this.getLastHurtByMob();
    }

    public int getLastHurtByMobTimestamp() {
        return this.lastHurtByMobTimestamp;
    }

    public void setLastHurtByPlayer(Player pPlayer, int pMemoryTime) {
        this.setLastHurtByPlayer(new EntityReference<>(pPlayer), pMemoryTime);
    }

    public void setLastHurtByPlayer(UUID pUuid, int pMemoryTime) {
        this.setLastHurtByPlayer(new EntityReference<>(pUuid), pMemoryTime);
    }

    private void setLastHurtByPlayer(EntityReference<Player> pPlayer, int pMemoryTime) {
        this.lastHurtByPlayer = pPlayer;
        this.lastHurtByPlayerMemoryTime = pMemoryTime;
    }

    public void setLastHurtByMob(@Nullable LivingEntity pLivingEntity) {
        this.lastHurtByMob = pLivingEntity != null ? new EntityReference<>(pLivingEntity) : null;
        this.lastHurtByMobTimestamp = this.tickCount;
    }

    @Nullable
    public LivingEntity getLastHurtMob() {
        return this.lastHurtMob;
    }

    public int getLastHurtMobTimestamp() {
        return this.lastHurtMobTimestamp;
    }

    public void setLastHurtMob(Entity pEntity) {
        if (pEntity instanceof LivingEntity) {
            this.lastHurtMob = (LivingEntity)pEntity;
        } else {
            this.lastHurtMob = null;
        }

        this.lastHurtMobTimestamp = this.tickCount;
    }

    public int getNoActionTime() {
        return this.noActionTime;
    }

    public void setNoActionTime(int pIdleTime) {
        this.noActionTime = pIdleTime;
    }

    public boolean shouldDiscardFriction() {
        return this.discardFriction;
    }

    public void setDiscardFriction(boolean pDiscardFriction) {
        this.discardFriction = pDiscardFriction;
    }

    protected boolean doesEmitEquipEvent(EquipmentSlot pSlot) {
        return true;
    }

    public void onEquipItem(EquipmentSlot pSlot, ItemStack pOldItem, ItemStack pNewItem) {
        if (!this.level().isClientSide() && !this.isSpectator()) {
            if (!ItemStack.isSameItemSameComponents(pOldItem, pNewItem) && !this.firstTick) {
                Equippable equippable = pNewItem.get(DataComponents.EQUIPPABLE);
                if (!this.isSilent() && equippable != null && pSlot == equippable.slot()) {
                    this.level()
                        .playSeededSound(
                            null,
                            this.getX(),
                            this.getY(),
                            this.getZ(),
                            this.getEquipSound(pSlot, pNewItem, equippable),
                            this.getSoundSource(),
                            1.0F,
                            1.0F,
                            this.random.nextLong()
                        );
                }

                if (this.doesEmitEquipEvent(pSlot)) {
                    this.gameEvent(equippable != null ? GameEvent.EQUIP : GameEvent.UNEQUIP);
                }
            }
        }
    }

    protected Holder<SoundEvent> getEquipSound(EquipmentSlot pSlot, ItemStack pStack, Equippable pEquippable) {
        return pEquippable.equipSound();
    }

    @Override
    public void remove(Entity.RemovalReason p_276115_) {
        if ((p_276115_ == Entity.RemovalReason.KILLED || p_276115_ == Entity.RemovalReason.DISCARDED) && this.level() instanceof ServerLevel serverlevel) {
            this.triggerOnDeathMobEffects(serverlevel, p_276115_);
        }

        super.remove(p_276115_);
        this.brain.clearMemories();
    }

    protected void triggerOnDeathMobEffects(ServerLevel pLevel, Entity.RemovalReason pRemovalReason) {
        for (MobEffectInstance mobeffectinstance : this.getActiveEffects()) {
            mobeffectinstance.onMobRemoved(pLevel, this, pRemovalReason);
        }

        this.activeEffects.clear();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        pCompound.putFloat("Health", this.getHealth());
        pCompound.putShort("HurtTime", (short)this.hurtTime);
        pCompound.putInt("HurtByTimestamp", this.lastHurtByMobTimestamp);
        pCompound.putShort("DeathTime", (short)this.deathTime);
        pCompound.putFloat("AbsorptionAmount", this.getAbsorptionAmount());
        pCompound.put("attributes", this.getAttributes().save());
        RegistryOps<Tag> registryops = this.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        if (!this.activeEffects.isEmpty()) {
            pCompound.store("active_effects", MobEffectInstance.CODEC.listOf(), registryops, List.copyOf(this.activeEffects.values()));
        }

        pCompound.putBoolean("FallFlying", this.isFallFlying());
        this.getSleepingPos().ifPresent(p_390517_ -> pCompound.store("sleeping_pos", BlockPos.CODEC, p_390517_));
        DataResult<Tag> dataresult = this.brain.serializeStart(NbtOps.INSTANCE);
        dataresult.resultOrPartial(LOGGER::error).ifPresent(p_21102_ -> pCompound.put("Brain", p_21102_));
        if (this.lastHurtByPlayer != null) {
            this.lastHurtByPlayer.store(pCompound, "last_hurt_by_player");
            pCompound.putInt("last_hurt_by_player_memory_time", this.lastHurtByPlayerMemoryTime);
        }

        if (this.lastHurtByMob != null) {
            this.lastHurtByMob.store(pCompound, "last_hurt_by_mob");
            pCompound.putInt("ticks_since_last_hurt_by_mob", this.tickCount - this.lastHurtByMobTimestamp);
        }

        if (!this.equipment.isEmpty()) {
            pCompound.store("equipment", EntityEquipment.CODEC, registryops, this.equipment);
        }
    }

    @Nullable
    public ItemEntity drop(ItemStack pStack, boolean pRandomizeMotion, boolean pIncludeThrower) {
        if (pStack.isEmpty()) {
            return null;
        } else if (this.level().isClientSide) {
            this.swing(InteractionHand.MAIN_HAND);
            return null;
        } else {
            ItemEntity itementity = this.createItemStackToDrop(pStack, pRandomizeMotion, pIncludeThrower);
            if (itementity != null) {
                if (captureDrops() != null)
                    captureDrops().add(itementity);
                else
                this.level().addFreshEntity(itementity);
            }

            return itementity;
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        this.internalSetAbsorptionAmount(pCompound.getFloatOr("AbsorptionAmount", 0.0F));
        if (this.level() != null && !this.level().isClientSide) {
            pCompound.getList("attributes").ifPresent(this.getAttributes()::load);
        }

        RegistryOps<Tag> registryops = this.registryAccess().createSerializationContext(NbtOps.INSTANCE);
        List<MobEffectInstance> list = pCompound.read("active_effects", MobEffectInstance.CODEC.listOf(), registryops).orElse(List.of());
        this.activeEffects.clear();

        for (MobEffectInstance mobeffectinstance : list) {
            this.activeEffects.put(mobeffectinstance.getEffect(), mobeffectinstance);
        }

        this.setHealth(pCompound.getFloatOr("Health", this.getMaxHealth()));
        this.hurtTime = pCompound.getShortOr("HurtTime", (short)0);
        this.deathTime = pCompound.getShortOr("DeathTime", (short)0);
        this.lastHurtByMobTimestamp = pCompound.getIntOr("HurtByTimestamp", 0);
        pCompound.getString("Team").ifPresent(p_390514_ -> {
            Scoreboard scoreboard = this.level().getScoreboard();
            PlayerTeam playerteam = scoreboard.getPlayerTeam(p_390514_);
            boolean flag = playerteam != null && scoreboard.addPlayerToTeam(this.getStringUUID(), playerteam);
            if (!flag) {
                LOGGER.warn("Unable to add mob to team \"{}\" (that team probably doesn't exist)", p_390514_);
            }
        });
        this.setSharedFlag(7, pCompound.getBooleanOr("FallFlying", false));
        pCompound.read("sleeping_pos", BlockPos.CODEC).ifPresentOrElse(p_390515_ -> {
            this.setSleepingPos(p_390515_);
            this.entityData.set(DATA_POSE, Pose.SLEEPING);
            if (!this.firstTick) {
                this.setPosToBed(p_390515_);
            }
        }, this::clearSleepingPos);
        pCompound.getCompound("Brain").ifPresent(p_390523_ -> {
            var dyn = new Dynamic<>(NbtOps.INSTANCE, p_390523_);
            this.brain = net.minecraftforge.common.ForgeHooks.onLivingMakeBrain(this, this.makeBrain(dyn), dyn);
        });
        this.lastHurtByPlayer = EntityReference.read(pCompound, "last_hurt_by_player");
        this.lastHurtByPlayerMemoryTime = pCompound.getIntOr("last_hurt_by_player_memory_time", 0);
        this.lastHurtByMob = EntityReference.read(pCompound, "last_hurt_by_mob");
        this.lastHurtByMobTimestamp = pCompound.getIntOr("ticks_since_last_hurt_by_mob", 0) + this.tickCount;
        this.equipment.setAll(pCompound.read("equipment", EntityEquipment.CODEC, registryops).orElseGet(EntityEquipment::new));
    }

    protected void tickEffects() {
        if (this.level() instanceof ServerLevel serverlevel) {
            Iterator<Holder<MobEffect>> iterator = this.activeEffects.keySet().iterator();

            try {
                while (iterator.hasNext()) {
                    Holder<MobEffect> holder = iterator.next();
                    MobEffectInstance mobeffectinstance = this.activeEffects.get(holder);
                    if (!mobeffectinstance.tickServer(serverlevel, this, () -> this.onEffectUpdated(mobeffectinstance, true, null))) {
                    if (!net.minecraftforge.event.ForgeEventFactory.onLivingEffectExpire(this, mobeffectinstance)) {
                        iterator.remove();
                        this.onEffectsRemoved(List.of(mobeffectinstance));
                    }
                    } else if (mobeffectinstance.getDuration() % 600 == 0) {
                        this.onEffectUpdated(mobeffectinstance, false, null);
                    }
                }
            } catch (ConcurrentModificationException concurrentmodificationexception) {
            }

            if (this.effectsDirty) {
                this.updateInvisibilityStatus();
                this.updateGlowingStatus();
                this.effectsDirty = false;
            }
        } else {
            for (MobEffectInstance mobeffectinstance1 : this.activeEffects.values()) {
                mobeffectinstance1.tickClient();
            }

            List<ParticleOptions> list = this.entityData.get(DATA_EFFECT_PARTICLES);
            if (!list.isEmpty()) {
                boolean flag = this.entityData.get(DATA_EFFECT_AMBIENCE_ID);
                int j = this.isInvisible() ? 15 : 4;
                int i = flag ? 5 : 1;
                if (this.random.nextInt(j * i) == 0) {
                    this.level().addParticle(Util.getRandom(list, this.random), this.getRandomX(0.5), this.getRandomY(), this.getRandomZ(0.5), 1.0, 1.0, 1.0);
                }
            }
        }
    }

    protected void updateInvisibilityStatus() {
        if (this.activeEffects.isEmpty()) {
            this.removeEffectParticles();
            this.setInvisible(false);
        } else {
            this.setInvisible(this.hasEffect(MobEffects.INVISIBILITY));
            this.updateSynchronizedMobEffectParticles();
        }
    }

    private void updateSynchronizedMobEffectParticles() {
        List<ParticleOptions> list = this.activeEffects.values().stream().filter(MobEffectInstance::isVisible).map(MobEffectInstance::getParticleOptions).toList();
        this.entityData.set(DATA_EFFECT_PARTICLES, list);
        this.entityData.set(DATA_EFFECT_AMBIENCE_ID, areAllEffectsAmbient(this.activeEffects.values()));
    }

    private void updateGlowingStatus() {
        boolean flag = this.isCurrentlyGlowing();
        if (this.getSharedFlag(6) != flag) {
            this.setSharedFlag(6, flag);
        }
    }

    public double getVisibilityPercent(@Nullable Entity pLookingEntity) {
        double d0 = 1.0;
        if (this.isDiscrete()) {
            d0 *= 0.8;
        }

        if (this.isInvisible()) {
            float f = this.getArmorCoverPercentage();
            if (f < 0.1F) {
                f = 0.1F;
            }

            d0 *= 0.7 * f;
        }

        if (pLookingEntity != null) {
            ItemStack itemstack = this.getItemBySlot(EquipmentSlot.HEAD);
            EntityType<?> entitytype = pLookingEntity.getType();
            if (entitytype == EntityType.SKELETON && itemstack.is(Items.SKELETON_SKULL)
                || entitytype == EntityType.ZOMBIE && itemstack.is(Items.ZOMBIE_HEAD)
                || entitytype == EntityType.PIGLIN && itemstack.is(Items.PIGLIN_HEAD)
                || entitytype == EntityType.PIGLIN_BRUTE && itemstack.is(Items.PIGLIN_HEAD)
                || entitytype == EntityType.CREEPER && itemstack.is(Items.CREEPER_HEAD)) {
                d0 *= 0.5;
            }
        }

        d0 = net.minecraftforge.common.ForgeHooks.getEntityVisibilityMultiplier(this, pLookingEntity, d0);
        return d0;
    }

    public boolean canAttack(LivingEntity pTarget) {
        return pTarget instanceof Player && this.level().getDifficulty() == Difficulty.PEACEFUL ? false : pTarget.canBeSeenAsEnemy();
    }

    public boolean canBeSeenAsEnemy() {
        return !this.isInvulnerable() && this.canBeSeenByAnyone();
    }

    public boolean canBeSeenByAnyone() {
        return !this.isSpectator() && this.isAlive();
    }

    public static boolean areAllEffectsAmbient(Collection<MobEffectInstance> pPotionEffects) {
        for (MobEffectInstance mobeffectinstance : pPotionEffects) {
            if (mobeffectinstance.isVisible() && !mobeffectinstance.isAmbient()) {
                return false;
            }
        }

        return true;
    }

    protected void removeEffectParticles() {
        this.entityData.set(DATA_EFFECT_PARTICLES, List.of());
    }

    public boolean removeAllEffects() {
        if (this.level().isClientSide) {
            return false;
        } else if (this.activeEffects.isEmpty()) {
            return false;
        } else {
            Map<Holder<MobEffect>, MobEffectInstance> map = Maps.newHashMap(this.activeEffects);
            this.activeEffects.clear();
            this.onEffectsRemoved(map.values());
            return true;
        }
    }

    public Collection<MobEffectInstance> getActiveEffects() {
        return this.activeEffects.values();
    }

    public Map<Holder<MobEffect>, MobEffectInstance> getActiveEffectsMap() {
        return this.activeEffects;
    }

    public boolean hasEffect(Holder<MobEffect> pEffect) {
        return this.activeEffects.containsKey(pEffect);
    }

    @Nullable
    public MobEffectInstance getEffect(Holder<MobEffect> pEffect) {
        return this.activeEffects.get(pEffect);
    }

    public float getEffectBlendFactor(Holder<MobEffect> pEffect, float pPartialTick) {
        MobEffectInstance mobeffectinstance = this.getEffect(pEffect);
        return mobeffectinstance != null ? mobeffectinstance.getBlendFactor(this, pPartialTick) : 0.0F;
    }

    public final boolean addEffect(MobEffectInstance pEffectInstance) {
        return this.addEffect(pEffectInstance, null);
    }

    public boolean addEffect(MobEffectInstance pEffectInstance, @Nullable Entity pEntity) {
        if (!this.canBeAffected(pEffectInstance)) {
            return false;
        } else {
            MobEffectInstance mobeffectinstance = this.activeEffects.get(pEffectInstance.getEffect());
            boolean flag = false;
            net.minecraftforge.event.ForgeEventFactory.onLivingEffectAdd(this, mobeffectinstance, pEffectInstance, pEntity);
            if (mobeffectinstance == null) {
                this.activeEffects.put(pEffectInstance.getEffect(), pEffectInstance);
                this.onEffectAdded(pEffectInstance, pEntity);
                flag = true;
                pEffectInstance.onEffectAdded(this);
            } else if (mobeffectinstance.update(pEffectInstance)) {
                this.onEffectUpdated(mobeffectinstance, true, pEntity);
                flag = true;
            }

            pEffectInstance.onEffectStarted(this);
            return flag;
        }
    }

    public boolean canBeAffected(MobEffectInstance pEffectInstance) {
        var eventResult = net.minecraftforge.event.ForgeEventFactory.onLivingEffectCanApply(this, pEffectInstance).getResult();
        if (!eventResult.isDefault()) {
            return eventResult.isAllowed();
        }
        if (this.getType().is(EntityTypeTags.IMMUNE_TO_INFESTED)) {
            return !pEffectInstance.is(MobEffects.INFESTED);
        } else if (this.getType().is(EntityTypeTags.IMMUNE_TO_OOZING)) {
            return !pEffectInstance.is(MobEffects.OOZING);
        } else {
            return !this.getType().is(EntityTypeTags.IGNORES_POISON_AND_REGEN)
                ? true
                : !pEffectInstance.is(MobEffects.REGENERATION) && !pEffectInstance.is(MobEffects.POISON);
        }
    }

    public void forceAddEffect(MobEffectInstance pInstance, @Nullable Entity pEntity) {
        if (this.canBeAffected(pInstance)) {
            MobEffectInstance mobeffectinstance = this.activeEffects.put(pInstance.getEffect(), pInstance);
            if (mobeffectinstance == null) {
                this.onEffectAdded(pInstance, pEntity);
            } else {
                pInstance.copyBlendState(mobeffectinstance);
                this.onEffectUpdated(pInstance, true, pEntity);
            }
        }
    }

    public boolean isInvertedHealAndHarm() {
        return this.getType().is(EntityTypeTags.INVERTED_HEALING_AND_HARM);
    }

    @Nullable
    public final MobEffectInstance removeEffectNoUpdate(Holder<MobEffect> pEffect) {
        return this.activeEffects.remove(pEffect);
    }

    public boolean removeEffect(Holder<MobEffect> pEffect) {
        if (net.minecraftforge.event.ForgeEventFactory.onLivingEffectRemove(this, pEffect.get())) {
            return false;
        }
        MobEffectInstance mobeffectinstance = this.removeEffectNoUpdate(pEffect);
        if (mobeffectinstance != null) {
            this.onEffectsRemoved(List.of(mobeffectinstance));
            return true;
        } else {
            return false;
        }
    }

    protected void onEffectAdded(MobEffectInstance pEffectInstance, @Nullable Entity pEntity) {
        if (!this.level().isClientSide) {
            this.effectsDirty = true;
            pEffectInstance.getEffect().value().addAttributeModifiers(this.getAttributes(), pEffectInstance.getAmplifier());
            this.sendEffectToPassengers(pEffectInstance);
        }
    }

    public void sendEffectToPassengers(MobEffectInstance pEffectInstance) {
        for (Entity entity : this.getPassengers()) {
            if (entity instanceof ServerPlayer serverplayer) {
                serverplayer.connection.send(new ClientboundUpdateMobEffectPacket(this.getId(), pEffectInstance, false));
            }
        }
    }

    protected void onEffectUpdated(MobEffectInstance pEffectInstance, boolean pForced, @Nullable Entity pEntity) {
        if (!this.level().isClientSide) {
            this.effectsDirty = true;
            if (pForced) {
                MobEffect mobeffect = pEffectInstance.getEffect().value();
                mobeffect.removeAttributeModifiers(this.getAttributes());
                mobeffect.addAttributeModifiers(this.getAttributes(), pEffectInstance.getAmplifier());
                this.refreshDirtyAttributes();
            }

            this.sendEffectToPassengers(pEffectInstance);
        }
    }

    protected void onEffectsRemoved(Collection<MobEffectInstance> pEffects) {
        if (!this.level().isClientSide) {
            this.effectsDirty = true;

            for (MobEffectInstance mobeffectinstance : pEffects) {
                if (net.minecraftforge.event.ForgeEventFactory.onLivingEffectRemove(this, mobeffectinstance)) {
                    continue;
                }
                mobeffectinstance.getEffect().value().removeAttributeModifiers(this.getAttributes());

                for (Entity entity : this.getPassengers()) {
                    if (entity instanceof ServerPlayer serverplayer) {
                        serverplayer.connection.send(new ClientboundRemoveMobEffectPacket(this.getId(), mobeffectinstance.getEffect()));
                    }
                }
            }

            this.refreshDirtyAttributes();
        }
    }

    private void refreshDirtyAttributes() {
        Set<AttributeInstance> set = this.getAttributes().getAttributesToUpdate();

        for (AttributeInstance attributeinstance : set) {
            this.onAttributeUpdated(attributeinstance.getAttribute());
        }

        set.clear();
    }

    protected void onAttributeUpdated(Holder<Attribute> pAttribute) {
        if (pAttribute.is(Attributes.MAX_HEALTH)) {
            float f = this.getMaxHealth();
            if (this.getHealth() > f) {
                this.setHealth(f);
            }
        } else if (pAttribute.is(Attributes.MAX_ABSORPTION)) {
            float f1 = this.getMaxAbsorption();
            if (this.getAbsorptionAmount() > f1) {
                this.setAbsorptionAmount(f1);
            }
        } else if (pAttribute.is(Attributes.SCALE)) {
            this.refreshDimensions();
        }
    }

    public void heal(float pHealAmount) {
        pHealAmount = net.minecraftforge.event.ForgeEventFactory.onLivingHeal(this, pHealAmount);
        if (pHealAmount <= 0) {
            return;
        }
        float f = this.getHealth();
        if (f > 0.0F) {
            this.setHealth(f + pHealAmount);
        }
    }

    public float getHealth() {
        return this.entityData.get(DATA_HEALTH_ID);
    }

    public void setHealth(float pHealth) {
        this.entityData.set(DATA_HEALTH_ID, Mth.clamp(pHealth, 0.0F, this.getMaxHealth()));
    }

    public boolean isDeadOrDying() {
        return this.getHealth() <= 0.0F;
    }

    @Override
    public boolean hurtServer(ServerLevel p_361743_, DamageSource p_361865_, float p_365677_) {
        if (!net.minecraftforge.common.ForgeHooks.onLivingAttack(this, p_361865_, p_365677_)) {
            return false;
        }
        if (this.isInvulnerableTo(p_361743_, p_361865_)) {
            return false;
        } else if (this.isDeadOrDying()) {
            return false;
        } else if (p_361865_.is(DamageTypeTags.IS_FIRE) && this.hasEffect(MobEffects.FIRE_RESISTANCE)) {
            return false;
        } else {
            if (this.isSleeping()) {
                this.stopSleeping();
            }

            this.noActionTime = 0;
            if (p_365677_ < 0.0F) {
                p_365677_ = 0.0F;
            }

            float f = this.applyItemBlocking(p_361743_, p_361865_, p_365677_);
            p_365677_ -= f;
            boolean flag = f > 0.0F;
            if (p_361865_.is(DamageTypeTags.IS_FREEZING) && this.getType().is(EntityTypeTags.FREEZE_HURTS_EXTRA_TYPES)) {
                p_365677_ *= 5.0F;
            }

            if (p_361865_.is(DamageTypeTags.DAMAGES_HELMET) && !this.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
                this.hurtHelmet(p_361865_, p_365677_);
                p_365677_ *= 0.75F;
            }

            if (Float.isNaN(p_365677_) || Float.isInfinite(p_365677_)) {
                p_365677_ = Float.MAX_VALUE;
            }

            boolean flag1 = true;
            if (this.invulnerableTime > 10.0F && !p_361865_.is(DamageTypeTags.BYPASSES_COOLDOWN)) {
                if (p_365677_ <= this.lastHurt) {
                    return false;
                }

                this.actuallyHurt(p_361743_, p_361865_, p_365677_ - this.lastHurt);
                this.lastHurt = p_365677_;
                flag1 = false;
            } else {
                this.lastHurt = p_365677_;
                this.invulnerableTime = 20;
                this.actuallyHurt(p_361743_, p_361865_, p_365677_);
                this.hurtDuration = 10;
                this.hurtTime = this.hurtDuration;
            }

            this.resolveMobResponsibleForDamage(p_361865_);
            this.resolvePlayerResponsibleForDamage(p_361865_);
            if (flag1) {
                BlocksAttacks blocksattacks = this.getUseItem().get(DataComponents.BLOCKS_ATTACKS);
                if (flag && blocksattacks != null) {
                    blocksattacks.onBlocked(p_361743_, this);
                } else {
                    p_361743_.broadcastDamageEvent(this, p_361865_);
                }

                if (!p_361865_.is(DamageTypeTags.NO_IMPACT) && (!flag || p_365677_ > 0.0F)) {
                    this.markHurt();
                }

                if (!p_361865_.is(DamageTypeTags.NO_KNOCKBACK)) {
                    double d0 = 0.0;
                    double d1 = 0.0;
                    if (p_361865_.getDirectEntity() instanceof Projectile projectile) {
                        DoubleDoubleImmutablePair doubledoubleimmutablepair = projectile.calculateHorizontalHurtKnockbackDirection(this, p_361865_);
                        d0 = -doubledoubleimmutablepair.leftDouble();
                        d1 = -doubledoubleimmutablepair.rightDouble();
                    } else if (p_361865_.getSourcePosition() != null) {
                        d0 = p_361865_.getSourcePosition().x() - this.getX();
                        d1 = p_361865_.getSourcePosition().z() - this.getZ();
                    }

                    this.knockback(0.4F, d0, d1);
                    if (!flag) {
                        this.indicateDamage(d0, d1);
                    }
                }
            }

            if (this.isDeadOrDying()) {
                if (!this.checkTotemDeathProtection(p_361865_)) {
                    if (flag1) {
                        this.makeSound(this.getDeathSound());
                        this.playSecondaryHurtSound(p_361865_);
                    }

                    this.die(p_361865_);
                }
            } else if (flag1) {
                this.playHurtSound(p_361865_);
                this.playSecondaryHurtSound(p_361865_);
            }

            boolean flag2 = !flag || p_365677_ > 0.0F;
            if (flag2) {
                this.lastDamageSource = p_361865_;
                this.lastDamageStamp = this.level().getGameTime();

                for (MobEffectInstance mobeffectinstance : this.getActiveEffects()) {
                    mobeffectinstance.onMobHurt(p_361743_, this, p_361865_, p_365677_);
                }
            }

            if (this instanceof ServerPlayer serverplayer) {
                CriteriaTriggers.ENTITY_HURT_PLAYER.trigger(serverplayer, p_361865_, p_365677_, p_365677_, flag);
                if (f > 0.0F && f < 3.4028235E37F) {
                    serverplayer.awardStat(Stats.DAMAGE_BLOCKED_BY_SHIELD, Math.round(f * 10.0F));
                }
            }

            if (p_361865_.getEntity() instanceof ServerPlayer serverplayer1) {
                CriteriaTriggers.PLAYER_HURT_ENTITY.trigger(serverplayer1, this, p_361865_, p_365677_, p_365677_, flag);
            }

            return flag2;
        }
    }

    public float applyItemBlocking(ServerLevel pLevel, DamageSource pDamageSource, float pDamageAmount) {
        if (pDamageAmount <= 0.0F) {
            return 0.0F;
        } else {
            ItemStack itemstack = this.getItemBlockingWith();
            if (itemstack == null) {
                return 0.0F;
            } else {
                BlocksAttacks blocksattacks = itemstack.get(DataComponents.BLOCKS_ATTACKS);
                if (blocksattacks != null && !blocksattacks.bypassedBy().map(pDamageSource::is).orElse(false)) {
                    if (pDamageSource.getDirectEntity() instanceof AbstractArrow abstractarrow && abstractarrow.getPierceLevel() > 0) {
                        return 0.0F;
                    } else {
                        Vec3 vec3 = pDamageSource.getSourcePosition();
                        double d0;
                        if (vec3 != null) {
                            Vec3 vec31 = this.calculateViewVector(0.0F, this.getYHeadRot());
                            Vec3 vec32 = vec3.subtract(this.position());
                            vec32 = new Vec3(vec32.x, 0.0, vec32.z).normalize();
                            d0 = Math.acos(vec32.dot(vec31));
                        } else {
                            d0 = (float) Math.PI;
                        }

                        float f = blocksattacks.resolveBlockedDamage(pDamageSource, pDamageAmount, d0);
                        var ev = net.minecraftforge.event.ForgeEventFactory.onShieldBlock(this, pDamageSource, f, itemstack);
                        if (ev.isCanceled()) return 0.0F;
                        f = ev.getBlockedDamage();
                        if (ev.shieldTakesDamage())
                        blocksattacks.hurtBlockingItem(this.level(), itemstack, this, this.getUsedItemHand(), f);
                        if (!pDamageSource.is(DamageTypeTags.IS_PROJECTILE) && pDamageSource.getDirectEntity() instanceof LivingEntity livingentity) {
                            this.blockUsingItem(pLevel, livingentity);
                        }

                        return f;
                    }
                } else {
                    return 0.0F;
                }
            }
        }
    }

    private void playSecondaryHurtSound(DamageSource pDamageSource) {
        if (pDamageSource.is(DamageTypes.THORNS)) {
            SoundSource soundsource = this instanceof Player ? SoundSource.PLAYERS : SoundSource.HOSTILE;
            this.level().playSound(null, this.position().x, this.position().y, this.position().z, SoundEvents.THORNS_HIT, soundsource);
        }
    }

    protected void resolveMobResponsibleForDamage(DamageSource pDamageSource) {
        if (pDamageSource.getEntity() instanceof LivingEntity livingentity
            && !pDamageSource.is(DamageTypeTags.NO_ANGER)
            && (!pDamageSource.is(DamageTypes.WIND_CHARGE) || !this.getType().is(EntityTypeTags.NO_ANGER_FROM_WIND_CHARGE))) {
            this.setLastHurtByMob(livingentity);
        }
    }

    @Nullable
    protected Player resolvePlayerResponsibleForDamage(DamageSource pDamageSource) {
        Entity entity = pDamageSource.getEntity();
        if (entity instanceof Player player) {
            this.setLastHurtByPlayer(player, 100);
        } else if (entity instanceof net.minecraft.world.entity.TamableAnimal wolf && wolf.isTame()) {
            if (wolf.getOwnerReference() != null) {
                this.setLastHurtByPlayer(wolf.getOwnerReference().getUUID(), 100);
            } else {
                this.lastHurtByPlayer = null;
                this.lastHurtByPlayerMemoryTime = 0;
            }
        }

        return EntityReference.get(this.lastHurtByPlayer, this.level(), Player.class);
    }

    protected void blockUsingItem(ServerLevel pLevel, LivingEntity pEntity) {
        pEntity.blockedByItem(this);
    }

    protected void blockedByItem(LivingEntity pEntity) {
        pEntity.knockback(0.5, pEntity.getX() - this.getX(), pEntity.getZ() - this.getZ());
    }

    private boolean checkTotemDeathProtection(DamageSource pDamageSource) {
        if (pDamageSource.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        } else {
            ItemStack itemstack = null;
            DeathProtection deathprotection = null;

            for (InteractionHand interactionhand : InteractionHand.values()) {
                ItemStack itemstack1 = this.getItemInHand(interactionhand);
                deathprotection = itemstack1.get(DataComponents.DEATH_PROTECTION);
                if (deathprotection != null && net.minecraftforge.common.ForgeHooks.onLivingUseTotem(this, pDamageSource, itemstack1, interactionhand)) {
                    itemstack = itemstack1.copy();
                    itemstack1.shrink(1);
                    break;
                }
            }

            if (itemstack != null) {
                if (this instanceof ServerPlayer serverplayer) {
                    serverplayer.awardStat(Stats.ITEM_USED.get(itemstack.getItem()));
                    CriteriaTriggers.USED_TOTEM.trigger(serverplayer, itemstack);
                    this.gameEvent(GameEvent.ITEM_INTERACT_FINISH);
                }

                this.setHealth(1.0F);
                deathprotection.applyEffects(itemstack, this);
                this.level().broadcastEntityEvent(this, (byte)35);
            }

            return deathprotection != null;
        }
    }

    @Nullable
    public DamageSource getLastDamageSource() {
        if (this.level().getGameTime() - this.lastDamageStamp > 40L) {
            this.lastDamageSource = null;
        }

        return this.lastDamageSource;
    }

    protected void playHurtSound(DamageSource pSource) {
        this.makeSound(this.getHurtSound(pSource));
    }

    public void makeSound(@Nullable SoundEvent pSound) {
        if (pSound != null) {
            this.playSound(pSound, this.getSoundVolume(), this.getVoicePitch());
        }
    }

    private void breakItem(ItemStack pStack) {
        if (!pStack.isEmpty()) {
            Holder<SoundEvent> holder = pStack.get(DataComponents.BREAK_SOUND);
            if (holder != null && !this.isSilent()) {
                this.level()
                    .playLocalSound(
                        this.getX(),
                        this.getY(),
                        this.getZ(),
                        holder.value(),
                        this.getSoundSource(),
                        0.8F,
                        0.8F + this.level().random.nextFloat() * 0.4F,
                        false
                    );
            }

            this.spawnItemParticles(pStack, 5);
        }
    }

    public void die(DamageSource pDamageSource) {
        if (net.minecraftforge.event.ForgeEventFactory.onLivingDeath(this, pDamageSource)) return;
        if (!this.isRemoved() && !this.dead) {
            Entity entity = pDamageSource.getEntity();
            LivingEntity livingentity = this.getKillCredit();
            if (livingentity != null) {
                livingentity.awardKillScore(this, pDamageSource);
            }

            if (this.isSleeping()) {
                this.stopSleeping();
            }

            if (!this.level().isClientSide && this.hasCustomName()) {
                LOGGER.info("Named entity {} died: {}", this, this.getCombatTracker().getDeathMessage().getString());
            }

            this.dead = true;
            this.getCombatTracker().recheckStatus();
            if (this.level() instanceof ServerLevel serverlevel) {
                if (entity == null || entity.killedEntity(serverlevel, this)) {
                    this.gameEvent(GameEvent.ENTITY_DIE);
                    this.dropAllDeathLoot(serverlevel, pDamageSource);
                    this.createWitherRose(livingentity);
                }

                this.level().broadcastEntityEvent(this, (byte)3);
            }

            this.setPose(Pose.DYING);
        }
    }

    protected void createWitherRose(@Nullable LivingEntity pEntitySource) {
        if (this.level() instanceof ServerLevel serverlevel) {
            boolean flag = false;
            if (pEntitySource instanceof WitherBoss) {
                if (net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(serverlevel, pEntitySource)) {
                    BlockPos blockpos = this.blockPosition();
                    BlockState blockstate = Blocks.WITHER_ROSE.defaultBlockState();
                    if (this.level().isEmptyBlock(blockpos) && blockstate.canSurvive(this.level(), blockpos)) {
                        this.level().setBlock(blockpos, blockstate, 3);
                        flag = true;
                    }
                }

                if (!flag) {
                    ItemEntity itementity = new ItemEntity(this.level(), this.getX(), this.getY(), this.getZ(), new ItemStack(Items.WITHER_ROSE));
                    this.level().addFreshEntity(itementity);
                }
            }
        }
    }

    protected void dropAllDeathLoot(ServerLevel pLevel, DamageSource pDamageSource) {
        this.captureDrops(new java.util.ArrayList<>());
        boolean flag = this.lastHurtByPlayerMemoryTime > 0;
        if (this.shouldDropLoot() && pLevel.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) {
            this.dropFromLootTable(pLevel, pDamageSource, flag);
            this.dropCustomDeathLoot(pLevel, pDamageSource, flag);
        }

        this.dropEquipment(pLevel);
        this.dropExperience(pLevel, pDamageSource.getEntity());

        var drops = captureDrops(null);
        if (!net.minecraftforge.event.ForgeEventFactory.onLivingDrops(this, pDamageSource, drops, flag)) {
            drops.forEach(e -> level().addFreshEntity(e));
        }
    }

    protected void dropEquipment(ServerLevel pLevel) {
    }

    protected void dropExperience(ServerLevel pLevel, @Nullable Entity pEntity) {
        if (!this.wasExperienceConsumed() && (this.isAlwaysExperienceDropper() || this.lastHurtByPlayerMemoryTime > 0 && this.shouldDropExperience() && pLevel.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT))) {
            int reward = net.minecraftforge.event.ForgeEventFactory.getExperienceDrop(this, this.getLastHurtByPlayer(), this.getExperienceReward(pLevel, pEntity));
            ExperienceOrb.award(pLevel, this.position(), reward);
        }
    }

    protected void dropCustomDeathLoot(ServerLevel pLevel, DamageSource pDamageSource, boolean pRecentlyHit) {
    }

    public long getLootTableSeed() {
        return 0L;
    }

    protected float getKnockback(Entity pAttacker, DamageSource pDamageSource) {
        float f = (float)this.getAttributeValue(Attributes.ATTACK_KNOCKBACK);
        return this.level() instanceof ServerLevel serverlevel ? EnchantmentHelper.modifyKnockback(serverlevel, this.getWeaponItem(), pAttacker, pDamageSource, f) : f;
    }

    protected void dropFromLootTable(ServerLevel pLevel, DamageSource pDamageSource, boolean pPlayerKill) {
        Optional<ResourceKey<LootTable>> optional = this.getLootTable();
        if (!optional.isEmpty()) {
            LootTable loottable = pLevel.getServer().reloadableRegistries().getLootTable(optional.get());
            LootParams.Builder lootparams$builder = new LootParams.Builder(pLevel)
                .withParameter(LootContextParams.THIS_ENTITY, this)
                .withParameter(LootContextParams.ORIGIN, this.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, pDamageSource)
                .withOptionalParameter(LootContextParams.ATTACKING_ENTITY, pDamageSource.getEntity())
                .withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, pDamageSource.getDirectEntity());
            Player player = this.getLastHurtByPlayer();
            if (pPlayerKill && player != null) {
                lootparams$builder = lootparams$builder.withParameter(LootContextParams.LAST_DAMAGE_PLAYER, player).withLuck(player.getLuck());
            }

            LootParams lootparams = lootparams$builder.create(LootContextParamSets.ENTITY);
            loottable.getRandomItems(lootparams, this.getLootTableSeed(), p_358880_ -> this.spawnAtLocation(pLevel, p_358880_));
        }
    }

    public boolean dropFromGiftLootTable(ServerLevel pLevel, ResourceKey<LootTable> pLootTable, BiConsumer<ServerLevel, ItemStack> pDropConsumer) {
        return this.dropFromLootTable(
            pLevel,
            pLootTable,
            p_374930_ -> p_374930_.withParameter(LootContextParams.ORIGIN, this.position())
                .withParameter(LootContextParams.THIS_ENTITY, this)
                .create(LootContextParamSets.GIFT),
            pDropConsumer
        );
    }

    protected void dropFromShearingLootTable(ServerLevel pLevel, ResourceKey<LootTable> pLootTable, ItemStack pShears, BiConsumer<ServerLevel, ItemStack> pDropConsumer) {
        this.dropFromLootTable(
            pLevel,
            pLootTable,
            p_374933_ -> p_374933_.withParameter(LootContextParams.ORIGIN, this.position())
                .withParameter(LootContextParams.THIS_ENTITY, this)
                .withParameter(LootContextParams.TOOL, pShears)
                .create(LootContextParamSets.SHEARING),
            pDropConsumer
        );
    }

    protected boolean dropFromLootTable(
        ServerLevel pLevel,
        ResourceKey<LootTable> pLootTable,
        Function<LootParams.Builder, LootParams> pParamsBuilder,
        BiConsumer<ServerLevel, ItemStack> pDropConsumer
    ) {
        LootTable loottable = pLevel.getServer().reloadableRegistries().getLootTable(pLootTable);
        LootParams lootparams = pParamsBuilder.apply(new LootParams.Builder(pLevel));
        List<ItemStack> list = loottable.getRandomItems(lootparams);
        if (!list.isEmpty()) {
            list.forEach(p_358893_ -> pDropConsumer.accept(pLevel, p_358893_));
            return true;
        } else {
            return false;
        }
    }

    public void knockback(double pStrength, double pX, double pZ) {
        var event = net.minecraftforge.event.ForgeEventFactory.onLivingKnockBack(this, (float) pStrength, pX, pZ);
        if (event.isCanceled()) return;
        pStrength = event.getStrength();
        pX = event.getRatioX();
        pZ = event.getRatioZ();
        pStrength *= 1.0 - this.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
        if (!(pStrength <= 0.0)) {
            this.hasImpulse = true;
            Vec3 vec3 = this.getDeltaMovement();

            while (pX * pX + pZ * pZ < 1.0E-5F) {
                pX = (Math.random() - Math.random()) * 0.01;
                pZ = (Math.random() - Math.random()) * 0.01;
            }

            Vec3 vec31 = new Vec3(pX, 0.0, pZ).normalize().scale(pStrength);
            this.setDeltaMovement(
                vec3.x / 2.0 - vec31.x,
                this.onGround() ? Math.min(0.4, vec3.y / 2.0 + pStrength) : vec3.y,
                vec3.z / 2.0 - vec31.z
            );
        }
    }

    public void indicateDamage(double pXDistance, double pZDistance) {
    }

    @Nullable
    protected SoundEvent getHurtSound(DamageSource pDamageSource) {
        return SoundEvents.GENERIC_HURT;
    }

    @Nullable
    protected SoundEvent getDeathSound() {
        return SoundEvents.GENERIC_DEATH;
    }

    private SoundEvent getFallDamageSound(int pHeight) {
        return pHeight > 4 ? this.getFallSounds().big() : this.getFallSounds().small();
    }

    public void skipDropExperience() {
        this.skipDropExperience = true;
    }

    public boolean wasExperienceConsumed() {
        return this.skipDropExperience;
    }

    public float getHurtDir() {
        return 0.0F;
    }

    protected AABB getHitbox() {
        AABB aabb = this.getBoundingBox();
        Entity entity = this.getVehicle();
        if (entity != null) {
            Vec3 vec3 = entity.getPassengerRidingPosition(this);
            return aabb.setMinY(Math.max(vec3.y, aabb.minY));
        } else {
            return aabb;
        }
    }

    public Map<Enchantment, Set<EnchantmentLocationBasedEffect>> activeLocationDependentEnchantments(EquipmentSlot pSlot) {
        return this.activeLocationDependentEnchantments.computeIfAbsent(pSlot, p_358895_ -> new Reference2ObjectArrayMap<>());
    }

    public LivingEntity.Fallsounds getFallSounds() {
        return new LivingEntity.Fallsounds(SoundEvents.GENERIC_SMALL_FALL, SoundEvents.GENERIC_BIG_FALL);
    }

    public Optional<BlockPos> getLastClimbablePos() {
        return this.lastClimbablePos;
    }

    public boolean onClimbable() {
        if (this.isSpectator()) {
            return false;
        } else {
            BlockPos blockpos = this.blockPosition();
            BlockState blockstate = this.getInBlockState();
            var ladderPos = net.minecraftforge.common.ForgeHooks.isLivingOnLadder(blockstate, level(), blockpos, this);
            if (ladderPos.isPresent()) {
                this.lastClimbablePos = ladderPos;
                return true;
            } else if (ladderPos != null) {
                return false;
            }
            if (blockstate.is(BlockTags.CLIMBABLE)) {
                this.lastClimbablePos = Optional.of(blockpos);
                return true;
            } else if (blockstate.getBlock() instanceof TrapDoorBlock && this.trapdoorUsableAsLadder(blockpos, blockstate)) {
                this.lastClimbablePos = Optional.of(blockpos);
                return true;
            } else {
                return false;
            }
        }
    }

    private boolean trapdoorUsableAsLadder(BlockPos pPos, BlockState pState) {
        if (!pState.getValue(TrapDoorBlock.OPEN)) {
            return false;
        } else {
            BlockState blockstate = this.level().getBlockState(pPos.below());
            return blockstate.is(Blocks.LADDER) && blockstate.getValue(LadderBlock.FACING) == pState.getValue(TrapDoorBlock.FACING);
        }
    }

    @Override
    public boolean isAlive() {
        return !this.isRemoved() && this.getHealth() > 0.0F;
    }

    public boolean isLookingAtMe(LivingEntity pEntity, double pTolerance, boolean pScaleByDistance, boolean pVisual, double... pYValues) {
        Vec3 vec3 = pEntity.getViewVector(1.0F).normalize();

        for (double d0 : pYValues) {
            Vec3 vec31 = new Vec3(this.getX() - pEntity.getX(), d0 - pEntity.getEyeY(), this.getZ() - pEntity.getZ());
            double d1 = vec31.length();
            vec31 = vec31.normalize();
            double d2 = vec3.dot(vec31);
            if (d2 > 1.0 - pTolerance / (pScaleByDistance ? d1 : 1.0)
                && pEntity.hasLineOfSight(this, pVisual ? ClipContext.Block.VISUAL : ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, d0)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public int getMaxFallDistance() {
        return this.getComfortableFallDistance(0.0F);
    }

    protected final int getComfortableFallDistance(float pHealth) {
        return Mth.floor(pHealth + 3.0F);
    }

    @Override
    public boolean causeFallDamage(double p_393354_, float p_147187_, DamageSource p_147189_) {
        var event = net.minecraftforge.event.ForgeEventFactory.onLivingFall(this, p_393354_, p_147187_);
        if (event.isCanceled()) return false;
        p_393354_ = event.getDistance();
        p_147187_ = event.getDamageMultiplier();
        boolean flag = super.causeFallDamage(p_393354_, p_147187_, p_147189_);
        int i = this.calculateFallDamage(p_393354_, p_147187_);
        if (i > 0) {
            this.playSound(this.getFallDamageSound(i), 1.0F, 1.0F);
            this.playBlockFallSound();
            this.hurt(p_147189_, i);
            return true;
        } else {
            return flag;
        }
    }

    protected int calculateFallDamage(double pFallDistance, float pDamageMultiplier) {
        if (this.getType().is(EntityTypeTags.FALL_DAMAGE_IMMUNE)) {
            return 0;
        } else {
            double d0 = this.calculateFallPower(pFallDistance);
            return Mth.floor(d0 * pDamageMultiplier * this.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER));
        }
    }

    private double calculateFallPower(double pFallDistance) {
        return pFallDistance + 1.0E-6 - this.getAttributeValue(Attributes.SAFE_FALL_DISTANCE);
    }

    protected void playBlockFallSound() {
        if (!this.isSilent()) {
            int i = Mth.floor(this.getX());
            int j = Mth.floor(this.getY() - 0.2F);
            int k = Mth.floor(this.getZ());
            BlockPos pos = new BlockPos(i, j, k);
            BlockState blockstate = this.level().getBlockState(pos);
            if (!blockstate.isAir()) {
                SoundType soundtype = blockstate.getSoundType(level(), pos, this);
                this.playSound(soundtype.getFallSound(), soundtype.getVolume() * 0.5F, soundtype.getPitch() * 0.75F);
            }
        }
    }

    @Override
    public void animateHurt(float p_265265_) {
        this.hurtDuration = 10;
        this.hurtTime = this.hurtDuration;
    }

    public int getArmorValue() {
        return Mth.floor(this.getAttributeValue(Attributes.ARMOR));
    }

    protected void hurtArmor(DamageSource pDamageSource, float pDamageAmount) {
    }

    protected void hurtHelmet(DamageSource pDamageSource, float pDamageAmount) {
    }

    protected void doHurtEquipment(DamageSource pDamageSource, float pDamageAmount, EquipmentSlot... pSlots) {
        if (!(pDamageAmount <= 0.0F)) {
            int i = (int)Math.max(1.0F, pDamageAmount / 4.0F);

            for (EquipmentSlot equipmentslot : pSlots) {
                ItemStack itemstack = this.getItemBySlot(equipmentslot);
                Equippable equippable = itemstack.get(DataComponents.EQUIPPABLE);
                if (equippable != null && equippable.damageOnHurt() && itemstack.isDamageableItem() && itemstack.canBeHurtBy(pDamageSource)) {
                    itemstack.hurtAndBreak(i, this, equipmentslot);
                }
            }
        }
    }

    protected float getDamageAfterArmorAbsorb(DamageSource pDamageSource, float pDamageAmount) {
        if (!pDamageSource.is(DamageTypeTags.BYPASSES_ARMOR)) {
            this.hurtArmor(pDamageSource, pDamageAmount);
            pDamageAmount = CombatRules.getDamageAfterAbsorb(this, pDamageAmount, pDamageSource, this.getArmorValue(), (float)this.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
        }

        return pDamageAmount;
    }

    protected float getDamageAfterMagicAbsorb(DamageSource pDamageSource, float pDamageAmount) {
        if (pDamageSource.is(DamageTypeTags.BYPASSES_EFFECTS)) {
            return pDamageAmount;
        } else {
            if (this.hasEffect(MobEffects.RESISTANCE) && !pDamageSource.is(DamageTypeTags.BYPASSES_RESISTANCE)) {
                int i = (this.getEffect(MobEffects.RESISTANCE).getAmplifier() + 1) * 5;
                int j = 25 - i;
                float f = pDamageAmount * j;
                float f1 = pDamageAmount;
                pDamageAmount = Math.max(f / 25.0F, 0.0F);
                float f2 = f1 - pDamageAmount;
                if (f2 > 0.0F && f2 < 3.4028235E37F) {
                    if (this instanceof ServerPlayer) {
                        ((ServerPlayer)this).awardStat(Stats.CUSTOM.get(Stats.DAMAGE_RESISTED), Math.round(f2 * 10.0F));
                    } else if (pDamageSource.getEntity() instanceof ServerPlayer) {
                        ((ServerPlayer)pDamageSource.getEntity()).awardStat(Stats.CUSTOM.get(Stats.DAMAGE_DEALT_RESISTED), Math.round(f2 * 10.0F));
                    }
                }
            }

            if (pDamageAmount <= 0.0F) {
                return 0.0F;
            } else if (pDamageSource.is(DamageTypeTags.BYPASSES_ENCHANTMENTS)) {
                return pDamageAmount;
            } else {
                float f3;
                if (this.level() instanceof ServerLevel serverlevel) {
                    f3 = EnchantmentHelper.getDamageProtection(serverlevel, this, pDamageSource);
                } else {
                    f3 = 0.0F;
                }

                if (f3 > 0.0F) {
                    pDamageAmount = CombatRules.getDamageAfterMagicAbsorb(pDamageAmount, f3);
                }

                return pDamageAmount;
            }
        }
    }

    protected void actuallyHurt(ServerLevel pLevel, DamageSource pDamageSource, float pAmount) {
        if (!this.isInvulnerableTo(pLevel, pDamageSource)) {
            pAmount = net.minecraftforge.common.ForgeHooks.onLivingHurt(this, pDamageSource, pAmount);
            if (pAmount <= 0) return;
            pAmount = this.getDamageAfterArmorAbsorb(pDamageSource, pAmount);
            pAmount = this.getDamageAfterMagicAbsorb(pDamageSource, pAmount);
            float f1 = Math.max(pAmount - this.getAbsorptionAmount(), 0.0F);
            this.setAbsorptionAmount(this.getAbsorptionAmount() - (pAmount - f1));
            float f = pAmount - f1;
            if (f > 0.0F && f < 3.4028235E37F && pDamageSource.getEntity() instanceof ServerPlayer serverplayer) {
                serverplayer.awardStat(Stats.DAMAGE_DEALT_ABSORBED, Math.round(f * 10.0F));
            }

            f1 = net.minecraftforge.common.ForgeHooks.onLivingDamage(this, pDamageSource, f1);
            if (f1 != 0.0F) {
                this.getCombatTracker().recordDamage(pDamageSource, f1);
                this.setHealth(this.getHealth() - f1);
                this.setAbsorptionAmount(this.getAbsorptionAmount() - f1);
                this.gameEvent(GameEvent.ENTITY_DAMAGE);
            }
        }
    }

    public CombatTracker getCombatTracker() {
        return this.combatTracker;
    }

    @Nullable
    public LivingEntity getKillCredit() {
        if (this.lastHurtByPlayer != null) {
            return this.lastHurtByPlayer.getEntity(this.level(), Player.class);
        } else {
            return this.lastHurtByMob != null ? this.lastHurtByMob.getEntity(this.level(), LivingEntity.class) : null;
        }
    }

    public final float getMaxHealth() {
        return (float)this.getAttributeValue(Attributes.MAX_HEALTH);
    }

    public final float getMaxAbsorption() {
        return (float)this.getAttributeValue(Attributes.MAX_ABSORPTION);
    }

    public final int getArrowCount() {
        return this.entityData.get(DATA_ARROW_COUNT_ID);
    }

    public final void setArrowCount(int pCount) {
        this.entityData.set(DATA_ARROW_COUNT_ID, pCount);
    }

    public final int getStingerCount() {
        return this.entityData.get(DATA_STINGER_COUNT_ID);
    }

    public final void setStingerCount(int pStingerCount) {
        this.entityData.set(DATA_STINGER_COUNT_ID, pStingerCount);
    }

    private int getCurrentSwingDuration() {
        if (MobEffectUtil.hasDigSpeed(this)) {
            return 6 - (1 + MobEffectUtil.getDigSpeedAmplification(this));
        } else {
            return this.hasEffect(MobEffects.MINING_FATIGUE) ? 6 + (1 + this.getEffect(MobEffects.MINING_FATIGUE).getAmplifier()) * 2 : 6;
        }
    }

    public void swing(InteractionHand pHand) {
        this.swing(pHand, false);
    }

    public void swing(InteractionHand pHand, boolean pUpdateSelf) {
        ItemStack stack = this.getItemInHand(pHand);
        if (!stack.isEmpty() && stack.onEntitySwing(this)) return;
        if (!this.swinging || this.swingTime >= this.getCurrentSwingDuration() / 2 || this.swingTime < 0) {
            this.swingTime = -1;
            this.swinging = true;
            this.swingingArm = pHand;
            if (this.level() instanceof ServerLevel) {
                ClientboundAnimatePacket clientboundanimatepacket = new ClientboundAnimatePacket(this, pHand == InteractionHand.MAIN_HAND ? 0 : 3);
                ServerChunkCache serverchunkcache = ((ServerLevel)this.level()).getChunkSource();
                if (pUpdateSelf) {
                    serverchunkcache.broadcastAndSend(this, clientboundanimatepacket);
                } else {
                    serverchunkcache.broadcast(this, clientboundanimatepacket);
                }
            }
        }
    }

    @Override
    public void handleDamageEvent(DamageSource p_270229_) {
        this.walkAnimation.setSpeed(1.5F);
        this.invulnerableTime = 20;
        this.hurtDuration = 10;
        this.hurtTime = this.hurtDuration;
        SoundEvent soundevent = this.getHurtSound(p_270229_);
        if (soundevent != null) {
            this.playSound(soundevent, this.getSoundVolume(), (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
        }

        this.lastDamageSource = p_270229_;
        this.lastDamageStamp = this.level().getGameTime();
    }

    @Override
    public void handleEntityEvent(byte pId) {
        switch (pId) {
            case 3:
                SoundEvent soundevent = this.getDeathSound();
                if (soundevent != null) {
                    this.playSound(soundevent, this.getSoundVolume(), (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
                }

                if (!(this instanceof Player)) {
                    this.setHealth(0.0F);
                    this.die(this.damageSources().generic());
                }
                break;
            case 46:
                int i = 128;

                for (int j = 0; j < 128; j++) {
                    double d0 = j / 127.0;
                    float f = (this.random.nextFloat() - 0.5F) * 0.2F;
                    float f1 = (this.random.nextFloat() - 0.5F) * 0.2F;
                    float f2 = (this.random.nextFloat() - 0.5F) * 0.2F;
                    double d1 = Mth.lerp(d0, this.xo, this.getX()) + (this.random.nextDouble() - 0.5) * this.getBbWidth() * 2.0;
                    double d2 = Mth.lerp(d0, this.yo, this.getY()) + this.random.nextDouble() * this.getBbHeight();
                    double d3 = Mth.lerp(d0, this.zo, this.getZ()) + (this.random.nextDouble() - 0.5) * this.getBbWidth() * 2.0;
                    this.level().addParticle(ParticleTypes.PORTAL, d1, d2, d3, f, f1, f2);
                }
                break;
            case 47:
                this.breakItem(this.getItemBySlot(EquipmentSlot.MAINHAND));
                break;
            case 48:
                this.breakItem(this.getItemBySlot(EquipmentSlot.OFFHAND));
                break;
            case 49:
                this.breakItem(this.getItemBySlot(EquipmentSlot.HEAD));
                break;
            case 50:
                this.breakItem(this.getItemBySlot(EquipmentSlot.CHEST));
                break;
            case 51:
                this.breakItem(this.getItemBySlot(EquipmentSlot.LEGS));
                break;
            case 52:
                this.breakItem(this.getItemBySlot(EquipmentSlot.FEET));
                break;
            case 54:
                HoneyBlock.showJumpParticles(this);
                break;
            case 55:
                this.swapHandItems();
                break;
            case 60:
                this.makePoofParticles();
                break;
            case 65:
                this.breakItem(this.getItemBySlot(EquipmentSlot.BODY));
                break;
            case 67:
                this.makeDrownParticles();
                break;
            case 68:
                this.breakItem(this.getItemBySlot(EquipmentSlot.SADDLE));
                break;
            default:
                super.handleEntityEvent(pId);
        }
    }

    public void makePoofParticles() {
        for (int i = 0; i < 20; i++) {
            double d0 = this.random.nextGaussian() * 0.02;
            double d1 = this.random.nextGaussian() * 0.02;
            double d2 = this.random.nextGaussian() * 0.02;
            double d3 = 10.0;
            this.level()
                .addParticle(ParticleTypes.POOF, this.getRandomX(1.0) - d0 * 10.0, this.getRandomY() - d1 * 10.0, this.getRandomZ(1.0) - d2 * 10.0, d0, d1, d2);
        }
    }

    private void makeDrownParticles() {
        Vec3 vec3 = this.getDeltaMovement();

        for (int i = 0; i < 8; i++) {
            double d0 = this.random.triangle(0.0, 1.0);
            double d1 = this.random.triangle(0.0, 1.0);
            double d2 = this.random.triangle(0.0, 1.0);
            this.level()
                .addParticle(ParticleTypes.BUBBLE, this.getX() + d0, this.getY() + d1, this.getZ() + d2, vec3.x, vec3.y, vec3.z);
        }
    }

    private void swapHandItems() {
        var event = net.minecraftforge.event.ForgeEventFactory.onLivingSwapHandItems(this);
        if (event.isCanceled()) return;
        this.setItemSlot(EquipmentSlot.OFFHAND, event.getItemSwappedToOffHand());
        this.setItemSlot(EquipmentSlot.MAINHAND, event.getItemSwappedToMainHand());
    }

    @Override
    protected void onBelowWorld() {
        this.hurt(this.damageSources().fellOutOfWorld(), 4.0F);
    }

    protected void updateSwingTime() {
        int i = this.getCurrentSwingDuration();
        if (this.swinging) {
            this.swingTime++;
            if (this.swingTime >= i) {
                this.swingTime = 0;
                this.swinging = false;
            }
        } else {
            this.swingTime = 0;
        }

        this.attackAnim = (float)this.swingTime / i;
    }

    @Nullable
    public AttributeInstance getAttribute(Holder<Attribute> pAttribute) {
        return this.getAttributes().getInstance(pAttribute);
    }

    public double getAttributeValue(Holder<Attribute> pAttribute) {
        return this.getAttributes().getValue(pAttribute);
    }

    public double getAttributeBaseValue(Holder<Attribute> pAttribute) {
        return this.getAttributes().getBaseValue(pAttribute);
    }

    public AttributeMap getAttributes() {
        return this.attributes;
    }

    public ItemStack getMainHandItem() {
        return this.getItemBySlot(EquipmentSlot.MAINHAND);
    }

    public ItemStack getOffhandItem() {
        return this.getItemBySlot(EquipmentSlot.OFFHAND);
    }

    public ItemStack getItemHeldByArm(HumanoidArm pArm) {
        return this.getMainArm() == pArm ? this.getMainHandItem() : this.getOffhandItem();
    }

    @Nonnull
    @Override
    public ItemStack getWeaponItem() {
        return this.getMainHandItem();
    }

    public boolean isHolding(Item pItem) {
        return this.isHolding(p_147200_ -> p_147200_.is(pItem));
    }

    public boolean isHolding(Predicate<ItemStack> pPredicate) {
        return pPredicate.test(this.getMainHandItem()) || pPredicate.test(this.getOffhandItem());
    }

    public ItemStack getItemInHand(InteractionHand pHand) {
        if (pHand == InteractionHand.MAIN_HAND) {
            return this.getItemBySlot(EquipmentSlot.MAINHAND);
        } else if (pHand == InteractionHand.OFF_HAND) {
            return this.getItemBySlot(EquipmentSlot.OFFHAND);
        } else {
            throw new IllegalArgumentException("Invalid hand " + pHand);
        }
    }

    public void setItemInHand(InteractionHand pHand, ItemStack pStack) {
        if (pHand == InteractionHand.MAIN_HAND) {
            this.setItemSlot(EquipmentSlot.MAINHAND, pStack);
        } else {
            if (pHand != InteractionHand.OFF_HAND) {
                throw new IllegalArgumentException("Invalid hand " + pHand);
            }

            this.setItemSlot(EquipmentSlot.OFFHAND, pStack);
        }
    }

    public boolean hasItemInSlot(EquipmentSlot pSlot) {
        return !this.getItemBySlot(pSlot).isEmpty();
    }

    public boolean canUseSlot(EquipmentSlot pSlot) {
        return true;
    }

    public ItemStack getItemBySlot(EquipmentSlot pSlot) {
        return this.equipment.get(pSlot);
    }

    public void setItemSlot(EquipmentSlot pSlot, ItemStack pStack) {
        this.onEquipItem(pSlot, this.equipment.set(pSlot, pStack), pStack);
    }

    public float getArmorCoverPercentage() {
        int i = 0;
        int j = 0;

        for (EquipmentSlot equipmentslot : EquipmentSlotGroup.ARMOR) {
            if (equipmentslot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
                ItemStack itemstack = this.getItemBySlot(equipmentslot);
                if (!itemstack.isEmpty()) {
                    j++;
                }

                i++;
            }
        }

        return i > 0 ? (float)j / i : 0.0F;
    }

    @Override
    public void setSprinting(boolean pSprinting) {
        super.setSprinting(pSprinting);
        AttributeInstance attributeinstance = this.getAttribute(Attributes.MOVEMENT_SPEED);
        attributeinstance.removeModifier(SPEED_MODIFIER_SPRINTING.id());
        if (pSprinting) {
            attributeinstance.addTransientModifier(SPEED_MODIFIER_SPRINTING);
        }
    }

    protected float getSoundVolume() {
        return 1.0F;
    }

    public float getVoicePitch() {
        return this.isBaby()
            ? (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.5F
            : (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F;
    }

    protected boolean isImmobile() {
        return this.isDeadOrDying();
    }

    @Override
    public void push(Entity pEntity) {
        if (!this.isSleeping()) {
            super.push(pEntity);
        }
    }

    private void dismountVehicle(Entity pVehicle) {
        Vec3 vec3;
        if (this.isRemoved()) {
            vec3 = this.position();
        } else if (!pVehicle.isRemoved() && !this.level().getBlockState(pVehicle.blockPosition()).is(BlockTags.PORTALS)) {
            vec3 = pVehicle.getDismountLocationForPassenger(this);
        } else {
            double d0 = Math.max(this.getY(), pVehicle.getY());
            vec3 = new Vec3(this.getX(), d0, this.getZ());
            boolean flag = this.getBbWidth() <= 4.0F && this.getBbHeight() <= 4.0F;
            if (flag) {
                double d1 = this.getBbHeight() / 2.0;
                Vec3 vec31 = vec3.add(0.0, d1, 0.0);
                VoxelShape voxelshape = Shapes.create(AABB.ofSize(vec31, this.getBbWidth(), this.getBbHeight(), this.getBbWidth()));
                vec3 = this.level()
                    .findFreePosition(this, voxelshape, vec31, this.getBbWidth(), this.getBbHeight(), this.getBbWidth())
                    .map(p_358887_ -> p_358887_.add(0.0, -d1, 0.0))
                    .orElse(vec3);
            }
        }

        this.dismountTo(vec3.x, vec3.y, vec3.z);
    }

    @Override
    public boolean shouldShowName() {
        return this.isCustomNameVisible();
    }

    protected float getJumpPower() {
        return this.getJumpPower(1.0F);
    }

    protected float getJumpPower(float pMultiplier) {
        return (float)this.getAttributeValue(Attributes.JUMP_STRENGTH) * pMultiplier * this.getBlockJumpFactor() + this.getJumpBoostPower();
    }

    public float getJumpBoostPower() {
        return this.hasEffect(MobEffects.JUMP_BOOST) ? 0.1F * (this.getEffect(MobEffects.JUMP_BOOST).getAmplifier() + 1.0F) : 0.0F;
    }

    @VisibleForTesting
    public void jumpFromGround() {
        float f = this.getJumpPower();
        if (!(f <= 1.0E-5F)) {
            Vec3 vec3 = this.getDeltaMovement();
            this.setDeltaMovement(vec3.x, Math.max((double)f, vec3.y), vec3.z);
            if (this.isSprinting()) {
                float f1 = this.getYRot() * (float) (Math.PI / 180.0);
                this.addDeltaMovement(new Vec3(-Mth.sin(f1) * 0.2, 0.0, Mth.cos(f1) * 0.2));
            }

            this.hasImpulse = true;
            net.minecraftforge.common.ForgeHooks.onLivingJump(this);
        }
    }

    @Deprecated // FORGE: use sinkInFluid instead
    protected void goDownInWater() {
        this.sinkInFluid(net.minecraftforge.common.ForgeMod.WATER_TYPE.get());
    }

    @Deprecated // FORGE: use jumpInFluid instead
    protected void jumpInLiquid(TagKey<Fluid> pFluidTag) {
        this.setDeltaMovement(this.getDeltaMovement().add(0.0, 0.04F * this.getAttributeValue(net.minecraftforge.common.ForgeMod.SWIM_SPEED.getHolder().get()), 0.0));
    }

    protected float getWaterSlowDown() {
        return 0.8F;
    }

    public boolean canStandOnFluid(FluidState pFluidState) {
        return false;
    }

    @Override
    protected double getDefaultGravity() {
        return this.getAttributeValue(Attributes.GRAVITY);
    }

    protected double getEffectiveGravity() {
        boolean flag = this.getDeltaMovement().y <= 0.0;
        return flag && this.hasEffect(MobEffects.SLOW_FALLING) ? Math.min(this.getGravity(), 0.01) : this.getGravity();
    }

    public void travel(Vec3 pTravelVector) {
        FluidState fluidstate = this.level().getFluidState(this.blockPosition());
        if ((this.isInWater() || this.isInLava() || this.isInFluidType(fluidstate)) && this.isAffectedByFluids() && !this.canStandOnFluid(fluidstate)) {
            this.travelInFluid(pTravelVector, fluidstate);
        } else if (this.isFallFlying()) {
            this.travelFallFlying(pTravelVector);
        } else {
            this.travelInAir(pTravelVector);
        }
    }

    private void travelInAir(Vec3 pTravelVector) {
        BlockPos blockpos = this.getBlockPosBelowThatAffectsMyMovement();
        float f = this.onGround() ? this.level().getBlockState(blockpos).getFriction(level(), blockpos, this) : 1.0F;
        float f1 = f * 0.91F;
        Vec3 vec3 = this.handleRelativeFrictionAndCalculateMovement(pTravelVector, f);
        double d0 = vec3.y;
        MobEffectInstance mobeffectinstance = this.getEffect(MobEffects.LEVITATION);
        if (mobeffectinstance != null) {
            d0 += (0.05 * (mobeffectinstance.getAmplifier() + 1) - vec3.y) * 0.2;
        } else if (!this.level().isClientSide || this.level().hasChunkAt(blockpos)) {
            d0 -= this.getEffectiveGravity();
        } else if (this.getY() > this.level().getMinY()) {
            d0 = -0.1;
        } else {
            d0 = 0.0;
        }

        if (this.shouldDiscardFriction()) {
            this.setDeltaMovement(vec3.x, d0, vec3.z);
        } else {
            float f2 = this instanceof FlyingAnimal ? f1 : 0.98F;
            this.setDeltaMovement(vec3.x * f1, d0 * f2, vec3.z * f1);
        }
    }

    @Deprecated // FORGE: Use the version that takes a FluidState
    private void travelInFluid(Vec3 pTravelVector) {
        this.travelInFluid(pTravelVector, net.minecraft.world.level.material.Fluids.WATER.defaultFluidState());
    }

    private void travelInFluid(Vec3 pTravelVector, FluidState fluidstate) {
        boolean flag = this.getDeltaMovement().y <= 0.0;
        double d0 = this.getY();
        double d1 = this.getEffectiveGravity();
        if (this.isInFluidType(fluidstate) && this.moveInFluid(fluidstate, pTravelVector, d0)) {
            // Modded fluid handled it
        } else
        if (this.isInWater()) {
            float f = this.isSprinting() ? 0.9F : this.getWaterSlowDown();
            float f1 = 0.02F;
            float f2 = (float)this.getAttributeValue(Attributes.WATER_MOVEMENT_EFFICIENCY);
            if (!this.onGround()) {
                f2 *= 0.5F;
            }

            if (f2 > 0.0F) {
                f += (0.54600006F - f) * f2;
                f1 += (this.getSpeed() - f1) * f2;
            }

            if (this.hasEffect(MobEffects.DOLPHINS_GRACE)) {
                f = 0.96F;
            }

                f1 *= this.getAttributeValue(net.minecraftforge.common.ForgeMod.SWIM_SPEED.getHolder().get());
            this.moveRelative(f1, pTravelVector);
            this.move(MoverType.SELF, this.getDeltaMovement());
            Vec3 vec3 = this.getDeltaMovement();
            if (this.horizontalCollision && this.onClimbable()) {
                vec3 = new Vec3(vec3.x, 0.2, vec3.z);
            }

            vec3 = vec3.multiply(f, 0.8F, f);
            this.setDeltaMovement(this.getFluidFallingAdjustedMovement(d1, flag, vec3));
        } else {
            this.moveRelative(0.02F, pTravelVector);
            this.move(MoverType.SELF, this.getDeltaMovement());
            if (this.getFluidHeight(FluidTags.LAVA) <= this.getFluidJumpThreshold()) {
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.5, 0.8F, 0.5));
                Vec3 vec31 = this.getFluidFallingAdjustedMovement(d1, flag, this.getDeltaMovement());
                this.setDeltaMovement(vec31);
            } else {
                this.setDeltaMovement(this.getDeltaMovement().scale(0.5));
            }

            if (d1 != 0.0) {
                this.setDeltaMovement(this.getDeltaMovement().add(0.0, -d1 / 4.0, 0.0));
            }
        }

        Vec3 vec32 = this.getDeltaMovement();
        if (this.horizontalCollision && this.isFree(vec32.x, vec32.y + 0.6F - this.getY() + d0, vec32.z)) {
            this.setDeltaMovement(vec32.x, 0.3F, vec32.z);
        }
    }

    private void travelFallFlying(Vec3 pTravelVector) {
        if (this.onClimbable()) {
            this.travelInAir(pTravelVector);
            this.stopFallFlying();
        } else {
            Vec3 vec3 = this.getDeltaMovement();
            double d0 = vec3.horizontalDistance();
            this.setDeltaMovement(this.updateFallFlyingMovement(vec3));
            this.move(MoverType.SELF, this.getDeltaMovement());
            if (!this.level().isClientSide) {
                double d1 = this.getDeltaMovement().horizontalDistance();
                this.handleFallFlyingCollisions(d0, d1);
            }
        }
    }

    public void stopFallFlying() {
        this.setSharedFlag(7, true);
        this.setSharedFlag(7, false);
    }

    private Vec3 updateFallFlyingMovement(Vec3 pDeltaMovement) {
        Vec3 vec3 = this.getLookAngle();
        float f = this.getXRot() * (float) (Math.PI / 180.0);
        double d0 = Math.sqrt(vec3.x * vec3.x + vec3.z * vec3.z);
        double d1 = pDeltaMovement.horizontalDistance();
        double d2 = this.getEffectiveGravity();
        double d3 = Mth.square(Math.cos(f));
        pDeltaMovement = pDeltaMovement.add(0.0, d2 * (-1.0 + d3 * 0.75), 0.0);
        if (pDeltaMovement.y < 0.0 && d0 > 0.0) {
            double d4 = pDeltaMovement.y * -0.1 * d3;
            pDeltaMovement = pDeltaMovement.add(vec3.x * d4 / d0, d4, vec3.z * d4 / d0);
        }

        if (f < 0.0F && d0 > 0.0) {
            double d5 = d1 * -Mth.sin(f) * 0.04;
            pDeltaMovement = pDeltaMovement.add(-vec3.x * d5 / d0, d5 * 3.2, -vec3.z * d5 / d0);
        }

        if (d0 > 0.0) {
            pDeltaMovement = pDeltaMovement.add((vec3.x / d0 * d1 - pDeltaMovement.x) * 0.1, 0.0, (vec3.z / d0 * d1 - pDeltaMovement.z) * 0.1);
        }

        return pDeltaMovement.multiply(0.99F, 0.98F, 0.99F);
    }

    private void handleFallFlyingCollisions(double pOldSpeed, double pNewSpeed) {
        if (this.horizontalCollision) {
            double d0 = pOldSpeed - pNewSpeed;
            float f = (float)(d0 * 10.0 - 3.0);
            if (f > 0.0F) {
                this.playSound(this.getFallDamageSound((int)f), 1.0F, 1.0F);
                this.hurt(this.damageSources().flyIntoWall(), f);
            }
        }
    }

    private void travelRidden(Player pPlayer, Vec3 pTravelVector) {
        Vec3 vec3 = this.getRiddenInput(pPlayer, pTravelVector);
        this.tickRidden(pPlayer, vec3);
        if (this.canSimulateMovement()) {
            this.setSpeed(this.getRiddenSpeed(pPlayer));
            this.travel(vec3);
        } else {
            this.setDeltaMovement(Vec3.ZERO);
        }
    }

    protected void tickRidden(Player pPlayer, Vec3 pTravelVector) {
    }

    protected Vec3 getRiddenInput(Player pPlayer, Vec3 pTravelVector) {
        return pTravelVector;
    }

    protected float getRiddenSpeed(Player pPlayer) {
        return this.getSpeed();
    }

    public void calculateEntityAnimation(boolean pIncludeHeight) {
        float f = (float)Mth.length(this.getX() - this.xo, pIncludeHeight ? this.getY() - this.yo : 0.0, this.getZ() - this.zo);
        if (!this.isPassenger() && this.isAlive()) {
            this.updateWalkAnimation(f);
        } else {
            this.walkAnimation.stop();
        }
    }

    protected void updateWalkAnimation(float pPartialTick) {
        float f = Math.min(pPartialTick * 4.0F, 1.0F);
        this.walkAnimation.update(f, 0.4F, this.isBaby() ? 3.0F : 1.0F);
    }

    private Vec3 handleRelativeFrictionAndCalculateMovement(Vec3 pDeltaMovement, float pFriction) {
        this.moveRelative(this.getFrictionInfluencedSpeed(pFriction), pDeltaMovement);
        this.setDeltaMovement(this.handleOnClimbable(this.getDeltaMovement()));
        this.move(MoverType.SELF, this.getDeltaMovement());
        Vec3 vec3 = this.getDeltaMovement();
        if ((this.horizontalCollision || this.jumping) && (this.onClimbable() || this.wasInPowderSnow && PowderSnowBlock.canEntityWalkOnPowderSnow(this))) {
            vec3 = new Vec3(vec3.x, 0.2, vec3.z);
        }

        return vec3;
    }

    public Vec3 getFluidFallingAdjustedMovement(double pGravity, boolean pIsFalling, Vec3 pDeltaMovement) {
        if (pGravity != 0.0 && !this.isSprinting()) {
            double d0;
            if (pIsFalling && Math.abs(pDeltaMovement.y - 0.005) >= 0.003 && Math.abs(pDeltaMovement.y - pGravity / 16.0) < 0.003) {
                d0 = -0.003;
            } else {
                d0 = pDeltaMovement.y - pGravity / 16.0;
            }

            return new Vec3(pDeltaMovement.x, d0, pDeltaMovement.z);
        } else {
            return pDeltaMovement;
        }
    }

    private Vec3 handleOnClimbable(Vec3 pDeltaMovement) {
        if (this.onClimbable()) {
            this.resetFallDistance();
            float f = 0.15F;
            double d0 = Mth.clamp(pDeltaMovement.x, -0.15F, 0.15F);
            double d1 = Mth.clamp(pDeltaMovement.z, -0.15F, 0.15F);
            double d2 = Math.max(pDeltaMovement.y, -0.15F);
            if (d2 < 0.0 && !this.getInBlockState().isScaffolding(this) && this.isSuppressingSlidingDownLadder() && this instanceof Player) {
                d2 = 0.0;
            }

            pDeltaMovement = new Vec3(d0, d2, d1);
        }

        return pDeltaMovement;
    }

    private float getFrictionInfluencedSpeed(float pFriction) {
        return this.onGround() ? this.getSpeed() * (0.21600002F / (pFriction * pFriction * pFriction)) : this.getFlyingSpeed();
    }

    protected float getFlyingSpeed() {
        return this.getControllingPassenger() instanceof Player ? this.getSpeed() * 0.1F : 0.02F;
    }

    public float getSpeed() {
        return this.speed;
    }

    public void setSpeed(float pSpeed) {
        this.speed = pSpeed;
    }

    public boolean doHurtTarget(ServerLevel pLevel, Entity pSource) {
        this.setLastHurtMob(pSource);
        return false;
    }

    @Override
    public void tick() {
        if (net.minecraftforge.event.ForgeEventFactory.onLivingTick(this)) return;
        super.tick();
        this.updatingUsingItem();
        this.updateSwimAmount();
        if (!this.level().isClientSide) {
            int i = this.getArrowCount();
            if (i > 0) {
                if (this.removeArrowTime <= 0) {
                    this.removeArrowTime = 20 * (30 - i);
                }

                this.removeArrowTime--;
                if (this.removeArrowTime <= 0) {
                    this.setArrowCount(i - 1);
                }
            }

            int j = this.getStingerCount();
            if (j > 0) {
                if (this.removeStingerTime <= 0) {
                    this.removeStingerTime = 20 * (30 - j);
                }

                this.removeStingerTime--;
                if (this.removeStingerTime <= 0) {
                    this.setStingerCount(j - 1);
                }
            }

            this.detectEquipmentUpdates();
            if (this.tickCount % 20 == 0) {
                this.getCombatTracker().recheckStatus();
            }

            if (this.isSleeping() && !this.checkBedExists()) {
                this.stopSleeping();
            }
        }

        if (!this.isRemoved()) {
            this.aiStep();
        }

        double d1 = this.getX() - this.xo;
        double d0 = this.getZ() - this.zo;
        float f = (float)(d1 * d1 + d0 * d0);
        float f1 = this.yBodyRot;
        if (f > 0.0025000002F) {
            float f2 = (float)Mth.atan2(d0, d1) * (180.0F / (float)Math.PI) - 90.0F;
            float f3 = Mth.abs(Mth.wrapDegrees(this.getYRot()) - f2);
            if (95.0F < f3 && f3 < 265.0F) {
                f1 = f2 - 180.0F;
            } else {
                f1 = f2;
            }
        }

        if (this.attackAnim > 0.0F) {
            f1 = this.getYRot();
        }

        ProfilerFiller profilerfiller = Profiler.get();
        profilerfiller.push("headTurn");
        this.tickHeadTurn(f1);
        profilerfiller.pop();
        profilerfiller.push("rangeChecks");

        while (this.getYRot() - this.yRotO < -180.0F) {
            this.yRotO -= 360.0F;
        }

        while (this.getYRot() - this.yRotO >= 180.0F) {
            this.yRotO += 360.0F;
        }

        while (this.yBodyRot - this.yBodyRotO < -180.0F) {
            this.yBodyRotO -= 360.0F;
        }

        while (this.yBodyRot - this.yBodyRotO >= 180.0F) {
            this.yBodyRotO += 360.0F;
        }

        while (this.getXRot() - this.xRotO < -180.0F) {
            this.xRotO -= 360.0F;
        }

        while (this.getXRot() - this.xRotO >= 180.0F) {
            this.xRotO += 360.0F;
        }

        while (this.yHeadRot - this.yHeadRotO < -180.0F) {
            this.yHeadRotO -= 360.0F;
        }

        while (this.yHeadRot - this.yHeadRotO >= 180.0F) {
            this.yHeadRotO += 360.0F;
        }

        profilerfiller.pop();
        if (this.isFallFlying()) {
            this.fallFlyTicks++;
        } else {
            this.fallFlyTicks = 0;
        }

        if (this.isSleeping()) {
            this.setXRot(0.0F);
        }

        this.refreshDirtyAttributes();
        this.elytraAnimationState.tick();
    }

    private void detectEquipmentUpdates() {
        Map<EquipmentSlot, ItemStack> map = this.collectEquipmentChanges();
        if (map != null) {
            this.handleHandSwap(map);
            if (!map.isEmpty()) {
                this.handleEquipmentChanges(map);
            }
        }
    }

    @Nullable
    private Map<EquipmentSlot, ItemStack> collectEquipmentChanges() {
        Map<EquipmentSlot, ItemStack> map = null;

        for (EquipmentSlot equipmentslot : EquipmentSlot.VALUES) {
            ItemStack itemstack = this.lastEquipmentItems.get(equipmentslot);
            ItemStack itemstack1 = this.getItemBySlot(equipmentslot);
            if (this.equipmentHasChanged(itemstack, itemstack1)) {
                net.minecraftforge.event.ForgeEventFactory.onLivingEquipmentChange(this, equipmentslot, itemstack, itemstack1);
                if (map == null) {
                    map = Maps.newEnumMap(EquipmentSlot.class);
                }

                map.put(equipmentslot, itemstack1);
                AttributeMap attributemap = this.getAttributes();
                if (!itemstack.isEmpty()) {
                    this.stopLocationBasedEffects(itemstack, equipmentslot, attributemap);
                }
            }
        }

        if (map != null) {
            for (Entry<EquipmentSlot, ItemStack> entry : map.entrySet()) {
                EquipmentSlot equipmentslot1 = entry.getKey();
                ItemStack itemstack2 = entry.getValue();
                if (!itemstack2.isEmpty() && !itemstack2.isBroken()) {
                    itemstack2.forEachModifier(equipmentslot1, (p_358896_, p_358897_) -> {
                        AttributeInstance attributeinstance = this.attributes.getInstance(p_358896_);
                        if (attributeinstance != null) {
                            attributeinstance.removeModifier(p_358897_.id());
                            attributeinstance.addTransientModifier(p_358897_);
                        }
                    });
                    if (this.level() instanceof ServerLevel serverlevel) {
                        EnchantmentHelper.runLocationChangedEffects(serverlevel, itemstack2, this, equipmentslot1);
                    }
                }
            }
        }

        return map;
    }

    public boolean equipmentHasChanged(ItemStack pOldItem, ItemStack pNewItem) {
        return !ItemStack.matches(pNewItem, pOldItem);
    }

    private void handleHandSwap(Map<EquipmentSlot, ItemStack> pHands) {
        ItemStack itemstack = pHands.get(EquipmentSlot.MAINHAND);
        ItemStack itemstack1 = pHands.get(EquipmentSlot.OFFHAND);
        if (itemstack != null
            && itemstack1 != null
            && ItemStack.matches(itemstack, this.lastEquipmentItems.get(EquipmentSlot.OFFHAND))
            && ItemStack.matches(itemstack1, this.lastEquipmentItems.get(EquipmentSlot.MAINHAND))) {
            ((ServerLevel)this.level()).getChunkSource().broadcast(this, new ClientboundEntityEventPacket(this, (byte)55));
            pHands.remove(EquipmentSlot.MAINHAND);
            pHands.remove(EquipmentSlot.OFFHAND);
            this.lastEquipmentItems.put(EquipmentSlot.MAINHAND, itemstack.copy());
            this.lastEquipmentItems.put(EquipmentSlot.OFFHAND, itemstack1.copy());
        }
    }

    private void handleEquipmentChanges(Map<EquipmentSlot, ItemStack> pEquipments) {
        List<Pair<EquipmentSlot, ItemStack>> list = Lists.newArrayListWithCapacity(pEquipments.size());
        pEquipments.forEach((p_390521_, p_390522_) -> {
            ItemStack itemstack = p_390522_.copy();
            list.add(Pair.of(p_390521_, itemstack));
            this.lastEquipmentItems.put(p_390521_, itemstack);
        });
        ((ServerLevel)this.level()).getChunkSource().broadcast(this, new ClientboundSetEquipmentPacket(this.getId(), list));
    }

    protected void tickHeadTurn(float pYBodyRot) {
        float f = Mth.wrapDegrees(pYBodyRot - this.yBodyRot);
        this.yBodyRot += f * 0.3F;
        float f1 = Mth.wrapDegrees(this.getYRot() - this.yBodyRot);
        float f2 = this.getMaxHeadRotationRelativeToBody();
        if (Math.abs(f1) > f2) {
            this.yBodyRot = this.yBodyRot + (f1 - Mth.sign(f1) * f2);
        }
    }

    protected float getMaxHeadRotationRelativeToBody() {
        return 50.0F;
    }

    public void aiStep() {
        if (this.noJumpDelay > 0) {
            this.noJumpDelay--;
        }

        if (this.isInterpolating()) {
            this.getInterpolation().interpolate();
        } else if (!this.canSimulateMovement()) {
            this.setDeltaMovement(this.getDeltaMovement().scale(0.98));
        }

        if (this.lerpHeadSteps > 0) {
            this.lerpHeadRotationStep(this.lerpHeadSteps, this.lerpYHeadRot);
            this.lerpHeadSteps--;
        }

        this.equipment.tick(this);
        Vec3 vec3 = this.getDeltaMovement();
        double d0 = vec3.x;
        double d1 = vec3.y;
        double d2 = vec3.z;
        if (this.getType().equals(EntityType.PLAYER)) {
            if (vec3.horizontalDistanceSqr() < 9.0E-6) {
                d0 = 0.0;
                d2 = 0.0;
            }
        } else {
            if (Math.abs(vec3.x) < 0.003) {
                d0 = 0.0;
            }

            if (Math.abs(vec3.z) < 0.003) {
                d2 = 0.0;
            }
        }

        if (Math.abs(vec3.y) < 0.003) {
            d1 = 0.0;
        }

        this.setDeltaMovement(d0, d1, d2);
        ProfilerFiller profilerfiller = Profiler.get();
        profilerfiller.push("ai");
        this.applyInput();
        if (this.isImmobile()) {
            this.jumping = false;
            this.xxa = 0.0F;
            this.zza = 0.0F;
        } else if (this.isEffectiveAi() && !this.level().isClientSide) {
            profilerfiller.push("newAi");
            this.serverAiStep();
            profilerfiller.pop();
        }

        profilerfiller.pop();
        profilerfiller.push("jump");
        if (this.jumping && this.isAffectedByFluids()) {
            double d3;
            var fluidType = this.getMaxHeightFluidType();
            if (!fluidType.isAir()) {
                d3 = this.getFluidTypeHeight(fluidType);
            } else
            if (this.isInLava()) {
                d3 = this.getFluidHeight(FluidTags.LAVA);
            } else {
                d3 = this.getFluidHeight(FluidTags.WATER);
            }

            boolean flag = this.isInWater() && d3 > 0.0;
            double d4 = this.getFluidJumpThreshold();
            if (!flag || this.onGround() && !(d3 > d4)) {
                if (!this.isInLava() || this.onGround() && !(d3 > d4)) {
                    if (fluidType.isAir() || this.onGround() && !(d3 > d4)) {
                    if ((this.onGround() || flag && d3 <= d4) && this.noJumpDelay == 0) {
                        this.jumpFromGround();
                        this.noJumpDelay = 10;
                    }
                    } else {
                        this.jumpInFluid(fluidType);
                    }
                } else {
                    var old = this.getDeltaMovement();
                    this.jumpInFluid(net.minecraftforge.common.ForgeMod.LAVA_TYPE.get());
                    if (this instanceof Player)
                        System.out.println(old + " " + this.getDeltaMovement());
                }
            } else {
                this.jumpInFluid(net.minecraftforge.common.ForgeMod.WATER_TYPE.get());
            }
        } else {
            this.noJumpDelay = 0;
        }

        profilerfiller.pop();
        profilerfiller.push("travel");
        if (this.isFallFlying()) {
            this.updateFallFlying();
        }

        AABB aabb = this.getBoundingBox();
        Vec3 vec31 = new Vec3(this.xxa, this.yya, this.zza);
        if (this.hasEffect(MobEffects.SLOW_FALLING) || this.hasEffect(MobEffects.LEVITATION)) {
            this.resetFallDistance();
        }

        if (this.getControllingPassenger() instanceof Player player && this.isAlive()) {
            this.travelRidden(player, vec31);
        } else if (this.canSimulateMovement()) {
            this.travel(vec31);
        }

        if (!this.level().isClientSide() || this.isLocalInstanceAuthoritative()) {
            this.applyEffectsFromBlocks();
        }

        if (this.level().isClientSide()) {
            this.calculateEntityAnimation(this instanceof FlyingAnimal);
        }

        profilerfiller.pop();
        if (this.level() instanceof ServerLevel serverlevel) {
            profilerfiller.push("freezing");
            if (!this.isInPowderSnow || !this.canFreeze()) {
                this.setTicksFrozen(Math.max(0, this.getTicksFrozen() - 2));
            }

            this.removeFrost();
            this.tryAddFrost();
            if (this.tickCount % 40 == 0 && this.isFullyFrozen() && this.canFreeze()) {
                this.hurtServer(serverlevel, this.damageSources().freeze(), 1.0F);
            }

            profilerfiller.pop();
        }

        profilerfiller.push("push");
        if (this.autoSpinAttackTicks > 0) {
            this.autoSpinAttackTicks--;
            this.checkAutoSpinAttack(aabb, this.getBoundingBox());
        }

        this.pushEntities();
        profilerfiller.pop();
        if (this.level() instanceof ServerLevel serverlevel1 && this.isSensitiveToWater() && this.isInWaterOrRain()) {
            this.hurtServer(serverlevel1, this.damageSources().drown(), 1.0F);
        }
    }

    protected void applyInput() {
        this.xxa *= 0.98F;
        this.zza *= 0.98F;
    }

    public boolean isSensitiveToWater() {
        return false;
    }

    protected void updateFallFlying() {
        this.checkSlowFallDistance();
        if (!this.level().isClientSide) {
            if (!this.canGlide()) {
                this.setSharedFlag(7, false);
                return;
            }

            int i = this.fallFlyTicks + 1;
            if (i % 10 == 0) {
                int j = i / 10;
                if (j % 2 == 0) {
                    List<EquipmentSlot> list = EquipmentSlot.VALUES.stream().filter(p_358890_ -> canGlideUsing(this.getItemBySlot(p_358890_), p_358890_)).toList();
                    EquipmentSlot equipmentslot = Util.getRandom(list, this.random);
                    this.getItemBySlot(equipmentslot).hurtAndBreak(1, this, equipmentslot);
                }

                this.gameEvent(GameEvent.ELYTRA_GLIDE);
            }
        }
    }

    protected boolean canGlide() {
        if (!this.onGround() && !this.isPassenger() && !this.hasEffect(MobEffects.LEVITATION)) {
            for (EquipmentSlot equipmentslot : EquipmentSlot.VALUES) {
                if (canGlideUsing(this.getItemBySlot(equipmentslot), equipmentslot)) {
                    return true;
                }
            }

            return false;
        } else {
            return false;
        }
    }

    protected void serverAiStep() {
    }

    protected void pushEntities() {
        List<Entity> list = this.level().getPushableEntities(this, this.getBoundingBox());
        if (!list.isEmpty()) {
            if (this.level() instanceof ServerLevel serverlevel) {
                int j = serverlevel.getGameRules().getInt(GameRules.RULE_MAX_ENTITY_CRAMMING);
                if (j > 0 && list.size() > j - 1 && this.random.nextInt(4) == 0) {
                    int i = 0;

                    for (Entity entity : list) {
                        if (!entity.isPassenger()) {
                            i++;
                        }
                    }

                    if (i > j - 1) {
                        this.hurtServer(serverlevel, this.damageSources().cramming(), 6.0F);
                    }
                }
            }

            for (Entity entity1 : list) {
                this.doPush(entity1);
            }
        }
    }

    protected void checkAutoSpinAttack(AABB pBoundingBoxBeforeSpin, AABB pBoundingBoxAfterSpin) {
        AABB aabb = pBoundingBoxBeforeSpin.minmax(pBoundingBoxAfterSpin);
        List<Entity> list = this.level().getEntities(this, aabb);
        if (!list.isEmpty()) {
            for (Entity entity : list) {
                if (entity instanceof LivingEntity) {
                    this.doAutoAttackOnTouch((LivingEntity)entity);
                    this.autoSpinAttackTicks = 0;
                    this.setDeltaMovement(this.getDeltaMovement().scale(-0.2));
                    break;
                }
            }
        } else if (this.horizontalCollision) {
            this.autoSpinAttackTicks = 0;
        }

        if (!this.level().isClientSide && this.autoSpinAttackTicks <= 0) {
            this.setLivingEntityFlag(4, false);
            this.autoSpinAttackDmg = 0.0F;
            this.autoSpinAttackItemStack = null;
        }
    }

    protected void doPush(Entity pEntity) {
        pEntity.push(this);
    }

    protected void doAutoAttackOnTouch(LivingEntity pTarget) {
    }

    public boolean isAutoSpinAttack() {
        return (this.entityData.get(DATA_LIVING_ENTITY_FLAGS) & 4) != 0;
    }

    @Override
    public void stopRiding() {
        Entity entity = this.getVehicle();
        super.stopRiding();
        if (entity != null && entity != this.getVehicle() && !this.level().isClientSide) {
            this.dismountVehicle(entity);
        }
    }

    @Override
    public void rideTick() {
        super.rideTick();
        this.resetFallDistance();
    }

    @Override
    public InterpolationHandler getInterpolation() {
        return this.interpolation;
    }

    @Override
    public void lerpHeadTo(float pYaw, int pPitch) {
        this.lerpYHeadRot = pYaw;
        this.lerpHeadSteps = pPitch;
    }

    public void setJumping(boolean pJumping) {
        this.jumping = pJumping;
    }

    public void onItemPickup(ItemEntity pItemEntity) {
        Entity entity = pItemEntity.getOwner();
        if (entity instanceof ServerPlayer) {
            CriteriaTriggers.THROWN_ITEM_PICKED_UP_BY_ENTITY.trigger((ServerPlayer)entity, pItemEntity.getItem(), this);
        }
    }

    public void take(Entity pEntity, int pAmount) {
        if (!pEntity.isRemoved()
            && !this.level().isClientSide
            && (pEntity instanceof ItemEntity || pEntity instanceof AbstractArrow || pEntity instanceof ExperienceOrb)) {
            ((ServerLevel)this.level()).getChunkSource().broadcast(pEntity, new ClientboundTakeItemEntityPacket(pEntity.getId(), this.getId(), pAmount));
        }
    }

    public boolean hasLineOfSight(Entity pEntity) {
        return this.hasLineOfSight(pEntity, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, pEntity.getEyeY());
    }

    public boolean hasLineOfSight(Entity pEntity, ClipContext.Block pBlock, ClipContext.Fluid pFluid, double pY) {
        if (pEntity.level() != this.level()) {
            return false;
        } else {
            Vec3 vec3 = new Vec3(this.getX(), this.getEyeY(), this.getZ());
            Vec3 vec31 = new Vec3(pEntity.getX(), pY, pEntity.getZ());
            return vec31.distanceTo(vec3) > 128.0
                ? false
                : this.level().clip(new ClipContext(vec3, vec31, pBlock, pFluid, this)).getType() == HitResult.Type.MISS;
        }
    }

    @Override
    public float getViewYRot(float pPartialTicks) {
        return pPartialTicks == 1.0F ? this.yHeadRot : Mth.rotLerp(pPartialTicks, this.yHeadRotO, this.yHeadRot);
    }

    public float getAttackAnim(float pPartialTick) {
        float f = this.attackAnim - this.oAttackAnim;
        if (f < 0.0F) {
            f++;
        }

        return this.oAttackAnim + f * pPartialTick;
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    public boolean isPushable() {
        return this.isAlive() && !this.isSpectator() && !this.onClimbable();
    }

    @Override
    public float getYHeadRot() {
        return this.yHeadRot;
    }

    @Override
    public void setYHeadRot(float pRotation) {
        this.yHeadRot = pRotation;
    }

    @Override
    public void setYBodyRot(float pOffset) {
        this.yBodyRot = pOffset;
    }

    @Override
    public Vec3 getRelativePortalPosition(Direction.Axis p_21085_, BlockUtil.FoundRectangle p_21086_) {
        return resetForwardDirectionOfRelativePortalPosition(super.getRelativePortalPosition(p_21085_, p_21086_));
    }

    public static Vec3 resetForwardDirectionOfRelativePortalPosition(Vec3 pRelativePortalPosition) {
        return new Vec3(pRelativePortalPosition.x, pRelativePortalPosition.y, 0.0);
    }

    public float getAbsorptionAmount() {
        return this.absorptionAmount;
    }

    public final void setAbsorptionAmount(float pAbsorptionAmount) {
        this.internalSetAbsorptionAmount(Mth.clamp(pAbsorptionAmount, 0.0F, this.getMaxAbsorption()));
    }

    protected void internalSetAbsorptionAmount(float pAbsorptionAmount) {
        this.absorptionAmount = pAbsorptionAmount;
    }

    public void onEnterCombat() {
    }

    public void onLeaveCombat() {
    }

    protected void updateEffectVisibility() {
        this.effectsDirty = true;
    }

    public abstract HumanoidArm getMainArm();

    public boolean isUsingItem() {
        return (this.entityData.get(DATA_LIVING_ENTITY_FLAGS) & 1) > 0;
    }

    public InteractionHand getUsedItemHand() {
        return (this.entityData.get(DATA_LIVING_ENTITY_FLAGS) & 2) > 0 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    private void updatingUsingItem() {
        if (this.isUsingItem()) {
            var current = this.getItemInHand(this.getUsedItemHand());
            if (net.minecraftforge.common.ForgeHooks.canContinueUsing(this.useItem, current)) {
                this.useItem = this.getItemInHand(this.getUsedItemHand());
            }
            if (this.useItem == current) {
                this.updateUsingItem(this.useItem);
            } else {
                this.stopUsingItem();
            }
        }
    }

    @Nullable
    private ItemEntity createItemStackToDrop(ItemStack pStack, boolean pRandomizeMotion, boolean pIncludeThrower) {
        if (pStack.isEmpty()) {
            return null;
        } else {
            double d0 = this.getEyeY() - 0.3F;
            ItemEntity itementity = new ItemEntity(this.level(), this.getX(), d0, this.getZ(), pStack);
            itementity.setPickUpDelay(40);
            if (pIncludeThrower) {
                itementity.setThrower(this);
            }

            if (pRandomizeMotion) {
                float f = this.random.nextFloat() * 0.5F;
                float f1 = this.random.nextFloat() * (float) (Math.PI * 2);
                itementity.setDeltaMovement(-Mth.sin(f1) * f, 0.2F, Mth.cos(f1) * f);
            } else {
                float f7 = 0.3F;
                float f8 = Mth.sin(this.getXRot() * (float) (Math.PI / 180.0));
                float f2 = Mth.cos(this.getXRot() * (float) (Math.PI / 180.0));
                float f3 = Mth.sin(this.getYRot() * (float) (Math.PI / 180.0));
                float f4 = Mth.cos(this.getYRot() * (float) (Math.PI / 180.0));
                float f5 = this.random.nextFloat() * (float) (Math.PI * 2);
                float f6 = 0.02F * this.random.nextFloat();
                itementity.setDeltaMovement(
                    -f3 * f2 * 0.3F + Math.cos(f5) * f6,
                    -f8 * 0.3F + 0.1F + (this.random.nextFloat() - this.random.nextFloat()) * 0.1F,
                    f4 * f2 * 0.3F + Math.sin(f5) * f6
                );
            }

            return itementity;
        }
    }

    protected void updateUsingItem(ItemStack pUsingItem) {
        if (!pUsingItem.isEmpty()) {
            this.useItemRemaining = net.minecraftforge.event.ForgeEventFactory.onItemUseTick(this, pUsingItem, this.getUseItemRemainingTicks());
        }
        if (this.getUseItemRemainingTicks() > 0)
        pUsingItem.onUseTick(this.level(), this, this.getUseItemRemainingTicks());
        if (--this.useItemRemaining <= 0 && !this.level().isClientSide && !pUsingItem.useOnRelease()) {
            this.completeUsingItem();
        }
    }

    private void updateSwimAmount() {
        this.swimAmountO = this.swimAmount;
        if (this.isVisuallySwimming()) {
            this.swimAmount = Math.min(1.0F, this.swimAmount + 0.09F);
        } else {
            this.swimAmount = Math.max(0.0F, this.swimAmount - 0.09F);
        }
    }

    protected void setLivingEntityFlag(int pKey, boolean pValue) {
        int i = this.entityData.get(DATA_LIVING_ENTITY_FLAGS);
        if (pValue) {
            i |= pKey;
        } else {
            i &= ~pKey;
        }

        this.entityData.set(DATA_LIVING_ENTITY_FLAGS, (byte)i);
    }

    public void startUsingItem(InteractionHand pHand) {
        ItemStack itemstack = this.getItemInHand(pHand);
        if (!itemstack.isEmpty() && !this.isUsingItem()) {
            int duration = net.minecraftforge.event.ForgeEventFactory.onItemUseStart(this, itemstack, itemstack.getUseDuration(this));
            if (duration < 0) return;
            this.useItem = itemstack;
            this.useItemRemaining = duration;
            if (!this.level().isClientSide) {
                this.setLivingEntityFlag(1, true);
                this.setLivingEntityFlag(2, pHand == InteractionHand.OFF_HAND);
                this.gameEvent(GameEvent.ITEM_INTERACT_START);
            }
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> pKey) {
        super.onSyncedDataUpdated(pKey);
        if (SLEEPING_POS_ID.equals(pKey)) {
            if (this.level().isClientSide) {
                this.getSleepingPos().ifPresent(this::setPosToBed);
            }
        } else if (DATA_LIVING_ENTITY_FLAGS.equals(pKey) && this.level().isClientSide) {
            if (this.isUsingItem() && this.useItem.isEmpty()) {
                this.useItem = this.getItemInHand(this.getUsedItemHand());
                if (!this.useItem.isEmpty()) {
                    this.useItemRemaining = this.useItem.getUseDuration(this);
                }
            } else if (!this.isUsingItem() && !this.useItem.isEmpty()) {
                this.useItem = ItemStack.EMPTY;
                this.useItemRemaining = 0;
            }
        }
    }

    @Override
    public void lookAt(EntityAnchorArgument.Anchor pAnchor, Vec3 pTarget) {
        super.lookAt(pAnchor, pTarget);
        this.yHeadRotO = this.yHeadRot;
        this.yBodyRot = this.yHeadRot;
        this.yBodyRotO = this.yBodyRot;
    }

    @Override
    public float getPreciseBodyRotation(float p_345405_) {
        return Mth.lerp(p_345405_, this.yBodyRotO, this.yBodyRot);
    }

    public void spawnItemParticles(ItemStack pStack, int pAmount) {
        for (int i = 0; i < pAmount; i++) {
            Vec3 vec3 = new Vec3((this.random.nextFloat() - 0.5) * 0.1, Math.random() * 0.1 + 0.1, 0.0);
            vec3 = vec3.xRot(-this.getXRot() * (float) (Math.PI / 180.0));
            vec3 = vec3.yRot(-this.getYRot() * (float) (Math.PI / 180.0));
            double d0 = -this.random.nextFloat() * 0.6 - 0.3;
            Vec3 vec31 = new Vec3((this.random.nextFloat() - 0.5) * 0.3, d0, 0.6);
            vec31 = vec31.xRot(-this.getXRot() * (float) (Math.PI / 180.0));
            vec31 = vec31.yRot(-this.getYRot() * (float) (Math.PI / 180.0));
            vec31 = vec31.add(this.getX(), this.getEyeY(), this.getZ());
            if (this.level() instanceof ServerLevel serverLevel) //Forge: Fix MC-2518 spawnParticle is nooped on server, need to use server specific variant
                serverLevel.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, pStack), vec31.x, vec31.y, vec31.z, 1, vec3.x, vec3.y + 0.05D, vec3.z, 0.0D);
            else
            this.level()
                .addParticle(
                    new ItemParticleOption(ParticleTypes.ITEM, pStack),
                    vec31.x,
                    vec31.y,
                    vec31.z,
                    vec3.x,
                    vec3.y + 0.05,
                    vec3.z
                );
        }
    }

    protected void completeUsingItem() {
        if (!this.level().isClientSide || this.isUsingItem()) {
            InteractionHand interactionhand = this.getUsedItemHand();
            if (!this.useItem.equals(this.getItemInHand(interactionhand))) {
                this.releaseUsingItem();
            } else {
                if (!this.useItem.isEmpty() && this.isUsingItem()) {
                    ItemStack copy = this.useItem.copy();
                    ItemStack itemstack = this.useItem.finishUsingItem(this.level(), this);
                    itemstack = net.minecraftforge.event.ForgeEventFactory.onItemUseFinish(this, copy, getUseItemRemainingTicks(), itemstack);
                    if (itemstack != this.useItem) {
                        this.setItemInHand(interactionhand, itemstack);
                    }

                    this.stopUsingItem();
                }
            }
        }
    }

    public void handleExtraItemsCreatedOnUse(ItemStack pStack) {
    }

    public ItemStack getUseItem() {
        return this.useItem;
    }

    public int getUseItemRemainingTicks() {
        return this.useItemRemaining;
    }

    public int getTicksUsingItem() {
        return this.isUsingItem() ? this.useItem.getUseDuration(this) - this.getUseItemRemainingTicks() : 0;
    }

    public void releaseUsingItem() {
        ItemStack itemstack = this.getItemInHand(this.getUsedItemHand());
        if (!this.useItem.isEmpty() && ItemStack.isSameItem(itemstack, this.useItem)) {
            this.useItem = itemstack;
            if (!net.minecraftforge.event.ForgeEventFactory.onUseItemStop(this, useItem, this.getUseItemRemainingTicks())) {
               ItemStack copy = this instanceof Player ? useItem.copy() : null;
            this.useItem.releaseUsing(this.level(), this, this.getUseItemRemainingTicks());
               if (copy != null && useItem.isEmpty()) {
                   net.minecraftforge.event.ForgeEventFactory.onPlayerDestroyItem((Player)this, copy, getUsedItemHand());
               }
            }
            if (this.useItem.useOnRelease()) {
                this.updatingUsingItem();
            }
        }

        this.stopUsingItem();
    }

    public void stopUsingItem() {
        if (this.isUsingItem() && !this.useItem.isEmpty()) this.useItem.onStopUsing(this, useItemRemaining);
        if (!this.level().isClientSide) {
            boolean flag = this.isUsingItem();
            this.setLivingEntityFlag(1, false);
            if (flag) {
                this.gameEvent(GameEvent.ITEM_INTERACT_FINISH);
            }
        }

        this.useItem = ItemStack.EMPTY;
        this.useItemRemaining = 0;
    }

    public boolean isBlocking() {
        return this.getItemBlockingWith() != null;
    }

    @Nullable
    public ItemStack getItemBlockingWith() {
        if (!this.isUsingItem()) {
            return null;
        } else {
            BlocksAttacks blocksattacks = this.useItem.get(DataComponents.BLOCKS_ATTACKS);
            if (blocksattacks != null) {
                int i = this.useItem.getItem().getUseDuration(this.useItem, this) - this.useItemRemaining;
                if (i >= blocksattacks.blockDelayTicks()) {
                    return this.useItem;
                }
            }

            return null;
        }
    }

    public boolean isSuppressingSlidingDownLadder() {
        return this.isShiftKeyDown();
    }

    public boolean isFallFlying() {
        return this.getSharedFlag(7);
    }

    @Override
    public boolean isVisuallySwimming() {
        return super.isVisuallySwimming() || !this.isFallFlying() && this.hasPose(Pose.FALL_FLYING);
    }

    public int getFallFlyingTicks() {
        return this.fallFlyTicks;
    }

    public boolean randomTeleport(double pX, double pY, double pZ, boolean pBroadcastTeleport) {
        double d0 = this.getX();
        double d1 = this.getY();
        double d2 = this.getZ();
        double d3 = pY;
        boolean flag = false;
        BlockPos blockpos = BlockPos.containing(pX, pY, pZ);
        Level level = this.level();
        if (level.hasChunkAt(blockpos)) {
            boolean flag1 = false;

            while (!flag1 && blockpos.getY() > level.getMinY()) {
                BlockPos blockpos1 = blockpos.below();
                BlockState blockstate = level.getBlockState(blockpos1);
                if (blockstate.blocksMotion()) {
                    flag1 = true;
                } else {
                    d3--;
                    blockpos = blockpos1;
                }
            }

            if (flag1) {
                this.teleportTo(pX, d3, pZ);
                if (level.noCollision(this) && !level.containsAnyLiquid(this.getBoundingBox())) {
                    flag = true;
                }
            }
        }

        if (!flag) {
            this.teleportTo(d0, d1, d2);
            return false;
        } else {
            if (pBroadcastTeleport) {
                level.broadcastEntityEvent(this, (byte)46);
            }

            if (this instanceof PathfinderMob pathfindermob) {
                pathfindermob.getNavigation().stop();
            }

            return true;
        }
    }

    public boolean isAffectedByPotions() {
        return !this.isDeadOrDying();
    }

    public boolean attackable() {
        return true;
    }

    public void setRecordPlayingNearby(BlockPos pJukebox, boolean pPartyParrot) {
    }

    public boolean canPickUpLoot() {
        return false;
    }

    @Override
    public final EntityDimensions getDimensions(Pose pPose) {
        return pPose == Pose.SLEEPING ? SLEEPING_DIMENSIONS : this.getDefaultDimensions(pPose).scale(this.getScale());
    }

    protected EntityDimensions getDefaultDimensions(Pose pPose) {
        return this.getType().getDimensions().scale(this.getAgeScale());
    }

    public ImmutableList<Pose> getDismountPoses() {
        return ImmutableList.of(Pose.STANDING);
    }

    public AABB getLocalBoundsForPose(Pose pPose) {
        EntityDimensions entitydimensions = this.getDimensions(pPose);
        return new AABB(
            -entitydimensions.width() / 2.0F,
            0.0,
            -entitydimensions.width() / 2.0F,
            entitydimensions.width() / 2.0F,
            entitydimensions.height(),
            entitydimensions.width() / 2.0F
        );
    }

    protected boolean wouldNotSuffocateAtTargetPose(Pose pPose) {
        AABB aabb = this.getDimensions(pPose).makeBoundingBox(this.position());
        return this.level().noBlockCollision(this, aabb);
    }

    @Override
    public boolean canUsePortal(boolean p_342370_) {
        return super.canUsePortal(p_342370_) && !this.isSleeping();
    }

    public Optional<BlockPos> getSleepingPos() {
        return this.entityData.get(SLEEPING_POS_ID);
    }

    public void setSleepingPos(BlockPos pPos) {
        this.entityData.set(SLEEPING_POS_ID, Optional.of(pPos));
    }

    public void clearSleepingPos() {
        this.entityData.set(SLEEPING_POS_ID, Optional.empty());
    }

    public boolean isSleeping() {
        return this.getSleepingPos().isPresent();
    }

    public void startSleeping(BlockPos pPos) {
        if (this.isPassenger()) {
            this.stopRiding();
        }

        BlockState blockstate = this.level().getBlockState(pPos);
        if (blockstate.isBed(level(), pPos, this)) {
            blockstate.setBedOccupied(level(), pPos, this, true);
        }

        this.setPose(Pose.SLEEPING);
        this.setPosToBed(pPos);
        this.setSleepingPos(pPos);
        this.setDeltaMovement(Vec3.ZERO);
        this.hasImpulse = true;
    }

    private void setPosToBed(BlockPos pPos) {
        this.setPos(pPos.getX() + 0.5, pPos.getY() + 0.6875, pPos.getZ() + 0.5);
    }

    private boolean checkBedExists() {
        return this.getSleepingPos().map(p_341272_ -> net.minecraftforge.event.ForgeEventFactory.fireSleepingLocationCheck(this, p_341272_)).orElse(false);
    }

    public void stopSleeping() {
        this.getSleepingPos().filter(this.level()::hasChunkAt).ifPresent(p_261435_ -> {
            BlockState blockstate = this.level().getBlockState(p_261435_);
            if (blockstate.isBed(level(), p_261435_, this)) {
                Direction direction = blockstate.getValue(BedBlock.FACING);
                blockstate.setBedOccupied(level(), p_261435_, this, false);
                Vec3 vec31 = BedBlock.findStandUpPosition(this.getType(), this.level(), p_261435_, direction, this.getYRot()).orElseGet(() -> {
                    BlockPos blockpos = p_261435_.above();
                    return new Vec3(blockpos.getX() + 0.5, blockpos.getY() + 0.1, blockpos.getZ() + 0.5);
                });
                Vec3 vec32 = Vec3.atBottomCenterOf(p_261435_).subtract(vec31).normalize();
                float f = (float)Mth.wrapDegrees(Mth.atan2(vec32.z, vec32.x) * 180.0F / (float)Math.PI - 90.0);
                this.setPos(vec31.x, vec31.y, vec31.z);
                this.setYRot(f);
                this.setXRot(0.0F);
            }
        });
        Vec3 vec3 = this.position();
        this.setPose(Pose.STANDING);
        this.setPos(vec3.x, vec3.y, vec3.z);
        this.clearSleepingPos();
    }

    @Nullable
    public Direction getBedOrientation() {
        BlockPos blockpos = this.getSleepingPos().orElse(null);
        if (blockpos == null) return Direction.UP;
        BlockState state = this.level().getBlockState(blockpos);
        return !state.isBed(level(), blockpos, this) ? Direction.UP : state.getBedDirection(level(), blockpos);
    }

    @Override
    public boolean isInWall() {
        return !this.isSleeping() && super.isInWall();
    }

    public ItemStack getProjectile(ItemStack pWeaponStack) {
        return net.minecraftforge.common.ForgeHooks.getProjectile(this, pWeaponStack, ItemStack.EMPTY);
    }

    private static byte entityEventForEquipmentBreak(EquipmentSlot pSlot) {
        return switch (pSlot) {
            case MAINHAND -> 47;
            case OFFHAND -> 48;
            case HEAD -> 49;
            case CHEST -> 50;
            case FEET -> 52;
            case LEGS -> 51;
            case BODY -> 65;
            case SADDLE -> 68;
        };
    }

    public void onEquippedItemBroken(Item pItem, EquipmentSlot pSlot) {
        this.level().broadcastEntityEvent(this, entityEventForEquipmentBreak(pSlot));
        this.stopLocationBasedEffects(this.getItemBySlot(pSlot), pSlot, this.attributes);
    }

    private void stopLocationBasedEffects(ItemStack pStack, EquipmentSlot pSlot, AttributeMap pAttributeMap) {
        pStack.forEachModifier(pSlot, (p_358882_, p_358883_) -> {
            AttributeInstance attributeinstance = pAttributeMap.getInstance(p_358882_);
            if (attributeinstance != null) {
                attributeinstance.removeModifier(p_358883_);
            }
        });
        EnchantmentHelper.stopLocationBasedEffects(pStack, this, pSlot);
    }

    public static EquipmentSlot getSlotForHand(InteractionHand pHand) {
        return pHand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
    }

    public final boolean canEquipWithDispenser(ItemStack pStack) {
        if (this.isAlive() && !this.isSpectator()) {
            Equippable equippable = pStack.get(DataComponents.EQUIPPABLE);
            if (equippable != null && equippable.dispensable()) {
                EquipmentSlot equipmentslot = equippable.slot();
                return this.canUseSlot(equipmentslot) && equippable.canBeEquippedBy(this.getType())
                    ? this.getItemBySlot(equipmentslot).isEmpty() && this.canDispenserEquipIntoSlot(equipmentslot)
                    : false;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    protected boolean canDispenserEquipIntoSlot(EquipmentSlot pSlot) {
        return true;
    }

    public final EquipmentSlot getEquipmentSlotForItem(ItemStack pStack) {
        final EquipmentSlot slot = pStack.getEquipmentSlot();
        if (slot != null) return slot; // FORGE: Allow modders to set a non-default equipment slot for a stack; e.g. a non-armor chestplate-slot item
        Equippable equippable = pStack.get(DataComponents.EQUIPPABLE);
        return equippable != null && this.canUseSlot(equippable.slot()) ? equippable.slot() : EquipmentSlot.MAINHAND;
    }

    public final boolean isEquippableInSlot(ItemStack pStack, EquipmentSlot pSlot) {
        Equippable equippable = pStack.get(DataComponents.EQUIPPABLE);
        return equippable == null
            ? pSlot == EquipmentSlot.MAINHAND && this.canUseSlot(EquipmentSlot.MAINHAND)
            : pSlot == equippable.slot() && this.canUseSlot(equippable.slot()) && equippable.canBeEquippedBy(this.getType());
    }

    private static SlotAccess createEquipmentSlotAccess(LivingEntity pEntity, EquipmentSlot pSlot) {
        return pSlot != EquipmentSlot.HEAD && pSlot != EquipmentSlot.MAINHAND && pSlot != EquipmentSlot.OFFHAND
            ? SlotAccess.forEquipmentSlot(pEntity, pSlot, p_341262_ -> p_341262_.isEmpty() || pEntity.getEquipmentSlotForItem(p_341262_) == pSlot)
            : SlotAccess.forEquipmentSlot(pEntity, pSlot);
    }

    @Nullable
    private static EquipmentSlot getEquipmentSlot(int pIndex) {
        if (pIndex == 100 + EquipmentSlot.HEAD.getIndex()) {
            return EquipmentSlot.HEAD;
        } else if (pIndex == 100 + EquipmentSlot.CHEST.getIndex()) {
            return EquipmentSlot.CHEST;
        } else if (pIndex == 100 + EquipmentSlot.LEGS.getIndex()) {
            return EquipmentSlot.LEGS;
        } else if (pIndex == 100 + EquipmentSlot.FEET.getIndex()) {
            return EquipmentSlot.FEET;
        } else if (pIndex == 98) {
            return EquipmentSlot.MAINHAND;
        } else if (pIndex == 99) {
            return EquipmentSlot.OFFHAND;
        } else if (pIndex == 105) {
            return EquipmentSlot.BODY;
        } else {
            return pIndex == 106 ? EquipmentSlot.SADDLE : null;
        }
    }

    @Override
    public SlotAccess getSlot(int p_147238_) {
        EquipmentSlot equipmentslot = getEquipmentSlot(p_147238_);
        return equipmentslot != null ? createEquipmentSlotAccess(this, equipmentslot) : super.getSlot(p_147238_);
    }

    @Override
    public boolean canFreeze() {
        if (this.isSpectator()) {
            return false;
        } else {
            for (EquipmentSlot equipmentslot : EquipmentSlotGroup.ARMOR) {
                if (this.getItemBySlot(equipmentslot).is(ItemTags.FREEZE_IMMUNE_WEARABLES)) {
                    return false;
                }
            }

            return super.canFreeze();
        }
    }

    @Override
    public boolean isCurrentlyGlowing() {
        return !this.level().isClientSide() && this.hasEffect(MobEffects.GLOWING) || super.isCurrentlyGlowing();
    }

    @Override
    public float getVisualRotationYInDegrees() {
        return this.yBodyRot;
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket p_217037_) {
        double d0 = p_217037_.getX();
        double d1 = p_217037_.getY();
        double d2 = p_217037_.getZ();
        float f = p_217037_.getYRot();
        float f1 = p_217037_.getXRot();
        this.syncPacketPositionCodec(d0, d1, d2);
        this.yBodyRot = p_217037_.getYHeadRot();
        this.yHeadRot = p_217037_.getYHeadRot();
        this.yBodyRotO = this.yBodyRot;
        this.yHeadRotO = this.yHeadRot;
        this.setId(p_217037_.getId());
        this.setUUID(p_217037_.getUUID());
        this.absSnapTo(d0, d1, d2, f, f1);
        this.setDeltaMovement(p_217037_.getXa(), p_217037_.getYa(), p_217037_.getZa());
    }

    public float getSecondsToDisableBlocking() {
        Weapon weapon = this.getWeaponItem().get(DataComponents.WEAPON);
        return weapon != null ? weapon.disableBlockingForSeconds() : 0.0F;
    }

    @Override
    public float maxUpStep() {
        float f = (float)this.getAttributeValue(Attributes.STEP_HEIGHT);
        return this.getControllingPassenger() instanceof Player ? Math.max(f, 1.0F) : f;
    }

    @Override
    public Vec3 getPassengerRidingPosition(Entity p_299288_) {
        return this.position().add(this.getPassengerAttachmentPoint(p_299288_, this.getDimensions(this.getPose()), this.getScale() * this.getAgeScale()));
    }

    protected void lerpHeadRotationStep(int pLerpHeadSteps, double pLerpYHeadRot) {
        this.yHeadRot = (float)Mth.rotLerp(1.0 / pLerpHeadSteps, this.yHeadRot, pLerpYHeadRot);
    }

    @Override
    public void igniteForTicks(int p_328356_) {
        super.igniteForTicks(Mth.ceil(p_328356_ * this.getAttributeValue(Attributes.BURNING_TIME)));
    }

    public boolean hasInfiniteMaterials() {
        return false;
    }

    public boolean isInvulnerableTo(ServerLevel pLevel, DamageSource pDamageSource) {
        return this.isInvulnerableToBase(pDamageSource) || EnchantmentHelper.isImmuneToDamage(pLevel, this, pDamageSource);
    }

    public static boolean canGlideUsing(ItemStack pStack, EquipmentSlot pSlot) {
        if (!pStack.has(DataComponents.GLIDER)) {
            return false;
        } else {
            Equippable equippable = pStack.get(DataComponents.EQUIPPABLE);
            return equippable != null && pSlot == equippable.slot() && !pStack.nextDamageWillBreak();
        }
    }

    @VisibleForTesting
    public int getLastHurtByPlayerMemoryTime() {
        return this.lastHurtByPlayerMemoryTime;
    }

    public record Fallsounds(SoundEvent small, SoundEvent big) {
    }

    /**
     * Returns true if the entity's rider (EntityPlayer) should face forward when mounted.
     * currently only used in vanilla code by pigs.
     *
     * @param player The player who is riding the entity.
     * @return If the player should orient the same direction as this entity.
     */
    public boolean shouldRiderFaceForward(Player player) {
        return this instanceof net.minecraft.world.entity.animal.Pig;
    }

    private net.minecraftforge.common.util.LazyOptional<?>[] handlers = net.minecraftforge.items.wrapper.EntityEquipmentInvWrapper.create(this);

    @Override
    public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> capability, @Nullable Direction facing) {
        if (capability == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER && this.isAlive()) {
             if (facing == null) return handlers[2].cast();
             else if (facing.getAxis().isVertical()) return handlers[0].cast();
             else if (facing.getAxis().isHorizontal()) return handlers[1].cast();
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        for (int x = 0; x < handlers.length; x++) {
             handlers[x].invalidate();
        }
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        handlers = net.minecraftforge.items.wrapper.EntityEquipmentInvWrapper.create(this);
    }
}
