package net.minecraft.world.level.levelgen.feature.stateproviders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

public class NoiseThresholdProvider extends NoiseBasedStateProvider {
    public static final MapCodec<NoiseThresholdProvider> CODEC = RecordCodecBuilder.mapCodec(
        p_191486_ -> noiseCodec(p_191486_)
            .and(
                p_191486_.group(
                    Codec.floatRange(-1.0F, 1.0F).fieldOf("threshold").forGetter(p_191494_ -> p_191494_.threshold),
                    Codec.floatRange(0.0F, 1.0F).fieldOf("high_chance").forGetter(p_191492_ -> p_191492_.highChance),
                    BlockState.CODEC.fieldOf("default_state").forGetter(p_191490_ -> p_191490_.defaultState),
                    ExtraCodecs.nonEmptyList(BlockState.CODEC.listOf()).fieldOf("low_states").forGetter(p_191488_ -> p_191488_.lowStates),
                    ExtraCodecs.nonEmptyList(BlockState.CODEC.listOf()).fieldOf("high_states").forGetter(p_191481_ -> p_191481_.highStates)
                )
            )
            .apply(p_191486_, NoiseThresholdProvider::new)
    );
    private final float threshold;
    private final float highChance;
    private final BlockState defaultState;
    private final List<BlockState> lowStates;
    private final List<BlockState> highStates;

    public NoiseThresholdProvider(
        long pSeed,
        NormalNoise.NoiseParameters pParameters,
        float pScale,
        float pThreshold,
        float pHighChance,
        BlockState pDefaultState,
        List<BlockState> pLowStates,
        List<BlockState> pHighStates
    ) {
        super(pSeed, pParameters, pScale);
        this.threshold = pThreshold;
        this.highChance = pHighChance;
        this.defaultState = pDefaultState;
        this.lowStates = pLowStates;
        this.highStates = pHighStates;
    }

    @Override
    protected BlockStateProviderType<?> type() {
        return BlockStateProviderType.NOISE_THRESHOLD_PROVIDER;
    }

    @Override
    public BlockState getState(RandomSource p_225916_, BlockPos p_225917_) {
        double d0 = this.getNoiseValue(p_225917_, this.scale);
        if (d0 < this.threshold) {
            return Util.getRandom(this.lowStates, p_225916_);
        } else {
            return p_225916_.nextFloat() < this.highChance ? Util.getRandom(this.highStates, p_225916_) : this.defaultState;
        }
    }
}