package net.minecraft.server.packs.resources;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.DataResult.Error;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

public abstract class SimpleJsonResourceReloadListener<T> extends SimplePreparableReloadListener<Map<ResourceLocation, T>> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final DynamicOps<JsonElement> ops;
    private final Codec<T> codec;
    private final FileToIdConverter lister;

    protected SimpleJsonResourceReloadListener(HolderLookup.Provider pProvider, Codec<T> pCodec, ResourceKey<? extends Registry<T>> pRegistryKey) {
        this(pProvider.createSerializationContext(JsonOps.INSTANCE), pCodec, FileToIdConverter.registry(pRegistryKey));
    }

    protected SimpleJsonResourceReloadListener(HolderLookup.Provider pProvider, Codec<T> pCodec, ResourceKey<? extends Registry<T>> pRegistryKey, net.minecraftforge.common.crafting.conditions.ICondition.IContext context) {
        this(context.wrap(pProvider.createSerializationContext(JsonOps.INSTANCE)), pCodec, FileToIdConverter.registry(pRegistryKey));
    }

    protected SimpleJsonResourceReloadListener(Codec<T> pCodec, FileToIdConverter pLister) {
        this(JsonOps.INSTANCE, pCodec, pLister);
    }

    protected SimpleJsonResourceReloadListener(DynamicOps<JsonElement> pOps, Codec<T> pCodec, FileToIdConverter pLister) {
        this.ops = pOps;
        this.codec = pCodec;
        this.lister = pLister;
    }

    protected Map<ResourceLocation, T> prepare(ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        Map<ResourceLocation, T> map = new HashMap<>();
        scanDirectory(pResourceManager, this.lister, this.ops, this.codec, map);
        return map;
    }

    public static <T> void scanDirectory(
        ResourceManager pResourceManager,
        ResourceKey<? extends Registry<T>> pRegistryKey,
        DynamicOps<JsonElement> pOps,
        Codec<T> pCodec,
        Map<ResourceLocation, T> pOutput
    ) {
        scanDirectory(pResourceManager, FileToIdConverter.registry(pRegistryKey), pOps, pCodec, pOutput);
    }

    public static <T> void scanDirectory(
        ResourceManager pResourceManager, FileToIdConverter pLister, DynamicOps<JsonElement> pOps, Codec<T> pCodec, Map<ResourceLocation, T> pOutput
    ) {
        for (Entry<ResourceLocation, Resource> entry : pLister.listMatchingResources(pResourceManager).entrySet()) {
            ResourceLocation resourcelocation = entry.getKey();
            ResourceLocation resourcelocation1 = pLister.fileToId(resourcelocation);

            try (Reader reader = entry.getValue().openAsReader()) {
                var json = JsonParser.parseReader(reader);
                json = net.minecraftforge.common.ForgeHooks.readConditional(pOps, json);
                if (json == null) {
                    LOGGER.debug("Skipping loading {} as its conditions were not met", resourcelocation);
                    continue;
                }
                pCodec.parse(pOps, json).ifSuccess(p_370131_ -> {
                    p_370131_ = net.minecraftforge.common.ForgeHooks.onJsonDataParsed(pCodec, resourcelocation1, p_370131_);
                    if (p_370131_ == null) return;
                    if (pOutput.putIfAbsent(resourcelocation1, (T)p_370131_) != null) {
                        throw new IllegalStateException("Duplicate data file ignored with ID " + resourcelocation1);
                    }
                }).ifError(p_362245_ -> LOGGER.error("Couldn't parse data file '{}' from '{}': {}", resourcelocation1, resourcelocation, p_362245_));
            } catch (IllegalArgumentException | IOException | JsonParseException jsonparseexception) {
                LOGGER.error("Couldn't parse data file '{}' from '{}'", resourcelocation1, resourcelocation, jsonparseexception);
            }
        }
    }
}
