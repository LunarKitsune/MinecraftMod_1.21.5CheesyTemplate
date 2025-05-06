package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;

public class PrepareRamNearestTarget<E extends PathfinderMob> extends Behavior<E> {
    public static final int TIME_OUT_DURATION = 160;
    private final ToIntFunction<E> getCooldownOnFail;
    private final int minRamDistance;
    private final int maxRamDistance;
    private final float walkSpeed;
    private final TargetingConditions ramTargeting;
    private final int ramPrepareTime;
    private final Function<E, SoundEvent> getPrepareRamSound;
    private Optional<Long> reachedRamPositionTimestamp = Optional.empty();
    private Optional<PrepareRamNearestTarget.RamCandidate> ramCandidate = Optional.empty();

    public PrepareRamNearestTarget(
        ToIntFunction<E> pGetCooldownOnFall,
        int pMinRamDistance,
        int pMaxRamDistance,
        float pWalkSpeed,
        TargetingConditions pRamTargeting,
        int pRamPrepareTime,
        Function<E, SoundEvent> pGetPrepareRamSound
    ) {
        super(
            ImmutableMap.of(
                MemoryModuleType.LOOK_TARGET,
                MemoryStatus.REGISTERED,
                MemoryModuleType.RAM_COOLDOWN_TICKS,
                MemoryStatus.VALUE_ABSENT,
                MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES,
                MemoryStatus.VALUE_PRESENT,
                MemoryModuleType.RAM_TARGET,
                MemoryStatus.VALUE_ABSENT
            ),
            160
        );
        this.getCooldownOnFail = pGetCooldownOnFall;
        this.minRamDistance = pMinRamDistance;
        this.maxRamDistance = pMaxRamDistance;
        this.walkSpeed = pWalkSpeed;
        this.ramTargeting = pRamTargeting;
        this.ramPrepareTime = pRamPrepareTime;
        this.getPrepareRamSound = pGetPrepareRamSound;
    }

    protected void start(ServerLevel p_147736_, PathfinderMob p_147737_, long p_147738_) {
        Brain<?> brain = p_147737_.getBrain();
        brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES)
            .flatMap(p_359021_ -> p_359021_.findClosest(p_359018_ -> this.ramTargeting.test(p_147736_, p_147737_, p_359018_)))
            .ifPresent(p_147778_ -> this.chooseRamPosition(p_147737_, p_147778_));
    }

    protected void stop(ServerLevel p_147762_, E p_147763_, long p_147764_) {
        Brain<?> brain = p_147763_.getBrain();
        if (!brain.hasMemoryValue(MemoryModuleType.RAM_TARGET)) {
            p_147762_.broadcastEntityEvent(p_147763_, (byte)59);
            brain.setMemory(MemoryModuleType.RAM_COOLDOWN_TICKS, this.getCooldownOnFail.applyAsInt(p_147763_));
        }
    }

    protected boolean canStillUse(ServerLevel p_147773_, PathfinderMob p_147774_, long p_147775_) {
        return this.ramCandidate.isPresent() && this.ramCandidate.get().getTarget().isAlive();
    }

    protected void tick(ServerLevel p_147784_, E p_147785_, long p_147786_) {
        if (!this.ramCandidate.isEmpty()) {
            p_147785_.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(this.ramCandidate.get().getStartPosition(), this.walkSpeed, 0));
            p_147785_.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(this.ramCandidate.get().getTarget(), true));
            boolean flag = !this.ramCandidate.get().getTarget().blockPosition().equals(this.ramCandidate.get().getTargetPosition());
            if (flag) {
                p_147784_.broadcastEntityEvent(p_147785_, (byte)59);
                p_147785_.getNavigation().stop();
                this.chooseRamPosition(p_147785_, this.ramCandidate.get().target);
            } else {
                BlockPos blockpos = p_147785_.blockPosition();
                if (blockpos.equals(this.ramCandidate.get().getStartPosition())) {
                    p_147784_.broadcastEntityEvent(p_147785_, (byte)58);
                    if (this.reachedRamPositionTimestamp.isEmpty()) {
                        this.reachedRamPositionTimestamp = Optional.of(p_147786_);
                    }

                    if (p_147786_ - this.reachedRamPositionTimestamp.get() >= this.ramPrepareTime) {
                        p_147785_.getBrain().setMemory(MemoryModuleType.RAM_TARGET, this.getEdgeOfBlock(blockpos, this.ramCandidate.get().getTargetPosition()));
                        p_147784_.playSound(null, p_147785_, this.getPrepareRamSound.apply(p_147785_), SoundSource.NEUTRAL, 1.0F, p_147785_.getVoicePitch());
                        this.ramCandidate = Optional.empty();
                    }
                }
            }
        }
    }

    private Vec3 getEdgeOfBlock(BlockPos pPos, BlockPos pOther) {
        double d0 = 0.5;
        double d1 = 0.5 * Mth.sign(pOther.getX() - pPos.getX());
        double d2 = 0.5 * Mth.sign(pOther.getZ() - pPos.getZ());
        return Vec3.atBottomCenterOf(pOther).add(d1, 0.0, d2);
    }

    private Optional<BlockPos> calculateRammingStartPosition(PathfinderMob pPathfinder, LivingEntity pEntity) {
        BlockPos blockpos = pEntity.blockPosition();
        if (!this.isWalkableBlock(pPathfinder, blockpos)) {
            return Optional.empty();
        } else {
            List<BlockPos> list = Lists.newArrayList();
            BlockPos.MutableBlockPos blockpos$mutableblockpos = blockpos.mutable();

            for (Direction direction : Direction.Plane.HORIZONTAL) {
                blockpos$mutableblockpos.set(blockpos);

                for (int i = 0; i < this.maxRamDistance; i++) {
                    if (!this.isWalkableBlock(pPathfinder, blockpos$mutableblockpos.move(direction))) {
                        blockpos$mutableblockpos.move(direction.getOpposite());
                        break;
                    }
                }

                if (blockpos$mutableblockpos.distManhattan(blockpos) >= this.minRamDistance) {
                    list.add(blockpos$mutableblockpos.immutable());
                }
            }

            PathNavigation pathnavigation = pPathfinder.getNavigation();
            return list.stream().sorted(Comparator.comparingDouble(pPathfinder.blockPosition()::distSqr)).filter(p_147753_ -> {
                Path path = pathnavigation.createPath(p_147753_, 0);
                return path != null && path.canReach();
            }).findFirst();
        }
    }

    private boolean isWalkableBlock(PathfinderMob pPathfinder, BlockPos pPos) {
        return pPathfinder.getNavigation().isStableDestination(pPos) && pPathfinder.getPathfindingMalus(WalkNodeEvaluator.getPathTypeStatic(pPathfinder, pPos)) == 0.0F;
    }

    private void chooseRamPosition(PathfinderMob pPathfinder, LivingEntity pEntity) {
        this.reachedRamPositionTimestamp = Optional.empty();
        this.ramCandidate = this.calculateRammingStartPosition(pPathfinder, pEntity)
            .map(p_375031_ -> new PrepareRamNearestTarget.RamCandidate(p_375031_, pEntity.blockPosition(), pEntity));
    }

    public static class RamCandidate {
        private final BlockPos startPosition;
        private final BlockPos targetPosition;
        final LivingEntity target;

        public RamCandidate(BlockPos pStartPosition, BlockPos pTargetPosition, LivingEntity pTarget) {
            this.startPosition = pStartPosition;
            this.targetPosition = pTargetPosition;
            this.target = pTarget;
        }

        public BlockPos getStartPosition() {
            return this.startPosition;
        }

        public BlockPos getTargetPosition() {
            return this.targetPosition;
        }

        public LivingEntity getTarget() {
            return this.target;
        }
    }
}