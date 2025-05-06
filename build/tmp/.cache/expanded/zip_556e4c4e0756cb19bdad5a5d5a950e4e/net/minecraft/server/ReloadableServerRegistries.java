package net.minecraft.server;

import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Stream;
import net.minecraft.Util;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagLoader;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import org.slf4j.Logger;

public class ReloadableServerRegistries {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final RegistrationInfo DEFAULT_REGISTRATION_INFO = new RegistrationInfo(Optional.empty(), Lifecycle.experimental());

    public static CompletableFuture<ReloadableServerRegistries.LoadResult> reload(
        LayeredRegistryAccess<RegistryLayer> pRegistryAccess, List<Registry.PendingTags<?>> pPostponedTags, ResourceManager pResourceManager, Executor pBackgroundExecutor
    ) {
        List<HolderLookup.RegistryLookup<?>> list = TagLoader.buildUpdatedLookups(pRegistryAccess.getAccessForLoading(RegistryLayer.RELOADABLE), pPostponedTags);
        HolderLookup.Provider holderlookup$provider = HolderLookup.Provider.create(list.stream());
        RegistryOps<JsonElement> registryops = holderlookup$provider.createSerializationContext(JsonOps.INSTANCE);
        List<CompletableFuture<WritableRegistry<?>>> list1 = LootDataType.values()
            .map(p_358525_ -> scheduleRegistryLoad((LootDataType<?>)p_358525_, registryops, pResourceManager, pBackgroundExecutor))
            .toList();
        CompletableFuture<List<WritableRegistry<?>>> completablefuture = Util.sequence(list1);
        return completablefuture.thenApplyAsync(p_358521_ -> createAndValidateFullContext(pRegistryAccess, holderlookup$provider, (List<WritableRegistry<?>>)p_358521_), pBackgroundExecutor);
    }

    private static <T> CompletableFuture<WritableRegistry<?>> scheduleRegistryLoad(
        LootDataType<T> pLootDataType, RegistryOps<JsonElement> pOps, ResourceManager pResourceManager, Executor pBackgroundExecutor
    ) {
        return CompletableFuture.supplyAsync(() -> {
            WritableRegistry<T> writableregistry = new MappedRegistry<>(pLootDataType.registryKey(), Lifecycle.experimental());
            Map<ResourceLocation, T> map = new HashMap<>();
            SimpleJsonResourceReloadListener.scanDirectory(pResourceManager, pLootDataType.registryKey(), pOps, pLootDataType.codec(), map);
            map.forEach((p_332563_, p_332628_) -> writableregistry.register(ResourceKey.create(pLootDataType.registryKey(), p_332563_), (T)p_332628_, DEFAULT_REGISTRATION_INFO));
            TagLoader.loadTagsForRegistry(pResourceManager, writableregistry);
            return writableregistry;
        }, pBackgroundExecutor);
    }

    private static ReloadableServerRegistries.LoadResult createAndValidateFullContext(
        LayeredRegistryAccess<RegistryLayer> pRegistryAccess, HolderLookup.Provider pProvider, List<WritableRegistry<?>> pRegistries
    ) {
        LayeredRegistryAccess<RegistryLayer> layeredregistryaccess = createUpdatedRegistries(pRegistryAccess, pRegistries);
        HolderLookup.Provider holderlookup$provider = concatenateLookups(pProvider, layeredregistryaccess.getLayer(RegistryLayer.RELOADABLE));
        validateLootRegistries(holderlookup$provider);
        return new ReloadableServerRegistries.LoadResult(layeredregistryaccess, holderlookup$provider);
    }

    private static HolderLookup.Provider concatenateLookups(HolderLookup.Provider pLookup1, HolderLookup.Provider pLookup2) {
        return HolderLookup.Provider.create(Stream.concat(pLookup1.listRegistries(), pLookup2.listRegistries()));
    }

    private static void validateLootRegistries(HolderLookup.Provider pRegistries) {
        ProblemReporter.Collector problemreporter$collector = new ProblemReporter.Collector();
        ValidationContext validationcontext = new ValidationContext(problemreporter$collector, LootContextParamSets.ALL_PARAMS, pRegistries);
        LootDataType.values().forEach(p_358528_ -> validateRegistry(validationcontext, (LootDataType<?>)p_358528_, pRegistries));
        problemreporter$collector.get()
            .forEach((p_336191_, p_332871_) -> LOGGER.warn("Found loot table element validation problem in {}: {}", p_336191_, p_332871_));
    }

    private static LayeredRegistryAccess<RegistryLayer> createUpdatedRegistries(LayeredRegistryAccess<RegistryLayer> pRegistryAccess, List<WritableRegistry<?>> pRegistries) {
        return pRegistryAccess.replaceFrom(RegistryLayer.RELOADABLE, new RegistryAccess.ImmutableRegistryAccess(pRegistries).freeze());
    }

    private static <T> void validateRegistry(ValidationContext pContext, LootDataType<T> pLootDataType, HolderLookup.Provider pRegistries) {
        HolderLookup<T> holderlookup = pRegistries.lookupOrThrow(pLootDataType.registryKey());
        holderlookup.listElements().forEach(p_334560_ -> pLootDataType.runValidation(pContext, p_334560_.key(), p_334560_.value()));
    }

    public static class Holder {
        private final HolderLookup.Provider registries;

        public Holder(HolderLookup.Provider pRegistries) {
            this.registries = pRegistries;
        }

        public HolderGetter.Provider lookup() {
            return this.registries;
        }

        public Collection<ResourceLocation> getKeys(ResourceKey<? extends Registry<?>> pRegistryKey) {
            return this.registries.lookupOrThrow(pRegistryKey).listElementIds().map(ResourceKey::location).toList();
        }

        public LootTable getLootTable(ResourceKey<LootTable> pLootTableKey) {
            return this.registries
                .lookup(Registries.LOOT_TABLE)
                .flatMap(p_328118_ -> p_328118_.get(pLootTableKey))
                .map(net.minecraft.core.Holder::value)
                .orElse(LootTable.EMPTY);
        }
    }

    public record LoadResult(LayeredRegistryAccess<RegistryLayer> layers, HolderLookup.Provider lookupWithUpdatedTags) {
    }
}