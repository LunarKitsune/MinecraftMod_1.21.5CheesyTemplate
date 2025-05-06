package net.minecraft.server.level;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntSupplier;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.thread.ConsecutiveExecutor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.slf4j.Logger;

public class ThreadedLevelLightEngine extends LevelLightEngine implements AutoCloseable {
    public static final int DEFAULT_BATCH_SIZE = 1000;
    private static final Logger LOGGER = LogUtils.getLogger();
    private final ConsecutiveExecutor consecutiveExecutor;
    private final ObjectList<Pair<ThreadedLevelLightEngine.TaskType, Runnable>> lightTasks = new ObjectArrayList<>();
    private final ChunkMap chunkMap;
    private final ChunkTaskDispatcher taskDispatcher;
    private final int taskPerBatch = 1000;
    private final AtomicBoolean scheduled = new AtomicBoolean();

    public ThreadedLevelLightEngine(LightChunkGetter pLightChunkGetter, ChunkMap pChunkMap, boolean pSkyLight, ConsecutiveExecutor pConsecutiveExecutor, ChunkTaskDispatcher pTaskDispatcher) {
        super(pLightChunkGetter, true, pSkyLight);
        this.chunkMap = pChunkMap;
        this.taskDispatcher = pTaskDispatcher;
        this.consecutiveExecutor = pConsecutiveExecutor;
    }

    @Override
    public void close() {
    }

    @Override
    public int runLightUpdates() {
        throw (UnsupportedOperationException)Util.pauseInIde(new UnsupportedOperationException("Ran automatically on a different thread!"));
    }

    @Override
    public void checkBlock(BlockPos p_9357_) {
        BlockPos blockpos = p_9357_.immutable();
        this.addTask(
            SectionPos.blockToSectionCoord(p_9357_.getX()),
            SectionPos.blockToSectionCoord(p_9357_.getZ()),
            ThreadedLevelLightEngine.TaskType.PRE_UPDATE,
            Util.name(() -> super.checkBlock(blockpos), () -> "checkBlock " + blockpos)
        );
    }

    protected void updateChunkStatus(ChunkPos pChunkPos) {
        this.addTask(pChunkPos.x, pChunkPos.z, () -> 0, ThreadedLevelLightEngine.TaskType.PRE_UPDATE, Util.name(() -> {
            super.retainData(pChunkPos, false);
            super.setLightEnabled(pChunkPos, false);

            for (int i = this.getMinLightSection(); i < this.getMaxLightSection(); i++) {
                super.queueSectionData(LightLayer.BLOCK, SectionPos.of(pChunkPos, i), null);
                super.queueSectionData(LightLayer.SKY, SectionPos.of(pChunkPos, i), null);
            }

            for (int j = this.levelHeightAccessor.getMinSectionY(); j <= this.levelHeightAccessor.getMaxSectionY(); j++) {
                super.updateSectionStatus(SectionPos.of(pChunkPos, j), true);
            }
        }, () -> "updateChunkStatus " + pChunkPos + " true"));
    }

    @Override
    public void updateSectionStatus(SectionPos pPos, boolean pIsEmpty) {
        this.addTask(
            pPos.x(),
            pPos.z(),
            () -> 0,
            ThreadedLevelLightEngine.TaskType.PRE_UPDATE,
            Util.name(() -> super.updateSectionStatus(pPos, pIsEmpty), () -> "updateSectionStatus " + pPos + " " + pIsEmpty)
        );
    }

    @Override
    public void propagateLightSources(ChunkPos p_285029_) {
        this.addTask(
            p_285029_.x,
            p_285029_.z,
            ThreadedLevelLightEngine.TaskType.PRE_UPDATE,
            Util.name(() -> super.propagateLightSources(p_285029_), () -> "propagateLight " + p_285029_)
        );
    }

    @Override
    public void setLightEnabled(ChunkPos p_9336_, boolean p_9337_) {
        this.addTask(
            p_9336_.x,
            p_9336_.z,
            ThreadedLevelLightEngine.TaskType.PRE_UPDATE,
            Util.name(() -> super.setLightEnabled(p_9336_, p_9337_), () -> "enableLight " + p_9336_ + " " + p_9337_)
        );
    }

    @Override
    public void queueSectionData(LightLayer p_285046_, SectionPos p_285496_, @Nullable DataLayer p_285495_) {
        this.addTask(
            p_285496_.x(),
            p_285496_.z(),
            () -> 0,
            ThreadedLevelLightEngine.TaskType.PRE_UPDATE,
            Util.name(() -> super.queueSectionData(p_285046_, p_285496_, p_285495_), () -> "queueData " + p_285496_)
        );
    }

    private void addTask(int pChunkX, int pChunkZ, ThreadedLevelLightEngine.TaskType pType, Runnable pTask) {
        this.addTask(pChunkX, pChunkZ, this.chunkMap.getChunkQueueLevel(ChunkPos.asLong(pChunkX, pChunkZ)), pType, pTask);
    }

    private void addTask(int pChunkX, int pChunkZ, IntSupplier pQueueLevelSupplier, ThreadedLevelLightEngine.TaskType pType, Runnable pTask) {
        this.taskDispatcher.submit(() -> {
            this.lightTasks.add(Pair.of(pType, pTask));
            if (this.lightTasks.size() >= 1000) {
                this.runUpdate();
            }
        }, ChunkPos.asLong(pChunkX, pChunkZ), pQueueLevelSupplier);
    }

    @Override
    public void retainData(ChunkPos pPos, boolean pRetain) {
        this.addTask(
            pPos.x,
            pPos.z,
            () -> 0,
            ThreadedLevelLightEngine.TaskType.PRE_UPDATE,
            Util.name(() -> super.retainData(pPos, pRetain), () -> "retainData " + pPos)
        );
    }

    public CompletableFuture<ChunkAccess> initializeLight(ChunkAccess pChunk, boolean pLightEnabled) {
        ChunkPos chunkpos = pChunk.getPos();
        this.addTask(chunkpos.x, chunkpos.z, ThreadedLevelLightEngine.TaskType.PRE_UPDATE, Util.name(() -> {
            LevelChunkSection[] alevelchunksection = pChunk.getSections();

            for (int i = 0; i < pChunk.getSectionsCount(); i++) {
                LevelChunkSection levelchunksection = alevelchunksection[i];
                if (!levelchunksection.hasOnlyAir()) {
                    int j = this.levelHeightAccessor.getSectionYFromSectionIndex(i);
                    super.updateSectionStatus(SectionPos.of(chunkpos, j), false);
                }
            }
        }, () -> "initializeLight: " + chunkpos));
        return CompletableFuture.supplyAsync(() -> {
            super.setLightEnabled(chunkpos, pLightEnabled);
            super.retainData(chunkpos, false);
            return pChunk;
        }, p_215135_ -> this.addTask(chunkpos.x, chunkpos.z, ThreadedLevelLightEngine.TaskType.POST_UPDATE, p_215135_));
    }

    public CompletableFuture<ChunkAccess> lightChunk(ChunkAccess pChunk, boolean pIsLighted) {
        ChunkPos chunkpos = pChunk.getPos();
        pChunk.setLightCorrect(false);
        this.addTask(chunkpos.x, chunkpos.z, ThreadedLevelLightEngine.TaskType.PRE_UPDATE, Util.name(() -> {
            if (!pIsLighted) {
                super.propagateLightSources(chunkpos);
            }
        }, () -> "lightChunk " + chunkpos + " " + pIsLighted));
        return CompletableFuture.supplyAsync(() -> {
            pChunk.setLightCorrect(true);
            net.minecraftforge.common.ForgeHooks.fireLightingCalculatedEvent(pChunk);
            return pChunk;
        }, p_280982_ -> this.addTask(chunkpos.x, chunkpos.z, ThreadedLevelLightEngine.TaskType.POST_UPDATE, p_280982_));
    }

    public void tryScheduleUpdate() {
        if ((!this.lightTasks.isEmpty() || super.hasLightWork()) && this.scheduled.compareAndSet(false, true)) {
            this.consecutiveExecutor.schedule(() -> {
                this.runUpdate();
                this.scheduled.set(false);
            });
        }
    }

    private void runUpdate() {
        int i = Math.min(this.lightTasks.size(), 1000);
        ObjectListIterator<Pair<ThreadedLevelLightEngine.TaskType, Runnable>> objectlistiterator = this.lightTasks.iterator();

        int j;
        for (j = 0; objectlistiterator.hasNext() && j < i; j++) {
            Pair<ThreadedLevelLightEngine.TaskType, Runnable> pair = objectlistiterator.next();
            if (pair.getFirst() == ThreadedLevelLightEngine.TaskType.PRE_UPDATE) {
                pair.getSecond().run();
            }
        }

        objectlistiterator.back(j);
        super.runLightUpdates();

        for (int k = 0; objectlistiterator.hasNext() && k < i; k++) {
            Pair<ThreadedLevelLightEngine.TaskType, Runnable> pair1 = objectlistiterator.next();
            if (pair1.getFirst() == ThreadedLevelLightEngine.TaskType.POST_UPDATE) {
                pair1.getSecond().run();
            }

            objectlistiterator.remove();
        }
    }

    public CompletableFuture<?> waitForPendingTasks(int pX, int pZ) {
        return CompletableFuture.runAsync(() -> {}, p_296584_ -> this.addTask(pX, pZ, ThreadedLevelLightEngine.TaskType.POST_UPDATE, p_296584_));
    }

    static enum TaskType {
        PRE_UPDATE,
        POST_UPDATE;
    }
}
