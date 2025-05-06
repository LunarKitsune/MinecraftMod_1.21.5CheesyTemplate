package com.mojang.blaze3d.opengl;

import com.google.common.collect.Sets;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.FogParameters;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.VisibleForTesting;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class GlProgram implements AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static Set<String> BUILT_IN_UNIFORMS = Sets.newHashSet(
        "ModelViewMat",
        "ProjMat",
        "TextureMat",
        "ScreenSize",
        "ColorModulator",
        "Light0_Direction",
        "Light1_Direction",
        "GlintAlpha",
        "FogStart",
        "FogEnd",
        "FogColor",
        "FogShape",
        "LineWidth",
        "GameTime",
        "ModelOffset"
    );
    public static GlProgram INVALID_PROGRAM = new GlProgram(-1, "invalid");
    private static final AbstractUniform DUMMY_UNIFORM = new AbstractUniform();
    private final List<String> samplers = new ArrayList<>();
    private final Object2ObjectMap<String, GpuTexture> samplerTextures = new Object2ObjectOpenHashMap<>();
    private final IntList samplerLocations = new IntArrayList();
    private final List<Uniform> uniforms = new ArrayList<>();
    private final Map<String, Uniform> uniformsByName = new HashMap<>();
    private final int programId;
    private final String debugLabel;
    @Nullable
    public Uniform MODEL_VIEW_MATRIX;
    @Nullable
    public Uniform PROJECTION_MATRIX;
    @Nullable
    public Uniform TEXTURE_MATRIX;
    @Nullable
    public Uniform SCREEN_SIZE;
    @Nullable
    public Uniform COLOR_MODULATOR;
    @Nullable
    public Uniform LIGHT0_DIRECTION;
    @Nullable
    public Uniform LIGHT1_DIRECTION;
    @Nullable
    public Uniform GLINT_ALPHA;
    @Nullable
    public Uniform FOG_START;
    @Nullable
    public Uniform FOG_END;
    @Nullable
    public Uniform FOG_COLOR;
    @Nullable
    public Uniform FOG_SHAPE;
    @Nullable
    public Uniform LINE_WIDTH;
    @Nullable
    public Uniform GAME_TIME;
    @Nullable
    public Uniform MODEL_OFFSET;

    private GlProgram(int pProgramId, String pDebugLabel) {
        this.programId = pProgramId;
        this.debugLabel = pDebugLabel;
    }

    public static GlProgram link(GlShaderModule pVertexShader, GlShaderModule pFragmentShader, VertexFormat pVertexFormat, String pDebugLabel) throws ShaderManager.CompilationException {
        int i = GlStateManager.glCreateProgram();
        if (i <= 0) {
            throw new ShaderManager.CompilationException("Could not create shader program (returned program ID " + i + ")");
        } else {
            int j = 0;

            for (String s : pVertexFormat.getElementAttributeNames()) {
                GlStateManager._glBindAttribLocation(i, j, s);
                j++;
            }

            GlStateManager.glAttachShader(i, pVertexShader.getShaderId());
            GlStateManager.glAttachShader(i, pFragmentShader.getShaderId());
            GlStateManager.glLinkProgram(i);
            int k = GlStateManager.glGetProgrami(i, 35714);
            if (k == 0) {
                String s1 = GlStateManager.glGetProgramInfoLog(i, 32768);
                throw new ShaderManager.CompilationException(
                    "Error encountered when linking program containing VS "
                        + pVertexShader.getId()
                        + " and FS "
                        + pFragmentShader.getId()
                        + ". Log output: "
                        + s1
                );
            } else {
                return new GlProgram(i, pDebugLabel);
            }
        }
    }

    public void setupUniforms(List<RenderPipeline.UniformDescription> pUniforms, List<String> pSamplers) {
        RenderSystem.assertOnRenderThread();

        for (RenderPipeline.UniformDescription renderpipeline$uniformdescription : pUniforms) {
            String s = renderpipeline$uniformdescription.name();
            int i = Uniform.glGetUniformLocation(this.programId, s);
            if (i != -1) {
                Uniform uniform = this.createUniform(renderpipeline$uniformdescription);
                uniform.setLocation(i);
                this.uniforms.add(uniform);
                this.uniformsByName.put(s, uniform);
            }
        }

        for (String s2 : pSamplers) {
            int k = Uniform.glGetUniformLocation(this.programId, s2);
            if (k == -1) {
                LOGGER.warn("{} shader program does not use sampler {} defined in the pipeline. This might be a bug.", this.debugLabel, s2);
            } else {
                this.samplers.add(s2);
                this.samplerLocations.add(k);
            }
        }

        int j = GlStateManager.glGetProgrami(this.programId, 35718);

        try (MemoryStack memorystack = MemoryStack.stackPush()) {
            IntBuffer intbuffer = memorystack.mallocInt(1);
            IntBuffer intbuffer1 = memorystack.mallocInt(1);

            for (int l = 0; l < j; l++) {
                String s1 = GL20.glGetActiveUniform(this.programId, l, intbuffer, intbuffer1);
                UniformType uniformtype = getTypeFromGl(intbuffer1.get(0));
                if (!this.uniformsByName.containsKey(s1) && !pSamplers.contains(s1)) {
                    if (uniformtype != null) {
                        LOGGER.info("Found unknown but potentially supported uniform {} in {}", s1, this.debugLabel);
                        Uniform uniform1 = new Uniform(s1, uniformtype);
                        uniform1.setLocation(l);
                        this.uniforms.add(uniform1);
                        this.uniformsByName.put(s1, uniform1);
                    } else {
                        LOGGER.warn("Found unknown and unsupported uniform {} in {}", s1, this.debugLabel);
                    }
                }
            }
        }

        this.MODEL_VIEW_MATRIX = this.getUniform("ModelViewMat");
        this.PROJECTION_MATRIX = this.getUniform("ProjMat");
        this.TEXTURE_MATRIX = this.getUniform("TextureMat");
        this.SCREEN_SIZE = this.getUniform("ScreenSize");
        this.COLOR_MODULATOR = this.getUniform("ColorModulator");
        this.LIGHT0_DIRECTION = this.getUniform("Light0_Direction");
        this.LIGHT1_DIRECTION = this.getUniform("Light1_Direction");
        this.GLINT_ALPHA = this.getUniform("GlintAlpha");
        this.FOG_START = this.getUniform("FogStart");
        this.FOG_END = this.getUniform("FogEnd");
        this.FOG_COLOR = this.getUniform("FogColor");
        this.FOG_SHAPE = this.getUniform("FogShape");
        this.LINE_WIDTH = this.getUniform("LineWidth");
        this.GAME_TIME = this.getUniform("GameTime");
        this.MODEL_OFFSET = this.getUniform("ModelOffset");
    }

    private Uniform createUniform(RenderPipeline.UniformDescription pDescription) {
        return new Uniform(pDescription.name(), pDescription.type());
    }

    @Override
    public void close() {
        this.uniforms.forEach(Uniform::close);
        GlStateManager.glDeleteProgram(this.programId);
    }

    public void clear() {
        RenderSystem.assertOnRenderThread();
        GlStateManager._glUseProgram(0);
        int i = GlStateManager._getActiveTexture();

        for (int j = 0; j < this.samplerLocations.size(); j++) {
            String s = this.samplers.get(j);
            if (!this.samplerTextures.containsKey(s)) {
                GlStateManager._activeTexture(33984 + j);
                GlStateManager._bindTexture(0);
            }
        }

        GlStateManager._activeTexture(i);
    }

    @Nullable
    public Uniform getUniform(String pName) {
        RenderSystem.assertOnRenderThread();
        return this.uniformsByName.get(pName);
    }

    public AbstractUniform safeGetUniform(String pName) {
        Uniform uniform = this.getUniform(pName);
        return (AbstractUniform)(uniform == null ? DUMMY_UNIFORM : uniform);
    }

    public void bindSampler(String pName, @Nullable GpuTexture pTexture) {
        this.samplerTextures.put(pName, pTexture);
    }

    public void setDefaultUniforms(VertexFormat.Mode pMode, Matrix4f pModelViewMatrix, Matrix4f pProjectionMatrix, float pScreenWidth, float pScreenHeight) {
        for (int i = 0; i < 12; i++) {
            GpuTexture gputexture = RenderSystem.getShaderTexture(i);
            this.bindSampler("Sampler" + i, gputexture);
        }

        if (this.MODEL_VIEW_MATRIX != null) {
            this.MODEL_VIEW_MATRIX.set(pModelViewMatrix);
        }

        if (this.PROJECTION_MATRIX != null) {
            this.PROJECTION_MATRIX.set(pProjectionMatrix);
        }

        if (this.COLOR_MODULATOR != null) {
            this.COLOR_MODULATOR.set(RenderSystem.getShaderColor());
        }

        if (this.GLINT_ALPHA != null) {
            this.GLINT_ALPHA.set(RenderSystem.getShaderGlintAlpha());
        }

        FogParameters fogparameters = RenderSystem.getShaderFog();
        if (this.FOG_START != null) {
            this.FOG_START.set(fogparameters.start());
        }

        if (this.FOG_END != null) {
            this.FOG_END.set(fogparameters.end());
        }

        if (this.FOG_COLOR != null) {
            this.FOG_COLOR.set(fogparameters.red(), fogparameters.green(), fogparameters.blue(), fogparameters.alpha());
        }

        if (this.FOG_SHAPE != null) {
            this.FOG_SHAPE.set(fogparameters.shape().getIndex());
        }

        if (this.TEXTURE_MATRIX != null) {
            this.TEXTURE_MATRIX.set(RenderSystem.getTextureMatrix());
        }

        if (this.GAME_TIME != null) {
            this.GAME_TIME.set(RenderSystem.getShaderGameTime());
        }

        if (this.MODEL_OFFSET != null) {
            this.MODEL_OFFSET.set(RenderSystem.getModelOffset());
        }

        if (this.SCREEN_SIZE != null) {
            this.SCREEN_SIZE.set(pScreenWidth, pScreenHeight);
        }

        if (this.LINE_WIDTH != null && (pMode == VertexFormat.Mode.LINES || pMode == VertexFormat.Mode.LINE_STRIP)) {
            this.LINE_WIDTH.set(RenderSystem.getShaderLineWidth());
        }

        Vector3f[] avector3f = RenderSystem.getShaderLights();
        if (this.LIGHT0_DIRECTION != null) {
            this.LIGHT0_DIRECTION.set(avector3f[0]);
        }

        if (this.LIGHT1_DIRECTION != null) {
            this.LIGHT1_DIRECTION.set(avector3f[1]);
        }
    }

    @VisibleForTesting
    public int getProgramId() {
        return this.programId;
    }

    @Override
    public String toString() {
        return this.debugLabel;
    }

    public String getDebugLabel() {
        return this.debugLabel;
    }

    public IntList getSamplerLocations() {
        return this.samplerLocations;
    }

    public List<String> getSamplers() {
        return this.samplers;
    }

    public List<Uniform> getUniforms() {
        return this.uniforms;
    }

    @Nullable
    private static UniformType getTypeFromGl(int pType) {
        return switch (pType) {
            case 5124 -> UniformType.INT;
            case 5126 -> UniformType.FLOAT;
            case 35664 -> UniformType.VEC2;
            case 35665 -> UniformType.VEC3;
            case 35666 -> UniformType.VEC4;
            case 35668 -> UniformType.IVEC3;
            case 35676 -> UniformType.MATRIX4X4;
            default -> null;
        };
    }
}