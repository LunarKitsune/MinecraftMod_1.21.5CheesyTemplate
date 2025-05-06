package net.minecraft.client.renderer.item;

import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record ModelRenderProperties(boolean usesBlockLight, TextureAtlasSprite particleIcon, ItemTransforms transforms) {
    public static ModelRenderProperties fromResolvedModel(ModelBaker pBaker, ResolvedModel pModel, TextureSlots pTextureSlots) {
        TextureAtlasSprite textureatlassprite = pModel.resolveParticleSprite(pTextureSlots, pBaker);
        return new ModelRenderProperties(pModel.getTopGuiLight().lightLikeBlock(), textureatlassprite, pModel.getTopTransforms());
    }

    public void applyToLayer(ItemStackRenderState.LayerRenderState pRenderState, ItemDisplayContext pDisplayContext) {
        pRenderState.setUsesBlockLight(this.usesBlockLight);
        pRenderState.setParticleIcon(this.particleIcon);
        pRenderState.setTransform(this.transforms.getTransform(pDisplayContext));
    }
}