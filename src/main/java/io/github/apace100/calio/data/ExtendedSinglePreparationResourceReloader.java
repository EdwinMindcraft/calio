package io.github.apace100.calio.data;

import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public abstract class ExtendedSinglePreparationResourceReloader<T> extends SimplePreparableReloadListener<T> {

    @Override
    public CompletableFuture<Void> reload(PreparationBarrier synchronizer, ResourceManager manager, ProfilerFiller prepareProfiler, ProfilerFiller applyProfiler, Executor prepareExecutor, Executor applyExecutor) {
        return CompletableFuture.supplyAsync(() -> this.prepare(manager, prepareProfiler), prepareExecutor)
            .thenCompose(synchronizer::wait)
            .thenAcceptAsync(prepared -> this.processBeforeApply(prepared, manager, applyProfiler), applyExecutor);
    }

    protected final void processBeforeApply(T prepared, ResourceManager manager, ProfilerFiller profiler) {
        this.preApply(prepared, manager, profiler);
        this.apply(prepared, manager, profiler);
    }

    protected void preApply(T prepared, ResourceManager manager, ProfilerFiller profiler) {

    }

}
