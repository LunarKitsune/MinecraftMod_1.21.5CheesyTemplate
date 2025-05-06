package net.minecraft.server.level;

import com.mojang.logging.LogUtils;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import javax.annotation.Nullable;
import net.minecraft.util.Unit;
import net.minecraft.util.thread.PriorityConsecutiveExecutor;
import net.minecraft.util.thread.StrictQueue;
import net.minecraft.util.thread.TaskScheduler;
import net.minecraft.world.level.ChunkPos;
import org.slf4j.Logger;

public class ChunkTaskDispatcher implements ChunkHolder.LevelChangeListener, AutoCloseable {
    public static final int DISPATCHER_PRIORITY_COUNT = 4;
    private static final Logger LOGGER = LogUtils.getLogger();
    private final ChunkTaskPriorityQueue queue;
    private final TaskScheduler<Runnable> executor;
    private final PriorityConsecutiveExecutor dispatcher;
    protected boolean sleeping;

    public ChunkTaskDispatcher(TaskScheduler<Runnable> pExecutor, Executor pDispatcher) {
        this.queue = new ChunkTaskPriorityQueue(pExecutor.name() + "_queue");
        this.executor = pExecutor;
        this.dispatcher = new PriorityConsecutiveExecutor(4, pDispatcher, "dispatcher");
        this.sleeping = true;
    }

    public boolean hasWork() {
        return this.dispatcher.hasWork() || this.queue.hasWork();
    }

    @Override
    public void onLevelChange(ChunkPos p_368881_, IntSupplier p_362965_, int p_369655_, IntConsumer p_365320_) {
        this.dispatcher.schedule(new StrictQueue.RunnableWithPriority(0, () -> {
            int i = p_362965_.getAsInt();
            this.queue.resortChunkTasks(i, p_368881_, p_369655_);
            p_365320_.accept(p_369655_);
        }));
    }

    public void release(long pChunkPos, Runnable pAfterRelease, boolean pFullClear) {
        this.dispatcher.schedule(new StrictQueue.RunnableWithPriority(1, () -> {
            this.queue.release(pChunkPos, pFullClear);
            this.onRelease(pChunkPos);
            if (this.sleeping) {
                this.sleeping = false;
                this.pollTask();
            }

            pAfterRelease.run();
        }));
    }

    public void submit(Runnable pTask, long pChunkPos, IntSupplier pQueueLevelSupplier) {
        this.dispatcher.schedule(new StrictQueue.RunnableWithPriority(2, () -> {
            int i = pQueueLevelSupplier.getAsInt();
            this.queue.submit(pTask, pChunkPos, i);
            if (this.sleeping) {
                this.sleeping = false;
                this.pollTask();
            }
        }));
    }

    protected void pollTask() {
        this.dispatcher.schedule(new StrictQueue.RunnableWithPriority(3, () -> {
            ChunkTaskPriorityQueue.TasksForChunk chunktaskpriorityqueue$tasksforchunk = this.popTasks();
            if (chunktaskpriorityqueue$tasksforchunk == null) {
                this.sleeping = true;
            } else {
                this.scheduleForExecution(chunktaskpriorityqueue$tasksforchunk);
            }
        }));
    }

    protected void scheduleForExecution(ChunkTaskPriorityQueue.TasksForChunk pTasks) {
        CompletableFuture.allOf(pTasks.tasks().stream().map(p_363376_ -> this.executor.scheduleWithResult(p_366925_ -> {
            p_363376_.run();
            p_366925_.complete(Unit.INSTANCE);
        })).toArray(CompletableFuture[]::new)).thenAccept(p_367735_ -> this.pollTask());
    }

    protected void onRelease(long pChunkPos) {
    }

    @Nullable
    protected ChunkTaskPriorityQueue.TasksForChunk popTasks() {
        return this.queue.pop();
    }

    @Override
    public void close() {
        this.executor.close();
    }
}