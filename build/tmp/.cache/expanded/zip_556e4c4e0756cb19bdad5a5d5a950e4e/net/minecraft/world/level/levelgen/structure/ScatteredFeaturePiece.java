package net.minecraft.world.level.levelgen.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

public abstract class ScatteredFeaturePiece extends StructurePiece {
    protected final int width;
    protected final int height;
    protected final int depth;
    protected int heightPosition = -1;

    protected ScatteredFeaturePiece(
        StructurePieceType pType, int pX, int pY, int pZ, int pWidth, int pHeight, int pDepth, Direction pOrientation
    ) {
        super(pType, 0, StructurePiece.makeBoundingBox(pX, pY, pZ, pOrientation, pWidth, pHeight, pDepth));
        this.width = pWidth;
        this.height = pHeight;
        this.depth = pDepth;
        this.setOrientation(pOrientation);
    }

    protected ScatteredFeaturePiece(StructurePieceType p_209929_, CompoundTag p_209930_) {
        super(p_209929_, p_209930_);
        this.width = p_209930_.getIntOr("Width", 0);
        this.height = p_209930_.getIntOr("Height", 0);
        this.depth = p_209930_.getIntOr("Depth", 0);
        this.heightPosition = p_209930_.getIntOr("HPos", 0);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext p_192471_, CompoundTag p_192472_) {
        p_192472_.putInt("Width", this.width);
        p_192472_.putInt("Height", this.height);
        p_192472_.putInt("Depth", this.depth);
        p_192472_.putInt("HPos", this.heightPosition);
    }

    protected boolean updateAverageGroundHeight(LevelAccessor pLevel, BoundingBox pBounds, int pHeight) {
        if (this.heightPosition >= 0) {
            return true;
        } else {
            int i = 0;
            int j = 0;
            BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

            for (int k = this.boundingBox.minZ(); k <= this.boundingBox.maxZ(); k++) {
                for (int l = this.boundingBox.minX(); l <= this.boundingBox.maxX(); l++) {
                    blockpos$mutableblockpos.set(l, 64, k);
                    if (pBounds.isInside(blockpos$mutableblockpos)) {
                        i += pLevel.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, blockpos$mutableblockpos).getY();
                        j++;
                    }
                }
            }

            if (j == 0) {
                return false;
            } else {
                this.heightPosition = i / j;
                this.boundingBox.move(0, this.heightPosition - this.boundingBox.minY() + pHeight, 0);
                return true;
            }
        }
    }

    protected boolean updateHeightPositionToLowestGroundHeight(LevelAccessor pLevel, int pHeight) {
        if (this.heightPosition >= 0) {
            return true;
        } else {
            int i = pLevel.getMaxY() + 1;
            boolean flag = false;
            BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

            for (int j = this.boundingBox.minZ(); j <= this.boundingBox.maxZ(); j++) {
                for (int k = this.boundingBox.minX(); k <= this.boundingBox.maxX(); k++) {
                    blockpos$mutableblockpos.set(k, 0, j);
                    i = Math.min(i, pLevel.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, blockpos$mutableblockpos).getY());
                    flag = true;
                }
            }

            if (!flag) {
                return false;
            } else {
                this.heightPosition = i;
                this.boundingBox.move(0, this.heightPosition - this.boundingBox.minY() + pHeight, 0);
                return true;
            }
        }
    }
}