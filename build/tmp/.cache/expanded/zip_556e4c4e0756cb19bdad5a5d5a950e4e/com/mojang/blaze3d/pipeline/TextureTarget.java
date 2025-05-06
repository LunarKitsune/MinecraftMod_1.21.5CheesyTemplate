package com.mojang.blaze3d.pipeline;

import com.mojang.blaze3d.systems.RenderSystem;
import javax.annotation.Nullable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TextureTarget extends RenderTarget {
    public TextureTarget(@Nullable String pName, int pWidth, int pHeight, boolean pUseDepth) {
        super(pName, pUseDepth);
        RenderSystem.assertOnRenderThread();
        this.resize(pWidth, pHeight);
    }
}