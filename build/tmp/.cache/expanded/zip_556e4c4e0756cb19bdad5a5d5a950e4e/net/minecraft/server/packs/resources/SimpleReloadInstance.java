package net.minecraft.server.packs.resources;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.util.Unit;

public class SimpleReloadInstance<S> implements ReloadInstance {
    private static final int PREPARATION_PROGRESS_WEIGHT = 2;
    private static final int EXTRA_RELOAD_PROGRESS_WEIGHT = 2;
    private static final int LISTENER_PROGRESS_WEIGHT = 1;
    final CompletableFuture<Unit> allPreparations = new CompletableFuture<>();
    @Nullable
    private CompletableFuture<List<S>> allDone;
    final Set<PreparableReloadListener> preparingListeners;
    private final int listenerCount;
    private final AtomicInteger startedTasks = new AtomicInteger();
    private final AtomicInteger finishedTasks = new AtomicInteger();
    private final AtomicInteger startedReloads = new AtomicInteger();
    private final AtomicInteger finishedReloads = new AtomicInteger();

    public static ReloadInstance of(
        ResourceManager pResourceManager, List<PreparableReloadListener> pListeners, Executor pBackgroundExecutor, Executor pGameExecutor, CompletableFuture<Unit> pAlsoWaitedFor
    ) {
        SimpleReloadInstance<Void> simplereloadinstance = new SimpleReloadInstance<>(pListeners);
        simplereloadinstance.startTasks(pBackgroundExecutor, pGameExecutor, pResourceManager, pListeners, SimpleReloadInstance.StateFactory.SIMPLE, pAlsoWaitedFor);
        return simplereloadinstance;
    }

    protected SimpleReloadInstance(List<PreparableReloadListener> pPreparingListeners) {
        this.listenerCount = pPreparingListeners.size();
        this.preparingListeners = new HashSet<>(pPreparingListeners);
    }

    protected void startTasks(
        Executor pBackgroundExecutor,
        Executor pGameExecutor,
        ResourceManager pResourceManager,
        List<PreparableReloadListener> pListeners,
        SimpleReloadInstance.StateFactory<S> pStateFactory,
        CompletableFuture<?> pAlsoWaitedFor
    ) {
        this.allDone = this.prepareTasks(pBackgroundExecutor, pGameExecutor, pResourceManager, pListeners, pStateFactory, pAlsoWaitedFor);
    }

    protected CompletableFuture<List<S>> prepareTasks(
        Executor pBackgroundExecutor,
        Executor pGameExectutor,
        ResourceManager pResourceManager,
        List<PreparableReloadListener> pListeners,
        SimpleReloadInstance.StateFactory<S> pStateFactory,
        CompletableFuture<?> pAlsoWaitedFor
    ) {
        Executor executor = p_390185_ -> {
            this.startedTasks.incrementAndGet();
            pBackgroundExecutor.execute(() -> {
                p_390185_.run();
                this.finishedTasks.incrementAndGet();
            });
        };
        Executor executor1 = p_390183_ -> {
            this.startedReloads.incrementAndGet();
            pGameExectutor.execute(() -> {
                p_390183_.run();
                this.finishedReloads.incrementAndGet();
            });
        };
        this.startedTasks.incrementAndGet();
        pAlsoWaitedFor.thenRun(this.finishedTasks::incrementAndGet);
        CompletableFuture<?> completablefuture = pAlsoWaitedFor;
        List<CompletableFuture<S>> list = new ArrayList<>();

        for (PreparableReloadListener preparablereloadlistener : pListeners) {
            PreparableReloadListener.PreparationBarrier preparablereloadlistener$preparationbarrier = this.createBarrierForListener(
                preparablereloadlistener, completablefuture, pGameExectutor
            );
            CompletableFuture<S> completablefuture1 = pStateFactory.create(
                preparablereloadlistener$preparationbarrier, pResourceManager, preparablereloadlistener, executor, executor1
            );
            list.add(completablefuture1);
            completablefuture = completablefuture1;
        }

        return Util.sequenceFailFast(list);
    }

    private PreparableReloadListener.PreparationBarrier createBarrierForListener(
        final PreparableReloadListener pListener, final CompletableFuture<?> pAlsoWaitedFor, final Executor pExecutor
    ) {
        return new PreparableReloadListener.PreparationBarrier() {
            @Override
            public <T> CompletableFuture<T> wait(T p_10858_) {
                pExecutor.execute(() -> {
                    SimpleReloadInstance.this.preparingListeners.remove(pListener);
                    if (SimpleReloadInstance.this.preparingListeners.isEmpty()) {
                        SimpleReloadInstance.this.allPreparations.complete(Unit.INSTANCE);
                    }
                });
                return SimpleReloadInstance.this.allPreparations.thenCombine((CompletionStage<? extends T>)pAlsoWaitedFor, (p_10861_, p_10862_) -> p_10858_);
            }
        };
    }

    @Override
    public CompletableFuture<?> done() {
        return Objects.requireNonNull(this.allDone, "not started");
    }

    @Override
    public float getActualProgress() {
        int i = this.listenerCount - this.preparingListeners.size();
        float f = weightProgress(this.finishedTasks.get(), this.finishedReloads.get(), i);
        float f1 = weightProgress(this.startedTasks.get(), this.startedReloads.get(), this.listenerCount);
        return f / f1;
    }

    private static int weightProgress(int pTasks, int pReloads, int pListeners) {
        return pTasks * 2 + pReloads * 2 + pListeners * 1;
    }

    public static ReloadInstance create(
        ResourceManager pResourceManager,
        List<PreparableReloadListener> pListeners,
        Executor pBackgroundExecutor,
        Executor pGameExecutor,
        CompletableFuture<Unit> pAlsoWaitedFor,
        boolean pProfiled
    ) {
        return pProfiled
            ? ProfiledReloadInstance.of(pResourceManager, pListeners, pBackgroundExecutor, pGameExecutor, pAlsoWaitedFor)
            : of(pResourceManager, pListeners, pBackgroundExecutor, pGameExecutor, pAlsoWaitedFor);
    }

    @FunctionalInterface
    protected interface StateFactory<S> {
        SimpleReloadInstance.StateFactory<Void> SIMPLE = (p_395920_, p_395816_, p_394007_, p_393904_, p_396167_) -> p_394007_.reload(
            p_395920_, p_395816_, p_393904_, p_396167_
        );

        CompletableFuture<S> create(
            PreparableReloadListener.PreparationBarrier pPreperationBarrier,
            ResourceManager pResourceManager,
            PreparableReloadListener pListener,
            Executor pBackgroundExecutor,
            Executor pGameExecutor
        );
    }
}