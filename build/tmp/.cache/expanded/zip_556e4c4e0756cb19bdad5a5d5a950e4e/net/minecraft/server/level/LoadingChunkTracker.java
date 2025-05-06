package net.minecraft.server.level;

import net.minecraft.world.level.TicketStorage;

class LoadingChunkTracker extends ChunkTracker {
    private static final int MAX_LEVEL = ChunkLevel.MAX_LEVEL + 1;
    private final DistanceManager distanceManager;
    private final TicketStorage ticketStorage;

    public LoadingChunkTracker(DistanceManager pDistanceManager, TicketStorage pTicketStorage) {
        super(MAX_LEVEL + 1, 16, 256);
        this.distanceManager = pDistanceManager;
        this.ticketStorage = pTicketStorage;
        pTicketStorage.setLoadingChunkUpdatedListener(this::update);
    }

    @Override
    protected int getLevelFromSource(long p_391415_) {
        return this.ticketStorage.getTicketLevelAt(p_391415_, false);
    }

    @Override
    protected int getLevel(long p_395921_) {
        if (!this.distanceManager.isChunkToRemove(p_395921_)) {
            ChunkHolder chunkholder = this.distanceManager.getChunk(p_395921_);
            if (chunkholder != null) {
                return chunkholder.getTicketLevel();
            }
        }

        return MAX_LEVEL;
    }

    @Override
    protected void setLevel(long p_391454_, int p_396240_) {
        ChunkHolder chunkholder = this.distanceManager.getChunk(p_391454_);
        int i = chunkholder == null ? MAX_LEVEL : chunkholder.getTicketLevel();
        if (i != p_396240_) {
            chunkholder = this.distanceManager.updateChunkScheduling(p_391454_, p_396240_, chunkholder, i);
            if (chunkholder != null) {
                this.distanceManager.chunksToUpdateFutures.add(chunkholder);
            }
        }
    }

    public int runDistanceUpdates(int pToUpdateCount) {
        return this.runUpdates(pToUpdateCount);
    }
}