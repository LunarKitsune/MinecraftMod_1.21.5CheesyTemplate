package net.minecraft.core.component;

import java.util.stream.Stream;
import javax.annotation.Nullable;

public interface DataComponentHolder extends DataComponentGetter {
    DataComponentMap getComponents();

    @Nullable
    @Override
    default <T> T get(DataComponentType<? extends T> pComponent) {
        return this.getComponents().get(pComponent);
    }

    default <T> Stream<T> getAllOfType(Class<? extends T> pType) {
        return this.getComponents()
            .stream()
            .map(TypedDataComponent::value)
            .filter(p_365228_ -> pType.isAssignableFrom(p_365228_.getClass()))
            .map(p_365353_ -> (T)p_365353_);
    }

    @Override
    default <T> T getOrDefault(DataComponentType<? extends T> pComponent, T pDefaultValue) {
        return this.getComponents().getOrDefault(pComponent, pDefaultValue);
    }

    default boolean has(DataComponentType<?> pComponent) {
        return this.getComponents().has(pComponent);
    }
}