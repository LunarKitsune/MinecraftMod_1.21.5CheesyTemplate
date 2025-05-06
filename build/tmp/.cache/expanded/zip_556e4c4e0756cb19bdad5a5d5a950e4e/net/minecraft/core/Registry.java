package net.minecraft.core;

import com.mojang.datafixers.DataFixUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Keyable;
import com.mojang.serialization.Lifecycle;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagLoader;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;

public interface Registry<T> extends Keyable, HolderLookup.RegistryLookup<T>, IdMap<T> {
    @Override
    ResourceKey<? extends Registry<T>> key();

    default Codec<T> byNameCodec() {
        return this.referenceHolderWithLifecycle().flatComapMap(Holder.Reference::value, p_325680_ -> this.safeCastToReference(this.wrapAsHolder((T)p_325680_)));
    }

    default Codec<Holder<T>> holderByNameCodec() {
        return this.referenceHolderWithLifecycle().flatComapMap(p_325683_ -> (Holder<T>)p_325683_, this::safeCastToReference);
    }

    private Codec<Holder.Reference<T>> referenceHolderWithLifecycle() {
        Codec<Holder.Reference<T>> codec = ResourceLocation.CODEC
            .comapFlatMap(
                p_358093_ -> this.get(p_358093_)
                    .map(DataResult::success)
                    .orElseGet(() -> DataResult.error(() -> "Unknown registry key in " + this.key() + ": " + p_358093_)),
                p_325675_ -> p_325675_.key().location()
            );
        return ExtraCodecs.overrideLifecycle(codec, p_325682_ -> this.registrationInfo(p_325682_.key()).map(RegistrationInfo::lifecycle).orElse(Lifecycle.experimental()));
    }

    private DataResult<Holder.Reference<T>> safeCastToReference(Holder<T> pValue) {
        return pValue instanceof Holder.Reference<T> reference
            ? DataResult.success(reference)
            : DataResult.error(() -> "Unregistered holder in " + this.key() + ": " + pValue);
    }

    @Override
    default <U> Stream<U> keys(DynamicOps<U> pOps) {
        return this.keySet().stream().map(p_235784_ -> pOps.createString(p_235784_.toString()));
    }

    @Nullable
    ResourceLocation getKey(T pValue);

    Optional<ResourceKey<T>> getResourceKey(T pValue);

    @Override
    int getId(@Nullable T p_122977_);

    @Nullable
    T getValue(@Nullable ResourceKey<T> pKey);

    @Nullable
    T getValue(@Nullable ResourceLocation pKey);

    Optional<RegistrationInfo> registrationInfo(ResourceKey<T> pKey);

    default Optional<T> getOptional(@Nullable ResourceLocation pName) {
        return Optional.ofNullable(this.getValue(pName));
    }

    default Optional<T> getOptional(@Nullable ResourceKey<T> pRegistryKey) {
        return Optional.ofNullable(this.getValue(pRegistryKey));
    }

    Optional<Holder.Reference<T>> getAny();

    default T getValueOrThrow(ResourceKey<T> pKey) {
        T t = this.getValue(pKey);
        if (t == null) {
            throw new IllegalStateException("Missing key in " + this.key() + ": " + pKey);
        } else {
            return t;
        }
    }

    Set<ResourceLocation> keySet();

    Set<Entry<ResourceKey<T>, T>> entrySet();

    Set<ResourceKey<T>> registryKeySet();

    Optional<Holder.Reference<T>> getRandom(RandomSource pRandom);

    default Stream<T> stream() {
        return StreamSupport.stream(this.spliterator(), false);
    }

    boolean containsKey(ResourceLocation pName);

    boolean containsKey(ResourceKey<T> pKey);

    static <T> T register(Registry<? super T> pRegistry, String pName, T pValue) {
        return register(pRegistry, ResourceLocation.parse(pName), pValue);
    }

    static <V, T extends V> T register(Registry<V> pRegistry, ResourceLocation pName, T pValue) {
        return register(pRegistry, ResourceKey.create(pRegistry.key(), pName), pValue);
    }

    static <V, T extends V> T register(Registry<V> pRegistry, ResourceKey<V> pKey, T pValue) {
        ((WritableRegistry)pRegistry).register(pKey, (V)pValue, RegistrationInfo.BUILT_IN);
        return pValue;
    }

    static <T> Holder.Reference<T> registerForHolder(Registry<T> pRegistry, ResourceKey<T> pKey, T pValue) {
        return ((WritableRegistry)pRegistry).register(pKey, pValue, RegistrationInfo.BUILT_IN);
    }

    static <T> Holder.Reference<T> registerForHolder(Registry<T> pRegistry, ResourceLocation pName, T pValue) {
        return registerForHolder(pRegistry, ResourceKey.create(pRegistry.key(), pName), pValue);
    }

    Registry<T> freeze();

    Holder.Reference<T> createIntrusiveHolder(T pValue);

    Optional<Holder.Reference<T>> get(int pIndex);

    Optional<Holder.Reference<T>> get(ResourceLocation pKey);

    Holder<T> wrapAsHolder(T pValue);

    default Iterable<Holder<T>> getTagOrEmpty(TagKey<T> pKey) {
        return DataFixUtils.orElse((Optional<Iterable>)(Optional)this.get(pKey), List.<T>of());
    }

    default Optional<Holder<T>> getRandomElementOf(TagKey<T> pKey, RandomSource pRandom) {
        return this.get(pKey).flatMap(p_325677_ -> p_325677_.getRandomElement(pRandom));
    }

    Stream<HolderSet.Named<T>> getTags();

    default IdMap<Holder<T>> asHolderIdMap() {
        return new IdMap<Holder<T>>() {
            /**
             * @return the integer ID used to identify the given object
             */
            public int getId(Holder<T> p_259992_) {
                return Registry.this.getId(p_259992_.value());
            }

            @Nullable
            public Holder<T> byId(int p_259972_) {
                return (Holder<T>)Registry.this.get(p_259972_).orElse(null);
            }

            @Override
            public int size() {
                return Registry.this.size();
            }

            @Override
            public Iterator<Holder<T>> iterator() {
                return Registry.this.listElements().map(p_260061_ -> (Holder<T>)p_260061_).iterator();
            }
        };
    }

    Registry.PendingTags<T> prepareTagReload(TagLoader.LoadResult<T> pLoadResult);

    public interface PendingTags<T> {
        ResourceKey<? extends Registry<? extends T>> key();

        HolderLookup.RegistryLookup<T> lookup();

        void apply();

        int size();

        List<Holder<T>> getPending(TagKey<T> key);
    }
}
