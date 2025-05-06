package net.minecraft.util.debugchart;

public abstract class AbstractSampleLogger implements SampleLogger {
    protected final long[] defaults;
    protected final long[] sample;

    protected AbstractSampleLogger(int pSize, long[] pDefaults) {
        if (pDefaults.length != pSize) {
            throw new IllegalArgumentException("defaults have incorrect length of " + pDefaults.length);
        } else {
            this.sample = new long[pSize];
            this.defaults = pDefaults;
        }
    }

    @Override
    public void logFullSample(long[] p_334735_) {
        System.arraycopy(p_334735_, 0, this.sample, 0, p_334735_.length);
        this.useSample();
        this.resetSample();
    }

    @Override
    public void logSample(long p_328993_) {
        this.sample[0] = p_328993_;
        this.useSample();
        this.resetSample();
    }

    @Override
    public void logPartialSample(long p_330576_, int p_334353_) {
        if (p_334353_ >= 1 && p_334353_ < this.sample.length) {
            this.sample[p_334353_] = p_330576_;
        } else {
            throw new IndexOutOfBoundsException(p_334353_ + " out of bounds for dimensions " + this.sample.length);
        }
    }

    protected abstract void useSample();

    protected void resetSample() {
        System.arraycopy(this.defaults, 0, this.sample, 0, this.defaults.length);
    }
}