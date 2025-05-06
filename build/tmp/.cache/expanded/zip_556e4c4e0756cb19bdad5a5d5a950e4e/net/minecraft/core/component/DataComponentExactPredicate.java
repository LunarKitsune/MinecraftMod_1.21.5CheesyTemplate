package net.minecraft.core.component;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public final class DataComponentExactPredicate implements Predicate<DataComponentGetter> {
    public static final Codec<DataComponentExactPredicate> CODEC = DataComponentType.VALUE_MAP_CODEC
        .xmap(
            p_397402_ -> new DataComponentExactPredicate(p_397402_.entrySet().stream().map(TypedDataComponent::fromEntryUnchecked).collect(Collectors.toList())),
            p_397856_ -> p_397856_.expectedComponents
                .stream()
                .filter(p_397260_ -> !p_397260_.type().isTransient())
                .collect(Collectors.toMap(TypedDataComponent::type, TypedDataComponent::value))
        );
    public static final StreamCodec<RegistryFriendlyByteBuf, DataComponentExactPredicate> STREAM_CODEC = TypedDataComponent.STREAM_CODEC
        .apply(ByteBufCodecs.list())
        .map(DataComponentExactPredicate::new, p_397055_ -> p_397055_.expectedComponents);
    public static final DataComponentExactPredicate EMPTY = new DataComponentExactPredicate(List.of());
    private final List<TypedDataComponent<?>> expectedComponents;

    DataComponentExactPredicate(List<TypedDataComponent<?>> pExpectedComponents) {
        this.expectedComponents = pExpectedComponents;
    }

    public static DataComponentExactPredicate.Builder builder() {
        return new DataComponentExactPredicate.Builder();
    }

    public static <T> DataComponentExactPredicate expect(DataComponentType<T> pComponent, T pValue) {
        return new DataComponentExactPredicate(List.of(new TypedDataComponent<>(pComponent, pValue)));
    }

    public static DataComponentExactPredicate allOf(DataComponentMap pMap) {
        return new DataComponentExactPredicate(ImmutableList.copyOf(pMap));
    }

    public static DataComponentExactPredicate someOf(DataComponentMap pMap, DataComponentType<?>... pTypes) {
        DataComponentExactPredicate.Builder datacomponentexactpredicate$builder = new DataComponentExactPredicate.Builder();

        for (DataComponentType<?> datacomponenttype : pTypes) {
            TypedDataComponent<?> typeddatacomponent = pMap.getTyped(datacomponenttype);
            if (typeddatacomponent != null) {
                datacomponentexactpredicate$builder.expect(typeddatacomponent);
            }
        }

        return datacomponentexactpredicate$builder.build();
    }

    public boolean isEmpty() {
        return this.expectedComponents.isEmpty();
    }

    @Override
    public boolean equals(Object pOther) {
        return pOther instanceof DataComponentExactPredicate datacomponentexactpredicate && this.expectedComponents.equals(datacomponentexactpredicate.expectedComponents);
    }

    @Override
    public int hashCode() {
        return this.expectedComponents.hashCode();
    }

    @Override
    public String toString() {
        return this.expectedComponents.toString();
    }

    public boolean test(DataComponentGetter p_393043_) {
        for (TypedDataComponent<?> typeddatacomponent : this.expectedComponents) {
            Object object = p_393043_.get(typeddatacomponent.type());
            if (!Objects.equals(typeddatacomponent.value(), object)) {
                return false;
            }
        }

        return true;
    }

    public boolean alwaysMatches() {
        return this.expectedComponents.isEmpty();
    }

    public DataComponentPatch asPatch() {
        DataComponentPatch.Builder datacomponentpatch$builder = DataComponentPatch.builder();

        for (TypedDataComponent<?> typeddatacomponent : this.expectedComponents) {
            datacomponentpatch$builder.set(typeddatacomponent);
        }

        return datacomponentpatch$builder.build();
    }

    public static class Builder {
        private final List<TypedDataComponent<?>> expectedComponents = new ArrayList<>();

        Builder() {
        }

        public <T> DataComponentExactPredicate.Builder expect(TypedDataComponent<T> pComponent) {
            return this.expect(pComponent.type(), pComponent.value());
        }

        public <T> DataComponentExactPredicate.Builder expect(DataComponentType<? super T> pComponent, T pValue) {
            for (TypedDataComponent<?> typeddatacomponent : this.expectedComponents) {
                if (typeddatacomponent.type() == pComponent) {
                    throw new IllegalArgumentException("Predicate already has component of type: '" + pComponent + "'");
                }
            }

            this.expectedComponents.add(new TypedDataComponent<>(pComponent, pValue));
            return this;
        }

        public DataComponentExactPredicate build() {
            return new DataComponentExactPredicate(List.copyOf(this.expectedComponents));
        }
    }
}