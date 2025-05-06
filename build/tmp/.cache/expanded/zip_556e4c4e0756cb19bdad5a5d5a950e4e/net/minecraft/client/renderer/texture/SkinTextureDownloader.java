package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import net.minecraft.FileUtil;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class SkinTextureDownloader {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int SKIN_WIDTH = 64;
    private static final int SKIN_HEIGHT = 64;
    private static final int LEGACY_SKIN_HEIGHT = 32;

    public static CompletableFuture<ResourceLocation> downloadAndRegisterSkin(ResourceLocation pTextureLocation, Path pPath, String pUrl, boolean pIsLegacySkin) {
        return CompletableFuture.<NativeImage>supplyAsync(() -> {
            NativeImage nativeimage;
            try {
                nativeimage = downloadSkin(pPath, pUrl);
            } catch (IOException ioexception) {
                throw new UncheckedIOException(ioexception);
            }

            return pIsLegacySkin ? processLegacySkin(nativeimage, pUrl) : nativeimage;
        }, Util.nonCriticalIoPool().forName("downloadTexture")).thenCompose(p_378652_ -> registerTextureInManager(pTextureLocation, p_378652_));
    }

    private static NativeImage downloadSkin(Path pPath, String pUrl) throws IOException {
        if (Files.isRegularFile(pPath)) {
            LOGGER.debug("Loading HTTP texture from local cache ({})", pPath);

            NativeImage nativeimage1;
            try (InputStream inputstream = Files.newInputStream(pPath)) {
                nativeimage1 = NativeImage.read(inputstream);
            }

            return nativeimage1;
        } else {
            HttpURLConnection httpurlconnection = null;
            LOGGER.debug("Downloading HTTP texture from {} to {}", pUrl, pPath);
            URI uri = URI.create(pUrl);

            NativeImage $$7;
            try {
                httpurlconnection = (HttpURLConnection)uri.toURL().openConnection(Minecraft.getInstance().getProxy());
                httpurlconnection.setDoInput(true);
                httpurlconnection.setDoOutput(false);
                httpurlconnection.connect();
                int i = httpurlconnection.getResponseCode();
                if (i / 100 != 2) {
                    throw new IOException("Failed to open " + uri + ", HTTP error code: " + i);
                }

                byte[] abyte = httpurlconnection.getInputStream().readAllBytes();

                try {
                    FileUtil.createDirectoriesSafe(pPath.getParent());
                    Files.write(pPath, abyte);
                } catch (IOException ioexception) {
                    LOGGER.warn("Failed to cache texture {} in {}", pUrl, pPath);
                }

                $$7 = NativeImage.read(abyte);
            } finally {
                if (httpurlconnection != null) {
                    httpurlconnection.disconnect();
                }
            }

            return $$7;
        }
    }

    private static CompletableFuture<ResourceLocation> registerTextureInManager(ResourceLocation pLocation, NativeImage pImage) {
        Minecraft minecraft = Minecraft.getInstance();
        return CompletableFuture.supplyAsync(() -> {
            minecraft.getTextureManager().register(pLocation, new DynamicTexture(pLocation::toString, pImage));
            return pLocation;
        }, minecraft);
    }

    private static NativeImage processLegacySkin(NativeImage pImage, String pUrl) {
        int i = pImage.getHeight();
        int j = pImage.getWidth();
        if (j == 64 && (i == 32 || i == 64)) {
            boolean flag = i == 32;
            if (flag) {
                NativeImage nativeimage = new NativeImage(64, 64, true);
                nativeimage.copyFrom(pImage);
                pImage.close();
                pImage = nativeimage;
                nativeimage.fillRect(0, 32, 64, 32, 0);
                nativeimage.copyRect(4, 16, 16, 32, 4, 4, true, false);
                nativeimage.copyRect(8, 16, 16, 32, 4, 4, true, false);
                nativeimage.copyRect(0, 20, 24, 32, 4, 12, true, false);
                nativeimage.copyRect(4, 20, 16, 32, 4, 12, true, false);
                nativeimage.copyRect(8, 20, 8, 32, 4, 12, true, false);
                nativeimage.copyRect(12, 20, 16, 32, 4, 12, true, false);
                nativeimage.copyRect(44, 16, -8, 32, 4, 4, true, false);
                nativeimage.copyRect(48, 16, -8, 32, 4, 4, true, false);
                nativeimage.copyRect(40, 20, 0, 32, 4, 12, true, false);
                nativeimage.copyRect(44, 20, -8, 32, 4, 12, true, false);
                nativeimage.copyRect(48, 20, -16, 32, 4, 12, true, false);
                nativeimage.copyRect(52, 20, -8, 32, 4, 12, true, false);
            }

            setNoAlpha(pImage, 0, 0, 32, 16);
            if (flag) {
                doNotchTransparencyHack(pImage, 32, 0, 64, 32);
            }

            setNoAlpha(pImage, 0, 16, 64, 32);
            setNoAlpha(pImage, 16, 48, 48, 64);
            return pImage;
        } else {
            pImage.close();
            throw new IllegalStateException("Discarding incorrectly sized (" + j + "x" + i + ") skin texture from " + pUrl);
        }
    }

    private static void doNotchTransparencyHack(NativeImage pImage, int pMinX, int pMinY, int pMaxX, int pMaxY) {
        for (int i = pMinX; i < pMaxX; i++) {
            for (int j = pMinY; j < pMaxY; j++) {
                int k = pImage.getPixel(i, j);
                if (ARGB.alpha(k) < 128) {
                    return;
                }
            }
        }

        for (int l = pMinX; l < pMaxX; l++) {
            for (int i1 = pMinY; i1 < pMaxY; i1++) {
                pImage.setPixel(l, i1, pImage.getPixel(l, i1) & 16777215);
            }
        }
    }

    private static void setNoAlpha(NativeImage pImage, int pMinX, int pMinY, int pMaxX, int pMaxY) {
        for (int i = pMinX; i < pMaxX; i++) {
            for (int j = pMinY; j < pMaxY; j++) {
                pImage.setPixel(i, j, ARGB.opaque(pImage.getPixel(i, j)));
            }
        }
    }
}