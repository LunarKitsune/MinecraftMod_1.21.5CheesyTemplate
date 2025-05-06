package net.minecraft.world.entity.animal;

import java.util.Optional;
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
import net.minecraft.world.entity.variant.BiomeCheck;
import net.minecraft.world.entity.variant.ModelAndTexture;
import net.minecraft.world.entity.variant.PriorityProvider;
import net.minecraft.world.entity.variant.SpawnContext;
import net.minecraft.world.entity.variant.SpawnPrioritySelectors;
import net.minecraft.world.level.biome.Biome;

public class PigVariants {
    public static final ResourceKey<PigVariant> TEMPERATE = createKey(TemperatureVariants.TEMPERATE);
    public static final ResourceKey<PigVariant> WARM = createKey(TemperatureVariants.WARM);
    public static final ResourceKey<PigVariant> COLD = createKey(TemperatureVariants.COLD);
    public static final ResourceKey<PigVariant> DEFAULT = TEMPERATE;

    private static ResourceKey<PigVariant> createKey(ResourceLocation pName) {
        return ResourceKey.create(Registries.PIG_VARIANT, pName);
    }

    public static void bootstrap(BootstrapContext<PigVariant> pContext) {
        register(pContext, TEMPERATE, PigVariant.ModelType.NORMAL, "temperate_pig", SpawnPrioritySelectors.fallback(0));
        register(pContext, WARM, PigVariant.ModelType.NORMAL, "warm_pig", BiomeTags.SPAWNS_WARM_VARIANT_FARM_ANIMALS);
        register(pContext, COLD, PigVariant.ModelType.COLD, "cold_pig", BiomeTags.SPAWNS_COLD_VARIANT_FARM_ANIMALS);
    }

    private static void register(
        BootstrapContext<PigVariant> pContext, ResourceKey<PigVariant> pKey, PigVariant.ModelType pModelType, String pName, TagKey<Biome> pBiomes
    ) {
        HolderSet<Biome> holderset = pContext.lookup(Registries.BIOME).getOrThrow(pBiomes);
        register(pContext, pKey, pModelType, pName, SpawnPrioritySelectors.single(new BiomeCheck(holderset), 1));
    }

    private static void register(
        BootstrapContext<PigVariant> pContext,
        ResourceKey<PigVariant> pKey,
        PigVariant.ModelType pModelType,
        String pName,
        SpawnPrioritySelectors pSpawnConditions
    ) {
        ResourceLocation resourcelocation = ResourceLocation.withDefaultNamespace("entity/pig/" + pName);
        pContext.register(pKey, new PigVariant(new ModelAndTexture<>(pModelType, resourcelocation), pSpawnConditions));
    }

    public static Optional<Holder.Reference<PigVariant>> selectVariantToSpawn(RandomSource pRandom, RegistryAccess pRegistryAccess, SpawnContext pContext) {
        return PriorityProvider.pick(pRegistryAccess.lookupOrThrow(Registries.PIG_VARIANT).listElements(), Holder::value, pRandom, pContext);
    }
}