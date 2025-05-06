package com.mojang.blaze3d.systems;

import com.mojang.blaze3d.DontObfuscate;
import com.mojang.blaze3d.buffers.BufferType;
import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.CompiledRenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@DontObfuscate
public interface GpuDevice {
    CommandEncoder createCommandEncoder();

    GpuTexture createTexture(@Nullable Supplier<String> pLabel, TextureFormat pFormat, int pWidth, int pHeight, int pMipLevels);

    /** Forge: same as {@link #createTexture(Supplier, TextureFormat, int, int, int)} but with stencil support */
    default GpuTexture createTexture(@Nullable Supplier<String> pLabel, TextureFormat pFormat, int pWidth, int pHeight, int pMipLevels, boolean stencil) {
        return this.createTexture(pLabel, pFormat, pWidth, pHeight, pMipLevels);
    }

    GpuTexture createTexture(@Nullable String pLabel, TextureFormat pFormat, int pWidth, int pHeight, int pMipLevels);

    /** Forge: same as {@link #createTexture(String, TextureFormat, int, int, int)} but with stencil support */
    default GpuTexture createTexture(@Nullable String pLabel, TextureFormat pFormat, int pWidth, int pHeight, int pMipLevels, boolean stencil) {
        return this.createTexture(pLabel, pFormat, pWidth, pHeight, pMipLevels);
    }

    GpuBuffer createBuffer(@Nullable Supplier<String> pLabel, BufferType pType, BufferUsage pUsage, int pSize);

    GpuBuffer createBuffer(@Nullable Supplier<String> pLabel, BufferType pType, BufferUsage pUsage, ByteBuffer pBuffer);

    String getImplementationInformation();

    List<String> getLastDebugMessages();

    boolean isDebuggingEnabled();

    String getVendor();

    String getBackendName();

    String getVersion();

    String getRenderer();

    int getMaxTextureSize();

    default CompiledRenderPipeline precompilePipeline(RenderPipeline pPipeline) {
        return this.precompilePipeline(pPipeline, null);
    }

    CompiledRenderPipeline precompilePipeline(RenderPipeline pRenderPipeline, @Nullable BiFunction<ResourceLocation, ShaderType, String> pShaderSource);

    void clearPipelineCache();

    List<String> getEnabledExtensions();

    void close();
}
