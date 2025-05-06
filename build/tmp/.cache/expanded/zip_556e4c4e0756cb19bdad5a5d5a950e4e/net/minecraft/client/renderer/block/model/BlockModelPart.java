package net.minecraft.client.renderer.block.model;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.core.Direction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface BlockModelPart {
    List<BakedQuad> getQuads(@Nullable Direction pDirection);

    boolean useAmbientOcclusion();

    TextureAtlasSprite particleIcon();

    @OnlyIn(Dist.CLIENT)
    public interface Unbaked extends ResolvableModel {
        BlockModelPart bake(ModelBaker pModelBaker);
    }
}