package net.minecraft.client.renderer;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.BufferType;
import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

@OnlyIn(Dist.CLIENT)
public class CubeMap {
    private static final int SIDES = 6;
    @Nullable
    private GpuBuffer cubeMapBuffer = null;
    private final List<ResourceLocation> sides;

    public CubeMap(ResourceLocation pBaseImageLocation) {
        this.sides = IntStream.range(0, 6).mapToObj(p_377762_ -> pBaseImageLocation.withPath(pBaseImageLocation.getPath() + "_" + p_377762_ + ".png")).toList();
    }

    public void render(Minecraft pMc, float pPitch, float pYaw, float pAlpha) {
        if (this.cubeMapBuffer == null) {
            this.initializeVertices();
        }

        Matrix4f matrix4f = new Matrix4f().setPerspective(1.4835298F, (float)pMc.getWindow().getWidth() / pMc.getWindow().getHeight(), 0.05F, 10.0F);
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(matrix4f, ProjectionType.PERSPECTIVE);
        Matrix4fStack matrix4fstack = RenderSystem.getModelViewStack();
        matrix4fstack.pushMatrix();
        matrix4fstack.rotationX((float) Math.PI);
        int i = 2;
        RenderPipeline renderpipeline = RenderPipelines.PANORAMA;
        RenderTarget rendertarget = Minecraft.getInstance().getMainRenderTarget();
        GpuTexture gputexture = rendertarget.getColorTexture();
        GpuTexture gputexture1 = rendertarget.getDepthTexture();
        RenderSystem.AutoStorageIndexBuffer rendersystem$autostorageindexbuffer = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
        GpuBuffer gpubuffer = rendersystem$autostorageindexbuffer.getBuffer(36);

        try (RenderPass renderpass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(gputexture, OptionalInt.empty(), gputexture1, OptionalDouble.empty())) {
            renderpass.setPipeline(renderpipeline);
            renderpass.setVertexBuffer(0, this.cubeMapBuffer);
            renderpass.setIndexBuffer(gpubuffer, rendersystem$autostorageindexbuffer.type());

            for (int j = 0; j < 4; j++) {
                matrix4fstack.pushMatrix();
                float f = (j % 2 / 2.0F - 0.5F) / 256.0F;
                float f1 = (j / 2 / 2.0F - 0.5F) / 256.0F;
                float f2 = 0.0F;
                matrix4fstack.translate(f, f1, 0.0F);
                matrix4fstack.rotateX(pPitch * (float) (Math.PI / 180.0));
                matrix4fstack.rotateY(pYaw * (float) (Math.PI / 180.0));
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, pAlpha / (j + 1));

                for (int k = 0; k < 6; k++) {
                    renderpass.bindSampler("Sampler0", pMc.getTextureManager().getTexture(this.sides.get(k)).getTexture());
                    renderpass.drawIndexed(6 * k, 6);
                }

                matrix4fstack.popMatrix();
            }
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.restoreProjectionMatrix();
        matrix4fstack.popMatrix();
    }

    private void initializeVertices() {
        this.cubeMapBuffer = RenderSystem.getDevice()
            .createBuffer(() -> "Cube map vertex buffer", BufferType.VERTICES, BufferUsage.DYNAMIC_WRITE, 24 * DefaultVertexFormat.POSITION_TEX.getVertexSize());

        try (ByteBufferBuilder bytebufferbuilder = new ByteBufferBuilder(DefaultVertexFormat.POSITION_TEX.getVertexSize() * 4)) {
            BufferBuilder bufferbuilder = new BufferBuilder(bytebufferbuilder, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            bufferbuilder.addVertex(-1.0F, -1.0F, 1.0F).setUv(0.0F, 0.0F);
            bufferbuilder.addVertex(-1.0F, 1.0F, 1.0F).setUv(0.0F, 1.0F);
            bufferbuilder.addVertex(1.0F, 1.0F, 1.0F).setUv(1.0F, 1.0F);
            bufferbuilder.addVertex(1.0F, -1.0F, 1.0F).setUv(1.0F, 0.0F);
            bufferbuilder.addVertex(1.0F, -1.0F, 1.0F).setUv(0.0F, 0.0F);
            bufferbuilder.addVertex(1.0F, 1.0F, 1.0F).setUv(0.0F, 1.0F);
            bufferbuilder.addVertex(1.0F, 1.0F, -1.0F).setUv(1.0F, 1.0F);
            bufferbuilder.addVertex(1.0F, -1.0F, -1.0F).setUv(1.0F, 0.0F);
            bufferbuilder.addVertex(1.0F, -1.0F, -1.0F).setUv(0.0F, 0.0F);
            bufferbuilder.addVertex(1.0F, 1.0F, -1.0F).setUv(0.0F, 1.0F);
            bufferbuilder.addVertex(-1.0F, 1.0F, -1.0F).setUv(1.0F, 1.0F);
            bufferbuilder.addVertex(-1.0F, -1.0F, -1.0F).setUv(1.0F, 0.0F);
            bufferbuilder.addVertex(-1.0F, -1.0F, -1.0F).setUv(0.0F, 0.0F);
            bufferbuilder.addVertex(-1.0F, 1.0F, -1.0F).setUv(0.0F, 1.0F);
            bufferbuilder.addVertex(-1.0F, 1.0F, 1.0F).setUv(1.0F, 1.0F);
            bufferbuilder.addVertex(-1.0F, -1.0F, 1.0F).setUv(1.0F, 0.0F);
            bufferbuilder.addVertex(-1.0F, -1.0F, -1.0F).setUv(0.0F, 0.0F);
            bufferbuilder.addVertex(-1.0F, -1.0F, 1.0F).setUv(0.0F, 1.0F);
            bufferbuilder.addVertex(1.0F, -1.0F, 1.0F).setUv(1.0F, 1.0F);
            bufferbuilder.addVertex(1.0F, -1.0F, -1.0F).setUv(1.0F, 0.0F);
            bufferbuilder.addVertex(-1.0F, 1.0F, 1.0F).setUv(0.0F, 0.0F);
            bufferbuilder.addVertex(-1.0F, 1.0F, -1.0F).setUv(0.0F, 1.0F);
            bufferbuilder.addVertex(1.0F, 1.0F, -1.0F).setUv(1.0F, 1.0F);
            bufferbuilder.addVertex(1.0F, 1.0F, 1.0F).setUv(1.0F, 0.0F);

            try (MeshData meshdata = bufferbuilder.buildOrThrow()) {
                CommandEncoder commandencoder = RenderSystem.getDevice().createCommandEncoder();
                commandencoder.writeToBuffer(this.cubeMapBuffer, meshdata.vertexBuffer(), 0);
            }
        }
    }

    public void registerTextures(TextureManager pTextureManager) {
        for (ResourceLocation resourcelocation : this.sides) {
            pTextureManager.registerForNextReload(resourcelocation);
        }
    }
}