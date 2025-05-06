package net.minecraft.server.packs.resources;

import com.google.common.base.Stopwatch;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.Util;
import net.minecraft.util.Unit;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

public class ProfiledReloadInstance extends SimpleReloadInstance<ProfiledReloadInstance.State> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Stopwatch total = Stopwatch.createUnstarted();

    public static ReloadInstance of(
        ResourceManager pResourceManager, List<PreparableReloadListener> pListeners, Executor pBackgroundExecutor, Executor pGameExecutor, CompletableFuture<Unit> pAlsoWaitedFor
    ) {
        ProfiledReloadInstance profiledreloadinstance = new ProfiledReloadInstance(pListeners);
        profiledreloadinstance.startTasks(
            pBackgroundExecutor,
            pGameExecutor,
            pResourceManager,
            pListeners,
            (p_390172_, p_390173_, p_390174_, p_390175_, p_390176_) -> {
                AtomicLong atomiclong = new AtomicLong();
                AtomicLong atomiclong1 = new AtomicLong();
                AtomicLong atomiclong2 = new AtomicLong();
                AtomicLong atomiclong3 = new AtomicLong();
                CompletableFuture<Void> completablefuture = p_390174_.reload(
                    p_390172_,
                    p_390173_,
                    profiledExecutor(p_390175_, atomiclong, atomiclong1, p_390174_.getName()),
                    profiledExecutor(p_390176_, atomiclong2, atomiclong3, p_390174_.getName())
                );
                return completablefuture.thenApplyAsync(p_390170_ -> {
                    LOGGER.debug("Finished reloading {}", p_390174_.getName());
                    return new ProfiledReloadInstance.State(p_390174_.getName(), atomiclong, atomiclong1, atomiclong2, atomiclong3);
                }, pGameExecutor);
            },
            pAlsoWaitedFor
        );
        return profiledreloadinstance;
    }

    private ProfiledReloadInstance(List<PreparableReloadListener> pListeners) {
        super(pListeners);
        this.total.start();
    }

    @Override
    protected CompletableFuture<List<ProfiledReloadInstance.State>> prepareTasks(
        Executor p_396171_,
        Executor p_394309_,
        ResourceManager p_397859_,
        List<PreparableReloadListener> p_394859_,
        SimpleReloadInstance.StateFactory<ProfiledReloadInstance.State> p_391634_,
        CompletableFuture<?> p_397690_
    ) {
        return super.prepareTasks(p_396171_, p_394309_, p_397859_, p_394859_, p_391634_, p_397690_).thenApplyAsync(this::finish, p_394309_);
    }

    private static Executor profiledExecutor(Executor pExecutor, AtomicLong pTimeTaken, AtomicLong pTimesRun, String pName) {
        return p_390164_ -> pExecutor.execute(() -> {
            ProfilerFiller profilerfiller = Profiler.get();
            profilerfiller.push(pName);
            long i = Util.getNanos();
            p_390164_.run();
            pTimeTaken.addAndGet(Util.getNanos() - i);
            pTimesRun.incrementAndGet();
            profilerfiller.pop();
        });
    }

    private List<ProfiledReloadInstance.State> finish(List<ProfiledReloadInstance.State> pDatapoints) {
        this.total.stop();
        long i = 0L;
        LOGGER.info("Resource reload finished after {} ms", this.total.elapsed(TimeUnit.MILLISECONDS));

        for (ProfiledReloadInstance.State profiledreloadinstance$state : pDatapoints) {
            long j = TimeUnit.NANOSECONDS.toMillis(profiledreloadinstance$state.preparationNanos.get());
            long k = profiledreloadinstance$state.preparationCount.get();
            long l = TimeUnit.NANOSECONDS.toMillis(profiledreloadinstance$state.reloadNanos.get());
            long i1 = profiledreloadinstance$state.reloadCount.get();
            long j1 = j + l;
            long k1 = k + i1;
            String s = profiledreloadinstance$state.name;
            LOGGER.info("{} took approximately {} tasks/{} ms ({} tasks/{} ms preparing, {} tasks/{} ms applying)", s, k1, j1, k, j, i1, l);
            i += l;
        }

        LOGGER.info("Total blocking time: {} ms", i);
        return pDatapoints;
    }

    public record State(String name, AtomicLong preparationNanos, AtomicLong preparationCount, AtomicLong reloadNanos, AtomicLong reloadCount) {
    }
}