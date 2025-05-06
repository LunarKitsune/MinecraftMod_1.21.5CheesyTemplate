package net.minecraft.client.renderer.debug;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Collection;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RaidDebugRenderer implements DebugRenderer.SimpleDebugRenderer {
    private static final int MAX_RENDER_DIST = 160;
    private static final float TEXT_SCALE = 0.04F;
    private final Minecraft minecraft;
    private Collection<BlockPos> raidCenters = Lists.newArrayList();

    public RaidDebugRenderer(Minecraft pMinecraft) {
        this.minecraft = pMinecraft;
    }

    public void setRaidCenters(Collection<BlockPos> pRaidCenters) {
        this.raidCenters = pRaidCenters;
    }

    @Override
    public void render(PoseStack p_113652_, MultiBufferSource p_113653_, double p_113654_, double p_113655_, double p_113656_) {
        BlockPos blockpos = this.getCamera().getBlockPosition();

        for (BlockPos blockpos1 : this.raidCenters) {
            if (blockpos.closerThan(blockpos1, 160.0)) {
                highlightRaidCenter(p_113652_, p_113653_, blockpos1);
            }
        }
    }

    private static void highlightRaidCenter(PoseStack pPoseStack, MultiBufferSource pBuffer, BlockPos pPos) {
        DebugRenderer.renderFilledUnitCube(pPoseStack, pBuffer, pPos, 1.0F, 0.0F, 0.0F, 0.15F);
        renderTextOverBlock(pPoseStack, pBuffer, "Raid center", pPos, -65536);
    }

    private static void renderTextOverBlock(PoseStack pPoseStack, MultiBufferSource pBuffer, String pText, BlockPos pPos, int pColor) {
        double d0 = pPos.getX() + 0.5;
        double d1 = pPos.getY() + 1.3;
        double d2 = pPos.getZ() + 0.5;
        DebugRenderer.renderFloatingText(pPoseStack, pBuffer, pText, d0, d1, d2, pColor, 0.04F, true, 0.0F, true);
    }

    private Camera getCamera() {
        return this.minecraft.gameRenderer.getMainCamera();
    }
}