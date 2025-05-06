package net.minecraft.world.level.levelgen;

import java.util.function.LongFunction;
import net.minecraft.util.RandomSource;

public class WorldgenRandom extends LegacyRandomSource {
    private final RandomSource randomSource;
    private int count;

    public WorldgenRandom(RandomSource pRandomSource) {
        super(0L);
        this.randomSource = pRandomSource;
    }

    public int getCount() {
        return this.count;
    }

    @Override
    public RandomSource fork() {
        return this.randomSource.fork();
    }

    @Override
    public PositionalRandomFactory forkPositional() {
        return this.randomSource.forkPositional();
    }

    @Override
    public int next(int pBits) {
        this.count++;
        return this.randomSource instanceof LegacyRandomSource legacyrandomsource
            ? legacyrandomsource.next(pBits)
            : (int)(this.randomSource.nextLong() >>> 64 - pBits);
    }

    @Override
    public synchronized void setSeed(long p_190073_) {
        if (this.randomSource != null) {
            this.randomSource.setSeed(p_190073_);
        }
    }

    public long setDecorationSeed(long pLevelSeed, int pMinChunkBlockX, int pMinChunkBlockZ) {
        this.setSeed(pLevelSeed);
        long i = this.nextLong() | 1L;
        long j = this.nextLong() | 1L;
        long k = pMinChunkBlockX * i + pMinChunkBlockZ * j ^ pLevelSeed;
        this.setSeed(k);
        return k;
    }

    public void setFeatureSeed(long pDecorationSeed, int pIndex, int pDecorationStep) {
        long i = pDecorationSeed + pIndex + 10000 * pDecorationStep;
        this.setSeed(i);
    }

    public void setLargeFeatureSeed(long pBaseSeed, int pChunkX, int pChunkZ) {
        this.setSeed(pBaseSeed);
        long i = this.nextLong();
        long j = this.nextLong();
        long k = pChunkX * i ^ pChunkZ * j ^ pBaseSeed;
        this.setSeed(k);
    }

    public void setLargeFeatureWithSalt(long pLevelSeed, int pRegionX, int pRegionZ, int pSalt) {
        long i = pRegionX * 341873128712L + pRegionZ * 132897987541L + pLevelSeed + pSalt;
        this.setSeed(i);
    }

    public static RandomSource seedSlimeChunk(int pChunkX, int pChunkZ, long pLevelSeed, long pSalt) {
        return RandomSource.create(
            pLevelSeed + pChunkX * pChunkX * 4987142 + pChunkX * 5947611 + pChunkZ * pChunkZ * 4392871L + pChunkZ * 389711 ^ pSalt
        );
    }

    public static enum Algorithm {
        LEGACY(LegacyRandomSource::new),
        XOROSHIRO(XoroshiroRandomSource::new);

        private final LongFunction<RandomSource> constructor;

        private Algorithm(final LongFunction<RandomSource> pConstructor) {
            this.constructor = pConstructor;
        }

        public RandomSource newInstance(long pSeed) {
            return this.constructor.apply(pSeed);
        }
    }
}