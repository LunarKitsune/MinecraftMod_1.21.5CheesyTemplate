package net.minecraft.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;

public record Brightness(int block, int sky) {
    public static final Codec<Integer> LIGHT_VALUE_CODEC = ExtraCodecs.intRange(0, 15);
    public static final Codec<Brightness> CODEC = RecordCodecBuilder.create(
        p_270774_ -> p_270774_.group(LIGHT_VALUE_CODEC.fieldOf("block").forGetter(Brightness::block), LIGHT_VALUE_CODEC.fieldOf("sky").forGetter(Brightness::sky))
            .apply(p_270774_, Brightness::new)
    );
    public static final Brightness FULL_BRIGHT = new Brightness(15, 15);

    public static int pack(int pBlock, int pSky) {
        return pBlock << 4 | pSky << 20;
    }

    public int pack() {
        return pack(this.block, this.sky);
    }

    public static int block(int pPacked) {
        return pPacked >> 4 & 65535;
    }

    public static int sky(int pPacked) {
        return pPacked >> 20 & 65535;
    }

    public static Brightness unpack(int pPackedBrightness) {
        return new Brightness(block(pPackedBrightness), sky(pPackedBrightness));
    }
}