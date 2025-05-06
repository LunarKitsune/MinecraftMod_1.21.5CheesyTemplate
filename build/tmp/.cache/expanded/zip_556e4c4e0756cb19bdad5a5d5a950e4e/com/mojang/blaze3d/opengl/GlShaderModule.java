package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.StringUtils;

@OnlyIn(Dist.CLIENT)
public class GlShaderModule implements AutoCloseable {
    private static final int NOT_ALLOCATED = -1;
    public static final GlShaderModule INVALID_SHADER = new GlShaderModule(-1, ResourceLocation.withDefaultNamespace("invalid"), ShaderType.VERTEX);
    private final ResourceLocation id;
    private int shaderId;
    private final ShaderType type;

    public GlShaderModule(int pShaderId, ResourceLocation pId, ShaderType pType) {
        this.id = pId;
        this.shaderId = pShaderId;
        this.type = pType;
    }

    public static GlShaderModule compile(ResourceLocation pId, ShaderType pType, String pSource) throws ShaderManager.CompilationException {
        RenderSystem.assertOnRenderThread();
        int i = GlStateManager.glCreateShader(GlConst.toGl(pType));
        GlStateManager.glShaderSource(i, pSource);
        GlStateManager.glCompileShader(i);
        if (GlStateManager.glGetShaderi(i, 35713) == 0) {
            String s = StringUtils.trim(GlStateManager.glGetShaderInfoLog(i, 32768));
            throw new ShaderManager.CompilationException("Couldn't compile " + pType.getName() + " shader (" + pId + ") : " + s);
        } else {
            return new GlShaderModule(i, pId, pType);
        }
    }

    @Override
    public void close() {
        if (this.shaderId == -1) {
            throw new IllegalStateException("Already closed");
        } else {
            RenderSystem.assertOnRenderThread();
            GlStateManager.glDeleteShader(this.shaderId);
            this.shaderId = -1;
        }
    }

    public ResourceLocation getId() {
        return this.id;
    }

    public int getShaderId() {
        return this.shaderId;
    }

    public String getDebugLabel() {
        return this.type.idConverter().idToFile(this.id).toString();
    }
}