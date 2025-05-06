package net.minecraft.world.level.chunk.storage;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.OptionalDynamic;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import org.slf4j.Logger;

public class SectionStorage<R, P> implements AutoCloseable {
    static final Logger LOGGER = LogUtils.getLogger();
    private static final String SECTIONS_TAG = "Sections";
    private final SimpleRegionStorage simpleRegionStorage;
    private final Long2ObjectMap<Optional<R>> storage = new Long2ObjectOpenHashMap<>();
    private final LongLinkedOpenHashSet dirtyChunks = new LongLinkedOpenHashSet();
    private final Codec<P> codec;
    private final Function<R, P> packer;
    private final BiFunction<P, Runnable, R> unpacker;
    private final Function<Runnable, R> factory;
    private final RegistryAccess registryAccess;
    private final ChunkIOErrorReporter errorReporter;
    protected final LevelHeightAccessor levelHeightAccessor;
    private final LongSet loadedChunks = new LongOpenHashSet();
    private final Long2ObjectMap<CompletableFuture<Optional<SectionStorage.PackedChunk<P>>>> pendingLoads = new Long2ObjectOpenHashMap<>();
    private final Object loadLock = new Object();

    public SectionStorage(
        SimpleRegionStorage pSimpleRegionStorage,
        Codec<P> pCodec,
        Function<R, P> pPacker,
        BiFunction<P, Runnable, R> pUnpacker,
        Function<Runnable, R> pFactory,
        RegistryAccess pRegistryAccess,
        ChunkIOErrorReporter pErrorReporter,
        LevelHeightAccessor pLevelHeightAccessor
    ) {
        this.simpleRegionStorage = pSimpleRegionStorage;
        this.codec = pCodec;
        this.packer = pPacker;
        this.unpacker = pUnpacker;
        this.factory = pFactory;
        this.registryAccess = pRegistryAccess;
        this.errorReporter = pErrorReporter;
        this.levelHeightAccessor = pLevelHeightAccessor;
    }

    protected void tick(BooleanSupplier pAheadOfTime) {
        LongIterator longiterator = this.dirtyChunks.iterator();

        while (longiterator.hasNext() && pAheadOfTime.getAsBoolean()) {
            ChunkPos chunkpos = new ChunkPos(longiterator.nextLong());
            longiterator.remove();
            this.writeChunk(chunkpos);
        }

        this.unpackPendingLoads();
    }

    private void unpackPendingLoads() {
        synchronized (this.loadLock) {
            Iterator<Entry<CompletableFuture<Optional<SectionStorage.PackedChunk<P>>>>> iterator = Long2ObjectMaps.fastIterator(this.pendingLoads);

            while (iterator.hasNext()) {
                Entry<CompletableFuture<Optional<SectionStorage.PackedChunk<P>>>> entry = iterator.next();
                Optional<SectionStorage.PackedChunk<P>> optional = entry.getValue().getNow(null);
                if (optional != null) {
                    long i = entry.getLongKey();
                    this.unpackChunk(new ChunkPos(i), optional.orElse(null));
                    iterator.remove();
                    this.loadedChunks.add(i);
                }
            }
        }
    }

    public void flushAll() {
        if (!this.dirtyChunks.isEmpty()) {
            this.dirtyChunks.forEach(p_360574_ -> this.writeChunk(new ChunkPos(p_360574_)));
            this.dirtyChunks.clear();
        }
    }

    public boolean hasWork() {
        return !this.dirtyChunks.isEmpty();
    }

    @Nullable
    protected Optional<R> get(long pSectionKey) {
        return this.storage.get(pSectionKey);
    }

    protected Optional<R> getOrLoad(long pSectionKey) {
        if (this.outsideStoredRange(pSectionKey)) {
            return Optional.empty();
        } else {
            Optional<R> optional = this.get(pSectionKey);
            if (optional != null) {
                return optional;
            } else {
                this.unpackChunk(SectionPos.of(pSectionKey).chunk());
                optional = this.get(pSectionKey);
                if (optional == null) {
                    throw (IllegalStateException)Util.pauseInIde(new IllegalStateException());
                } else {
                    return optional;
                }
            }
        }
    }

    protected boolean outsideStoredRange(long pSectionKey) {
        int i = SectionPos.sectionToBlockCoord(SectionPos.y(pSectionKey));
        return this.levelHeightAccessor.isOutsideBuildHeight(i);
    }

    protected R getOrCreate(long pSectionKey) {
        if (this.outsideStoredRange(pSectionKey)) {
            throw (IllegalArgumentException)Util.pauseInIde(new IllegalArgumentException("sectionPos out of bounds"));
        } else {
            Optional<R> optional = this.getOrLoad(pSectionKey);
            if (optional.isPresent()) {
                return optional.get();
            } else {
                R r = this.factory.apply(() -> this.setDirty(pSectionKey));
                this.storage.put(pSectionKey, Optional.of(r));
                return r;
            }
        }
    }

    public CompletableFuture<?> prefetch(ChunkPos pPos) {
        synchronized (this.loadLock) {
            long i = pPos.toLong();
            return this.loadedChunks.contains(i)
                ? CompletableFuture.completedFuture(null)
                : this.pendingLoads.computeIfAbsent(i, p_360582_ -> this.tryRead(pPos));
        }
    }

    private void unpackChunk(ChunkPos pPos) {
        long i = pPos.toLong();
        CompletableFuture<Optional<SectionStorage.PackedChunk<P>>> completablefuture;
        synchronized (this.loadLock) {
            if (!this.loadedChunks.add(i)) {
                return;
            }

            completablefuture = this.pendingLoads.computeIfAbsent(i, p_360576_ -> this.tryRead(pPos));
        }

        this.unpackChunk(pPos, completablefuture.join().orElse(null));
        synchronized (this.loadLock) {
            this.pendingLoads.remove(i);
        }
    }

    private CompletableFuture<Optional<SectionStorage.PackedChunk<P>>> tryRead(ChunkPos pChunkPos) {
        RegistryOps<Tag> registryops = this.registryAccess.createSerializationContext(NbtOps.INSTANCE);
        return this.simpleRegionStorage
            .read(pChunkPos)
            .thenApplyAsync(
                p_360573_ -> p_360573_.map(
                    p_360578_ -> SectionStorage.PackedChunk.parse(this.codec, registryops, p_360578_, this.simpleRegionStorage, this.levelHeightAccessor)
                ),
                Util.backgroundExecutor().forName("parseSection")
            )
            .exceptionally(p_375353_ -> {
                if (p_375353_ instanceof CompletionException) {
                    p_375353_ = p_375353_.getCause();
                }

                if (p_375353_ instanceof IOException ioexception) {
                    LOGGER.error("Error reading chunk {} data from disk", pChunkPos, ioexception);
                    this.errorReporter.reportChunkLoadFailure(ioexception, this.simpleRegionStorage.storageInfo(), pChunkPos);
                    return Optional.empty();
                } else {
                    throw new CompletionException(p_375353_);
                }
            });
    }

    private void unpackChunk(ChunkPos pPos, @Nullable SectionStorage.PackedChunk<P> pPackedChunk) {
        if (pPackedChunk == null) {
            for (int i = this.levelHeightAccessor.getMinSectionY(); i <= this.levelHeightAccessor.getMaxSectionY(); i++) {
                this.storage.put(getKey(pPos, i), Optional.empty());
            }
        } else {
            boolean flag = pPackedChunk.versionChanged();

            for (int j = this.levelHeightAccessor.getMinSectionY(); j <= this.levelHeightAccessor.getMaxSectionY(); j++) {
                long k = getKey(pPos, j);
                Optional<R> optional = Optional.ofNullable(pPackedChunk.sectionsByY.get(j))
                    .map(p_360580_ -> this.unpacker.apply((P)p_360580_, () -> this.setDirty(k)));
                this.storage.put(k, optional);
                optional.ifPresent(p_223523_ -> {
                    this.onSectionLoad(k);
                    if (flag) {
                        this.setDirty(k);
                    }
                });
            }
        }
    }

    private void writeChunk(ChunkPos pPos) {
        RegistryOps<Tag> registryops = this.registryAccess.createSerializationContext(NbtOps.INSTANCE);
        Dynamic<Tag> dynamic = this.writeChunk(pPos, registryops);
        Tag tag = dynamic.getValue();
        if (tag instanceof CompoundTag) {
            this.simpleRegionStorage.write(pPos, (CompoundTag)tag).exceptionally(p_341891_ -> {
                this.errorReporter.reportChunkSaveFailure(p_341891_, this.simpleRegionStorage.storageInfo(), pPos);
                return null;
            });
        } else {
            LOGGER.error("Expected compound tag, got {}", tag);
        }
    }

    private <T> Dynamic<T> writeChunk(ChunkPos pPos, DynamicOps<T> pOps) {
        Map<T, T> map = Maps.newHashMap();

        for (int i = this.levelHeightAccessor.getMinSectionY(); i <= this.levelHeightAccessor.getMaxSectionY(); i++) {
            long j = getKey(pPos, i);
            Optional<R> optional = this.storage.get(j);
            if (optional != null && !optional.isEmpty()) {
                DataResult<T> dataresult = this.codec.encodeStart(pOps, this.packer.apply(optional.get()));
                String s = Integer.toString(i);
                dataresult.resultOrPartial(LOGGER::error).ifPresent(p_223531_ -> map.put(pOps.createString(s), (T)p_223531_));
            }
        }

        return new Dynamic<>(
            pOps,
            pOps.createMap(
                ImmutableMap.of(
                    pOps.createString("Sections"),
                    pOps.createMap(map),
                    pOps.createString("DataVersion"),
                    pOps.createInt(SharedConstants.getCurrentVersion().getDataVersion().getVersion())
                )
            )
        );
    }

    private static long getKey(ChunkPos pChunkPos, int pSectionY) {
        return SectionPos.asLong(pChunkPos.x, pSectionY, pChunkPos.z);
    }

    protected void onSectionLoad(long pSectionKey) {
    }

    protected void setDirty(long pSectionPos) {
        Optional<R> optional = this.storage.get(pSectionPos);
        if (optional != null && !optional.isEmpty()) {
            this.dirtyChunks.add(ChunkPos.asLong(SectionPos.x(pSectionPos), SectionPos.z(pSectionPos)));
        } else {
            LOGGER.warn("No data for position: {}", SectionPos.of(pSectionPos));
        }
    }

    static int getVersion(Dynamic<?> pColumnData) {
        return pColumnData.get("DataVersion").asInt(1945);
    }

    public void flush(ChunkPos pChunkPos) {
        if (this.dirtyChunks.remove(pChunkPos.toLong())) {
            this.writeChunk(pChunkPos);
        }
    }

    @Override
    public void close() throws IOException {
        this.simpleRegionStorage.close();
    }

    record PackedChunk<T>(Int2ObjectMap<T> sectionsByY, boolean versionChanged) {
        public static <T> SectionStorage.PackedChunk<T> parse(
            Codec<T> pCodec, DynamicOps<Tag> pOps, Tag pValue, SimpleRegionStorage pSimpleRegionStorage, LevelHeightAccessor pLevelHeightAccessor
        ) {
            Dynamic<Tag> dynamic = new Dynamic<>(pOps, pValue);
            int i = SectionStorage.getVersion(dynamic);
            int j = SharedConstants.getCurrentVersion().getDataVersion().getVersion();
            boolean flag = i != j;
            Dynamic<Tag> dynamic1 = pSimpleRegionStorage.upgradeChunkTag(dynamic, i);
            OptionalDynamic<Tag> optionaldynamic = dynamic1.get("Sections");
            Int2ObjectMap<T> int2objectmap = new Int2ObjectOpenHashMap<>();

            for (int k = pLevelHeightAccessor.getMinSectionY(); k <= pLevelHeightAccessor.getMaxSectionY(); k++) {
                Optional<T> optional = optionaldynamic.get(Integer.toString(k))
                    .result()
                    .flatMap(p_368164_ -> pCodec.parse((Dynamic<Tag>)p_368164_).resultOrPartial(SectionStorage.LOGGER::error));
                if (optional.isPresent()) {
                    int2objectmap.put(k, optional.get());
                }
            }

            return new SectionStorage.PackedChunk<>(int2objectmap, flag);
        }
    }
}