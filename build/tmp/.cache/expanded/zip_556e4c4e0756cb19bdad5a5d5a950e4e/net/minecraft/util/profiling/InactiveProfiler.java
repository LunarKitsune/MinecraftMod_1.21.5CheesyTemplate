package net.minecraft.util.profiling;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.util.profiling.metrics.MetricCategory;
import org.apache.commons.lang3.tuple.Pair;

public class InactiveProfiler implements ProfileCollector {
    public static final InactiveProfiler INSTANCE = new InactiveProfiler();

    private InactiveProfiler() {
    }

    @Override
    public void startTick() {
    }

    @Override
    public void endTick() {
    }

    @Override
    public void push(String pName) {
    }

    @Override
    public void push(Supplier<String> pNameSupplier) {
    }

    @Override
    public void markForCharting(MetricCategory p_145951_) {
    }

    @Override
    public void pop() {
    }

    @Override
    public void popPush(String pName) {
    }

    @Override
    public void popPush(Supplier<String> pNameSupplier) {
    }

    @Override
    public Zone zone(String p_362797_) {
        return Zone.INACTIVE;
    }

    @Override
    public Zone zone(Supplier<String> p_367093_) {
        return Zone.INACTIVE;
    }

    @Override
    public void incrementCounter(String p_185253_, int p_185254_) {
    }

    @Override
    public void incrementCounter(Supplier<String> p_185256_, int p_185257_) {
    }

    @Override
    public ProfileResults getResults() {
        return EmptyProfileResults.EMPTY;
    }

    @Nullable
    @Override
    public ActiveProfiler.PathEntry getEntry(String p_145953_) {
        return null;
    }

    @Override
    public Set<Pair<String, MetricCategory>> getChartedPaths() {
        return ImmutableSet.of();
    }
}