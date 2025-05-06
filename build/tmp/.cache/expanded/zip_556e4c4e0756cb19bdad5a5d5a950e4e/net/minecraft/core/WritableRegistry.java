package net.minecraft.core;

import java.util.List;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

public interface WritableRegistry<T> extends Registry<T> {
    Holder.Reference<T> register(ResourceKey<T> pKey, T pValue, RegistrationInfo pRegistrationInfo);

    void bindTag(TagKey<T> pTag, List<Holder<T>> pValues);

    boolean isEmpty();

    HolderGetter<T> createRegistrationLookup();
}