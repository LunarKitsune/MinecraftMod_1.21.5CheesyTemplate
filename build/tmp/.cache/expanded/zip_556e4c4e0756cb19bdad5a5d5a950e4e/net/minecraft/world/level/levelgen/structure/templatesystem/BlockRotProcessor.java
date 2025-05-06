package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;

public class BlockRotProcessor extends StructureProcessor {
    public static final MapCodec<BlockRotProcessor> CODEC = RecordCodecBuilder.mapCodec(
        p_259016_ -> p_259016_.group(
                RegistryCodecs.homogeneousList(Registries.BLOCK).optionalFieldOf("rottable_blocks").forGetter(p_230291_ -> p_230291_.rottableBlocks),
                Codec.floatRange(0.0F, 1.0F).fieldOf("integrity").forGetter(p_230289_ -> p_230289_.integrity)
            )
            .apply(p_259016_, BlockRotProcessor::new)
    );
    private final Optional<HolderSet<Block>> rottableBlocks;
    private final float integrity;

    public BlockRotProcessor(HolderSet<Block> pRottableBlocks, float pIntegrity) {
        this(Optional.of(pRottableBlocks), pIntegrity);
    }

    public BlockRotProcessor(float pIntegrity) {
        this(Optional.empty(), pIntegrity);
    }

    private BlockRotProcessor(Optional<HolderSet<Block>> pRottableBlocks, float pIntegrity) {
        this.integrity = pIntegrity;
        this.rottableBlocks = pRottableBlocks;
    }

    @Nullable
    @Override
    public StructureTemplate.StructureBlockInfo processBlock(
        LevelReader p_74081_,
        BlockPos p_74082_,
        BlockPos p_74083_,
        StructureTemplate.StructureBlockInfo p_74084_,
        StructureTemplate.StructureBlockInfo p_74085_,
        StructurePlaceSettings p_74086_
    ) {
        RandomSource randomsource = p_74086_.getRandom(p_74085_.pos());
        return (!this.rottableBlocks.isPresent() || p_74084_.state().is(this.rottableBlocks.get())) && !(randomsource.nextFloat() <= this.integrity)
            ? null
            : p_74085_;
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return StructureProcessorType.BLOCK_ROT;
    }
}