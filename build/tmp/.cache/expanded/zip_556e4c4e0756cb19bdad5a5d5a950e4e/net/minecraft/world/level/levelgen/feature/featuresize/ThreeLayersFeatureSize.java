package net.minecraft.world.level.levelgen.feature.featuresize;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.OptionalInt;

public class ThreeLayersFeatureSize extends FeatureSize {
    public static final MapCodec<ThreeLayersFeatureSize> CODEC = RecordCodecBuilder.mapCodec(
        p_68326_ -> p_68326_.group(
                Codec.intRange(0, 80).fieldOf("limit").orElse(1).forGetter(p_161335_ -> p_161335_.limit),
                Codec.intRange(0, 80).fieldOf("upper_limit").orElse(1).forGetter(p_161333_ -> p_161333_.upperLimit),
                Codec.intRange(0, 16).fieldOf("lower_size").orElse(0).forGetter(p_161331_ -> p_161331_.lowerSize),
                Codec.intRange(0, 16).fieldOf("middle_size").orElse(1).forGetter(p_161329_ -> p_161329_.middleSize),
                Codec.intRange(0, 16).fieldOf("upper_size").orElse(1).forGetter(p_161327_ -> p_161327_.upperSize),
                minClippedHeightCodec()
            )
            .apply(p_68326_, ThreeLayersFeatureSize::new)
    );
    private final int limit;
    private final int upperLimit;
    private final int lowerSize;
    private final int middleSize;
    private final int upperSize;

    public ThreeLayersFeatureSize(int pLimit, int pUpperLimit, int pLowerSize, int pMiddleSize, int pUpperSize, OptionalInt pMinClippedHeight) {
        super(pMinClippedHeight);
        this.limit = pLimit;
        this.upperLimit = pUpperLimit;
        this.lowerSize = pLowerSize;
        this.middleSize = pMiddleSize;
        this.upperSize = pUpperSize;
    }

    @Override
    protected FeatureSizeType<?> type() {
        return FeatureSizeType.THREE_LAYERS_FEATURE_SIZE;
    }

    @Override
    public int getSizeAtHeight(int p_68321_, int p_68322_) {
        if (p_68322_ < this.limit) {
            return this.lowerSize;
        } else {
            return p_68322_ >= p_68321_ - this.upperLimit ? this.upperSize : this.middleSize;
        }
    }
}