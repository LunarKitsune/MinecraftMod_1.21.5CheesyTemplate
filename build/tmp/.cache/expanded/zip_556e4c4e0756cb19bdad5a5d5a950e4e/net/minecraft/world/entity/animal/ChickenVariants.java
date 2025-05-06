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

public class ChickenVariants {
    public static final ResourceKey<ChickenVariant> TEMPERATE = createKey(TemperatureVariants.TEMPERATE);
    public static final ResourceKey<ChickenVariant> WARM = createKey(TemperatureVariants.WARM);
    public static final ResourceKey<ChickenVariant> COLD = createKey(TemperatureVariants.COLD);
    public static final ResourceKey<ChickenVariant> DEFAULT = TEMPERATE;

    private static ResourceKey<ChickenVariant> createKey(ResourceLocation pName) {
        return ResourceKey.create(Registries.CHICKEN_VARIANT, pName);
    }

    public static void bootstrap(BootstrapContext<ChickenVariant> pContext) {
        register(pContext, TEMPERATE, ChickenVariant.ModelType.NORMAL, "temperate_chicken", SpawnPrioritySelectors.fallback(0));
        register(pContext, WARM, ChickenVariant.ModelType.NORMAL, "warm_chicken", BiomeTags.SPAWNS_WARM_VARIANT_FARM_ANIMALS);
        register(pContext, COLD, ChickenVariant.ModelType.COLD, "cold_chicken", BiomeTags.SPAWNS_COLD_VARIANT_FARM_ANIMALS);
    }

    private static void register(
        BootstrapContext<ChickenVariant> pContext,
        ResourceKey<ChickenVariant> pKey,
        ChickenVariant.ModelType pModelType,
        String pName,
        TagKey<Biome> pBiomes
    ) {
        HolderSet<Biome> holderset = pContext.lookup(Registries.BIOME).getOrThrow(pBiomes);
        register(pContext, pKey, pModelType, pName, SpawnPrioritySelectors.single(new BiomeCheck(holderset), 1));
    }

    private static void register(
        BootstrapContext<ChickenVariant> pContext,
        ResourceKey<ChickenVariant> pKey,
        ChickenVariant.ModelType pModelType,
        String pName,
        SpawnPrioritySelectors pSpawnConditions
    ) {
        ResourceLocation resourcelocation = ResourceLocation.withDefaultNamespace("entity/chicken/" + pName);
        pContext.register(pKey, new ChickenVariant(new ModelAndTexture<>(pModelType, resourcelocation), pSpawnConditions));
    }

    public static Optional<Holder.Reference<ChickenVariant>> selectVariantToSpawn(RandomSource pRandom, RegistryAccess pRegistryAccess, SpawnContext pSpawnContext) {
        return PriorityProvider.pick(pRegistryAccess.lookupOrThrow(Registries.CHICKEN_VARIANT).listElements(), Holder::value, pRandom, pSpawnContext);
    }
}