package net.minecraft.world.level.levelgen.structure.pools.alias;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.Pools;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

public class PoolAliasBindings {
    public static MapCodec<? extends PoolAliasBinding> bootstrap(Registry<MapCodec<? extends PoolAliasBinding>> pRegistry) {
        Registry.register(pRegistry, "random", RandomPoolAlias.CODEC);
        Registry.register(pRegistry, "random_group", RandomGroupPoolAlias.CODEC);
        return Registry.register(pRegistry, "direct", DirectPoolAlias.CODEC);
    }

    public static void registerTargetsAsPools(BootstrapContext<StructureTemplatePool> pContext, Holder<StructureTemplatePool> pPool, List<PoolAliasBinding> pPoolAliasBindings) {
        pPoolAliasBindings.stream()
            .flatMap(PoolAliasBinding::allTargets)
            .map(p_312426_ -> p_312426_.location().getPath())
            .forEach(
                p_327483_ -> Pools.register(
                    pContext,
                    p_327483_,
                    new StructureTemplatePool(pPool, List.of(Pair.of(StructurePoolElement.single(p_327483_), 1)), StructureTemplatePool.Projection.RIGID)
                )
            );
    }
}