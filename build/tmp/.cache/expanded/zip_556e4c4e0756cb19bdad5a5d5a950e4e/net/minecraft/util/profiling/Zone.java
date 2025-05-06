package net.minecraft.util.profiling;

import java.util.function.Supplier;
import javax.annotation.Nullable;

public class Zone implements AutoCloseable {
    public static final Zone INACTIVE = new Zone(null);
    @Nullable
    private final ProfilerFiller profiler;

    Zone(@Nullable ProfilerFiller pProfiler) {
        this.profiler = pProfiler;
    }

    public Zone addText(String pText) {
        if (this.profiler != null) {
            this.profiler.addZoneText(pText);
        }

        return this;
    }

    public Zone addText(Supplier<String> pText) {
        if (this.profiler != null) {
            this.profiler.addZoneText(pText.get());
        }

        return this;
    }

    public Zone addValue(long pValue) {
        if (this.profiler != null) {
            this.profiler.addZoneValue(pValue);
        }

        return this;
    }

    public Zone setColor(int pColor) {
        if (this.profiler != null) {
            this.profiler.setZoneColor(pColor);
        }

        return this;
    }

    @Override
    public void close() {
        if (this.profiler != null) {
            this.profiler.pop();
        }
    }
}