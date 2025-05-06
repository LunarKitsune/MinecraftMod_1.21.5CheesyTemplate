package net.minecraft.client.renderer;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.util.Mth;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3d;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class SectionOcclusionGraph {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final int MINIMUM_ADVANCED_CULLING_DISTANCE = 60;
    private static final int MINIMUM_ADVANCED_CULLING_SECTION_DISTANCE = SectionPos.blockToSectionCoord(60);
    private static final double CEILED_SECTION_DIAGONAL = Math.ceil(Math.sqrt(3.0) * 16.0);
    private boolean needsFullUpdate = true;
    @Nullable
    private Future<?> fullUpdateTask;
    @Nullable
    private ViewArea viewArea;
    private final AtomicReference<SectionOcclusionGraph.GraphState> currentGraph = new AtomicReference<>();
    private final AtomicReference<SectionOcclusionGraph.GraphEvents> nextGraphEvents = new AtomicReference<>();
    private final AtomicBoolean needsFrustumUpdate = new AtomicBoolean(false);

    public void waitAndReset(@Nullable ViewArea pViewArea) {
        if (this.fullUpdateTask != null) {
            try {
                this.fullUpdateTask.get();
                this.fullUpdateTask = null;
            } catch (Exception exception) {
                LOGGER.warn("Full update failed", (Throwable)exception);
            }
        }

        this.viewArea = pViewArea;
        if (pViewArea != null) {
            this.currentGraph.set(new SectionOcclusionGraph.GraphState(pViewArea));
            this.invalidate();
        } else {
            this.currentGraph.set(null);
        }
    }

    public void invalidate() {
        this.needsFullUpdate = true;
    }

    public void addSectionsInFrustum(Frustum pFrustum, List<SectionRenderDispatcher.RenderSection> pVisibleSections, List<SectionRenderDispatcher.RenderSection> pNearbyVisibleSections) {
        this.currentGraph.get().storage().sectionTree.visitNodes((p_357918_, p_357919_, p_357920_, p_357921_) -> {
            SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection = p_357918_.getSection();
            if (sectionrenderdispatcher$rendersection != null) {
                pVisibleSections.add(sectionrenderdispatcher$rendersection);
                if (p_357921_) {
                    pNearbyVisibleSections.add(sectionrenderdispatcher$rendersection);
                }
            }
        }, pFrustum, 32);
    }

    public boolean consumeFrustumUpdate() {
        return this.needsFrustumUpdate.compareAndSet(true, false);
    }

    public void onChunkReadyToRender(ChunkPos pChunkPos) {
        SectionOcclusionGraph.GraphEvents sectionocclusiongraph$graphevents = this.nextGraphEvents.get();
        if (sectionocclusiongraph$graphevents != null) {
            this.addNeighbors(sectionocclusiongraph$graphevents, pChunkPos);
        }

        SectionOcclusionGraph.GraphEvents sectionocclusiongraph$graphevents1 = this.currentGraph.get().events;
        if (sectionocclusiongraph$graphevents1 != sectionocclusiongraph$graphevents) {
            this.addNeighbors(sectionocclusiongraph$graphevents1, pChunkPos);
        }
    }

    public void schedulePropagationFrom(SectionRenderDispatcher.RenderSection pSection) {
        SectionOcclusionGraph.GraphEvents sectionocclusiongraph$graphevents = this.nextGraphEvents.get();
        if (sectionocclusiongraph$graphevents != null) {
            sectionocclusiongraph$graphevents.sectionsToPropagateFrom.add(pSection);
        }

        SectionOcclusionGraph.GraphEvents sectionocclusiongraph$graphevents1 = this.currentGraph.get().events;
        if (sectionocclusiongraph$graphevents1 != sectionocclusiongraph$graphevents) {
            sectionocclusiongraph$graphevents1.sectionsToPropagateFrom.add(pSection);
        }
    }

    public void update(
        boolean pSmartCull, Camera pCamera, Frustum pFrustum, List<SectionRenderDispatcher.RenderSection> pVisibleSections, LongOpenHashSet pLoadedEmptySections
    ) {
        Vec3 vec3 = pCamera.getPosition();
        if (this.needsFullUpdate && (this.fullUpdateTask == null || this.fullUpdateTask.isDone())) {
            this.scheduleFullUpdate(pSmartCull, pCamera, vec3, pLoadedEmptySections);
        }

        this.runPartialUpdate(pSmartCull, pFrustum, pVisibleSections, vec3, pLoadedEmptySections);
    }

    private void scheduleFullUpdate(boolean pSmartCull, Camera pCamera, Vec3 pCameraPosition, LongOpenHashSet pLoadedEmptySections) {
        this.needsFullUpdate = false;
        LongOpenHashSet longopenhashset = pLoadedEmptySections.clone();
        this.fullUpdateTask = CompletableFuture.runAsync(() -> {
            SectionOcclusionGraph.GraphState sectionocclusiongraph$graphstate = new SectionOcclusionGraph.GraphState(this.viewArea);
            this.nextGraphEvents.set(sectionocclusiongraph$graphstate.events);
            Queue<SectionOcclusionGraph.Node> queue = Queues.newArrayDeque();
            this.initializeQueueForFullUpdate(pCamera, queue);
            queue.forEach(p_299757_ -> sectionocclusiongraph$graphstate.storage.sectionToNodeMap.put(p_299757_.section, p_299757_));
            this.runUpdates(sectionocclusiongraph$graphstate.storage, pCameraPosition, queue, pSmartCull, p_299279_ -> {}, longopenhashset);
            this.currentGraph.set(sectionocclusiongraph$graphstate);
            this.nextGraphEvents.set(null);
            this.needsFrustumUpdate.set(true);
        }, Util.backgroundExecutor());
    }

    private void runPartialUpdate(
        boolean pSmartCull, Frustum pFrustum, List<SectionRenderDispatcher.RenderSection> pVisibleSections, Vec3 pCameraPosition, LongOpenHashSet pLoadedEmptySections
    ) {
        SectionOcclusionGraph.GraphState sectionocclusiongraph$graphstate = this.currentGraph.get();
        this.queueSectionsWithNewNeighbors(sectionocclusiongraph$graphstate);
        if (!sectionocclusiongraph$graphstate.events.sectionsToPropagateFrom.isEmpty()) {
            Queue<SectionOcclusionGraph.Node> queue = Queues.newArrayDeque();

            while (!sectionocclusiongraph$graphstate.events.sectionsToPropagateFrom.isEmpty()) {
                SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection = sectionocclusiongraph$graphstate.events.sectionsToPropagateFrom.poll();
                SectionOcclusionGraph.Node sectionocclusiongraph$node = sectionocclusiongraph$graphstate.storage
                    .sectionToNodeMap
                    .get(sectionrenderdispatcher$rendersection);
                if (sectionocclusiongraph$node != null && sectionocclusiongraph$node.section == sectionrenderdispatcher$rendersection) {
                    queue.add(sectionocclusiongraph$node);
                }
            }

            Frustum frustum = LevelRenderer.offsetFrustum(pFrustum);
            Consumer<SectionRenderDispatcher.RenderSection> consumer = p_357923_ -> {
                if (frustum.isVisible(p_357923_.getBoundingBox())) {
                    this.needsFrustumUpdate.set(true);
                }
            };
            this.runUpdates(sectionocclusiongraph$graphstate.storage, pCameraPosition, queue, pSmartCull, consumer, pLoadedEmptySections);
        }
    }

    private void queueSectionsWithNewNeighbors(SectionOcclusionGraph.GraphState pGraphState) {
        LongIterator longiterator = pGraphState.events.chunksWhichReceivedNeighbors.iterator();

        while (longiterator.hasNext()) {
            long i = longiterator.nextLong();
            List<SectionRenderDispatcher.RenderSection> list = pGraphState.storage.chunksWaitingForNeighbors.get(i);
            if (list != null && list.get(0).hasAllNeighbors()) {
                pGraphState.events.sectionsToPropagateFrom.addAll(list);
                pGraphState.storage.chunksWaitingForNeighbors.remove(i);
            }
        }

        pGraphState.events.chunksWhichReceivedNeighbors.clear();
    }

    private void addNeighbors(SectionOcclusionGraph.GraphEvents pGraphEvents, ChunkPos pChunkPos) {
        pGraphEvents.chunksWhichReceivedNeighbors.add(ChunkPos.asLong(pChunkPos.x - 1, pChunkPos.z));
        pGraphEvents.chunksWhichReceivedNeighbors.add(ChunkPos.asLong(pChunkPos.x, pChunkPos.z - 1));
        pGraphEvents.chunksWhichReceivedNeighbors.add(ChunkPos.asLong(pChunkPos.x + 1, pChunkPos.z));
        pGraphEvents.chunksWhichReceivedNeighbors.add(ChunkPos.asLong(pChunkPos.x, pChunkPos.z + 1));
        pGraphEvents.chunksWhichReceivedNeighbors.add(ChunkPos.asLong(pChunkPos.x - 1, pChunkPos.z - 1));
        pGraphEvents.chunksWhichReceivedNeighbors.add(ChunkPos.asLong(pChunkPos.x - 1, pChunkPos.z + 1));
        pGraphEvents.chunksWhichReceivedNeighbors.add(ChunkPos.asLong(pChunkPos.x + 1, pChunkPos.z - 1));
        pGraphEvents.chunksWhichReceivedNeighbors.add(ChunkPos.asLong(pChunkPos.x + 1, pChunkPos.z + 1));
    }

    private void initializeQueueForFullUpdate(Camera pCamera, Queue<SectionOcclusionGraph.Node> pNodeQueue) {
        BlockPos blockpos = pCamera.getBlockPosition();
        long i = SectionPos.asLong(blockpos);
        int j = SectionPos.y(i);
        SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection = this.viewArea.getRenderSection(i);
        if (sectionrenderdispatcher$rendersection == null) {
            LevelHeightAccessor levelheightaccessor = this.viewArea.getLevelHeightAccessor();
            boolean flag = j < levelheightaccessor.getMinSectionY();
            int k = flag ? levelheightaccessor.getMinSectionY() : levelheightaccessor.getMaxSectionY();
            int l = this.viewArea.getViewDistance();
            List<SectionOcclusionGraph.Node> list = Lists.newArrayList();
            int i1 = SectionPos.x(i);
            int j1 = SectionPos.z(i);

            for (int k1 = -l; k1 <= l; k1++) {
                for (int l1 = -l; l1 <= l; l1++) {
                    SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection1 = this.viewArea
                        .getRenderSection(SectionPos.asLong(k1 + i1, k, l1 + j1));
                    if (sectionrenderdispatcher$rendersection1 != null && this.isInViewDistance(i, sectionrenderdispatcher$rendersection1.getSectionNode())) {
                        Direction direction = flag ? Direction.UP : Direction.DOWN;
                        SectionOcclusionGraph.Node sectionocclusiongraph$node = new SectionOcclusionGraph.Node(
                            sectionrenderdispatcher$rendersection1, direction, 0
                        );
                        sectionocclusiongraph$node.setDirections(sectionocclusiongraph$node.directions, direction);
                        if (k1 > 0) {
                            sectionocclusiongraph$node.setDirections(sectionocclusiongraph$node.directions, Direction.EAST);
                        } else if (k1 < 0) {
                            sectionocclusiongraph$node.setDirections(sectionocclusiongraph$node.directions, Direction.WEST);
                        }

                        if (l1 > 0) {
                            sectionocclusiongraph$node.setDirections(sectionocclusiongraph$node.directions, Direction.SOUTH);
                        } else if (l1 < 0) {
                            sectionocclusiongraph$node.setDirections(sectionocclusiongraph$node.directions, Direction.NORTH);
                        }

                        list.add(sectionocclusiongraph$node);
                    }
                }
            }

            list.sort(Comparator.comparingDouble(p_389460_ -> blockpos.distSqr(SectionPos.of(p_389460_.section.getSectionNode()).center())));
            pNodeQueue.addAll(list);
        } else {
            pNodeQueue.add(new SectionOcclusionGraph.Node(sectionrenderdispatcher$rendersection, null, 0));
        }
    }

    private void runUpdates(
        SectionOcclusionGraph.GraphStorage pStorage,
        Vec3 pCameraPosition,
        Queue<SectionOcclusionGraph.Node> pQueue,
        boolean pSmartCull,
        Consumer<SectionRenderDispatcher.RenderSection> pVisibleSectionConsumer,
        LongOpenHashSet pLoadedEmptySection
    ) {
        SectionPos sectionpos = SectionPos.of(pCameraPosition);
        long i = sectionpos.asLong();
        BlockPos blockpos = sectionpos.center();

        while (!pQueue.isEmpty()) {
            SectionOcclusionGraph.Node sectionocclusiongraph$node = pQueue.poll();
            SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection = sectionocclusiongraph$node.section;
            if (!pLoadedEmptySection.contains(sectionocclusiongraph$node.section.getSectionNode())) {
                if (pStorage.sectionTree.add(sectionocclusiongraph$node.section)) {
                    pVisibleSectionConsumer.accept(sectionocclusiongraph$node.section);
                }
            } else {
                sectionocclusiongraph$node.section
                    .compiled
                    .compareAndSet(SectionRenderDispatcher.CompiledSection.UNCOMPILED, SectionRenderDispatcher.CompiledSection.EMPTY);
            }

            long j = sectionrenderdispatcher$rendersection.getSectionNode();
            boolean flag = Math.abs(SectionPos.x(j) - sectionpos.x()) > MINIMUM_ADVANCED_CULLING_SECTION_DISTANCE
                || Math.abs(SectionPos.y(j) - sectionpos.y()) > MINIMUM_ADVANCED_CULLING_SECTION_DISTANCE
                || Math.abs(SectionPos.z(j) - sectionpos.z()) > MINIMUM_ADVANCED_CULLING_SECTION_DISTANCE;

            for (Direction direction : DIRECTIONS) {
                SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection1 = this.getRelativeFrom(
                    i, sectionrenderdispatcher$rendersection, direction
                );
                if (sectionrenderdispatcher$rendersection1 != null && (!pSmartCull || !sectionocclusiongraph$node.hasDirection(direction.getOpposite()))) {
                    if (pSmartCull && sectionocclusiongraph$node.hasSourceDirections()) {
                        SectionRenderDispatcher.CompiledSection sectionrenderdispatcher$compiledsection = sectionrenderdispatcher$rendersection.getCompiled();
                        boolean flag1 = false;

                        for (int k = 0; k < DIRECTIONS.length; k++) {
                            if (sectionocclusiongraph$node.hasSourceDirection(k)
                                && sectionrenderdispatcher$compiledsection.facesCanSeeEachother(DIRECTIONS[k].getOpposite(), direction)) {
                                flag1 = true;
                                break;
                            }
                        }

                        if (!flag1) {
                            continue;
                        }
                    }

                    if (pSmartCull && flag) {
                        int l = SectionPos.sectionToBlockCoord(SectionPos.x(j));
                        int i1 = SectionPos.sectionToBlockCoord(SectionPos.y(j));
                        int j1 = SectionPos.sectionToBlockCoord(SectionPos.z(j));
                        boolean flag2 = direction.getAxis() == Direction.Axis.X ? blockpos.getX() > l : blockpos.getX() < l;
                        boolean flag3 = direction.getAxis() == Direction.Axis.Y ? blockpos.getY() > i1 : blockpos.getY() < i1;
                        boolean flag4 = direction.getAxis() == Direction.Axis.Z ? blockpos.getZ() > j1 : blockpos.getZ() < j1;
                        Vector3d vector3d = new Vector3d(l + (flag2 ? 16 : 0), i1 + (flag3 ? 16 : 0), j1 + (flag4 ? 16 : 0));
                        Vector3d vector3d1 = new Vector3d(pCameraPosition.x, pCameraPosition.y, pCameraPosition.z).sub(vector3d).normalize().mul(CEILED_SECTION_DIAGONAL);
                        boolean flag5 = true;

                        while (vector3d.distanceSquared(pCameraPosition.x, pCameraPosition.y, pCameraPosition.z) > 3600.0) {
                            vector3d.add(vector3d1);
                            LevelHeightAccessor levelheightaccessor = this.viewArea.getLevelHeightAccessor();
                            if (vector3d.y > levelheightaccessor.getMaxY() || vector3d.y < levelheightaccessor.getMinY()) {
                                break;
                            }

                            SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection2 = this.viewArea
                                .getRenderSectionAt(BlockPos.containing(vector3d.x, vector3d.y, vector3d.z));
                            if (sectionrenderdispatcher$rendersection2 == null || pStorage.sectionToNodeMap.get(sectionrenderdispatcher$rendersection2) == null
                                )
                             {
                                flag5 = false;
                                break;
                            }
                        }

                        if (!flag5) {
                            continue;
                        }
                    }

                    SectionOcclusionGraph.Node sectionocclusiongraph$node1 = pStorage.sectionToNodeMap.get(sectionrenderdispatcher$rendersection1);
                    if (sectionocclusiongraph$node1 != null) {
                        sectionocclusiongraph$node1.addSourceDirection(direction);
                    } else {
                        SectionOcclusionGraph.Node sectionocclusiongraph$node2 = new SectionOcclusionGraph.Node(
                            sectionrenderdispatcher$rendersection1, direction, sectionocclusiongraph$node.step + 1
                        );
                        sectionocclusiongraph$node2.setDirections(sectionocclusiongraph$node.directions, direction);
                        if (sectionrenderdispatcher$rendersection1.hasAllNeighbors()) {
                            pQueue.add(sectionocclusiongraph$node2);
                            pStorage.sectionToNodeMap.put(sectionrenderdispatcher$rendersection1, sectionocclusiongraph$node2);
                        } else if (this.isInViewDistance(i, sectionrenderdispatcher$rendersection1.getSectionNode())) {
                            pStorage.sectionToNodeMap.put(sectionrenderdispatcher$rendersection1, sectionocclusiongraph$node2);
                            long k1 = SectionPos.sectionToChunk(sectionrenderdispatcher$rendersection1.getSectionNode());
                            pStorage.chunksWaitingForNeighbors.computeIfAbsent(k1, p_298371_ -> new ArrayList<>()).add(sectionrenderdispatcher$rendersection1);
                        }
                    }
                }
            }
        }
    }

    private boolean isInViewDistance(long pCenterPos, long pPos) {
        return ChunkTrackingView.isInViewDistance(
            SectionPos.x(pCenterPos),
            SectionPos.z(pCenterPos),
            this.viewArea.getViewDistance(),
            SectionPos.x(pPos),
            SectionPos.z(pPos)
        );
    }

    @Nullable
    private SectionRenderDispatcher.RenderSection getRelativeFrom(long pSectionPos, SectionRenderDispatcher.RenderSection pSection, Direction pDirection) {
        long i = pSection.getNeighborSectionNode(pDirection);
        if (!this.isInViewDistance(pSectionPos, i)) {
            return null;
        } else {
            return Mth.abs(SectionPos.y(pSectionPos) - SectionPos.y(i)) > this.viewArea.getViewDistance() ? null : this.viewArea.getRenderSection(i);
        }
    }

    @Nullable
    @VisibleForDebug
    public SectionOcclusionGraph.Node getNode(SectionRenderDispatcher.RenderSection pSection) {
        return this.currentGraph.get().storage.sectionToNodeMap.get(pSection);
    }

    public Octree getOctree() {
        return this.currentGraph.get().storage.sectionTree;
    }

    @OnlyIn(Dist.CLIENT)
    record GraphEvents(LongSet chunksWhichReceivedNeighbors, BlockingQueue<SectionRenderDispatcher.RenderSection> sectionsToPropagateFrom) {
        GraphEvents() {
            this(new LongOpenHashSet(), new LinkedBlockingQueue<>());
        }
    }

    @OnlyIn(Dist.CLIENT)
    record GraphState(SectionOcclusionGraph.GraphStorage storage, SectionOcclusionGraph.GraphEvents events) {
        GraphState(ViewArea pViewArea) {
            this(new SectionOcclusionGraph.GraphStorage(pViewArea), new SectionOcclusionGraph.GraphEvents());
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class GraphStorage {
        public final SectionOcclusionGraph.SectionToNodeMap sectionToNodeMap;
        public final Octree sectionTree;
        public final Long2ObjectMap<List<SectionRenderDispatcher.RenderSection>> chunksWaitingForNeighbors;

        public GraphStorage(ViewArea pViewArea) {
            this.sectionToNodeMap = new SectionOcclusionGraph.SectionToNodeMap(pViewArea.sections.length);
            this.sectionTree = new Octree(pViewArea.getCameraSectionPos(), pViewArea.getViewDistance(), pViewArea.sectionGridSizeY, pViewArea.level.getMinY());
            this.chunksWaitingForNeighbors = new Long2ObjectOpenHashMap<>();
        }
    }

    @OnlyIn(Dist.CLIENT)
    @VisibleForDebug
    public static class Node {
        @VisibleForDebug
        protected final SectionRenderDispatcher.RenderSection section;
        private byte sourceDirections;
        byte directions;
        @VisibleForDebug
        public final int step;

        Node(SectionRenderDispatcher.RenderSection pSection, @Nullable Direction pSourceDirection, int pStep) {
            this.section = pSection;
            if (pSourceDirection != null) {
                this.addSourceDirection(pSourceDirection);
            }

            this.step = pStep;
        }

        void setDirections(byte pCurrentValue, Direction pDirection) {
            this.directions = (byte)(this.directions | pCurrentValue | 1 << pDirection.ordinal());
        }

        boolean hasDirection(Direction pDirection) {
            return (this.directions & 1 << pDirection.ordinal()) > 0;
        }

        void addSourceDirection(Direction pSourceDirection) {
            this.sourceDirections = (byte)(this.sourceDirections | this.sourceDirections | 1 << pSourceDirection.ordinal());
        }

        @VisibleForDebug
        public boolean hasSourceDirection(int pDirection) {
            return (this.sourceDirections & 1 << pDirection) > 0;
        }

        boolean hasSourceDirections() {
            return this.sourceDirections != 0;
        }

        @Override
        public int hashCode() {
            return Long.hashCode(this.section.getSectionNode());
        }

        @Override
        public boolean equals(Object pOther) {
            return !(pOther instanceof SectionOcclusionGraph.Node sectionocclusiongraph$node)
                ? false
                : this.section.getSectionNode() == sectionocclusiongraph$node.section.getSectionNode();
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class SectionToNodeMap {
        private final SectionOcclusionGraph.Node[] nodes;

        SectionToNodeMap(int pSize) {
            this.nodes = new SectionOcclusionGraph.Node[pSize];
        }

        public void put(SectionRenderDispatcher.RenderSection pSection, SectionOcclusionGraph.Node pNode) {
            this.nodes[pSection.index] = pNode;
        }

        @Nullable
        public SectionOcclusionGraph.Node get(SectionRenderDispatcher.RenderSection pSection) {
            int i = pSection.index;
            return i >= 0 && i < this.nodes.length ? this.nodes[i] : null;
        }
    }
}