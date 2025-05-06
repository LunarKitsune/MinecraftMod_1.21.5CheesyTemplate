package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.state.PaintingRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.PaintingTextureManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PaintingRenderer extends EntityRenderer<Painting, PaintingRenderState> {
    public PaintingRenderer(EntityRendererProvider.Context p_174332_) {
        super(p_174332_);
    }

    public void render(PaintingRenderState p_362459_, PoseStack p_115555_, MultiBufferSource p_115556_, int p_115557_) {
        PaintingVariant paintingvariant = p_362459_.variant;
        if (paintingvariant != null) {
            p_115555_.pushPose();
            p_115555_.mulPose(Axis.YP.rotationDegrees(180 - p_362459_.direction.get2DDataValue() * 90));
            PaintingTextureManager paintingtexturemanager = Minecraft.getInstance().getPaintingTextures();
            TextureAtlasSprite textureatlassprite = paintingtexturemanager.getBackSprite();
            VertexConsumer vertexconsumer = p_115556_.getBuffer(RenderType.entitySolidZOffsetForward(textureatlassprite.atlasLocation()));
            this.renderPainting(
                p_115555_,
                vertexconsumer,
                p_362459_.lightCoords,
                paintingvariant.width(),
                paintingvariant.height(),
                paintingtexturemanager.get(paintingvariant),
                textureatlassprite
            );
            p_115555_.popPose();
            super.render(p_362459_, p_115555_, p_115556_, p_115557_);
        }
    }

    public PaintingRenderState createRenderState() {
        return new PaintingRenderState();
    }

    public void extractRenderState(Painting p_366500_, PaintingRenderState p_365628_, float p_360852_) {
        super.extractRenderState(p_366500_, p_365628_, p_360852_);
        Direction direction = p_366500_.getDirection();
        PaintingVariant paintingvariant = p_366500_.getVariant().value();
        p_365628_.direction = direction;
        p_365628_.variant = paintingvariant;
        int i = paintingvariant.width();
        int j = paintingvariant.height();
        if (p_365628_.lightCoords.length != i * j) {
            p_365628_.lightCoords = new int[i * j];
        }

        float f = -i / 2.0F;
        float f1 = -j / 2.0F;
        Level level = p_366500_.level();

        for (int k = 0; k < j; k++) {
            for (int l = 0; l < i; l++) {
                float f2 = l + f + 0.5F;
                float f3 = k + f1 + 0.5F;
                int i1 = p_366500_.getBlockX();
                int j1 = Mth.floor(p_366500_.getY() + f3);
                int k1 = p_366500_.getBlockZ();
                switch (direction) {
                    case NORTH:
                        i1 = Mth.floor(p_366500_.getX() + f2);
                        break;
                    case WEST:
                        k1 = Mth.floor(p_366500_.getZ() - f2);
                        break;
                    case SOUTH:
                        i1 = Mth.floor(p_366500_.getX() - f2);
                        break;
                    case EAST:
                        k1 = Mth.floor(p_366500_.getZ() + f2);
                }

                p_365628_.lightCoords[l + k * i] = LevelRenderer.getLightColor(level, new BlockPos(i1, j1, k1));
            }
        }
    }

    private void renderPainting(
        PoseStack pPoseStack,
        VertexConsumer pBuffer,
        int[] pLightCoords,
        int pWidth,
        int pHeight,
        TextureAtlasSprite pFrontSprite,
        TextureAtlasSprite pBackSprite
    ) {
        PoseStack.Pose posestack$pose = pPoseStack.last();
        float f = -pWidth / 2.0F;
        float f1 = -pHeight / 2.0F;
        float f2 = 0.03125F;
        float f3 = pBackSprite.getU0();
        float f4 = pBackSprite.getU1();
        float f5 = pBackSprite.getV0();
        float f6 = pBackSprite.getV1();
        float f7 = pBackSprite.getU0();
        float f8 = pBackSprite.getU1();
        float f9 = pBackSprite.getV0();
        float f10 = pBackSprite.getV(0.0625F);
        float f11 = pBackSprite.getU0();
        float f12 = pBackSprite.getU(0.0625F);
        float f13 = pBackSprite.getV0();
        float f14 = pBackSprite.getV1();
        double d0 = 1.0 / pWidth;
        double d1 = 1.0 / pHeight;

        for (int i = 0; i < pWidth; i++) {
            for (int j = 0; j < pHeight; j++) {
                float f15 = f + (i + 1);
                float f16 = f + i;
                float f17 = f1 + (j + 1);
                float f18 = f1 + j;
                int k = pLightCoords[i + j * pWidth];
                float f19 = pFrontSprite.getU((float)(d0 * (pWidth - i)));
                float f20 = pFrontSprite.getU((float)(d0 * (pWidth - (i + 1))));
                float f21 = pFrontSprite.getV((float)(d1 * (pHeight - j)));
                float f22 = pFrontSprite.getV((float)(d1 * (pHeight - (j + 1))));
                this.vertex(posestack$pose, pBuffer, f15, f18, f20, f21, -0.03125F, 0, 0, -1, k);
                this.vertex(posestack$pose, pBuffer, f16, f18, f19, f21, -0.03125F, 0, 0, -1, k);
                this.vertex(posestack$pose, pBuffer, f16, f17, f19, f22, -0.03125F, 0, 0, -1, k);
                this.vertex(posestack$pose, pBuffer, f15, f17, f20, f22, -0.03125F, 0, 0, -1, k);
                this.vertex(posestack$pose, pBuffer, f15, f17, f4, f5, 0.03125F, 0, 0, 1, k);
                this.vertex(posestack$pose, pBuffer, f16, f17, f3, f5, 0.03125F, 0, 0, 1, k);
                this.vertex(posestack$pose, pBuffer, f16, f18, f3, f6, 0.03125F, 0, 0, 1, k);
                this.vertex(posestack$pose, pBuffer, f15, f18, f4, f6, 0.03125F, 0, 0, 1, k);
                this.vertex(posestack$pose, pBuffer, f15, f17, f7, f9, -0.03125F, 0, 1, 0, k);
                this.vertex(posestack$pose, pBuffer, f16, f17, f8, f9, -0.03125F, 0, 1, 0, k);
                this.vertex(posestack$pose, pBuffer, f16, f17, f8, f10, 0.03125F, 0, 1, 0, k);
                this.vertex(posestack$pose, pBuffer, f15, f17, f7, f10, 0.03125F, 0, 1, 0, k);
                this.vertex(posestack$pose, pBuffer, f15, f18, f7, f9, 0.03125F, 0, -1, 0, k);
                this.vertex(posestack$pose, pBuffer, f16, f18, f8, f9, 0.03125F, 0, -1, 0, k);
                this.vertex(posestack$pose, pBuffer, f16, f18, f8, f10, -0.03125F, 0, -1, 0, k);
                this.vertex(posestack$pose, pBuffer, f15, f18, f7, f10, -0.03125F, 0, -1, 0, k);
                this.vertex(posestack$pose, pBuffer, f15, f17, f12, f13, 0.03125F, -1, 0, 0, k);
                this.vertex(posestack$pose, pBuffer, f15, f18, f12, f14, 0.03125F, -1, 0, 0, k);
                this.vertex(posestack$pose, pBuffer, f15, f18, f11, f14, -0.03125F, -1, 0, 0, k);
                this.vertex(posestack$pose, pBuffer, f15, f17, f11, f13, -0.03125F, -1, 0, 0, k);
                this.vertex(posestack$pose, pBuffer, f16, f17, f12, f13, -0.03125F, 1, 0, 0, k);
                this.vertex(posestack$pose, pBuffer, f16, f18, f12, f14, -0.03125F, 1, 0, 0, k);
                this.vertex(posestack$pose, pBuffer, f16, f18, f11, f14, 0.03125F, 1, 0, 0, k);
                this.vertex(posestack$pose, pBuffer, f16, f17, f11, f13, 0.03125F, 1, 0, 0, k);
            }
        }
    }

    private void vertex(
        PoseStack.Pose pPose,
        VertexConsumer pConsumer,
        float pX,
        float pY,
        float pU,
        float pV,
        float pZ,
        int pNormalX,
        int pNormalY,
        int pNormalZ,
        int pPackedLight
    ) {
        pConsumer.addVertex(pPose, pX, pY, pZ)
            .setColor(-1)
            .setUv(pU, pV)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(pPackedLight)
            .setNormal(pPose, pNormalX, pNormalY, pNormalZ);
    }
}