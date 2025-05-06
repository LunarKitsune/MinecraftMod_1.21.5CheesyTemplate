package net.minecraft.world.level.levelgen.structure.structures;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

public class OceanRuinStructure extends Structure {
    public static final MapCodec<OceanRuinStructure> CODEC = RecordCodecBuilder.mapCodec(
        p_229075_ -> p_229075_.group(
                settingsCodec(p_229075_),
                OceanRuinStructure.Type.CODEC.fieldOf("biome_temp").forGetter(p_229079_ -> p_229079_.biomeTemp),
                Codec.floatRange(0.0F, 1.0F).fieldOf("large_probability").forGetter(p_229077_ -> p_229077_.largeProbability),
                Codec.floatRange(0.0F, 1.0F).fieldOf("cluster_probability").forGetter(p_229073_ -> p_229073_.clusterProbability)
            )
            .apply(p_229075_, OceanRuinStructure::new)
    );
    public final OceanRuinStructure.Type biomeTemp;
    public final float largeProbability;
    public final float clusterProbability;

    public OceanRuinStructure(Structure.StructureSettings pSettings, OceanRuinStructure.Type pBiomeTemp, float pLargeProbability, float pClusterProbability) {
        super(pSettings);
        this.biomeTemp = pBiomeTemp;
        this.largeProbability = pLargeProbability;
        this.clusterProbability = pClusterProbability;
    }

    @Override
    public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext p_229065_) {
        return onTopOfChunkCenter(p_229065_, Heightmap.Types.OCEAN_FLOOR_WG, p_229068_ -> this.generatePieces(p_229068_, p_229065_));
    }

    private void generatePieces(StructurePiecesBuilder pBuilder, Structure.GenerationContext pContext) {
        BlockPos blockpos = new BlockPos(pContext.chunkPos().getMinBlockX(), 90, pContext.chunkPos().getMinBlockZ());
        Rotation rotation = Rotation.getRandom(pContext.random());
        OceanRuinPieces.addPieces(pContext.structureTemplateManager(), blockpos, rotation, pBuilder, pContext.random(), this);
    }

    @Override
    public StructureType<?> type() {
        return StructureType.OCEAN_RUIN;
    }

    public static enum Type implements StringRepresentable {
        WARM("warm"),
        COLD("cold");

        public static final Codec<OceanRuinStructure.Type> CODEC = StringRepresentable.fromEnum(OceanRuinStructure.Type::values);
        @Deprecated
        public static final Codec<OceanRuinStructure.Type> LEGACY_CODEC = ExtraCodecs.legacyEnum(OceanRuinStructure.Type::valueOf);
        private final String name;

        private Type(final String pName) {
            this.name = pName;
        }

        public String getName() {
            return this.name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }
    }
}