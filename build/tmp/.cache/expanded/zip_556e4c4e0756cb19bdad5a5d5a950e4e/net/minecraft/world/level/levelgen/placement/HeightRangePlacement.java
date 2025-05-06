package net.minecraft.world.level.levelgen.placement;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.heightproviders.TrapezoidHeight;
import net.minecraft.world.level.levelgen.heightproviders.UniformHeight;

public class HeightRangePlacement extends PlacementModifier {
    public static final MapCodec<HeightRangePlacement> CODEC = RecordCodecBuilder.mapCodec(
        p_191679_ -> p_191679_.group(HeightProvider.CODEC.fieldOf("height").forGetter(p_191686_ -> p_191686_.height))
            .apply(p_191679_, HeightRangePlacement::new)
    );
    private final HeightProvider height;

    private HeightRangePlacement(HeightProvider pHeight) {
        this.height = pHeight;
    }

    public static HeightRangePlacement of(HeightProvider pHeight) {
        return new HeightRangePlacement(pHeight);
    }

    public static HeightRangePlacement uniform(VerticalAnchor pMinInclusive, VerticalAnchor pMaxInclusive) {
        return of(UniformHeight.of(pMinInclusive, pMaxInclusive));
    }

    public static HeightRangePlacement triangle(VerticalAnchor pMinInclusive, VerticalAnchor pMaxInclusive) {
        return of(TrapezoidHeight.of(pMinInclusive, pMaxInclusive));
    }

    @Override
    public Stream<BlockPos> getPositions(PlacementContext p_226340_, RandomSource p_226341_, BlockPos p_226342_) {
        return Stream.of(p_226342_.atY(this.height.sample(p_226341_, p_226340_)));
    }

    @Override
    public PlacementModifierType<?> type() {
        return PlacementModifierType.HEIGHT_RANGE;
    }
}