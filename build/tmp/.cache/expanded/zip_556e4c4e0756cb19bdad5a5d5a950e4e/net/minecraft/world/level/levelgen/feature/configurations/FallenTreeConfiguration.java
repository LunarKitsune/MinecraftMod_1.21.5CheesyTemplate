package net.minecraft.world.level.levelgen.feature.configurations;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator;

public class FallenTreeConfiguration implements FeatureConfiguration {
    public static final Codec<FallenTreeConfiguration> CODEC = RecordCodecBuilder.create(
        p_391895_ -> p_391895_.group(
                BlockStateProvider.CODEC.fieldOf("trunk_provider").forGetter(p_391875_ -> p_391875_.trunkProvider),
                IntProvider.codec(0, 16).fieldOf("log_length").forGetter(p_397602_ -> p_397602_.logLength),
                TreeDecorator.CODEC.listOf().fieldOf("stump_decorators").forGetter(p_393281_ -> p_393281_.stumpDecorators),
                TreeDecorator.CODEC.listOf().fieldOf("log_decorators").forGetter(p_393303_ -> p_393303_.logDecorators)
            )
            .apply(p_391895_, FallenTreeConfiguration::new)
    );
    public final BlockStateProvider trunkProvider;
    public final IntProvider logLength;
    public final List<TreeDecorator> stumpDecorators;
    public final List<TreeDecorator> logDecorators;

    protected FallenTreeConfiguration(BlockStateProvider pTrunkProvider, IntProvider pLogLength, List<TreeDecorator> pStumpDecorators, List<TreeDecorator> pLogDecorators) {
        this.trunkProvider = pTrunkProvider;
        this.logLength = pLogLength;
        this.stumpDecorators = pStumpDecorators;
        this.logDecorators = pLogDecorators;
    }

    public static class FallenTreeConfigurationBuilder {
        private final BlockStateProvider trunkProvider;
        private final IntProvider logLength;
        private List<TreeDecorator> stumpDecorators = new ArrayList<>();
        private List<TreeDecorator> logDecorators = new ArrayList<>();

        public FallenTreeConfigurationBuilder(BlockStateProvider pTrunkProvider, IntProvider pLogLength) {
            this.trunkProvider = pTrunkProvider;
            this.logLength = pLogLength;
        }

        public FallenTreeConfiguration.FallenTreeConfigurationBuilder stumpDecorators(List<TreeDecorator> pStumpDecorators) {
            this.stumpDecorators = pStumpDecorators;
            return this;
        }

        public FallenTreeConfiguration.FallenTreeConfigurationBuilder logDecorators(List<TreeDecorator> pLogDecorators) {
            this.logDecorators = pLogDecorators;
            return this;
        }

        public FallenTreeConfiguration build() {
            return new FallenTreeConfiguration(this.trunkProvider, this.logLength, this.stumpDecorators, this.logDecorators);
        }
    }
}