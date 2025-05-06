package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ItemInHandLayer<S extends ArmedEntityRenderState, M extends EntityModel<S> & ArmedModel> extends RenderLayer<S, M> {
    public ItemInHandLayer(RenderLayerParent<S, M> p_234846_) {
        super(p_234846_);
    }

    public void render(PoseStack p_117204_, MultiBufferSource p_117205_, int p_117206_, S p_375467_, float p_117208_, float p_117209_) {
        this.renderArmWithItem(p_375467_, p_375467_.rightHandItem, HumanoidArm.RIGHT, p_117204_, p_117205_, p_117206_);
        this.renderArmWithItem(p_375467_, p_375467_.leftHandItem, HumanoidArm.LEFT, p_117204_, p_117205_, p_117206_);
    }

    protected void renderArmWithItem(
        S pRenderState, ItemStackRenderState pItemStackRenderState, HumanoidArm pArm, PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight
    ) {
        if (!pItemStackRenderState.isEmpty()) {
            pPoseStack.pushPose();
            this.getParentModel().translateToHand(pArm, pPoseStack);
            pPoseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            pPoseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            boolean flag = pArm == HumanoidArm.LEFT;
            pPoseStack.translate((flag ? -1 : 1) / 16.0F, 0.125F, -0.625F);
            pItemStackRenderState.render(pPoseStack, pBufferSource, pPackedLight, OverlayTexture.NO_OVERLAY);
            pPoseStack.popPose();
        }
    }
}