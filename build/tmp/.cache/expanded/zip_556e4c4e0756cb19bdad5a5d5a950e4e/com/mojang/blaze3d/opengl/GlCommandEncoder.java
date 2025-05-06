package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.buffers.BufferType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.IntList;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Collection;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ARGB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.opengl.GL11;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class GlCommandEncoder implements CommandEncoder {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final GlDevice device;
    private final int readFbo;
    private final int drawFbo;
    @Nullable
    private RenderPipeline lastPipeline;
    private boolean inRenderPass;
    @Nullable
    private GlProgram lastProgram;

    protected GlCommandEncoder(GlDevice pDevice) {
        this.device = pDevice;
        this.readFbo = pDevice.directStateAccess().createFrameBufferObject();
        this.drawFbo = pDevice.directStateAccess().createFrameBufferObject();
    }

    @Override
    public RenderPass createRenderPass(GpuTexture p_396916_, OptionalInt p_395809_) {
        return this.createRenderPass(p_396916_, p_395809_, null, OptionalDouble.empty());
    }

    @Override
    public RenderPass createRenderPass(GpuTexture p_394473_, OptionalInt p_391728_, @Nullable GpuTexture p_396844_, OptionalDouble p_395171_) {
        if (this.inRenderPass) {
            throw new IllegalStateException("Close the existing render pass before creating a new one!");
        } else {
            if (p_395171_.isPresent() && p_396844_ == null) {
                LOGGER.warn("Depth clear value was provided but no depth texture is being used");
            }

            if (p_394473_.isClosed()) {
                throw new IllegalStateException("Color texture is closed");
            } else if (p_396844_ != null && p_396844_.isClosed()) {
                throw new IllegalStateException("Depth texture is closed");
            } else {
                this.inRenderPass = true;
                int i = ((GlTexture)p_394473_).getFbo(this.device.directStateAccess(), p_396844_);
                GlStateManager._glBindFramebuffer(36160, i);
                int j = 0;
                if (p_391728_.isPresent()) {
                    int k = p_391728_.getAsInt();
                    GL11.glClearColor(ARGB.redFloat(k), ARGB.greenFloat(k), ARGB.blueFloat(k), ARGB.alphaFloat(k));
                    j |= 16384;
                }

                if (p_396844_ != null && p_395171_.isPresent()) {
                    GL11.glClearDepth(p_395171_.getAsDouble());
                    j |= 256;
                }

                if (j != 0) {
                    GlStateManager._disableScissorTest();
                    GlStateManager._depthMask(true);
                    GlStateManager._colorMask(true, true, true, true);
                    GlStateManager._clear(j);
                }

                GlStateManager._viewport(0, 0, p_394473_.getWidth(0), p_394473_.getHeight(0));
                this.lastPipeline = null;
                return new GlRenderPass(this, p_396844_ != null);
            }
        }
    }

    @Override
    public void clearColorTexture(GpuTexture p_394273_, int p_393834_) {
        if (this.inRenderPass) {
            throw new IllegalStateException("Close the existing render pass before creating a new one!");
        } else if (!p_394273_.getFormat().hasColorAspect()) {
            throw new IllegalStateException("Trying to clear a non-color texture as color");
        } else if (p_394273_.isClosed()) {
            throw new IllegalStateException("Color texture is closed");
        } else {
            this.device.directStateAccess().bindFrameBufferTextures(this.drawFbo, ((GlTexture)p_394273_).id, 0, 0, 36160);
            GL11.glClearColor(ARGB.redFloat(p_393834_), ARGB.greenFloat(p_393834_), ARGB.blueFloat(p_393834_), ARGB.alphaFloat(p_393834_));
            GlStateManager._disableScissorTest();
            GlStateManager._colorMask(true, true, true, true);
            GlStateManager._clear(16384);
            GlStateManager._glFramebufferTexture2D(36160, 36064, 3553, 0, 0);
            // Forge: if using stencil, clear it as well (3553 = GlConst.GL_TEXTURE_2D)
            if (p_394273_.isStencilEnabled())
                GlStateManager._glFramebufferTexture2D(GlConst.GL_FRAMEBUFFER, org.lwjgl.opengl.GL30.GL_STENCIL_ATTACHMENT, GlConst.GL_TEXTURE_2D, 0, 0);
            GlStateManager._glBindFramebuffer(36160, 0);
        }
    }

    @Override
    public void clearColorAndDepthTextures(GpuTexture p_393527_, int p_391700_, GpuTexture p_391582_, double p_393930_) {
        if (this.inRenderPass) {
            throw new IllegalStateException("Close the existing render pass before creating a new one!");
        } else if (!p_393527_.getFormat().hasColorAspect()) {
            throw new IllegalStateException("Trying to clear a non-color texture as color");
        } else if (!p_391582_.getFormat().hasDepthAspect()) {
            throw new IllegalStateException("Trying to clear a non-depth texture as depth");
        } else if (p_393527_.isClosed()) {
            throw new IllegalStateException("Color texture is closed");
        } else if (p_391582_.isClosed()) {
            throw new IllegalStateException("Depth texture is closed");
        } else {
            int i = ((GlTexture)p_393527_).getFbo(this.device.directStateAccess(), p_391582_);
            GlStateManager._glBindFramebuffer(36160, i);
            GlStateManager._disableScissorTest();
            GL11.glClearDepth(p_393930_);
            GL11.glClearColor(ARGB.redFloat(p_391700_), ARGB.greenFloat(p_391700_), ARGB.blueFloat(p_391700_), ARGB.alphaFloat(p_391700_));
            GlStateManager._depthMask(true);
            GlStateManager._colorMask(true, true, true, true);
            GlStateManager._clear(16640);
            GlStateManager._glBindFramebuffer(36160, 0);
        }
    }

    @Override
    public void clearDepthTexture(GpuTexture p_397307_, double p_397388_) {
        if (this.inRenderPass) {
            throw new IllegalStateException("Close the existing render pass before creating a new one!");
        } else if (!p_397307_.getFormat().hasDepthAspect()) {
            throw new IllegalStateException("Trying to clear a non-depth texture as depth");
        } else if (p_397307_.isClosed()) {
            throw new IllegalStateException("Depth texture is closed");
        } else {
            this.device.directStateAccess().bindFrameBufferTextures(this.drawFbo, 0, ((GlTexture)p_397307_).id, 0, 36160);
            GL11.glDrawBuffer(0);
            GL11.glClearDepth(p_397388_);
            GlStateManager._depthMask(true);
            GlStateManager._disableScissorTest();
            GlStateManager._clear(256);
            GL11.glDrawBuffer(36064);
            GlStateManager._glFramebufferTexture2D(36160, 36096, 3553, 0, 0);
            GlStateManager._glBindFramebuffer(36160, 0);
        }
    }

    @Override
    public void writeToBuffer(GpuBuffer p_396379_, ByteBuffer p_397249_, int p_395340_) {
        if (this.inRenderPass) {
            throw new IllegalStateException("Close the existing render pass before performing additional commands");
        } else {
            GlBuffer glbuffer = (GlBuffer)p_396379_;
            if (glbuffer.closed) {
                throw new IllegalStateException("Buffer already closed");
            } else if (!glbuffer.usage().isWritable()) {
                throw new IllegalStateException("Buffer is not writable");
            } else {
                int i = p_397249_.remaining();
                if (i + p_395340_ > glbuffer.size) {
                    throw new IllegalArgumentException(
                        "Cannot write more data than this buffer can hold (attempting to write "
                            + i
                            + " bytes at offset "
                            + p_395340_
                            + " to "
                            + glbuffer.size
                            + " size buffer)"
                    );
                } else {
                    GlStateManager._glBindBuffer(GlConst.toGl(glbuffer.type()), glbuffer.handle);
                    if (glbuffer.initialized) {
                        GlStateManager._glBufferSubData(GlConst.toGl(glbuffer.type()), p_395340_, p_397249_);
                    } else if (p_395340_ == 0 && i == glbuffer.size) {
                        GlStateManager._glBufferData(GlConst.toGl(glbuffer.type()), p_397249_, GlConst.toGl(glbuffer.usage()));
                        GlBuffer.MEMORY_POOl.malloc(glbuffer.handle, glbuffer.size);
                        glbuffer.initialized = true;
                        this.device.debugLabels().applyLabel(glbuffer);
                    } else {
                        GlStateManager._glBufferData(GlConst.toGl(glbuffer.type()), glbuffer.size, GlConst.toGl(glbuffer.usage()));
                        GlStateManager._glBufferSubData(GlConst.toGl(glbuffer.type()), p_395340_, p_397249_);
                        GlBuffer.MEMORY_POOl.malloc(glbuffer.handle, glbuffer.size);
                        glbuffer.initialized = true;
                        this.device.debugLabels().applyLabel(glbuffer);
                    }
                }
            }
        }
    }

    @Override
    public GpuBuffer.ReadView readBuffer(GpuBuffer p_392232_) {
        return this.readBuffer(p_392232_, 0, p_392232_.size());
    }

    @Override
    public GpuBuffer.ReadView readBuffer(GpuBuffer p_398035_, int p_396139_, int p_394074_) {
        if (this.inRenderPass) {
            throw new IllegalStateException("Close the existing render pass before performing additional commands");
        } else {
            GlBuffer glbuffer = (GlBuffer)p_398035_;
            if (glbuffer.closed) {
                throw new IllegalStateException("Buffer already closed");
            } else if (!glbuffer.usage().isReadable()) {
                throw new IllegalStateException("Buffer is not readable");
            } else if (p_396139_ + p_394074_ > glbuffer.size) {
                throw new IllegalArgumentException(
                    "Cannot read more data than this buffer can hold (attempting to read "
                        + p_394074_
                        + " bytes at offset "
                        + p_396139_
                        + " from "
                        + glbuffer.size
                        + " size buffer)"
                );
            } else {
                GlStateManager.clearGlErrors();
                GlStateManager._glBindBuffer(GlConst.toGl(glbuffer.type()), glbuffer.handle);
                ByteBuffer bytebuffer = GlStateManager._glMapBufferRange(GlConst.toGl(glbuffer.type()), p_396139_, p_394074_, 1);
                if (bytebuffer == null) {
                    throw new IllegalStateException("Can't read buffer, opengl error " + GlStateManager._getError());
                } else {
                    return new GlBuffer.ReadView(GlConst.toGl(glbuffer.type()), bytebuffer);
                }
            }
        }
    }

    @Override
    public void writeToTexture(GpuTexture p_394020_, NativeImage p_396595_) {
        int i = p_394020_.getWidth(0);
        int j = p_394020_.getHeight(0);
        if (p_396595_.getWidth() != i || p_396595_.getHeight() != j) {
            throw new IllegalArgumentException(
                "Cannot replace texture of size " + i + "x" + j + " with image of size " + p_396595_.getWidth() + "x" + p_396595_.getHeight()
            );
        } else if (p_394020_.isClosed()) {
            throw new IllegalStateException("Destination texture is closed");
        } else {
            this.writeToTexture(p_394020_, p_396595_, 0, 0, 0, i, j, 0, 0);
        }
    }

    @Override
    public void writeToTexture(
        GpuTexture p_395107_, NativeImage p_392321_, int p_394222_, int p_396221_, int p_392746_, int p_391501_, int p_397458_, int p_397527_, int p_392683_
    ) {
        if (this.inRenderPass) {
            throw new IllegalStateException("Close the existing render pass before performing additional commands");
        } else if (p_394222_ >= 0 && p_394222_ < p_395107_.getMipLevels()) {
            if (p_397527_ + p_391501_ > p_392321_.getWidth() || p_392683_ + p_397458_ > p_392321_.getHeight()) {
                throw new IllegalArgumentException(
                    "Copy source ("
                        + p_392321_.getWidth()
                        + "x"
                        + p_392321_.getHeight()
                        + ") is not large enough to read a rectangle of "
                        + p_391501_
                        + "x"
                        + p_397458_
                        + " from "
                        + p_397527_
                        + "x"
                        + p_392683_
                );
            } else if (p_396221_ + p_391501_ > p_395107_.getWidth(p_394222_) || p_392746_ + p_397458_ > p_395107_.getHeight(p_394222_)) {
                throw new IllegalArgumentException(
                    "Dest texture ("
                        + p_391501_
                        + "x"
                        + p_397458_
                        + ") is not large enough to write a rectangle of "
                        + p_391501_
                        + "x"
                        + p_397458_
                        + " at "
                        + p_396221_
                        + "x"
                        + p_392746_
                        + " (at mip level "
                        + p_394222_
                        + ")"
                );
            } else if (p_395107_.isClosed()) {
                throw new IllegalStateException("Destination texture is closed");
            } else {
                GlStateManager._bindTexture(((GlTexture)p_395107_).id);
                GlStateManager._pixelStore(3314, p_392321_.getWidth());
                GlStateManager._pixelStore(3316, p_397527_);
                GlStateManager._pixelStore(3315, p_392683_);
                GlStateManager._pixelStore(3317, p_392321_.format().components());
                GlStateManager._texSubImage2D(
                    3553, p_394222_, p_396221_, p_392746_, p_391501_, p_397458_, GlConst.toGl(p_392321_.format()), 5121, p_392321_.getPointer()
                );
            }
        } else {
            throw new IllegalArgumentException("Invalid mipLevel " + p_394222_ + ", must be >= 0 and < " + p_395107_.getMipLevels());
        }
    }

    @Override
    public void writeToTexture(
        GpuTexture p_396389_, IntBuffer p_397466_, NativeImage.Format p_392785_, int p_394994_, int p_395915_, int p_394993_, int p_393355_, int p_396347_
    ) {
        if (this.inRenderPass) {
            throw new IllegalStateException("Close the existing render pass before performing additional commands");
        } else if (p_394994_ >= 0 && p_394994_ < p_396389_.getMipLevels()) {
            if (p_393355_ * p_396347_ > p_397466_.remaining()) {
                throw new IllegalArgumentException(
                    "Copy would overrun the source buffer (remaining length of " + p_397466_.remaining() + ", but copy is " + p_393355_ + "x" + p_396347_ + ")"
                );
            } else if (p_395915_ + p_393355_ > p_396389_.getWidth(p_394994_) || p_394993_ + p_396347_ > p_396389_.getHeight(p_394994_)) {
                throw new IllegalArgumentException(
                    "Dest texture ("
                        + p_396389_.getWidth(p_394994_)
                        + "x"
                        + p_396389_.getHeight(p_394994_)
                        + ") is not large enough to write a rectangle of "
                        + p_393355_
                        + "x"
                        + p_396347_
                        + " at "
                        + p_395915_
                        + "x"
                        + p_394993_
                );
            } else if (p_396389_.isClosed()) {
                throw new IllegalStateException("Destination texture is closed");
            } else {
                GlStateManager._bindTexture(((GlTexture)p_396389_).id);
                GlStateManager._pixelStore(3314, p_393355_);
                GlStateManager._pixelStore(3316, 0);
                GlStateManager._pixelStore(3315, 0);
                GlStateManager._pixelStore(3317, p_392785_.components());
                GlStateManager._texSubImage2D(3553, p_394994_, p_395915_, p_394993_, p_393355_, p_396347_, GlConst.toGl(p_392785_), 5121, p_397466_);
            }
        } else {
            throw new IllegalArgumentException("Invalid mipLevel, must be >= 0 and < " + p_396389_.getMipLevels());
        }
    }

    @Override
    public void copyTextureToBuffer(GpuTexture p_397941_, GpuBuffer p_395918_, int p_391975_, Runnable p_397559_, int p_391607_) {
        if (this.inRenderPass) {
            throw new IllegalStateException("Close the existing render pass before performing additional commands");
        } else {
            this.copyTextureToBuffer(p_397941_, p_395918_, p_391975_, p_397559_, p_391607_, 0, 0, p_397941_.getWidth(p_391607_), p_397941_.getHeight(p_391607_));
        }
    }

    @Override
    public void copyTextureToBuffer(
        GpuTexture p_391871_, GpuBuffer p_395502_, int p_395739_, Runnable p_397589_, int p_391264_, int p_393748_, int p_396780_, int p_391271_, int p_395113_
    ) {
        if (this.inRenderPass) {
            throw new IllegalStateException("Close the existing render pass before performing additional commands");
        } else if (p_391264_ >= 0 && p_391264_ < p_391871_.getMipLevels()) {
            if (p_391871_.getWidth(p_391264_) * p_391871_.getHeight(p_391264_) * p_391871_.getFormat().pixelSize() + p_395739_ > p_395502_.size()) {
                throw new IllegalArgumentException(
                    "Buffer of size "
                        + p_395502_.size()
                        + " is not large enough to hold "
                        + p_391271_
                        + "x"
                        + p_395113_
                        + " pixels ("
                        + p_391871_.getFormat().pixelSize()
                        + " bytes each) starting from offset "
                        + p_395739_
                );
            } else if (p_395502_.type() != BufferType.PIXEL_PACK) {
                throw new IllegalArgumentException("Buffer of type " + p_395502_.type() + " cannot be used to retrieve a texture");
            } else if (p_393748_ + p_391271_ > p_391871_.getWidth(p_391264_) || p_396780_ + p_395113_ > p_391871_.getHeight(p_391264_)) {
                throw new IllegalArgumentException(
                    "Copy source texture ("
                        + p_391871_.getWidth(p_391264_)
                        + "x"
                        + p_391871_.getHeight(p_391264_)
                        + ") is not large enough to read a rectangle of "
                        + p_391271_
                        + "x"
                        + p_395113_
                        + " from "
                        + p_393748_
                        + ","
                        + p_396780_
                );
            } else if (p_391871_.isClosed()) {
                throw new IllegalStateException("Source texture is closed");
            } else if (p_395502_.isClosed()) {
                throw new IllegalStateException("Destination buffer is closed");
            } else {
                GlStateManager.clearGlErrors();
                this.device.directStateAccess().bindFrameBufferTextures(this.readFbo, ((GlTexture)p_391871_).glId(), 0, p_391264_, 36008);
                GlStateManager._glBindBuffer(GlConst.toGl(p_395502_.type()), ((GlBuffer)p_395502_).handle);
                GlStateManager._pixelStore(3330, p_391271_);
                GlStateManager._readPixels(
                    p_393748_,
                    p_396780_,
                    p_391271_,
                    p_395113_,
                    GlConst.toGlExternalId(p_391871_.getFormat()),
                    GlConst.toGlType(p_391871_.getFormat()),
                    p_395739_
                );
                RenderSystem.queueFencedTask(p_397589_);
                GlStateManager._glFramebufferTexture2D(36008, 36064, 3553, 0, p_391264_);
                GlStateManager._glBindFramebuffer(36008, 0);
                GlStateManager._glBindBuffer(GlConst.toGl(p_395502_.type()), 0);
                int i = GlStateManager._getError();
                if (i != 0) {
                    throw new IllegalStateException("Couldn't perform copyTobuffer for texture " + p_391871_.getLabel() + ": GL error " + i);
                }
            }
        } else {
            throw new IllegalArgumentException("Invalid mipLevel " + p_391264_ + ", must be >= 0 and < " + p_391871_.getMipLevels());
        }
    }

    @Override
    public void copyTextureToTexture(
        GpuTexture p_394155_, GpuTexture p_394461_, int p_396176_, int p_393684_, int p_394159_, int p_394139_, int p_396698_, int p_394668_, int p_397937_
    ) {
        if (this.inRenderPass) {
            throw new IllegalStateException("Close the existing render pass before performing additional commands");
        } else if (p_396176_ >= 0 && p_396176_ < p_394155_.getMipLevels() && p_396176_ < p_394461_.getMipLevels()) {
            if (p_393684_ + p_394668_ > p_394461_.getWidth(p_396176_) || p_394159_ + p_397937_ > p_394461_.getHeight(p_396176_)) {
                throw new IllegalArgumentException(
                    "Dest texture ("
                        + p_394461_.getWidth(p_396176_)
                        + "x"
                        + p_394461_.getHeight(p_396176_)
                        + ") is not large enough to write a rectangle of "
                        + p_394668_
                        + "x"
                        + p_397937_
                        + " at "
                        + p_393684_
                        + "x"
                        + p_394159_
                );
            } else if (p_394139_ + p_394668_ > p_394155_.getWidth(p_396176_) || p_396698_ + p_397937_ > p_394155_.getHeight(p_396176_)) {
                throw new IllegalArgumentException(
                    "Source texture ("
                        + p_394155_.getWidth(p_396176_)
                        + "x"
                        + p_394155_.getHeight(p_396176_)
                        + ") is not large enough to read a rectangle of "
                        + p_394668_
                        + "x"
                        + p_397937_
                        + " at "
                        + p_394139_
                        + "x"
                        + p_396698_
                );
            } else if (p_394155_.isClosed()) {
                throw new IllegalStateException("Source texture is closed");
            } else if (p_394461_.isClosed()) {
                throw new IllegalStateException("Destination texture is closed");
            } else {
                GlStateManager.clearGlErrors();
                GlStateManager._disableScissorTest();
                boolean flag = p_394155_.getFormat().hasDepthAspect();
                int i = ((GlTexture)p_394155_).glId();
                int j = ((GlTexture)p_394461_).glId();
                this.device.directStateAccess().bindFrameBufferTextures(this.readFbo, flag ? 0 : i, flag ? i : 0, 0, 0);
                this.device.directStateAccess().bindFrameBufferTextures(this.drawFbo, flag ? 0 : j, flag ? j : 0, 0, 0);
                this.device
                    .directStateAccess()
                    .blitFrameBuffers(
                        this.readFbo,
                        this.drawFbo,
                        p_394139_,
                        p_396698_,
                        p_394668_,
                        p_397937_,
                        p_393684_,
                        p_394159_,
                        p_394668_,
                        p_397937_,
                        flag ? 256 : 16384,
                        9728
                    );
                int k = GlStateManager._getError();
                if (k != 0) {
                    throw new IllegalStateException(
                        "Couldn't perform copyToTexture for texture " + p_394155_.getLabel() + " to " + p_394461_.getLabel() + ": GL error " + k
                    );
                }
            }
        } else {
            throw new IllegalArgumentException(
                "Invalid mipLevel " + p_396176_ + ", must be >= 0 and < " + p_394155_.getMipLevels() + " and < " + p_394461_.getMipLevels()
            );
        }
    }

    @Override
    public void presentTexture(GpuTexture p_396400_) {
        if (this.inRenderPass) {
            throw new IllegalStateException("Close the existing render pass before performing additional commands");
        } else if (!p_396400_.getFormat().hasColorAspect()) {
            throw new IllegalStateException("Cannot present a non-color texture!");
        } else {
            GlStateManager._disableScissorTest();
            GlStateManager._viewport(0, 0, p_396400_.getWidth(0), p_396400_.getHeight(0));
            GlStateManager._depthMask(true);
            GlStateManager._colorMask(true, true, true, true);
            this.device.directStateAccess().bindFrameBufferTextures(this.drawFbo, ((GlTexture)p_396400_).glId(), 0, 0, 0);
            this.device
                .directStateAccess()
                .blitFrameBuffers(
                    this.drawFbo, 0, 0, 0, p_396400_.getWidth(0), p_396400_.getHeight(0), 0, 0, p_396400_.getWidth(0), p_396400_.getHeight(0), 16384, 9728
                );
        }
    }

    protected void executeDrawMultiple(
        GlRenderPass pRenderPass, Collection<RenderPass.Draw> pDraws, @Nullable GpuBuffer pBuffer, @Nullable VertexFormat.IndexType pIndexType
    ) {
        if (this.trySetup(pRenderPass)) {
            if (pIndexType == null) {
                pIndexType = VertexFormat.IndexType.SHORT;
            }

            for (RenderPass.Draw renderpass$draw : pDraws) {
                VertexFormat.IndexType vertexformat$indextype = renderpass$draw.indexType() == null ? pIndexType : renderpass$draw.indexType();
                pRenderPass.setIndexBuffer(renderpass$draw.indexBuffer() == null ? pBuffer : renderpass$draw.indexBuffer(), vertexformat$indextype);
                pRenderPass.setVertexBuffer(renderpass$draw.slot(), renderpass$draw.vertexBuffer());
                if (GlRenderPass.VALIDATION) {
                    if (pRenderPass.indexBuffer == null) {
                        throw new IllegalStateException("Missing index buffer");
                    }

                    if (pRenderPass.indexBuffer.isClosed()) {
                        throw new IllegalStateException("Index buffer has been closed!");
                    }

                    if (pRenderPass.vertexBuffers[0] == null) {
                        throw new IllegalStateException("Missing vertex buffer at slot 0");
                    }

                    if (pRenderPass.vertexBuffers[0].isClosed()) {
                        throw new IllegalStateException("Vertex buffer at slot 0 has been closed!");
                    }
                }

                Consumer<RenderPass.UniformUploader> consumer = renderpass$draw.uniformUploaderConsumer();
                if (consumer != null) {
                    consumer.accept((p_394656_, p_393530_) -> {
                        Uniform uniform = pRenderPass.pipeline.program().getUniform(p_394656_);
                        if (uniform != null) {
                            uniform.set(p_393530_);
                            uniform.upload();
                        }
                    });
                }

                this.drawFromBuffers(pRenderPass, renderpass$draw.firstIndex(), renderpass$draw.indexCount(), vertexformat$indextype, pRenderPass.pipeline);
            }
        }
    }

    protected void executeDraw(GlRenderPass pRenderPass, int pFirstIndex, int pIndexCount, @Nullable VertexFormat.IndexType pIndexType) {
        if (this.trySetup(pRenderPass)) {
            if (GlRenderPass.VALIDATION) {
                if (pIndexType != null) {
                    if (pRenderPass.indexBuffer == null) {
                        throw new IllegalStateException("Missing index buffer");
                    }

                    if (pRenderPass.indexBuffer.isClosed()) {
                        throw new IllegalStateException("Index buffer has been closed!");
                    }
                }

                if (pRenderPass.vertexBuffers[0] == null) {
                    throw new IllegalStateException("Missing vertex buffer at slot 0");
                }

                if (pRenderPass.vertexBuffers[0].isClosed()) {
                    throw new IllegalStateException("Vertex buffer at slot 0 has been closed!");
                }
            }

            this.drawFromBuffers(pRenderPass, pFirstIndex, pIndexCount, pIndexType, pRenderPass.pipeline);
        }
    }

    private void drawFromBuffers(GlRenderPass pRenderPass, int pFirstIndex, int pIndexCount, @Nullable VertexFormat.IndexType pIndexType, GlRenderPipeline pPipeline) {
        this.device.vertexArrayCache().bindVertexArray(pPipeline.info().getVertexFormat(), (GlBuffer)pRenderPass.vertexBuffers[0]);
        if (pIndexType != null) {
            GlStateManager._glBindBuffer(34963, ((GlBuffer)pRenderPass.indexBuffer).handle);
            GlStateManager._drawElements(
                GlConst.toGl(pPipeline.info().getVertexFormatMode()), pIndexCount, GlConst.toGl(pIndexType), (long)pFirstIndex * pIndexType.bytes
            );
        } else {
            GlStateManager._drawArrays(GlConst.toGl(pPipeline.info().getVertexFormatMode()), pFirstIndex, pIndexCount);
        }
    }

    private boolean trySetup(GlRenderPass pRenderPass) {
        if (GlRenderPass.VALIDATION) {
            if (pRenderPass.pipeline == null) {
                throw new IllegalStateException("Can't draw without a render pipeline");
            }

            if (pRenderPass.pipeline.program() == GlProgram.INVALID_PROGRAM) {
                throw new IllegalStateException("Pipeline contains invalid shader program");
            }

            for (RenderPipeline.UniformDescription renderpipeline$uniformdescription : pRenderPass.pipeline.info().getUniforms()) {
                Object object = pRenderPass.uniforms.get(renderpipeline$uniformdescription.name());
                if (object == null && !GlProgram.BUILT_IN_UNIFORMS.contains(renderpipeline$uniformdescription.name())) {
                    throw new IllegalStateException(
                        "Missing uniform " + renderpipeline$uniformdescription.name() + " (should be " + renderpipeline$uniformdescription.type() + ")"
                    );
                }
            }

            for (String s1 : pRenderPass.pipeline.program().getSamplers()) {
                if (!pRenderPass.samplers.containsKey(s1)) {
                    throw new IllegalStateException("Missing sampler " + s1);
                }

                if (pRenderPass.samplers.get(s1).isClosed()) {
                    throw new IllegalStateException("Sampler " + s1 + " has been closed!");
                }
            }

            if (pRenderPass.pipeline.info().wantsDepthTexture() && !pRenderPass.hasDepthTexture()) {
                LOGGER.warn("Render pipeline {} wants a depth texture but none was provided - this is probably a bug", pRenderPass.pipeline.info().getLocation());
            }
        } else if (pRenderPass.pipeline == null || pRenderPass.pipeline.program() == GlProgram.INVALID_PROGRAM) {
            return false;
        }

        RenderPipeline renderpipeline = pRenderPass.pipeline.info();
        GlProgram glprogram = pRenderPass.pipeline.program();

        for (Uniform uniform : glprogram.getUniforms()) {
            if (pRenderPass.dirtyUniforms.contains(uniform.getName())) {
                Object object1 = pRenderPass.uniforms.get(uniform.getName());
                if (object1 instanceof int[]) {
                    glprogram.safeGetUniform(uniform.getName()).set((int[])object1);
                } else if (object1 instanceof float[]) {
                    glprogram.safeGetUniform(uniform.getName()).set((float[])object1);
                } else if (object1 != null) {
                    throw new IllegalStateException("Unknown uniform type - expected " + uniform.getType() + ", found " + object1);
                }
            }
        }

        pRenderPass.dirtyUniforms.clear();
        this.applyPipelineState(renderpipeline);
        boolean flag = this.lastProgram != glprogram;
        if (flag) {
            GlStateManager._glUseProgram(glprogram.getProgramId());
            this.lastProgram = glprogram;
        }

        IntList intlist = glprogram.getSamplerLocations();

        for (int j = 0; j < glprogram.getSamplers().size(); j++) {
            String s = glprogram.getSamplers().get(j);
            GlTexture gltexture = (GlTexture)pRenderPass.samplers.get(s);
            if (gltexture != null) {
                if (flag || pRenderPass.dirtySamplers.contains(s)) {
                    int i = intlist.getInt(j);
                    Uniform.uploadInteger(i, j);
                    GlStateManager._activeTexture(33984 + j);
                }

                GlStateManager._bindTexture(gltexture.glId());
                gltexture.flushModeChanges();
            }
        }

        Window window = Minecraft.getInstance() == null ? null : Minecraft.getInstance().getWindow();
        glprogram.setDefaultUniforms(
            renderpipeline.getVertexFormatMode(),
            RenderSystem.getModelViewMatrix(),
            RenderSystem.getProjectionMatrix(),
            window == null ? 0.0F : window.getWidth(),
            window == null ? 0.0F : window.getHeight()
        );

        for (Uniform uniform1 : glprogram.getUniforms()) {
            uniform1.upload();
        }

        if (pRenderPass.scissorState.isEnabled()) {
            GlStateManager._enableScissorTest();
            GlStateManager._scissorBox(
                pRenderPass.scissorState.getX(), pRenderPass.scissorState.getY(), pRenderPass.scissorState.getWidth(), pRenderPass.scissorState.getHeight()
            );
        } else {
            GlStateManager._disableScissorTest();
        }

        return true;
    }

    private void applyPipelineState(RenderPipeline pPipeline) {
        if (this.lastPipeline != pPipeline) {
            this.lastPipeline = pPipeline;
            if (pPipeline.getDepthTestFunction() != DepthTestFunction.NO_DEPTH_TEST) {
                GlStateManager._enableDepthTest();
                GlStateManager._depthFunc(GlConst.toGl(pPipeline.getDepthTestFunction()));
            } else {
                GlStateManager._disableDepthTest();
            }

            if (pPipeline.isCull()) {
                GlStateManager._enableCull();
            } else {
                GlStateManager._disableCull();
            }

            if (pPipeline.getBlendFunction().isPresent()) {
                GlStateManager._enableBlend();
                BlendFunction blendfunction = pPipeline.getBlendFunction().get();
                GlStateManager._blendFuncSeparate(
                    GlConst.toGl(blendfunction.sourceColor()),
                    GlConst.toGl(blendfunction.destColor()),
                    GlConst.toGl(blendfunction.sourceAlpha()),
                    GlConst.toGl(blendfunction.destAlpha())
                );
            } else {
                GlStateManager._disableBlend();
            }

            GlStateManager._polygonMode(1032, GlConst.toGl(pPipeline.getPolygonMode()));
            GlStateManager._depthMask(pPipeline.isWriteDepth());
            GlStateManager._colorMask(pPipeline.isWriteColor(), pPipeline.isWriteColor(), pPipeline.isWriteColor(), pPipeline.isWriteAlpha());
            if (pPipeline.getDepthBiasConstant() == 0.0F && pPipeline.getDepthBiasScaleFactor() == 0.0F) {
                GlStateManager._disablePolygonOffset();
            } else {
                GlStateManager._polygonOffset(pPipeline.getDepthBiasScaleFactor(), pPipeline.getDepthBiasConstant());
                GlStateManager._enablePolygonOffset();
            }

            switch (pPipeline.getColorLogic()) {
                case NONE:
                    GlStateManager._disableColorLogicOp();
                    break;
                case OR_REVERSE:
                    GlStateManager._enableColorLogicOp();
                    GlStateManager._logicOp(5387);
            }
        }
    }

    public void finishRenderPass() {
        this.inRenderPass = false;
        GlStateManager._glBindFramebuffer(36160, 0);
    }

    protected GlDevice getDevice() {
        return this.device;
    }
}
