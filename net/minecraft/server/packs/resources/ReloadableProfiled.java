package net.minecraft.server.packs.resources;

import com.google.common.base.Stopwatch;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.SystemUtils;
import net.minecraft.util.Unit;
import net.minecraft.util.profiling.MethodProfiler;
import net.minecraft.util.profiling.MethodProfilerResults;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ReloadableProfiled extends Reloadable<ReloadableProfiled.State> {
    private static final Logger LOGGER = LogManager.getLogger();
    private final Stopwatch total = Stopwatch.createUnstarted();

    public ReloadableProfiled(IResourceManager manager, List<IReloadListener> reloaders, Executor prepareExecutor, Executor applyExecutor, CompletableFuture<Unit> initialStage) {
        super(prepareExecutor, applyExecutor, manager, reloaders, (synchronizer, resourceManager, reloader, prepare, apply) -> {
            AtomicLong atomicLong = new AtomicLong();
            AtomicLong atomicLong2 = new AtomicLong();
            MethodProfiler activeProfiler = new MethodProfiler(SystemUtils.timeSource, () -> {
                return 0;
            }, false);
            MethodProfiler activeProfiler2 = new MethodProfiler(SystemUtils.timeSource, () -> {
                return 0;
            }, false);
            CompletableFuture<Void> completableFuture = reloader.reload(synchronizer, resourceManager, activeProfiler, activeProfiler2, (preparation) -> {
                prepare.execute(() -> {
                    long l = SystemUtils.getMonotonicNanos();
                    preparation.run();
                    atomicLong.addAndGet(SystemUtils.getMonotonicNanos() - l);
                });
            }, (application) -> {
                apply.execute(() -> {
                    long l = SystemUtils.getMonotonicNanos();
                    application.run();
                    atomicLong2.addAndGet(SystemUtils.getMonotonicNanos() - l);
                });
            });
            return completableFuture.thenApplyAsync((void_) -> {
                LOGGER.debug("Finished reloading " + reloader.getName());
                return new ReloadableProfiled.State(reloader.getName(), activeProfiler.getResults(), activeProfiler2.getResults(), atomicLong, atomicLong2);
            }, applyExecutor);
        }, initialStage);
        this.total.start();
        this.allDone.thenAcceptAsync(this::finish, applyExecutor);
    }

    private void finish(List<ReloadableProfiled.State> summaries) {
        this.total.stop();
        int i = 0;
        LOGGER.info("Resource reload finished after {} ms", (long)this.total.elapsed(TimeUnit.MILLISECONDS));

        for(ReloadableProfiled.State state : summaries) {
            MethodProfilerResults profileResults = state.preparationResult;
            MethodProfilerResults profileResults2 = state.reloadResult;
            int j = (int)((double)state.preparationNanos.get() / 1000000.0D);
            int k = (int)((double)state.reloadNanos.get() / 1000000.0D);
            int l = j + k;
            String string = state.name;
            LOGGER.info("{} took approximately {} ms ({} ms preparing, {} ms applying)", string, l, j, k);
            i += k;
        }

        LOGGER.info("Total blocking time: {} ms", (int)i);
    }

    public static class State {
        final String name;
        final MethodProfilerResults preparationResult;
        final MethodProfilerResults reloadResult;
        final AtomicLong preparationNanos;
        final AtomicLong reloadNanos;

        State(String name, MethodProfilerResults prepareProfile, MethodProfilerResults applyProfile, AtomicLong prepareTimeMs, AtomicLong applyTimeMs) {
            this.name = name;
            this.preparationResult = prepareProfile;
            this.reloadResult = applyProfile;
            this.preparationNanos = prepareTimeMs;
            this.reloadNanos = applyTimeMs;
        }
    }
}
