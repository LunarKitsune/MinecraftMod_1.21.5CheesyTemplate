package net.minecraft.util.valueproviders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public class ClampedNormalInt extends IntProvider {
    public static final MapCodec<ClampedNormalInt> CODEC = RecordCodecBuilder.<ClampedNormalInt>mapCodec(
            p_185887_ -> p_185887_.group(
                    Codec.FLOAT.fieldOf("mean").forGetter(p_185905_ -> p_185905_.mean),
                    Codec.FLOAT.fieldOf("deviation").forGetter(p_185903_ -> p_185903_.deviation),
                    Codec.INT.fieldOf("min_inclusive").forGetter(p_326736_ -> p_326736_.minInclusive),
                    Codec.INT.fieldOf("max_inclusive").forGetter(p_326737_ -> p_326737_.maxInclusive)
                )
                .apply(p_185887_, ClampedNormalInt::new)
        )
        .validate(
            p_326735_ -> p_326735_.maxInclusive < p_326735_.minInclusive
                ? DataResult.error(() -> "Max must be larger than min: [" + p_326735_.minInclusive + ", " + p_326735_.maxInclusive + "]")
                : DataResult.success(p_326735_)
        );
    private final float mean;
    private final float deviation;
    private final int minInclusive;
    private final int maxInclusive;

    public static ClampedNormalInt of(float pMean, float pDeviation, int pMinInclusive, int pMaxInclusive) {
        return new ClampedNormalInt(pMean, pDeviation, pMinInclusive, pMaxInclusive);
    }

    private ClampedNormalInt(float pMean, float pDeviation, int pMinInclusive, int pMaxInclusive) {
        this.mean = pMean;
        this.deviation = pDeviation;
        this.minInclusive = pMinInclusive;
        this.maxInclusive = pMaxInclusive;
    }

    @Override
    public int sample(RandomSource p_216844_) {
        return sample(p_216844_, this.mean, this.deviation, this.minInclusive, this.maxInclusive);
    }

    public static int sample(RandomSource pRandom, float pMean, float pDeviation, float pMinInclusive, float pMaxInclusive) {
        return (int)Mth.clamp(Mth.normal(pRandom, pMean, pDeviation), pMinInclusive, pMaxInclusive);
    }

    @Override
    public int getMinValue() {
        return this.minInclusive;
    }

    @Override
    public int getMaxValue() {
        return this.maxInclusive;
    }

    @Override
    public IntProviderType<?> getType() {
        return IntProviderType.CLAMPED_NORMAL;
    }

    @Override
    public String toString() {
        return "normal(" + this.mean + ", " + this.deviation + ") in [" + this.minInclusive + "-" + this.maxInclusive + "]";
    }
}