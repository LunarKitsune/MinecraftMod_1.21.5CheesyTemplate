package net.minecraft.util.valueproviders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.RandomSource;

public class ConstantInt extends IntProvider {
    public static final ConstantInt ZERO = new ConstantInt(0);
    public static final MapCodec<ConstantInt> CODEC = Codec.INT.fieldOf("value").xmap(ConstantInt::of, ConstantInt::getValue);
    private final int value;

    public static ConstantInt of(int pValue) {
        return pValue == 0 ? ZERO : new ConstantInt(pValue);
    }

    private ConstantInt(int pValue) {
        this.value = pValue;
    }

    public int getValue() {
        return this.value;
    }

    @Override
    public int sample(RandomSource p_216854_) {
        return this.value;
    }

    @Override
    public int getMinValue() {
        return this.value;
    }

    @Override
    public int getMaxValue() {
        return this.value;
    }

    @Override
    public IntProviderType<?> getType() {
        return IntProviderType.CONSTANT;
    }

    @Override
    public String toString() {
        return Integer.toString(this.value);
    }
}