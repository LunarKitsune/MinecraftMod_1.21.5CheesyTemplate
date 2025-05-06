package net.minecraft.world.level.levelgen.structure.pools.alias;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.worldgen.Pools;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.Weighted;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

public interface PoolAliasBinding {
    Codec<PoolAliasBinding> CODEC = BuiltInRegistries.POOL_ALIAS_BINDING_TYPE.byNameCodec().dispatch(PoolAliasBinding::codec, Function.identity());

    void forEachResolved(RandomSource pRandom, BiConsumer<ResourceKey<StructureTemplatePool>, ResourceKey<StructureTemplatePool>> pStucturePoolKey);

    Stream<ResourceKey<StructureTemplatePool>> allTargets();

    static DirectPoolAlias direct(String pAlias, String pTarget) {
        return direct(Pools.createKey(pAlias), Pools.createKey(pTarget));
    }

    static DirectPoolAlias direct(ResourceKey<StructureTemplatePool> pAlias, ResourceKey<StructureTemplatePool> pTarget) {
        return new DirectPoolAlias(pAlias, pTarget);
    }

    static RandomPoolAlias random(String pAlias, WeightedList<String> pTargets) {
        WeightedList.Builder<ResourceKey<StructureTemplatePool>> builder = WeightedList.builder();
        pTargets.unwrap().forEach(p_391073_ -> builder.add(Pools.createKey(p_391073_.value()), p_391073_.weight()));
        return random(Pools.createKey(pAlias), builder.build());
    }

    static RandomPoolAlias random(ResourceKey<StructureTemplatePool> pAlias, WeightedList<ResourceKey<StructureTemplatePool>> pTargets) {
        return new RandomPoolAlias(pAlias, pTargets);
    }

    static RandomGroupPoolAlias randomGroup(WeightedList<List<PoolAliasBinding>> pGroups) {
        return new RandomGroupPoolAlias(pGroups);
    }

    MapCodec<? extends PoolAliasBinding> codec();
}