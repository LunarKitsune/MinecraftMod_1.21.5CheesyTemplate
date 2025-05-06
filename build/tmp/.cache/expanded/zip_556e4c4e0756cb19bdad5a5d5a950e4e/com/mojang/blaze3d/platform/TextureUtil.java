package com.mojang.blaze3d.platform;

import com.mojang.blaze3d.DontObfuscate;
import com.mojang.blaze3d.buffers.BufferType;
import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntUnaryOperator;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
@DontObfuscate
public class TextureUtil {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final int MIN_MIPMAP_LEVEL = 0;
    private static final int DEFAULT_IMAGE_BUFFER_SIZE = 8192;

    public static ByteBuffer readResource(InputStream pInputStream) throws IOException {
        ReadableByteChannel readablebytechannel = Channels.newChannel(pInputStream);
        return readablebytechannel instanceof SeekableByteChannel seekablebytechannel
            ? readResource(readablebytechannel, (int)seekablebytechannel.size() + 1)
            : readResource(readablebytechannel, 8192);
    }

    private static ByteBuffer readResource(ReadableByteChannel pChannel, int pSize) throws IOException {
        ByteBuffer bytebuffer = MemoryUtil.memAlloc(pSize);

        try {
            while (pChannel.read(bytebuffer) != -1) {
                if (!bytebuffer.hasRemaining()) {
                    bytebuffer = MemoryUtil.memRealloc(bytebuffer, bytebuffer.capacity() * 2);
                }
            }

            return bytebuffer;
        } catch (IOException ioexception) {
            MemoryUtil.memFree(bytebuffer);
            throw ioexception;
        }
    }

    public static void writeAsPNG(Path pPath, String pFilename, GpuTexture pTexture, int pMipLevel, IntUnaryOperator pPixelUpdater) {
        RenderSystem.assertOnRenderThread();
        int i = 0;

        for (int j = 0; j <= pMipLevel; j++) {
            i += pTexture.getFormat().pixelSize() * pTexture.getWidth(j) * pTexture.getHeight(j);
        }

        GpuBuffer gpubuffer = RenderSystem.getDevice().createBuffer(() -> "Texture output buffer", BufferType.PIXEL_PACK, BufferUsage.STATIC_READ, i);
        CommandEncoder commandencoder = RenderSystem.getDevice().createCommandEncoder();
        Runnable runnable = () -> {
            try (GpuBuffer.ReadView gpubuffer$readview = commandencoder.readBuffer(gpubuffer)) {
                int i1 = 0;

                for (int j1 = 0; j1 <= pMipLevel; j1++) {
                    int k1 = pTexture.getWidth(j1);
                    int l1 = pTexture.getHeight(j1);

                    try (NativeImage nativeimage = new NativeImage(k1, l1, false)) {
                        for (int i2 = 0; i2 < l1; i2++) {
                            for (int j2 = 0; j2 < k1; j2++) {
                                int k2 = gpubuffer$readview.data().getInt(i1 + (j2 + i2 * k1) * pTexture.getFormat().pixelSize());
                                nativeimage.setPixelABGR(j2, i2, pPixelUpdater.applyAsInt(k2));
                            }
                        }

                        Path path = pPath.resolve(pFilename + "_" + j1 + ".png");
                        nativeimage.writeToFile(path);
                        LOGGER.debug("Exported png to: {}", path.toAbsolutePath());
                    } catch (IOException ioexception) {
                        LOGGER.debug("Unable to write: ", (Throwable)ioexception);
                    }

                    i1 += pTexture.getFormat().pixelSize() * k1 * l1;
                }
            }

            gpubuffer.close();
        };
        AtomicInteger atomicinteger = new AtomicInteger();
        int k = 0;

        for (int l = 0; l <= pMipLevel; l++) {
            commandencoder.copyTextureToBuffer(pTexture, gpubuffer, k, () -> {
                if (atomicinteger.getAndIncrement() == pMipLevel) {
                    runnable.run();
                }
            }, l);
            k += pTexture.getFormat().pixelSize() * pTexture.getWidth(l) * pTexture.getHeight(l);
        }
    }

    public static Path getDebugTexturePath(Path pBasePath) {
        return pBasePath.resolve("screenshots").resolve("debug");
    }

    public static Path getDebugTexturePath() {
        return getDebugTexturePath(Path.of("."));
    }
}