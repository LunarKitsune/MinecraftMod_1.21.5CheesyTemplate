package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.state.LightningBoltRenderState;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LightningBolt;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class LightningBoltRenderer extends EntityRenderer<LightningBolt, LightningBoltRenderState> {
    public LightningBoltRenderer(EntityRendererProvider.Context p_174286_) {
        super(p_174286_);
    }

    public void render(LightningBoltRenderState p_367381_, PoseStack p_115269_, MultiBufferSource p_115270_, int p_115271_) {
        float[] afloat = new float[8];
        float[] afloat1 = new float[8];
        float f = 0.0F;
        float f1 = 0.0F;
        RandomSource randomsource = RandomSource.create(p_367381_.seed);

        for (int i = 7; i >= 0; i--) {
            afloat[i] = f;
            afloat1[i] = f1;
            f += randomsource.nextInt(11) - 5;
            f1 += randomsource.nextInt(11) - 5;
        }

        VertexConsumer vertexconsumer = p_115270_.getBuffer(RenderType.lightning());
        Matrix4f matrix4f = p_115269_.last().pose();

        for (int j = 0; j < 4; j++) {
            RandomSource randomsource1 = RandomSource.create(p_367381_.seed);

            for (int k = 0; k < 3; k++) {
                int l = 7;
                int i1 = 0;
                if (k > 0) {
                    l = 7 - k;
                }

                if (k > 0) {
                    i1 = l - 2;
                }

                float f2 = afloat[l] - f;
                float f3 = afloat1[l] - f1;

                for (int j1 = l; j1 >= i1; j1--) {
                    float f4 = f2;
                    float f5 = f3;
                    if (k == 0) {
                        f2 += randomsource1.nextInt(11) - 5;
                        f3 += randomsource1.nextInt(11) - 5;
                    } else {
                        f2 += randomsource1.nextInt(31) - 15;
                        f3 += randomsource1.nextInt(31) - 15;
                    }

                    float f6 = 0.5F;
                    float f7 = 0.45F;
                    float f8 = 0.45F;
                    float f9 = 0.5F;
                    float f10 = 0.1F + j * 0.2F;
                    if (k == 0) {
                        f10 *= j1 * 0.1F + 1.0F;
                    }

                    float f11 = 0.1F + j * 0.2F;
                    if (k == 0) {
                        f11 *= (j1 - 1.0F) * 0.1F + 1.0F;
                    }

                    quad(matrix4f, vertexconsumer, f2, f3, j1, f4, f5, 0.45F, 0.45F, 0.5F, f10, f11, false, false, true, false);
                    quad(matrix4f, vertexconsumer, f2, f3, j1, f4, f5, 0.45F, 0.45F, 0.5F, f10, f11, true, false, true, true);
                    quad(matrix4f, vertexconsumer, f2, f3, j1, f4, f5, 0.45F, 0.45F, 0.5F, f10, f11, true, true, false, true);
                    quad(matrix4f, vertexconsumer, f2, f3, j1, f4, f5, 0.45F, 0.45F, 0.5F, f10, f11, false, true, false, false);
                }
            }
        }
    }

    private static void quad(
        Matrix4f pPose,
        VertexConsumer pBuffer,
        float pX1,
        float pZ1,
        int pSectionY,
        float pX2,
        float pZ2,
        float pRed,
        float pGreen,
        float pBlue,
        float pInnerThickness,
        float pOuterThickness,
        boolean pAddThicknessLeftSideX,
        boolean pAddThicknessLeftSideZ,
        boolean pAddThicknessRightSideX,
        boolean pAddThicknessRightSideZ
    ) {
        pBuffer.addVertex(pPose, pX1 + (pAddThicknessLeftSideX ? pOuterThickness : -pOuterThickness), pSectionY * 16, pZ1 + (pAddThicknessLeftSideZ ? pOuterThickness : -pOuterThickness))
            .setColor(pRed, pGreen, pBlue, 0.3F);
        pBuffer.addVertex(pPose, pX2 + (pAddThicknessLeftSideX ? pInnerThickness : -pInnerThickness), (pSectionY + 1) * 16, pZ2 + (pAddThicknessLeftSideZ ? pInnerThickness : -pInnerThickness))
            .setColor(pRed, pGreen, pBlue, 0.3F);
        pBuffer.addVertex(pPose, pX2 + (pAddThicknessRightSideX ? pInnerThickness : -pInnerThickness), (pSectionY + 1) * 16, pZ2 + (pAddThicknessRightSideZ ? pInnerThickness : -pInnerThickness))
            .setColor(pRed, pGreen, pBlue, 0.3F);
        pBuffer.addVertex(pPose, pX1 + (pAddThicknessRightSideX ? pOuterThickness : -pOuterThickness), pSectionY * 16, pZ1 + (pAddThicknessRightSideZ ? pOuterThickness : -pOuterThickness))
            .setColor(pRed, pGreen, pBlue, 0.3F);
    }

    public LightningBoltRenderState createRenderState() {
        return new LightningBoltRenderState();
    }

    public void extractRenderState(LightningBolt p_364798_, LightningBoltRenderState p_367959_, float p_369027_) {
        super.extractRenderState(p_364798_, p_367959_, p_369027_);
        p_367959_.seed = p_364798_.seed;
    }

    protected boolean affectedByCulling(LightningBolt p_365522_) {
        return false;
    }
}