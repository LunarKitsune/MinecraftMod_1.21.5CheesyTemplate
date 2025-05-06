package net.minecraft.client.renderer.debug;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WorldGenAttemptRenderer implements DebugRenderer.SimpleDebugRenderer {
    private final List<BlockPos> toRender = Lists.newArrayList();
    private final List<Float> scales = Lists.newArrayList();
    private final List<Float> alphas = Lists.newArrayList();
    private final List<Float> reds = Lists.newArrayList();
    private final List<Float> greens = Lists.newArrayList();
    private final List<Float> blues = Lists.newArrayList();

    public void addPos(BlockPos pPos, float pScale, float pRed, float pGreen, float pBlue, float pAlpha) {
        this.toRender.add(pPos);
        this.scales.add(pScale);
        this.alphas.add(pAlpha);
        this.reds.add(pRed);
        this.greens.add(pGreen);
        this.blues.add(pBlue);
    }

    @Override
    public void render(PoseStack p_113732_, MultiBufferSource p_113733_, double p_113734_, double p_113735_, double p_113736_) {
        VertexConsumer vertexconsumer = p_113733_.getBuffer(RenderType.debugFilledBox());

        for (int i = 0; i < this.toRender.size(); i++) {
            BlockPos blockpos = this.toRender.get(i);
            Float f = this.scales.get(i);
            float f1 = f / 2.0F;
            ShapeRenderer.addChainedFilledBoxVertices(
                p_113732_,
                vertexconsumer,
                blockpos.getX() + 0.5F - f1 - p_113734_,
                blockpos.getY() + 0.5F - f1 - p_113735_,
                blockpos.getZ() + 0.5F - f1 - p_113736_,
                blockpos.getX() + 0.5F + f1 - p_113734_,
                blockpos.getY() + 0.5F + f1 - p_113735_,
                blockpos.getZ() + 0.5F + f1 - p_113736_,
                this.reds.get(i),
                this.greens.get(i),
                this.blues.get(i),
                this.alphas.get(i)
            );
        }
    }
}