package net.minecraft.core.component.predicates;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public interface DataComponentPredicate {
    Codec<Map<DataComponentPredicate.Type<?>, DataComponentPredicate>> CODEC = Codec.dispatchedMap(
        BuiltInRegistries.DATA_COMPONENT_PREDICATE_TYPE.byNameCodec(), DataComponentPredicate.Type::codec
    );
    StreamCodec<RegistryFriendlyByteBuf, DataComponentPredicate.Single<?>> SINGLE_STREAM_CODEC = ByteBufCodecs.registry(Registries.DATA_COMPONENT_PREDICATE_TYPE)
        .dispatch(DataComponentPredicate.Single::type, DataComponentPredicate.Type::singleStreamCodec);
    StreamCodec<RegistryFriendlyByteBuf, Map<DataComponentPredicate.Type<?>, DataComponentPredicate>> STREAM_CODEC = SINGLE_STREAM_CODEC.apply(
            ByteBufCodecs.list(64)
        )
        .map(
            p_392899_ -> p_392899_.stream().collect(Collectors.toMap(DataComponentPredicate.Single::type, DataComponentPredicate.Single::predicate)),
            p_393256_ -> p_393256_.entrySet().stream().<DataComponentPredicate.Single<?>>map(DataComponentPredicate.Single::fromEntry).toList()
        );

    static MapCodec<DataComponentPredicate.Single<?>> singleCodec(String pName) {
        return BuiltInRegistries.DATA_COMPONENT_PREDICATE_TYPE.byNameCodec().dispatchMap(pName, DataComponentPredicate.Single::type, DataComponentPredicate.Type::wrappedCodec);
    }

    boolean matches(DataComponentGetter pComponentGetter);

    public record Single<T extends DataComponentPredicate>(DataComponentPredicate.Type<T> type, T predicate) {
        private static <T extends DataComponentPredicate> DataComponentPredicate.Single<T> fromEntry(Entry<DataComponentPredicate.Type<?>, T> pEntry) {
            return new DataComponentPredicate.Single<>((DataComponentPredicate.Type<T>)pEntry.getKey(), pEntry.getValue());
        }
    }

    public static final class Type<T extends DataComponentPredicate> {
        private final Codec<T> codec;
        private final MapCodec<DataComponentPredicate.Single<T>> wrappedCodec;
        private final StreamCodec<RegistryFriendlyByteBuf, DataComponentPredicate.Single<T>> singleStreamCodec;

        public Type(Codec<T> pCodec) {
            this.codec = pCodec;
            this.wrappedCodec = RecordCodecBuilder.mapCodec(
                p_396885_ -> p_396885_.group(pCodec.fieldOf("value").forGetter(DataComponentPredicate.Single::predicate))
                    .apply(p_396885_, p_393737_ -> new DataComponentPredicate.Single<>(this, p_393737_))
            );
            this.singleStreamCodec = ByteBufCodecs.fromCodecWithRegistries(pCodec)
                .map(p_395958_ -> new DataComponentPredicate.Single<>(this, (T)p_395958_), DataComponentPredicate.Single::predicate);
        }

        public Codec<T> codec() {
            return this.codec;
        }

        public MapCodec<DataComponentPredicate.Single<T>> wrappedCodec() {
            return this.wrappedCodec;
        }

        public StreamCodec<RegistryFriendlyByteBuf, DataComponentPredicate.Single<T>> singleStreamCodec() {
            return this.singleStreamCodec;
        }
    }
}