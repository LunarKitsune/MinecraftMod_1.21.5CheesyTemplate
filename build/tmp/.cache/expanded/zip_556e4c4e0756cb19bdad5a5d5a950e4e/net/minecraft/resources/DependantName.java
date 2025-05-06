package net.minecraft.resources;

@FunctionalInterface
public interface DependantName<T, V> {
    V get(ResourceKey<T> pKey);

    static <T, V> DependantName<T, V> fixed(V pValue) {
        return p_365225_ -> pValue;
    }
}