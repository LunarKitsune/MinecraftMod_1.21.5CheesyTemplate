package net.minecraft.server.level;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.Long2ByteMap;
import it.unimi.dsi.fastutil.longs.Long2ByteMaps;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntMaps;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongConsumer;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.Long2ByteMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import javax.annotation.Nullable;
import net.minecraft.core.SectionPos;
import net.minecraft.util.TriState;
import net.minecraft.util.thread.TaskScheduler;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.TicketStorage;
import net.minecraft.world.level.chunk.LevelChunk;
import org.slf4j.Logger;

public abstract class DistanceManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    static final int PLAYER_TICKET_LEVEL = ChunkLevel.byStatus(FullChunkStatus.ENTITY_TICKING);
    final Long2ObjectMap<ObjectSet<ServerPlayer>> playersPerChunk = new Long2ObjectOpenHashMap<>();
    private final LoadingChunkTracker loadingChunkTracker;
    private final SimulationChunkTracker simulationChunkTracker;
    final TicketStorage ticketStorage;
    private final DistanceManager.FixedPlayerDistanceChunkTracker naturalSpawnChunkCounter = new DistanceManager.FixedPlayerDistanceChunkTracker(8);
    private final DistanceManager.PlayerTicketTracker playerTicketManager = new DistanceManager.PlayerTicketTracker(32);
    protected final Set<ChunkHolder> chunksToUpdateFutures = new ReferenceOpenHashSet<>();
    final ThrottlingChunkTaskDispatcher ticketDispatcher;
    final LongSet ticketsToRelease = new LongOpenHashSet();
    final Executor mainThreadExecutor;
    private int simulationDistance = 10;

    protected DistanceManager(TicketStorage pTicketStorage, Executor pDispatcher, Executor pMainThreadExecutor) {
        this.ticketStorage = pTicketStorage;
        this.loadingChunkTracker = new LoadingChunkTracker(this, pTicketStorage);
        this.simulationChunkTracker = new SimulationChunkTracker(pTicketStorage);
        TaskScheduler<Runnable> taskscheduler = TaskScheduler.wrapExecutor("player ticket throttler", pMainThreadExecutor);
        this.ticketDispatcher = new ThrottlingChunkTaskDispatcher(taskscheduler, pDispatcher, 4);
        this.mainThreadExecutor = pMainThreadExecutor;
    }

    protected abstract boolean isChunkToRemove(long pChunkPos);

    @Nullable
    protected abstract ChunkHolder getChunk(long pChunkPos);

    @Nullable
    protected abstract ChunkHolder updateChunkScheduling(long pChunkPos, int pNewLevel, @Nullable ChunkHolder pHolder, int pOldLevel);

    public boolean runAllUpdates(ChunkMap pChunkMap) {
        this.naturalSpawnChunkCounter.runAllUpdates();
        this.simulationChunkTracker.runAllUpdates();
        this.playerTicketManager.runAllUpdates();
        int i = Integer.MAX_VALUE - this.loadingChunkTracker.runDistanceUpdates(Integer.MAX_VALUE);
        boolean flag = i != 0;
        if (flag) {
        }

        if (!this.chunksToUpdateFutures.isEmpty()) {
            for (ChunkHolder chunkholder1 : this.chunksToUpdateFutures) {
                chunkholder1.updateHighestAllowedStatus(pChunkMap);
            }

            for (ChunkHolder chunkholder2 : this.chunksToUpdateFutures) {
                chunkholder2.updateFutures(pChunkMap, this.mainThreadExecutor);
            }

            this.chunksToUpdateFutures.clear();
            return true;
        } else {
            if (!this.ticketsToRelease.isEmpty()) {
                LongIterator longiterator = this.ticketsToRelease.iterator();

                while (longiterator.hasNext()) {
                    long j = longiterator.nextLong();
                    if (this.ticketStorage.getTickets(j).stream().anyMatch(p_390137_ -> p_390137_.getType() == TicketType.PLAYER_LOADING)) {
                        ChunkHolder chunkholder = pChunkMap.getUpdatingChunkIfPresent(j);
                        if (chunkholder == null) {
                            throw new IllegalStateException();
                        }

                        CompletableFuture<ChunkResult<LevelChunk>> completablefuture = chunkholder.getEntityTickingChunkFuture();
                        completablefuture.thenAccept(p_336030_ -> this.mainThreadExecutor.execute(() -> this.ticketDispatcher.release(j, () -> {}, false)));
                    }
                }

                this.ticketsToRelease.clear();
            }

            return flag;
        }
    }

    public void addPlayer(SectionPos pSectionPos, ServerPlayer pPlayer) {
        ChunkPos chunkpos = pSectionPos.chunk();
        long i = chunkpos.toLong();
        this.playersPerChunk.computeIfAbsent(i, p_183921_ -> new ObjectOpenHashSet<>()).add(pPlayer);
        this.naturalSpawnChunkCounter.update(i, 0, true);
        this.playerTicketManager.update(i, 0, true);
        this.ticketStorage.addTicket(new Ticket(TicketType.PLAYER_SIMULATION, this.getPlayerTicketLevel()), chunkpos);
    }

    public void removePlayer(SectionPos pSectionPos, ServerPlayer pPlayer) {
        ChunkPos chunkpos = pSectionPos.chunk();
        long i = chunkpos.toLong();
        ObjectSet<ServerPlayer> objectset = this.playersPerChunk.get(i);
        objectset.remove(pPlayer);
        if (objectset.isEmpty()) {
            this.playersPerChunk.remove(i);
            this.naturalSpawnChunkCounter.update(i, Integer.MAX_VALUE, false);
            this.playerTicketManager.update(i, Integer.MAX_VALUE, false);
            this.ticketStorage.removeTicket(new Ticket(TicketType.PLAYER_SIMULATION, this.getPlayerTicketLevel()), chunkpos);
        }
    }

    private int getPlayerTicketLevel() {
        return Math.max(0, ChunkLevel.byStatus(FullChunkStatus.ENTITY_TICKING) - this.simulationDistance);
    }

    public boolean inEntityTickingRange(long pChunkPos) {
        return ChunkLevel.isEntityTicking(this.simulationChunkTracker.getLevel(pChunkPos));
    }

    public boolean inBlockTickingRange(long pChunkPos) {
        return ChunkLevel.isBlockTicking(this.simulationChunkTracker.getLevel(pChunkPos));
    }

    public int getChunkLevel(long pChunkPos, boolean pSimulate) {
        return pSimulate ? this.simulationChunkTracker.getLevel(pChunkPos) : this.loadingChunkTracker.getLevel(pChunkPos);
    }

    protected void updatePlayerTickets(int pViewDistance) {
        this.playerTicketManager.updateViewDistance(pViewDistance);
    }

    public void updateSimulationDistance(int pSimulationDistance) {
        if (pSimulationDistance != this.simulationDistance) {
            this.simulationDistance = pSimulationDistance;
            this.ticketStorage.replaceTicketLevelOfType(this.getPlayerTicketLevel(), TicketType.PLAYER_SIMULATION);
        }
    }

    public int getNaturalSpawnChunkCount() {
        this.naturalSpawnChunkCounter.runAllUpdates();
        return this.naturalSpawnChunkCounter.chunks.size();
    }

    public TriState hasPlayersNearby(long pChunkPos) {
        this.naturalSpawnChunkCounter.runAllUpdates();
        int i = this.naturalSpawnChunkCounter.getLevel(pChunkPos);
        if (i <= NaturalSpawner.INSCRIBED_SQUARE_SPAWN_DISTANCE_CHUNK) {
            return TriState.TRUE;
        } else {
            return i > 8 ? TriState.FALSE : TriState.DEFAULT;
        }
    }

    public void forEachEntityTickingChunk(LongConsumer pAction) {
        for (Entry entry : Long2ByteMaps.fastIterable(this.simulationChunkTracker.chunks)) {
            byte b0 = entry.getByteValue();
            long i = entry.getLongKey();
            if (ChunkLevel.isEntityTicking(b0)) {
                pAction.accept(i);
            }
        }
    }

    public LongIterator getSpawnCandidateChunks() {
        this.naturalSpawnChunkCounter.runAllUpdates();
        return this.naturalSpawnChunkCounter.chunks.keySet().iterator();
    }

    public String getDebugStatus() {
        return this.ticketDispatcher.getDebugStatus();
    }

    public boolean shouldForceTicks(long chunkPos) {
        return this.ticketStorage.getForceLoadedChunks().contains(chunkPos);
    }

    public boolean hasTickets() {
        return this.ticketStorage.hasTickets();
    }

    class FixedPlayerDistanceChunkTracker extends ChunkTracker {
        protected final Long2ByteMap chunks = new Long2ByteOpenHashMap();
        protected final int maxDistance;

        protected FixedPlayerDistanceChunkTracker(final int pMaxDistance) {
            super(pMaxDistance + 2, 16, 256);
            this.maxDistance = pMaxDistance;
            this.chunks.defaultReturnValue((byte)(pMaxDistance + 2));
        }

        @Override
        protected int getLevel(long pSectionPos) {
            return this.chunks.get(pSectionPos);
        }

        @Override
        protected void setLevel(long pSectionPos, int pLevel) {
            byte b0;
            if (pLevel > this.maxDistance) {
                b0 = this.chunks.remove(pSectionPos);
            } else {
                b0 = this.chunks.put(pSectionPos, (byte)pLevel);
            }

            this.onLevelChange(pSectionPos, b0, pLevel);
        }

        protected void onLevelChange(long pChunkPos, int pOldLevel, int pNewLevel) {
        }

        @Override
        protected int getLevelFromSource(long pPos) {
            return this.havePlayer(pPos) ? 0 : Integer.MAX_VALUE;
        }

        private boolean havePlayer(long pChunkPos) {
            ObjectSet<ServerPlayer> objectset = DistanceManager.this.playersPerChunk.get(pChunkPos);
            return objectset != null && !objectset.isEmpty();
        }

        public void runAllUpdates() {
            this.runUpdates(Integer.MAX_VALUE);
        }
    }

    class PlayerTicketTracker extends DistanceManager.FixedPlayerDistanceChunkTracker {
        private int viewDistance;
        private final Long2IntMap queueLevels = Long2IntMaps.synchronize(new Long2IntOpenHashMap());
        private final LongSet toUpdate = new LongOpenHashSet();

        protected PlayerTicketTracker(final int p_140910_) {
            super(p_140910_);
            this.viewDistance = 0;
            this.queueLevels.defaultReturnValue(p_140910_ + 2);
        }

        @Override
        protected void onLevelChange(long pChunkPos, int pOldLevel, int pNewLevel) {
            this.toUpdate.add(pChunkPos);
        }

        public void updateViewDistance(int pViewDistance) {
            for (Entry entry : this.chunks.long2ByteEntrySet()) {
                byte b0 = entry.getByteValue();
                long i = entry.getLongKey();
                this.onLevelChange(i, b0, this.haveTicketFor(b0), b0 <= pViewDistance);
            }

            this.viewDistance = pViewDistance;
        }

        private void onLevelChange(long pChunkPos, int pLevel, boolean pHadTicket, boolean pHasTicket) {
            if (pHadTicket != pHasTicket) {
                Ticket ticket = new Ticket(TicketType.PLAYER_LOADING, DistanceManager.PLAYER_TICKET_LEVEL);
                if (pHasTicket) {
                    DistanceManager.this.ticketDispatcher.submit(() -> DistanceManager.this.mainThreadExecutor.execute(() -> {
                        if (this.haveTicketFor(this.getLevel(pChunkPos))) {
                            DistanceManager.this.ticketStorage.addTicket(pChunkPos, ticket);
                            DistanceManager.this.ticketsToRelease.add(pChunkPos);
                        } else {
                            DistanceManager.this.ticketDispatcher.release(pChunkPos, () -> {}, false);
                        }
                    }), pChunkPos, () -> pLevel);
                } else {
                    DistanceManager.this.ticketDispatcher
                        .release(
                            pChunkPos, () -> DistanceManager.this.mainThreadExecutor.execute(() -> DistanceManager.this.ticketStorage.removeTicket(pChunkPos, ticket)), true
                        );
                }
            }
        }

        @Override
        public void runAllUpdates() {
            super.runAllUpdates();
            if (!this.toUpdate.isEmpty()) {
                LongIterator longiterator = this.toUpdate.iterator();

                while (longiterator.hasNext()) {
                    long i = longiterator.nextLong();
                    int j = this.queueLevels.get(i);
                    int k = this.getLevel(i);
                    if (j != k) {
                        DistanceManager.this.ticketDispatcher.onLevelChange(new ChunkPos(i), () -> this.queueLevels.get(i), k, p_140928_ -> {
                            if (p_140928_ >= this.queueLevels.defaultReturnValue()) {
                                this.queueLevels.remove(i);
                            } else {
                                this.queueLevels.put(i, p_140928_);
                            }
                        });
                        this.onLevelChange(i, k, this.haveTicketFor(j), this.haveTicketFor(k));
                    }
                }

                this.toUpdate.clear();
            }
        }

        private boolean haveTicketFor(int pLevel) {
            return pLevel <= this.viewDistance;
        }
    }
}
