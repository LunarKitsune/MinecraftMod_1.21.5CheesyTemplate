package net.minecraft.world.level;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;

public interface LevelHeightAccessor {
    int getHeight();

    int getMinY();

    default int getMaxY() {
        return this.getMinY() + this.getHeight() - 1;
    }

    default int getSectionsCount() {
        return this.getMaxSectionY() - this.getMinSectionY() + 1;
    }

    default int getMinSectionY() {
        return SectionPos.blockToSectionCoord(this.getMinY());
    }

    default int getMaxSectionY() {
        return SectionPos.blockToSectionCoord(this.getMaxY());
    }

    default boolean isInsideBuildHeight(int pY) {
        return pY >= this.getMinY() && pY <= this.getMaxY();
    }

    default boolean isOutsideBuildHeight(BlockPos pPos) {
        return this.isOutsideBuildHeight(pPos.getY());
    }

    default boolean isOutsideBuildHeight(int pY) {
        return pY < this.getMinY() || pY > this.getMaxY();
    }

    default int getSectionIndex(int pY) {
        return this.getSectionIndexFromSectionY(SectionPos.blockToSectionCoord(pY));
    }

    default int getSectionIndexFromSectionY(int pSectionIndex) {
        return pSectionIndex - this.getMinSectionY();
    }

    default int getSectionYFromSectionIndex(int pSectionIndex) {
        return pSectionIndex + this.getMinSectionY();
    }

    static LevelHeightAccessor create(final int pMinBuildHeight, final int pHeight) {
        return new LevelHeightAccessor() {
            @Override
            public int getHeight() {
                return pHeight;
            }

            @Override
            public int getMinY() {
                return pMinBuildHeight;
            }
        };
    }
}