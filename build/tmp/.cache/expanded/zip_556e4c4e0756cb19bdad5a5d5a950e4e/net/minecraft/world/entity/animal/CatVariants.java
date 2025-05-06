package net.minecraft.world.entity.animal;

import java.util.List;
import java.util.Optional;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.core.ClientAsset;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.StructureTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.variant.MoonBrightnessCheck;
import net.minecraft.world.entity.variant.PriorityProvider;
import net.minecraft.world.entity.variant.SpawnContext;
import net.minecraft.world.entity.variant.SpawnPrioritySelectors;
import net.minecraft.world.entity.variant.StructureCheck;
import net.minecraft.world.level.levelgen.structure.Structure;

public interface CatVariants {
    ResourceKey<CatVariant> TABBY = createKey("tabby");
    ResourceKey<CatVariant> BLACK = createKey("black");
    ResourceKey<CatVariant> RED = createKey("red");
    ResourceKey<CatVariant> SIAMESE = createKey("siamese");
    ResourceKey<CatVariant> BRITISH_SHORTHAIR = createKey("british_shorthair");
    ResourceKey<CatVariant> CALICO = createKey("calico");
    ResourceKey<CatVariant> PERSIAN = createKey("persian");
    ResourceKey<CatVariant> RAGDOLL = createKey("ragdoll");
    ResourceKey<CatVariant> WHITE = createKey("white");
    ResourceKey<CatVariant> JELLIE = createKey("jellie");
    ResourceKey<CatVariant> ALL_BLACK = createKey("all_black");

    private static ResourceKey<CatVariant> createKey(String pName) {
        return ResourceKey.create(Registries.CAT_VARIANT, ResourceLocation.withDefaultNamespace(pName));
    }

    static void bootstrap(BootstrapContext<CatVariant> pContext) {
        HolderGetter<Structure> holdergetter = pContext.lookup(Registries.STRUCTURE);
        registerForAnyConditions(pContext, TABBY, "entity/cat/tabby");
        registerForAnyConditions(pContext, BLACK, "entity/cat/black");
        registerForAnyConditions(pContext, RED, "entity/cat/red");
        registerForAnyConditions(pContext, SIAMESE, "entity/cat/siamese");
        registerForAnyConditions(pContext, BRITISH_SHORTHAIR, "entity/cat/british_shorthair");
        registerForAnyConditions(pContext, CALICO, "entity/cat/calico");
        registerForAnyConditions(pContext, PERSIAN, "entity/cat/persian");
        registerForAnyConditions(pContext, RAGDOLL, "entity/cat/ragdoll");
        registerForAnyConditions(pContext, WHITE, "entity/cat/white");
        registerForAnyConditions(pContext, JELLIE, "entity/cat/jellie");
        register(
            pContext,
            ALL_BLACK,
            "entity/cat/all_black",
            new SpawnPrioritySelectors(
                List.of(
                    new PriorityProvider.Selector<>(new StructureCheck(holdergetter.getOrThrow(StructureTags.CATS_SPAWN_AS_BLACK)), 1),
                    new PriorityProvider.Selector<>(new MoonBrightnessCheck(MinMaxBounds.Doubles.atLeast(0.9)), 0)
                )
            )
        );
    }

    private static void registerForAnyConditions(BootstrapContext<CatVariant> pContext, ResourceKey<CatVariant> pKey, String pName) {
        register(pContext, pKey, pName, SpawnPrioritySelectors.fallback(0));
    }

    private static void register(BootstrapContext<CatVariant> pContext, ResourceKey<CatVariant> pKey, String pName, SpawnPrioritySelectors pSpawnConditions) {
        pContext.register(pKey, new CatVariant(new ClientAsset(ResourceLocation.withDefaultNamespace(pName)), pSpawnConditions));
    }

    static Optional<Holder.Reference<CatVariant>> selectVariantToSpawn(RandomSource pRandom, RegistryAccess pRegistryAccess, SpawnContext pContext) {
        return PriorityProvider.pick(pRegistryAccess.lookupOrThrow(Registries.CAT_VARIANT).listElements(), Holder::value, pRandom, pContext);
    }
}