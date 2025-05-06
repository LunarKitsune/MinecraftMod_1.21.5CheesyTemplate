package net.minecraft.client.renderer.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DebugRenderer {
    public final PathfindingRenderer pathfindingRenderer = new PathfindingRenderer();
    public final DebugRenderer.SimpleDebugRenderer waterDebugRenderer;
    public final DebugRenderer.SimpleDebugRenderer chunkBorderRenderer;
    public final DebugRenderer.SimpleDebugRenderer heightMapRenderer;
    public final DebugRenderer.SimpleDebugRenderer collisionBoxRenderer;
    public final DebugRenderer.SimpleDebugRenderer supportBlockRenderer;
    public final NeighborsUpdateRenderer neighborsUpdateRenderer;
    public final RedstoneWireOrientationsRenderer redstoneWireOrientationsRenderer;
    public final StructureRenderer structureRenderer;
    public final DebugRenderer.SimpleDebugRenderer lightDebugRenderer;
    public final DebugRenderer.SimpleDebugRenderer worldGenAttemptRenderer;
    public final DebugRenderer.SimpleDebugRenderer solidFaceRenderer;
    public final DebugRenderer.SimpleDebugRenderer chunkRenderer;
    public final BrainDebugRenderer brainDebugRenderer;
    public final VillageSectionsDebugRenderer villageSectionsDebugRenderer;
    public final BeeDebugRenderer beeDebugRenderer;
    public final RaidDebugRenderer raidDebugRenderer;
    public final GoalSelectorDebugRenderer goalSelectorRenderer;
    public final GameTestDebugRenderer gameTestDebugRenderer;
    public final GameEventListenerRenderer gameEventListenerRenderer;
    public final LightSectionDebugRenderer skyLightSectionDebugRenderer;
    public final BreezeDebugRenderer breezeDebugRenderer;
    public final ChunkCullingDebugRenderer chunkCullingDebugRenderer;
    public final OctreeDebugRenderer octreeDebugRenderer;
    private boolean renderChunkborder;
    private boolean renderOctree;

    public DebugRenderer(Minecraft pMinecraft) {
        this.waterDebugRenderer = new WaterDebugRenderer(pMinecraft);
        this.chunkBorderRenderer = new ChunkBorderRenderer(pMinecraft);
        this.heightMapRenderer = new HeightMapRenderer(pMinecraft);
        this.collisionBoxRenderer = new CollisionBoxRenderer(pMinecraft);
        this.supportBlockRenderer = new SupportBlockRenderer(pMinecraft);
        this.neighborsUpdateRenderer = new NeighborsUpdateRenderer(pMinecraft);
        this.redstoneWireOrientationsRenderer = new RedstoneWireOrientationsRenderer(pMinecraft);
        this.structureRenderer = new StructureRenderer(pMinecraft);
        this.lightDebugRenderer = new LightDebugRenderer(pMinecraft);
        this.worldGenAttemptRenderer = new WorldGenAttemptRenderer();
        this.solidFaceRenderer = new SolidFaceRenderer(pMinecraft);
        this.chunkRenderer = new ChunkDebugRenderer(pMinecraft);
        this.brainDebugRenderer = new BrainDebugRenderer(pMinecraft);
        this.villageSectionsDebugRenderer = new VillageSectionsDebugRenderer();
        this.beeDebugRenderer = new BeeDebugRenderer(pMinecraft);
        this.raidDebugRenderer = new RaidDebugRenderer(pMinecraft);
        this.goalSelectorRenderer = new GoalSelectorDebugRenderer(pMinecraft);
        this.gameTestDebugRenderer = new GameTestDebugRenderer();
        this.gameEventListenerRenderer = new GameEventListenerRenderer(pMinecraft);
        this.skyLightSectionDebugRenderer = new LightSectionDebugRenderer(pMinecraft, LightLayer.SKY);
        this.breezeDebugRenderer = new BreezeDebugRenderer(pMinecraft);
        this.chunkCullingDebugRenderer = new ChunkCullingDebugRenderer(pMinecraft);
        this.octreeDebugRenderer = new OctreeDebugRenderer(pMinecraft);
    }

    public void clear() {
        this.pathfindingRenderer.clear();
        this.waterDebugRenderer.clear();
        this.chunkBorderRenderer.clear();
        this.heightMapRenderer.clear();
        this.collisionBoxRenderer.clear();
        this.supportBlockRenderer.clear();
        this.neighborsUpdateRenderer.clear();
        this.structureRenderer.clear();
        this.lightDebugRenderer.clear();
        this.worldGenAttemptRenderer.clear();
        this.solidFaceRenderer.clear();
        this.chunkRenderer.clear();
        this.brainDebugRenderer.clear();
        this.villageSectionsDebugRenderer.clear();
        this.beeDebugRenderer.clear();
        this.raidDebugRenderer.clear();
        this.goalSelectorRenderer.clear();
        this.gameTestDebugRenderer.clear();
        this.gameEventListenerRenderer.clear();
        this.skyLightSectionDebugRenderer.clear();
        this.breezeDebugRenderer.clear();
        this.chunkCullingDebugRenderer.clear();
    }

    public boolean switchRenderChunkborder() {
        this.renderChunkborder = !this.renderChunkborder;
        return this.renderChunkborder;
    }

    public boolean toggleRenderOctree() {
        return this.renderOctree = !this.renderOctree;
    }

    public void render(
        PoseStack pPoseStack, Frustum pFrustum, MultiBufferSource.BufferSource pBufferSource, double pCamX, double pCamY, double pCamZ
    ) {
        if (this.renderChunkborder && !Minecraft.getInstance().showOnlyReducedInfo()) {
            this.chunkBorderRenderer.render(pPoseStack, pBufferSource, pCamX, pCamY, pCamZ);
        }

        if (this.renderOctree) {
            this.octreeDebugRenderer.render(pPoseStack, pFrustum, pBufferSource, pCamX, pCamY, pCamZ);
        }

        this.gameTestDebugRenderer.render(pPoseStack, pBufferSource, pCamX, pCamY, pCamZ);
    }

    public void renderAfterTranslucents(PoseStack pPoseStack, MultiBufferSource.BufferSource pBufferSource, double pCamX, double pCamY, double pCamZ) {
        this.chunkCullingDebugRenderer.render(pPoseStack, pBufferSource, pCamX, pCamY, pCamZ);
    }

    public static Optional<Entity> getTargetedEntity(@Nullable Entity pEntity, int pDistance) {
        if (pEntity == null) {
            return Optional.empty();
        } else {
            Vec3 vec3 = pEntity.getEyePosition();
            Vec3 vec31 = pEntity.getViewVector(1.0F).scale(pDistance);
            Vec3 vec32 = vec3.add(vec31);
            AABB aabb = pEntity.getBoundingBox().expandTowards(vec31).inflate(1.0);
            int i = pDistance * pDistance;
            EntityHitResult entityhitresult = ProjectileUtil.getEntityHitResult(pEntity, vec3, vec32, aabb, EntitySelector.CAN_BE_PICKED, i);
            if (entityhitresult == null) {
                return Optional.empty();
            } else {
                return vec3.distanceToSqr(entityhitresult.getLocation()) > i ? Optional.empty() : Optional.of(entityhitresult.getEntity());
            }
        }
    }

    public static void renderFilledUnitCube(
        PoseStack pPoseStack, MultiBufferSource pBufferSource, BlockPos pPos, float pRed, float pGreen, float pBlue, float pAlpha
    ) {
        renderFilledBox(pPoseStack, pBufferSource, pPos, pPos.offset(1, 1, 1), pRed, pGreen, pBlue, pAlpha);
    }

    public static void renderFilledBox(
        PoseStack pPoseStack,
        MultiBufferSource pBufferSource,
        BlockPos pStartPos,
        BlockPos pEndPos,
        float pRed,
        float pGreen,
        float pBlue,
        float pAlpha
    ) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        if (camera.isInitialized()) {
            Vec3 vec3 = camera.getPosition().reverse();
            AABB aabb = AABB.encapsulatingFullBlocks(pStartPos, pEndPos).move(vec3);
            renderFilledBox(pPoseStack, pBufferSource, aabb, pRed, pGreen, pBlue, pAlpha);
        }
    }

    public static void renderFilledBox(
        PoseStack pPoseStack,
        MultiBufferSource pBufferSource,
        BlockPos pPos,
        float pScale,
        float pRed,
        float pGreen,
        float pBlue,
        float pAlpha
    ) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        if (camera.isInitialized()) {
            Vec3 vec3 = camera.getPosition().reverse();
            AABB aabb = new AABB(pPos).move(vec3).inflate(pScale);
            renderFilledBox(pPoseStack, pBufferSource, aabb, pRed, pGreen, pBlue, pAlpha);
        }
    }

    public static void renderFilledBox(
        PoseStack pPoseStack, MultiBufferSource pBufferSource, AABB pBoundingBox, float pRed, float pGreen, float pBlue, float pAlpha
    ) {
        renderFilledBox(
            pPoseStack,
            pBufferSource,
            pBoundingBox.minX,
            pBoundingBox.minY,
            pBoundingBox.minZ,
            pBoundingBox.maxX,
            pBoundingBox.maxY,
            pBoundingBox.maxZ,
            pRed,
            pGreen,
            pBlue,
            pAlpha
        );
    }

    public static void renderFilledBox(
        PoseStack pPoseStack,
        MultiBufferSource pBufferSource,
        double pMinX,
        double pMinY,
        double pMinZ,
        double pMaxX,
        double pMaxY,
        double pMaxZ,
        float pRed,
        float pGreen,
        float pBlue,
        float pAlpha
    ) {
        VertexConsumer vertexconsumer = pBufferSource.getBuffer(RenderType.debugFilledBox());
        ShapeRenderer.addChainedFilledBoxVertices(
            pPoseStack, vertexconsumer, pMinX, pMinY, pMinZ, pMaxX, pMaxY, pMaxZ, pRed, pGreen, pBlue, pAlpha
        );
    }

    public static void renderFloatingText(PoseStack pPoseStack, MultiBufferSource pBufferSource, String pText, int pX, int pY, int pZ, int pColor) {
        renderFloatingText(pPoseStack, pBufferSource, pText, pX + 0.5, pY + 0.5, pZ + 0.5, pColor);
    }

    public static void renderFloatingText(
        PoseStack pPoseStack, MultiBufferSource pBufferSource, String pText, double pX, double pY, double pZ, int pColor
    ) {
        renderFloatingText(pPoseStack, pBufferSource, pText, pX, pY, pZ, pColor, 0.02F);
    }

    public static void renderFloatingText(
        PoseStack pPoseStack,
        MultiBufferSource pBufferSource,
        String pText,
        double pX,
        double pY,
        double pZ,
        int pColor,
        float pScale
    ) {
        renderFloatingText(pPoseStack, pBufferSource, pText, pX, pY, pZ, pColor, pScale, true, 0.0F, false);
    }

    public static void renderFloatingText(
        PoseStack pPoseStack,
        MultiBufferSource pBufferSource,
        String pText,
        double pX,
        double pY,
        double pZ,
        int pColor,
        float pScale,
        boolean pCenter,
        float pXOffset,
        boolean pTransparent
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        Camera camera = minecraft.gameRenderer.getMainCamera();
        if (camera.isInitialized() && minecraft.getEntityRenderDispatcher().options != null) {
            Font font = minecraft.font;
            double d0 = camera.getPosition().x;
            double d1 = camera.getPosition().y;
            double d2 = camera.getPosition().z;
            pPoseStack.pushPose();
            pPoseStack.translate((float)(pX - d0), (float)(pY - d1) + 0.07F, (float)(pZ - d2));
            pPoseStack.mulPose(camera.rotation());
            pPoseStack.scale(pScale, -pScale, pScale);
            float f = pCenter ? -font.width(pText) / 2.0F : 0.0F;
            f -= pXOffset / pScale;
            font.drawInBatch(
                pText,
                f,
                0.0F,
                pColor,
                false,
                pPoseStack.last().pose(),
                pBufferSource,
                pTransparent ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.NORMAL,
                0,
                15728880
            );
            pPoseStack.popPose();
        }
    }

    private static Vec3 mixColor(float pShift) {
        float f = 5.99999F;
        int i = (int)(Mth.clamp(pShift, 0.0F, 1.0F) * 5.99999F);
        float f1 = pShift * 5.99999F - i;

        return switch (i) {
            case 0 -> new Vec3(1.0, f1, 0.0);
            case 1 -> new Vec3(1.0F - f1, 1.0, 0.0);
            case 2 -> new Vec3(0.0, 1.0, f1);
            case 3 -> new Vec3(0.0, 1.0 - f1, 1.0);
            case 4 -> new Vec3(f1, 0.0, 1.0);
            case 5 -> new Vec3(1.0, 0.0, 1.0 - f1);
            default -> throw new IllegalStateException("Unexpected value: " + i);
        };
    }

    private static Vec3 shiftHue(float pRed, float pGreen, float pBlue, float pShift) {
        Vec3 vec3 = mixColor(pShift).scale(pRed);
        Vec3 vec31 = mixColor((pShift + 0.33333334F) % 1.0F).scale(pGreen);
        Vec3 vec32 = mixColor((pShift + 0.6666667F) % 1.0F).scale(pBlue);
        Vec3 vec33 = vec3.add(vec31).add(vec32);
        double d0 = Math.max(Math.max(1.0, vec33.x), Math.max(vec33.y, vec33.z));
        return new Vec3(vec33.x / d0, vec33.y / d0, vec33.z / d0);
    }

    public static void renderVoxelShape(
        PoseStack pPoseStack,
        VertexConsumer pBuffer,
        VoxelShape pShape,
        double pX,
        double pY,
        double pZ,
        float pRed,
        float pGreen,
        float pBlue,
        float pAlpha,
        boolean pLowerColorVariance
    ) {
        List<AABB> list = pShape.toAabbs();
        if (!list.isEmpty()) {
            int i = pLowerColorVariance ? list.size() : list.size() * 8;
            ShapeRenderer.renderShape(
                pPoseStack, pBuffer, Shapes.create(list.get(0)), pX, pY, pZ, ARGB.colorFromFloat(pAlpha, pRed, pGreen, pBlue)
            );

            for (int j = 1; j < list.size(); j++) {
                AABB aabb = list.get(j);
                float f = (float)j / i;
                Vec3 vec3 = shiftHue(pRed, pGreen, pBlue, f);
                ShapeRenderer.renderShape(
                    pPoseStack,
                    pBuffer,
                    Shapes.create(aabb),
                    pX,
                    pY,
                    pZ,
                    ARGB.colorFromFloat(pAlpha, (float)vec3.x, (float)vec3.y, (float)vec3.z)
                );
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public interface SimpleDebugRenderer {
        void render(PoseStack pPoseStack, MultiBufferSource pBufferSource, double pCamX, double pCamY, double pCamZ);

        default void clear() {
        }
    }
}