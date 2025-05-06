package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;

public class RandomOffsetPlacement extends PlacementModifier {
    public static final MapCodec<RandomOffsetPlacement> CODEC = RecordCodecBuilder.mapCodec(
        p_191883_ -> p_191883_.group(
                IntProvider.codec(-16, 16).fieldOf("xz_spread").forGetter(p_191894_ -> p_191894_.xzSpread),
                IntProvider.codec(-16, 16).fieldOf("y_spread").forGetter(p_191885_ -> p_191885_.ySpread)
            )
            .apply(p_191883_, RandomOffsetPlacement::new)
    );
    private final IntProvider xzSpread;
    private final IntProvider ySpread;

    public static RandomOffsetPlacement of(IntProvider pXzSpread, IntProvider pYSpread) {
        return new RandomOffsetPlacement(pXzSpread, pYSpread);
    }

    public static RandomOffsetPlacement vertical(IntProvider pYSpread) {
        return new RandomOffsetPlacement(ConstantInt.of(0), pYSpread);
    }

    public static RandomOffsetPlacement horizontal(IntProvider pXzSpread) {
        return new RandomOffsetPlacement(pXzSpread, ConstantInt.of(0));
    }

    private RandomOffsetPlacement(IntProvider pXzSpread, IntProvider pYSpread) {
        this.xzSpread = pXzSpread;
        this.ySpread = pYSpread;
    }

    @Override
    public Stream<BlockPos> getPositions(PlacementContext p_226393_, RandomSource p_226394_, BlockPos p_226395_) {
        int i = p_226395_.getX() + this.xzSpread.sample(p_226394_);
        int j = p_226395_.getY() + this.ySpread.sample(p_226394_);
        int k = p_226395_.getZ() + this.xzSpread.sample(p_226394_);
        return Stream.of(new BlockPos(i, j, k));
    }

    @Override
    public PlacementModifierType<?> type() {
        return PlacementModifierType.RANDOM_OFFSET;
    }
}