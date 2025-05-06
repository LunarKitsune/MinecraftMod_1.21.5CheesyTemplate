package net.minecraft.world.entity.raid;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.PathfindToRaidGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.monster.PatrollingMonster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

public abstract class Raider extends PatrollingMonster {
    protected static final EntityDataAccessor<Boolean> IS_CELEBRATING = SynchedEntityData.defineId(Raider.class, EntityDataSerializers.BOOLEAN);
    static final Predicate<ItemEntity> ALLOWED_ITEMS = p_390760_ -> !p_390760_.hasPickUpDelay()
        && p_390760_.isAlive()
        && ItemStack.matches(p_390760_.getItem(), Raid.getOminousBannerInstance(p_390760_.registryAccess().lookupOrThrow(Registries.BANNER_PATTERN)));
    private static final int DEFAULT_WAVE = 0;
    private static final boolean DEFAULT_CAN_JOIN_RAID = false;
    @Nullable
    protected Raid raid;
    private int wave = 0;
    private boolean canJoinRaid = false;
    private int ticksOutsideRaid;

    protected Raider(EntityType<? extends Raider> p_37839_, Level p_37840_) {
        super(p_37839_, p_37840_);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new Raider.ObtainRaidLeaderBannerGoal<>(this));
        this.goalSelector.addGoal(3, new PathfindToRaidGoal<>(this));
        this.goalSelector.addGoal(4, new Raider.RaiderMoveThroughVillageGoal(this, 1.05F, 1));
        this.goalSelector.addGoal(5, new Raider.RaiderCelebration(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder p_333182_) {
        super.defineSynchedData(p_333182_);
        p_333182_.define(IS_CELEBRATING, false);
    }

    public abstract void applyRaidBuffs(ServerLevel pLevel, int pWave, boolean pUnused);

    public boolean canJoinRaid() {
        return this.canJoinRaid;
    }

    public void setCanJoinRaid(boolean pCanJoinRaid) {
        this.canJoinRaid = pCanJoinRaid;
    }

    @Override
    public void aiStep() {
        if (this.level() instanceof ServerLevel serverlevel && this.isAlive()) {
            Raid raid1 = this.getCurrentRaid();
            if (this.canJoinRaid()) {
                if (raid1 == null) {
                    if (this.level().getGameTime() % 20L == 0L) {
                        Raid raid = serverlevel.getRaidAt(this.blockPosition());
                        if (raid != null && Raids.canJoinRaid(this)) {
                            raid.joinRaid(serverlevel, raid.getGroupsSpawned(), this, null, true);
                        }
                    }
                } else {
                    LivingEntity livingentity = this.getTarget();
                    if (livingentity != null && (livingentity.getType() == EntityType.PLAYER || livingentity.getType() == EntityType.IRON_GOLEM)) {
                        this.noActionTime = 0;
                    }
                }
            }
        }

        super.aiStep();
    }

    @Override
    protected void updateNoActionTime() {
        this.noActionTime += 2;
    }

    @Override
    public void die(DamageSource pCause) {
        if (this.level() instanceof ServerLevel serverlevel) {
            Entity entity = pCause.getEntity();
            Raid raid = this.getCurrentRaid();
            if (raid != null) {
                if (this.isPatrolLeader()) {
                    raid.removeLeader(this.getWave());
                }

                if (entity != null && entity.getType() == EntityType.PLAYER) {
                    raid.addHeroOfTheVillage(entity);
                }

                raid.removeFromRaid(serverlevel, this, false);
            }
        }

        super.die(pCause);
    }

    @Override
    public boolean canJoinPatrol() {
        return !this.hasActiveRaid();
    }

    public void setCurrentRaid(@Nullable Raid pRaid) {
        this.raid = pRaid;
    }

    @Nullable
    public Raid getCurrentRaid() {
        return this.raid;
    }

    public boolean isCaptain() {
        ItemStack itemstack = this.getItemBySlot(EquipmentSlot.HEAD);
        boolean flag = !itemstack.isEmpty() && ItemStack.matches(itemstack, Raid.getOminousBannerInstance(this.registryAccess().lookupOrThrow(Registries.BANNER_PATTERN)));
        boolean flag1 = this.isPatrolLeader();
        return flag && flag1;
    }

    public boolean hasRaid() {
        return !(this.level() instanceof ServerLevel serverlevel) ? false : this.getCurrentRaid() != null || serverlevel.getRaidAt(this.blockPosition()) != null;
    }

    public boolean hasActiveRaid() {
        return this.getCurrentRaid() != null && this.getCurrentRaid().isActive();
    }

    public void setWave(int pWave) {
        this.wave = pWave;
    }

    public int getWave() {
        return this.wave;
    }

    public boolean isCelebrating() {
        return this.entityData.get(IS_CELEBRATING);
    }

    public void setCelebrating(boolean pCelebrating) {
        this.entityData.set(IS_CELEBRATING, pCelebrating);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putInt("Wave", this.wave);
        pCompound.putBoolean("CanJoinRaid", this.canJoinRaid);
        if (this.raid != null && this.level() instanceof ServerLevel serverlevel) {
            serverlevel.getRaids().getId(this.raid).ifPresent(p_390764_ -> pCompound.putInt("RaidId", p_390764_));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        this.wave = pCompound.getIntOr("Wave", 0);
        this.canJoinRaid = pCompound.getBooleanOr("CanJoinRaid", false);
        if (this.level() instanceof ServerLevel serverlevel) {
            pCompound.getInt("RaidId").ifPresent(p_390762_ -> {
                this.raid = serverlevel.getRaids().get(p_390762_);
                if (this.raid != null) {
                    this.raid.addWaveMob(serverlevel, this.wave, this, false);
                    if (this.isPatrolLeader()) {
                        this.raid.setLeader(this.wave, this);
                    }
                }
            });
        }
    }

    @Override
    protected void pickUpItem(ServerLevel p_362025_, ItemEntity p_37866_) {
        ItemStack itemstack = p_37866_.getItem();
        boolean flag = this.hasActiveRaid() && this.getCurrentRaid().getLeader(this.getWave()) != null;
        if (this.hasActiveRaid() && !flag && ItemStack.matches(itemstack, Raid.getOminousBannerInstance(this.registryAccess().lookupOrThrow(Registries.BANNER_PATTERN)))) {
            EquipmentSlot equipmentslot = EquipmentSlot.HEAD;
            ItemStack itemstack1 = this.getItemBySlot(equipmentslot);
            double d0 = this.getDropChances().byEquipment(equipmentslot);
            if (!itemstack1.isEmpty() && Math.max(this.random.nextFloat() - 0.1F, 0.0F) < d0) {
                this.spawnAtLocation(p_362025_, itemstack1);
            }

            this.onItemPickup(p_37866_);
            this.setItemSlot(equipmentslot, itemstack);
            this.take(p_37866_, itemstack.getCount());
            p_37866_.discard();
            this.getCurrentRaid().setLeader(this.getWave(), this);
            this.setPatrolLeader(true);
        } else {
            super.pickUpItem(p_362025_, p_37866_);
        }
    }

    @Override
    public boolean removeWhenFarAway(double pDistanceToClosestPlayer) {
        return this.getCurrentRaid() == null ? super.removeWhenFarAway(pDistanceToClosestPlayer) : false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return super.requiresCustomPersistence() || this.getCurrentRaid() != null;
    }

    public int getTicksOutsideRaid() {
        return this.ticksOutsideRaid;
    }

    public void setTicksOutsideRaid(int pTicksOutsideRaid) {
        this.ticksOutsideRaid = pTicksOutsideRaid;
    }

    @Override
    public boolean hurtServer(ServerLevel p_363909_, DamageSource p_362937_, float p_369722_) {
        if (this.hasActiveRaid()) {
            this.getCurrentRaid().updateBossbar();
        }

        return super.hurtServer(p_363909_, p_362937_, p_369722_);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor p_37856_, DifficultyInstance p_37857_, EntitySpawnReason p_368278_, @Nullable SpawnGroupData p_37859_) {
        this.setCanJoinRaid(this.getType() != EntityType.WITCH || p_368278_ != EntitySpawnReason.NATURAL);
        return super.finalizeSpawn(p_37856_, p_37857_, p_368278_, p_37859_);
    }

    public abstract SoundEvent getCelebrateSound();

    protected static class HoldGroundAttackGoal extends Goal {
        private final Raider mob;
        private final float hostileRadiusSqr;
        public final TargetingConditions shoutTargeting = TargetingConditions.forNonCombat().range(8.0).ignoreLineOfSight().ignoreInvisibilityTesting();

        public HoldGroundAttackGoal(AbstractIllager pMob, float pRadius) {
            this.mob = pMob;
            this.hostileRadiusSqr = pRadius * pRadius;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity livingentity = this.mob.getLastHurtByMob();
            return this.mob.getCurrentRaid() == null
                && this.mob.isPatrolling()
                && this.mob.getTarget() != null
                && !this.mob.isAggressive()
                && (livingentity == null || livingentity.getType() != EntityType.PLAYER);
        }

        @Override
        public void start() {
            super.start();
            this.mob.getNavigation().stop();

            for (Raider raider : getServerLevel(this.mob)
                .getNearbyEntities(Raider.class, this.shoutTargeting, this.mob, this.mob.getBoundingBox().inflate(8.0, 8.0, 8.0))) {
                raider.setTarget(this.mob.getTarget());
            }
        }

        @Override
        public void stop() {
            super.stop();
            LivingEntity livingentity = this.mob.getTarget();
            if (livingentity != null) {
                for (Raider raider : getServerLevel(this.mob)
                    .getNearbyEntities(Raider.class, this.shoutTargeting, this.mob, this.mob.getBoundingBox().inflate(8.0, 8.0, 8.0))) {
                    raider.setTarget(livingentity);
                    raider.setAggressive(true);
                }

                this.mob.setAggressive(true);
            }
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity livingentity = this.mob.getTarget();
            if (livingentity != null) {
                if (this.mob.distanceToSqr(livingentity) > this.hostileRadiusSqr) {
                    this.mob.getLookControl().setLookAt(livingentity, 30.0F, 30.0F);
                    if (this.mob.random.nextInt(50) == 0) {
                        this.mob.playAmbientSound();
                    }
                } else {
                    this.mob.setAggressive(true);
                }

                super.tick();
            }
        }
    }

    public class ObtainRaidLeaderBannerGoal<T extends Raider> extends Goal {
        private final T mob;
        private Int2LongOpenHashMap unreachableBannerCache = new Int2LongOpenHashMap();
        @Nullable
        private Path pathToBanner;
        @Nullable
        private ItemEntity pursuedBannerItemEntity;

        public ObtainRaidLeaderBannerGoal(final T pMob) {
            this.mob = pMob;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (this.cannotPickUpBanner()) {
                return false;
            } else {
                Int2LongOpenHashMap int2longopenhashmap = new Int2LongOpenHashMap();
                double d0 = Raider.this.getAttributeValue(Attributes.FOLLOW_RANGE);

                for (ItemEntity itementity : this.mob.level().getEntitiesOfClass(ItemEntity.class, this.mob.getBoundingBox().inflate(d0, 8.0, d0), Raider.ALLOWED_ITEMS)) {
                    long i = this.unreachableBannerCache.getOrDefault(itementity.getId(), Long.MIN_VALUE);
                    if (Raider.this.level().getGameTime() < i) {
                        int2longopenhashmap.put(itementity.getId(), i);
                    } else {
                        Path path = this.mob.getNavigation().createPath(itementity, 1);
                        if (path != null && path.canReach()) {
                            this.pathToBanner = path;
                            this.pursuedBannerItemEntity = itementity;
                            return true;
                        }

                        int2longopenhashmap.put(itementity.getId(), Raider.this.level().getGameTime() + 600L);
                    }
                }

                this.unreachableBannerCache = int2longopenhashmap;
                return false;
            }
        }

        @Override
        public boolean canContinueToUse() {
            if (this.pursuedBannerItemEntity == null || this.pathToBanner == null) {
                return false;
            } else if (this.pursuedBannerItemEntity.isRemoved()) {
                return false;
            } else {
                return this.pathToBanner.isDone() ? false : !this.cannotPickUpBanner();
            }
        }

        private boolean cannotPickUpBanner() {
            if (!this.mob.hasActiveRaid()) {
                return true;
            } else if (this.mob.getCurrentRaid().isOver()) {
                return true;
            } else if (!this.mob.canBeLeader()) {
                return true;
            } else if (ItemStack.matches(this.mob.getItemBySlot(EquipmentSlot.HEAD), Raid.getOminousBannerInstance(this.mob.registryAccess().lookupOrThrow(Registries.BANNER_PATTERN)))
                )
             {
                return true;
            } else {
                Raider raider = Raider.this.raid.getLeader(this.mob.getWave());
                return raider != null && raider.isAlive();
            }
        }

        @Override
        public void start() {
            this.mob.getNavigation().moveTo(this.pathToBanner, 1.15F);
        }

        @Override
        public void stop() {
            this.pathToBanner = null;
            this.pursuedBannerItemEntity = null;
        }

        @Override
        public void tick() {
            if (this.pursuedBannerItemEntity != null && this.pursuedBannerItemEntity.closerThan(this.mob, 1.414)) {
                this.mob.pickUpItem(getServerLevel(Raider.this.level()), this.pursuedBannerItemEntity);
            }
        }
    }

    public class RaiderCelebration extends Goal {
        private final Raider mob;

        RaiderCelebration(final Raider pMob) {
            this.mob = pMob;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            Raid raid = this.mob.getCurrentRaid();
            return this.mob.isAlive() && this.mob.getTarget() == null && raid != null && raid.isLoss();
        }

        @Override
        public void start() {
            this.mob.setCelebrating(true);
            super.start();
        }

        @Override
        public void stop() {
            this.mob.setCelebrating(false);
            super.stop();
        }

        @Override
        public void tick() {
            if (!this.mob.isSilent() && this.mob.random.nextInt(this.adjustedTickDelay(100)) == 0) {
                Raider.this.makeSound(Raider.this.getCelebrateSound());
            }

            if (!this.mob.isPassenger() && this.mob.random.nextInt(this.adjustedTickDelay(50)) == 0) {
                this.mob.getJumpControl().jump();
            }

            super.tick();
        }
    }

    static class RaiderMoveThroughVillageGoal extends Goal {
        private final Raider raider;
        private final double speedModifier;
        private BlockPos poiPos;
        private final List<BlockPos> visited = Lists.newArrayList();
        private final int distanceToPoi;
        private boolean stuck;

        public RaiderMoveThroughVillageGoal(Raider pRaider, double pSpeedModifier, int pDistanceToPoi) {
            this.raider = pRaider;
            this.speedModifier = pSpeedModifier;
            this.distanceToPoi = pDistanceToPoi;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            this.updateVisited();
            return this.isValidRaid() && this.hasSuitablePoi() && this.raider.getTarget() == null;
        }

        private boolean isValidRaid() {
            return this.raider.hasActiveRaid() && !this.raider.getCurrentRaid().isOver();
        }

        private boolean hasSuitablePoi() {
            ServerLevel serverlevel = (ServerLevel)this.raider.level();
            BlockPos blockpos = this.raider.blockPosition();
            Optional<BlockPos> optional = serverlevel.getPoiManager()
                .getRandom(p_219843_ -> p_219843_.is(PoiTypes.HOME), this::hasNotVisited, PoiManager.Occupancy.ANY, blockpos, 48, this.raider.random);
            if (optional.isEmpty()) {
                return false;
            } else {
                this.poiPos = optional.get().immutable();
                return true;
            }
        }

        @Override
        public boolean canContinueToUse() {
            return this.raider.getNavigation().isDone()
                ? false
                : this.raider.getTarget() == null
                    && !this.poiPos.closerToCenterThan(this.raider.position(), this.raider.getBbWidth() + this.distanceToPoi)
                    && !this.stuck;
        }

        @Override
        public void stop() {
            if (this.poiPos.closerToCenterThan(this.raider.position(), this.distanceToPoi)) {
                this.visited.add(this.poiPos);
            }
        }

        @Override
        public void start() {
            super.start();
            this.raider.setNoActionTime(0);
            this.raider.getNavigation().moveTo(this.poiPos.getX(), this.poiPos.getY(), this.poiPos.getZ(), this.speedModifier);
            this.stuck = false;
        }

        @Override
        public void tick() {
            if (this.raider.getNavigation().isDone()) {
                Vec3 vec3 = Vec3.atBottomCenterOf(this.poiPos);
                Vec3 vec31 = DefaultRandomPos.getPosTowards(this.raider, 16, 7, vec3, (float) (Math.PI / 10));
                if (vec31 == null) {
                    vec31 = DefaultRandomPos.getPosTowards(this.raider, 8, 7, vec3, (float) (Math.PI / 2));
                }

                if (vec31 == null) {
                    this.stuck = true;
                    return;
                }

                this.raider.getNavigation().moveTo(vec31.x, vec31.y, vec31.z, this.speedModifier);
            }
        }

        private boolean hasNotVisited(BlockPos pPos) {
            for (BlockPos blockpos : this.visited) {
                if (Objects.equals(pPos, blockpos)) {
                    return false;
                }
            }

            return true;
        }

        private void updateVisited() {
            if (this.visited.size() > 2) {
                this.visited.remove(0);
            }
        }
    }
}