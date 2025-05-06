package com.mojang.blaze3d.systems;

import com.mojang.blaze3d.DontObfuscate;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.Collection;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
@DontObfuscate
public interface RenderPass extends AutoCloseable {
    void setPipeline(RenderPipeline pPipeline);

    void bindSampler(String pName, GpuTexture pTexture);

    void setUniform(String pName, int... pValues);

    void setUniform(String pName, float... pValues);

    void setUniform(String pName, Matrix4f pValues);

    void enableScissor(ScissorState pScissorState);

    void enableScissor(int pX, int pY, int pWidth, int pHeight);

    void disableScissor();

    void setVertexBuffer(int pIndex, GpuBuffer pBuffer);

    void setIndexBuffer(GpuBuffer pIndexBuffer, VertexFormat.IndexType pIndexType);

    void drawIndexed(int pFirstIndex, int pIndexCount);

    void drawMultipleIndexed(Collection<RenderPass.Draw> pDraws, @Nullable GpuBuffer pBuffer, @Nullable VertexFormat.IndexType pIndexType);

    void draw(int pFirstIndex, int pIndexCount);

    @Override
    void close();

    @OnlyIn(Dist.CLIENT)
    public record Draw(
        int slot,
        GpuBuffer vertexBuffer,
        @Nullable GpuBuffer indexBuffer,
        @Nullable VertexFormat.IndexType indexType,
        int firstIndex,
        int indexCount,
        @Nullable Consumer<RenderPass.UniformUploader> uniformUploaderConsumer
    ) {
        public Draw(int pSlot, GpuBuffer pVertexBuffer, GpuBuffer pIndexBuffer, VertexFormat.IndexType pIndexType, int pFirstIndex, int pIndexCount) {
            this(pSlot, pVertexBuffer, pIndexBuffer, pIndexType, pFirstIndex, pIndexCount, null);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public interface UniformUploader {
        void upload(String pName, float... pValues);
    }
}