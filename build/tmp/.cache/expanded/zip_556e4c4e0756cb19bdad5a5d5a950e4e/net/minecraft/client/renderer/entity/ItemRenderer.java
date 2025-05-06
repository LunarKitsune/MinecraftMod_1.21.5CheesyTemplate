package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import com.mojang.math.MatrixUtil;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ItemRenderer {
    public static final ResourceLocation ENCHANTED_GLINT_ARMOR = ResourceLocation.withDefaultNamespace("textures/misc/enchanted_glint_armor.png");
    public static final ResourceLocation ENCHANTED_GLINT_ITEM = ResourceLocation.withDefaultNamespace("textures/misc/enchanted_glint_item.png");
    public static final int GUI_SLOT_CENTER_X = 8;
    public static final int GUI_SLOT_CENTER_Y = 8;
    public static final int ITEM_DECORATION_BLIT_OFFSET = 200;
    public static final float COMPASS_FOIL_UI_SCALE = 0.5F;
    public static final float COMPASS_FOIL_FIRST_PERSON_SCALE = 0.75F;
    public static final float COMPASS_FOIL_TEXTURE_SCALE = 0.0078125F;
    public static final int NO_TINT = -1;
    private final ItemModelResolver resolver;
    private final ItemStackRenderState scratchItemStackRenderState = new ItemStackRenderState();

    public ItemRenderer(ItemModelResolver pResolver) {
        this.resolver = pResolver;
    }

    public static void renderItem(
        ItemDisplayContext pDisplayContext,
        PoseStack pPoseStack,
        MultiBufferSource pBufferSource,
        int pPackedLight,
        int pPackedOverlay,
        int[] pTintLayers,
        List<BakedQuad> pQuads,
        RenderType pRenderType,
        ItemStackRenderState.FoilType pFoilType
    ) {
        VertexConsumer vertexconsumer;
        if (pFoilType == ItemStackRenderState.FoilType.SPECIAL) {
            PoseStack.Pose posestack$pose = pPoseStack.last().copy();
            if (pDisplayContext == ItemDisplayContext.GUI) {
                MatrixUtil.mulComponentWise(posestack$pose.pose(), 0.5F);
            } else if (pDisplayContext.firstPerson()) {
                MatrixUtil.mulComponentWise(posestack$pose.pose(), 0.75F);
            }

            vertexconsumer = getCompassFoilBuffer(pBufferSource, pRenderType, posestack$pose);
        } else {
            vertexconsumer = getFoilBuffer(pBufferSource, pRenderType, true, pFoilType != ItemStackRenderState.FoilType.NONE);
        }

        renderQuadList(pPoseStack, vertexconsumer, pQuads, pTintLayers, pPackedLight, pPackedOverlay);
    }

    public static VertexConsumer getArmorFoilBuffer(MultiBufferSource pBufferSource, RenderType pRenderType, boolean pHasFoil) {
        return pHasFoil ? VertexMultiConsumer.create(pBufferSource.getBuffer(RenderType.armorEntityGlint()), pBufferSource.getBuffer(pRenderType)) : pBufferSource.getBuffer(pRenderType);
    }

    private static VertexConsumer getCompassFoilBuffer(MultiBufferSource pBufferSource, RenderType pRenderType, PoseStack.Pose pPose) {
        return VertexMultiConsumer.create(
            new SheetedDecalTextureGenerator(pBufferSource.getBuffer(RenderType.glint()), pPose, 0.0078125F), pBufferSource.getBuffer(pRenderType)
        );
    }

    public static VertexConsumer getFoilBuffer(MultiBufferSource pBufferSource, RenderType pRenderType, boolean pIsItem, boolean pGlint) {
        if (pGlint) {
            return Minecraft.useShaderTransparency() && pRenderType == Sheets.translucentItemSheet()
                ? VertexMultiConsumer.create(pBufferSource.getBuffer(RenderType.glintTranslucent()), pBufferSource.getBuffer(pRenderType))
                : VertexMultiConsumer.create(pBufferSource.getBuffer(pIsItem ? RenderType.glint() : RenderType.entityGlint()), pBufferSource.getBuffer(pRenderType));
        } else {
            return pBufferSource.getBuffer(pRenderType);
        }
    }

    private static int getLayerColorSafe(int[] pTintLayers, int pIndex) {
        return pIndex >= 0 && pIndex < pTintLayers.length ? pTintLayers[pIndex] : -1;
    }

    public static void renderQuadList(PoseStack pPoseStack, VertexConsumer pBuffer, List<BakedQuad> pQuads, int[] pTintLayers, int pPackedLight, int pPackedOverlay) {
        PoseStack.Pose posestack$pose = pPoseStack.last();

        for (BakedQuad bakedquad : pQuads) {
            float f;
            float f1;
            float f2;
            float f3;
            if (bakedquad.isTinted()) {
                int i = getLayerColorSafe(pTintLayers, bakedquad.tintIndex());
                f = ARGB.alpha(i) / 255.0F;
                f1 = ARGB.red(i) / 255.0F;
                f2 = ARGB.green(i) / 255.0F;
                f3 = ARGB.blue(i) / 255.0F;
            } else {
                f = 1.0F;
                f1 = 1.0F;
                f2 = 1.0F;
                f3 = 1.0F;
            }

            pBuffer.putBulkData(posestack$pose, bakedquad, f1, f2, f3, f, pPackedLight, pPackedOverlay, true);
        }
    }

    public void renderStatic(
        ItemStack pStack,
        ItemDisplayContext pDisplayContext,
        int pPackedLight,
        int pPackedOverlay,
        PoseStack pPoseStack,
        MultiBufferSource pBufferSource,
        @Nullable Level pLevel,
        int pSeed
    ) {
        this.renderStatic(null, pStack, pDisplayContext, pPoseStack, pBufferSource, pLevel, pPackedLight, pPackedOverlay, pSeed);
    }

    public void renderStatic(
        @Nullable LivingEntity pEntity,
        ItemStack pStack,
        ItemDisplayContext pDisplayContext,
        PoseStack pPoseStack,
        MultiBufferSource pBufferSource,
        @Nullable Level pLevel,
        int pPackedLight,
        int pPackedOverlay,
        int pSeed
    ) {
        this.resolver.updateForTopItem(this.scratchItemStackRenderState, pStack, pDisplayContext, pLevel, pEntity, pSeed);
        this.scratchItemStackRenderState.render(pPoseStack, pBufferSource, pPackedLight, pPackedOverlay);
    }
}
