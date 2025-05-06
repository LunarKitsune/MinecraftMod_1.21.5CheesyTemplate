package net.minecraft.sounds;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.ResourceLocation;

public record SoundEvent(ResourceLocation location, Optional<Float> fixedRange) {
    public static final Codec<SoundEvent> DIRECT_CODEC = RecordCodecBuilder.create(
        p_326482_ -> p_326482_.group(
                ResourceLocation.CODEC.fieldOf("sound_id").forGetter(SoundEvent::location),
                Codec.FLOAT.lenientOptionalFieldOf("range").forGetter(SoundEvent::fixedRange)
            )
            .apply(p_326482_, SoundEvent::create)
    );
    public static final Codec<Holder<SoundEvent>> CODEC = RegistryFileCodec.create(Registries.SOUND_EVENT, DIRECT_CODEC);
    public static final StreamCodec<ByteBuf, SoundEvent> DIRECT_STREAM_CODEC = StreamCodec.composite(
        ResourceLocation.STREAM_CODEC,
        SoundEvent::location,
        ByteBufCodecs.FLOAT.apply(ByteBufCodecs::optional),
        SoundEvent::fixedRange,
        SoundEvent::create
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, Holder<SoundEvent>> STREAM_CODEC = ByteBufCodecs.holder(Registries.SOUND_EVENT, DIRECT_STREAM_CODEC);

    private static SoundEvent create(ResourceLocation pLocation, Optional<Float> pRange) {
        return pRange.<SoundEvent>map(p_263360_ -> createFixedRangeEvent(pLocation, p_263360_)).orElseGet(() -> createVariableRangeEvent(pLocation));
    }

    public static SoundEvent createVariableRangeEvent(ResourceLocation pLocation) {
        return new SoundEvent(pLocation, Optional.empty());
    }

    public static SoundEvent createFixedRangeEvent(ResourceLocation pLocation, float pRange) {
        return new SoundEvent(pLocation, Optional.of(pRange));
    }

    public float getRange(float pVolume) {
        return this.fixedRange.orElse(pVolume > 1.0F ? 16.0F * pVolume : 16.0F);
    }
}