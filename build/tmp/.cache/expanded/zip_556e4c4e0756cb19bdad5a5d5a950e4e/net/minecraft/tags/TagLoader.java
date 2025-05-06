package net.minecraft.tags;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SequencedSet;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.WritableRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.DependencySorter;
import org.slf4j.Logger;

public class TagLoader<T> {
    private static final Logger LOGGER = LogUtils.getLogger();
    final TagLoader.ElementLookup<T> elementLookup;
    private final String directory;

    public TagLoader(TagLoader.ElementLookup<T> pElementLookup, String pDirectory) {
        this.elementLookup = pElementLookup;
        this.directory = pDirectory;
    }

    public Map<ResourceLocation, List<TagLoader.EntryWithSource>> load(ResourceManager pResourceManager) {
        Map<ResourceLocation, List<TagLoader.EntryWithSource>> map = new HashMap<>();
        FileToIdConverter filetoidconverter = FileToIdConverter.json(this.directory);

        for (Entry<ResourceLocation, List<Resource>> entry : filetoidconverter.listMatchingResourceStacks(pResourceManager).entrySet()) {
            ResourceLocation resourcelocation = entry.getKey();
            ResourceLocation resourcelocation1 = filetoidconverter.fileToId(resourcelocation);

            for (Resource resource : entry.getValue()) {
                try (Reader reader = resource.openAsReader()) {
                    JsonElement jsonelement = JsonParser.parseReader(reader);
                    List<TagLoader.EntryWithSource> list = map.computeIfAbsent(resourcelocation1, p_215974_ -> new ArrayList<>());
                    TagFile tagfile = TagFile.CODEC.parse(new Dynamic<>(JsonOps.INSTANCE, jsonelement)).getOrThrow();
                    if (tagfile.replace()) {
                        list.clear();
                    }

                    String s = resource.sourcePackId();
                    tagfile.entries().forEach(p_215997_ -> list.add(new TagLoader.EntryWithSource(p_215997_, s)));
                    tagfile.remove().forEach(e -> list.add(new TagLoader.EntryWithSource(e, s, true)));
                } catch (Exception exception) {
                    LOGGER.error("Couldn't read tag list {} from {} in data pack {}", resourcelocation1, resourcelocation, resource.sourcePackId(), exception);
                }
            }
        }

        return map;
    }

    private Either<List<TagLoader.EntryWithSource>, List<T>> tryBuildTag(TagEntry.Lookup<T> pLookup, List<TagLoader.EntryWithSource> pEntries) {
        var builder = new java.util.LinkedHashSet<T>(); // Order is important, as ImmutableSet is ordered and some people rely on that. https://github.com/MinecraftForge/MinecraftForge/issues/9774
        List<TagLoader.EntryWithSource> list = new ArrayList<>();

        for (TagLoader.EntryWithSource tagloader$entrywithsource : pEntries) {
            if (!tagloader$entrywithsource.entry().build(pLookup, tagloader$entrywithsource.remove() ? builder::remove : builder::add)) {
                if (!tagloader$entrywithsource.remove()) // Treat all removals as optional at runtime. If it was missing, then it could of never been added.
                list.add(tagloader$entrywithsource);
            }
        }

        return list.isEmpty() ? Either.right(List.copyOf(builder)) : Either.left(list);
    }

    public Map<ResourceLocation, List<T>> build(Map<ResourceLocation, List<TagLoader.EntryWithSource>> pBuilders) {
        final Map<ResourceLocation, List<T>> map = new HashMap<>();
        TagEntry.Lookup<T> lookup = new TagEntry.Lookup<T>() {
            @Nullable
            @Override
            public T element(ResourceLocation p_216039_, boolean p_366980_) {
                return (T)TagLoader.this.elementLookup.get(p_216039_, p_366980_).orElse(null);
            }

            @Nullable
            @Override
            public Collection<T> tag(ResourceLocation p_216041_) {
                return map.get(p_216041_);
            }
        };
        DependencySorter<ResourceLocation, TagLoader.SortingEntry> dependencysorter = new DependencySorter<>();
        pBuilders.forEach(
            (p_284685_, p_284686_) -> dependencysorter.addEntry(p_284685_, new TagLoader.SortingEntry((List<TagLoader.EntryWithSource>)p_284686_))
        );
        dependencysorter.orderByDependencies(
            (p_358780_, p_358781_) -> this.tryBuildTag(lookup, p_358781_.entries)
                .ifLeft(
                    p_358772_ -> LOGGER.error(
                        "Couldn't load tag {} as it is missing following references: {}",
                        p_358780_,
                        p_358772_.stream().map(Objects::toString).collect(Collectors.joining(", \n\t"))
                    )
                )
                .ifRight(p_369415_ -> map.put(p_358780_, (List<T>)p_369415_))
        );
        return map;
    }

    public static <T> void loadTagsFromNetwork(TagNetworkSerialization.NetworkPayload pPayload, WritableRegistry<T> pRegistry) {
        pPayload.resolve(pRegistry).tags.forEach(pRegistry::bindTag);
    }

    public static List<Registry.PendingTags<?>> loadTagsForExistingRegistries(ResourceManager pResourceManager, RegistryAccess pRegistryAccess) {
        return pRegistryAccess.registries()
            .map(p_358777_ -> loadPendingTags(pResourceManager, p_358777_.value()))
            .flatMap(Optional::stream)
            .collect(Collectors.toUnmodifiableList());
    }

    public static <T> void loadTagsForRegistry(ResourceManager pResourceManager, WritableRegistry<T> pRegistry) {
        ResourceKey<? extends Registry<T>> resourcekey = pRegistry.key();
        TagLoader<Holder<T>> tagloader = new TagLoader<>(TagLoader.ElementLookup.fromWritableRegistry(pRegistry), Registries.tagsDirPath(resourcekey));
        tagloader.build(tagloader.load(pResourceManager))
            .forEach((p_358786_, p_358787_) -> pRegistry.bindTag(TagKey.create(resourcekey, p_358786_), (List<Holder<T>>)p_358787_));
    }

    private static <T> Map<TagKey<T>, List<Holder<T>>> wrapTags(ResourceKey<? extends Registry<T>> pRegistryKey, Map<ResourceLocation, List<Holder<T>>> pTags) {
        return pTags.entrySet()
            .stream()
            .collect(Collectors.toUnmodifiableMap(p_358783_ -> TagKey.create(pRegistryKey, p_358783_.getKey()), Entry::getValue));
    }

    private static <T> Optional<Registry.PendingTags<T>> loadPendingTags(ResourceManager pResourceManager, Registry<T> pRegistry) {
        ResourceKey<? extends Registry<T>> resourcekey = pRegistry.key();
        TagLoader<Holder<T>> tagloader = new TagLoader<>(
            (TagLoader.ElementLookup<Holder<T>>)TagLoader.ElementLookup.fromFrozenRegistry(pRegistry), Registries.tagsDirPath(resourcekey)
        );
        TagLoader.LoadResult<T> loadresult = new TagLoader.LoadResult<>(
            resourcekey, wrapTags(pRegistry.key(), tagloader.build(tagloader.load(pResourceManager)))
        );
        return loadresult.tags().isEmpty() ? Optional.empty() : Optional.of(pRegistry.prepareTagReload(loadresult));
    }

    public static List<HolderLookup.RegistryLookup<?>> buildUpdatedLookups(RegistryAccess.Frozen pRegistry, List<Registry.PendingTags<?>> pTags) {
        List<HolderLookup.RegistryLookup<?>> list = new ArrayList<>();
        pRegistry.registries().forEach(p_358775_ -> {
            Registry.PendingTags<?> pendingtags = findTagsForRegistry(pTags, p_358775_.key());
            list.add((HolderLookup.RegistryLookup<?>)(pendingtags != null ? pendingtags.lookup() : p_358775_.value()));
        });
        return list;
    }

    @Nullable
    private static Registry.PendingTags<?> findTagsForRegistry(List<Registry.PendingTags<?>> pTags, ResourceKey<? extends Registry<?>> pRegistryKey) {
        for (Registry.PendingTags<?> pendingtags : pTags) {
            if (pendingtags.key() == pRegistryKey) {
                return pendingtags;
            }
        }

        return null;
    }

    public interface ElementLookup<T> {
        Optional<? extends T> get(ResourceLocation pId, boolean pRequired);

        static <T> TagLoader.ElementLookup<? extends Holder<T>> fromFrozenRegistry(Registry<T> pRegistry) {
            return (p_367027_, p_367996_) -> pRegistry.get(p_367027_);
        }

        static <T> TagLoader.ElementLookup<Holder<T>> fromWritableRegistry(WritableRegistry<T> pRegistry) {
            HolderGetter<T> holdergetter = pRegistry.createRegistrationLookup();
            return (p_367634_, p_365243_) -> ((HolderGetter<T>)(p_365243_ ? holdergetter : pRegistry))
                .get(ResourceKey.create(pRegistry.key(), p_367634_));
        }
    }

    public static record EntryWithSource(TagEntry entry, String source, boolean remove) {
        public EntryWithSource(TagEntry entry, String source) {
            this(entry, source, false);
        }

        @Override
        public String toString() {
            return this.entry + " (from " + this.source + ")";
        }
    }

    public record LoadResult<T>(ResourceKey<? extends Registry<T>> key, Map<TagKey<T>, List<Holder<T>>> tags) {
    }

    record SortingEntry(List<TagLoader.EntryWithSource> entries) implements DependencySorter.Entry<ResourceLocation> {
        @Override
        public void visitRequiredDependencies(Consumer<ResourceLocation> p_285529_) {
            this.entries.forEach(p_285236_ -> p_285236_.entry.visitRequiredDependencies(p_285529_));
        }

        @Override
        public void visitOptionalDependencies(Consumer<ResourceLocation> p_285469_) {
            this.entries.forEach(p_284943_ -> p_284943_.entry.visitOptionalDependencies(p_285469_));
        }
    }
}
