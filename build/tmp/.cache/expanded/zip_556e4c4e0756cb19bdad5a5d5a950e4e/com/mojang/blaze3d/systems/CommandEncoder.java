package com.mojang.blaze3d.systems;

import com.mojang.blaze3d.DontObfuscate;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.textures.GpuTexture;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import javax.annotation.Nullable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@DontObfuscate
public interface CommandEncoder {
    RenderPass createRenderPass(GpuTexture pTexture, OptionalInt pColor);

    RenderPass createRenderPass(GpuTexture pColorTexture, OptionalInt pColor, @Nullable GpuTexture pDepthTexture, OptionalDouble pClearDepth);

    void clearColorTexture(GpuTexture pTexture, int pColor);

    void clearColorAndDepthTextures(GpuTexture pColorTexture, int pColor, GpuTexture pDepthTexture, double pClearDepth);

    void clearDepthTexture(GpuTexture pDepthTexture, double pClearDepth);

    void writeToBuffer(GpuBuffer pBuffer, ByteBuffer pSource, int pOffset);

    GpuBuffer.ReadView readBuffer(GpuBuffer pBuffer);

    GpuBuffer.ReadView readBuffer(GpuBuffer pBuffer, int pOffset, int pLength);

    void writeToTexture(GpuTexture pTexture, NativeImage pImage);

    void writeToTexture(
        GpuTexture pTexture, NativeImage pImage, int pMipLevel, int pX, int pY, int pWidth, int pHeight, int pSourceX, int pSourceY
    );

    void writeToTexture(
        GpuTexture pTexture, IntBuffer pBuffer, NativeImage.Format pFormat, int pMipLevel, int pX, int pY, int pWidth, int pHeight
    );

    void copyTextureToBuffer(GpuTexture pTexture, GpuBuffer pBuffer, int pOffset, Runnable pTask, int pMipLevel);

    void copyTextureToBuffer(
        GpuTexture pTexture, GpuBuffer pBuffer, int pOffset, Runnable pTask, int pMipLevel, int pX, int pY, int pWidth, int pHeight
    );

    void copyTextureToTexture(
        GpuTexture pSource, GpuTexture pDestination, int pMipLevel, int pX, int pY, int pSourceX, int pSourceY, int pWidth, int pHeight
    );

    void presentTexture(GpuTexture pTexture);
}