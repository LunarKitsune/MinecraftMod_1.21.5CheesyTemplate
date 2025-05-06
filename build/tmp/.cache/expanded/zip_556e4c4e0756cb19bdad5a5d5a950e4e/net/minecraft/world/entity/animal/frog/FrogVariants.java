package net.minecraft.world.entity.animal.frog;

import java.util.Optional;
import net.minecraft.core.ClientAsset;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.animal.TemperatureVariants;
import net.minecraft.world.entity.variant.BiomeCheck;
import net.minecraft.world.entity.variant.PriorityProvider;
import net.minecraft.world.entity.variant.SpawnContext;
import net.minecraft.world.entity.variant.SpawnPrioritySelectors;
import net.minecraft.world.level.biome.Biome;

public interface FrogVariants {
    ResourceKey<FrogVariant> TEMPERATE = createKey(TemperatureVariants.TEMPERATE);
    ResourceKey<FrogVariant> WARM = createKey(TemperatureVariants.WARM);
    ResourceKey<FrogVariant> COLD = createKey(TemperatureVariants.COLD);

    private static ResourceKey<FrogVariant> createKey(ResourceLocation pName) {
        return ResourceKey.create(Registries.FROG_VARIANT, pName);
    }

    static void bootstrap(BootstrapContext<FrogVariant> pContext) {
        register(pContext, TEMPERATE, "entity/frog/temperate_frog", SpawnPrioritySelectors.fallback(0));
        register(pContext, WARM, "entity/frog/warm_frog", BiomeTags.SPAWNS_WARM_VARIANT_FROGS);
        register(pContext, COLD, "entity/frog/cold_frog", BiomeTags.SPAWNS_COLD_VARIANT_FROGS);
    }

    private static void register(BootstrapContext<FrogVariant> pContext, ResourceKey<FrogVariant> pKey, String pName, TagKey<Biome> pBiome) {
        HolderSet<Biome> holderset = pContext.lookup(Registries.BIOME).getOrThrow(pBiome);
        register(pContext, pKey, pName, SpawnPrioritySelectors.single(new BiomeCheck(holderset), 1));
    }

    private static void register(
        BootstrapContext<FrogVariant> pContext, ResourceKey<FrogVariant> pKey, String pName, SpawnPrioritySelectors pSpawnCondiftions
    ) {
        pContext.register(pKey, new FrogVariant(new ClientAsset(ResourceLocation.withDefaultNamespace(pName)), pSpawnCondiftions));
    }

    static Optional<Holder.Reference<FrogVariant>> selectVariantToSpawn(RandomSource pSource, RegistryAccess pRegistryAccess, SpawnContext pContext) {
        return PriorityProvider.pick(pRegistryAccess.lookupOrThrow(Registries.FROG_VARIANT).listElements(), Holder::value, pSource, pContext);
    }
}