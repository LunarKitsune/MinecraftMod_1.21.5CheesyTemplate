package net.minecraft.world.level.entity;

import it.unimi.dsi.fastutil.longs.Long2ObjectFunction;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSortedSet;
import java.util.Objects;
import java.util.Spliterators;
import java.util.PrimitiveIterator.OfLong;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import net.minecraft.core.SectionPos;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;

public class EntitySectionStorage<T extends EntityAccess> {
    public static final int CHONKY_ENTITY_SEARCH_GRACE = 2;
    public static final int MAX_NON_CHONKY_ENTITY_SIZE = 4;
    private final Class<T> entityClass;
    private final Long2ObjectFunction<Visibility> intialSectionVisibility;
    private final Long2ObjectMap<EntitySection<T>> sections = new Long2ObjectOpenHashMap<>();
    private final LongSortedSet sectionIds = new LongAVLTreeSet();

    public EntitySectionStorage(Class<T> pEntityClass, Long2ObjectFunction<Visibility> pInitialSectionVisibility) {
        this.entityClass = pEntityClass;
        this.intialSectionVisibility = pInitialSectionVisibility;
    }

    public void forEachAccessibleNonEmptySection(AABB pBoundingBox, AbortableIterationConsumer<EntitySection<T>> pConsumer) {
        int i = SectionPos.posToSectionCoord(pBoundingBox.minX - 2.0);
        int j = SectionPos.posToSectionCoord(pBoundingBox.minY - 4.0);
        int k = SectionPos.posToSectionCoord(pBoundingBox.minZ - 2.0);
        int l = SectionPos.posToSectionCoord(pBoundingBox.maxX + 2.0);
        int i1 = SectionPos.posToSectionCoord(pBoundingBox.maxY + 0.0);
        int j1 = SectionPos.posToSectionCoord(pBoundingBox.maxZ + 2.0);

        for (int k1 = i; k1 <= l; k1++) {
            long l1 = SectionPos.asLong(k1, 0, 0);
            long i2 = SectionPos.asLong(k1, -1, -1);
            LongIterator longiterator = this.sectionIds.subSet(l1, i2 + 1L).iterator();

            while (longiterator.hasNext()) {
                long j2 = longiterator.nextLong();
                int k2 = SectionPos.y(j2);
                int l2 = SectionPos.z(j2);
                if (k2 >= j && k2 <= i1 && l2 >= k && l2 <= j1) {
                    EntitySection<T> entitysection = this.sections.get(j2);
                    if (entitysection != null
                        && !entitysection.isEmpty()
                        && entitysection.getStatus().isAccessible()
                        && pConsumer.accept(entitysection).shouldAbort()) {
                        return;
                    }
                }
            }
        }
    }

    public LongStream getExistingSectionPositionsInChunk(long pPos) {
        int i = ChunkPos.getX(pPos);
        int j = ChunkPos.getZ(pPos);
        LongSortedSet longsortedset = this.getChunkSections(i, j);
        if (longsortedset.isEmpty()) {
            return LongStream.empty();
        } else {
            OfLong oflong = longsortedset.iterator();
            return StreamSupport.longStream(Spliterators.spliteratorUnknownSize(oflong, 1301), false);
        }
    }

    private LongSortedSet getChunkSections(int pX, int pZ) {
        long i = SectionPos.asLong(pX, 0, pZ);
        long j = SectionPos.asLong(pX, -1, pZ);
        return this.sectionIds.subSet(i, j + 1L);
    }

    public Stream<EntitySection<T>> getExistingSectionsInChunk(long pPos) {
        return this.getExistingSectionPositionsInChunk(pPos).mapToObj(this.sections::get).filter(Objects::nonNull);
    }

    private static long getChunkKeyFromSectionKey(long pPos) {
        return ChunkPos.asLong(SectionPos.x(pPos), SectionPos.z(pPos));
    }

    public EntitySection<T> getOrCreateSection(long pSectionPos) {
        return this.sections.computeIfAbsent(pSectionPos, this::createSection);
    }

    @Nullable
    public EntitySection<T> getSection(long pSectionPos) {
        return this.sections.get(pSectionPos);
    }

    private EntitySection<T> createSection(long pSectionPos) {
        long i = getChunkKeyFromSectionKey(pSectionPos);
        Visibility visibility = this.intialSectionVisibility.get(i);
        this.sectionIds.add(pSectionPos);
        return new EntitySection<>(this.entityClass, visibility);
    }

    public LongSet getAllChunksWithExistingSections() {
        LongSet longset = new LongOpenHashSet();
        this.sections.keySet().forEach((long p_156886_) -> longset.add(getChunkKeyFromSectionKey(p_156886_)));
        return longset;
    }

    public void getEntities(AABB pBounds, AbortableIterationConsumer<T> pConsumer) {
        this.forEachAccessibleNonEmptySection(pBounds, p_261459_ -> p_261459_.getEntities(pBounds, pConsumer));
    }

    public <U extends T> void getEntities(EntityTypeTest<T, U> pTest, AABB pBounds, AbortableIterationConsumer<U> pConsumer) {
        this.forEachAccessibleNonEmptySection(pBounds, p_261463_ -> p_261463_.getEntities(pTest, pBounds, pConsumer));
    }

    public void remove(long pSectionId) {
        this.sections.remove(pSectionId);
        this.sectionIds.remove(pSectionId);
    }

    @VisibleForDebug
    public int count() {
        return this.sectionIds.size();
    }
}