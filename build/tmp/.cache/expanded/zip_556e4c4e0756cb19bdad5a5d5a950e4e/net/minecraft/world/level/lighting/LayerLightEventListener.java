package net.minecraft.world.level.lighting;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.DataLayer;

public interface LayerLightEventListener extends LightEventListener {
    @Nullable
    DataLayer getDataLayerData(SectionPos pSectionPos);

    int getLightValue(BlockPos pLevelPos);

    public static enum DummyLightLayerEventListener implements LayerLightEventListener {
        INSTANCE;

        @Nullable
        @Override
        public DataLayer getDataLayerData(SectionPos p_75718_) {
            return null;
        }

        @Override
        public int getLightValue(BlockPos pLevelPos) {
            return 0;
        }

        @Override
        public void checkBlock(BlockPos p_164434_) {
        }

        @Override
        public boolean hasLightWork() {
            return false;
        }

        @Override
        public int runLightUpdates() {
            return 0;
        }

        @Override
        public void updateSectionStatus(SectionPos pPos, boolean pIsEmpty) {
        }

        @Override
        public void setLightEnabled(ChunkPos p_164431_, boolean p_164432_) {
        }

        @Override
        public void propagateLightSources(ChunkPos p_285209_) {
        }
    }
}