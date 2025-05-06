package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.BannerFlagModel;
import net.minecraft.client.model.BannerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BannerRenderer implements BlockEntityRenderer<BannerBlockEntity> {
    private static final int MAX_PATTERNS = 16;
    private static final float SIZE = 0.6666667F;
    private final BannerModel standingModel;
    private final BannerModel wallModel;
    private final BannerFlagModel standingFlagModel;
    private final BannerFlagModel wallFlagModel;

    public BannerRenderer(BlockEntityRendererProvider.Context pContext) {
        this(pContext.getModelSet());
    }

    public BannerRenderer(EntityModelSet pModelSet) {
        this.standingModel = new BannerModel(pModelSet.bakeLayer(ModelLayers.STANDING_BANNER));
        this.wallModel = new BannerModel(pModelSet.bakeLayer(ModelLayers.WALL_BANNER));
        this.standingFlagModel = new BannerFlagModel(pModelSet.bakeLayer(ModelLayers.STANDING_BANNER_FLAG));
        this.wallFlagModel = new BannerFlagModel(pModelSet.bakeLayer(ModelLayers.WALL_BANNER_FLAG));
    }

    public void render(
        BannerBlockEntity p_112052_, float p_112053_, PoseStack p_112054_, MultiBufferSource p_112055_, int p_112056_, int p_112057_, Vec3 p_395980_
    ) {
        BlockState blockstate = p_112052_.getBlockState();
        BannerModel bannermodel;
        BannerFlagModel bannerflagmodel;
        float f;
        if (blockstate.getBlock() instanceof BannerBlock) {
            f = -RotationSegment.convertToDegrees(blockstate.getValue(BannerBlock.ROTATION));
            bannermodel = this.standingModel;
            bannerflagmodel = this.standingFlagModel;
        } else {
            f = -blockstate.getValue(WallBannerBlock.FACING).toYRot();
            bannermodel = this.wallModel;
            bannerflagmodel = this.wallFlagModel;
        }

        long i = p_112052_.getLevel().getGameTime();
        BlockPos blockpos = p_112052_.getBlockPos();
        float f1 = ((float)Math.floorMod(blockpos.getX() * 7 + blockpos.getY() * 9 + blockpos.getZ() * 13 + i, 100L) + p_112053_) / 100.0F;
        renderBanner(p_112054_, p_112055_, p_112056_, p_112057_, f, bannermodel, bannerflagmodel, f1, p_112052_.getBaseColor(), p_112052_.getPatterns());
    }

    public void renderInHand(PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, int pPackedOverlay, DyeColor pBaseColor, BannerPatternLayers pPatterns) {
        renderBanner(pPoseStack, pBufferSource, pPackedLight, pPackedOverlay, 0.0F, this.standingModel, this.standingFlagModel, 0.0F, pBaseColor, pPatterns);
    }

    private static void renderBanner(
        PoseStack pPoseStack,
        MultiBufferSource pBufferSource,
        int pPackedLight,
        int pPackedOverlay,
        float pRotation,
        BannerModel pStandingModel,
        BannerFlagModel pStandingFlagModel,
        float pAngle,
        DyeColor pBaseColor,
        BannerPatternLayers pPatterns
    ) {
        pPoseStack.pushPose();
        pPoseStack.translate(0.5F, 0.0F, 0.5F);
        pPoseStack.mulPose(Axis.YP.rotationDegrees(pRotation));
        pPoseStack.scale(0.6666667F, -0.6666667F, -0.6666667F);
        pStandingModel.renderToBuffer(pPoseStack, ModelBakery.BANNER_BASE.buffer(pBufferSource, RenderType::entitySolid), pPackedLight, pPackedOverlay);
        pStandingFlagModel.setupAnim(pAngle);
        renderPatterns(pPoseStack, pBufferSource, pPackedLight, pPackedOverlay, pStandingFlagModel.root(), ModelBakery.BANNER_BASE, true, pBaseColor, pPatterns);
        pPoseStack.popPose();
    }

    public static void renderPatterns(
        PoseStack pPoseStack,
        MultiBufferSource pBufferSource,
        int pPackedLight,
        int pPackedOverlay,
        ModelPart pFlagPart,
        Material pFlagMaterial,
        boolean pBanner,
        DyeColor pBaseColor,
        BannerPatternLayers pPatterns
    ) {
        renderPatterns(pPoseStack, pBufferSource, pPackedLight, pPackedOverlay, pFlagPart, pFlagMaterial, pBanner, pBaseColor, pPatterns, false, true);
    }

    public static void renderPatterns(
        PoseStack pPoseStack,
        MultiBufferSource pBufferSource,
        int pPackedLight,
        int pPackedOverlay,
        ModelPart pFlagPart,
        Material pFlagMaterial,
        boolean pBanner,
        DyeColor pBaseColor,
        BannerPatternLayers pPatterns,
        boolean pWithGlint,
        boolean pNoEntity
    ) {
        pFlagPart.render(pPoseStack, pFlagMaterial.buffer(pBufferSource, RenderType::entitySolid, pNoEntity, pWithGlint), pPackedLight, pPackedOverlay);
        renderPatternLayer(pPoseStack, pBufferSource, pPackedLight, pPackedOverlay, pFlagPart, pBanner ? Sheets.BANNER_BASE : Sheets.SHIELD_BASE, pBaseColor);

        for (int i = 0; i < 16 && i < pPatterns.layers().size(); i++) {
            BannerPatternLayers.Layer bannerpatternlayers$layer = pPatterns.layers().get(i);
            Material material = pBanner ? Sheets.getBannerMaterial(bannerpatternlayers$layer.pattern()) : Sheets.getShieldMaterial(bannerpatternlayers$layer.pattern());
            renderPatternLayer(pPoseStack, pBufferSource, pPackedLight, pPackedOverlay, pFlagPart, material, bannerpatternlayers$layer.color());
        }
    }

    private static void renderPatternLayer(
        PoseStack pPoseStack, MultiBufferSource pBuffer, int pPackedLight, int pPackedOverlay, ModelPart pFlagPart, Material pMaterial, DyeColor pColor
    ) {
        int i = pColor.getTextureDiffuseColor();
        pFlagPart.render(pPoseStack, pMaterial.buffer(pBuffer, RenderType::entityNoOutline), pPackedLight, pPackedOverlay, i);
    }
}