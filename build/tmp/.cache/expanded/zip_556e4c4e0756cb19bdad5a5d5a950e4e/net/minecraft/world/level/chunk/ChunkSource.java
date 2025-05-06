package net.minecraft.world.level.chunk;

import it.unimi.dsi.fastutil.longs.LongSet;
import java.io.IOException;
import java.util.function.BooleanSupplier;
import javax.annotation.Nullable;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.lighting.LevelLightEngine;

public abstract class ChunkSource implements LightChunkGetter, AutoCloseable {
    @Nullable
    public LevelChunk getChunk(int pChunkX, int pChunkZ, boolean pLoad) {
        return (LevelChunk)this.getChunk(pChunkX, pChunkZ, ChunkStatus.FULL, pLoad);
    }

    @Nullable
    public LevelChunk getChunkNow(int pChunkX, int pChunkZ) {
        return this.getChunk(pChunkX, pChunkZ, false);
    }

    @Nullable
    @Override
    public LightChunk getChunkForLighting(int p_62241_, int p_62242_) {
        return this.getChunk(p_62241_, p_62242_, ChunkStatus.EMPTY, false);
    }

    public boolean hasChunk(int pChunkX, int pChunkZ) {
        return this.getChunk(pChunkX, pChunkZ, ChunkStatus.FULL, false) != null;
    }

    @Nullable
    public abstract ChunkAccess getChunk(int pX, int pZ, ChunkStatus pChunkStatus, boolean pRequireChunk);

    public abstract void tick(BooleanSupplier pHasTimeLeft, boolean pTickChunks);

    public void onSectionEmptinessChanged(int pX, int pY, int pZ, boolean pIsEmpty) {
    }

    public abstract String gatherStats();

    public abstract int getLoadedChunksCount();

    @Override
    public void close() throws IOException {
    }

    public abstract LevelLightEngine getLightEngine();

    public void setSpawnSettings(boolean pSpawnSettings) {
    }

    public boolean updateChunkForced(ChunkPos pChunkPos, boolean pAdd) {
        return false;
    }

    public LongSet getForceLoadedChunks() {
        return LongSet.of();
    }
}