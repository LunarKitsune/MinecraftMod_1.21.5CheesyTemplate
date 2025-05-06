package net.minecraft.client.renderer;

import com.mojang.blaze3d.buffers.BufferType;
import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import javax.annotation.Nullable;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class CloudRenderer extends SimplePreparableReloadListener<Optional<CloudRenderer.TextureData>> implements AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation TEXTURE_LOCATION = ResourceLocation.withDefaultNamespace("textures/environment/clouds.png");
    private static final float CELL_SIZE_IN_BLOCKS = 12.0F;
    private static final float HEIGHT_IN_BLOCKS = 4.0F;
    private static final float BLOCKS_PER_SECOND = 0.6F;
    private static final long EMPTY_CELL = 0L;
    private static final int COLOR_OFFSET = 4;
    private static final int NORTH_OFFSET = 3;
    private static final int EAST_OFFSET = 2;
    private static final int SOUTH_OFFSET = 1;
    private static final int WEST_OFFSET = 0;
    private boolean needsRebuild = true;
    private int prevCellX = Integer.MIN_VALUE;
    private int prevCellZ = Integer.MIN_VALUE;
    private CloudRenderer.RelativeCameraPos prevRelativeCameraPos = CloudRenderer.RelativeCameraPos.INSIDE_CLOUDS;
    @Nullable
    private CloudStatus prevType;
    @Nullable
    private CloudRenderer.TextureData texture;
    @Nullable
    private GpuBuffer vertexBuffer = null;
    private int indexCount = 0;
    private final RenderSystem.AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);

    protected Optional<CloudRenderer.TextureData> prepare(ResourceManager p_361257_, ProfilerFiller p_362196_) {
        try {
            Optional optional;
            try (
                InputStream inputstream = p_361257_.open(TEXTURE_LOCATION);
                NativeImage nativeimage = NativeImage.read(inputstream);
            ) {
                int i = nativeimage.getWidth();
                int j = nativeimage.getHeight();
                long[] along = new long[i * j];

                for (int k = 0; k < j; k++) {
                    for (int l = 0; l < i; l++) {
                        int i1 = nativeimage.getPixel(l, k);
                        if (isCellEmpty(i1)) {
                            along[l + k * i] = 0L;
                        } else {
                            boolean flag = isCellEmpty(nativeimage.getPixel(l, Math.floorMod(k - 1, j)));
                            boolean flag1 = isCellEmpty(nativeimage.getPixel(Math.floorMod(l + 1, j), k));
                            boolean flag2 = isCellEmpty(nativeimage.getPixel(l, Math.floorMod(k + 1, j)));
                            boolean flag3 = isCellEmpty(nativeimage.getPixel(Math.floorMod(l - 1, j), k));
                            along[l + k * i] = packCellData(i1, flag, flag1, flag2, flag3);
                        }
                    }
                }

                optional = Optional.of(new CloudRenderer.TextureData(along, i, j));
            }

            return optional;
        } catch (IOException ioexception) {
            LOGGER.error("Failed to load cloud texture", (Throwable)ioexception);
            return Optional.empty();
        }
    }

    protected void apply(Optional<CloudRenderer.TextureData> p_370042_, ResourceManager p_368869_, ProfilerFiller p_367795_) {
        this.texture = p_370042_.orElse(null);
        this.needsRebuild = true;
    }

    private static boolean isCellEmpty(int pColor) {
        return ARGB.alpha(pColor) < 10;
    }

    private static long packCellData(int pColor, boolean pNorthEmpty, boolean pEastEmpty, boolean pSouthEmpty, boolean pWestEmpty) {
        return (long)pColor << 4 | (pNorthEmpty ? 1 : 0) << 3 | (pEastEmpty ? 1 : 0) << 2 | (pSouthEmpty ? 1 : 0) << 1 | (pWestEmpty ? 1 : 0) << 0;
    }

    private static int getColor(long pCellData) {
        return (int)(pCellData >> 4 & 4294967295L);
    }

    private static boolean isNorthEmpty(long pCellData) {
        return (pCellData >> 3 & 1L) != 0L;
    }

    private static boolean isEastEmpty(long pCellData) {
        return (pCellData >> 2 & 1L) != 0L;
    }

    private static boolean isSouthEmpty(long pCellData) {
        return (pCellData >> 1 & 1L) != 0L;
    }

    private static boolean isWestEmpty(long pCellData) {
        return (pCellData >> 0 & 1L) != 0L;
    }

    public void render(int pCloudColor, CloudStatus pCloudStatus, float pHeight, Vec3 pCameraPosition, float pTicks) {
        if (this.texture != null) {
            float f = (float)(pHeight - pCameraPosition.y);
            float f1 = f + 4.0F;
            CloudRenderer.RelativeCameraPos cloudrenderer$relativecamerapos;
            if (f1 < 0.0F) {
                cloudrenderer$relativecamerapos = CloudRenderer.RelativeCameraPos.ABOVE_CLOUDS;
            } else if (f > 0.0F) {
                cloudrenderer$relativecamerapos = CloudRenderer.RelativeCameraPos.BELOW_CLOUDS;
            } else {
                cloudrenderer$relativecamerapos = CloudRenderer.RelativeCameraPos.INSIDE_CLOUDS;
            }

            double d0 = pCameraPosition.x + pTicks * 0.030000001F;
            double d1 = pCameraPosition.z + 3.96F;
            double d2 = this.texture.width * 12.0;
            double d3 = this.texture.height * 12.0;
            d0 -= Mth.floor(d0 / d2) * d2;
            d1 -= Mth.floor(d1 / d3) * d3;
            int i = Mth.floor(d0 / 12.0);
            int j = Mth.floor(d1 / 12.0);
            float f2 = (float)(d0 - i * 12.0F);
            float f3 = (float)(d1 - j * 12.0F);
            boolean flag = pCloudStatus == CloudStatus.FANCY;
            RenderPipeline renderpipeline = flag ? RenderPipelines.CLOUDS : RenderPipelines.FLAT_CLOUDS;
            if (this.needsRebuild
                || i != this.prevCellX
                || j != this.prevCellZ
                || cloudrenderer$relativecamerapos != this.prevRelativeCameraPos
                || pCloudStatus != this.prevType) {
                this.needsRebuild = false;
                this.prevCellX = i;
                this.prevCellZ = j;
                this.prevRelativeCameraPos = cloudrenderer$relativecamerapos;
                this.prevType = pCloudStatus;

                try (MeshData meshdata = this.buildMesh(Tesselator.getInstance(), i, j, pCloudStatus, cloudrenderer$relativecamerapos, renderpipeline)) {
                    if (meshdata == null) {
                        this.indexCount = 0;
                    } else {
                        if (this.vertexBuffer != null && this.vertexBuffer.size >= meshdata.vertexBuffer().remaining()) {
                            CommandEncoder commandencoder = RenderSystem.getDevice().createCommandEncoder();
                            commandencoder.writeToBuffer(this.vertexBuffer, meshdata.vertexBuffer(), 0);
                        } else {
                            if (this.vertexBuffer != null) {
                                this.vertexBuffer.close();
                            }

                            this.vertexBuffer = RenderSystem.getDevice()
                                .createBuffer(() -> "Cloud vertex buffer", BufferType.VERTICES, BufferUsage.DYNAMIC_WRITE, meshdata.vertexBuffer());
                        }

                        this.indexCount = meshdata.drawState().indexCount();
                    }
                }
            }

            if (this.indexCount != 0) {
                RenderSystem.setShaderColor(ARGB.redFloat(pCloudColor), ARGB.greenFloat(pCloudColor), ARGB.blueFloat(pCloudColor), 1.0F);
                if (flag) {
                    this.draw(RenderPipelines.CLOUDS_DEPTH_ONLY, f2, f, f3);
                }

                this.draw(renderpipeline, f2, f, f3);
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            }
        }
    }

    private void draw(RenderPipeline pPipeline, float pX, float pY, float pZ) {
        RenderSystem.setModelOffset(-pX, pY, -pZ);
        RenderTarget rendertarget = Minecraft.getInstance().getMainRenderTarget();
        RenderTarget rendertarget1 = Minecraft.getInstance().levelRenderer.getCloudsTarget();
        GpuTexture gputexture;
        GpuTexture gputexture1;
        if (rendertarget1 != null) {
            gputexture = rendertarget1.getColorTexture();
            gputexture1 = rendertarget1.getDepthTexture();
        } else {
            gputexture = rendertarget.getColorTexture();
            gputexture1 = rendertarget.getDepthTexture();
        }

        GpuBuffer gpubuffer = this.indices.getBuffer(this.indexCount);

        try (RenderPass renderpass = RenderSystem.getDevice()
                .createCommandEncoder()
                .createRenderPass(gputexture, OptionalInt.empty(), gputexture1, OptionalDouble.empty())) {
            renderpass.setPipeline(pPipeline);
            renderpass.setIndexBuffer(gpubuffer, this.indices.type());
            renderpass.setVertexBuffer(0, this.vertexBuffer);
            renderpass.drawIndexed(0, this.indexCount);
        }

        RenderSystem.resetModelOffset();
    }

    @Nullable
    private MeshData buildMesh(
        Tesselator pTesselator, int pCellX, int pCellY, CloudStatus pCloudStatus, CloudRenderer.RelativeCameraPos pRelativeCameraPos, RenderPipeline pPipeline
    ) {
        float f = 0.8F;
        int i = ARGB.colorFromFloat(0.8F, 1.0F, 1.0F, 1.0F);
        int j = ARGB.colorFromFloat(0.8F, 0.9F, 0.9F, 0.9F);
        int k = ARGB.colorFromFloat(0.8F, 0.7F, 0.7F, 0.7F);
        int l = ARGB.colorFromFloat(0.8F, 0.8F, 0.8F, 0.8F);
        BufferBuilder bufferbuilder = pTesselator.begin(pPipeline.getVertexFormatMode(), pPipeline.getVertexFormat());
        this.buildMesh(pRelativeCameraPos, bufferbuilder, pCellX, pCellY, k, i, j, l, pCloudStatus == CloudStatus.FANCY);
        return bufferbuilder.build();
    }

    private void buildMesh(
        CloudRenderer.RelativeCameraPos pRelativeCameraPos,
        BufferBuilder pBufferBuilder,
        int pCellX,
        int pCellZ,
        int pBottomColor,
        int pTopColor,
        int pSideColor,
        int pFrontColor,
        boolean pFancyClouds
    ) {
        if (this.texture != null) {
            int i = 32;
            long[] along = this.texture.cells;
            int j = this.texture.width;
            int k = this.texture.height;

            for (int l = -32; l <= 32; l++) {
                for (int i1 = -32; i1 <= 32; i1++) {
                    int j1 = Math.floorMod(pCellX + i1, j);
                    int k1 = Math.floorMod(pCellZ + l, k);
                    long l1 = along[j1 + k1 * j];
                    if (l1 != 0L) {
                        int i2 = getColor(l1);
                        if (pFancyClouds) {
                            this.buildExtrudedCell(
                                pRelativeCameraPos,
                                pBufferBuilder,
                                ARGB.multiply(pBottomColor, i2),
                                ARGB.multiply(pTopColor, i2),
                                ARGB.multiply(pSideColor, i2),
                                ARGB.multiply(pFrontColor, i2),
                                i1,
                                l,
                                l1
                            );
                        } else {
                            this.buildFlatCell(pBufferBuilder, ARGB.multiply(pTopColor, i2), i1, l);
                        }
                    }
                }
            }
        }
    }

    private void buildFlatCell(BufferBuilder pBufferBuilder, int pColor, int pX, int pY) {
        float f = pX * 12.0F;
        float f1 = f + 12.0F;
        float f2 = pY * 12.0F;
        float f3 = f2 + 12.0F;
        pBufferBuilder.addVertex(f, 0.0F, f2).setColor(pColor);
        pBufferBuilder.addVertex(f, 0.0F, f3).setColor(pColor);
        pBufferBuilder.addVertex(f1, 0.0F, f3).setColor(pColor);
        pBufferBuilder.addVertex(f1, 0.0F, f2).setColor(pColor);
    }

    private void buildExtrudedCell(
        CloudRenderer.RelativeCameraPos pRelativeCameraPos,
        BufferBuilder pBufferBuilder,
        int pBottomColor,
        int pTopColor,
        int pSideColor,
        int pFrontColor,
        int pX,
        int pY,
        long pCellData
    ) {
        float f = pX * 12.0F;
        float f1 = f + 12.0F;
        float f2 = 0.0F;
        float f3 = 4.0F;
        float f4 = pY * 12.0F;
        float f5 = f4 + 12.0F;
        if (pRelativeCameraPos != CloudRenderer.RelativeCameraPos.BELOW_CLOUDS) {
            pBufferBuilder.addVertex(f, 4.0F, f4).setColor(pTopColor);
            pBufferBuilder.addVertex(f, 4.0F, f5).setColor(pTopColor);
            pBufferBuilder.addVertex(f1, 4.0F, f5).setColor(pTopColor);
            pBufferBuilder.addVertex(f1, 4.0F, f4).setColor(pTopColor);
        }

        if (pRelativeCameraPos != CloudRenderer.RelativeCameraPos.ABOVE_CLOUDS) {
            pBufferBuilder.addVertex(f1, 0.0F, f4).setColor(pBottomColor);
            pBufferBuilder.addVertex(f1, 0.0F, f5).setColor(pBottomColor);
            pBufferBuilder.addVertex(f, 0.0F, f5).setColor(pBottomColor);
            pBufferBuilder.addVertex(f, 0.0F, f4).setColor(pBottomColor);
        }

        if (isNorthEmpty(pCellData) && pY > 0) {
            pBufferBuilder.addVertex(f, 0.0F, f4).setColor(pFrontColor);
            pBufferBuilder.addVertex(f, 4.0F, f4).setColor(pFrontColor);
            pBufferBuilder.addVertex(f1, 4.0F, f4).setColor(pFrontColor);
            pBufferBuilder.addVertex(f1, 0.0F, f4).setColor(pFrontColor);
        }

        if (isSouthEmpty(pCellData) && pY < 0) {
            pBufferBuilder.addVertex(f1, 0.0F, f5).setColor(pFrontColor);
            pBufferBuilder.addVertex(f1, 4.0F, f5).setColor(pFrontColor);
            pBufferBuilder.addVertex(f, 4.0F, f5).setColor(pFrontColor);
            pBufferBuilder.addVertex(f, 0.0F, f5).setColor(pFrontColor);
        }

        if (isWestEmpty(pCellData) && pX > 0) {
            pBufferBuilder.addVertex(f, 0.0F, f5).setColor(pSideColor);
            pBufferBuilder.addVertex(f, 4.0F, f5).setColor(pSideColor);
            pBufferBuilder.addVertex(f, 4.0F, f4).setColor(pSideColor);
            pBufferBuilder.addVertex(f, 0.0F, f4).setColor(pSideColor);
        }

        if (isEastEmpty(pCellData) && pX < 0) {
            pBufferBuilder.addVertex(f1, 0.0F, f4).setColor(pSideColor);
            pBufferBuilder.addVertex(f1, 4.0F, f4).setColor(pSideColor);
            pBufferBuilder.addVertex(f1, 4.0F, f5).setColor(pSideColor);
            pBufferBuilder.addVertex(f1, 0.0F, f5).setColor(pSideColor);
        }

        boolean flag = Math.abs(pX) <= 1 && Math.abs(pY) <= 1;
        if (flag) {
            pBufferBuilder.addVertex(f1, 4.0F, f4).setColor(pTopColor);
            pBufferBuilder.addVertex(f1, 4.0F, f5).setColor(pTopColor);
            pBufferBuilder.addVertex(f, 4.0F, f5).setColor(pTopColor);
            pBufferBuilder.addVertex(f, 4.0F, f4).setColor(pTopColor);
            pBufferBuilder.addVertex(f, 0.0F, f4).setColor(pBottomColor);
            pBufferBuilder.addVertex(f, 0.0F, f5).setColor(pBottomColor);
            pBufferBuilder.addVertex(f1, 0.0F, f5).setColor(pBottomColor);
            pBufferBuilder.addVertex(f1, 0.0F, f4).setColor(pBottomColor);
            pBufferBuilder.addVertex(f1, 0.0F, f4).setColor(pFrontColor);
            pBufferBuilder.addVertex(f1, 4.0F, f4).setColor(pFrontColor);
            pBufferBuilder.addVertex(f, 4.0F, f4).setColor(pFrontColor);
            pBufferBuilder.addVertex(f, 0.0F, f4).setColor(pFrontColor);
            pBufferBuilder.addVertex(f, 0.0F, f5).setColor(pFrontColor);
            pBufferBuilder.addVertex(f, 4.0F, f5).setColor(pFrontColor);
            pBufferBuilder.addVertex(f1, 4.0F, f5).setColor(pFrontColor);
            pBufferBuilder.addVertex(f1, 0.0F, f5).setColor(pFrontColor);
            pBufferBuilder.addVertex(f, 0.0F, f4).setColor(pSideColor);
            pBufferBuilder.addVertex(f, 4.0F, f4).setColor(pSideColor);
            pBufferBuilder.addVertex(f, 4.0F, f5).setColor(pSideColor);
            pBufferBuilder.addVertex(f, 0.0F, f5).setColor(pSideColor);
            pBufferBuilder.addVertex(f1, 0.0F, f5).setColor(pSideColor);
            pBufferBuilder.addVertex(f1, 4.0F, f5).setColor(pSideColor);
            pBufferBuilder.addVertex(f1, 4.0F, f4).setColor(pSideColor);
            pBufferBuilder.addVertex(f1, 0.0F, f4).setColor(pSideColor);
        }
    }

    public void markForRebuild() {
        this.needsRebuild = true;
    }

    @Override
    public void close() {
        if (this.vertexBuffer != null) {
            this.vertexBuffer.close();
        }
    }

    @OnlyIn(Dist.CLIENT)
    static enum RelativeCameraPos {
        ABOVE_CLOUDS,
        INSIDE_CLOUDS,
        BELOW_CLOUDS;
    }

    @OnlyIn(Dist.CLIENT)
    public record TextureData(long[] cells, int width, int height) {
    }
}