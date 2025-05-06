package com.mojang.blaze3d.vertex;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.util.ARGB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.lwjgl.system.MemoryStack;

@OnlyIn(Dist.CLIENT)
public interface VertexConsumer extends net.minecraftforge.client.extensions.IForgeVertexConsumer {
    VertexConsumer addVertex(float pX, float pY, float pZ);

    VertexConsumer setColor(int pRed, int pGreen, int pBlue, int pAlpha);

    VertexConsumer setUv(float pU, float pV);

    VertexConsumer setUv1(int pU, int pV);

    VertexConsumer setUv2(int pU, int pV);

    VertexConsumer setNormal(float pNormalX, float pNormalY, float pNormalZ);

    default void addVertex(
        float pX,
        float pY,
        float pZ,
        int pColor,
        float pU,
        float pV,
        int pPackedOverlay,
        int pPackedLight,
        float pNormalX,
        float pNormalY,
        float pNormalZ
    ) {
        this.addVertex(pX, pY, pZ);
        this.setColor(pColor);
        this.setUv(pU, pV);
        this.setOverlay(pPackedOverlay);
        this.setLight(pPackedLight);
        this.setNormal(pNormalX, pNormalY, pNormalZ);
    }

    default VertexConsumer setColor(float pRed, float pGreen, float pBlue, float pAlpha) {
        return this.setColor((int)(pRed * 255.0F), (int)(pGreen * 255.0F), (int)(pBlue * 255.0F), (int)(pAlpha * 255.0F));
    }

    default VertexConsumer setColor(int pColor) {
        return this.setColor(ARGB.red(pColor), ARGB.green(pColor), ARGB.blue(pColor), ARGB.alpha(pColor));
    }

    default VertexConsumer setWhiteAlpha(int pAlpha) {
        return this.setColor(ARGB.color(pAlpha, -1));
    }

    default VertexConsumer setLight(int pPackedLight) {
        return this.setUv2(pPackedLight & 65535, pPackedLight >> 16 & 65535);
    }

    default VertexConsumer setOverlay(int pPackedOverlay) {
        return this.setUv1(pPackedOverlay & 65535, pPackedOverlay >> 16 & 65535);
    }

    default void putBulkData(
        PoseStack.Pose pPose, BakedQuad pQuad, float pRed, float pGreen, float pBlue, float pAlpha, int pPackedLight, int pPackedOverlay
    ) {
        this.putBulkData(
            pPose,
            pQuad,
            new float[]{1.0F, 1.0F, 1.0F, 1.0F},
            pRed,
            pGreen,
            pBlue,
            pAlpha,
            new int[]{pPackedLight, pPackedLight, pPackedLight, pPackedLight},
            pPackedOverlay,
            false
        );
    }

    default void putBulkData(
        PoseStack.Pose pPose,
        BakedQuad pQuad,
        float[] p_85998_,
        float pRed,
        float pGreen,
        float pBlue,
        float alpha,
        int[] p_86002_,
        int pPackedLight,
        boolean p_86004_
    ) {
        int[] aint = pQuad.vertices();
        Vector3fc vector3fc = pQuad.direction().getUnitVec3f();
        Matrix4f matrix4f = pPose.pose();
        Vector3f vector3f = pPose.transformNormal(vector3fc, new Vector3f());
        int i = 8;
        int j = aint.length / 8;
        int k = (int)(alpha * 255.0F);
        int l = pQuad.lightEmission();

        try (MemoryStack memorystack = MemoryStack.stackPush()) {
            ByteBuffer bytebuffer = memorystack.malloc(DefaultVertexFormat.BLOCK.getVertexSize());
            IntBuffer intbuffer = bytebuffer.asIntBuffer();

            for (int i1 = 0; i1 < j; i1++) {
                intbuffer.clear();
                intbuffer.put(aint, i1 * 8, 8);
                float f = bytebuffer.getFloat(0);
                float f1 = bytebuffer.getFloat(4);
                float f2 = bytebuffer.getFloat(8);
                float f3;
                float f4;
                float f5;
                if (p_86004_) {
                    float f6 = bytebuffer.get(12) & 255;
                    float f7 = bytebuffer.get(13) & 255;
                    float f8 = bytebuffer.get(14) & 255;
                    f3 = f6 * p_85998_[i1] * pRed;
                    f4 = f7 * p_85998_[i1] * pGreen;
                    f5 = f8 * p_85998_[i1] * pBlue;
                } else {
                    f3 = p_85998_[i1] * pRed * 255.0F;
                    f4 = p_85998_[i1] * pGreen * 255.0F;
                    f5 = p_85998_[i1] * pBlue * 255.0F;
                }

                int j1 = ARGB.color(k, (int)f3, (int)f4, (int)f5);
                int k1 = LightTexture.lightCoordsWithEmission(p_86002_[i1], l);
                k1 = applyBakedLighting(k1, bytebuffer);
                float f10 = bytebuffer.getFloat(16);
                float f9 = bytebuffer.getFloat(20);
                Vector3f vector3f1 = matrix4f.transformPosition(f, f1, f2, new Vector3f());
                applyBakedNormals(vector3f, bytebuffer, pPose.normal());
                this.addVertex(vector3f1.x(), vector3f1.y(), vector3f1.z(), j1, f10, f9, pPackedLight, k1, vector3f.x(), vector3f.y(), vector3f.z());
            }
        }
    }

    default VertexConsumer addVertex(Vector3f pPos) {
        return this.addVertex(pPos.x(), pPos.y(), pPos.z());
    }

    default VertexConsumer addVertex(PoseStack.Pose pPose, Vector3f pPos) {
        return this.addVertex(pPose, pPos.x(), pPos.y(), pPos.z());
    }

    default VertexConsumer addVertex(PoseStack.Pose pPose, float pX, float pY, float pZ) {
        return this.addVertex(pPose.pose(), pX, pY, pZ);
    }

    default VertexConsumer addVertex(Matrix4f pPose, float pX, float pY, float pZ) {
        Vector3f vector3f = pPose.transformPosition(pX, pY, pZ, new Vector3f());
        return this.addVertex(vector3f.x(), vector3f.y(), vector3f.z());
    }

    default VertexConsumer setNormal(PoseStack.Pose pPose, float pNormalX, float pNormalY, float pNormalZ) {
        Vector3f vector3f = pPose.transformNormal(pNormalX, pNormalY, pNormalZ, new Vector3f());
        return this.setNormal(vector3f.x(), vector3f.y(), vector3f.z());
    }

    default VertexConsumer setNormal(PoseStack.Pose pPose, Vector3f pNormalVector) {
        return this.setNormal(pPose, pNormalVector.x(), pNormalVector.y(), pNormalVector.z());
    }
}
