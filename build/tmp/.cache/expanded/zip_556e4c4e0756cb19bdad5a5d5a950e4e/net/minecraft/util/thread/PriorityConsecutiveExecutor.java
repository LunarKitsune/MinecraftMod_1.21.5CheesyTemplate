package net.minecraft.util.thread;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import net.minecraft.util.profiling.metrics.MetricsRegistry;

public class PriorityConsecutiveExecutor extends AbstractConsecutiveExecutor<StrictQueue.RunnableWithPriority> {
    public PriorityConsecutiveExecutor(int pSize, Executor pExecutor, String pName) {
        super(new StrictQueue.FixedPriorityQueue(pSize), pExecutor, pName);
        MetricsRegistry.INSTANCE.add(this);
    }

    public StrictQueue.RunnableWithPriority wrapRunnable(Runnable p_370061_) {
        return new StrictQueue.RunnableWithPriority(0, p_370061_);
    }

    public <Source> CompletableFuture<Source> scheduleWithResult(int pPriority, Consumer<CompletableFuture<Source>> pResultConsumer) {
        CompletableFuture<Source> completablefuture = new CompletableFuture<>();
        this.schedule(new StrictQueue.RunnableWithPriority(pPriority, () -> pResultConsumer.accept(completablefuture)));
        return completablefuture;
    }
}