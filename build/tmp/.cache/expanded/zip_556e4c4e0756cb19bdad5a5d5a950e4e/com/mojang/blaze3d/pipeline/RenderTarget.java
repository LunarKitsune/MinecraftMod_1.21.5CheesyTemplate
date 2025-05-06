package com.mojang.blaze3d.pipeline;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.OptionalInt;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class RenderTarget {
    private static int UNNAMED_RENDER_TARGETS = 0;
    public int width;
    public int height;
    public int viewWidth;
    public int viewHeight;
    protected final String label;
    public final boolean useDepth;
    @Nullable
    protected GpuTexture colorTexture;
    @Nullable
    protected GpuTexture depthTexture;
    public FilterMode filterMode;

    public RenderTarget(@Nullable String pName, boolean pUseDepth) {
        this.label = pName == null ? "FBO " + UNNAMED_RENDER_TARGETS++ : pName;
        this.useDepth = pUseDepth;
    }

    public void resize(int pWidth, int pHeight) {
        RenderSystem.assertOnRenderThread();
        this.destroyBuffers();
        this.createBuffers(pWidth, pHeight);
    }

    public void destroyBuffers() {
        RenderSystem.assertOnRenderThread();
        if (this.depthTexture != null) {
            this.depthTexture.close();
            this.depthTexture = null;
        }

        if (this.colorTexture != null) {
            this.colorTexture.close();
            this.colorTexture = null;
        }
    }

    public void copyDepthFrom(RenderTarget pOtherTarget) {
        RenderSystem.assertOnRenderThread();
        if (this.depthTexture == null) {
            throw new IllegalStateException("Trying to copy depth texture to a RenderTarget without a depth texture");
        } else if (pOtherTarget.depthTexture == null) {
            throw new IllegalStateException("Trying to copy depth texture from a RenderTarget without a depth texture");
        } else {
            RenderSystem.getDevice()
                .createCommandEncoder()
                .copyTextureToTexture(pOtherTarget.depthTexture, this.depthTexture, 0, 0, 0, 0, 0, this.width, this.height);
        }
    }

    public void createBuffers(int pWidth, int pHeight) {
        RenderSystem.assertOnRenderThread();
        int i = RenderSystem.getDevice().getMaxTextureSize();
        if (pWidth > 0 && pWidth <= i && pHeight > 0 && pHeight <= i) {
            this.viewWidth = pWidth;
            this.viewHeight = pHeight;
            this.width = pWidth;
            this.height = pHeight;
            if (this.useDepth) {
                this.depthTexture = RenderSystem.getDevice().createTexture(() -> this.label + " / Depth", TextureFormat.DEPTH32, pWidth, pHeight, 1, this.stencilEnabled);
                this.depthTexture.setTextureFilter(FilterMode.NEAREST, false);
                this.depthTexture.setAddressMode(AddressMode.CLAMP_TO_EDGE);
            }

            this.colorTexture = RenderSystem.getDevice().createTexture(() -> this.label + " / Color", TextureFormat.RGBA8, pWidth, pHeight, 1);
            this.colorTexture.setAddressMode(AddressMode.CLAMP_TO_EDGE);
            this.setFilterMode(FilterMode.NEAREST, true);
        } else {
            throw new IllegalArgumentException("Window " + pWidth + "x" + pHeight + " size out of bounds (max. size: " + i + ")");
        }
    }

    public void setFilterMode(FilterMode pFilterMode) {
        this.setFilterMode(pFilterMode, false);
    }

    private void setFilterMode(FilterMode pFilterMode, boolean pForce) {
        if (this.colorTexture == null) {
            throw new IllegalStateException("Can't change filter mode, color texture doesn't exist yet");
        } else {
            if (pForce || pFilterMode != this.filterMode) {
                this.filterMode = pFilterMode;
                this.colorTexture.setTextureFilter(pFilterMode, false);
            }
        }
    }

    public void blitToScreen() {
        if (this.colorTexture == null) {
            throw new IllegalStateException("Can't blit to screen, color texture doesn't exist yet");
        } else {
            RenderSystem.getDevice().createCommandEncoder().presentTexture(this.colorTexture);
        }
    }

    public void blitAndBlendToTexture(GpuTexture pTexture) {
        RenderSystem.assertOnRenderThread();
        RenderSystem.AutoStorageIndexBuffer rendersystem$autostorageindexbuffer = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
        GpuBuffer gpubuffer = rendersystem$autostorageindexbuffer.getBuffer(6);
        GpuBuffer gpubuffer1 = RenderSystem.getQuadVertexBuffer();

        try (RenderPass renderpass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(pTexture, OptionalInt.empty())) {
            renderpass.setPipeline(RenderPipelines.ENTITY_OUTLINE_BLIT);
            renderpass.setVertexBuffer(0, gpubuffer1);
            renderpass.setIndexBuffer(gpubuffer, rendersystem$autostorageindexbuffer.type());
            renderpass.bindSampler("InSampler", this.colorTexture);
            renderpass.drawIndexed(0, 6);
        }
    }

    @Nullable
    public GpuTexture getColorTexture() {
        return this.colorTexture;
    }

    @Nullable
    public GpuTexture getDepthTexture() {
        return this.depthTexture;
    }

    private boolean stencilEnabled = false;
    /**
     * Attempts to enable 8 bits of stencil buffer on this FrameBuffer.
     * Modders must call this directly to set things up.
     * This is to prevent the default cause where graphics cards do not support stencil bits.
     * <b>Make sure to call this on the main render thread!</b>
     */
    public void enableStencil() {
        if (stencilEnabled) return;
        stencilEnabled = true;
        this.resize(viewWidth, viewHeight);
    }

    /**
     * Returns wither or not this FBO has been successfully initialized with stencil bits.
     * If not, and a modder wishes it to be, they must call enableStencil.
     */
    public boolean isStencilEnabled() {
        return this.stencilEnabled;
    }
}
