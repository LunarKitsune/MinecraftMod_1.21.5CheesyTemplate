package net.minecraft.server.level;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.datafixers.DataFixer;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.FileUtil;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.LocalMobCapCalculator;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.TicketStorage;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LightChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.storage.ChunkScanAccess;
import net.minecraft.world.level.entity.ChunkStatusUpdateListener;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.slf4j.Logger;

public class ServerChunkCache extends ChunkSource {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final DistanceManager distanceManager;
    public final ServerLevel level;
    final Thread mainThread;
    final ThreadedLevelLightEngine lightEngine;
    private final ServerChunkCache.MainThreadExecutor mainThreadProcessor;
    public final ChunkMap chunkMap;
    private final DimensionDataStorage dataStorage;
    private final TicketStorage ticketStorage;
    private long lastInhabitedUpdate;
    private boolean spawnEnemies = true;
    private boolean spawnFriendlies = true;
    private static final int CACHE_SIZE = 4;
    private final long[] lastChunkPos = new long[4];
    private final ChunkStatus[] lastChunkStatus = new ChunkStatus[4];
    private final ChunkAccess[] lastChunk = new ChunkAccess[4];
    private final List<LevelChunk> spawningChunks = new ObjectArrayList<>();
    private final Set<ChunkHolder> chunkHoldersToBroadcast = new ReferenceOpenHashSet<>();
    @Nullable
    @VisibleForDebug
    private NaturalSpawner.SpawnState lastSpawnState;

    public ServerChunkCache(
        ServerLevel pLevel,
        LevelStorageSource.LevelStorageAccess pLevelStorageAccess,
        DataFixer pFixerUpper,
        StructureTemplateManager pStructureManager,
        Executor pDispatcher,
        ChunkGenerator pGenerator,
        int pViewDistance,
        int pSimulationDistance,
        boolean pSync,
        ChunkProgressListener pProgressListener,
        ChunkStatusUpdateListener pChunkStatusListener,
        Supplier<DimensionDataStorage> pOverworldDataStorage
    ) {
        this.level = pLevel;
        this.mainThreadProcessor = new ServerChunkCache.MainThreadExecutor(pLevel);
        this.mainThread = Thread.currentThread();
        Path path = pLevelStorageAccess.getDimensionPath(pLevel.dimension()).resolve("data");

        try {
            FileUtil.createDirectoriesSafe(path);
        } catch (IOException ioexception) {
            LOGGER.error("Failed to create dimension data storage directory", (Throwable)ioexception);
        }

        this.dataStorage = new DimensionDataStorage(new SavedData.Context(pLevel), path, pFixerUpper, pLevel.registryAccess());
        this.ticketStorage = this.dataStorage.computeIfAbsent(TicketStorage.TYPE);
        this.chunkMap = new ChunkMap(
            pLevel,
            pLevelStorageAccess,
            pFixerUpper,
            pStructureManager,
            pDispatcher,
            this.mainThreadProcessor,
            this,
            pGenerator,
            pProgressListener,
            pChunkStatusListener,
            pOverworldDataStorage,
            this.ticketStorage,
            pViewDistance,
            pSync
        );
        this.lightEngine = this.chunkMap.getLightEngine();
        this.distanceManager = this.chunkMap.getDistanceManager();
        this.distanceManager.updateSimulationDistance(pSimulationDistance);
        this.clearCache();
    }

    public ThreadedLevelLightEngine getLightEngine() {
        return this.lightEngine;
    }

    @Nullable
    private ChunkHolder getVisibleChunkIfPresent(long pChunkPos) {
        return this.chunkMap.getVisibleChunkIfPresent(pChunkPos);
    }

    public int getTickingGenerated() {
        return this.chunkMap.getTickingGenerated();
    }

    private void storeInCache(long pChunkPos, @Nullable ChunkAccess pChunk, ChunkStatus pChunkStatus) {
        for (int i = 3; i > 0; i--) {
            this.lastChunkPos[i] = this.lastChunkPos[i - 1];
            this.lastChunkStatus[i] = this.lastChunkStatus[i - 1];
            this.lastChunk[i] = this.lastChunk[i - 1];
        }

        this.lastChunkPos[0] = pChunkPos;
        this.lastChunkStatus[0] = pChunkStatus;
        this.lastChunk[0] = pChunk;
    }

    @Nullable
    @Override
    public ChunkAccess getChunk(int p_8360_, int p_8361_, ChunkStatus p_334940_, boolean p_8363_) {
        if (Thread.currentThread() != this.mainThread) {
            return CompletableFuture.<ChunkAccess>supplyAsync(() -> this.getChunk(p_8360_, p_8361_, p_334940_, p_8363_), this.mainThreadProcessor).join();
        } else {
            ProfilerFiller profilerfiller = Profiler.get();
            profilerfiller.incrementCounter("getChunk");
            long i = ChunkPos.asLong(p_8360_, p_8361_);

            for (int j = 0; j < 4; j++) {
                if (i == this.lastChunkPos[j] && p_334940_ == this.lastChunkStatus[j]) {
                    ChunkAccess chunkaccess = this.lastChunk[j];
                    if (chunkaccess != null || !p_8363_) {
                        return chunkaccess;
                    }
                }
            }

            profilerfiller.incrementCounter("getChunkCacheMiss");
            CompletableFuture<ChunkResult<ChunkAccess>> completablefuture = this.getChunkFutureMainThread(p_8360_, p_8361_, p_334940_, p_8363_);
            this.mainThreadProcessor.managedBlock(completablefuture::isDone);
            ChunkResult<ChunkAccess> chunkresult = completablefuture.join();
            ChunkAccess chunkaccess1 = chunkresult.orElse(null);
            if (chunkaccess1 == null && p_8363_) {
                throw (IllegalStateException)Util.pauseInIde(new IllegalStateException("Chunk not there when requested: " + chunkresult.getError()));
            } else {
                this.storeInCache(i, chunkaccess1, p_334940_);
                return chunkaccess1;
            }
        }
    }

    @Nullable
    @Override
    public LevelChunk getChunkNow(int pChunkX, int pChunkZ) {
        if (Thread.currentThread() != this.mainThread) {
            return null;
        } else {
            Profiler.get().incrementCounter("getChunkNow");
            long i = ChunkPos.asLong(pChunkX, pChunkZ);

            for (int j = 0; j < 4; j++) {
                if (i == this.lastChunkPos[j] && this.lastChunkStatus[j] == ChunkStatus.FULL) {
                    ChunkAccess chunkaccess = this.lastChunk[j];
                    return chunkaccess instanceof LevelChunk ? (LevelChunk)chunkaccess : null;
                }
            }

            ChunkHolder chunkholder = this.getVisibleChunkIfPresent(i);
            if (chunkholder == null) {
                return null;
            } else {
                // Forge: If the requested chunk is loading, bypass the future chain to prevent a deadlock.
                if (chunkholder.currentlyLoading != null) {
                    return chunkholder.currentlyLoading;
                }
                ChunkAccess chunkaccess1 = chunkholder.getChunkIfPresent(ChunkStatus.FULL);
                if (chunkaccess1 != null) {
                    this.storeInCache(i, chunkaccess1, ChunkStatus.FULL);
                    if (chunkaccess1 instanceof LevelChunk) {
                        return (LevelChunk)chunkaccess1;
                    }
                }

                return null;
            }
        }
    }

    private void clearCache() {
        Arrays.fill(this.lastChunkPos, ChunkPos.INVALID_CHUNK_POS);
        Arrays.fill(this.lastChunkStatus, null);
        Arrays.fill(this.lastChunk, null);
    }

    public CompletableFuture<ChunkResult<ChunkAccess>> getChunkFuture(int pX, int pZ, ChunkStatus pChunkStatus, boolean pRequireChunk) {
        boolean flag = Thread.currentThread() == this.mainThread;
        CompletableFuture<ChunkResult<ChunkAccess>> completablefuture;
        if (flag) {
            completablefuture = this.getChunkFutureMainThread(pX, pZ, pChunkStatus, pRequireChunk);
            this.mainThreadProcessor.managedBlock(completablefuture::isDone);
        } else {
            completablefuture = CompletableFuture.<CompletableFuture<ChunkResult<ChunkAccess>>>supplyAsync(
                    () -> this.getChunkFutureMainThread(pX, pZ, pChunkStatus, pRequireChunk), this.mainThreadProcessor
                )
                .thenCompose(p_333930_ -> (CompletionStage<ChunkResult<ChunkAccess>>)p_333930_);
        }

        return completablefuture;
    }

    private CompletableFuture<ChunkResult<ChunkAccess>> getChunkFutureMainThread(int pX, int pZ, ChunkStatus pChunkStatus, boolean pRequireChunk) {
        ChunkPos chunkpos = new ChunkPos(pX, pZ);
        long i = chunkpos.toLong();
        int j = ChunkLevel.byStatus(pChunkStatus);
        ChunkHolder chunkholder = this.getVisibleChunkIfPresent(i);
        if (pRequireChunk) {
            this.addTicket(new Ticket(TicketType.UNKNOWN, j), chunkpos);
            if (this.chunkAbsent(chunkholder, j)) {
                ProfilerFiller profilerfiller = Profiler.get();
                profilerfiller.push("chunkLoad");
                this.runDistanceManagerUpdates();
                chunkholder = this.getVisibleChunkIfPresent(i);
                profilerfiller.pop();
                if (this.chunkAbsent(chunkholder, j)) {
                    throw (IllegalStateException)Util.pauseInIde(new IllegalStateException("No chunk holder after ticket has been added"));
                }
            }
        }

        return this.chunkAbsent(chunkholder, j) ? GenerationChunkHolder.UNLOADED_CHUNK_FUTURE : chunkholder.scheduleChunkGenerationTask(pChunkStatus, this.chunkMap);
    }

    private boolean chunkAbsent(@Nullable ChunkHolder pChunkHolder, int pStatus) {
        return pChunkHolder == null || pChunkHolder.getTicketLevel() > pStatus;
    }

    @Override
    public boolean hasChunk(int pX, int pZ) {
        ChunkHolder chunkholder = this.getVisibleChunkIfPresent(new ChunkPos(pX, pZ).toLong());
        int i = ChunkLevel.byStatus(ChunkStatus.FULL);
        return !this.chunkAbsent(chunkholder, i);
    }

    @Nullable
    @Override
    public LightChunk getChunkForLighting(int p_8454_, int p_8455_) {
        long i = ChunkPos.asLong(p_8454_, p_8455_);
        ChunkHolder chunkholder = this.getVisibleChunkIfPresent(i);
        return chunkholder == null ? null : chunkholder.getChunkIfPresentUnchecked(ChunkStatus.INITIALIZE_LIGHT.getParent());
    }

    public Level getLevel() {
        return this.level;
    }

    public boolean pollTask() {
        return this.mainThreadProcessor.pollTask();
    }

    boolean runDistanceManagerUpdates() {
        boolean flag = this.distanceManager.runAllUpdates(this.chunkMap);
        boolean flag1 = this.chunkMap.promoteChunkMap();
        this.chunkMap.runGenerationTasks();
        if (!flag && !flag1) {
            return false;
        } else {
            this.clearCache();
            return true;
        }
    }

    public boolean isPositionTicking(long pChunkPos) {
        if (!this.level.shouldTickBlocksAt(pChunkPos)) {
            return false;
        } else {
            ChunkHolder chunkholder = this.getVisibleChunkIfPresent(pChunkPos);
            return chunkholder == null ? false : chunkholder.getTickingChunkFuture().getNow(ChunkHolder.UNLOADED_LEVEL_CHUNK).isSuccess();
        }
    }

    public void save(boolean pFlush) {
        this.runDistanceManagerUpdates();
        this.chunkMap.saveAllChunks(pFlush);
    }

    @Override
    public void close() throws IOException {
        this.save(true);
        this.dataStorage.close();
        this.lightEngine.close();
        this.chunkMap.close();
    }

    @Override
    public void tick(BooleanSupplier p_201913_, boolean p_201914_) {
        ProfilerFiller profilerfiller = Profiler.get();
        profilerfiller.push("purge");
        if (this.level.tickRateManager().runsNormally() || !p_201914_) {
            this.ticketStorage.purgeStaleTickets();
        }

        this.runDistanceManagerUpdates();
        profilerfiller.popPush("chunks");
        if (p_201914_) {
            this.tickChunks();
            this.chunkMap.tick();
        }

        profilerfiller.popPush("unload");
        this.chunkMap.tick(p_201913_);
        profilerfiller.pop();
        this.clearCache();
    }

    private void tickChunks() {
        long i = this.level.getGameTime();
        long j = i - this.lastInhabitedUpdate;
        this.lastInhabitedUpdate = i;
        if (!this.level.isDebug()) {
            ProfilerFiller profilerfiller = Profiler.get();
            profilerfiller.push("pollingChunks");
            if (this.level.tickRateManager().runsNormally()) {
                profilerfiller.push("tickingChunks");
                this.tickChunks(profilerfiller, j);
                profilerfiller.pop();
            }

            this.broadcastChangedChunks(profilerfiller);
            profilerfiller.pop();
        }
    }

    private void broadcastChangedChunks(ProfilerFiller pProfiler) {
        pProfiler.push("broadcast");

        for (ChunkHolder chunkholder : this.chunkHoldersToBroadcast) {
            LevelChunk levelchunk = chunkholder.getTickingChunk();
            if (levelchunk != null) {
                chunkholder.broadcastChanges(levelchunk);
            }
        }

        this.chunkHoldersToBroadcast.clear();
        pProfiler.pop();
    }

    private void tickChunks(ProfilerFiller pProfiler, long pTimeInhabited) {
        pProfiler.popPush("naturalSpawnCount");
        int i = this.distanceManager.getNaturalSpawnChunkCount();
        NaturalSpawner.SpawnState naturalspawner$spawnstate = NaturalSpawner.createState(
            i, this.level.getAllEntities(), this::getFullChunk, new LocalMobCapCalculator(this.chunkMap)
        );
        this.lastSpawnState = naturalspawner$spawnstate;
        pProfiler.popPush("spawnAndTick");
        boolean flag = this.level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING);
        int j = this.level.getGameRules().getInt(GameRules.RULE_RANDOMTICKING);
        List<MobCategory> list;
        if (flag && (this.spawnEnemies || this.spawnFriendlies)) {
            boolean flag1 = this.level.getLevelData().getGameTime() % 400L == 0L;
            list = NaturalSpawner.getFilteredSpawningCategories(naturalspawner$spawnstate, this.spawnFriendlies, this.spawnEnemies, flag1);
        } else {
            list = List.of();
        }

        List<LevelChunk> list1 = this.spawningChunks;

        try {
            pProfiler.push("filteringSpawningChunks");
            this.chunkMap.collectSpawningChunks(list1);
            pProfiler.popPush("shuffleSpawningChunks");
            Util.shuffle(list1, this.level.random);
            pProfiler.popPush("tickSpawningChunks");

            for (LevelChunk levelchunk : list1) {
                this.tickSpawningChunk(levelchunk, pTimeInhabited, list, naturalspawner$spawnstate);
            }
        } finally {
            list1.clear();
        }

        pProfiler.popPush("tickTickingChunks");
        this.chunkMap.forEachBlockTickingChunk(p_390143_ -> this.level.tickChunk(p_390143_, j));
        pProfiler.pop();
        pProfiler.popPush("customSpawners");
        if (flag) {
            this.level.tickCustomSpawners(this.spawnEnemies, this.spawnFriendlies);
        }
    }

    private void tickSpawningChunk(LevelChunk pChunk, long pTimeInhabited, List<MobCategory> pSpawnCategories, NaturalSpawner.SpawnState pSpawnState) {
        ChunkPos chunkpos = pChunk.getPos();
        pChunk.incrementInhabitedTime(pTimeInhabited);
        if (this.distanceManager.inEntityTickingRange(chunkpos.toLong())) {
            this.level.tickThunder(pChunk);
        }

        if (!pSpawnCategories.isEmpty()) {
            if (this.level.canSpawnEntitiesInChunk(chunkpos)) {
                NaturalSpawner.spawnForChunk(this.level, pChunk, pSpawnState, pSpawnCategories);
            }
        }
    }

    private void getFullChunk(long pChunkPos, Consumer<LevelChunk> pFullChunkGetter) {
        ChunkHolder chunkholder = this.getVisibleChunkIfPresent(pChunkPos);
        if (chunkholder != null) {
            chunkholder.getFullChunkFuture().getNow(ChunkHolder.UNLOADED_LEVEL_CHUNK).ifSuccess(pFullChunkGetter);
        }
    }

    @Override
    public String gatherStats() {
        return Integer.toString(this.getLoadedChunksCount());
    }

    @VisibleForTesting
    public int getPendingTasksCount() {
        return this.mainThreadProcessor.getPendingTasksCount();
    }

    public ChunkGenerator getGenerator() {
        return this.chunkMap.generator();
    }

    public ChunkGeneratorStructureState getGeneratorState() {
        return this.chunkMap.generatorState();
    }

    public RandomState randomState() {
        return this.chunkMap.randomState();
    }

    @Override
    public int getLoadedChunksCount() {
        return this.chunkMap.size();
    }

    public void blockChanged(BlockPos pPos) {
        int i = SectionPos.blockToSectionCoord(pPos.getX());
        int j = SectionPos.blockToSectionCoord(pPos.getZ());
        ChunkHolder chunkholder = this.getVisibleChunkIfPresent(ChunkPos.asLong(i, j));
        if (chunkholder != null && chunkholder.blockChanged(pPos)) {
            this.chunkHoldersToBroadcast.add(chunkholder);
        }
    }

    @Override
    public void onLightUpdate(LightLayer pType, SectionPos pPos) {
        this.mainThreadProcessor.execute(() -> {
            ChunkHolder chunkholder = this.getVisibleChunkIfPresent(pPos.chunk().toLong());
            if (chunkholder != null && chunkholder.sectionLightChanged(pType, pPos.y())) {
                this.chunkHoldersToBroadcast.add(chunkholder);
            }
        });
    }

    public void addTicket(Ticket pTicket, ChunkPos pChunkPos) {
        this.ticketStorage.addTicket(pTicket, pChunkPos);
    }

    public void addTicketWithRadius(TicketType pTicket, ChunkPos pChunkPos, int pRadius) {
        this.ticketStorage.addTicketWithRadius(pTicket, pChunkPos, pRadius);
    }

    public void removeTicketWithRadius(TicketType pTicket, ChunkPos pChunkPos, int pRadius) {
        this.ticketStorage.removeTicketWithRadius(pTicket, pChunkPos, pRadius);
    }

    @Override
    public boolean updateChunkForced(ChunkPos p_8400_, boolean p_8401_) {
        return this.ticketStorage.updateChunkForced(p_8400_, p_8401_);
    }

    @Override
    public LongSet getForceLoadedChunks() {
        return this.ticketStorage.getForceLoadedChunks();
    }

    public void move(ServerPlayer pPlayer) {
        if (!pPlayer.isRemoved()) {
            this.chunkMap.move(pPlayer);
        }
    }

    public void removeEntity(Entity pEntity) {
        this.chunkMap.removeEntity(pEntity);
    }

    public void addEntity(Entity pEntity) {
        this.chunkMap.addEntity(pEntity);
    }

    public void broadcastAndSend(Entity pEntity, Packet<?> pPacket) {
        this.chunkMap.broadcastAndSend(pEntity, pPacket);
    }

    public void broadcast(Entity pEntity, Packet<?> pPacket) {
        this.chunkMap.broadcast(pEntity, pPacket);
    }

    public void setViewDistance(int pViewDistance) {
        this.chunkMap.setServerViewDistance(pViewDistance);
    }

    public void setSimulationDistance(int pSimulationDistance) {
        this.distanceManager.updateSimulationDistance(pSimulationDistance);
    }

    @Override
    public void setSpawnSettings(boolean p_8425_) {
        this.spawnEnemies = p_8425_;
        this.spawnFriendlies = this.spawnFriendlies;
    }

    public String getChunkDebugData(ChunkPos pChunkPos) {
        return this.chunkMap.getChunkDebugData(pChunkPos);
    }

    public DimensionDataStorage getDataStorage() {
        return this.dataStorage;
    }

    public PoiManager getPoiManager() {
        return this.chunkMap.getPoiManager();
    }

    public ChunkScanAccess chunkScanner() {
        return this.chunkMap.chunkScanner();
    }

    @Nullable
    @VisibleForDebug
    public NaturalSpawner.SpawnState getLastSpawnState() {
        return this.lastSpawnState;
    }

    public void deactivateTicketsOnClosing() {
        this.ticketStorage.deactivateTicketsOnClosing();
    }

    public void onChunkReadyToSend(ChunkHolder pChunkHolder) {
        if (pChunkHolder.hasChangesToBroadcast()) {
            this.chunkHoldersToBroadcast.add(pChunkHolder);
        }
    }

    final class MainThreadExecutor extends BlockableEventLoop<Runnable> {
        MainThreadExecutor(final Level pLevel) {
            super("Chunk source main thread executor for " + pLevel.dimension().location());
        }

        @Override
        public void managedBlock(BooleanSupplier p_344943_) {
            super.managedBlock(() -> MinecraftServer.throwIfFatalException() && p_344943_.getAsBoolean());
        }

        @Override
        public Runnable wrapRunnable(Runnable pRunnable) {
            return pRunnable;
        }

        @Override
        protected boolean shouldRun(Runnable pRunnable) {
            return true;
        }

        @Override
        protected boolean scheduleExecutables() {
            return true;
        }

        @Override
        protected Thread getRunningThread() {
            return ServerChunkCache.this.mainThread;
        }

        @Override
        protected void doRunTask(Runnable pTask) {
            Profiler.get().incrementCounter("runTask");
            super.doRunTask(pTask);
        }

        @Override
        public boolean pollTask() {
            if (ServerChunkCache.this.runDistanceManagerUpdates()) {
                return true;
            } else {
                ServerChunkCache.this.lightEngine.tryScheduleUpdate();
                return super.pollTask();
            }
        }
    }

    /** @deprecated Use {@link #addTicketWithRadius(TicketType, ChunkPos, int)} with {@link TicketType#FORCED} */
    @Deprecated(forRemoval = true, since = "1.21.5")
    public void addRegionTicket(TicketType type, ChunkPos pos, int ticketLevel, boolean forceTicks) {
        this.addTicketWithRadius(forceTicks ? TicketType.FORCED : type, pos, ticketLevel);
    }

    /** @deprecated Use {@link #removeTicketWithRadius(TicketType, ChunkPos, int)} with {@link TicketType#FORCED} */
    @Deprecated(forRemoval = true, since = "1.21.5")
    public void removeRegionTicket(TicketType type, ChunkPos pos, int ticketLevel, boolean forceTicks) {
        this.removeTicketWithRadius(forceTicks ? TicketType.FORCED : type, pos, ticketLevel);
    }
}
