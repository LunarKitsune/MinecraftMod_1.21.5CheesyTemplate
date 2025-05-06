package net.minecraft.client.renderer.chunk;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.buffers.BufferType;
import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.TracingExecutor;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.SectionBufferBuilderPool;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.Zone;
import net.minecraft.util.thread.ConsecutiveExecutor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SectionRenderDispatcher {
    private final CompileTaskDynamicQueue compileQueue = new CompileTaskDynamicQueue();
    final Queue<Runnable> toUpload = Queues.newConcurrentLinkedQueue();
    final SectionBufferBuilderPack fixedBuffers;
    private final SectionBufferBuilderPool bufferPool;
    private volatile int toBatchCount;
    volatile boolean closed;
    private final ConsecutiveExecutor consecutiveExecutor;
    private final TracingExecutor executor;
    ClientLevel level;
    final LevelRenderer renderer;
    private Vec3 camera = Vec3.ZERO;
    final SectionCompiler sectionCompiler;

    public SectionRenderDispatcher(
        ClientLevel pLevel,
        LevelRenderer pRenderer,
        TracingExecutor pExecutor,
        RenderBuffers pBuffer,
        BlockRenderDispatcher pBlockRenderer,
        BlockEntityRenderDispatcher pBlockEntityRenderer
    ) {
        this.level = pLevel;
        this.renderer = pRenderer;
        this.fixedBuffers = pBuffer.fixedBufferPack();
        this.bufferPool = pBuffer.sectionBufferPool();
        this.executor = pExecutor;
        this.consecutiveExecutor = new ConsecutiveExecutor(pExecutor, "Section Renderer");
        this.consecutiveExecutor.schedule(this::runTask);
        this.sectionCompiler = new SectionCompiler(pBlockRenderer, pBlockEntityRenderer);
    }

    public void setLevel(ClientLevel pLevel) {
        this.level = pLevel;
    }

    private void runTask() {
        if (!this.closed && !this.bufferPool.isEmpty()) {
            SectionRenderDispatcher.RenderSection.CompileTask sectionrenderdispatcher$rendersection$compiletask = this.compileQueue.poll(this.getCameraPosition());
            if (sectionrenderdispatcher$rendersection$compiletask != null) {
                SectionBufferBuilderPack sectionbufferbuilderpack = Objects.requireNonNull(this.bufferPool.acquire());
                this.toBatchCount = this.compileQueue.size();
                CompletableFuture.<CompletableFuture<SectionRenderDispatcher.SectionTaskResult>>supplyAsync(
                        () -> sectionrenderdispatcher$rendersection$compiletask.doTask(sectionbufferbuilderpack),
                        this.executor.forName(sectionrenderdispatcher$rendersection$compiletask.name())
                    )
                    .thenCompose(p_298155_ -> (CompletionStage<SectionRenderDispatcher.SectionTaskResult>)p_298155_)
                    .whenComplete((p_357938_, p_357939_) -> {
                        if (p_357939_ != null) {
                            Minecraft.getInstance().delayCrash(CrashReport.forThrowable(p_357939_, "Batching sections"));
                        } else {
                            sectionrenderdispatcher$rendersection$compiletask.isCompleted.set(true);
                            this.consecutiveExecutor.schedule(() -> {
                                if (p_357938_ == SectionRenderDispatcher.SectionTaskResult.SUCCESSFUL) {
                                    sectionbufferbuilderpack.clearAll();
                                } else {
                                    sectionbufferbuilderpack.discardAll();
                                }

                                this.bufferPool.release(sectionbufferbuilderpack);
                                this.runTask();
                            });
                        }
                    });
            }
        }
    }

    public String getStats() {
        return String.format(Locale.ROOT, "pC: %03d, pU: %02d, aB: %02d", this.toBatchCount, this.toUpload.size(), this.bufferPool.getFreeBufferCount());
    }

    public int getToBatchCount() {
        return this.toBatchCount;
    }

    public int getToUpload() {
        return this.toUpload.size();
    }

    public int getFreeBufferCount() {
        return this.bufferPool.getFreeBufferCount();
    }

    public void setCamera(Vec3 pCamera) {
        this.camera = pCamera;
    }

    public Vec3 getCameraPosition() {
        return this.camera;
    }

    public void uploadAllPendingUploads() {
        Runnable runnable;
        while ((runnable = this.toUpload.poll()) != null) {
            runnable.run();
        }
    }

    public void rebuildSectionSync(SectionRenderDispatcher.RenderSection pSection, RenderRegionCache pRegionCache) {
        pSection.compileSync(pRegionCache);
    }

    public void blockUntilClear() {
        this.clearBatchQueue();
    }

    public void schedule(SectionRenderDispatcher.RenderSection.CompileTask pTask) {
        if (!this.closed) {
            this.consecutiveExecutor.schedule(() -> {
                if (!this.closed) {
                    this.compileQueue.add(pTask);
                    this.toBatchCount = this.compileQueue.size();
                    this.runTask();
                }
            });
        }
    }

    private void clearBatchQueue() {
        this.compileQueue.clear();
        this.toBatchCount = 0;
    }

    public boolean isQueueEmpty() {
        return this.toBatchCount == 0 && this.toUpload.isEmpty();
    }

    public void dispose() {
        this.closed = true;
        this.clearBatchQueue();
        this.uploadAllPendingUploads();
    }

    @OnlyIn(Dist.CLIENT)
    public static class CompiledSection {
        public static final SectionRenderDispatcher.CompiledSection UNCOMPILED = new SectionRenderDispatcher.CompiledSection() {
            @Override
            public boolean facesCanSeeEachother(Direction p_301280_, Direction p_299155_) {
                return false;
            }
        };
        public static final SectionRenderDispatcher.CompiledSection EMPTY = new SectionRenderDispatcher.CompiledSection() {
            @Override
            public boolean facesCanSeeEachother(Direction p_343413_, Direction p_342431_) {
                return true;
            }
        };
        final Set<RenderType> hasBlocks = new ObjectArraySet<>(RenderType.chunkBufferLayers().size());
        final List<BlockEntity> renderableBlockEntities = Lists.newArrayList();
        VisibilitySet visibilitySet = new VisibilitySet();
        @Nullable
        MeshData.SortState transparencyState;

        public boolean hasRenderableLayers() {
            return !this.hasBlocks.isEmpty();
        }

        public boolean isEmpty(RenderType pRenderType) {
            return !this.hasBlocks.contains(pRenderType);
        }

        public List<BlockEntity> getRenderableBlockEntities() {
            return this.renderableBlockEntities;
        }

        public boolean facesCanSeeEachother(Direction pFace1, Direction pFace2) {
            return this.visibilitySet.visibilityBetween(pFace1, pFace2);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public class RenderSection {
        public static final int SIZE = 16;
        public final int index;
        public final AtomicReference<SectionRenderDispatcher.CompiledSection> compiled = new AtomicReference<>(
            SectionRenderDispatcher.CompiledSection.UNCOMPILED
        );
        public final AtomicReference<SectionRenderDispatcher.TranslucencyPointOfView> pointOfView = new AtomicReference<>(null);
        @Nullable
        private SectionRenderDispatcher.RenderSection.RebuildTask lastRebuildTask;
        @Nullable
        private SectionRenderDispatcher.RenderSection.ResortTransparencyTask lastResortTransparencyTask;
        private final Set<BlockEntity> globalBlockEntities = Sets.newHashSet();
        private final Map<RenderType, SectionRenderDispatcher.SectionBuffers> buffers = new HashMap<>();
        private AABB bb;
        private boolean dirty = true;
        volatile long sectionNode = SectionPos.asLong(-1, -1, -1);
        final BlockPos.MutableBlockPos renderOrigin = new BlockPos.MutableBlockPos(-1, -1, -1);
        private boolean playerChanged;

        public RenderSection(final int pIndex, final long pSectionNode) {
            this.index = pIndex;
            this.setSectionNode(pSectionNode);
        }

        private boolean doesChunkExistAt(long pPos) {
            ChunkAccess chunkaccess = SectionRenderDispatcher.this.level
                .getChunk(SectionPos.x(pPos), SectionPos.z(pPos), ChunkStatus.FULL, false);
            return chunkaccess != null && SectionRenderDispatcher.this.level.getLightEngine().lightOnInColumn(SectionPos.getZeroNode(pPos));
        }

        public boolean hasAllNeighbors() {
            int i = 24;
            return !(this.getDistToPlayerSqr() > 576.0)
                ? true
                : this.doesChunkExistAt(SectionPos.offset(this.sectionNode, Direction.WEST))
                    && this.doesChunkExistAt(SectionPos.offset(this.sectionNode, Direction.NORTH))
                    && this.doesChunkExistAt(SectionPos.offset(this.sectionNode, Direction.EAST))
                    && this.doesChunkExistAt(SectionPos.offset(this.sectionNode, Direction.SOUTH))
                    && this.doesChunkExistAt(SectionPos.offset(this.sectionNode, -1, 0, -1))
                    && this.doesChunkExistAt(SectionPos.offset(this.sectionNode, -1, 0, 1))
                    && this.doesChunkExistAt(SectionPos.offset(this.sectionNode, 1, 0, -1))
                    && this.doesChunkExistAt(SectionPos.offset(this.sectionNode, 1, 0, 1));
        }

        public AABB getBoundingBox() {
            return this.bb;
        }

        @Nullable
        public SectionRenderDispatcher.SectionBuffers getBuffers(RenderType pRenderType) {
            return this.buffers.get(pRenderType);
        }

        public CompletableFuture<Void> uploadSectionLayer(RenderType pRenderType, MeshData pMeshData) {
            if (SectionRenderDispatcher.this.closed) {
                pMeshData.close();
                return CompletableFuture.completedFuture(null);
            } else {
                return CompletableFuture.runAsync(
                    () -> {
                        try (Zone zone = Profiler.get().zone("Upload Section Layer")) {
                            CommandEncoder commandencoder = RenderSystem.getDevice().createCommandEncoder();
                            if (this.buffers.containsKey(pRenderType)) {
                                SectionRenderDispatcher.SectionBuffers sectionrenderdispatcher$sectionbuffers1 = this.buffers.get(pRenderType);
                                if (sectionrenderdispatcher$sectionbuffers1.vertexBuffer.size() < pMeshData.vertexBuffer().remaining()) {
                                    sectionrenderdispatcher$sectionbuffers1.vertexBuffer.close();
                                    sectionrenderdispatcher$sectionbuffers1.setVertexBuffer(
                                        RenderSystem.getDevice()
                                            .createBuffer(
                                                () -> "Section vertex buffer - layer: "
                                                    + pRenderType.getName()
                                                    + "; cords: "
                                                    + SectionPos.x(this.sectionNode)
                                                    + ", "
                                                    + SectionPos.y(this.sectionNode)
                                                    + ", "
                                                    + SectionPos.z(this.sectionNode),
                                                BufferType.VERTICES,
                                                BufferUsage.STATIC_WRITE,
                                                pMeshData.vertexBuffer()
                                            )
                                    );
                                } else if (!sectionrenderdispatcher$sectionbuffers1.vertexBuffer.isClosed()) {
                                    commandencoder.writeToBuffer(sectionrenderdispatcher$sectionbuffers1.vertexBuffer, pMeshData.vertexBuffer(), 0);
                                }

                                if (pMeshData.indexBuffer() != null) {
                                    if (sectionrenderdispatcher$sectionbuffers1.indexBuffer != null
                                        && sectionrenderdispatcher$sectionbuffers1.indexBuffer.size() >= pMeshData.indexBuffer().remaining()) {
                                        if (!sectionrenderdispatcher$sectionbuffers1.indexBuffer.isClosed()) {
                                            commandencoder.writeToBuffer(sectionrenderdispatcher$sectionbuffers1.indexBuffer, pMeshData.indexBuffer(), 0);
                                        }
                                    } else {
                                        if (sectionrenderdispatcher$sectionbuffers1.indexBuffer != null) {
                                            sectionrenderdispatcher$sectionbuffers1.indexBuffer.close();
                                        }

                                        sectionrenderdispatcher$sectionbuffers1.setIndexBuffer(
                                            RenderSystem.getDevice()
                                                .createBuffer(
                                                    () -> "Section index buffer - layer: "
                                                        + pRenderType.getName()
                                                        + "; cords: "
                                                        + SectionPos.x(this.sectionNode)
                                                        + ", "
                                                        + SectionPos.y(this.sectionNode)
                                                        + ", "
                                                        + SectionPos.z(this.sectionNode),
                                                    BufferType.INDICES,
                                                    BufferUsage.STATIC_WRITE,
                                                    pMeshData.indexBuffer()
                                                )
                                        );
                                    }
                                } else if (sectionrenderdispatcher$sectionbuffers1.indexBuffer != null) {
                                    sectionrenderdispatcher$sectionbuffers1.indexBuffer.close();
                                    sectionrenderdispatcher$sectionbuffers1.setIndexBuffer(null);
                                }

                                sectionrenderdispatcher$sectionbuffers1.setIndexCount(pMeshData.drawState().indexCount());
                                sectionrenderdispatcher$sectionbuffers1.setIndexType(pMeshData.drawState().indexType());
                            } else {
                                GpuBuffer gpubuffer = RenderSystem.getDevice()
                                    .createBuffer(
                                        () -> "Section vertex buffer - layer: "
                                            + pRenderType.getName()
                                            + "; cords: "
                                            + SectionPos.x(this.sectionNode)
                                            + ", "
                                            + SectionPos.y(this.sectionNode)
                                            + ", "
                                            + SectionPos.z(this.sectionNode),
                                        BufferType.VERTICES,
                                        BufferUsage.STATIC_WRITE,
                                        pMeshData.vertexBuffer()
                                    );
                                GpuBuffer gpubuffer1 = pMeshData.indexBuffer() != null
                                    ? RenderSystem.getDevice()
                                        .createBuffer(
                                            () -> "Section index buffer - layer: "
                                                + pRenderType.getName()
                                                + "; cords: "
                                                + SectionPos.x(this.sectionNode)
                                                + ", "
                                                + SectionPos.y(this.sectionNode)
                                                + ", "
                                                + SectionPos.z(this.sectionNode),
                                            BufferType.INDICES,
                                            BufferUsage.STATIC_WRITE,
                                            pMeshData.indexBuffer()
                                        )
                                    : null;
                                SectionRenderDispatcher.SectionBuffers sectionrenderdispatcher$sectionbuffers = new SectionRenderDispatcher.SectionBuffers(
                                    gpubuffer, gpubuffer1, pMeshData.drawState().indexCount(), pMeshData.drawState().indexType()
                                );
                                this.buffers.put(pRenderType, sectionrenderdispatcher$sectionbuffers);
                            }

                            pMeshData.close();
                        }
                    },
                    SectionRenderDispatcher.this.toUpload::add
                );
            }
        }

        public CompletableFuture<Void> uploadSectionIndexBuffer(ByteBufferBuilder.Result pResult, RenderType pRenderType) {
            if (SectionRenderDispatcher.this.closed) {
                pResult.close();
                return CompletableFuture.completedFuture(null);
            } else {
                return CompletableFuture.runAsync(
                    () -> {
                        try (Zone zone = Profiler.get().zone("Upload Section Indices")) {
                            SectionRenderDispatcher.SectionBuffers sectionrenderdispatcher$sectionbuffers = this.getBuffers(pRenderType);
                            if (sectionrenderdispatcher$sectionbuffers != null && !SectionRenderDispatcher.this.closed) {
                                if (sectionrenderdispatcher$sectionbuffers.indexBuffer == null) {
                                    sectionrenderdispatcher$sectionbuffers.setIndexBuffer(
                                        RenderSystem.getDevice()
                                            .createBuffer(
                                                () -> "Section index buffer - layer: "
                                                    + pRenderType.getName()
                                                    + "; cords: "
                                                    + SectionPos.x(this.sectionNode)
                                                    + ", "
                                                    + SectionPos.y(this.sectionNode)
                                                    + ", "
                                                    + SectionPos.z(this.sectionNode),
                                                BufferType.INDICES,
                                                BufferUsage.STATIC_WRITE,
                                                pResult.byteBuffer()
                                            )
                                    );
                                } else {
                                    CommandEncoder commandencoder = RenderSystem.getDevice().createCommandEncoder();
                                    if (!sectionrenderdispatcher$sectionbuffers.indexBuffer.isClosed()) {
                                        commandencoder.writeToBuffer(sectionrenderdispatcher$sectionbuffers.indexBuffer, pResult.byteBuffer(), 0);
                                    }
                                }

                                pResult.close();
                                return;
                            }

                            pResult.close();
                        }
                    },
                    SectionRenderDispatcher.this.toUpload::add
                );
            }
        }

        public void setSectionNode(long pSectionNode) {
            this.reset();
            this.sectionNode = pSectionNode;
            int i = SectionPos.sectionToBlockCoord(SectionPos.x(pSectionNode));
            int j = SectionPos.sectionToBlockCoord(SectionPos.y(pSectionNode));
            int k = SectionPos.sectionToBlockCoord(SectionPos.z(pSectionNode));
            this.renderOrigin.set(i, j, k);
            this.bb = new AABB(i, j, k, i + 16, j + 16, k + 16);
        }

        protected double getDistToPlayerSqr() {
            Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
            double d0 = this.bb.minX + 8.0 - camera.getPosition().x;
            double d1 = this.bb.minY + 8.0 - camera.getPosition().y;
            double d2 = this.bb.minZ + 8.0 - camera.getPosition().z;
            return d0 * d0 + d1 * d1 + d2 * d2;
        }

        public SectionRenderDispatcher.CompiledSection getCompiled() {
            return this.compiled.get();
        }

        public void reset() {
            this.cancelTasks();
            this.compiled.set(SectionRenderDispatcher.CompiledSection.UNCOMPILED);
            this.pointOfView.set(null);
            this.dirty = true;
            this.buffers.values().forEach(SectionRenderDispatcher.SectionBuffers::close);
            this.buffers.clear();
        }

        public BlockPos getRenderOrigin() {
            return this.renderOrigin;
        }

        public long getSectionNode() {
            return this.sectionNode;
        }

        public void setDirty(boolean pPlayerChanged) {
            boolean flag = this.dirty;
            this.dirty = true;
            this.playerChanged = pPlayerChanged | (flag && this.playerChanged);
        }

        public void setNotDirty() {
            this.dirty = false;
            this.playerChanged = false;
        }

        public boolean isDirty() {
            return this.dirty;
        }

        public boolean isDirtyFromPlayer() {
            return this.dirty && this.playerChanged;
        }

        public long getNeighborSectionNode(Direction pDirection) {
            return SectionPos.offset(this.sectionNode, pDirection);
        }

        public void resortTransparency(SectionRenderDispatcher pDispatcher) {
            this.lastResortTransparencyTask = new SectionRenderDispatcher.RenderSection.ResortTransparencyTask(this.getCompiled());
            pDispatcher.schedule(this.lastResortTransparencyTask);
        }

        public boolean hasTranslucentGeometry() {
            return this.getCompiled().hasBlocks.contains(RenderType.translucent());
        }

        public boolean transparencyResortingScheduled() {
            return this.lastResortTransparencyTask != null && !this.lastResortTransparencyTask.isCompleted.get();
        }

        protected void cancelTasks() {
            if (this.lastRebuildTask != null) {
                this.lastRebuildTask.cancel();
                this.lastRebuildTask = null;
            }

            if (this.lastResortTransparencyTask != null) {
                this.lastResortTransparencyTask.cancel();
                this.lastResortTransparencyTask = null;
            }
        }

        public SectionRenderDispatcher.RenderSection.CompileTask createCompileTask(RenderRegionCache pRegionCache) {
            this.cancelTasks();
            RenderChunkRegion renderchunkregion = pRegionCache.createRegion(SectionRenderDispatcher.this.level, SectionPos.of(this.sectionNode));
            boolean flag = this.compiled.get() != SectionRenderDispatcher.CompiledSection.UNCOMPILED;
            this.lastRebuildTask = new SectionRenderDispatcher.RenderSection.RebuildTask(renderchunkregion, flag);
            return this.lastRebuildTask;
        }

        public void rebuildSectionAsync(SectionRenderDispatcher pSectionRenderDispatcher, RenderRegionCache pRegionCache) {
            SectionRenderDispatcher.RenderSection.CompileTask sectionrenderdispatcher$rendersection$compiletask = this.createCompileTask(pRegionCache);
            pSectionRenderDispatcher.schedule(sectionrenderdispatcher$rendersection$compiletask);
        }

        void updateGlobalBlockEntities(Collection<BlockEntity> pBlockEntities) {
            Set<BlockEntity> set = Sets.newHashSet(pBlockEntities);
            Set<BlockEntity> set1;
            synchronized (this.globalBlockEntities) {
                set1 = Sets.newHashSet(this.globalBlockEntities);
                set.removeAll(this.globalBlockEntities);
                set1.removeAll(pBlockEntities);
                this.globalBlockEntities.clear();
                this.globalBlockEntities.addAll(pBlockEntities);
            }

            SectionRenderDispatcher.this.renderer.updateGlobalBlockEntities(set1, set);
        }

        public void compileSync(RenderRegionCache pRegionCache) {
            SectionRenderDispatcher.RenderSection.CompileTask sectionrenderdispatcher$rendersection$compiletask = this.createCompileTask(pRegionCache);
            sectionrenderdispatcher$rendersection$compiletask.doTask(SectionRenderDispatcher.this.fixedBuffers);
        }

        void setCompiled(SectionRenderDispatcher.CompiledSection pCompiled) {
            this.compiled.set(pCompiled);
            SectionRenderDispatcher.this.renderer.addRecentlyCompiledSection(this);
        }

        VertexSorting createVertexSorting(SectionPos pSectionPos) {
            Vec3 vec3 = SectionRenderDispatcher.this.getCameraPosition();
            return VertexSorting.byDistance(
                (float)(vec3.x - pSectionPos.minBlockX()), (float)(vec3.y - pSectionPos.minBlockY()), (float)(vec3.z - pSectionPos.minBlockZ())
            );
        }

        @OnlyIn(Dist.CLIENT)
        public abstract class CompileTask {
            protected final AtomicBoolean isCancelled = new AtomicBoolean(false);
            protected final AtomicBoolean isCompleted = new AtomicBoolean(false);
            protected final boolean isRecompile;

            public CompileTask(final boolean pIsRecompile) {
                this.isRecompile = pIsRecompile;
            }

            public abstract CompletableFuture<SectionRenderDispatcher.SectionTaskResult> doTask(SectionBufferBuilderPack pSectionBufferBuilderPack);

            public abstract void cancel();

            protected abstract String name();

            public boolean isRecompile() {
                return this.isRecompile;
            }

            public BlockPos getRenderOrigin() {
                return RenderSection.this.renderOrigin;
            }
        }

        @OnlyIn(Dist.CLIENT)
        class RebuildTask extends SectionRenderDispatcher.RenderSection.CompileTask {
            @Nullable
            protected volatile RenderChunkRegion region;

            public RebuildTask(@Nullable final RenderChunkRegion pRegion, final boolean pIsRecompile) {
                super(pIsRecompile);
                this.region = pRegion;
            }

            @Override
            protected String name() {
                return "rend_chk_rebuild";
            }

            @Override
            public CompletableFuture<SectionRenderDispatcher.SectionTaskResult> doTask(SectionBufferBuilderPack p_299595_) {
                if (this.isCancelled.get()) {
                    return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                } else {
                    RenderChunkRegion renderchunkregion = this.region;
                    this.region = null;
                    if (renderchunkregion == null) {
                        RenderSection.this.setCompiled(SectionRenderDispatcher.CompiledSection.EMPTY);
                        return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.SUCCESSFUL);
                    } else {
                        long i = RenderSection.this.sectionNode;
                        SectionPos sectionpos = SectionPos.of(i);
                        if (this.isCancelled.get()) {
                            return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                        } else {
                            SectionCompiler.Results sectioncompiler$results;
                            try (Zone zone = Profiler.get().zone("Compile Section")) {
                                sectioncompiler$results = SectionRenderDispatcher.this.sectionCompiler
                                    .compile(sectionpos, renderchunkregion, RenderSection.this.createVertexSorting(sectionpos), p_299595_);
                            }

                            SectionRenderDispatcher.TranslucencyPointOfView sectionrenderdispatcher$translucencypointofview = SectionRenderDispatcher.TranslucencyPointOfView.of(
                                SectionRenderDispatcher.this.getCameraPosition(), i
                            );
                            RenderSection.this.updateGlobalBlockEntities(sectioncompiler$results.globalBlockEntities);
                            if (this.isCancelled.get()) {
                                sectioncompiler$results.release();
                                return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                            } else {
                                SectionRenderDispatcher.CompiledSection sectionrenderdispatcher$compiledsection = new SectionRenderDispatcher.CompiledSection();
                                sectionrenderdispatcher$compiledsection.visibilitySet = sectioncompiler$results.visibilitySet;
                                sectionrenderdispatcher$compiledsection.renderableBlockEntities.addAll(sectioncompiler$results.blockEntities);
                                sectionrenderdispatcher$compiledsection.transparencyState = sectioncompiler$results.transparencyState;
                                List<CompletableFuture<Void>> list = new ArrayList<>(sectioncompiler$results.renderedLayers.size());
                                sectioncompiler$results.renderedLayers.forEach((p_389502_, p_389503_) -> {
                                    list.add(RenderSection.this.uploadSectionLayer(p_389502_, p_389503_));
                                    sectionrenderdispatcher$compiledsection.hasBlocks.add(p_389502_);
                                });
                                return Util.sequenceFailFast(list).handle((p_357946_, p_357947_) -> {
                                    if (p_357947_ != null && !(p_357947_ instanceof CancellationException) && !(p_357947_ instanceof InterruptedException)) {
                                        Minecraft.getInstance().delayCrash(CrashReport.forThrowable(p_357947_, "Rendering section"));
                                    }

                                    if (this.isCancelled.get()) {
                                        return SectionRenderDispatcher.SectionTaskResult.CANCELLED;
                                    } else {
                                        RenderSection.this.setCompiled(sectionrenderdispatcher$compiledsection);
                                        RenderSection.this.pointOfView.set(sectionrenderdispatcher$translucencypointofview);
                                        return SectionRenderDispatcher.SectionTaskResult.SUCCESSFUL;
                                    }
                                });
                            }
                        }
                    }
                }
            }

            @Override
            public void cancel() {
                this.region = null;
                if (this.isCancelled.compareAndSet(false, true)) {
                    RenderSection.this.setDirty(false);
                }
            }
        }

        @OnlyIn(Dist.CLIENT)
        class ResortTransparencyTask extends SectionRenderDispatcher.RenderSection.CompileTask {
            private final SectionRenderDispatcher.CompiledSection compiledSection;

            public ResortTransparencyTask(final SectionRenderDispatcher.CompiledSection pCompiledSection) {
                super(true);
                this.compiledSection = pCompiledSection;
            }

            @Override
            protected String name() {
                return "rend_chk_sort";
            }

            @Override
            public CompletableFuture<SectionRenderDispatcher.SectionTaskResult> doTask(SectionBufferBuilderPack p_297366_) {
                if (this.isCancelled.get()) {
                    return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                } else {
                    MeshData.SortState meshdata$sortstate = this.compiledSection.transparencyState;
                    if (meshdata$sortstate != null && !this.compiledSection.isEmpty(RenderType.translucent())) {
                        long i = RenderSection.this.sectionNode;
                        VertexSorting vertexsorting = RenderSection.this.createVertexSorting(SectionPos.of(i));
                        SectionRenderDispatcher.TranslucencyPointOfView sectionrenderdispatcher$translucencypointofview = SectionRenderDispatcher.TranslucencyPointOfView.of(
                            SectionRenderDispatcher.this.getCameraPosition(), i
                        );
                        if (sectionrenderdispatcher$translucencypointofview.equals(RenderSection.this.pointOfView.get())
                            && !sectionrenderdispatcher$translucencypointofview.isAxisAligned()) {
                            return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                        } else {
                            ByteBufferBuilder.Result bytebufferbuilder$result = meshdata$sortstate.buildSortedIndexBuffer(
                                p_297366_.buffer(RenderType.translucent()), vertexsorting
                            );
                            if (bytebufferbuilder$result == null) {
                                return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                            } else if (this.isCancelled.get()) {
                                bytebufferbuilder$result.close();
                                return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                            } else {
                                CompletableFuture<SectionRenderDispatcher.SectionTaskResult> completablefuture = RenderSection.this.uploadSectionIndexBuffer(
                                        bytebufferbuilder$result, RenderType.translucent()
                                    )
                                    .thenApply(p_297230_ -> SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                                return completablefuture.handle((p_357949_, p_357950_) -> {
                                    if (p_357950_ != null && !(p_357950_ instanceof CancellationException) && !(p_357950_ instanceof InterruptedException)) {
                                        Minecraft.getInstance().delayCrash(CrashReport.forThrowable(p_357950_, "Rendering section"));
                                    }

                                    if (this.isCancelled.get()) {
                                        return SectionRenderDispatcher.SectionTaskResult.CANCELLED;
                                    } else {
                                        RenderSection.this.pointOfView.set(sectionrenderdispatcher$translucencypointofview);
                                        return SectionRenderDispatcher.SectionTaskResult.SUCCESSFUL;
                                    }
                                });
                            }
                        }
                    } else {
                        return CompletableFuture.completedFuture(SectionRenderDispatcher.SectionTaskResult.CANCELLED);
                    }
                }
            }

            @Override
            public void cancel() {
                this.isCancelled.set(true);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static final class SectionBuffers implements AutoCloseable {
        GpuBuffer vertexBuffer;
        @Nullable
        GpuBuffer indexBuffer;
        private int indexCount;
        private VertexFormat.IndexType indexType;

        public SectionBuffers(GpuBuffer pVertexBuffer, @Nullable GpuBuffer pIndexBuffer, int pIndexCount, VertexFormat.IndexType pIndexType) {
            this.vertexBuffer = pVertexBuffer;
            this.indexBuffer = pIndexBuffer;
            this.indexCount = pIndexCount;
            this.indexType = pIndexType;
        }

        public GpuBuffer getVertexBuffer() {
            return this.vertexBuffer;
        }

        @Nullable
        public GpuBuffer getIndexBuffer() {
            return this.indexBuffer;
        }

        public void setIndexBuffer(@Nullable GpuBuffer pIndexBuffer) {
            this.indexBuffer = pIndexBuffer;
        }

        public int getIndexCount() {
            return this.indexCount;
        }

        public VertexFormat.IndexType getIndexType() {
            return this.indexType;
        }

        public void setIndexType(VertexFormat.IndexType pIndexType) {
            this.indexType = pIndexType;
        }

        public void setIndexCount(int pIndexCount) {
            this.indexCount = pIndexCount;
        }

        public void setVertexBuffer(GpuBuffer pVertexBuffer) {
            this.vertexBuffer = pVertexBuffer;
        }

        @Override
        public void close() {
            this.vertexBuffer.close();
            if (this.indexBuffer != null) {
                this.indexBuffer.close();
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    static enum SectionTaskResult {
        SUCCESSFUL,
        CANCELLED;
    }

    @OnlyIn(Dist.CLIENT)
    public static final class TranslucencyPointOfView {
        private int x;
        private int y;
        private int z;

        public static SectionRenderDispatcher.TranslucencyPointOfView of(Vec3 pCameraPosition, long pSectionNode) {
            return new SectionRenderDispatcher.TranslucencyPointOfView().set(pCameraPosition, pSectionNode);
        }

        public SectionRenderDispatcher.TranslucencyPointOfView set(Vec3 pCameraPosition, long pSectionNode) {
            this.x = getCoordinate(pCameraPosition.x(), SectionPos.x(pSectionNode));
            this.y = getCoordinate(pCameraPosition.y(), SectionPos.y(pSectionNode));
            this.z = getCoordinate(pCameraPosition.z(), SectionPos.z(pSectionNode));
            return this;
        }

        private static int getCoordinate(double pCameraCoord, int pSectionCoord) {
            int i = SectionPos.blockToSectionCoord(pCameraCoord) - pSectionCoord;
            return Mth.clamp(i, -1, 1);
        }

        public boolean isAxisAligned() {
            return this.x == 0 || this.y == 0 || this.z == 0;
        }

        @Override
        public boolean equals(Object pOther) {
            if (pOther == this) {
                return true;
            } else {
                return !(pOther instanceof SectionRenderDispatcher.TranslucencyPointOfView sectionrenderdispatcher$translucencypointofview)
                    ? false
                    : this.x == sectionrenderdispatcher$translucencypointofview.x
                        && this.y == sectionrenderdispatcher$translucencypointofview.y
                        && this.z == sectionrenderdispatcher$translucencypointofview.z;
            }
        }
    }
}