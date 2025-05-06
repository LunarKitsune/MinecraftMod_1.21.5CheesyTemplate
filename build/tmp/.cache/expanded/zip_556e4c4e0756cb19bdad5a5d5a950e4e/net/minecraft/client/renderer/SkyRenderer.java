package net.minecraft.client.renderer;

import com.mojang.blaze3d.buffers.BufferType;
import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TriState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class SkyRenderer implements AutoCloseable {
    private static final ResourceLocation SUN_LOCATION = ResourceLocation.withDefaultNamespace("textures/environment/sun.png");
    private static final ResourceLocation MOON_LOCATION = ResourceLocation.withDefaultNamespace("textures/environment/moon_phases.png");
    public static final ResourceLocation END_SKY_LOCATION = ResourceLocation.withDefaultNamespace("textures/environment/end_sky.png");
    private static final float SKY_DISC_RADIUS = 512.0F;
    private static final int SKY_VERTICES = 10;
    private static final int STAR_COUNT = 1500;
    private static final int END_SKY_QUAD_COUNT = 6;
    private final GpuBuffer starBuffer;
    private final RenderSystem.AutoStorageIndexBuffer starIndices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
    private final GpuBuffer topSkyBuffer;
    private final GpuBuffer bottomSkyBuffer;
    private final GpuBuffer endSkyBuffer;
    private int starIndexCount;

    public SkyRenderer() {
        this.starBuffer = this.buildStars();
        this.endSkyBuffer = buildEndSky();

        try (ByteBufferBuilder bytebufferbuilder = new ByteBufferBuilder(10 * DefaultVertexFormat.POSITION.getVertexSize())) {
            BufferBuilder bufferbuilder = new BufferBuilder(bytebufferbuilder, VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION);
            this.buildSkyDisc(bufferbuilder, 16.0F);

            try (MeshData meshdata = bufferbuilder.buildOrThrow()) {
                this.topSkyBuffer = RenderSystem.getDevice()
                    .createBuffer(() -> "Top sky vertex buffer", BufferType.VERTICES, BufferUsage.STATIC_WRITE, meshdata.vertexBuffer());
            }

            bufferbuilder = new BufferBuilder(bytebufferbuilder, VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION);
            this.buildSkyDisc(bufferbuilder, -16.0F);

            try (MeshData meshdata1 = bufferbuilder.buildOrThrow()) {
                this.bottomSkyBuffer = RenderSystem.getDevice()
                    .createBuffer(() -> "Bottom sky vertex buffer", BufferType.VERTICES, BufferUsage.STATIC_WRITE, meshdata1.vertexBuffer());
            }
        }
    }

    private GpuBuffer buildStars() {
        RandomSource randomsource = RandomSource.create(10842L);
        float f = 100.0F;

        GpuBuffer gpubuffer;
        try (ByteBufferBuilder bytebufferbuilder = new ByteBufferBuilder(DefaultVertexFormat.POSITION.getVertexSize() * 1500 * 4)) {
            BufferBuilder bufferbuilder = new BufferBuilder(bytebufferbuilder, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);

            for (int i = 0; i < 1500; i++) {
                float f1 = randomsource.nextFloat() * 2.0F - 1.0F;
                float f2 = randomsource.nextFloat() * 2.0F - 1.0F;
                float f3 = randomsource.nextFloat() * 2.0F - 1.0F;
                float f4 = 0.15F + randomsource.nextFloat() * 0.1F;
                float f5 = Mth.lengthSquared(f1, f2, f3);
                if (!(f5 <= 0.010000001F) && !(f5 >= 1.0F)) {
                    Vector3f vector3f = new Vector3f(f1, f2, f3).normalize(100.0F);
                    float f6 = (float)(randomsource.nextDouble() * (float) Math.PI * 2.0);
                    Matrix3f matrix3f = new Matrix3f().rotateTowards(new Vector3f(vector3f).negate(), new Vector3f(0.0F, 1.0F, 0.0F)).rotateZ(-f6);
                    bufferbuilder.addVertex(new Vector3f(f4, -f4, 0.0F).mul(matrix3f).add(vector3f));
                    bufferbuilder.addVertex(new Vector3f(f4, f4, 0.0F).mul(matrix3f).add(vector3f));
                    bufferbuilder.addVertex(new Vector3f(-f4, f4, 0.0F).mul(matrix3f).add(vector3f));
                    bufferbuilder.addVertex(new Vector3f(-f4, -f4, 0.0F).mul(matrix3f).add(vector3f));
                }
            }

            try (MeshData meshdata = bufferbuilder.buildOrThrow()) {
                this.starIndexCount = meshdata.drawState().indexCount();
                gpubuffer = RenderSystem.getDevice()
                    .createBuffer(() -> "Stars vertex buffer", BufferType.VERTICES, BufferUsage.STATIC_WRITE, meshdata.vertexBuffer());
            }
        }

        return gpubuffer;
    }

    private void buildSkyDisc(VertexConsumer pBuffer, float pY) {
        float f = Math.signum(pY) * 512.0F;
        pBuffer.addVertex(0.0F, pY, 0.0F);

        for (int i = -180; i <= 180; i += 45) {
            pBuffer.addVertex(f * Mth.cos(i * (float) (Math.PI / 180.0)), pY, 512.0F * Mth.sin(i * (float) (Math.PI / 180.0)));
        }
    }

    public void renderSkyDisc(float pRed, float pGreen, float pBlue) {
        RenderSystem.setShaderColor(pRed, pGreen, pBlue, 1.0F);
        GpuTexture gputexture = Minecraft.getInstance().getMainRenderTarget().getColorTexture();
        GpuTexture gputexture1 = Minecraft.getInstance().getMainRenderTarget().getDepthTexture();

        try (RenderPass renderpass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(gputexture, OptionalInt.empty(), gputexture1, OptionalDouble.empty())) {
            renderpass.setPipeline(RenderPipelines.SKY);
            renderpass.setVertexBuffer(0, this.topSkyBuffer);
            renderpass.draw(0, 10);
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public void renderDarkDisc() {
        RenderSystem.setShaderColor(0.0F, 0.0F, 0.0F, 1.0F);
        Matrix4fStack matrix4fstack = RenderSystem.getModelViewStack();
        matrix4fstack.pushMatrix();
        matrix4fstack.translate(0.0F, 12.0F, 0.0F);
        GpuTexture gputexture = Minecraft.getInstance().getMainRenderTarget().getColorTexture();
        GpuTexture gputexture1 = Minecraft.getInstance().getMainRenderTarget().getDepthTexture();

        try (RenderPass renderpass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(gputexture, OptionalInt.empty(), gputexture1, OptionalDouble.empty())) {
            renderpass.setPipeline(RenderPipelines.SKY);
            renderpass.setVertexBuffer(0, this.bottomSkyBuffer);
            renderpass.draw(0, 10);
        }

        matrix4fstack.popMatrix();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    public void renderSunMoonAndStars(
        PoseStack pPoseStack,
        MultiBufferSource.BufferSource pBufferSource,
        float pTimeOfDay,
        int pMoonPhase,
        float pRainLevel,
        float pStarBrightness,
        FogParameters pFog
    ) {
        pPoseStack.pushPose();
        pPoseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
        pPoseStack.mulPose(Axis.XP.rotationDegrees(pTimeOfDay * 360.0F));
        this.renderSun(pRainLevel, pBufferSource, pPoseStack);
        this.renderMoon(pMoonPhase, pRainLevel, pBufferSource, pPoseStack);
        pBufferSource.endBatch();
        if (pStarBrightness > 0.0F) {
            this.renderStars(pFog, pStarBrightness, pPoseStack);
        }

        pPoseStack.popPose();
    }

    private void renderSun(float pAlpha, MultiBufferSource pBufferSource, PoseStack pPoseStack) {
        float f = 30.0F;
        float f1 = 100.0F;
        VertexConsumer vertexconsumer = pBufferSource.getBuffer(RenderType.celestial(SUN_LOCATION));
        int i = ARGB.white(pAlpha);
        Matrix4f matrix4f = pPoseStack.last().pose();
        vertexconsumer.addVertex(matrix4f, -30.0F, 100.0F, -30.0F).setUv(0.0F, 0.0F).setColor(i);
        vertexconsumer.addVertex(matrix4f, 30.0F, 100.0F, -30.0F).setUv(1.0F, 0.0F).setColor(i);
        vertexconsumer.addVertex(matrix4f, 30.0F, 100.0F, 30.0F).setUv(1.0F, 1.0F).setColor(i);
        vertexconsumer.addVertex(matrix4f, -30.0F, 100.0F, 30.0F).setUv(0.0F, 1.0F).setColor(i);
    }

    private void renderMoon(int pPhase, float pAlpha, MultiBufferSource pBufferSource, PoseStack pPoseStack) {
        float f = 20.0F;
        int i = pPhase % 4;
        int j = pPhase / 4 % 2;
        float f1 = (i + 0) / 4.0F;
        float f2 = (j + 0) / 2.0F;
        float f3 = (i + 1) / 4.0F;
        float f4 = (j + 1) / 2.0F;
        float f5 = 100.0F;
        VertexConsumer vertexconsumer = pBufferSource.getBuffer(RenderType.celestial(MOON_LOCATION));
        int k = ARGB.white(pAlpha);
        Matrix4f matrix4f = pPoseStack.last().pose();
        vertexconsumer.addVertex(matrix4f, -20.0F, -100.0F, 20.0F).setUv(f3, f4).setColor(k);
        vertexconsumer.addVertex(matrix4f, 20.0F, -100.0F, 20.0F).setUv(f1, f4).setColor(k);
        vertexconsumer.addVertex(matrix4f, 20.0F, -100.0F, -20.0F).setUv(f1, f2).setColor(k);
        vertexconsumer.addVertex(matrix4f, -20.0F, -100.0F, -20.0F).setUv(f3, f2).setColor(k);
    }

    private void renderStars(FogParameters pFog, float pStarBrightness, PoseStack pPoseStack) {
        Matrix4fStack matrix4fstack = RenderSystem.getModelViewStack();
        matrix4fstack.pushMatrix();
        matrix4fstack.mul(pPoseStack.last().pose());
        RenderSystem.setShaderColor(pStarBrightness, pStarBrightness, pStarBrightness, pStarBrightness);
        RenderSystem.setShaderFog(FogParameters.NO_FOG);
        RenderPipeline renderpipeline = RenderPipelines.STARS;
        GpuTexture gputexture = Minecraft.getInstance().getMainRenderTarget().getColorTexture();
        GpuTexture gputexture1 = Minecraft.getInstance().getMainRenderTarget().getDepthTexture();
        GpuBuffer gpubuffer = this.starIndices.getBuffer(this.starIndexCount);

        try (RenderPass renderpass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(gputexture, OptionalInt.empty(), gputexture1, OptionalDouble.empty())) {
            renderpass.setPipeline(renderpipeline);
            renderpass.setVertexBuffer(0, this.starBuffer);
            renderpass.setIndexBuffer(gpubuffer, this.starIndices.type());
            renderpass.drawIndexed(0, this.starIndexCount);
        }

        RenderSystem.setShaderFog(pFog);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        matrix4fstack.popMatrix();
    }

    public void renderSunriseAndSunset(PoseStack pPoseStack, MultiBufferSource.BufferSource pBufferSource, float pSunAngle, int pColor) {
        pPoseStack.pushPose();
        pPoseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        float f = Mth.sin(pSunAngle) < 0.0F ? 180.0F : 0.0F;
        pPoseStack.mulPose(Axis.ZP.rotationDegrees(f));
        pPoseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
        Matrix4f matrix4f = pPoseStack.last().pose();
        VertexConsumer vertexconsumer = pBufferSource.getBuffer(RenderType.sunriseSunset());
        float f1 = ARGB.alphaFloat(pColor);
        vertexconsumer.addVertex(matrix4f, 0.0F, 100.0F, 0.0F).setColor(pColor);
        int i = ARGB.transparent(pColor);
        int j = 16;

        for (int k = 0; k <= 16; k++) {
            float f2 = k * (float) (Math.PI * 2) / 16.0F;
            float f3 = Mth.sin(f2);
            float f4 = Mth.cos(f2);
            vertexconsumer.addVertex(matrix4f, f3 * 120.0F, f4 * 120.0F, -f4 * 40.0F * f1).setColor(i);
        }

        pPoseStack.popPose();
    }

    private static GpuBuffer buildEndSky() {
        GpuBuffer gpubuffer;
        try (ByteBufferBuilder bytebufferbuilder = new ByteBufferBuilder(24 * DefaultVertexFormat.POSITION_TEX_COLOR.getVertexSize())) {
            BufferBuilder bufferbuilder = new BufferBuilder(bytebufferbuilder, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

            for (int i = 0; i < 6; i++) {
                Matrix4f matrix4f = new Matrix4f();
                switch (i) {
                    case 1:
                        matrix4f.rotationX((float) (Math.PI / 2));
                        break;
                    case 2:
                        matrix4f.rotationX((float) (-Math.PI / 2));
                        break;
                    case 3:
                        matrix4f.rotationX((float) Math.PI);
                        break;
                    case 4:
                        matrix4f.rotationZ((float) (Math.PI / 2));
                        break;
                    case 5:
                        matrix4f.rotationZ((float) (-Math.PI / 2));
                }

                bufferbuilder.addVertex(matrix4f, -100.0F, -100.0F, -100.0F).setUv(0.0F, 0.0F).setColor(-14145496);
                bufferbuilder.addVertex(matrix4f, -100.0F, -100.0F, 100.0F).setUv(0.0F, 16.0F).setColor(-14145496);
                bufferbuilder.addVertex(matrix4f, 100.0F, -100.0F, 100.0F).setUv(16.0F, 16.0F).setColor(-14145496);
                bufferbuilder.addVertex(matrix4f, 100.0F, -100.0F, -100.0F).setUv(16.0F, 0.0F).setColor(-14145496);
            }

            try (MeshData meshdata = bufferbuilder.buildOrThrow()) {
                gpubuffer = RenderSystem.getDevice()
                    .createBuffer(() -> "End sky vertex buffer", BufferType.VERTICES, BufferUsage.STATIC_WRITE, meshdata.vertexBuffer());
            }
        }

        return gpubuffer;
    }

    public void renderEndSky() {
        TextureManager texturemanager = Minecraft.getInstance().getTextureManager();
        AbstractTexture abstracttexture = texturemanager.getTexture(END_SKY_LOCATION);
        abstracttexture.setFilter(TriState.FALSE, false);
        RenderSystem.AutoStorageIndexBuffer rendersystem$autostorageindexbuffer = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
        GpuBuffer gpubuffer = rendersystem$autostorageindexbuffer.getBuffer(36);
        GpuTexture gputexture = Minecraft.getInstance().getMainRenderTarget().getColorTexture();
        GpuTexture gputexture1 = Minecraft.getInstance().getMainRenderTarget().getDepthTexture();

        try (RenderPass renderpass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(gputexture, OptionalInt.empty(), gputexture1, OptionalDouble.empty())) {
            renderpass.setPipeline(RenderPipelines.END_SKY);
            renderpass.bindSampler("Sampler0", abstracttexture.getTexture());
            renderpass.setVertexBuffer(0, this.endSkyBuffer);
            renderpass.setIndexBuffer(gpubuffer, rendersystem$autostorageindexbuffer.type());
            renderpass.drawIndexed(0, 36);
        }
    }

    @Override
    public void close() {
        this.starBuffer.close();
        this.topSkyBuffer.close();
        this.bottomSkyBuffer.close();
        this.endSkyBuffer.close();
    }
}