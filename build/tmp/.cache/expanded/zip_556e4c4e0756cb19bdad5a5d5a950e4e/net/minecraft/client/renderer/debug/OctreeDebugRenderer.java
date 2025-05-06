package net.minecraft.client.renderer.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Octree;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.mutable.MutableInt;

@OnlyIn(Dist.CLIENT)
public class OctreeDebugRenderer {
    private final Minecraft minecraft;

    public OctreeDebugRenderer(Minecraft pMinecraft) {
        this.minecraft = pMinecraft;
    }

    public void render(PoseStack pPoseStack, Frustum pFrustum, MultiBufferSource pBufferSource, double pCamX, double pCamY, double pCamZ) {
        Octree octree = this.minecraft.levelRenderer.getSectionOcclusionGraph().getOctree();
        MutableInt mutableint = new MutableInt(0);
        octree.visitNodes(
            (p_367461_, p_361624_, p_368817_, p_363024_) -> this.renderNode(
                p_367461_, pPoseStack, pBufferSource, pCamX, pCamY, pCamZ, p_368817_, p_361624_, mutableint, p_363024_
            ),
            pFrustum,
            32
        );
    }

    private void renderNode(
        Octree.Node pNode,
        PoseStack pPoseStack,
        MultiBufferSource pBufferSource,
        double pCamX,
        double pCamY,
        double pCamZ,
        int pRecursionDepth,
        boolean pIsLeafNode,
        MutableInt pNodesRendered,
        boolean pIsNearby
    ) {
        AABB aabb = pNode.getAABB();
        double d0 = aabb.getXsize();
        long i = Math.round(d0 / 16.0);
        if (i == 1L) {
            pNodesRendered.add(1);
            double d1 = aabb.getCenter().x;
            double d2 = aabb.getCenter().y;
            double d3 = aabb.getCenter().z;
            int k = pIsNearby ? -16711936 : -1;
            DebugRenderer.renderFloatingText(pPoseStack, pBufferSource, String.valueOf(pNodesRendered.getValue()), d1, d2, d3, k, 0.3F);
        }

        VertexConsumer vertexconsumer = pBufferSource.getBuffer(RenderType.lines());
        long j = i + 5L;
        ShapeRenderer.renderLineBox(
            pPoseStack,
            vertexconsumer,
            aabb.deflate(0.1 * pRecursionDepth).move(-pCamX, -pCamY, -pCamZ),
            getColorComponent(j, 0.3F),
            getColorComponent(j, 0.8F),
            getColorComponent(j, 0.5F),
            pIsLeafNode ? 0.4F : 1.0F
        );
    }

    private static float getColorComponent(long pValue, float pMultiplier) {
        float f = 0.1F;
        return Mth.frac(pMultiplier * (float)pValue) * 0.9F + 0.1F;
    }
}