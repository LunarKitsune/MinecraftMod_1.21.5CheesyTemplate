package net.minecraft.client.renderer.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import net.minecraft.client.renderer.chunk.SectionRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Vector4f;

@OnlyIn(Dist.CLIENT)
public class ChunkCullingDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
    public static final Direction[] DIRECTIONS = Direction.values();
    private final Minecraft minecraft;

    public ChunkCullingDebugRenderer(Minecraft pMinecraft) {
        this.minecraft = pMinecraft;
    }

    @Override
    public void render(PoseStack p_360717_, MultiBufferSource p_369170_, double p_361626_, double p_369161_, double p_367114_) {
        LevelRenderer levelrenderer = this.minecraft.levelRenderer;
        if (this.minecraft.sectionPath || this.minecraft.sectionVisibility) {
            SectionOcclusionGraph sectionocclusiongraph = levelrenderer.getSectionOcclusionGraph();

            for (SectionRenderDispatcher.RenderSection sectionrenderdispatcher$rendersection : levelrenderer.getVisibleSections()) {
                SectionOcclusionGraph.Node sectionocclusiongraph$node = sectionocclusiongraph.getNode(sectionrenderdispatcher$rendersection);
                if (sectionocclusiongraph$node != null) {
                    BlockPos blockpos = sectionrenderdispatcher$rendersection.getRenderOrigin();
                    p_360717_.pushPose();
                    p_360717_.translate(blockpos.getX() - p_361626_, blockpos.getY() - p_369161_, blockpos.getZ() - p_367114_);
                    Matrix4f matrix4f = p_360717_.last().pose();
                    if (this.minecraft.sectionPath) {
                        VertexConsumer vertexconsumer = p_369170_.getBuffer(RenderType.lines());
                        int i = sectionocclusiongraph$node.step == 0 ? 0 : Mth.hsvToRgb(sectionocclusiongraph$node.step / 50.0F, 0.9F, 0.9F);
                        int j = i >> 16 & 0xFF;
                        int k = i >> 8 & 0xFF;
                        int l = i & 0xFF;

                        for (int i1 = 0; i1 < DIRECTIONS.length; i1++) {
                            if (sectionocclusiongraph$node.hasSourceDirection(i1)) {
                                Direction direction = DIRECTIONS[i1];
                                vertexconsumer.addVertex(matrix4f, 8.0F, 8.0F, 8.0F)
                                    .setColor(j, k, l, 255)
                                    .setNormal(direction.getStepX(), direction.getStepY(), direction.getStepZ());
                                vertexconsumer.addVertex(
                                        matrix4f, 8 - 16 * direction.getStepX(), 8 - 16 * direction.getStepY(), 8 - 16 * direction.getStepZ()
                                    )
                                    .setColor(j, k, l, 255)
                                    .setNormal(direction.getStepX(), direction.getStepY(), direction.getStepZ());
                            }
                        }
                    }

                    if (this.minecraft.sectionVisibility && sectionrenderdispatcher$rendersection.getCompiled().hasRenderableLayers()) {
                        VertexConsumer vertexconsumer3 = p_369170_.getBuffer(RenderType.lines());
                        int j1 = 0;

                        for (Direction direction2 : DIRECTIONS) {
                            for (Direction direction1 : DIRECTIONS) {
                                boolean flag = sectionrenderdispatcher$rendersection.getCompiled().facesCanSeeEachother(direction2, direction1);
                                if (!flag) {
                                    j1++;
                                    vertexconsumer3.addVertex(
                                            matrix4f, 8 + 8 * direction2.getStepX(), 8 + 8 * direction2.getStepY(), 8 + 8 * direction2.getStepZ()
                                        )
                                        .setColor(255, 0, 0, 255)
                                        .setNormal(direction2.getStepX(), direction2.getStepY(), direction2.getStepZ());
                                    vertexconsumer3.addVertex(
                                            matrix4f, 8 + 8 * direction1.getStepX(), 8 + 8 * direction1.getStepY(), 8 + 8 * direction1.getStepZ()
                                        )
                                        .setColor(255, 0, 0, 255)
                                        .setNormal(direction1.getStepX(), direction1.getStepY(), direction1.getStepZ());
                                }
                            }
                        }

                        if (j1 > 0) {
                            VertexConsumer vertexconsumer4 = p_369170_.getBuffer(RenderType.debugQuads());
                            float f = 0.5F;
                            float f1 = 0.2F;
                            vertexconsumer4.addVertex(matrix4f, 0.5F, 15.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 15.5F, 15.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 15.5F, 15.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 0.5F, 15.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 0.5F, 0.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 15.5F, 0.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 15.5F, 0.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 0.5F, 0.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 0.5F, 15.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 0.5F, 15.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 0.5F, 0.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 0.5F, 0.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 15.5F, 0.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 15.5F, 0.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 15.5F, 15.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 15.5F, 15.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 0.5F, 0.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 15.5F, 0.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 15.5F, 15.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 0.5F, 15.5F, 0.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 0.5F, 15.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 15.5F, 15.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 15.5F, 0.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                            vertexconsumer4.addVertex(matrix4f, 0.5F, 0.5F, 15.5F).setColor(0.9F, 0.9F, 0.0F, 0.2F);
                        }
                    }

                    p_360717_.popPose();
                }
            }
        }

        Frustum frustum = levelrenderer.getCapturedFrustum();
        if (frustum != null) {
            p_360717_.pushPose();
            p_360717_.translate((float)(frustum.getCamX() - p_361626_), (float)(frustum.getCamY() - p_369161_), (float)(frustum.getCamZ() - p_367114_));
            Matrix4f matrix4f1 = p_360717_.last().pose();
            Vector4f[] avector4f = frustum.getFrustumPoints();
            VertexConsumer vertexconsumer1 = p_369170_.getBuffer(RenderType.debugQuads());
            this.addFrustumQuad(vertexconsumer1, matrix4f1, avector4f, 0, 1, 2, 3, 0, 1, 1);
            this.addFrustumQuad(vertexconsumer1, matrix4f1, avector4f, 4, 5, 6, 7, 1, 0, 0);
            this.addFrustumQuad(vertexconsumer1, matrix4f1, avector4f, 0, 1, 5, 4, 1, 1, 0);
            this.addFrustumQuad(vertexconsumer1, matrix4f1, avector4f, 2, 3, 7, 6, 0, 0, 1);
            this.addFrustumQuad(vertexconsumer1, matrix4f1, avector4f, 0, 4, 7, 3, 0, 1, 0);
            this.addFrustumQuad(vertexconsumer1, matrix4f1, avector4f, 1, 5, 6, 2, 1, 0, 1);
            VertexConsumer vertexconsumer2 = p_369170_.getBuffer(RenderType.lines());
            this.addFrustumVertex(vertexconsumer2, matrix4f1, avector4f[0]);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, avector4f[1]);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, avector4f[1]);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, avector4f[2]);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, avector4f[2]);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, avector4f[3]);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, avector4f[3]);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, avector4f[0]);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, avector4f[4]);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, avector4f[5]);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, avector4f[5]);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, avector4f[6]);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, avector4f[6]);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, avector4f[7]);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, avector4f[7]);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, avector4f[4]);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, avector4f[0]);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, avector4f[4]);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, avector4f[1]);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, avector4f[5]);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, avector4f[2]);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, avector4f[6]);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, avector4f[3]);
            this.addFrustumVertex(vertexconsumer2, matrix4f1, avector4f[7]);
            p_360717_.popPose();
        }
    }

    private void addFrustumVertex(VertexConsumer pBuffer, Matrix4f pPose, Vector4f pPosition) {
        pBuffer.addVertex(pPose, pPosition.x(), pPosition.y(), pPosition.z()).setColor(-16777216).setNormal(0.0F, 0.0F, -1.0F);
    }

    private void addFrustumQuad(
        VertexConsumer pBuffer,
        Matrix4f pPose,
        Vector4f[] pFrustumPoints,
        int pPoint1,
        int pPoint2,
        int pPoint3,
        int pPoint4,
        int pRed,
        int pGreen,
        int pBlue
    ) {
        float f = 0.25F;
        pBuffer.addVertex(pPose, pFrustumPoints[pPoint1].x(), pFrustumPoints[pPoint1].y(), pFrustumPoints[pPoint1].z())
            .setColor(pRed, pGreen, pBlue, 0.25F);
        pBuffer.addVertex(pPose, pFrustumPoints[pPoint2].x(), pFrustumPoints[pPoint2].y(), pFrustumPoints[pPoint2].z())
            .setColor(pRed, pGreen, pBlue, 0.25F);
        pBuffer.addVertex(pPose, pFrustumPoints[pPoint3].x(), pFrustumPoints[pPoint3].y(), pFrustumPoints[pPoint3].z())
            .setColor(pRed, pGreen, pBlue, 0.25F);
        pBuffer.addVertex(pPose, pFrustumPoints[pPoint4].x(), pFrustumPoints[pPoint4].y(), pFrustumPoints[pPoint4].z())
            .setColor(pRed, pGreen, pBlue, 0.25F);
    }
}