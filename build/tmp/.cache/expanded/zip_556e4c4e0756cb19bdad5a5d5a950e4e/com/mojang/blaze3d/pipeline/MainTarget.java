package com.mojang.blaze3d.pipeline;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.GpuOutOfMemoryException;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MainTarget extends RenderTarget {
    public static final int DEFAULT_WIDTH = 854;
    public static final int DEFAULT_HEIGHT = 480;
    static final MainTarget.Dimension DEFAULT_DIMENSIONS = new MainTarget.Dimension(854, 480);

    public MainTarget(int pWidth, int pHeight) {
        super("Main", true);
        this.createFrameBuffer(pWidth, pHeight);
    }

    private void createFrameBuffer(int pWidth, int pHeight) {
        MainTarget.Dimension maintarget$dimension = this.allocateAttachments(pWidth, pHeight);
        if (this.colorTexture != null && this.depthTexture != null) {
            this.colorTexture.setTextureFilter(FilterMode.NEAREST, false);
            this.colorTexture.setAddressMode(AddressMode.CLAMP_TO_EDGE);
            this.colorTexture.setTextureFilter(FilterMode.NEAREST, false);
            this.colorTexture.setAddressMode(AddressMode.CLAMP_TO_EDGE);
            this.viewWidth = maintarget$dimension.width;
            this.viewHeight = maintarget$dimension.height;
            this.width = maintarget$dimension.width;
            this.height = maintarget$dimension.height;
        } else {
            throw new IllegalStateException("Missing color and/or depth textures");
        }
    }

    private MainTarget.Dimension allocateAttachments(int pWidth, int pHeight) {
        RenderSystem.assertOnRenderThread();

        for (MainTarget.Dimension maintarget$dimension : MainTarget.Dimension.listWithFallback(pWidth, pHeight)) {
            if (this.colorTexture != null) {
                this.colorTexture.close();
                this.colorTexture = null;
            }

            if (this.depthTexture != null) {
                this.depthTexture.close();
                this.depthTexture = null;
            }

            this.colorTexture = this.allocateColorAttachment(maintarget$dimension);
            this.depthTexture = this.allocateDepthAttachment(maintarget$dimension);
            if (this.colorTexture != null && this.depthTexture != null) {
                return maintarget$dimension;
            }
        }

        throw new RuntimeException(
            "Unrecoverable GL_OUT_OF_MEMORY ("
                + (this.colorTexture == null ? "missing color" : "have color")
                + ", "
                + (this.depthTexture == null ? "missing depth" : "have depth")
                + ")"
        );
    }

    @Nullable
    private GpuTexture allocateColorAttachment(MainTarget.Dimension pDimension) {
        try {
            return RenderSystem.getDevice().createTexture(() -> this.label + " / Color", TextureFormat.RGBA8, pDimension.width, pDimension.height, 1);
        } catch (GpuOutOfMemoryException gpuoutofmemoryexception) {
            return null;
        }
    }

    @Nullable
    private GpuTexture allocateDepthAttachment(MainTarget.Dimension pDimension) {
        try {
            return RenderSystem.getDevice()
                .createTexture(() -> this.label + " / Depth", TextureFormat.DEPTH32, pDimension.width, pDimension.height, 1);
        } catch (GpuOutOfMemoryException gpuoutofmemoryexception) {
            return null;
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class Dimension {
        public final int width;
        public final int height;

        Dimension(int pWidth, int pHeight) {
            this.width = pWidth;
            this.height = pHeight;
        }

        static List<MainTarget.Dimension> listWithFallback(int pWidth, int pHeight) {
            RenderSystem.assertOnRenderThread();
            int i = RenderSystem.getDevice().getMaxTextureSize();
            return pWidth > 0 && pWidth <= i && pHeight > 0 && pHeight <= i
                ? ImmutableList.of(new MainTarget.Dimension(pWidth, pHeight), MainTarget.DEFAULT_DIMENSIONS)
                : ImmutableList.of(MainTarget.DEFAULT_DIMENSIONS);
        }

        @Override
        public boolean equals(Object pOther) {
            if (this == pOther) {
                return true;
            } else if (pOther != null && this.getClass() == pOther.getClass()) {
                MainTarget.Dimension maintarget$dimension = (MainTarget.Dimension)pOther;
                return this.width == maintarget$dimension.width && this.height == maintarget$dimension.height;
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.width, this.height);
        }

        @Override
        public String toString() {
            return this.width + "x" + this.height;
        }
    }
}