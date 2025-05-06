package net.minecraft.world.level.levelgen.structure;

import java.util.Optional;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

public abstract class SinglePieceStructure extends Structure {
    private final SinglePieceStructure.PieceConstructor constructor;
    private final int width;
    private final int depth;

    protected SinglePieceStructure(SinglePieceStructure.PieceConstructor pConstructor, int pWidth, int pDepth, Structure.StructureSettings pSettings) {
        super(pSettings);
        this.constructor = pConstructor;
        this.width = pWidth;
        this.depth = pDepth;
    }

    @Override
    public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext p_226542_) {
        return getLowestY(p_226542_, this.width, this.depth) < p_226542_.chunkGenerator().getSeaLevel()
            ? Optional.empty()
            : onTopOfChunkCenter(p_226542_, Heightmap.Types.WORLD_SURFACE_WG, p_226545_ -> this.generatePieces(p_226545_, p_226542_));
    }

    private void generatePieces(StructurePiecesBuilder pBuilder, Structure.GenerationContext pContext) {
        ChunkPos chunkpos = pContext.chunkPos();
        pBuilder.addPiece(this.constructor.construct(pContext.random(), chunkpos.getMinBlockX(), chunkpos.getMinBlockZ()));
    }

    @FunctionalInterface
    protected interface PieceConstructor {
        StructurePiece construct(WorldgenRandom pRandom, int pMinBlockX, int pMinBlockZ);
    }
}