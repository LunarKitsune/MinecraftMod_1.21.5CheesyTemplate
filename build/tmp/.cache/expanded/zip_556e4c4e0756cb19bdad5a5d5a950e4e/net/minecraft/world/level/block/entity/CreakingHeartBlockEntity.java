package net.minecraft.world.level.block.entity;

import com.mojang.datafixers.util.Either;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.TrailParticleOption;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.SpawnUtil;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.creaking.Creaking;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CreakingHeartBlock;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.CreakingHeartState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;

public class CreakingHeartBlockEntity extends BlockEntity {
    private static final int PLAYER_DETECTION_RANGE = 32;
    public static final int CREAKING_ROAMING_RADIUS = 32;
    private static final int DISTANCE_CREAKING_TOO_FAR = 34;
    private static final int SPAWN_RANGE_XZ = 16;
    private static final int SPAWN_RANGE_Y = 8;
    private static final int ATTEMPTS_PER_SPAWN = 5;
    private static final int UPDATE_TICKS = 20;
    private static final int UPDATE_TICKS_VARIANCE = 5;
    private static final int HURT_CALL_TOTAL_TICKS = 100;
    private static final int NUMBER_OF_HURT_CALLS = 10;
    private static final int HURT_CALL_INTERVAL = 10;
    private static final int HURT_CALL_PARTICLE_TICKS = 50;
    private static final int MAX_DEPTH = 2;
    private static final int MAX_COUNT = 64;
    private static final int TICKS_GRACE_PERIOD = 30;
    private static final Optional<Creaking> NO_CREAKING = Optional.empty();
    @Nullable
    private Either<Creaking, UUID> creakingInfo;
    private long ticksExisted;
    private int ticker;
    private int emitter;
    @Nullable
    private Vec3 emitterTarget;
    private int outputSignal;

    public CreakingHeartBlockEntity(BlockPos pPos, BlockState pState) {
        super(BlockEntityType.CREAKING_HEART, pPos, pState);
    }

    public static void serverTick(Level pLevel, BlockPos pPos, BlockState pState, CreakingHeartBlockEntity pCreakingHeart) {
        pCreakingHeart.ticksExisted++;
        if (pLevel instanceof ServerLevel serverlevel) {
            int $$6 = pCreakingHeart.computeAnalogOutputSignal();
            if (pCreakingHeart.outputSignal != $$6) {
                pCreakingHeart.outputSignal = $$6;
                pLevel.updateNeighbourForOutputSignal(pPos, Blocks.CREAKING_HEART);
            }

            if (pCreakingHeart.emitter > 0) {
                if (pCreakingHeart.emitter > 50) {
                    pCreakingHeart.emitParticles(serverlevel, 1, true);
                    pCreakingHeart.emitParticles(serverlevel, 1, false);
                }

                if (pCreakingHeart.emitter % 10 == 0 && pCreakingHeart.emitterTarget != null) {
                    pCreakingHeart.getCreakingProtector().ifPresent(p_376513_ -> pCreakingHeart.emitterTarget = p_376513_.getBoundingBox().getCenter());
                    Vec3 vec3 = Vec3.atCenterOf(pPos);
                    float f = 0.2F + 0.8F * (100 - pCreakingHeart.emitter) / 100.0F;
                    Vec3 vec31 = vec3.subtract(pCreakingHeart.emitterTarget).scale(f).add(pCreakingHeart.emitterTarget);
                    BlockPos blockpos = BlockPos.containing(vec31);
                    float f1 = pCreakingHeart.emitter / 2.0F / 100.0F + 0.5F;
                    serverlevel.playSound(null, blockpos, SoundEvents.CREAKING_HEART_HURT, SoundSource.BLOCKS, f1, 1.0F);
                }

                pCreakingHeart.emitter--;
            }

            if (pCreakingHeart.ticker-- < 0) {
                pCreakingHeart.ticker = pCreakingHeart.level == null ? 20 : pCreakingHeart.level.random.nextInt(5) + 20;
                BlockState blockstate = updateCreakingState(pLevel, pState, pPos, pCreakingHeart);
                if (blockstate != pState) {
                    pLevel.setBlock(pPos, blockstate, 3);
                    if (blockstate.getValue(CreakingHeartBlock.STATE) == CreakingHeartState.UPROOTED) {
                        return;
                    }
                }

                if (pCreakingHeart.creakingInfo == null) {
                    if (blockstate.getValue(CreakingHeartBlock.STATE) == CreakingHeartState.AWAKE) {
                        if (pLevel.getDifficulty() != Difficulty.PEACEFUL) {
                            if (serverlevel.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) {
                                Player player = pLevel.getNearestPlayer(pPos.getX(), pPos.getY(), pPos.getZ(), 32.0, false);
                                if (player != null) {
                                    Creaking creaking1 = spawnProtector(serverlevel, pCreakingHeart);
                                    if (creaking1 != null) {
                                        pCreakingHeart.setCreakingInfo(creaking1);
                                        creaking1.makeSound(SoundEvents.CREAKING_SPAWN);
                                        pLevel.playSound(null, pCreakingHeart.getBlockPos(), SoundEvents.CREAKING_HEART_SPAWN, SoundSource.BLOCKS, 1.0F, 1.0F);
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Optional<Creaking> optional = pCreakingHeart.getCreakingProtector();
                    if (optional.isPresent()) {
                        Creaking creaking = optional.get();
                        if (!CreakingHeartBlock.isNaturalNight(pLevel) && !creaking.isPersistenceRequired() || pCreakingHeart.distanceToCreaking() > 34.0 || creaking.playerIsStuckInYou()) {
                            pCreakingHeart.removeProtector(null);
                        }
                    }
                }
            }
        }
    }

    private static BlockState updateCreakingState(Level pLevel, BlockState pState, BlockPos pPos, CreakingHeartBlockEntity pCreakingHeart) {
        if (!CreakingHeartBlock.hasRequiredLogs(pState, pLevel, pPos) && pCreakingHeart.creakingInfo == null) {
            return pState.setValue(CreakingHeartBlock.STATE, CreakingHeartState.UPROOTED);
        } else {
            boolean flag = CreakingHeartBlock.isNaturalNight(pLevel);
            return pState.setValue(CreakingHeartBlock.STATE, flag ? CreakingHeartState.AWAKE : CreakingHeartState.DORMANT);
        }
    }

    private double distanceToCreaking() {
        return this.getCreakingProtector().map(p_390963_ -> Math.sqrt(p_390963_.distanceToSqr(Vec3.atBottomCenterOf(this.getBlockPos())))).orElse(0.0);
    }

    private void clearCreakingInfo() {
        this.creakingInfo = null;
        this.setChanged();
    }

    public void setCreakingInfo(Creaking pCreaking) {
        this.creakingInfo = Either.left(pCreaking);
        this.setChanged();
    }

    public void setCreakingInfo(UUID pCreakingUuid) {
        this.creakingInfo = Either.right(pCreakingUuid);
        this.ticksExisted = 0L;
        this.setChanged();
    }

    private Optional<Creaking> getCreakingProtector() {
        if (this.creakingInfo == null) {
            return NO_CREAKING;
        } else {
            if (this.creakingInfo.left().isPresent()) {
                Creaking creaking = this.creakingInfo.left().get();
                if (!creaking.isRemoved()) {
                    return Optional.of(creaking);
                }

                this.setCreakingInfo(creaking.getUUID());
            }

            if (this.level instanceof ServerLevel serverlevel && this.creakingInfo.right().isPresent()) {
                UUID uuid = this.creakingInfo.right().get();
                if (serverlevel.getEntity(uuid) instanceof Creaking creaking1) {
                    this.setCreakingInfo(creaking1);
                    return Optional.of(creaking1);
                } else {
                    if (this.ticksExisted >= 30L) {
                        this.clearCreakingInfo();
                    }

                    return NO_CREAKING;
                }
            } else {
                return NO_CREAKING;
            }
        }
    }

    @Nullable
    private static Creaking spawnProtector(ServerLevel pLevel, CreakingHeartBlockEntity pCreakingHeart) {
        BlockPos blockpos = pCreakingHeart.getBlockPos();
        Optional<Creaking> optional = SpawnUtil.trySpawnMob(
            EntityType.CREAKING, EntitySpawnReason.SPAWNER, pLevel, blockpos, 5, 16, 8, SpawnUtil.Strategy.ON_TOP_OF_COLLIDER_NO_LEAVES, true
        );
        if (optional.isEmpty()) {
            return null;
        } else {
            Creaking creaking = optional.get();
            pLevel.gameEvent(creaking, GameEvent.ENTITY_PLACE, creaking.position());
            pLevel.broadcastEntityEvent(creaking, (byte)60);
            creaking.setTransient(blockpos);
            return creaking;
        }
    }

    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider p_366353_) {
        return this.saveCustomOnly(p_366353_);
    }

    public void creakingHurt() {
        if (this.getCreakingProtector().orElse(null) instanceof Creaking creaking) {
            if (this.level instanceof ServerLevel serverlevel) {
                if (this.emitter <= 0) {
                    this.emitParticles(serverlevel, 20, false);
                    if (this.getBlockState().getValue(CreakingHeartBlock.STATE) == CreakingHeartState.AWAKE) {
                        int j = this.level.getRandom().nextIntBetweenInclusive(2, 3);

                        for (int i = 0; i < j; i++) {
                            this.spreadResin().ifPresent(p_390962_ -> {
                                this.level.playSound(null, p_390962_, SoundEvents.RESIN_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
                                this.level.gameEvent(GameEvent.BLOCK_PLACE, p_390962_, GameEvent.Context.of(this.getBlockState()));
                            });
                        }
                    }

                    this.emitter = 100;
                    this.emitterTarget = creaking.getBoundingBox().getCenter();
                }
            }
        }
    }

    private Optional<BlockPos> spreadResin() {
        Mutable<BlockPos> mutable = new MutableObject<>(null);
        BlockPos.breadthFirstTraversal(this.worldPosition, 2, 64, (p_377894_, p_376787_) -> {
            for (Direction direction : Util.shuffledCopy(Direction.values(), this.level.random)) {
                BlockPos blockpos = p_377894_.relative(direction);
                if (this.level.getBlockState(blockpos).is(BlockTags.PALE_OAK_LOGS)) {
                    p_376787_.accept(blockpos);
                }
            }
        }, p_377354_ -> {
            if (!this.level.getBlockState(p_377354_).is(BlockTags.PALE_OAK_LOGS)) {
                return BlockPos.TraversalNodeStatus.ACCEPT;
            } else {
                for (Direction direction : Util.shuffledCopy(Direction.values(), this.level.random)) {
                    BlockPos blockpos = p_377354_.relative(direction);
                    BlockState blockstate = this.level.getBlockState(blockpos);
                    Direction direction1 = direction.getOpposite();
                    if (blockstate.isAir()) {
                        blockstate = Blocks.RESIN_CLUMP.defaultBlockState();
                    } else if (blockstate.is(Blocks.WATER) && blockstate.getFluidState().isSource()) {
                        blockstate = Blocks.RESIN_CLUMP.defaultBlockState().setValue(MultifaceBlock.WATERLOGGED, true);
                    }

                    if (blockstate.is(Blocks.RESIN_CLUMP) && !MultifaceBlock.hasFace(blockstate, direction1)) {
                        this.level.setBlock(blockpos, blockstate.setValue(MultifaceBlock.getFaceProperty(direction1), true), 3);
                        mutable.setValue(blockpos);
                        return BlockPos.TraversalNodeStatus.STOP;
                    }
                }

                return BlockPos.TraversalNodeStatus.ACCEPT;
            }
        });
        return Optional.ofNullable(mutable.getValue());
    }

    private void emitParticles(ServerLevel pLevel, int pCount, boolean pReverseDirection) {
        if (this.getCreakingProtector().orElse(null) instanceof Creaking creaking) {
            int i = pReverseDirection ? 16545810 : 6250335;
            RandomSource randomsource = pLevel.random;

            for (double d0 = 0.0; d0 < pCount; d0++) {
                AABB aabb = creaking.getBoundingBox();
                Vec3 vec3 = aabb.getMinPosition()
                    .add(
                        randomsource.nextDouble() * aabb.getXsize(), randomsource.nextDouble() * aabb.getYsize(), randomsource.nextDouble() * aabb.getZsize()
                    );
                Vec3 vec31 = Vec3.atLowerCornerOf(this.getBlockPos()).add(randomsource.nextDouble(), randomsource.nextDouble(), randomsource.nextDouble());
                if (pReverseDirection) {
                    Vec3 vec32 = vec3;
                    vec3 = vec31;
                    vec31 = vec32;
                }

                TrailParticleOption trailparticleoption = new TrailParticleOption(vec31, i, randomsource.nextInt(40) + 10);
                pLevel.sendParticles(trailparticleoption, true, true, vec3.x, vec3.y, vec3.z, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos p_395687_, BlockState p_393763_) {
        this.removeProtector(null);
    }

    public void removeProtector(@Nullable DamageSource pDamageSource) {
        if (this.getCreakingProtector().orElse(null) instanceof Creaking creaking) {
            if (pDamageSource == null) {
                creaking.tearDown();
            } else {
                creaking.creakingDeathEffects(pDamageSource);
                creaking.setTearingDown();
                creaking.setHealth(0.0F);
            }

            this.clearCreakingInfo();
        }
    }

    public boolean isProtector(Creaking pCreaking) {
        return this.getCreakingProtector().map(p_375974_ -> p_375974_ == pCreaking).orElse(false);
    }

    public int getAnalogOutputSignal() {
        return this.outputSignal;
    }

    public int computeAnalogOutputSignal() {
        if (this.creakingInfo != null && !this.getCreakingProtector().isEmpty()) {
            double d0 = this.distanceToCreaking();
            double d1 = Math.clamp(d0, 0.0, 32.0) / 32.0;
            return 15 - (int)Math.floor(d1 * 15.0);
        } else {
            return 0;
        }
    }

    @Override
    protected void loadAdditional(CompoundTag p_378014_, HolderLookup.Provider p_376954_) {
        super.loadAdditional(p_378014_, p_376954_);
        p_378014_.read("creaking", UUIDUtil.CODEC).ifPresentOrElse(this::setCreakingInfo, this::clearCreakingInfo);
    }

    @Override
    protected void saveAdditional(CompoundTag p_376888_, HolderLookup.Provider p_377263_) {
        super.saveAdditional(p_376888_, p_377263_);
        if (this.creakingInfo != null) {
            p_376888_.store("creaking", UUIDUtil.CODEC, this.creakingInfo.map(Entity::getUUID, p_377652_ -> (UUID)p_377652_));
        }
    }
}