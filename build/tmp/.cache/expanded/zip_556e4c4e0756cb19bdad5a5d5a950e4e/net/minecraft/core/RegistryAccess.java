package net.minecraft.core;

import com.google.common.collect.ImmutableMap;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceKey;
import org.slf4j.Logger;

/**
 * The root level registry, essentially a registry of registries. It is also an access point, hence the name, for other
 * dynamic registries.
 */
public interface RegistryAccess extends HolderLookup.Provider {
    Logger LOGGER = LogUtils.getLogger();
    RegistryAccess.Frozen EMPTY = new RegistryAccess.ImmutableRegistryAccess(Map.of()).freeze();

    @Override
    <E> Optional<Registry<E>> lookup(ResourceKey<? extends Registry<? extends E>> p_256275_);

    default <E> Registry<E> lookupOrThrow(ResourceKey<? extends Registry<? extends E>> p_369484_) {
        return this.lookup(p_369484_).orElseThrow(() -> new IllegalStateException("Missing registry: " + p_369484_));
    }

    Stream<RegistryAccess.RegistryEntry<?>> registries();

    @Override
    default Stream<ResourceKey<? extends Registry<?>>> listRegistryKeys() {
        return this.registries().map(p_358094_ -> p_358094_.key);
    }

    static RegistryAccess.Frozen fromRegistryOfRegistries(final Registry<? extends Registry<?>> pRegistryOfRegistries) {
        return new RegistryAccess.Frozen() {
            @Override
            public <T> Optional<Registry<T>> lookup(ResourceKey<? extends Registry<? extends T>> p_206220_) {
                Registry<Registry<T>> registry = (Registry<Registry<T>>)pRegistryOfRegistries;
                return registry.getOptional((ResourceKey<Registry<T>>)p_206220_);
            }

            @Override
            public Stream<RegistryAccess.RegistryEntry<?>> registries() {
                return pRegistryOfRegistries.entrySet().stream().map(RegistryAccess.RegistryEntry::fromMapEntry);
            }

            @Override
            public RegistryAccess.Frozen freeze() {
                return this;
            }
        };
    }

    default RegistryAccess.Frozen freeze() {
        class FrozenAccess extends RegistryAccess.ImmutableRegistryAccess implements RegistryAccess.Frozen {
            protected FrozenAccess(final Stream<RegistryAccess.RegistryEntry<?>> p_252031_) {
                super(p_252031_);
            }
        }

        return new FrozenAccess(this.registries().map(RegistryAccess.RegistryEntry::freeze));
    }

    public interface Frozen extends RegistryAccess {
    }

    public static class ImmutableRegistryAccess implements RegistryAccess {
        private final Map<? extends ResourceKey<? extends Registry<?>>, ? extends Registry<?>> registries;

        public ImmutableRegistryAccess(List<? extends Registry<?>> pRegistries) {
            this.registries = pRegistries.stream().collect(Collectors.toUnmodifiableMap(Registry::key, p_247993_ -> p_247993_));
        }

        public ImmutableRegistryAccess(Map<? extends ResourceKey<? extends Registry<?>>, ? extends Registry<?>> pRegistries) {
            this.registries = Map.copyOf(pRegistries);
        }

        public ImmutableRegistryAccess(Stream<RegistryAccess.RegistryEntry<?>> pRegistries) {
            this.registries = pRegistries.collect(ImmutableMap.toImmutableMap(RegistryAccess.RegistryEntry::key, RegistryAccess.RegistryEntry::value));
        }

        @Override
        public <E> Optional<Registry<E>> lookup(ResourceKey<? extends Registry<? extends E>> p_206229_) {
            return Optional.ofNullable(this.registries.get(p_206229_)).map(p_206232_ -> (Registry<E>)p_206232_);
        }

        @Override
        public Stream<RegistryAccess.RegistryEntry<?>> registries() {
            return this.registries.entrySet().stream().map(RegistryAccess.RegistryEntry::fromMapEntry);
        }
    }

    public record RegistryEntry<T>(ResourceKey<? extends Registry<T>> key, Registry<T> value) {
        private static <T, R extends Registry<? extends T>> RegistryAccess.RegistryEntry<T> fromMapEntry(
            Entry<? extends ResourceKey<? extends Registry<?>>, R> pMapEntry
        ) {
            return fromUntyped((ResourceKey<? extends Registry<?>>)pMapEntry.getKey(), pMapEntry.getValue());
        }

        private static <T> RegistryAccess.RegistryEntry<T> fromUntyped(ResourceKey<? extends Registry<?>> pKey, Registry<?> pValue) {
            return new RegistryAccess.RegistryEntry<>((ResourceKey<? extends Registry<T>>)pKey, (Registry<T>)pValue);
        }

        private RegistryAccess.RegistryEntry<T> freeze() {
            return new RegistryAccess.RegistryEntry<>(this.key, this.value.freeze());
        }
    }
}