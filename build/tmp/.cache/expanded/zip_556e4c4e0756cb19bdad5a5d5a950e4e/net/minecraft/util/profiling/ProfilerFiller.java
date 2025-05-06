package net.minecraft.util.profiling;

import java.util.function.Supplier;
import net.minecraft.util.profiling.metrics.MetricCategory;

public interface ProfilerFiller {
    String ROOT = "root";

    void startTick();

    void endTick();

    void push(String pName);

    void push(Supplier<String> pNameSupplier);

    void pop();

    void popPush(String pName);

    void popPush(Supplier<String> pNameSupplier);

    default void addZoneText(String pText) {
    }

    default void addZoneValue(long pValue) {
    }

    default void setZoneColor(int pColor) {
    }

    default Zone zone(String pName) {
        this.push(pName);
        return new Zone(this);
    }

    default Zone zone(Supplier<String> pName) {
        this.push(pName);
        return new Zone(this);
    }

    void markForCharting(MetricCategory pCategory);

    default void incrementCounter(String pEntryId) {
        this.incrementCounter(pEntryId, 1);
    }

    void incrementCounter(String pCounterName, int pIncrement);

    default void incrementCounter(Supplier<String> pEntryIdSupplier) {
        this.incrementCounter(pEntryIdSupplier, 1);
    }

    void incrementCounter(Supplier<String> pCounterNameSupplier, int pIncrement);

    static ProfilerFiller combine(ProfilerFiller pFirst, ProfilerFiller pSecond) {
        if (pFirst == InactiveProfiler.INSTANCE) {
            return pSecond;
        } else {
            return (ProfilerFiller)(pSecond == InactiveProfiler.INSTANCE ? pFirst : new ProfilerFiller.CombinedProfileFiller(pFirst, pSecond));
        }
    }

    public static class CombinedProfileFiller implements ProfilerFiller {
        private final ProfilerFiller first;
        private final ProfilerFiller second;

        public CombinedProfileFiller(ProfilerFiller pFirst, ProfilerFiller pSecond) {
            this.first = pFirst;
            this.second = pSecond;
        }

        @Override
        public void startTick() {
            this.first.startTick();
            this.second.startTick();
        }

        @Override
        public void endTick() {
            this.first.endTick();
            this.second.endTick();
        }

        @Override
        public void push(String p_363352_) {
            this.first.push(p_363352_);
            this.second.push(p_363352_);
        }

        @Override
        public void push(Supplier<String> p_361348_) {
            this.first.push(p_361348_);
            this.second.push(p_361348_);
        }

        @Override
        public void markForCharting(MetricCategory p_365312_) {
            this.first.markForCharting(p_365312_);
            this.second.markForCharting(p_365312_);
        }

        @Override
        public void pop() {
            this.first.pop();
            this.second.pop();
        }

        @Override
        public void popPush(String p_364738_) {
            this.first.popPush(p_364738_);
            this.second.popPush(p_364738_);
        }

        @Override
        public void popPush(Supplier<String> p_361184_) {
            this.first.popPush(p_361184_);
            this.second.popPush(p_361184_);
        }

        @Override
        public void incrementCounter(String p_368612_, int p_365761_) {
            this.first.incrementCounter(p_368612_, p_365761_);
            this.second.incrementCounter(p_368612_, p_365761_);
        }

        @Override
        public void incrementCounter(Supplier<String> p_365250_, int p_365517_) {
            this.first.incrementCounter(p_365250_, p_365517_);
            this.second.incrementCounter(p_365250_, p_365517_);
        }

        @Override
        public void addZoneText(String p_369699_) {
            this.first.addZoneText(p_369699_);
            this.second.addZoneText(p_369699_);
        }

        @Override
        public void addZoneValue(long p_362373_) {
            this.first.addZoneValue(p_362373_);
            this.second.addZoneValue(p_362373_);
        }

        @Override
        public void setZoneColor(int p_365533_) {
            this.first.setZoneColor(p_365533_);
            this.second.setZoneColor(p_365533_);
        }
    }
}