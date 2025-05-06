package net.minecraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Map;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SpecialBlockModelRenderer {
    public static final SpecialBlockModelRenderer EMPTY = new SpecialBlockModelRenderer(Map.of());
    private final Map<Block, SpecialModelRenderer<?>> renderers;

    public SpecialBlockModelRenderer(Map<Block, SpecialModelRenderer<?>> pRenderers) {
        this.renderers = pRenderers;
    }

    public static SpecialBlockModelRenderer vanilla(EntityModelSet pModelSet) {
        return new SpecialBlockModelRenderer(SpecialModelRenderers.createBlockRenderers(pModelSet));
    }

    public void renderByBlock(Block pBlock, ItemDisplayContext pDisplayContext, PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, int pPackedOverlay) {
        SpecialModelRenderer<?> specialmodelrenderer = this.renderers.get(pBlock);
        if (specialmodelrenderer != null) {
            specialmodelrenderer.render(null, pDisplayContext, pPoseStack, pBufferSource, pPackedLight, pPackedOverlay, false);
        }
    }
}