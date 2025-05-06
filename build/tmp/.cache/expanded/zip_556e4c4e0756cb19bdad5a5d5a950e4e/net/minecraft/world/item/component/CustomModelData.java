package net.minecraft.world.item.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import io.netty.buffer.ByteBuf;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;

public record CustomModelData(List<Float> floats, List<Boolean> flags, List<String> strings, List<Integer> colors) {
    public static final CustomModelData EMPTY = new CustomModelData(List.of(), List.of(), List.of(), List.of());
    public static final Codec<CustomModelData> CODEC = RecordCodecBuilder.create(
        p_378135_ -> p_378135_.group(
                Codec.FLOAT.listOf().optionalFieldOf("floats", List.of()).forGetter(CustomModelData::floats),
                Codec.BOOL.listOf().optionalFieldOf("flags", List.of()).forGetter(CustomModelData::flags),
                Codec.STRING.listOf().optionalFieldOf("strings", List.of()).forGetter(CustomModelData::strings),
                ExtraCodecs.RGB_COLOR_CODEC.listOf().optionalFieldOf("colors", List.of()).forGetter(CustomModelData::colors)
            )
            .apply(p_378135_, CustomModelData::new)
    );
    public static final StreamCodec<ByteBuf, CustomModelData> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.FLOAT.apply(ByteBufCodecs.list()),
        CustomModelData::floats,
        ByteBufCodecs.BOOL.apply(ByteBufCodecs.list()),
        CustomModelData::flags,
        ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
        CustomModelData::strings,
        ByteBufCodecs.INT.apply(ByteBufCodecs.list()),
        CustomModelData::colors,
        CustomModelData::new
    );

    @Nullable
    private static <T> T getSafe(List<T> pValues, int pIndex) {
        return pIndex >= 0 && pIndex < pValues.size() ? pValues.get(pIndex) : null;
    }

    @Nullable
    public Float getFloat(int pIndex) {
        return getSafe(this.floats, pIndex);
    }

    @Nullable
    public Boolean getBoolean(int pIndex) {
        return getSafe(this.flags, pIndex);
    }

    @Nullable
    public String getString(int pIndex) {
        return getSafe(this.strings, pIndex);
    }

    @Nullable
    public Integer getColor(int pIndex) {
        return getSafe(this.colors, pIndex);
    }
}