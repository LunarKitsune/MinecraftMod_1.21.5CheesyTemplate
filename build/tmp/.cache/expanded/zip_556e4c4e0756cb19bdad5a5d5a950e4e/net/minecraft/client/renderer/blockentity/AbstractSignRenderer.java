package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractSignRenderer implements BlockEntityRenderer<SignBlockEntity> {
    private static final int BLACK_TEXT_OUTLINE_COLOR = -988212;
    private static final int OUTLINE_RENDER_DISTANCE = Mth.square(16);
    private final Font font;

    public AbstractSignRenderer(BlockEntityRendererProvider.Context pContext) {
        this.font = pContext.getFont();
    }

    protected abstract Model getSignModel(BlockState pState, WoodType pWoodType);

    protected abstract Material getSignMaterial(WoodType pWoodType);

    protected abstract float getSignModelRenderScale();

    protected abstract float getSignTextRenderScale();

    protected abstract Vec3 getTextOffset();

    protected abstract void translateSign(PoseStack pPoseStack, float pYRot, BlockState pState);

    public void render(
        SignBlockEntity p_375644_, float p_376234_, PoseStack p_377246_, MultiBufferSource p_378186_, int p_378621_, int p_376297_, Vec3 p_394865_
    ) {
        BlockState blockstate = p_375644_.getBlockState();
        SignBlock signblock = (SignBlock)blockstate.getBlock();
        Model model = this.getSignModel(blockstate, signblock.type());
        this.renderSignWithText(p_375644_, p_377246_, p_378186_, p_378621_, p_376297_, blockstate, signblock, signblock.type(), model);
    }

    private void renderSignWithText(
        SignBlockEntity pBlockEntity,
        PoseStack pPoseStack,
        MultiBufferSource pBufferSource,
        int pPackedLight,
        int pPackedOverlay,
        BlockState pState,
        SignBlock pSign,
        WoodType pWoodType,
        Model pModel
    ) {
        pPoseStack.pushPose();
        this.translateSign(pPoseStack, -pSign.getYRotationDegrees(pState), pState);
        this.renderSign(pPoseStack, pBufferSource, pPackedLight, pPackedOverlay, pWoodType, pModel);
        this.renderSignText(pBlockEntity.getBlockPos(), pBlockEntity.getFrontText(), pPoseStack, pBufferSource, pPackedLight, pBlockEntity.getTextLineHeight(), pBlockEntity.getMaxTextLineWidth(), true);
        this.renderSignText(pBlockEntity.getBlockPos(), pBlockEntity.getBackText(), pPoseStack, pBufferSource, pPackedLight, pBlockEntity.getTextLineHeight(), pBlockEntity.getMaxTextLineWidth(), false);
        pPoseStack.popPose();
    }

    protected void renderSign(PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, int pPackedOverlay, WoodType pWoodType, Model pModel) {
        pPoseStack.pushPose();
        float f = this.getSignModelRenderScale();
        pPoseStack.scale(f, -f, -f);
        Material material = this.getSignMaterial(pWoodType);
        VertexConsumer vertexconsumer = material.buffer(pBufferSource, pModel::renderType);
        pModel.renderToBuffer(pPoseStack, vertexconsumer, pPackedLight, pPackedOverlay);
        pPoseStack.popPose();
    }

    private void renderSignText(
        BlockPos pPos,
        SignText pText,
        PoseStack pPoseStack,
        MultiBufferSource pBufferSource,
        int pPackedLight,
        int pLineHeight,
        int pMaxLineWidth,
        boolean pIsFront
    ) {
        pPoseStack.pushPose();
        this.translateSignText(pPoseStack, pIsFront, this.getTextOffset());
        int i = getDarkColor(pText);
        int j = 4 * pLineHeight / 2;
        FormattedCharSequence[] aformattedcharsequence = pText.getRenderMessages(Minecraft.getInstance().isTextFilteringEnabled(), p_378315_ -> {
            List<FormattedCharSequence> list = this.font.split(p_378315_, pMaxLineWidth);
            return list.isEmpty() ? FormattedCharSequence.EMPTY : list.get(0);
        });
        int k;
        boolean flag;
        int l;
        if (pText.hasGlowingText()) {
            k = pText.getColor().getTextColor();
            flag = isOutlineVisible(pPos, k);
            l = 15728880;
        } else {
            k = i;
            flag = false;
            l = pPackedLight;
        }

        for (int i1 = 0; i1 < 4; i1++) {
            FormattedCharSequence formattedcharsequence = aformattedcharsequence[i1];
            float f = -this.font.width(formattedcharsequence) / 2;
            if (flag) {
                this.font.drawInBatch8xOutline(formattedcharsequence, f, i1 * pLineHeight - j, k, i, pPoseStack.last().pose(), pBufferSource, l);
            } else {
                this.font
                    .drawInBatch(
                        formattedcharsequence,
                        f,
                        i1 * pLineHeight - j,
                        k,
                        false,
                        pPoseStack.last().pose(),
                        pBufferSource,
                        Font.DisplayMode.POLYGON_OFFSET,
                        0,
                        l
                    );
            }
        }

        pPoseStack.popPose();
    }

    private void translateSignText(PoseStack pPoseStack, boolean pIsFront, Vec3 pOffset) {
        if (!pIsFront) {
            pPoseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        }

        float f = 0.015625F * this.getSignTextRenderScale();
        pPoseStack.translate(pOffset);
        pPoseStack.scale(f, -f, f);
    }

    private static boolean isOutlineVisible(BlockPos pPos, int pColor) {
        if (pColor == DyeColor.BLACK.getTextColor()) {
            return true;
        } else {
            Minecraft minecraft = Minecraft.getInstance();
            LocalPlayer localplayer = minecraft.player;
            if (localplayer != null && minecraft.options.getCameraType().isFirstPerson() && localplayer.isScoping()) {
                return true;
            } else {
                Entity entity = minecraft.getCameraEntity();
                return entity != null && entity.distanceToSqr(Vec3.atCenterOf(pPos)) < OUTLINE_RENDER_DISTANCE;
            }
        }
    }

    public static int getDarkColor(SignText pText) {
        int i = pText.getColor().getTextColor();
        if (i == DyeColor.BLACK.getTextColor() && pText.hasGlowingText()) {
            return -988212;
        } else {
            double d0 = 0.4;
            int j = (int)(ARGB.red(i) * 0.4);
            int k = (int)(ARGB.green(i) * 0.4);
            int l = (int)(ARGB.blue(i) * 0.4);
            return ARGB.color(0, j, k, l);
        }
    }
}