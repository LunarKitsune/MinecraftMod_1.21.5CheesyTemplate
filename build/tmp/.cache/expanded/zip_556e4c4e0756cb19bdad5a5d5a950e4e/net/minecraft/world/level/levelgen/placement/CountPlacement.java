package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;

public class CountPlacement extends RepeatingPlacement {
    public static final MapCodec<CountPlacement> CODEC = IntProvider.codec(0, 256)
        .fieldOf("count")
        .xmap(CountPlacement::new, p_191633_ -> p_191633_.count);
    private final IntProvider count;

    private CountPlacement(IntProvider pCount) {
        this.count = pCount;
    }

    public static CountPlacement of(IntProvider pCount) {
        return new CountPlacement(pCount);
    }

    public static CountPlacement of(int pCount) {
        return of(ConstantInt.of(pCount));
    }

    @Override
    protected int count(RandomSource p_226333_, BlockPos p_226334_) {
        return this.count.sample(p_226333_);
    }

    @Override
    public PlacementModifierType<?> type() {
        return PlacementModifierType.COUNT;
    }
}