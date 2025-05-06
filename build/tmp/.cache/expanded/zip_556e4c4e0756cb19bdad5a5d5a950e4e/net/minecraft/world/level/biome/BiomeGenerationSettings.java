package net.minecraft.world.level.biome;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.slf4j.Logger;

public class BiomeGenerationSettings {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final BiomeGenerationSettings EMPTY = new BiomeGenerationSettings(HolderSet.direct(), List.of());
    public static final MapCodec<BiomeGenerationSettings> CODEC = RecordCodecBuilder.mapCodec(
        p_359960_ -> p_359960_.group(
                ConfiguredWorldCarver.LIST_CODEC
                    .promotePartial(Util.prefix("Carver: ", LOGGER::error))
                    .fieldOf("carvers")
                    .forGetter(p_359961_ -> p_359961_.carvers),
                PlacedFeature.LIST_OF_LISTS_CODEC
                    .promotePartial(Util.prefix("Features: ", LOGGER::error))
                    .fieldOf("features")
                    .forGetter(p_186653_ -> p_186653_.features)
            )
            .apply(p_359960_, BiomeGenerationSettings::new)
    );
    private final HolderSet<ConfiguredWorldCarver<?>> carvers;
    private final List<HolderSet<PlacedFeature>> features;
    private final Supplier<List<ConfiguredFeature<?, ?>>> flowerFeatures;
    private final Supplier<Set<PlacedFeature>> featureSet;

    BiomeGenerationSettings(HolderSet<ConfiguredWorldCarver<?>> pCarvers, List<HolderSet<PlacedFeature>> pFeatures) {
        this.carvers = pCarvers;
        this.features = pFeatures;
        this.flowerFeatures = Suppliers.memoize(
            () -> pFeatures.stream()
                .flatMap(HolderSet::stream)
                .map(Holder::value)
                .flatMap(PlacedFeature::getFeatures)
                .filter(p_186657_ -> p_186657_.feature() == Feature.FLOWER)
                .collect(ImmutableList.toImmutableList())
        );
        this.featureSet = Suppliers.memoize(() -> pFeatures.stream().flatMap(HolderSet::stream).map(Holder::value).collect(Collectors.toSet()));
    }

    public Iterable<Holder<ConfiguredWorldCarver<?>>> getCarvers() {
        return this.carvers;
    }

    public List<ConfiguredFeature<?, ?>> getFlowerFeatures() {
        return this.flowerFeatures.get();
    }

    public List<HolderSet<PlacedFeature>> features() {
        return this.features;
    }

    public boolean hasFeature(PlacedFeature pFeature) {
        return this.featureSet.get().contains(pFeature);
    }

    public static class Builder extends BiomeGenerationSettings.PlainBuilder {
        private final HolderGetter<PlacedFeature> placedFeatures;
        private final HolderGetter<ConfiguredWorldCarver<?>> worldCarvers;

        public Builder(HolderGetter<PlacedFeature> pPlacedFeatures, HolderGetter<ConfiguredWorldCarver<?>> pWorldCarvers) {
            this.placedFeatures = pPlacedFeatures;
            this.worldCarvers = pWorldCarvers;
        }

        public BiomeGenerationSettings.Builder addFeature(GenerationStep.Decoration pDecoration, ResourceKey<PlacedFeature> pFeature) {
            this.addFeature(pDecoration.ordinal(), this.placedFeatures.getOrThrow(pFeature));
            return this;
        }

        public BiomeGenerationSettings.Builder addCarver(ResourceKey<ConfiguredWorldCarver<?>> pCarver) {
            this.addCarver(this.worldCarvers.getOrThrow(pCarver));
            return this;
        }
    }

    public static class PlainBuilder {
        protected final List<Holder<ConfiguredWorldCarver<?>>> carvers = new ArrayList<>();
        protected final List<List<Holder<PlacedFeature>>> features = new ArrayList<>();

        public BiomeGenerationSettings.PlainBuilder addFrom(BiomeGenerationSettings other) {
            for (var carver : other.getCarvers())
                addCarver(carver);
            var stages = other.features();
            for (int x = 0; x < stages.size(); x++) {
                for (var feature : stages.get(x))
                    addFeature(x, feature);
            }
            return this;
        }

        public BiomeGenerationSettings.PlainBuilder addFeature(GenerationStep.Decoration pDecoration, Holder<PlacedFeature> pFeature) {
            return this.addFeature(pDecoration.ordinal(), pFeature);
        }

        public BiomeGenerationSettings.PlainBuilder addFeature(int pStep, Holder<PlacedFeature> pFeature) {
            this.addFeatureStepsUpTo(pStep);
            this.features.get(pStep).add(pFeature);
            return this;
        }

        public List<Holder<PlacedFeature>> getFeatures(GenerationStep.Decoration stage) {
            this.addFeatureStepsUpTo(stage.ordinal());
            return this.features.get(stage.ordinal());
        }

        public BiomeGenerationSettings.PlainBuilder addCarver(Holder<ConfiguredWorldCarver<?>> pCarver) {
            this.carvers.add(pCarver);
            return this;
        }

        protected void addFeatureStepsUpTo(int pStep) {
            while (this.features.size() <= pStep) {
                this.features.add(Lists.newArrayList());
            }
        }

        public BiomeGenerationSettings build() {
            return new BiomeGenerationSettings(
                HolderSet.direct(this.carvers), this.features.stream().map(HolderSet::direct).collect(ImmutableList.toImmutableList())
            );
        }
    }
}
