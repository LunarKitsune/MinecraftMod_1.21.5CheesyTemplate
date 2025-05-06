package net.minecraft.network.chat;

import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapDecoder;
import com.mojang.serialization.MapEncoder;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.contents.KeybindContents;
import net.minecraft.network.chat.contents.NbtContents;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.ScoreContents;
import net.minecraft.network.chat.contents.SelectorContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.StringRepresentable;

public class ComponentSerialization {
    public static final Codec<Component> CODEC = Codec.recursive("Component", ComponentSerialization::createCodec);
    public static final StreamCodec<RegistryFriendlyByteBuf, Component> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC);
    public static final StreamCodec<RegistryFriendlyByteBuf, Optional<Component>> OPTIONAL_STREAM_CODEC = STREAM_CODEC.apply(ByteBufCodecs::optional);
    public static final StreamCodec<RegistryFriendlyByteBuf, Component> TRUSTED_STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistriesTrusted(CODEC);
    public static final StreamCodec<RegistryFriendlyByteBuf, Optional<Component>> TRUSTED_OPTIONAL_STREAM_CODEC = TRUSTED_STREAM_CODEC.apply(ByteBufCodecs::optional);
    public static final StreamCodec<ByteBuf, Component> TRUSTED_CONTEXT_FREE_STREAM_CODEC = ByteBufCodecs.fromCodecTrusted(CODEC);

    public static Codec<Component> flatRestrictedCodec(final int pMaxSize) {
        return new Codec<Component>() {
            @Override
            public <T> DataResult<Pair<Component, T>> decode(DynamicOps<T> p_334494_, T p_334478_) {
                return ComponentSerialization.CODEC
                    .decode(p_334494_, p_334478_)
                    .flatMap(
                        p_389912_ -> this.isTooLarge(p_334494_, p_389912_.getFirst())
                            ? DataResult.error(() -> "Component was too large: greater than max size " + pMaxSize)
                            : DataResult.success((Pair<Component, T>)p_389912_)
                    );
            }

            public <T> DataResult<T> encode(Component p_330654_, DynamicOps<T> p_330879_, T p_336296_) {
                return ComponentSerialization.CODEC.encodeStart(p_330879_, p_330654_);
            }

            private <T> boolean isTooLarge(DynamicOps<T> p_397653_, Component p_397086_) {
                DataResult<JsonElement> dataresult = ComponentSerialization.CODEC.encodeStart(asJsonOps(p_397653_), p_397086_);
                return dataresult.isSuccess() && GsonHelper.encodesLongerThan(dataresult.getOrThrow(), pMaxSize);
            }

            private static <T> DynamicOps<JsonElement> asJsonOps(DynamicOps<T> p_331374_) {
                return (DynamicOps<JsonElement>)(p_331374_ instanceof RegistryOps<T> registryops ? registryops.withParent(JsonOps.INSTANCE) : JsonOps.INSTANCE);
            }
        };
    }

    private static MutableComponent createFromList(List<Component> pComponents) {
        MutableComponent mutablecomponent = pComponents.get(0).copy();

        for (int i = 1; i < pComponents.size(); i++) {
            mutablecomponent.append(pComponents.get(i));
        }

        return mutablecomponent;
    }

    public static <T extends StringRepresentable, E> MapCodec<E> createLegacyComponentMatcher(
        T[] pTypes, Function<T, MapCodec<? extends E>> pCodecGetter, Function<E, T> pTypeGetter, String pTypeFieldName
    ) {
        MapCodec<E> mapcodec = new ComponentSerialization.FuzzyCodec<>(
            Stream.<T>of(pTypes).map(pCodecGetter).toList(), p_312251_ -> pCodecGetter.apply(pTypeGetter.apply(p_312251_))
        );
        Codec<T> codec = StringRepresentable.fromValues((Supplier<T[]>)(() -> pTypes));
        MapCodec<E> mapcodec1 = codec.dispatchMap(pTypeFieldName, pTypeGetter, pCodecGetter);
        MapCodec<E> mapcodec2 = new ComponentSerialization.StrictEither<>(pTypeFieldName, mapcodec1, mapcodec);
        return ExtraCodecs.orCompressed(mapcodec2, mapcodec1);
    }

    private static Codec<Component> createCodec(Codec<Component> pCodec) {
        ComponentContents.Type<?>[] type = new ComponentContents.Type[]{
            PlainTextContents.TYPE,
            TranslatableContents.TYPE,
            KeybindContents.TYPE,
            ScoreContents.TYPE,
            SelectorContents.TYPE,
            NbtContents.TYPE
        };
        MapCodec<ComponentContents> mapcodec = createLegacyComponentMatcher(type, ComponentContents.Type::codec, ComponentContents::type, "type");
        Codec<Component> codec = RecordCodecBuilder.create(
            p_326064_ -> p_326064_.group(
                    mapcodec.forGetter(Component::getContents),
                    ExtraCodecs.nonEmptyList(pCodec.listOf()).optionalFieldOf("extra", List.of()).forGetter(Component::getSiblings),
                    Style.Serializer.MAP_CODEC.forGetter(Component::getStyle)
                )
                .apply(p_326064_, MutableComponent::new)
        );
        return Codec.either(Codec.either(Codec.STRING, ExtraCodecs.nonEmptyList(pCodec.listOf())), codec)
            .xmap(
                p_312362_ -> p_312362_.map(
                    p_310114_ -> p_310114_.map(Component::literal, ComponentSerialization::createFromList), p_310523_ -> (Component)p_310523_
                ),
                p_312558_ -> {
                    String s = p_312558_.tryCollapseToString();
                    return s != null ? Either.left(Either.left(s)) : Either.right(p_312558_);
                }
            );
    }

    static class FuzzyCodec<T> extends MapCodec<T> {
        private final List<MapCodec<? extends T>> codecs;
        private final Function<T, MapEncoder<? extends T>> encoderGetter;

        public FuzzyCodec(List<MapCodec<? extends T>> pCodecs, Function<T, MapEncoder<? extends T>> pEncoderGetter) {
            this.codecs = pCodecs;
            this.encoderGetter = pEncoderGetter;
        }

        @Override
        public <S> DataResult<T> decode(DynamicOps<S> pOps, MapLike<S> pInput) {
            for (MapDecoder<? extends T> mapdecoder : this.codecs) {
                DataResult<? extends T> dataresult = mapdecoder.decode(pOps, pInput);
                if (dataresult.result().isPresent()) {
                    return (DataResult<T>)dataresult;
                }
            }

            return DataResult.error(() -> "No matching codec found");
        }

        @Override
        public <S> RecordBuilder<S> encode(T pInput, DynamicOps<S> pOps, RecordBuilder<S> pPrefix) {
            MapEncoder<T> mapencoder = (MapEncoder<T>)this.encoderGetter.apply(pInput);
            return mapencoder.encode(pInput, pOps, pPrefix);
        }

        @Override
        public <S> Stream<S> keys(DynamicOps<S> pOps) {
            return this.codecs.stream().flatMap(p_310919_ -> p_310919_.keys(pOps)).distinct();
        }

        @Override
        public String toString() {
            return "FuzzyCodec[" + this.codecs + "]";
        }
    }

    static class StrictEither<T> extends MapCodec<T> {
        private final String typeFieldName;
        private final MapCodec<T> typed;
        private final MapCodec<T> fuzzy;

        public StrictEither(String pTypeFieldName, MapCodec<T> pTyped, MapCodec<T> pFuzzy) {
            this.typeFieldName = pTypeFieldName;
            this.typed = pTyped;
            this.fuzzy = pFuzzy;
        }

        @Override
        public <O> DataResult<T> decode(DynamicOps<O> pOps, MapLike<O> pInput) {
            return pInput.get(this.typeFieldName) != null ? this.typed.decode(pOps, pInput) : this.fuzzy.decode(pOps, pInput);
        }

        @Override
        public <O> RecordBuilder<O> encode(T pInput, DynamicOps<O> pOps, RecordBuilder<O> pPrefix) {
            return this.fuzzy.encode(pInput, pOps, pPrefix);
        }

        @Override
        public <T1> Stream<T1> keys(DynamicOps<T1> pOps) {
            return Stream.concat(this.typed.keys(pOps), this.fuzzy.keys(pOps)).distinct();
        }
    }
}