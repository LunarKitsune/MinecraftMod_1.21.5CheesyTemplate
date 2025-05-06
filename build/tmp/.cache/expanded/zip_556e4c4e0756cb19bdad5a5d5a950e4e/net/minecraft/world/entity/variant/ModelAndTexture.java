package net.minecraft.world.entity.variant;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.core.ClientAsset;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public record ModelAndTexture<T>(T model, ClientAsset asset) {
    public ModelAndTexture(T pModel, ResourceLocation pAssetId) {
        this(pModel, new ClientAsset(pAssetId));
    }

    public static <T> MapCodec<ModelAndTexture<T>> codec(Codec<T> pModelCodec, T pDefaultModel) {
        return RecordCodecBuilder.mapCodec(
            p_394042_ -> p_394042_.group(
                    pModelCodec.optionalFieldOf("model", pDefaultModel).forGetter(ModelAndTexture::model),
                    ClientAsset.DEFAULT_FIELD_CODEC.forGetter(ModelAndTexture::asset)
                )
                .apply(p_394042_, ModelAndTexture::new)
        );
    }

    public static <T> StreamCodec<RegistryFriendlyByteBuf, ModelAndTexture<T>> streamCodec(StreamCodec<? super RegistryFriendlyByteBuf, T> pModelCodec) {
        return StreamCodec.composite(pModelCodec, ModelAndTexture::model, ClientAsset.STREAM_CODEC, ModelAndTexture::asset, ModelAndTexture::new);
    }
}