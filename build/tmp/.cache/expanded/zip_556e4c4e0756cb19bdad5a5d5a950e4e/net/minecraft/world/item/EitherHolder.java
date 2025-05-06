package net.minecraft.world.item;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.Optional;
import java.util.function.Function;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;

public record EitherHolder<T>(Either<Holder<T>, ResourceKey<T>> contents) {
    public EitherHolder(Holder<T> pHolder) {
        this(Either.left(pHolder));
    }

    public EitherHolder(ResourceKey<T> pKey) {
        this(Either.right(pKey));
    }

    public static <T> Codec<EitherHolder<T>> codec(ResourceKey<Registry<T>> pRegistryKey, Codec<Holder<T>> pCodec) {
        return Codec.either(
                pCodec,
                ResourceKey.codec(pRegistryKey).comapFlatMap(p_343571_ -> DataResult.error(() -> "Cannot parse as key without registry"), Function.identity())
            )
            .xmap(EitherHolder::new, EitherHolder::contents);
    }

    public static <T> StreamCodec<RegistryFriendlyByteBuf, EitherHolder<T>> streamCodec(
        ResourceKey<Registry<T>> pRegistryKey, StreamCodec<RegistryFriendlyByteBuf, Holder<T>> pStreamCodec
    ) {
        return StreamCodec.composite(ByteBufCodecs.either(pStreamCodec, ResourceKey.streamCodec(pRegistryKey)), EitherHolder::contents, EitherHolder::new);
    }

    public Optional<T> unwrap(Registry<T> pRegistry) {
        return this.contents.map(p_390807_ -> Optional.of(p_390807_.value()), pRegistry::getOptional);
    }

    public Optional<Holder<T>> unwrap(HolderLookup.Provider pRegistries) {
        return this.contents.map(Optional::of, p_390806_ -> pRegistries.get((ResourceKey<T>)p_390806_).map(p_390808_ -> (Holder<T>)p_390808_));
    }

    public Optional<ResourceKey<T>> key() {
        return this.contents.map(Holder::unwrapKey, Optional::of);
    }
}