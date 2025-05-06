package net.minecraft.client.renderer.block.model;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.QuadCollection;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record SimpleModelWrapper(QuadCollection quads, boolean useAmbientOcclusion, TextureAtlasSprite particleIcon) implements BlockModelPart {
    public static SimpleModelWrapper bake(ModelBaker pBaker, ResourceLocation pModelLocation, ModelState pModelState) {
        ResolvedModel resolvedmodel = pBaker.getModel(pModelLocation);
        TextureSlots textureslots = resolvedmodel.getTopTextureSlots();
        boolean flag = resolvedmodel.getTopAmbientOcclusion();
        TextureAtlasSprite textureatlassprite = resolvedmodel.resolveParticleSprite(textureslots, pBaker);
        QuadCollection quadcollection = resolvedmodel.bakeTopGeometry(textureslots, pBaker, pModelState);
        return new SimpleModelWrapper(quadcollection, flag, textureatlassprite);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable Direction p_395134_) {
        return this.quads.getQuads(p_395134_);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return this.useAmbientOcclusion;
    }

    @Override
    public TextureAtlasSprite particleIcon() {
        return this.particleIcon;
    }
}