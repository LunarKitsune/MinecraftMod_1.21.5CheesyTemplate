package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class RenderLayer<S extends EntityRenderState, M extends EntityModel<? super S>> {
    private final RenderLayerParent<S, M> renderer;

    public RenderLayer(RenderLayerParent<S, M> pRenderer) {
        this.renderer = pRenderer;
    }

    protected static <S extends LivingEntityRenderState> void coloredCutoutModelCopyLayerRender(
        EntityModel<S> pModel, ResourceLocation pTextureLocation, PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, S pRenderState, int pColor
    ) {
        if (!pRenderState.isInvisible) {
            pModel.setupAnim(pRenderState);
            renderColoredCutoutModel(pModel, pTextureLocation, pPoseStack, pBufferSource, pPackedLight, pRenderState, pColor);
        }
    }

    protected static void renderColoredCutoutModel(
        EntityModel<?> pModel,
        ResourceLocation pTextureLocation,
        PoseStack pPoseStack,
        MultiBufferSource pBufferSource,
        int pPackedLight,
        LivingEntityRenderState pRenderState,
        int pColor
    ) {
        VertexConsumer vertexconsumer = pBufferSource.getBuffer(RenderType.entityCutoutNoCull(pTextureLocation));
        pModel.renderToBuffer(pPoseStack, vertexconsumer, pPackedLight, LivingEntityRenderer.getOverlayCoords(pRenderState, 0.0F), pColor);
    }

    public M getParentModel() {
        return this.renderer.getModel();
    }

    public abstract void render(PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, S pRenderState, float pYRot, float pXRot);
}