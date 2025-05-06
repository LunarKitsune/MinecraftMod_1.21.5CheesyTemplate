package net.minecraft.client;

import com.mojang.blaze3d.buffers.BufferType;
import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.logging.LogUtils;
import java.io.File;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class Screenshot {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String SCREENSHOT_DIR = "screenshots";

    public static void grab(File pGameDirectory, RenderTarget pBuffer, Consumer<Component> pMessageConsumer) {
        grab(pGameDirectory, null, pBuffer, pMessageConsumer);
    }

    public static void grab(File pGameDirectory, @Nullable String pScreenshotName, RenderTarget pBuffer, Consumer<Component> pMessageConsumer) {
        takeScreenshot(
            pBuffer,
            p_389141_ -> {
                File file1 = new File(pGameDirectory, "screenshots");
                file1.mkdir();
                File file2;
                if (pScreenshotName == null) {
                    file2 = getFile(file1);
                } else {
                    file2 = new File(file1, pScreenshotName);
                }

                var event = net.minecraftforge.client.event.ForgeEventFactoryClient.onScreenshot(p_389141_, file2);
                if (event.isCanceled()) {
                    pMessageConsumer.accept(event.getCancelMessage());
                    return;
                }
                final File target = event.getScreenshotFile();

                Util.ioPool()
                    .execute(
                        () -> {
                            try {
                                NativeImage $$4x = p_389141_;

                                try {
                                    p_389141_.writeToFile(target);
                                    Component component = Component.literal(target.getName())
                                        .withStyle(ChatFormatting.UNDERLINE)
                                        .withStyle(p_389149_ -> p_389149_.withClickEvent(new ClickEvent.OpenFile(file2.getAbsoluteFile())));
                                    if (event.getResultMessage() != null)
                                        pMessageConsumer.accept(event.getResultMessage());
                                    else
                                    pMessageConsumer.accept(Component.translatable("screenshot.success", component));
                                } catch (Throwable throwable1) {
                                    if (p_389141_ != null) {
                                        try {
                                            $$4x.close();
                                        } catch (Throwable throwable) {
                                            throwable1.addSuppressed(throwable);
                                        }
                                    }

                                    throw throwable1;
                                }

                                if (p_389141_ != null) {
                                    p_389141_.close();
                                }
                            } catch (Exception exception) {
                                LOGGER.warn("Couldn't save screenshot", (Throwable)exception);
                                pMessageConsumer.accept(Component.translatable("screenshot.failure", exception.getMessage()));
                            }
                        }
                    );
            }
        );
    }

    public static void takeScreenshot(RenderTarget pRenderTarget, Consumer<NativeImage> pWriter) {
        int i = pRenderTarget.width;
        int j = pRenderTarget.height;
        GpuTexture gputexture = pRenderTarget.getColorTexture();
        if (gputexture == null) {
            throw new IllegalStateException("Tried to capture screenshot of an incomplete framebuffer");
        } else {
            GpuBuffer gpubuffer = RenderSystem.getDevice()
                .createBuffer(() -> "Screenshot buffer", BufferType.PIXEL_PACK, BufferUsage.STATIC_READ, i * j * gputexture.getFormat().pixelSize());
            CommandEncoder commandencoder = RenderSystem.getDevice().createCommandEncoder();
            RenderSystem.getDevice().createCommandEncoder().copyTextureToBuffer(gputexture, gpubuffer, 0, () -> {
                try (GpuBuffer.ReadView gpubuffer$readview = commandencoder.readBuffer(gpubuffer)) {
                    NativeImage nativeimage = new NativeImage(i, j, false);

                    for (int k = 0; k < j; k++) {
                        for (int l = 0; l < i; l++) {
                            int i1 = gpubuffer$readview.data().getInt((l + k * i) * gputexture.getFormat().pixelSize());
                            nativeimage.setPixelABGR(l, j - k - 1, i1 | 0xFF000000);
                        }
                    }

                    pWriter.accept(nativeimage);
                }

                gpubuffer.close();
            }, 0);
        }
    }

    private static File getFile(File pGameDirectory) {
        String s = Util.getFilenameFormattedDateTime();
        int i = 1;

        while (true) {
            File file1 = new File(pGameDirectory, s + (i == 1 ? "" : "_" + i) + ".png");
            if (!file1.exists()) {
                return file1;
            }

            i++;
        }
    }
}
