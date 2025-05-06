package net.minecraft.core.component;

import javax.annotation.Nullable;

public interface DataComponentGetter {
    @Nullable
    <T> T get(DataComponentType<? extends T> pComponent);

    default <T> T getOrDefault(DataComponentType<? extends T> pComponent, T pDefaultValue) {
        T t = this.get(pComponent);
        return t != null ? t : pDefaultValue;
    }

    @Nullable
    default <T> TypedDataComponent<T> getTyped(DataComponentType<T> pComponent) {
        T t = this.get(pComponent);
        return t != null ? new TypedDataComponent<>(pComponent, t) : null;
    }
}