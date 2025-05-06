package net.minecraft.server.level.progress;

import java.util.concurrent.Executor;
import javax.annotation.Nullable;
import net.minecraft.util.thread.ConsecutiveExecutor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;

public class ProcessorChunkProgressListener implements ChunkProgressListener {
    private final ChunkProgressListener delegate;
    private final ConsecutiveExecutor consecutiveExecutor;
    private boolean started;

    private ProcessorChunkProgressListener(ChunkProgressListener pDelegate, Executor pDispatcher) {
        this.delegate = pDelegate;
        this.consecutiveExecutor = new ConsecutiveExecutor(pDispatcher, "progressListener");
    }

    public static ProcessorChunkProgressListener createStarted(ChunkProgressListener pDelegate, Executor pDispatcher) {
        ProcessorChunkProgressListener processorchunkprogresslistener = new ProcessorChunkProgressListener(pDelegate, pDispatcher);
        processorchunkprogresslistener.start();
        return processorchunkprogresslistener;
    }

    @Override
    public void updateSpawnPos(ChunkPos pCenter) {
        this.consecutiveExecutor.schedule(() -> this.delegate.updateSpawnPos(pCenter));
    }

    @Override
    public void onStatusChange(ChunkPos p_9645_, @Nullable ChunkStatus p_330099_) {
        if (this.started) {
            this.consecutiveExecutor.schedule(() -> this.delegate.onStatusChange(p_9645_, p_330099_));
        }
    }

    @Override
    public void start() {
        this.started = true;
        this.consecutiveExecutor.schedule(this.delegate::start);
    }

    @Override
    public void stop() {
        this.started = false;
        this.consecutiveExecutor.schedule(this.delegate::stop);
    }
}