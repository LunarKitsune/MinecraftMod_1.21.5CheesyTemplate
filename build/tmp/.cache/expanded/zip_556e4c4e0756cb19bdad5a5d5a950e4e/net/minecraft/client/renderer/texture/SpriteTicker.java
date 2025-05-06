package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface SpriteTicker extends AutoCloseable {
    void tickAndUpload(int pX, int pY, GpuTexture pTexture);

    @Override
    void close();
}