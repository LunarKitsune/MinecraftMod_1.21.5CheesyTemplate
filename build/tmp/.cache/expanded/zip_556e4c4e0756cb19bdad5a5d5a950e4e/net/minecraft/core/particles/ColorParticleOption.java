package net.minecraft.core.particles;

import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ARGB;
import net.minecraft.util.ExtraCodecs;

public class ColorParticleOption implements ParticleOptions {
    private final ParticleType<ColorParticleOption> type;
    private final int color;

    public static MapCodec<ColorParticleOption> codec(ParticleType<ColorParticleOption> pParticleType) {
        return ExtraCodecs.ARGB_COLOR_CODEC.xmap(p_335886_ -> new ColorParticleOption(pParticleType, p_335886_), p_328917_ -> p_328917_.color).fieldOf("color");
    }

    public static StreamCodec<? super ByteBuf, ColorParticleOption> streamCodec(ParticleType<ColorParticleOption> pType) {
        return ByteBufCodecs.INT.map(p_330079_ -> new ColorParticleOption(pType, p_330079_), p_329364_ -> p_329364_.color);
    }

    private ColorParticleOption(ParticleType<ColorParticleOption> pType, int pColor) {
        this.type = pType;
        this.color = pColor;
    }

    @Override
    public ParticleType<ColorParticleOption> getType() {
        return this.type;
    }

    public float getRed() {
        return ARGB.red(this.color) / 255.0F;
    }

    public float getGreen() {
        return ARGB.green(this.color) / 255.0F;
    }

    public float getBlue() {
        return ARGB.blue(this.color) / 255.0F;
    }

    public float getAlpha() {
        return ARGB.alpha(this.color) / 255.0F;
    }

    public static ColorParticleOption create(ParticleType<ColorParticleOption> pType, int pColor) {
        return new ColorParticleOption(pType, pColor);
    }

    public static ColorParticleOption create(ParticleType<ColorParticleOption> pType, float pRed, float pGreen, float pBlue) {
        return create(pType, ARGB.colorFromFloat(1.0F, pRed, pGreen, pBlue));
    }
}