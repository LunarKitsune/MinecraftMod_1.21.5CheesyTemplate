package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BeaconBeamOwner;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BeaconRenderer<T extends BlockEntity & BeaconBeamOwner> implements BlockEntityRenderer<T> {
    public static final ResourceLocation BEAM_LOCATION = ResourceLocation.withDefaultNamespace("textures/entity/beacon_beam.png");
    public static final int MAX_RENDER_Y = 2048;
    private static final float BEAM_SCALE_THRESHOLD = 96.0F;
    public static final float SOLID_BEAM_RADIUS = 0.2F;
    public static final float BEAM_GLOW_RADIUS = 0.25F;

    public BeaconRenderer(BlockEntityRendererProvider.Context pContext) {
    }

    @Override
    public void render(T p_395792_, float p_112141_, PoseStack p_112142_, MultiBufferSource p_112143_, int p_112144_, int p_112145_, Vec3 p_396466_) {
        long i = p_395792_.getLevel().getGameTime();
        float f = (float)p_396466_.subtract(p_395792_.getBlockPos().getCenter()).horizontalDistance();
        LocalPlayer localplayer = Minecraft.getInstance().player;
        float f1 = localplayer != null && localplayer.isScoping() ? 1.0F : Math.max(1.0F, f / 96.0F);
        List<BeaconBeamOwner.Section> list = p_395792_.getBeamSections();
        int j = 0;

        for (int k = 0; k < list.size(); k++) {
            BeaconBeamOwner.Section beaconbeamowner$section = list.get(k);
            renderBeaconBeam(
                p_112142_,
                p_112143_,
                p_112141_,
                f1,
                i,
                j,
                k == list.size() - 1 ? 2048 : beaconbeamowner$section.getHeight(),
                beaconbeamowner$section.getColor()
            );
            j += beaconbeamowner$section.getHeight();
        }
    }

    private static void renderBeaconBeam(
        PoseStack pPoseStack, MultiBufferSource pBufferSource, float pPartialTick, float pRadius, long pGameTime, int pYOffset, int pHeight, int pColor
    ) {
        renderBeaconBeam(pPoseStack, pBufferSource, BEAM_LOCATION, pPartialTick, 1.0F, pGameTime, pYOffset, pHeight, pColor, 0.2F * pRadius, 0.25F * pRadius);
    }

    public static void renderBeaconBeam(
        PoseStack pPoseStack,
        MultiBufferSource pBufferSource,
        ResourceLocation pBeamLocation,
        float pPartialTick,
        float pTextureScale,
        long pGameTime,
        int pYOffset,
        int pHeight,
        int pColor,
        float pBeamRadius,
        float pGlowRadius
    ) {
        int i = pYOffset + pHeight;
        pPoseStack.pushPose();
        pPoseStack.translate(0.5, 0.0, 0.5);
        float f = Math.floorMod(pGameTime, 40) + pPartialTick;
        float f1 = pHeight < 0 ? f : -f;
        float f2 = Mth.frac(f1 * 0.2F - Mth.floor(f1 * 0.1F));
        pPoseStack.pushPose();
        pPoseStack.mulPose(Axis.YP.rotationDegrees(f * 2.25F - 45.0F));
        float f3 = 0.0F;
        float f5 = 0.0F;
        float f6 = -pBeamRadius;
        float f7 = 0.0F;
        float f8 = 0.0F;
        float f9 = -pBeamRadius;
        float f10 = 0.0F;
        float f11 = 1.0F;
        float f12 = -1.0F + f2;
        float f13 = pHeight * pTextureScale * (0.5F / pBeamRadius) + f12;
        renderPart(
            pPoseStack,
            pBufferSource.getBuffer(RenderType.beaconBeam(pBeamLocation, false)),
            pColor,
            pYOffset,
            i,
            0.0F,
            pBeamRadius,
            pBeamRadius,
            0.0F,
            f6,
            0.0F,
            0.0F,
            f9,
            0.0F,
            1.0F,
            f13,
            f12
        );
        pPoseStack.popPose();
        f3 = -pGlowRadius;
        float f4 = -pGlowRadius;
        f5 = -pGlowRadius;
        f6 = -pGlowRadius;
        f10 = 0.0F;
        f11 = 1.0F;
        f12 = -1.0F + f2;
        f13 = pHeight * pTextureScale + f12;
        renderPart(
            pPoseStack,
            pBufferSource.getBuffer(RenderType.beaconBeam(pBeamLocation, true)),
            ARGB.color(32, pColor),
            pYOffset,
            i,
            f3,
            f4,
            pGlowRadius,
            f5,
            f6,
            pGlowRadius,
            pGlowRadius,
            pGlowRadius,
            0.0F,
            1.0F,
            f13,
            f12
        );
        pPoseStack.popPose();
    }

    private static void renderPart(
        PoseStack pPoseStack,
        VertexConsumer pConsumer,
        int pColor,
        int pMinY,
        int pMaxY,
        float pX1,
        float pZ1,
        float pX2,
        float pZ2,
        float pX3,
        float pZ3,
        float pX4,
        float pZ4,
        float pMinU,
        float pMaxU,
        float pMinV,
        float pMaxV
    ) {
        PoseStack.Pose posestack$pose = pPoseStack.last();
        renderQuad(
            posestack$pose, pConsumer, pColor, pMinY, pMaxY, pX1, pZ1, pX2, pZ2, pMinU, pMaxU, pMinV, pMaxV
        );
        renderQuad(
            posestack$pose, pConsumer, pColor, pMinY, pMaxY, pX4, pZ4, pX3, pZ3, pMinU, pMaxU, pMinV, pMaxV
        );
        renderQuad(
            posestack$pose, pConsumer, pColor, pMinY, pMaxY, pX2, pZ2, pX4, pZ4, pMinU, pMaxU, pMinV, pMaxV
        );
        renderQuad(
            posestack$pose, pConsumer, pColor, pMinY, pMaxY, pX3, pZ3, pX1, pZ1, pMinU, pMaxU, pMinV, pMaxV
        );
    }

    private static void renderQuad(
        PoseStack.Pose pPose,
        VertexConsumer pConsumer,
        int pColor,
        int pMinY,
        int pMaxY,
        float pMinX,
        float pMinZ,
        float pMaxX,
        float pMaxZ,
        float pMinU,
        float pMaxU,
        float pMinV,
        float pMaxV
    ) {
        addVertex(pPose, pConsumer, pColor, pMaxY, pMinX, pMinZ, pMaxU, pMinV);
        addVertex(pPose, pConsumer, pColor, pMinY, pMinX, pMinZ, pMaxU, pMaxV);
        addVertex(pPose, pConsumer, pColor, pMinY, pMaxX, pMaxZ, pMinU, pMaxV);
        addVertex(pPose, pConsumer, pColor, pMaxY, pMaxX, pMaxZ, pMinU, pMinV);
    }

    private static void addVertex(
        PoseStack.Pose pPose, VertexConsumer pConsumer, int pColor, int pY, float pX, float pZ, float pU, float pV
    ) {
        pConsumer.addVertex(pPose, pX, pY, pZ)
            .setColor(pColor)
            .setUv(pU, pV)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(15728880)
            .setNormal(pPose, 0.0F, 1.0F, 0.0F);
    }

    @Override
    public boolean shouldRenderOffScreen(T p_112147_) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return Minecraft.getInstance().options.getEffectiveRenderDistance() * 16;
    }

    @Override
    public boolean shouldRender(T p_173534_, Vec3 p_173535_) {
        return Vec3.atCenterOf(p_173534_.getBlockPos()).multiply(1.0, 0.0, 1.0).closerThan(p_173535_.multiply(1.0, 0.0, 1.0), this.getViewDistance());
    }
}