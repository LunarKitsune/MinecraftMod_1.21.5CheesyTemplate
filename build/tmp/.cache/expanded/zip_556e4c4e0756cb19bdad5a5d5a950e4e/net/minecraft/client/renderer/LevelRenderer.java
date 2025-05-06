package net.minecraft.client.renderer;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.framegraph.FramePass;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.resource.RenderTargetDescriptor;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Set;
import java.util.SortedSet;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.PrioritizeChunkUpdates;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.chunk.RenderRegionCache;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.ARGB;
import net.minecraft.util.Brightness;
import net.minecraft.util.Mth;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.profiling.Zone;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector4f;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class LevelRenderer implements ResourceManagerReloadListener, AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation TRANSPARENCY_POST_CHAIN_ID = ResourceLocation.withDefaultNamespace("transparency");
    private static final ResourceLocation ENTITY_OUTLINE_POST_CHAIN_ID = ResourceLocation.withDefaultNamespace("entity_outline");
    public static final int SECTION_SIZE = 16;
    public static final int HALF_SECTION_SIZE = 8;
    public static final int NEARBY_SECTION_DISTANCE_IN_BLOCKS = 32;
    private static final int MINIMUM_TRANSPARENT_SORT_COUNT = 15;
    private final Minecraft minecraft;
    private final EntityRenderDispatcher entityRenderDispatcher;
    private final BlockEntityRenderDispatcher blockEntityRenderDispatcher;
    private final RenderBuffers renderBuffers;
    private final SkyRenderer skyRenderer = new SkyRenderer();
    private final CloudRenderer cloudRenderer = new CloudRenderer();
    private final WorldBorderRenderer worldBorderRenderer = new WorldBorderRenderer();
    private WeatherEffectRenderer weatherEffectRenderer = new WeatherEffectRenderer();
    @Nullable
    private ClientLevel level;
    private final SectionOcclusionGraph sectionOcclusionGraph = new SectionOcclusionGraph();
    private final ObjectArrayList<SectionRenderDispatcher.RenderSection> visibleSections = new ObjectArrayList<>(10000);
    private final ObjectArrayList<SectionRenderDispatcher.RenderSection> nearbyVisibleSections = new ObjectArrayList<>(50);
    private final Set<BlockEntity> globalBlockEntities = Sets.newHashSet();
    @Nullable
    private ViewArea viewArea;
    private int ticks;
    private final Int2ObjectMap<BlockDestructionProgress> destroyingBlocks = new Int2ObjectOpenHashMap<>();
    private final Long2ObjectMap<SortedSet<BlockDestructionProgress>> destructionProgress = new Long2ObjectOpenHashMap<>();
    @Nullable
    private RenderTarget entityOutlineTarget;
    private final LevelTargetBundle targets = new LevelTargetBundle();
    private int lastCameraSectionX = Integer.MIN_VALUE;
    private int lastCameraSectionY = Integer.MIN_VALUE;
    private int lastCameraSectionZ = Integer.MIN_VALUE;
    private double prevCamX = Double.MIN_VALUE;
    private double prevCamY = Double.MIN_VALUE;
    private double prevCamZ = Double.MIN_VALUE;
    private double prevCamRotX = Double.MIN_VALUE;
    private double prevCamRotY = Double.MIN_VALUE;
    @Nullable
    private SectionRenderDispatcher sectionRenderDispatcher;
    private int lastViewDistance = -1;
    private final List<Entity> visibleEntities = new ArrayList<>();
    private int visibleEntityCount;
    private Frustum cullingFrustum;
    private boolean captureFrustum;
    @Nullable
    private Frustum capturedFrustum;
    @Nullable
    private BlockPos lastTranslucentSortBlockPos;
    private int translucencyResortIterationIndex;

    public LevelRenderer(Minecraft pMinecraft, EntityRenderDispatcher pEntityRenderDispatcher, BlockEntityRenderDispatcher pBlockEntityRenderDispatcher, RenderBuffers pRenderBuffers) {
        this.minecraft = pMinecraft;
        this.entityRenderDispatcher = pEntityRenderDispatcher;
        this.blockEntityRenderDispatcher = pBlockEntityRenderDispatcher;
        this.renderBuffers = pRenderBuffers;
    }

    public void tickParticles(Camera pCamera) {
        this.weatherEffectRenderer.tickRainParticles(this.minecraft.level, pCamera, this.ticks, this.minecraft.options.particles().get());
    }

    @Override
    public void close() {
        if (this.entityOutlineTarget != null) {
            this.entityOutlineTarget.destroyBuffers();
        }

        this.skyRenderer.close();
        this.cloudRenderer.close();
    }

    @Override
    public void onResourceManagerReload(ResourceManager pResourceManager) {
        this.initOutline();
    }

    public void initOutline() {
        if (this.entityOutlineTarget != null) {
            this.entityOutlineTarget.destroyBuffers();
        }

        this.entityOutlineTarget = new TextureTarget("Entity Outline", this.minecraft.getWindow().getWidth(), this.minecraft.getWindow().getHeight(), true);
    }

    @Nullable
    private PostChain getTransparencyChain() {
        if (!Minecraft.useShaderTransparency()) {
            return null;
        } else {
            PostChain postchain = this.minecraft.getShaderManager().getPostChain(TRANSPARENCY_POST_CHAIN_ID, LevelTargetBundle.SORTING_TARGETS);
            if (postchain == null) {
                this.minecraft.options.graphicsMode().set(GraphicsStatus.FANCY);
                this.minecraft.options.save();
            }

            return postchain;
        }
    }

    public void doEntityOutline() {
        if (this.shouldShowEntityOutlines()) {
            this.entityOutlineTarget.blitAndBlendToTexture(this.minecraft.getMainRenderTarget().getColorTexture());
        }
    }

    public boolean shouldShowEntityOutlines() {
        return !this.minecraft.gameRenderer.isPanoramicMode() && this.entityOutlineTarget != null && this.minecraft.player != null;
    }

    public void setLevel(@Nullable ClientLevel pLevel) {
        this.lastCameraSectionX = Integer.MIN_VALUE;
        this.lastCameraSectionY = Integer.MIN_VALUE;
        this.lastCameraSectionZ = Integer.MIN_VALUE;
        this.entityRenderDispatcher.setLevel(pLevel);
        this.level = pLevel;
        if (pLevel != null) {
            this.allChanged();
        } else {
            if (this.viewArea != null) {
                this.viewArea.releaseAllBuffers();
                this.viewArea = null;
            }

            if (this.sectionRenderDispatcher != null) {
                this.sectionRenderDispatcher.dispose();
            }

            this.sectionRenderDispatcher = null;
            this.globalBlockEntities.clear();
            this.sectionOcclusionGraph.waitAndReset(null);
            this.clearVisibleSections();
        }
    }

    private void clearVisibleSections() {
        this.visibleSections.clear();
        this.nearbyVisibleSections.clear();
    }

    public void allChanged() {
        if (this.level != null) {
            this.level.clearTintCaches();
            if (this.sectionRenderDispatcher == null) {
                this.sectionRenderDispatcher = new SectionRenderDispatcher(
                    this.level, this, Util.backgroundExecutor(), this.renderBuffers, this.minecraft.getBlockRenderer(), this.minecraft.getBlockEntityRenderDispatcher()
                );
            } else {
                this.sectionRenderDispatcher.setLevel(this.level);
            }

            this.cloudRenderer.markForRebuild();
            ItemBlockRenderTypes.setFancy(Minecraft.useFancyGraphics());
            this.lastViewDistance = this.minecraft.options.getEffectiveRenderDistance();
            if (this.viewArea != null) {
                this.viewArea.releaseAllBuffers();
            }

            this.sectionRenderDispatcher.blockUntilClear();
            synchronized (this.globalBlockEntities) {
                this.globalBlockEntities.clear();
            }

            this.viewArea = new ViewArea(this.sectionRenderDispatcher, this.level, this.minecraft.options.getEffectiveRenderDistance(), this);
            this.sectionOcclusionGraph.waitAndReset(this.viewArea);
            this.clearVisibleSections();
            Camera camera = this.minecraft.gameRenderer.getMainCamera();
            this.viewArea.repositionCamera(SectionPos.of(camera.getPosition()));
        }
    }

    public void resize(int pWidth, int pHeight) {
        this.needsUpdate();
        if (this.entityOutlineTarget != null) {
            this.entityOutlineTarget.resize(pWidth, pHeight);
        }
    }

    public String getSectionStatistics() {
        int i = this.viewArea.sections.length;
        int j = this.countRenderedSections();
        return String.format(
            Locale.ROOT,
            "C: %d/%d %sD: %d, %s",
            j,
            i,
            this.minecraft.smartCull ? "(s) " : "",
            this.lastViewDistance,
            this.sectionRenderDispatcher == null ? "null" : this.sectionRenderDispatcher.getStats()
        );
    }

    public SectionRenderDispatcher getSectionRenderDispatcher() {
        return this.sectionRenderDispatcher;
    }

    public double getTotalSections() {
        return this.viewArea.sections.length;
    }

    public double getLastViewDistance() {
        return this.lastViewDistance;
    }

    public int countRenderedSections() {
        int i = 0;

        for (SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection : this.visibleSections) {
            if (sectionrenderdispatcher$rendersection.getCompiled().hasRenderableLayers()) {
                i++;
            }
        }

        return i;
    }

    public String getEntityStatistics() {
        return "E: " + this.visibleEntityCount + "/" + this.level.getEntityCount() + ", SD: " + this.level.getServerSimulationDistance();
    }

    private void setupRender(Camera pCamera, Frustum pFrustum, boolean pHasCapturedFrustum, boolean pIsSpectator) {
        Vec3 vec3 = pCamera.getPosition();
        if (this.minecraft.options.getEffectiveRenderDistance() != this.lastViewDistance) {
            this.allChanged();
        }

        ProfilerFiller profilerfiller = Profiler.get();
        profilerfiller.push("camera");
        int i = SectionPos.posToSectionCoord(vec3.x());
        int j = SectionPos.posToSectionCoord(vec3.y());
        int k = SectionPos.posToSectionCoord(vec3.z());
        if (this.lastCameraSectionX != i || this.lastCameraSectionY != j || this.lastCameraSectionZ != k) {
            this.lastCameraSectionX = i;
            this.lastCameraSectionY = j;
            this.lastCameraSectionZ = k;
            this.viewArea.repositionCamera(SectionPos.of(vec3));
            this.worldBorderRenderer.invalidate();
        }

        this.sectionRenderDispatcher.setCamera(vec3);
        profilerfiller.popPush("cull");
        double d0 = Math.floor(vec3.x / 8.0);
        double d1 = Math.floor(vec3.y / 8.0);
        double d2 = Math.floor(vec3.z / 8.0);
        if (d0 != this.prevCamX || d1 != this.prevCamY || d2 != this.prevCamZ) {
            this.sectionOcclusionGraph.invalidate();
        }

        this.prevCamX = d0;
        this.prevCamY = d1;
        this.prevCamZ = d2;
        profilerfiller.popPush("update");
        if (!pHasCapturedFrustum) {
            boolean flag = this.minecraft.smartCull;
            if (pIsSpectator && this.level.getBlockState(pCamera.getBlockPosition()).isSolidRender()) {
                flag = false;
            }

            profilerfiller.push("section_occlusion_graph");
            this.sectionOcclusionGraph.update(flag, pCamera, pFrustum, this.visibleSections, this.level.getChunkSource().getLoadedEmptySections());
            profilerfiller.pop();
            double d3 = Math.floor(pCamera.getXRot() / 2.0F);
            double d4 = Math.floor(pCamera.getYRot() / 2.0F);
            if (this.sectionOcclusionGraph.consumeFrustumUpdate() || d3 != this.prevCamRotX || d4 != this.prevCamRotY) {
                this.applyFrustum(offsetFrustum(pFrustum));
                this.prevCamRotX = d3;
                this.prevCamRotY = d4;
            }
        }

        profilerfiller.pop();
    }

    public static Frustum offsetFrustum(Frustum pFrustum) {
        return new Frustum(pFrustum).offsetToFullyIncludeCameraCube(8);
    }

    private void applyFrustum(Frustum pFrustum) {
        if (!Minecraft.getInstance().isSameThread()) {
            throw new IllegalStateException("applyFrustum called from wrong thread: " + Thread.currentThread().getName());
        } else {
            Profiler.get().push("apply_frustum");
            this.clearVisibleSections();
            this.sectionOcclusionGraph.addSectionsInFrustum(pFrustum, this.visibleSections, this.nearbyVisibleSections);
            Profiler.get().pop();
        }
    }

    public void addRecentlyCompiledSection(SectionRenderDispatcher.RenderSection pRenderSection) {
        this.sectionOcclusionGraph.schedulePropagationFrom(pRenderSection);
    }

    public void prepareCullFrustum(Vec3 pCameraPosition, Matrix4f pFrustumMatrix, Matrix4f pProjectionMatrix) {
        this.cullingFrustum = new Frustum(pFrustumMatrix, pProjectionMatrix);
        this.cullingFrustum.prepare(pCameraPosition.x(), pCameraPosition.y(), pCameraPosition.z());
    }

    public void renderLevel(
        GraphicsResourceAllocator pGraphicsResourceAllocator,
        DeltaTracker pDeltaTracker,
        boolean pRenderBlockOutline,
        Camera pCamera,
        GameRenderer pGameRenderer,
        Matrix4f pFrustumMatrix,
        Matrix4f pProjectionMatrix
    ) {
        float f = pDeltaTracker.getGameTimeDeltaPartialTick(false);
        RenderSystem.setShaderGameTime(this.level.getGameTime(), f);
        this.blockEntityRenderDispatcher.prepare(this.level, pCamera, this.minecraft.hitResult);
        this.entityRenderDispatcher.prepare(this.level, pCamera, this.minecraft.crosshairPickEntity);
        final ProfilerFiller profilerfiller = Profiler.get();
        profilerfiller.popPush("light_update_queue");
        this.level.pollLightUpdates();
        profilerfiller.popPush("light_updates");
        this.level.getChunkSource().getLightEngine().runLightUpdates();
        Vec3 vec3 = pCamera.getPosition();
        double d0 = vec3.x();
        double d1 = vec3.y();
        double d2 = vec3.z();
        profilerfiller.popPush("culling");
        boolean flag = this.capturedFrustum != null;
        Frustum frustum = flag ? this.capturedFrustum : this.cullingFrustum;
        Profiler.get().popPush("captureFrustum");
        if (this.captureFrustum) {
            this.capturedFrustum = flag ? new Frustum(pFrustumMatrix, pProjectionMatrix) : frustum;
            this.capturedFrustum.prepare(d0, d1, d2);
            this.captureFrustum = false;
        }

        profilerfiller.popPush("fog");
        float f1 = pGameRenderer.getRenderDistance();
        boolean flag1 = this.minecraft.level.effects().isFoggyAt(Mth.floor(d0), Mth.floor(d1)) || this.minecraft.gui.getBossOverlay().shouldCreateWorldFog();
        Vector4f vector4f = FogRenderer.computeFogColor(pCamera, f, this.minecraft.level, this.minecraft.options.getEffectiveRenderDistance(), pGameRenderer.getDarkenWorldAmount(f));
        FogParameters fogparameters = FogRenderer.setupFog(pCamera, FogRenderer.FogMode.FOG_TERRAIN, vector4f, f1, flag1, f);
        FogParameters fogparameters1 = FogRenderer.setupFog(pCamera, FogRenderer.FogMode.FOG_SKY, vector4f, f1, flag1, f);
        profilerfiller.popPush("cullEntities");
        boolean flag2 = this.collectVisibleEntities(pCamera, frustum, this.visibleEntities);
        this.visibleEntityCount = this.visibleEntities.size();
        profilerfiller.popPush("terrain_setup");
        this.setupRender(pCamera, frustum, flag, this.minecraft.player.isSpectator());
        profilerfiller.popPush("compile_sections");
        this.compileSections(pCamera);
        Matrix4fStack matrix4fstack = RenderSystem.getModelViewStack();
        matrix4fstack.pushMatrix();
        matrix4fstack.mul(pFrustumMatrix);
        FrameGraphBuilder framegraphbuilder = new FrameGraphBuilder();
        this.targets.main = framegraphbuilder.importExternal("main", this.minecraft.getMainRenderTarget());
        int i = this.minecraft.getMainRenderTarget().width;
        int j = this.minecraft.getMainRenderTarget().height;
        RenderTargetDescriptor rendertargetdescriptor = new RenderTargetDescriptor(i, j, true, 0);
        PostChain postchain = this.getTransparencyChain();
        if (postchain != null) {
            this.targets.translucent = framegraphbuilder.createInternal("translucent", rendertargetdescriptor);
            this.targets.itemEntity = framegraphbuilder.createInternal("item_entity", rendertargetdescriptor);
            this.targets.particles = framegraphbuilder.createInternal("particles", rendertargetdescriptor);
            this.targets.weather = framegraphbuilder.createInternal("weather", rendertargetdescriptor);
            this.targets.clouds = framegraphbuilder.createInternal("clouds", rendertargetdescriptor);
        }

        if (this.entityOutlineTarget != null) {
            this.targets.entityOutline = framegraphbuilder.importExternal("entity_outline", this.entityOutlineTarget);
        }

        FramePass framepass = framegraphbuilder.addPass("clear");
        this.targets.main = framepass.readsAndWrites(this.targets.main);
        framepass.executes(
            () -> {
                RenderTarget rendertarget = this.minecraft.getMainRenderTarget();
                RenderSystem.getDevice()
                    .createCommandEncoder()
                    .clearColorAndDepthTextures(
                        rendertarget.getColorTexture(), ARGB.colorFromFloat(0.0F, vector4f.x, vector4f.y, vector4f.z), rendertarget.getDepthTexture(), 1.0
                    );
            }
        );
        if (!flag1) {
            this.addSkyPass(framegraphbuilder, pCamera, f, fogparameters1);
        }

        this.addMainPass(framegraphbuilder, frustum, pCamera, pFrustumMatrix, pProjectionMatrix, fogparameters, pRenderBlockOutline, flag2, pDeltaTracker, profilerfiller);
        PostChain postchain1 = this.minecraft.getShaderManager().getPostChain(ENTITY_OUTLINE_POST_CHAIN_ID, LevelTargetBundle.OUTLINE_TARGETS);
        if (flag2 && postchain1 != null) {
            postchain1.addToFrame(framegraphbuilder, i, j, this.targets, null);
        }

        this.addParticlesPass(framegraphbuilder, pCamera, f, fogparameters, frustum);
        CloudStatus cloudstatus = this.minecraft.options.getCloudsType();
        if (cloudstatus != CloudStatus.OFF) {
            float f2 = this.level.effects().getCloudHeight();
            if (!Float.isNaN(f2)) {
                float f3 = this.ticks + f;
                int k = this.level.getCloudColor(f);
                this.addCloudsPass(framegraphbuilder, cloudstatus, pCamera.getPosition(), f3, k, f2 + 0.33F);
            }
        }

        this.addWeatherPass(framegraphbuilder, pCamera.getPosition(), f, fogparameters);
        if (postchain != null) {
            postchain.addToFrame(framegraphbuilder, i, j, this.targets, null);
        }

        this.addLateDebugPass(framegraphbuilder, vec3, fogparameters);
        profilerfiller.popPush("framegraph");
        framegraphbuilder.execute(pGraphicsResourceAllocator, new FrameGraphBuilder.Inspector() {
            @Override
            public void beforeExecutePass(String p_367748_) {
                profilerfiller.push(p_367748_);
            }

            @Override
            public void afterExecutePass(String p_367757_) {
                profilerfiller.pop();
            }
        });
        this.visibleEntities.clear();
        this.targets.clear();
        matrix4fstack.popMatrix();
        RenderSystem.setShaderFog(FogParameters.NO_FOG);
    }

    private void addMainPass(
        FrameGraphBuilder pFrameGraphBuilder,
        Frustum pFrustum,
        Camera pCamera,
        Matrix4f pFrustumMatrix,
        Matrix4f pProjectionMatrix,
        FogParameters pFogParameters,
        boolean pRenderBlockOutline,
        boolean pRenderEntityOutline,
        DeltaTracker pDeltaTracker,
        ProfilerFiller pProfiler
    ) {
        FramePass framepass = pFrameGraphBuilder.addPass("main");
        this.targets.main = framepass.readsAndWrites(this.targets.main);
        if (this.targets.translucent != null) {
            this.targets.translucent = framepass.readsAndWrites(this.targets.translucent);
        }

        if (this.targets.itemEntity != null) {
            this.targets.itemEntity = framepass.readsAndWrites(this.targets.itemEntity);
        }

        if (this.targets.weather != null) {
            this.targets.weather = framepass.readsAndWrites(this.targets.weather);
        }

        if (pRenderEntityOutline && this.targets.entityOutline != null) {
            this.targets.entityOutline = framepass.readsAndWrites(this.targets.entityOutline);
        }

        ResourceHandle<RenderTarget> resourcehandle = this.targets.main;
        ResourceHandle<RenderTarget> resourcehandle1 = this.targets.translucent;
        ResourceHandle<RenderTarget> resourcehandle2 = this.targets.itemEntity;
        ResourceHandle<RenderTarget> resourcehandle3 = this.targets.entityOutline;
        framepass.executes(() -> {
            RenderSystem.setShaderFog(pFogParameters);
            float f = pDeltaTracker.getGameTimeDeltaPartialTick(false);
            Vec3 vec3 = pCamera.getPosition();
            double d0 = vec3.x();
            double d1 = vec3.y();
            double d2 = vec3.z();
            pProfiler.push("terrain");
            this.renderSectionLayer(RenderType.solid(), d0, d1, d2, pFrustumMatrix, pProjectionMatrix);
            // FORGE: fix flickering leaves when mods mess up the blurMipmap settings
            var atlas = this.minecraft.getModelManager().getAtlas(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS);
            atlas.setBlurMipmap(false, this.minecraft.options.mipmapLevels().get() > 0);
            this.renderSectionLayer(RenderType.cutoutMipped(), d0, d1, d2, pFrustumMatrix, pProjectionMatrix);
            atlas.restoreLastBlurMipmap();
            this.renderSectionLayer(RenderType.cutout(), d0, d1, d2, pFrustumMatrix, pProjectionMatrix);
            if (this.level.effects().constantAmbientLight()) {
                Lighting.setupNetherLevel();
            } else {
                Lighting.setupLevel();
            }

            if (resourcehandle2 != null) {
                resourcehandle2.get().copyDepthFrom(this.minecraft.getMainRenderTarget());
            }

            if (this.shouldShowEntityOutlines() && resourcehandle3 != null) {
                RenderTarget rendertarget = resourcehandle3.get();
                RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(rendertarget.getColorTexture(), 0, rendertarget.getDepthTexture(), 1.0);
            }

            PoseStack posestack = new PoseStack();
            MultiBufferSource.BufferSource multibuffersource$buffersource = this.renderBuffers.bufferSource();
            MultiBufferSource.BufferSource multibuffersource$buffersource1 = this.renderBuffers.crumblingBufferSource();
            pProfiler.popPush("entities");
            this.renderEntities(posestack, multibuffersource$buffersource, pCamera, pDeltaTracker, this.visibleEntities);
            multibuffersource$buffersource.endLastBatch();
            this.checkPoseStack(posestack);
            pProfiler.popPush("blockentities");
            var renderOutline = this.renderBlockEntities(posestack, multibuffersource$buffersource, multibuffersource$buffersource1, pCamera, f, pFrustum) || pRenderBlockOutline;
            multibuffersource$buffersource.endLastBatch();
            this.checkPoseStack(posestack);
            multibuffersource$buffersource.endBatch(RenderType.solid());
            multibuffersource$buffersource.endBatch(RenderType.endPortal());
            multibuffersource$buffersource.endBatch(RenderType.endGateway());
            multibuffersource$buffersource.endBatch(Sheets.solidBlockSheet());
            multibuffersource$buffersource.endBatch(Sheets.cutoutBlockSheet());
            multibuffersource$buffersource.endBatch(Sheets.bedSheet());
            multibuffersource$buffersource.endBatch(Sheets.shulkerBoxSheet());
            multibuffersource$buffersource.endBatch(Sheets.signSheet());
            multibuffersource$buffersource.endBatch(Sheets.hangingSignSheet());
            multibuffersource$buffersource.endBatch(Sheets.chestSheet());
            this.renderBuffers.outlineBufferSource().endOutlineBatch();
            if (renderOutline) {
                this.renderBlockOutline(pCamera, multibuffersource$buffersource, posestack, false, f);
            }

            pProfiler.popPush("debug");
            this.minecraft.debugRenderer.render(posestack, pFrustum, multibuffersource$buffersource, d0, d1, d2);
            multibuffersource$buffersource.endLastBatch();
            this.checkPoseStack(posestack);
            multibuffersource$buffersource.endBatch(Sheets.translucentItemSheet());
            multibuffersource$buffersource.endBatch(Sheets.bannerSheet());
            multibuffersource$buffersource.endBatch(Sheets.shieldSheet());
            multibuffersource$buffersource.endBatch(RenderType.armorEntityGlint());
            multibuffersource$buffersource.endBatch(RenderType.glint());
            multibuffersource$buffersource.endBatch(RenderType.glintTranslucent());
            multibuffersource$buffersource.endBatch(RenderType.entityGlint());
            pProfiler.popPush("destroyProgress");
            this.renderBlockDestroyAnimation(posestack, pCamera, multibuffersource$buffersource1);
            multibuffersource$buffersource1.endBatch();
            this.checkPoseStack(posestack);
            multibuffersource$buffersource.endBatch(RenderType.waterMask());
            multibuffersource$buffersource.endBatch();
            if (resourcehandle1 != null) {
                resourcehandle1.get().copyDepthFrom(resourcehandle.get());
            }

            pProfiler.popPush("translucent");
            this.renderSectionLayer(RenderType.translucent(), d0, d1, d2, pFrustumMatrix, pProjectionMatrix);
            pProfiler.popPush("string");
            this.renderSectionLayer(RenderType.tripwire(), d0, d1, d2, pFrustumMatrix, pProjectionMatrix);
            if (renderOutline) {
                this.renderBlockOutline(pCamera, multibuffersource$buffersource, posestack, true, f);
            }

            multibuffersource$buffersource.endBatch();
            pProfiler.pop();
        });
    }

    private void addParticlesPass(FrameGraphBuilder pFrameGraphBuilder, Camera pCamera, float pPartialTick, FogParameters pFog, Frustum frustum) {
        FramePass framepass = pFrameGraphBuilder.addPass("particles");
        if (this.targets.particles != null) {
            this.targets.particles = framepass.readsAndWrites(this.targets.particles);
            framepass.reads(this.targets.main);
        } else {
            this.targets.main = framepass.readsAndWrites(this.targets.main);
        }

        ResourceHandle<RenderTarget> resourcehandle = this.targets.main;
        ResourceHandle<RenderTarget> resourcehandle1 = this.targets.particles;
        framepass.executes(() -> {
            RenderSystem.setShaderFog(pFog);
            if (resourcehandle1 != null) {
                resourcehandle1.get().copyDepthFrom(resourcehandle.get());
            }

            this.minecraft.particleEngine.render(pCamera, pPartialTick, this.renderBuffers.bufferSource(), frustum);
        });
    }

    private void addCloudsPass(FrameGraphBuilder pFrameGraphBuilder, CloudStatus pCloudStatus, Vec3 pCameraPosition, float pTicks, int pCloudColor, float pCloudHeight) {
        FramePass framepass = pFrameGraphBuilder.addPass("clouds");
        if (this.targets.clouds != null) {
            this.targets.clouds = framepass.readsAndWrites(this.targets.clouds);
        } else {
            this.targets.main = framepass.readsAndWrites(this.targets.main);
        }

        framepass.executes(() -> this.cloudRenderer.render(pCloudColor, pCloudStatus, pCloudHeight, pCameraPosition, pTicks));
    }

    private void addWeatherPass(FrameGraphBuilder pFrameGraphBuilder, Vec3 pCameraPosition, float pPartialTick, FogParameters pFog) {
        int i = this.minecraft.options.getEffectiveRenderDistance() * 16;
        float f = this.minecraft.gameRenderer.getDepthFar();
        FramePass framepass = pFrameGraphBuilder.addPass("weather");
        if (this.targets.weather != null) {
            this.targets.weather = framepass.readsAndWrites(this.targets.weather);
        } else {
            this.targets.main = framepass.readsAndWrites(this.targets.main);
        }

        framepass.executes(() -> {
            RenderSystem.setShaderFog(pFog);
            MultiBufferSource.BufferSource multibuffersource$buffersource = this.renderBuffers.bufferSource();
            this.weatherEffectRenderer.render(this.minecraft.level, multibuffersource$buffersource, this.ticks, pPartialTick, pCameraPosition);
            this.worldBorderRenderer.render(this.level.getWorldBorder(), pCameraPosition, i, f);
            multibuffersource$buffersource.endBatch();
        });
    }

    private void addLateDebugPass(FrameGraphBuilder pFrameGraphBuilder, Vec3 pCameraPosition, FogParameters pFog) {
        FramePass framepass = pFrameGraphBuilder.addPass("late_debug");
        this.targets.main = framepass.readsAndWrites(this.targets.main);
        if (this.targets.itemEntity != null) {
            this.targets.itemEntity = framepass.readsAndWrites(this.targets.itemEntity);
        }

        ResourceHandle<RenderTarget> resourcehandle = this.targets.main;
        framepass.executes(() -> {
            RenderSystem.setShaderFog(pFog);
            PoseStack posestack = new PoseStack();
            MultiBufferSource.BufferSource multibuffersource$buffersource = this.renderBuffers.bufferSource();
            this.minecraft.debugRenderer.renderAfterTranslucents(posestack, multibuffersource$buffersource, pCameraPosition.x, pCameraPosition.y, pCameraPosition.z);
            multibuffersource$buffersource.endLastBatch();
            this.checkPoseStack(posestack);
        });
    }

    private boolean collectVisibleEntities(Camera pCamera, Frustum pFrustum, List<Entity> pOutput) {
        Vec3 vec3 = pCamera.getPosition();
        double d0 = vec3.x();
        double d1 = vec3.y();
        double d2 = vec3.z();
        boolean flag = false;
        boolean flag1 = this.shouldShowEntityOutlines();
        Entity.setViewScale(Mth.clamp(this.minecraft.options.getEffectiveRenderDistance() / 8.0, 1.0, 2.5) * this.minecraft.options.entityDistanceScaling().get());

        for (Entity entity : this.level.entitiesForRendering()) {
            if (this.entityRenderDispatcher.shouldRender(entity, pFrustum, d0, d1, d2) || entity.hasIndirectPassenger(this.minecraft.player)) {
                BlockPos blockpos = entity.blockPosition();
                if ((this.level.isOutsideBuildHeight(blockpos.getY()) || this.isSectionCompiled(blockpos))
                    && (
                        entity != pCamera.getEntity()
                            || pCamera.isDetached()
                            || pCamera.getEntity() instanceof LivingEntity && ((LivingEntity)pCamera.getEntity()).isSleeping()
                    )
                    && (!(entity instanceof LocalPlayer) || pCamera.getEntity() == entity || (entity == minecraft.player && !minecraft.player.isSpectator()))) { //FORGE: render local player entity when it is not the renderViewEntity
                    pOutput.add(entity);
                    if (flag1 && (this.minecraft.shouldEntityAppearGlowing(entity) || entity.hasCustomOutlineRendering(this.minecraft.player))) {
                        flag = true;
                    }
                }
            }
        }

        return flag;
    }

    private void renderEntities(PoseStack pPoseStack, MultiBufferSource.BufferSource pBufferSource, Camera pCamera, DeltaTracker pDeltaTracker, List<Entity> pEntities) {
        Vec3 vec3 = pCamera.getPosition();
        double d0 = vec3.x();
        double d1 = vec3.y();
        double d2 = vec3.z();
        TickRateManager tickratemanager = this.minecraft.level.tickRateManager();
        boolean flag = this.shouldShowEntityOutlines();

        for (Entity entity : pEntities) {
            if (entity.tickCount == 0) {
                entity.xOld = entity.getX();
                entity.yOld = entity.getY();
                entity.zOld = entity.getZ();
            }

            MultiBufferSource multibuffersource;
            if (flag && this.minecraft.shouldEntityAppearGlowing(entity)) {
                OutlineBufferSource outlinebuffersource = this.renderBuffers.outlineBufferSource();
                multibuffersource = outlinebuffersource;
                int i = entity.getTeamColor();
                outlinebuffersource.setColor(ARGB.red(i), ARGB.green(i), ARGB.blue(i), 255);
            } else {
                multibuffersource = pBufferSource;
            }

            float f = pDeltaTracker.getGameTimeDeltaPartialTick(!tickratemanager.isEntityFrozen(entity));
            this.renderEntity(entity, d0, d1, d2, f, pPoseStack, multibuffersource);
        }
    }

    private boolean renderBlockEntities(
        PoseStack pPoseStack, MultiBufferSource.BufferSource pBufferSource, MultiBufferSource.BufferSource pCrumblingBufferSource, Camera pCamera, float pPartialTick, Frustum frustum
    ) {
        boolean customOutline = false;
        Vec3 vec3 = pCamera.getPosition();
        double d0 = vec3.x();
        double d1 = vec3.y();
        double d2 = vec3.z();

        for (SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection : this.visibleSections) {
            List<BlockEntity> list = sectionrenderdispatcher$rendersection.getCompiled().getRenderableBlockEntities();
            if (!list.isEmpty()) {
                for (BlockEntity blockentity : list) {
                    if (!frustum.isVisible(blockentity.getRenderBoundingBox())) continue;
                    BlockPos blockpos = blockentity.getBlockPos();
                    MultiBufferSource multibuffersource = pBufferSource;
                    pPoseStack.pushPose();
                    pPoseStack.translate(blockpos.getX() - d0, blockpos.getY() - d1, blockpos.getZ() - d2);
                    SortedSet<BlockDestructionProgress> sortedset = this.destructionProgress.get(blockpos.asLong());
                    if (sortedset != null && !sortedset.isEmpty()) {
                        int i = sortedset.last().getProgress();
                        if (i >= 0) {
                            PoseStack.Pose posestack$pose = pPoseStack.last();
                            VertexConsumer vertexconsumer = new SheetedDecalTextureGenerator(
                                pCrumblingBufferSource.getBuffer(ModelBakery.DESTROY_TYPES.get(i)), posestack$pose, 1.0F
                            );
                            multibuffersource = p_234298_ -> {
                                VertexConsumer vertexconsumer1 = pBufferSource.getBuffer(p_234298_);
                                return p_234298_.affectsCrumbling() ? VertexMultiConsumer.create(vertexconsumer, vertexconsumer1) : vertexconsumer1;
                            };
                        }
                    }

                    if (!customOutline && this.shouldShowEntityOutlines() && blockentity.hasCustomOutlineRendering(this.minecraft.player))
                        customOutline = true;

                    this.blockEntityRenderDispatcher.render(blockentity, pPartialTick, pPoseStack, multibuffersource);
                    pPoseStack.popPose();
                }
            }
        }

        synchronized (this.globalBlockEntities) {
            for (BlockEntity blockentity1 : this.globalBlockEntities) {
                if (!frustum.isVisible(blockentity1.getRenderBoundingBox())) continue;
                BlockPos blockpos1 = blockentity1.getBlockPos();
                pPoseStack.pushPose();
                pPoseStack.translate(blockpos1.getX() - d0, blockpos1.getY() - d1, blockpos1.getZ() - d2);
                if (!customOutline && this.shouldShowEntityOutlines() && blockentity1.hasCustomOutlineRendering(this.minecraft.player))
                    customOutline = true;
                this.blockEntityRenderDispatcher.render(blockentity1, pPartialTick, pPoseStack, pBufferSource);
                pPoseStack.popPose();
            }
        }

        return customOutline;
    }

    private void renderBlockDestroyAnimation(PoseStack pPoseStack, Camera pCamera, MultiBufferSource.BufferSource pBufferSource) {
        Vec3 vec3 = pCamera.getPosition();
        double d0 = vec3.x();
        double d1 = vec3.y();
        double d2 = vec3.z();

        for (Entry<SortedSet<BlockDestructionProgress>> entry : this.destructionProgress.long2ObjectEntrySet()) {
            BlockPos blockpos = BlockPos.of(entry.getLongKey());
            if (!(blockpos.distToCenterSqr(d0, d1, d2) > 1024.0)) {
                SortedSet<BlockDestructionProgress> sortedset = entry.getValue();
                if (sortedset != null && !sortedset.isEmpty()) {
                    int i = sortedset.last().getProgress();
                    pPoseStack.pushPose();
                    pPoseStack.translate(blockpos.getX() - d0, blockpos.getY() - d1, blockpos.getZ() - d2);
                    PoseStack.Pose posestack$pose = pPoseStack.last();
                    VertexConsumer vertexconsumer = new SheetedDecalTextureGenerator(pBufferSource.getBuffer(ModelBakery.DESTROY_TYPES.get(i)), posestack$pose, 1.0F);
                    this.minecraft.getBlockRenderer().renderBreakingTexture(this.level.getBlockState(blockpos), blockpos, this.level, pPoseStack, vertexconsumer, level.getModelDataManager().getAtOrEmpty(blockpos));
                    pPoseStack.popPose();
                }
            }
        }
    }

    private void renderBlockOutline(Camera pCamera, MultiBufferSource.BufferSource pBufferSource, PoseStack pPoseStack, boolean pSort, float partialTicks) {
        if (this.minecraft.hitResult instanceof BlockHitResult blockhitresult) {
            if (blockhitresult.getType() != HitResult.Type.MISS) {
                BlockPos blockpos = blockhitresult.getBlockPos();
                BlockState blockstate = this.level.getBlockState(blockpos);
                if (!blockstate.isAir() && this.level.getWorldBorder().isWithinBounds(blockpos)) {
                    boolean flag = ItemBlockRenderTypes.getChunkRenderType(blockstate).sortOnUpload();
                    if (flag != pSort) {
                        return;
                    }

                    if (net.minecraftforge.client.ForgeHooksClient.onDrawHighlight(this, pCamera, blockhitresult, partialTicks, pPoseStack, pBufferSource))
                        return;

                    Vec3 vec3 = pCamera.getPosition();
                    Boolean obool = this.minecraft.options.highContrastBlockOutline().get();
                    if (obool) {
                        VertexConsumer vertexconsumer = pBufferSource.getBuffer(RenderType.secondaryBlockOutline());
                        this.renderHitOutline(
                            pPoseStack, vertexconsumer, pCamera.getEntity(), vec3.x, vec3.y, vec3.z, blockpos, blockstate, -16777216
                        );
                    }

                    VertexConsumer vertexconsumer1 = pBufferSource.getBuffer(RenderType.lines());
                    int i = obool ? -11010079 : ARGB.color(102, -16777216);
                    this.renderHitOutline(pPoseStack, vertexconsumer1, pCamera.getEntity(), vec3.x, vec3.y, vec3.z, blockpos, blockstate, i);
                    pBufferSource.endLastBatch();
                }
            }
        } else if (this.minecraft.hitResult instanceof net.minecraft.world.phys.EntityHitResult entity) {
            net.minecraftforge.client.ForgeHooksClient.onDrawHighlight(this, pCamera, entity, partialTicks, pPoseStack, pBufferSource);
        }
    }

    private void checkPoseStack(PoseStack pPoseStack) {
        if (!pPoseStack.isEmpty()) {
            throw new IllegalStateException("Pose stack not empty");
        }
    }

    private void renderEntity(
        Entity pEntity, double pCamX, double pCamY, double pCamZ, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBufferSource
    ) {
        double d0 = Mth.lerp(pPartialTick, pEntity.xOld, pEntity.getX());
        double d1 = Mth.lerp(pPartialTick, pEntity.yOld, pEntity.getY());
        double d2 = Mth.lerp(pPartialTick, pEntity.zOld, pEntity.getZ());
        this.entityRenderDispatcher
            .render(
                pEntity, d0 - pCamX, d1 - pCamY, d2 - pCamZ, pPartialTick, pPoseStack, pBufferSource, this.entityRenderDispatcher.getPackedLightCoords(pEntity, pPartialTick)
            );
    }

    private void scheduleTranslucentSectionResort(Vec3 pCameraPosition) {
        if (!this.visibleSections.isEmpty()) {
            BlockPos blockpos = BlockPos.containing(pCameraPosition);
            boolean flag = !blockpos.equals(this.lastTranslucentSortBlockPos);
            Profiler.get().push("translucent_sort");
            SectionRenderDispatcher.TranslucencyPointOfView sectionrenderdispatcher$translucencypointofview = new SectionRenderDispatcher.TranslucencyPointOfView();

            for (SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection : this.nearbyVisibleSections) {
                this.scheduleResort(sectionrenderdispatcher$rendersection, sectionrenderdispatcher$translucencypointofview, pCameraPosition, flag, true);
            }

            this.translucencyResortIterationIndex = this.translucencyResortIterationIndex % this.visibleSections.size();
            int i = Math.max(this.visibleSections.size() / 8, 15);

            while (i-- > 0) {
                int j = this.translucencyResortIterationIndex++ % this.visibleSections.size();
                this.scheduleResort(this.visibleSections.get(j), sectionrenderdispatcher$translucencypointofview, pCameraPosition, flag, false);
            }

            this.lastTranslucentSortBlockPos = blockpos;
            Profiler.get().pop();
        }
    }

    private void scheduleResort(
        SectionRenderDispatcher.RenderSection pSection,
        SectionRenderDispatcher.TranslucencyPointOfView pPointOfView,
        Vec3 pCameraPosition,
        boolean pForce,
        boolean pIgnoreAxisAlignment
    ) {
        pPointOfView.set(pCameraPosition, pSection.getSectionNode());
        boolean flag = !pPointOfView.equals(pSection.pointOfView.get());
        boolean flag1 = pForce && (pPointOfView.isAxisAligned() || pIgnoreAxisAlignment);
        if ((flag1 || flag) && !pSection.transparencyResortingScheduled() && pSection.hasTranslucentGeometry()) {
            pSection.resortTransparency(this.sectionRenderDispatcher);
        }
    }

    private void renderSectionLayer(RenderType pRenderType, double pX, double pY, double pZ, Matrix4f pFrustrumMatrix, Matrix4f pProjectionMatrix) {
        RenderSystem.assertOnRenderThread();
        Zone zone = Profiler.get().zone(() -> "render_" + pRenderType.name);
        zone.addText(pRenderType::toString);
        boolean flag = pRenderType != RenderType.translucent();
        ObjectListIterator<SectionRenderDispatcher.RenderSection> objectlistiterator = this.visibleSections.listIterator(flag ? 0 : this.visibleSections.size());
        pRenderType.setupRenderState();
        RenderPipeline renderpipeline = pRenderType.getRenderPipeline();
        ArrayList<RenderPass.Draw> arraylist = new ArrayList<>();
        RenderSystem.AutoStorageIndexBuffer rendersystem$autostorageindexbuffer = RenderSystem.getSequentialBuffer(pRenderType.mode());
        int i = 0;

        while (flag ? objectlistiterator.hasNext() : objectlistiterator.hasPrevious()) {
            SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection = flag ? objectlistiterator.next() : objectlistiterator.previous();
            SectionRenderDispatcher.SectionBuffers sectionrenderdispatcher$sectionbuffers = sectionrenderdispatcher$rendersection.getBuffers(pRenderType);
            if (!sectionrenderdispatcher$rendersection.getCompiled().isEmpty(pRenderType) && sectionrenderdispatcher$sectionbuffers != null) {
                GpuBuffer gpubuffer;
                VertexFormat.IndexType vertexformat$indextype;
                if (sectionrenderdispatcher$sectionbuffers.getIndexBuffer() == null) {
                    if (sectionrenderdispatcher$sectionbuffers.getIndexCount() > i) {
                        i = sectionrenderdispatcher$sectionbuffers.getIndexCount();
                    }

                    gpubuffer = null;
                    vertexformat$indextype = null;
                } else {
                    gpubuffer = sectionrenderdispatcher$sectionbuffers.getIndexBuffer();
                    vertexformat$indextype = sectionrenderdispatcher$sectionbuffers.getIndexType();
                }

                BlockPos blockpos = sectionrenderdispatcher$rendersection.getRenderOrigin();
                arraylist.add(
                    new RenderPass.Draw(
                        0,
                        sectionrenderdispatcher$sectionbuffers.getVertexBuffer(),
                        gpubuffer,
                        vertexformat$indextype,
                        0,
                        sectionrenderdispatcher$sectionbuffers.getIndexCount(),
                        p_389376_ -> p_389376_.upload(
                            "ModelOffset",
                            (float)(blockpos.getX() - pX),
                            (float)(blockpos.getY() - pY),
                            (float)(blockpos.getZ() - pZ)
                        )
                    )
                );
            }
        }

        GpuBuffer gpubuffer1 = i == 0 ? null : rendersystem$autostorageindexbuffer.getBuffer(i);
        VertexFormat.IndexType vertexformat$indextype1 = i == 0 ? null : rendersystem$autostorageindexbuffer.type();

        try (RenderPass renderpass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(pRenderType.getRenderTarget().getColorTexture(), OptionalInt.empty(), pRenderType.getRenderTarget().getDepthTexture(), OptionalDouble.empty())) {
            renderpass.setPipeline(renderpipeline);

            for (int j = 0; j < 12; j++) {
                GpuTexture gputexture = RenderSystem.getShaderTexture(j);
                if (gputexture != null) {
                    renderpass.bindSampler("Sampler" + j, gputexture);
                }
            }

            renderpass.drawMultipleIndexed(arraylist, gpubuffer1, vertexformat$indextype1);
        }

        zone.close();
        pRenderType.clearRenderState();
    }

    public void captureFrustum() {
        this.captureFrustum = true;
    }

    public void killFrustum() {
        this.capturedFrustum = null;
    }

    public void tick() {
        if (this.level.tickRateManager().runsNormally()) {
            this.ticks++;
        }

        if (this.ticks % 20 == 0) {
            Iterator<BlockDestructionProgress> iterator = this.destroyingBlocks.values().iterator();

            while (iterator.hasNext()) {
                BlockDestructionProgress blockdestructionprogress = iterator.next();
                int i = blockdestructionprogress.getUpdatedRenderTick();
                if (this.ticks - i > 400) {
                    iterator.remove();
                    this.removeProgress(blockdestructionprogress);
                }
            }
        }
    }

    private void removeProgress(BlockDestructionProgress pProgress) {
        long i = pProgress.getPos().asLong();
        Set<BlockDestructionProgress> set = this.destructionProgress.get(i);
        set.remove(pProgress);
        if (set.isEmpty()) {
            this.destructionProgress.remove(i);
        }
    }

    private void addSkyPass(FrameGraphBuilder pFrameGraphBuilder, Camera pCamera, float pPartialTick, FogParameters pFog) {
        FogType fogtype = pCamera.getFluidInCamera();
        if (fogtype != FogType.POWDER_SNOW && fogtype != FogType.LAVA && !this.doesMobEffectBlockSky(pCamera)) {
            DimensionSpecialEffects dimensionspecialeffects = this.level.effects();
            DimensionSpecialEffects.SkyType dimensionspecialeffects$skytype = dimensionspecialeffects.skyType();
            if (dimensionspecialeffects$skytype != DimensionSpecialEffects.SkyType.NONE) {
                FramePass framepass = pFrameGraphBuilder.addPass("sky");
                this.targets.main = framepass.readsAndWrites(this.targets.main);
                framepass.executes(() -> {
                    RenderSystem.setShaderFog(pFog);
                    if (dimensionspecialeffects$skytype == DimensionSpecialEffects.SkyType.END) {
                        this.skyRenderer.renderEndSky();
                    } else {
                        PoseStack posestack = new PoseStack();
                        float f = this.level.getSunAngle(pPartialTick);
                        float f1 = this.level.getTimeOfDay(pPartialTick);
                        float f2 = 1.0F - this.level.getRainLevel(pPartialTick);
                        float f3 = this.level.getStarBrightness(pPartialTick) * f2;
                        int i = dimensionspecialeffects.getSunriseOrSunsetColor(f1);
                        int j = this.level.getMoonPhase();
                        int k = this.level.getSkyColor(this.minecraft.gameRenderer.getMainCamera().getPosition(), pPartialTick);
                        float f4 = ARGB.redFloat(k);
                        float f5 = ARGB.greenFloat(k);
                        float f6 = ARGB.blueFloat(k);
                        this.skyRenderer.renderSkyDisc(f4, f5, f6);
                        MultiBufferSource.BufferSource multibuffersource$buffersource = this.renderBuffers.bufferSource();
                        if (dimensionspecialeffects.isSunriseOrSunset(f1)) {
                            this.skyRenderer.renderSunriseAndSunset(posestack, multibuffersource$buffersource, f, i);
                        }

                        this.skyRenderer.renderSunMoonAndStars(posestack, multibuffersource$buffersource, f1, j, f2, f3, pFog);
                        multibuffersource$buffersource.endBatch();
                        if (this.shouldRenderDarkDisc(pPartialTick)) {
                            this.skyRenderer.renderDarkDisc();
                        }
                    }
                });
            }
        }
    }

    private boolean shouldRenderDarkDisc(float pPartialTick) {
        return this.minecraft.player.getEyePosition(pPartialTick).y - this.level.getLevelData().getHorizonHeight(this.level) < 0.0;
    }

    private boolean doesMobEffectBlockSky(Camera pCamera) {
        return !(pCamera.getEntity() instanceof LivingEntity livingentity)
            ? false
            : livingentity.hasEffect(MobEffects.BLINDNESS) || livingentity.hasEffect(MobEffects.DARKNESS);
    }

    private void compileSections(Camera pCamera) {
        ProfilerFiller profilerfiller = Profiler.get();
        profilerfiller.push("populate_sections_to_compile");
        RenderRegionCache renderregioncache = new RenderRegionCache();
        BlockPos blockpos = pCamera.getBlockPosition();
        List<SectionRenderDispatcher.RenderSection> list = Lists.newArrayList();

        for (SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection : this.visibleSections) {
            if (sectionrenderdispatcher$rendersection.isDirty() && sectionrenderdispatcher$rendersection.hasAllNeighbors()) {
                boolean flag = false;
                if (this.minecraft.options.prioritizeChunkUpdates().get() == PrioritizeChunkUpdates.NEARBY) {
                    BlockPos blockpos1 = SectionPos.of(sectionrenderdispatcher$rendersection.getSectionNode()).center();
                    flag = blockpos1.distSqr(blockpos) < 768.0 || sectionrenderdispatcher$rendersection.isDirtyFromPlayer();
                } else if (this.minecraft.options.prioritizeChunkUpdates().get() == PrioritizeChunkUpdates.PLAYER_AFFECTED) {
                    flag = sectionrenderdispatcher$rendersection.isDirtyFromPlayer();
                }

                if (flag) {
                    profilerfiller.push("build_near_sync");
                    this.sectionRenderDispatcher.rebuildSectionSync(sectionrenderdispatcher$rendersection, renderregioncache);
                    sectionrenderdispatcher$rendersection.setNotDirty();
                    profilerfiller.pop();
                } else {
                    list.add(sectionrenderdispatcher$rendersection);
                }
            }
        }

        profilerfiller.popPush("upload");
        this.sectionRenderDispatcher.uploadAllPendingUploads();
        profilerfiller.popPush("schedule_async_compile");

        for (SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection1 : list) {
            sectionrenderdispatcher$rendersection1.rebuildSectionAsync(this.sectionRenderDispatcher, renderregioncache);
            sectionrenderdispatcher$rendersection1.setNotDirty();
        }

        profilerfiller.pop();
        this.scheduleTranslucentSectionResort(pCamera.getPosition());
    }

    private void renderHitOutline(
        PoseStack pPoseStack,
        VertexConsumer pBuffer,
        Entity pEntity,
        double pCamX,
        double pCamY,
        double pCamZ,
        BlockPos pPos,
        BlockState pState,
        int pColor
    ) {
        ShapeRenderer.renderShape(
            pPoseStack,
            pBuffer,
            pState.getShape(this.level, pPos, CollisionContext.of(pEntity)),
            pPos.getX() - pCamX,
            pPos.getY() - pCamY,
            pPos.getZ() - pCamZ,
            pColor
        );
    }

    public void blockChanged(BlockGetter pLevel, BlockPos pPos, BlockState pOldState, BlockState pNewState, int pFlags) {
        this.setBlockDirty(pPos, (pFlags & 8) != 0);
    }

    private void setBlockDirty(BlockPos pPos, boolean pReRenderOnMainThread) {
        for (int i = pPos.getZ() - 1; i <= pPos.getZ() + 1; i++) {
            for (int j = pPos.getX() - 1; j <= pPos.getX() + 1; j++) {
                for (int k = pPos.getY() - 1; k <= pPos.getY() + 1; k++) {
                    this.setSectionDirty(SectionPos.blockToSectionCoord(j), SectionPos.blockToSectionCoord(k), SectionPos.blockToSectionCoord(i), pReRenderOnMainThread);
                }
            }
        }
    }

    public void setBlocksDirty(int pMinX, int pMinY, int pMinZ, int pMaxX, int pMaxY, int pMaxZ) {
        for (int i = pMinZ - 1; i <= pMaxZ + 1; i++) {
            for (int j = pMinX - 1; j <= pMaxX + 1; j++) {
                for (int k = pMinY - 1; k <= pMaxY + 1; k++) {
                    this.setSectionDirty(SectionPos.blockToSectionCoord(j), SectionPos.blockToSectionCoord(k), SectionPos.blockToSectionCoord(i));
                }
            }
        }
    }

    public void setBlockDirty(BlockPos pPos, BlockState pOldState, BlockState pNewState) {
        if (this.minecraft.getModelManager().requiresRender(pOldState, pNewState)) {
            this.setBlocksDirty(
                pPos.getX(), pPos.getY(), pPos.getZ(), pPos.getX(), pPos.getY(), pPos.getZ()
            );
        }
    }

    public void setSectionDirtyWithNeighbors(int pSectionX, int pSectionY, int pSectionZ) {
        this.setSectionRangeDirty(pSectionX - 1, pSectionY - 1, pSectionZ - 1, pSectionX + 1, pSectionY + 1, pSectionZ + 1);
    }

    public void setSectionRangeDirty(int pMinY, int pMinX, int pMinZ, int pMaxY, int pMaxX, int pMaxZ) {
        for (int i = pMinZ; i <= pMaxZ; i++) {
            for (int j = pMinY; j <= pMaxY; j++) {
                for (int k = pMinX; k <= pMaxX; k++) {
                    this.setSectionDirty(j, k, i);
                }
            }
        }
    }

    public void setSectionDirty(int pSectionX, int pSectionY, int pSectionZ) {
        this.setSectionDirty(pSectionX, pSectionY, pSectionZ, false);
    }

    private void setSectionDirty(int pSectionX, int pSectionY, int pSectionZ, boolean pReRenderOnMainThread) {
        this.viewArea.setDirty(pSectionX, pSectionY, pSectionZ, pReRenderOnMainThread);
    }

    public void onSectionBecomingNonEmpty(long pSectionPos) {
        SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection = this.viewArea.getRenderSection(pSectionPos);
        if (sectionrenderdispatcher$rendersection != null) {
            this.sectionOcclusionGraph.schedulePropagationFrom(sectionrenderdispatcher$rendersection);
        }
    }

    public void addParticle(
        ParticleOptions pOptions,
        boolean pForce,
        double pX,
        double pY,
        double pZ,
        double pXSpeed,
        double pYSpeed,
        double pZSpeed
    ) {
        this.addParticle(pOptions, pForce, false, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed);
    }

    public void addParticle(
        ParticleOptions pOptions,
        boolean pForce,
        boolean pDecreased,
        double pX,
        double pY,
        double pZ,
        double pXSpeed,
        double pYSpeed,
        double pZSpeed
    ) {
        try {
            this.addParticleInternal(pOptions, pForce, pDecreased, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed);
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Exception while adding particle");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Particle being added");
            crashreportcategory.setDetail("ID", BuiltInRegistries.PARTICLE_TYPE.getKey(pOptions.getType()));
            crashreportcategory.setDetail(
                "Parameters", () -> ParticleTypes.CODEC.encodeStart(this.level.registryAccess().createSerializationContext(NbtOps.INSTANCE), pOptions).toString()
            );
            crashreportcategory.setDetail("Position", () -> CrashReportCategory.formatLocation(this.level, pX, pY, pZ));
            throw new ReportedException(crashreport);
        }
    }

    public <T extends ParticleOptions> void addParticle(
        T pOptions, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed
    ) {
        this.addParticle(pOptions, pOptions.getType().getOverrideLimiter(), pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed);
    }

    @Nullable
    Particle addParticleInternal(
        ParticleOptions pOptions,
        boolean pForce,
        double pX,
        double pY,
        double pZ,
        double pXSpeed,
        double pYSpeed,
        double pZSpeed
    ) {
        return this.addParticleInternal(pOptions, pForce, false, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed);
    }

    @Nullable
    private Particle addParticleInternal(
        ParticleOptions pOptions,
        boolean pForce,
        boolean pDecreased,
        double pX,
        double pY,
        double pZ,
        double pXSpeed,
        double pYSpeed,
        double pZSpeed
    ) {
        Camera camera = this.minecraft.gameRenderer.getMainCamera();
        ParticleStatus particlestatus = this.calculateParticleLevel(pDecreased);
        if (pForce) {
            return this.minecraft.particleEngine.createParticle(pOptions, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed);
        } else if (camera.getPosition().distanceToSqr(pX, pY, pZ) > 1024.0) {
            return null;
        } else {
            return particlestatus == ParticleStatus.MINIMAL
                ? null
                : this.minecraft.particleEngine.createParticle(pOptions, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed);
        }
    }

    private ParticleStatus calculateParticleLevel(boolean pDecreased) {
        ParticleStatus particlestatus = this.minecraft.options.particles().get();
        if (pDecreased && particlestatus == ParticleStatus.MINIMAL && this.level.random.nextInt(10) == 0) {
            particlestatus = ParticleStatus.DECREASED;
        }

        if (particlestatus == ParticleStatus.DECREASED && this.level.random.nextInt(3) == 0) {
            particlestatus = ParticleStatus.MINIMAL;
        }

        return particlestatus;
    }

    public void destroyBlockProgress(int pBreakerId, BlockPos pPos, int pProgress) {
        if (pProgress >= 0 && pProgress < 10) {
            BlockDestructionProgress blockdestructionprogress1 = this.destroyingBlocks.get(pBreakerId);
            if (blockdestructionprogress1 != null) {
                this.removeProgress(blockdestructionprogress1);
            }

            if (blockdestructionprogress1 == null
                || blockdestructionprogress1.getPos().getX() != pPos.getX()
                || blockdestructionprogress1.getPos().getY() != pPos.getY()
                || blockdestructionprogress1.getPos().getZ() != pPos.getZ()) {
                blockdestructionprogress1 = new BlockDestructionProgress(pBreakerId, pPos);
                this.destroyingBlocks.put(pBreakerId, blockdestructionprogress1);
            }

            blockdestructionprogress1.setProgress(pProgress);
            blockdestructionprogress1.updateTick(this.ticks);
            this.destructionProgress.computeIfAbsent(blockdestructionprogress1.getPos().asLong(), p_234254_ -> Sets.newTreeSet()).add(blockdestructionprogress1);
        } else {
            BlockDestructionProgress blockdestructionprogress = this.destroyingBlocks.remove(pBreakerId);
            if (blockdestructionprogress != null) {
                this.removeProgress(blockdestructionprogress);
            }
        }
    }

    public boolean hasRenderedAllSections() {
        return this.sectionRenderDispatcher.isQueueEmpty();
    }

    public void onChunkReadyToRender(ChunkPos pChunkPos) {
        this.sectionOcclusionGraph.onChunkReadyToRender(pChunkPos);
    }

    public void needsUpdate() {
        this.sectionOcclusionGraph.invalidate();
        this.cloudRenderer.markForRebuild();
    }

    public void updateGlobalBlockEntities(Collection<BlockEntity> pBlockEntitiesToRemove, Collection<BlockEntity> pBlockEntitiesToAdd) {
        synchronized (this.globalBlockEntities) {
            this.globalBlockEntities.removeAll(pBlockEntitiesToRemove);
            this.globalBlockEntities.addAll(pBlockEntitiesToAdd);
        }
    }

    public static int getLightColor(BlockAndTintGetter pLevel, BlockPos pPos) {
        return getLightColor(LevelRenderer.BrightnessGetter.DEFAULT, pLevel, pLevel.getBlockState(pPos), pPos);
    }

    public static int getLightColor(LevelRenderer.BrightnessGetter pBrightnessGetter, BlockAndTintGetter pLevel, BlockState pState, BlockPos pPos) {
        if (pState.emissiveRendering(pLevel, pPos)) {
            return 15728880;
        } else {
            int i = pBrightnessGetter.packedBrightness(pLevel, pPos);
            int j = LightTexture.block(i);
            int k = pState.getLightEmission(pLevel, pPos);
            if (j < k) {
                int l = LightTexture.sky(i);
                return LightTexture.pack(k, l);
            } else {
                return i;
            }
        }
    }

    public boolean isSectionCompiled(BlockPos pPos) {
        SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection = this.viewArea.getRenderSectionAt(pPos);
        return sectionrenderdispatcher$rendersection != null
            && sectionrenderdispatcher$rendersection.compiled.get() != SectionRenderDispatcher.CompiledSection.UNCOMPILED;
    }

    @Nullable
    public RenderTarget entityOutlineTarget() {
        return this.targets.entityOutline != null ? this.targets.entityOutline.get() : null;
    }

    @Nullable
    public RenderTarget getTranslucentTarget() {
        return this.targets.translucent != null ? this.targets.translucent.get() : null;
    }

    @Nullable
    public RenderTarget getItemEntityTarget() {
        return this.targets.itemEntity != null ? this.targets.itemEntity.get() : null;
    }

    @Nullable
    public RenderTarget getParticlesTarget() {
        return this.targets.particles != null ? this.targets.particles.get() : null;
    }

    @Nullable
    public RenderTarget getWeatherTarget() {
        return this.targets.weather != null ? this.targets.weather.get() : null;
    }

    @Nullable
    public RenderTarget getCloudsTarget() {
        return this.targets.clouds != null ? this.targets.clouds.get() : null;
    }

    @VisibleForDebug
    public ObjectArrayList<SectionRenderDispatcher.RenderSection> getVisibleSections() {
        return this.visibleSections;
    }

    @VisibleForDebug
    public SectionOcclusionGraph getSectionOcclusionGraph() {
        return this.sectionOcclusionGraph;
    }

    @Nullable
    public Frustum getCapturedFrustum() {
        return this.capturedFrustum;
    }

    public CloudRenderer getCloudRenderer() {
        return this.cloudRenderer;
    }

    public Frustum getFrustum() {
        return this.capturedFrustum != null ? this.capturedFrustum : this.cullingFrustum;
    }

    public int getTicks() {
        return this.ticks;
    }

    public WeatherEffectRenderer getWeatherEffects() {
        return this.weatherEffectRenderer;
    }

    public void setWeatherEffects(WeatherEffectRenderer value) {
        this.weatherEffectRenderer = value;
    }

    @FunctionalInterface
    @OnlyIn(Dist.CLIENT)
    public interface BrightnessGetter {
        LevelRenderer.BrightnessGetter DEFAULT = (p_398214_, p_398219_) -> {
            int i = p_398214_.getBrightness(LightLayer.SKY, p_398219_);
            int j = p_398214_.getBrightness(LightLayer.BLOCK, p_398219_);
            return Brightness.pack(j, i);
        };

        int packedBrightness(BlockAndTintGetter pLevel, BlockPos pPos);
    }
}
