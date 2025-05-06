package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.CowModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.MushroomCowRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MushroomCowMushroomLayer extends RenderLayer<MushroomCowRenderState, CowModel> {
    private final BlockRenderDispatcher blockRenderer;

    public MushroomCowMushroomLayer(RenderLayerParent<MushroomCowRenderState, CowModel> pRenderer, BlockRenderDispatcher pBlockRenderer) {
        super(pRenderer);
        this.blockRenderer = pBlockRenderer;
    }

    public void render(PoseStack p_117256_, MultiBufferSource p_117257_, int p_117258_, MushroomCowRenderState p_367819_, float p_117260_, float p_117261_) {
        if (!p_367819_.isBaby) {
            boolean flag = p_367819_.appearsGlowing && p_367819_.isInvisible;
            if (!p_367819_.isInvisible || flag) {
                BlockState blockstate = p_367819_.variant.getBlockState();
                int i = LivingEntityRenderer.getOverlayCoords(p_367819_, 0.0F);
                BlockStateModel blockstatemodel = this.blockRenderer.getBlockModel(blockstate);
                p_117256_.pushPose();
                p_117256_.translate(0.2F, -0.35F, 0.5F);
                p_117256_.mulPose(Axis.YP.rotationDegrees(-48.0F));
                p_117256_.scale(-1.0F, -1.0F, 1.0F);
                p_117256_.translate(-0.5F, -0.5F, -0.5F);
                this.renderMushroomBlock(p_117256_, p_117257_, p_117258_, flag, blockstate, i, blockstatemodel);
                p_117256_.popPose();
                p_117256_.pushPose();
                p_117256_.translate(0.2F, -0.35F, 0.5F);
                p_117256_.mulPose(Axis.YP.rotationDegrees(42.0F));
                p_117256_.translate(0.1F, 0.0F, -0.6F);
                p_117256_.mulPose(Axis.YP.rotationDegrees(-48.0F));
                p_117256_.scale(-1.0F, -1.0F, 1.0F);
                p_117256_.translate(-0.5F, -0.5F, -0.5F);
                this.renderMushroomBlock(p_117256_, p_117257_, p_117258_, flag, blockstate, i, blockstatemodel);
                p_117256_.popPose();
                p_117256_.pushPose();
                this.getParentModel().getHead().translateAndRotate(p_117256_);
                p_117256_.translate(0.0F, -0.7F, -0.2F);
                p_117256_.mulPose(Axis.YP.rotationDegrees(-78.0F));
                p_117256_.scale(-1.0F, -1.0F, 1.0F);
                p_117256_.translate(-0.5F, -0.5F, -0.5F);
                this.renderMushroomBlock(p_117256_, p_117257_, p_117258_, flag, blockstate, i, blockstatemodel);
                p_117256_.popPose();
            }
        }
    }

    private void renderMushroomBlock(
        PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, boolean pRenderOutline, BlockState pState, int pPackedOverlay, BlockStateModel pModel
    ) {
        if (pRenderOutline) {
            ModelBlockRenderer.renderModel(
                pPoseStack.last(), pBufferSource.getBuffer(RenderType.outline(TextureAtlas.LOCATION_BLOCKS)), pModel, 0.0F, 0.0F, 0.0F, pPackedLight, pPackedOverlay
            );
        } else {
            this.blockRenderer.renderSingleBlock(pState, pPoseStack, pBufferSource, pPackedLight, pPackedOverlay);
        }
    }
}