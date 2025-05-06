package net.minecraft.client.multiplayer;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.EmptyLevelChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ClientChunkCache extends ChunkSource {
    static final Logger LOGGER = LogUtils.getLogger();
    private final LevelChunk emptyChunk;
    private final LevelLightEngine lightEngine;
    volatile ClientChunkCache.Storage storage;
    final ClientLevel level;

    public ClientChunkCache(ClientLevel pLevel, int pViewDistance) {
        this.level = pLevel;
        this.emptyChunk = new EmptyLevelChunk(pLevel, new ChunkPos(0, 0), pLevel.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(Biomes.PLAINS));
        this.lightEngine = new LevelLightEngine(this, true, pLevel.dimensionType().hasSkyLight());
        this.storage = new ClientChunkCache.Storage(calculateStorageRange(pViewDistance));
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return this.lightEngine;
    }

    private static boolean isValidChunk(@Nullable LevelChunk pChunk, int pX, int pZ) {
        if (pChunk == null) {
            return false;
        } else {
            ChunkPos chunkpos = pChunk.getPos();
            return chunkpos.x == pX && chunkpos.z == pZ;
        }
    }

    public void drop(ChunkPos pChunkPos) {
        if (this.storage.inRange(pChunkPos.x, pChunkPos.z)) {
            int i = this.storage.getIndex(pChunkPos.x, pChunkPos.z);
            LevelChunk levelchunk = this.storage.getChunk(i);
            if (isValidChunk(levelchunk, pChunkPos.x, pChunkPos.z)) {
                net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.level.ChunkEvent.Unload(levelchunk));
                this.storage.drop(i, levelchunk);
            }
        }
    }

    @Nullable
    public LevelChunk getChunk(int p_104451_, int p_104452_, ChunkStatus p_334602_, boolean p_104454_) {
        if (this.storage.inRange(p_104451_, p_104452_)) {
            LevelChunk levelchunk = this.storage.getChunk(this.storage.getIndex(p_104451_, p_104452_));
            if (isValidChunk(levelchunk, p_104451_, p_104452_)) {
                return levelchunk;
            }
        }

        return p_104454_ ? this.emptyChunk : null;
    }

    @Override
    public BlockGetter getLevel() {
        return this.level;
    }

    public void replaceBiomes(int pX, int pZ, FriendlyByteBuf pBuffer) {
        if (!this.storage.inRange(pX, pZ)) {
            LOGGER.warn("Ignoring chunk since it's not in the view range: {}, {}", pX, pZ);
        } else {
            int i = this.storage.getIndex(pX, pZ);
            LevelChunk levelchunk = this.storage.chunks.get(i);
            if (!isValidChunk(levelchunk, pX, pZ)) {
                LOGGER.warn("Ignoring chunk since it's not present: {}, {}", pX, pZ);
            } else {
                levelchunk.replaceBiomes(pBuffer);
            }
        }
    }

    @Nullable
    public LevelChunk replaceWithPacketData(
        int pX,
        int pZ,
        FriendlyByteBuf pReadBuffer,
        Map<Heightmap.Types, long[]> pHeightmaps,
        Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> pConsumer
    ) {
        if (!this.storage.inRange(pX, pZ)) {
            LOGGER.warn("Ignoring chunk since it's not in the view range: {}, {}", pX, pZ);
            return null;
        } else {
            int i = this.storage.getIndex(pX, pZ);
            LevelChunk levelchunk = this.storage.chunks.get(i);
            ChunkPos chunkpos = new ChunkPos(pX, pZ);
            if (!isValidChunk(levelchunk, pX, pZ)) {
                levelchunk = new LevelChunk(this.level, chunkpos);
                levelchunk.replaceWithPacketData(pReadBuffer, pHeightmaps, pConsumer);
                this.storage.replace(i, levelchunk);
            } else {
                levelchunk.replaceWithPacketData(pReadBuffer, pHeightmaps, pConsumer);
                this.storage.refreshEmptySections(levelchunk);
            }

            this.level.onChunkLoaded(chunkpos);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.level.ChunkEvent.Load(levelchunk, false));
            return levelchunk;
        }
    }

    @Override
    public void tick(BooleanSupplier p_202421_, boolean p_202422_) {
    }

    public void updateViewCenter(int pX, int pZ) {
        this.storage.viewCenterX = pX;
        this.storage.viewCenterZ = pZ;
    }

    public void updateViewRadius(int pViewDistance) {
        int i = this.storage.chunkRadius;
        int j = calculateStorageRange(pViewDistance);
        if (i != j) {
            ClientChunkCache.Storage clientchunkcache$storage = new ClientChunkCache.Storage(j);
            clientchunkcache$storage.viewCenterX = this.storage.viewCenterX;
            clientchunkcache$storage.viewCenterZ = this.storage.viewCenterZ;

            for (int k = 0; k < this.storage.chunks.length(); k++) {
                LevelChunk levelchunk = this.storage.chunks.get(k);
                if (levelchunk != null) {
                    ChunkPos chunkpos = levelchunk.getPos();
                    if (clientchunkcache$storage.inRange(chunkpos.x, chunkpos.z)) {
                        clientchunkcache$storage.replace(clientchunkcache$storage.getIndex(chunkpos.x, chunkpos.z), levelchunk);
                    }
                }
            }

            this.storage = clientchunkcache$storage;
        }
    }

    private static int calculateStorageRange(int pViewDistance) {
        return Math.max(2, pViewDistance) + 3;
    }

    @Override
    public String gatherStats() {
        return this.storage.chunks.length() + ", " + this.getLoadedChunksCount();
    }

    @Override
    public int getLoadedChunksCount() {
        return this.storage.chunkCount;
    }

    @Override
    public void onLightUpdate(LightLayer pType, SectionPos pPos) {
        Minecraft.getInstance().levelRenderer.setSectionDirty(pPos.x(), pPos.y(), pPos.z());
    }

    public LongOpenHashSet getLoadedEmptySections() {
        return this.storage.loadedEmptySections;
    }

    @Override
    public void onSectionEmptinessChanged(int p_366771_, int p_363867_, int p_364686_, boolean p_362705_) {
        this.storage.onSectionEmptinessChanged(p_366771_, p_363867_, p_364686_, p_362705_);
    }

    @OnlyIn(Dist.CLIENT)
    final class Storage {
        final AtomicReferenceArray<LevelChunk> chunks;
        final LongOpenHashSet loadedEmptySections = new LongOpenHashSet();
        final int chunkRadius;
        private final int viewRange;
        volatile int viewCenterX;
        volatile int viewCenterZ;
        int chunkCount;

        Storage(final int pChunkRadius) {
            this.chunkRadius = pChunkRadius;
            this.viewRange = pChunkRadius * 2 + 1;
            this.chunks = new AtomicReferenceArray<>(this.viewRange * this.viewRange);
        }

        int getIndex(int pX, int pZ) {
            return Math.floorMod(pZ, this.viewRange) * this.viewRange + Math.floorMod(pX, this.viewRange);
        }

        void replace(int pChunkIndex, @Nullable LevelChunk pChunk) {
            LevelChunk levelchunk = this.chunks.getAndSet(pChunkIndex, pChunk);
            if (levelchunk != null) {
                this.chunkCount--;
                this.dropEmptySections(levelchunk);
                ClientChunkCache.this.level.unload(levelchunk);
            }

            if (pChunk != null) {
                this.chunkCount++;
                this.addEmptySections(pChunk);
            }
        }

        void drop(int pChunkIndex, LevelChunk pChunk) {
            if (this.chunks.compareAndSet(pChunkIndex, pChunk, null)) {
                this.chunkCount--;
                this.dropEmptySections(pChunk);
            }

            ClientChunkCache.this.level.unload(pChunk);
        }

        public void onSectionEmptinessChanged(int pX, int pY, int pZ, boolean pIsEmpty) {
            if (this.inRange(pX, pZ)) {
                long i = SectionPos.asLong(pX, pY, pZ);
                if (pIsEmpty) {
                    this.loadedEmptySections.add(i);
                } else if (this.loadedEmptySections.remove(i)) {
                    ClientChunkCache.this.level.onSectionBecomingNonEmpty(i);
                }
            }
        }

        private void dropEmptySections(LevelChunk pChunk) {
            LevelChunkSection[] alevelchunksection = pChunk.getSections();

            for (int i = 0; i < alevelchunksection.length; i++) {
                ChunkPos chunkpos = pChunk.getPos();
                this.loadedEmptySections.remove(SectionPos.asLong(chunkpos.x, pChunk.getSectionYFromSectionIndex(i), chunkpos.z));
            }
        }

        private void addEmptySections(LevelChunk pChunk) {
            LevelChunkSection[] alevelchunksection = pChunk.getSections();

            for (int i = 0; i < alevelchunksection.length; i++) {
                LevelChunkSection levelchunksection = alevelchunksection[i];
                if (levelchunksection.hasOnlyAir()) {
                    ChunkPos chunkpos = pChunk.getPos();
                    this.loadedEmptySections.add(SectionPos.asLong(chunkpos.x, pChunk.getSectionYFromSectionIndex(i), chunkpos.z));
                }
            }
        }

        void refreshEmptySections(LevelChunk pChunk) {
            ChunkPos chunkpos = pChunk.getPos();
            LevelChunkSection[] alevelchunksection = pChunk.getSections();

            for (int i = 0; i < alevelchunksection.length; i++) {
                LevelChunkSection levelchunksection = alevelchunksection[i];
                long j = SectionPos.asLong(chunkpos.x, pChunk.getSectionYFromSectionIndex(i), chunkpos.z);
                if (levelchunksection.hasOnlyAir()) {
                    this.loadedEmptySections.add(j);
                } else if (this.loadedEmptySections.remove(j)) {
                    ClientChunkCache.this.level.onSectionBecomingNonEmpty(j);
                }
            }
        }

        boolean inRange(int pX, int pZ) {
            return Math.abs(pX - this.viewCenterX) <= this.chunkRadius && Math.abs(pZ - this.viewCenterZ) <= this.chunkRadius;
        }

        @Nullable
        protected LevelChunk getChunk(int pChunkIndex) {
            return this.chunks.get(pChunkIndex);
        }

        private void dumpChunks(String pFilePath) {
            try (FileOutputStream fileoutputstream = new FileOutputStream(pFilePath)) {
                int i = ClientChunkCache.this.storage.chunkRadius;

                for (int j = this.viewCenterZ - i; j <= this.viewCenterZ + i; j++) {
                    for (int k = this.viewCenterX - i; k <= this.viewCenterX + i; k++) {
                        LevelChunk levelchunk = ClientChunkCache.this.storage.chunks.get(ClientChunkCache.this.storage.getIndex(k, j));
                        if (levelchunk != null) {
                            ChunkPos chunkpos = levelchunk.getPos();
                            fileoutputstream.write(
                                (chunkpos.x + "\t" + chunkpos.z + "\t" + levelchunk.isEmpty() + "\n").getBytes(StandardCharsets.UTF_8)
                            );
                        }
                    }
                }
            } catch (IOException ioexception) {
                ClientChunkCache.LOGGER.error("Failed to dump chunks to file {}", pFilePath, ioexception);
            }
        }
    }
}
