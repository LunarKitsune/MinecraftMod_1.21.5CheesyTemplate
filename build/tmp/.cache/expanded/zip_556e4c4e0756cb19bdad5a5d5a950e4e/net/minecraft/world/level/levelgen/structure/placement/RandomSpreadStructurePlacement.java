package net.minecraft.world.level.levelgen.structure.placement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;

public class RandomSpreadStructurePlacement extends StructurePlacement {
    public static final MapCodec<RandomSpreadStructurePlacement> CODEC = RecordCodecBuilder.<RandomSpreadStructurePlacement>mapCodec(
            p_204996_ -> placementCodec(p_204996_)
                .and(
                    p_204996_.group(
                        Codec.intRange(0, 4096).fieldOf("spacing").forGetter(RandomSpreadStructurePlacement::spacing),
                        Codec.intRange(0, 4096).fieldOf("separation").forGetter(RandomSpreadStructurePlacement::separation),
                        RandomSpreadType.CODEC.optionalFieldOf("spread_type", RandomSpreadType.LINEAR).forGetter(RandomSpreadStructurePlacement::spreadType)
                    )
                )
                .apply(p_204996_, RandomSpreadStructurePlacement::new)
        )
        .validate(RandomSpreadStructurePlacement::validate);
    private final int spacing;
    private final int separation;
    private final RandomSpreadType spreadType;

    private static DataResult<RandomSpreadStructurePlacement> validate(RandomSpreadStructurePlacement pPlacement) {
        return pPlacement.spacing <= pPlacement.separation ? DataResult.error(() -> "Spacing has to be larger than separation") : DataResult.success(pPlacement);
    }

    public RandomSpreadStructurePlacement(
        Vec3i pLocateOffset,
        StructurePlacement.FrequencyReductionMethod pFrequencyReductionMethod,
        float pFrequency,
        int pSalt,
        Optional<StructurePlacement.ExclusionZone> pExclusionZone,
        int pSpacing,
        int pSeparation,
        RandomSpreadType pSpreadType
    ) {
        super(pLocateOffset, pFrequencyReductionMethod, pFrequency, pSalt, pExclusionZone);
        this.spacing = pSpacing;
        this.separation = pSeparation;
        this.spreadType = pSpreadType;
    }

    public RandomSpreadStructurePlacement(int pSpacing, int pSeparation, RandomSpreadType pSpreadType, int pSalt) {
        this(Vec3i.ZERO, StructurePlacement.FrequencyReductionMethod.DEFAULT, 1.0F, pSalt, Optional.empty(), pSpacing, pSeparation, pSpreadType);
    }

    public int spacing() {
        return this.spacing;
    }

    public int separation() {
        return this.separation;
    }

    public RandomSpreadType spreadType() {
        return this.spreadType;
    }

    public ChunkPos getPotentialStructureChunk(long pSeed, int pRegionX, int pRegionZ) {
        int i = Math.floorDiv(pRegionX, this.spacing);
        int j = Math.floorDiv(pRegionZ, this.spacing);
        WorldgenRandom worldgenrandom = new WorldgenRandom(new LegacyRandomSource(0L));
        worldgenrandom.setLargeFeatureWithSalt(pSeed, i, j, this.salt());
        int k = this.spacing - this.separation;
        int l = this.spreadType.evaluate(worldgenrandom, k);
        int i1 = this.spreadType.evaluate(worldgenrandom, k);
        return new ChunkPos(i * this.spacing + l, j * this.spacing + i1);
    }

    @Override
    protected boolean isPlacementChunk(ChunkGeneratorStructureState p_256267_, int p_256050_, int p_255975_) {
        ChunkPos chunkpos = this.getPotentialStructureChunk(p_256267_.getLevelSeed(), p_256050_, p_255975_);
        return chunkpos.x == p_256050_ && chunkpos.z == p_255975_;
    }

    @Override
    public StructurePlacementType<?> type() {
        return StructurePlacementType.RANDOM_SPREAD;
    }
}