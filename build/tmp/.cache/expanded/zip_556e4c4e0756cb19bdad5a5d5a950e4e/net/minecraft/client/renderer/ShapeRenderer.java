package net.minecraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class ShapeRenderer {
    public static void renderShape(
        PoseStack pPoseStack, VertexConsumer pBuffer, VoxelShape pShape, double pX, double pY, double pZ, int pColor
    ) {
        PoseStack.Pose posestack$pose = pPoseStack.last();
        pShape.forAllEdges(
            (p_368095_, p_361366_, p_363660_, p_361928_, p_364145_, p_361311_) -> {
                Vector3f vector3f = new Vector3f((float)(p_361928_ - p_368095_), (float)(p_364145_ - p_361366_), (float)(p_361311_ - p_363660_)).normalize();
                pBuffer.addVertex(posestack$pose, (float)(p_368095_ + pX), (float)(p_361366_ + pY), (float)(p_363660_ + pZ))
                    .setColor(pColor)
                    .setNormal(posestack$pose, vector3f);
                pBuffer.addVertex(posestack$pose, (float)(p_361928_ + pX), (float)(p_364145_ + pY), (float)(p_361311_ + pZ))
                    .setColor(pColor)
                    .setNormal(posestack$pose, vector3f);
            }
        );
    }

    public static void renderLineBox(
        PoseStack pPoseStack, VertexConsumer pBuffer, AABB pBox, float pRed, float pGreen, float pBlue, float pAlpha
    ) {
        renderLineBox(
            pPoseStack,
            pBuffer,
            pBox.minX,
            pBox.minY,
            pBox.minZ,
            pBox.maxX,
            pBox.maxY,
            pBox.maxZ,
            pRed,
            pGreen,
            pBlue,
            pAlpha,
            pRed,
            pGreen,
            pBlue
        );
    }

    public static void renderLineBox(
        PoseStack pPoseStack,
        VertexConsumer pBuffer,
        double pMinX,
        double pMinY,
        double pMinZ,
        double pMaxX,
        double pMaxY,
        double pMaxZ,
        float pRed,
        float pGreen,
        float pBlue,
        float pAlpha
    ) {
        renderLineBox(
            pPoseStack,
            pBuffer,
            pMinX,
            pMinY,
            pMinZ,
            pMaxX,
            pMaxY,
            pMaxZ,
            pRed,
            pGreen,
            pBlue,
            pAlpha,
            pRed,
            pGreen,
            pBlue
        );
    }

    public static void renderLineBox(
        PoseStack pPoseStack,
        VertexConsumer pBuffer,
        double pMinX,
        double pMinY,
        double pMinZ,
        double pMaxX,
        double pMaxY,
        double pMaxZ,
        float pRed,
        float pGreen,
        float pBlue,
        float pAlpha,
        float pRed2,
        float pGreen2,
        float pBlue2
    ) {
        PoseStack.Pose posestack$pose = pPoseStack.last();
        float f = (float)pMinX;
        float f1 = (float)pMinY;
        float f2 = (float)pMinZ;
        float f3 = (float)pMaxX;
        float f4 = (float)pMaxY;
        float f5 = (float)pMaxZ;
        pBuffer.addVertex(posestack$pose, f, f1, f2).setColor(pRed, pGreen2, pBlue2, pAlpha).setNormal(posestack$pose, 1.0F, 0.0F, 0.0F);
        pBuffer.addVertex(posestack$pose, f3, f1, f2).setColor(pRed, pGreen2, pBlue2, pAlpha).setNormal(posestack$pose, 1.0F, 0.0F, 0.0F);
        pBuffer.addVertex(posestack$pose, f, f1, f2).setColor(pRed2, pGreen, pBlue2, pAlpha).setNormal(posestack$pose, 0.0F, 1.0F, 0.0F);
        pBuffer.addVertex(posestack$pose, f, f4, f2).setColor(pRed2, pGreen, pBlue2, pAlpha).setNormal(posestack$pose, 0.0F, 1.0F, 0.0F);
        pBuffer.addVertex(posestack$pose, f, f1, f2).setColor(pRed2, pGreen2, pBlue, pAlpha).setNormal(posestack$pose, 0.0F, 0.0F, 1.0F);
        pBuffer.addVertex(posestack$pose, f, f1, f5).setColor(pRed2, pGreen2, pBlue, pAlpha).setNormal(posestack$pose, 0.0F, 0.0F, 1.0F);
        pBuffer.addVertex(posestack$pose, f3, f1, f2).setColor(pRed, pGreen, pBlue, pAlpha).setNormal(posestack$pose, 0.0F, 1.0F, 0.0F);
        pBuffer.addVertex(posestack$pose, f3, f4, f2).setColor(pRed, pGreen, pBlue, pAlpha).setNormal(posestack$pose, 0.0F, 1.0F, 0.0F);
        pBuffer.addVertex(posestack$pose, f3, f4, f2).setColor(pRed, pGreen, pBlue, pAlpha).setNormal(posestack$pose, -1.0F, 0.0F, 0.0F);
        pBuffer.addVertex(posestack$pose, f, f4, f2).setColor(pRed, pGreen, pBlue, pAlpha).setNormal(posestack$pose, -1.0F, 0.0F, 0.0F);
        pBuffer.addVertex(posestack$pose, f, f4, f2).setColor(pRed, pGreen, pBlue, pAlpha).setNormal(posestack$pose, 0.0F, 0.0F, 1.0F);
        pBuffer.addVertex(posestack$pose, f, f4, f5).setColor(pRed, pGreen, pBlue, pAlpha).setNormal(posestack$pose, 0.0F, 0.0F, 1.0F);
        pBuffer.addVertex(posestack$pose, f, f4, f5).setColor(pRed, pGreen, pBlue, pAlpha).setNormal(posestack$pose, 0.0F, -1.0F, 0.0F);
        pBuffer.addVertex(posestack$pose, f, f1, f5).setColor(pRed, pGreen, pBlue, pAlpha).setNormal(posestack$pose, 0.0F, -1.0F, 0.0F);
        pBuffer.addVertex(posestack$pose, f, f1, f5).setColor(pRed, pGreen, pBlue, pAlpha).setNormal(posestack$pose, 1.0F, 0.0F, 0.0F);
        pBuffer.addVertex(posestack$pose, f3, f1, f5).setColor(pRed, pGreen, pBlue, pAlpha).setNormal(posestack$pose, 1.0F, 0.0F, 0.0F);
        pBuffer.addVertex(posestack$pose, f3, f1, f5).setColor(pRed, pGreen, pBlue, pAlpha).setNormal(posestack$pose, 0.0F, 0.0F, -1.0F);
        pBuffer.addVertex(posestack$pose, f3, f1, f2).setColor(pRed, pGreen, pBlue, pAlpha).setNormal(posestack$pose, 0.0F, 0.0F, -1.0F);
        pBuffer.addVertex(posestack$pose, f, f4, f5).setColor(pRed, pGreen, pBlue, pAlpha).setNormal(posestack$pose, 1.0F, 0.0F, 0.0F);
        pBuffer.addVertex(posestack$pose, f3, f4, f5).setColor(pRed, pGreen, pBlue, pAlpha).setNormal(posestack$pose, 1.0F, 0.0F, 0.0F);
        pBuffer.addVertex(posestack$pose, f3, f1, f5).setColor(pRed, pGreen, pBlue, pAlpha).setNormal(posestack$pose, 0.0F, 1.0F, 0.0F);
        pBuffer.addVertex(posestack$pose, f3, f4, f5).setColor(pRed, pGreen, pBlue, pAlpha).setNormal(posestack$pose, 0.0F, 1.0F, 0.0F);
        pBuffer.addVertex(posestack$pose, f3, f4, f2).setColor(pRed, pGreen, pBlue, pAlpha).setNormal(posestack$pose, 0.0F, 0.0F, 1.0F);
        pBuffer.addVertex(posestack$pose, f3, f4, f5).setColor(pRed, pGreen, pBlue, pAlpha).setNormal(posestack$pose, 0.0F, 0.0F, 1.0F);
    }

    public static void addChainedFilledBoxVertices(
        PoseStack pPoseStack,
        VertexConsumer pBuffer,
        double pMinX,
        double pMinY,
        double pMinZ,
        double pMaxX,
        double pMaxY,
        double pMaxZ,
        float pRed,
        float pGreen,
        float pBlue,
        float pAlpha
    ) {
        addChainedFilledBoxVertices(
            pPoseStack,
            pBuffer,
            (float)pMinX,
            (float)pMinY,
            (float)pMinZ,
            (float)pMaxX,
            (float)pMaxY,
            (float)pMaxZ,
            pRed,
            pGreen,
            pBlue,
            pAlpha
        );
    }

    public static void addChainedFilledBoxVertices(
        PoseStack pPoseStack,
        VertexConsumer pBuffer,
        float pMinX,
        float pMinY,
        float pMinZ,
        float pMaxX,
        float pMaxY,
        float pMaxZ,
        float pRed,
        float pGreen,
        float pBlue,
        float pAlpha
    ) {
        Matrix4f matrix4f = pPoseStack.last().pose();
        pBuffer.addVertex(matrix4f, pMinX, pMinY, pMinZ).setColor(pRed, pGreen, pBlue, pAlpha);
        pBuffer.addVertex(matrix4f, pMinX, pMinY, pMinZ).setColor(pRed, pGreen, pBlue, pAlpha);
        pBuffer.addVertex(matrix4f, pMinX, pMinY, pMinZ).setColor(pRed, pGreen, pBlue, pAlpha);
        pBuffer.addVertex(matrix4f, pMinX, pMinY, pMaxZ).setColor(pRed, pGreen, pBlue, pAlpha);
        pBuffer.addVertex(matrix4f, pMinX, pMaxY, pMinZ).setColor(pRed, pGreen, pBlue, pAlpha);
        pBuffer.addVertex(matrix4f, pMinX, pMaxY, pMaxZ).setColor(pRed, pGreen, pBlue, pAlpha);
        pBuffer.addVertex(matrix4f, pMinX, pMaxY, pMaxZ).setColor(pRed, pGreen, pBlue, pAlpha);
        pBuffer.addVertex(matrix4f, pMinX, pMinY, pMaxZ).setColor(pRed, pGreen, pBlue, pAlpha);
        pBuffer.addVertex(matrix4f, pMaxX, pMaxY, pMaxZ).setColor(pRed, pGreen, pBlue, pAlpha);
        pBuffer.addVertex(matrix4f, pMaxX, pMinY, pMaxZ).setColor(pRed, pGreen, pBlue, pAlpha);
        pBuffer.addVertex(matrix4f, pMaxX, pMinY, pMaxZ).setColor(pRed, pGreen, pBlue, pAlpha);
        pBuffer.addVertex(matrix4f, pMaxX, pMinY, pMinZ).setColor(pRed, pGreen, pBlue, pAlpha);
        pBuffer.addVertex(matrix4f, pMaxX, pMaxY, pMaxZ).setColor(pRed, pGreen, pBlue, pAlpha);
        pBuffer.addVertex(matrix4f, pMaxX, pMaxY, pMinZ).setColor(pRed, pGreen, pBlue, pAlpha);
        pBuffer.addVertex(matrix4f, pMaxX, pMaxY, pMinZ).setColor(pRed, pGreen, pBlue, pAlpha);
        pBuffer.addVertex(matrix4f, pMaxX, pMinY, pMinZ).setColor(pRed, pGreen, pBlue, pAlpha);
        pBuffer.addVertex(matrix4f, pMinX, pMaxY, pMinZ).setColor(pRed, pGreen, pBlue, pAlpha);
        pBuffer.addVertex(matrix4f, pMinX, pMinY, pMinZ).setColor(pRed, pGreen, pBlue, pAlpha);
        pBuffer.addVertex(matrix4f, pMinX, pMinY, pMinZ).setColor(pRed, pGreen, pBlue, pAlpha);
        pBuffer.addVertex(matrix4f, pMaxX, pMinY, pMinZ).setColor(pRed, pGreen, pBlue, pAlpha);
        pBuffer.addVertex(matrix4f, pMinX, pMinY, pMaxZ).setColor(pRed, pGreen, pBlue, pAlpha);
        pBuffer.addVertex(matrix4f, pMaxX, pMinY, pMaxZ).setColor(pRed, pGreen, pBlue, pAlpha);
        pBuffer.addVertex(matrix4f, pMaxX, pMinY, pMaxZ).setColor(pRed, pGreen, pBlue, pAlpha);
        pBuffer.addVertex(matrix4f, pMinX, pMaxY, pMinZ).setColor(pRed, pGreen, pBlue, pAlpha);
        pBuffer.addVertex(matrix4f, pMinX, pMaxY, pMinZ).setColor(pRed, pGreen, pBlue, pAlpha);
        pBuffer.addVertex(matrix4f, pMinX, pMaxY, pMaxZ).setColor(pRed, pGreen, pBlue, pAlpha);
        pBuffer.addVertex(matrix4f, pMaxX, pMaxY, pMinZ).setColor(pRed, pGreen, pBlue, pAlpha);
        pBuffer.addVertex(matrix4f, pMaxX, pMaxY, pMaxZ).setColor(pRed, pGreen, pBlue, pAlpha);
        pBuffer.addVertex(matrix4f, pMaxX, pMaxY, pMaxZ).setColor(pRed, pGreen, pBlue, pAlpha);
        pBuffer.addVertex(matrix4f, pMaxX, pMaxY, pMaxZ).setColor(pRed, pGreen, pBlue, pAlpha);
    }

    public static void renderFace(
        PoseStack pPoseStack,
        VertexConsumer pBuffer,
        Direction pFace,
        float pX1,
        float pY1,
        float pZ1,
        float pX2,
        float pY2,
        float pZ2,
        float pRed,
        float pGreen,
        float pBlue,
        float pAlpha
    ) {
        Matrix4f matrix4f = pPoseStack.last().pose();
        switch (pFace) {
            case DOWN:
                pBuffer.addVertex(matrix4f, pX1, pY1, pZ1).setColor(pRed, pGreen, pBlue, pAlpha);
                pBuffer.addVertex(matrix4f, pX2, pY1, pZ1).setColor(pRed, pGreen, pBlue, pAlpha);
                pBuffer.addVertex(matrix4f, pX2, pY1, pZ2).setColor(pRed, pGreen, pBlue, pAlpha);
                pBuffer.addVertex(matrix4f, pX1, pY1, pZ2).setColor(pRed, pGreen, pBlue, pAlpha);
                break;
            case UP:
                pBuffer.addVertex(matrix4f, pX1, pY2, pZ1).setColor(pRed, pGreen, pBlue, pAlpha);
                pBuffer.addVertex(matrix4f, pX1, pY2, pZ2).setColor(pRed, pGreen, pBlue, pAlpha);
                pBuffer.addVertex(matrix4f, pX2, pY2, pZ2).setColor(pRed, pGreen, pBlue, pAlpha);
                pBuffer.addVertex(matrix4f, pX2, pY2, pZ1).setColor(pRed, pGreen, pBlue, pAlpha);
                break;
            case NORTH:
                pBuffer.addVertex(matrix4f, pX1, pY1, pZ1).setColor(pRed, pGreen, pBlue, pAlpha);
                pBuffer.addVertex(matrix4f, pX1, pY2, pZ1).setColor(pRed, pGreen, pBlue, pAlpha);
                pBuffer.addVertex(matrix4f, pX2, pY2, pZ1).setColor(pRed, pGreen, pBlue, pAlpha);
                pBuffer.addVertex(matrix4f, pX2, pY1, pZ1).setColor(pRed, pGreen, pBlue, pAlpha);
                break;
            case SOUTH:
                pBuffer.addVertex(matrix4f, pX1, pY1, pZ2).setColor(pRed, pGreen, pBlue, pAlpha);
                pBuffer.addVertex(matrix4f, pX2, pY1, pZ2).setColor(pRed, pGreen, pBlue, pAlpha);
                pBuffer.addVertex(matrix4f, pX2, pY2, pZ2).setColor(pRed, pGreen, pBlue, pAlpha);
                pBuffer.addVertex(matrix4f, pX1, pY2, pZ2).setColor(pRed, pGreen, pBlue, pAlpha);
                break;
            case WEST:
                pBuffer.addVertex(matrix4f, pX1, pY1, pZ1).setColor(pRed, pGreen, pBlue, pAlpha);
                pBuffer.addVertex(matrix4f, pX1, pY1, pZ2).setColor(pRed, pGreen, pBlue, pAlpha);
                pBuffer.addVertex(matrix4f, pX1, pY2, pZ2).setColor(pRed, pGreen, pBlue, pAlpha);
                pBuffer.addVertex(matrix4f, pX1, pY2, pZ1).setColor(pRed, pGreen, pBlue, pAlpha);
                break;
            case EAST:
                pBuffer.addVertex(matrix4f, pX2, pY1, pZ1).setColor(pRed, pGreen, pBlue, pAlpha);
                pBuffer.addVertex(matrix4f, pX2, pY2, pZ1).setColor(pRed, pGreen, pBlue, pAlpha);
                pBuffer.addVertex(matrix4f, pX2, pY2, pZ2).setColor(pRed, pGreen, pBlue, pAlpha);
                pBuffer.addVertex(matrix4f, pX2, pY1, pZ2).setColor(pRed, pGreen, pBlue, pAlpha);
        }
    }

    public static void renderVector(PoseStack pPoseStack, VertexConsumer pBuffer, Vector3f pStartPos, Vec3 pVector, int pColor) {
        PoseStack.Pose posestack$pose = pPoseStack.last();
        pBuffer.addVertex(posestack$pose, pStartPos)
            .setColor(pColor)
            .setNormal(posestack$pose, (float)pVector.x, (float)pVector.y, (float)pVector.z);
        pBuffer.addVertex(
                posestack$pose,
                (float)(pStartPos.x() + pVector.x),
                (float)(pStartPos.y() + pVector.y),
                (float)(pStartPos.z() + pVector.z)
            )
            .setColor(pColor)
            .setNormal(posestack$pose, (float)pVector.x, (float)pVector.y, (float)pVector.z);
    }
}