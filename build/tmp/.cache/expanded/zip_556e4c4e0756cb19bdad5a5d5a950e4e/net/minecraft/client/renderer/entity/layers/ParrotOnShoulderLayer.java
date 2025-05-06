package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.ParrotModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ParrotRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.ParrotRenderState;
import net.minecraft.client.renderer.entity.state.PlayerRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ParrotOnShoulderLayer extends RenderLayer<PlayerRenderState, PlayerModel> {
    private final ParrotModel model;
    private final ParrotRenderState parrotState = new ParrotRenderState();

    public ParrotOnShoulderLayer(RenderLayerParent<PlayerRenderState, PlayerModel> pRenderer, EntityModelSet pModelSet) {
        super(pRenderer);
        this.model = new ParrotModel(pModelSet.bakeLayer(ModelLayers.PARROT));
        this.parrotState.pose = ParrotModel.Pose.ON_SHOULDER;
    }

    public void render(PoseStack p_117318_, MultiBufferSource p_117319_, int p_117320_, PlayerRenderState p_365020_, float p_117322_, float p_117323_) {
        Parrot.Variant parrot$variant = p_365020_.parrotOnLeftShoulder;
        if (parrot$variant != null) {
            this.renderOnShoulder(p_117318_, p_117319_, p_117320_, p_365020_, parrot$variant, p_117322_, p_117323_, true);
        }

        Parrot.Variant parrot$variant1 = p_365020_.parrotOnRightShoulder;
        if (parrot$variant1 != null) {
            this.renderOnShoulder(p_117318_, p_117319_, p_117320_, p_365020_, parrot$variant1, p_117322_, p_117323_, false);
        }
    }

    private void renderOnShoulder(
        PoseStack pPoseStack,
        MultiBufferSource pBuffer,
        int pPackedLight,
        PlayerRenderState pRenderState,
        Parrot.Variant pVariant,
        float pYRot,
        float pXRot,
        boolean pLeftShoulder
    ) {
        pPoseStack.pushPose();
        pPoseStack.translate(pLeftShoulder ? 0.4F : -0.4F, pRenderState.isCrouching ? -1.3F : -1.5F, 0.0F);
        this.parrotState.ageInTicks = pRenderState.ageInTicks;
        this.parrotState.walkAnimationPos = pRenderState.walkAnimationPos;
        this.parrotState.walkAnimationSpeed = pRenderState.walkAnimationSpeed;
        this.parrotState.yRot = pYRot;
        this.parrotState.xRot = pXRot;
        this.model.setupAnim(this.parrotState);
        this.model
            .renderToBuffer(pPoseStack, pBuffer.getBuffer(this.model.renderType(ParrotRenderer.getVariantTexture(pVariant))), pPackedLight, OverlayTexture.NO_OVERLAY);
        pPoseStack.popPose();
    }
}