package net.minecraft.client.renderer.texture;

import java.io.IOException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SimpleTexture extends ReloadableTexture {
    public SimpleTexture(ResourceLocation pLocation) {
        super(pLocation);
    }

    @Override
    public TextureContents loadContents(ResourceManager p_376679_) throws IOException {
        return TextureContents.load(p_376679_, this.resourceId());
    }
}