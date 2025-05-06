package net.minecraft.world.entity.ai.behavior;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryAccessor;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.pathfinder.Path;
import org.apache.commons.lang3.mutable.MutableLong;

public class AcquirePoi {
    public static final int SCAN_RANGE = 48;

    public static BehaviorControl<PathfinderMob> create(
        Predicate<Holder<PoiType>> pAcquirablePois,
        MemoryModuleType<GlobalPos> pAcquiringMemory,
        boolean pOnlyIfAdult,
        Optional<Byte> pEntityEventId,
        BiPredicate<ServerLevel, BlockPos> pPredicate
    ) {
        return create(pAcquirablePois, pAcquiringMemory, pAcquiringMemory, pOnlyIfAdult, pEntityEventId, pPredicate);
    }

    public static BehaviorControl<PathfinderMob> create(
        Predicate<Holder<PoiType>> pAcquirablePois, MemoryModuleType<GlobalPos> pAcquiringMemory, boolean pOnlyIfAdult, Optional<Byte> pEntityEventId
    ) {
        return create(pAcquirablePois, pAcquiringMemory, pAcquiringMemory, pOnlyIfAdult, pEntityEventId, (p_374959_, p_374960_) -> true);
    }

    public static BehaviorControl<PathfinderMob> create(
        Predicate<Holder<PoiType>> pAcquirablePois,
        MemoryModuleType<GlobalPos> pExistingAbsentMemory,
        MemoryModuleType<GlobalPos> pAcquiringMemory,
        boolean pOnlyIfAdult,
        Optional<Byte> pEntityEventId,
        BiPredicate<ServerLevel, BlockPos> pPredicate
    ) {
        int i = 5;
        int j = 20;
        MutableLong mutablelong = new MutableLong(0L);
        Long2ObjectMap<AcquirePoi.JitteredLinearRetry> long2objectmap = new Long2ObjectOpenHashMap<>();
        OneShot<PathfinderMob> oneshot = BehaviorBuilder.create(
            p_374955_ -> p_374955_.group(p_374955_.absent(pAcquiringMemory))
                .apply(
                    p_374955_,
                    p_374967_ -> (p_374945_, p_374946_, p_374947_) -> {
                        if (pOnlyIfAdult && p_374946_.isBaby()) {
                            return false;
                        } else if (mutablelong.getValue() == 0L) {
                            mutablelong.setValue(p_374945_.getGameTime() + p_374945_.random.nextInt(20));
                            return false;
                        } else if (p_374945_.getGameTime() < mutablelong.getValue()) {
                            return false;
                        } else {
                            mutablelong.setValue(p_374947_ + 20L + p_374945_.getRandom().nextInt(20));
                            PoiManager poimanager = p_374945_.getPoiManager();
                            long2objectmap.long2ObjectEntrySet().removeIf(p_22338_ -> !p_22338_.getValue().isStillValid(p_374947_));
                            Predicate<BlockPos> predicate = p_258266_ -> {
                                AcquirePoi.JitteredLinearRetry acquirepoi$jitteredlinearretry = long2objectmap.get(p_258266_.asLong());
                                if (acquirepoi$jitteredlinearretry == null) {
                                    return true;
                                } else if (!acquirepoi$jitteredlinearretry.shouldRetry(p_374947_)) {
                                    return false;
                                } else {
                                    acquirepoi$jitteredlinearretry.markAttempt(p_374947_);
                                    return true;
                                }
                            };
                            Set<Pair<Holder<PoiType>, BlockPos>> set = poimanager.findAllClosestFirstWithType(
                                    pAcquirablePois, predicate, p_374946_.blockPosition(), 48, PoiManager.Occupancy.HAS_SPACE
                                )
                                .limit(5L)
                                .filter(p_374958_ -> pPredicate.test(p_374945_, p_374958_.getSecond()))
                                .collect(Collectors.toSet());
                            Path path = findPathToPois(p_374946_, set);
                            if (path != null && path.canReach()) {
                                BlockPos blockpos = path.getTarget();
                                poimanager.getType(blockpos).ifPresent(p_390542_ -> {
                                    poimanager.take(pAcquirablePois, (p_217108_, p_217109_) -> p_217109_.equals(blockpos), blockpos, 1);
                                    p_374967_.set(GlobalPos.of(p_374945_.dimension(), blockpos));
                                    pEntityEventId.ifPresent(p_147369_ -> p_374945_.broadcastEntityEvent(p_374946_, p_147369_));
                                    long2objectmap.clear();
                                    DebugPackets.sendPoiTicketCountPacket(p_374945_, blockpos);
                                });
                            } else {
                                for (Pair<Holder<PoiType>, BlockPos> pair : set) {
                                    long2objectmap.computeIfAbsent(
                                        pair.getSecond().asLong(), p_264881_ -> new AcquirePoi.JitteredLinearRetry(p_374945_.random, p_374947_)
                                    );
                                }
                            }

                            return true;
                        }
                    }
                )
        );
        return pAcquiringMemory == pExistingAbsentMemory
            ? oneshot
            : BehaviorBuilder.create(p_258269_ -> p_258269_.group(p_258269_.absent(pExistingAbsentMemory)).apply(p_258269_, p_258302_ -> oneshot));
    }

    @Nullable
    public static Path findPathToPois(Mob pMob, Set<Pair<Holder<PoiType>, BlockPos>> pPoiPositions) {
        if (pPoiPositions.isEmpty()) {
            return null;
        } else {
            Set<BlockPos> set = new HashSet<>();
            int i = 1;

            for (Pair<Holder<PoiType>, BlockPos> pair : pPoiPositions) {
                i = Math.max(i, pair.getFirst().value().validRange());
                set.add(pair.getSecond());
            }

            return pMob.getNavigation().createPath(set, i);
        }
    }

    static class JitteredLinearRetry {
        private static final int MIN_INTERVAL_INCREASE = 40;
        private static final int MAX_INTERVAL_INCREASE = 80;
        private static final int MAX_RETRY_PATHFINDING_INTERVAL = 400;
        private final RandomSource random;
        private long previousAttemptTimestamp;
        private long nextScheduledAttemptTimestamp;
        private int currentDelay;

        JitteredLinearRetry(RandomSource pRandom, long pTimestamp) {
            this.random = pRandom;
            this.markAttempt(pTimestamp);
        }

        public void markAttempt(long pTimestamp) {
            this.previousAttemptTimestamp = pTimestamp;
            int i = this.currentDelay + this.random.nextInt(40) + 40;
            this.currentDelay = Math.min(i, 400);
            this.nextScheduledAttemptTimestamp = pTimestamp + this.currentDelay;
        }

        public boolean isStillValid(long pTimestamp) {
            return pTimestamp - this.previousAttemptTimestamp < 400L;
        }

        public boolean shouldRetry(long pTimestamp) {
            return pTimestamp >= this.nextScheduledAttemptTimestamp;
        }

        @Override
        public String toString() {
            return "RetryMarker{, previousAttemptAt=" + this.previousAttemptTimestamp + ", nextScheduledAttemptAt=" + this.nextScheduledAttemptTimestamp + ", currentDelay=" + this.currentDelay + "}";
        }
    }
}