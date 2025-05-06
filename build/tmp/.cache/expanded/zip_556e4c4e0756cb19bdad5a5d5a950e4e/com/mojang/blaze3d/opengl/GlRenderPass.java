package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.ScissorState;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class GlRenderPass implements RenderPass {
    protected static final int MAX_VERTEX_BUFFERS = 1;
    public static final boolean VALIDATION = SharedConstants.IS_RUNNING_IN_IDE;
    private final GlCommandEncoder encoder;
    private final boolean hasDepthTexture;
    private boolean closed;
    @Nullable
    protected GlRenderPipeline pipeline;
    protected final GpuBuffer[] vertexBuffers = new GpuBuffer[1];
    @Nullable
    protected GpuBuffer indexBuffer;
    protected VertexFormat.IndexType indexType = VertexFormat.IndexType.INT;
    protected final ScissorState scissorState = new ScissorState();
    protected final HashMap<String, Object> uniforms = new HashMap<>();
    protected final HashMap<String, GpuTexture> samplers = new HashMap<>();
    protected final Set<String> dirtyUniforms = new HashSet<>();
    protected final Set<String> dirtySamplers = new HashSet<>();

    public GlRenderPass(GlCommandEncoder pEncoder, boolean pHasDepthTexture) {
        this.encoder = pEncoder;
        this.hasDepthTexture = pHasDepthTexture;
    }

    public boolean hasDepthTexture() {
        return this.hasDepthTexture;
    }

    @Override
    public void setPipeline(RenderPipeline p_394211_) {
        if (this.pipeline == null || this.pipeline.info() != p_394211_) {
            this.dirtyUniforms.addAll(this.uniforms.keySet());
            this.dirtySamplers.addAll(this.samplers.keySet());
        }

        this.pipeline = this.encoder.getDevice().getOrCompilePipeline(p_394211_);
    }

    @Override
    public void bindSampler(String p_392583_, GpuTexture p_393970_) {
        this.samplers.put(p_392583_, p_393970_);
        this.dirtySamplers.add(p_392583_);
    }

    @Override
    public void setUniform(String p_394503_, int... p_395779_) {
        this.uniforms.put(p_394503_, p_395779_);
        this.dirtyUniforms.add(p_394503_);
    }

    @Override
    public void setUniform(String p_394528_, float... p_395797_) {
        this.uniforms.put(p_394528_, p_395797_);
        this.dirtyUniforms.add(p_394528_);
    }

    @Override
    public void setUniform(String p_391945_, Matrix4f p_392655_) {
        this.uniforms.put(p_391945_, p_392655_.get(new float[16]));
        this.dirtyUniforms.add(p_391945_);
    }

    @Override
    public void enableScissor(ScissorState p_394440_) {
        this.scissorState.copyFrom(p_394440_);
    }

    @Override
    public void enableScissor(int p_394105_, int p_397366_, int p_397303_, int p_391821_) {
        this.scissorState.enable(p_394105_, p_397366_, p_397303_, p_391821_);
    }

    @Override
    public void disableScissor() {
        this.scissorState.disable();
    }

    @Override
    public void setVertexBuffer(int p_394641_, GpuBuffer p_397665_) {
        if (p_394641_ >= 0 && p_394641_ < 1) {
            this.vertexBuffers[p_394641_] = p_397665_;
        } else {
            throw new IllegalArgumentException("Vertex buffer slot is out of range: " + p_394641_);
        }
    }

    @Override
    public void setIndexBuffer(@Nullable GpuBuffer p_393276_, VertexFormat.IndexType p_392180_) {
        this.indexBuffer = p_393276_;
        this.indexType = p_392180_;
    }

    @Override
    public void drawIndexed(int p_393186_, int p_395612_) {
        if (this.closed) {
            throw new IllegalStateException("Can't use a closed render pass");
        } else {
            this.encoder.executeDraw(this, p_393186_, p_395612_, this.indexType);
        }
    }

    @Override
    public void drawMultipleIndexed(Collection<RenderPass.Draw> p_394525_, @Nullable GpuBuffer p_397624_, @Nullable VertexFormat.IndexType p_391410_) {
        if (this.closed) {
            throw new IllegalStateException("Can't use a closed render pass");
        } else {
            this.encoder.executeDrawMultiple(this, p_394525_, p_397624_, p_391410_);
        }
    }

    @Override
    public void draw(int p_392940_, int p_391785_) {
        if (this.closed) {
            throw new IllegalStateException("Can't use a closed render pass");
        } else {
            this.encoder.executeDraw(this, p_392940_, p_391785_, null);
        }
    }

    @Override
    public void close() {
        if (!this.closed) {
            this.closed = true;
            this.encoder.finishRenderPass();
        }
    }
}