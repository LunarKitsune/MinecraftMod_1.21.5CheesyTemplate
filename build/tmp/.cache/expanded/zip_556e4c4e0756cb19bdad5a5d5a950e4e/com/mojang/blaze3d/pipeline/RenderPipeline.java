package com.mojang.blaze3d.pipeline;

import com.mojang.blaze3d.DontObfuscate;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.LogicOp;
import com.mojang.blaze3d.platform.PolygonMode;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Map.Entry;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@DontObfuscate
public class RenderPipeline {
    private final ResourceLocation location;
    private final ResourceLocation vertexShader;
    private final ResourceLocation fragmentShader;
    private final ShaderDefines shaderDefines;
    private final List<String> samplers;
    private final List<RenderPipeline.UniformDescription> uniforms;
    private final DepthTestFunction depthTestFunction;
    private final PolygonMode polygonMode;
    private final boolean cull;
    private final LogicOp colorLogic;
    private final Optional<BlendFunction> blendFunction;
    private final boolean writeColor;
    private final boolean writeAlpha;
    private final boolean writeDepth;
    private final VertexFormat vertexFormat;
    private final VertexFormat.Mode vertexFormatMode;
    private final float depthBiasScaleFactor;
    private final float depthBiasConstant;

    protected RenderPipeline(
        ResourceLocation pLocation,
        ResourceLocation pVertexShader,
        ResourceLocation pFragmentShader,
        ShaderDefines pShaderDefines,
        List<String> pSamplers,
        List<RenderPipeline.UniformDescription> pUniforms,
        Optional<BlendFunction> pBlendFunction,
        DepthTestFunction pDepthTestFunction,
        PolygonMode pPolygonMode,
        boolean pCull,
        boolean pWriteColor,
        boolean pWriteAlpha,
        boolean pWriteDepth,
        LogicOp pColorLogic,
        VertexFormat pVertexFormat,
        VertexFormat.Mode pVertexFormatMode,
        float pDepthBiasScaleFactor,
        float pDepthBiasConstant
    ) {
        this.location = pLocation;
        this.vertexShader = pVertexShader;
        this.fragmentShader = pFragmentShader;
        this.shaderDefines = pShaderDefines;
        this.samplers = pSamplers;
        this.uniforms = pUniforms;
        this.depthTestFunction = pDepthTestFunction;
        this.polygonMode = pPolygonMode;
        this.cull = pCull;
        this.blendFunction = pBlendFunction;
        this.writeColor = pWriteColor;
        this.writeAlpha = pWriteAlpha;
        this.writeDepth = pWriteDepth;
        this.colorLogic = pColorLogic;
        this.vertexFormat = pVertexFormat;
        this.vertexFormatMode = pVertexFormatMode;
        this.depthBiasScaleFactor = pDepthBiasScaleFactor;
        this.depthBiasConstant = pDepthBiasConstant;
    }

    @Override
    public String toString() {
        return this.location.toString();
    }

    public DepthTestFunction getDepthTestFunction() {
        return this.depthTestFunction;
    }

    public PolygonMode getPolygonMode() {
        return this.polygonMode;
    }

    public boolean isCull() {
        return this.cull;
    }

    public LogicOp getColorLogic() {
        return this.colorLogic;
    }

    public Optional<BlendFunction> getBlendFunction() {
        return this.blendFunction;
    }

    public boolean isWriteColor() {
        return this.writeColor;
    }

    public boolean isWriteAlpha() {
        return this.writeAlpha;
    }

    public boolean isWriteDepth() {
        return this.writeDepth;
    }

    public float getDepthBiasScaleFactor() {
        return this.depthBiasScaleFactor;
    }

    public float getDepthBiasConstant() {
        return this.depthBiasConstant;
    }

    public ResourceLocation getLocation() {
        return this.location;
    }

    public VertexFormat getVertexFormat() {
        return this.vertexFormat;
    }

    public VertexFormat.Mode getVertexFormatMode() {
        return this.vertexFormatMode;
    }

    public ResourceLocation getVertexShader() {
        return this.vertexShader;
    }

    public ResourceLocation getFragmentShader() {
        return this.fragmentShader;
    }

    public ShaderDefines getShaderDefines() {
        return this.shaderDefines;
    }

    public List<String> getSamplers() {
        return this.samplers;
    }

    public List<RenderPipeline.UniformDescription> getUniforms() {
        return this.uniforms;
    }

    public boolean wantsDepthTexture() {
        return this.depthTestFunction != DepthTestFunction.NO_DEPTH_TEST
            || this.depthBiasConstant != 0.0F
            || this.depthBiasScaleFactor != 0.0F
            || this.writeDepth;
    }

    public static RenderPipeline.Builder builder(RenderPipeline.Snippet... pSnippets) {
        RenderPipeline.Builder renderpipeline$builder = new RenderPipeline.Builder();

        for (RenderPipeline.Snippet renderpipeline$snippet : pSnippets) {
            renderpipeline$builder.withSnippet(renderpipeline$snippet);
        }

        return renderpipeline$builder;
    }

    @OnlyIn(Dist.CLIENT)
    @DontObfuscate
    public static class Builder {
        private Optional<ResourceLocation> location = Optional.empty();
        private Optional<ResourceLocation> fragmentShader = Optional.empty();
        private Optional<ResourceLocation> vertexShader = Optional.empty();
        private Optional<ShaderDefines.Builder> definesBuilder = Optional.empty();
        private Optional<List<String>> samplers = Optional.empty();
        private Optional<List<RenderPipeline.UniformDescription>> uniforms = Optional.empty();
        private Optional<DepthTestFunction> depthTestFunction = Optional.empty();
        private Optional<PolygonMode> polygonMode = Optional.empty();
        private Optional<Boolean> cull = Optional.empty();
        private Optional<Boolean> writeColor = Optional.empty();
        private Optional<Boolean> writeAlpha = Optional.empty();
        private Optional<Boolean> writeDepth = Optional.empty();
        private Optional<LogicOp> colorLogic = Optional.empty();
        private Optional<BlendFunction> blendFunction = Optional.empty();
        private Optional<VertexFormat> vertexFormat = Optional.empty();
        private Optional<VertexFormat.Mode> vertexFormatMode = Optional.empty();
        private float depthBiasScaleFactor;
        private float depthBiasConstant;

        Builder() {
        }

        public RenderPipeline.Builder withLocation(String pLocation) {
            this.location = Optional.of(ResourceLocation.withDefaultNamespace(pLocation));
            return this;
        }

        public RenderPipeline.Builder withLocation(ResourceLocation pLocation) {
            this.location = Optional.of(pLocation);
            return this;
        }

        public RenderPipeline.Builder withFragmentShader(String pFragmentShader) {
            this.fragmentShader = Optional.of(ResourceLocation.withDefaultNamespace(pFragmentShader));
            return this;
        }

        public RenderPipeline.Builder withFragmentShader(ResourceLocation pFragmentShader) {
            this.fragmentShader = Optional.of(pFragmentShader);
            return this;
        }

        public RenderPipeline.Builder withVertexShader(String pVertexShader) {
            this.vertexShader = Optional.of(ResourceLocation.withDefaultNamespace(pVertexShader));
            return this;
        }

        public RenderPipeline.Builder withVertexShader(ResourceLocation pVertexShader) {
            this.vertexShader = Optional.of(pVertexShader);
            return this;
        }

        public RenderPipeline.Builder withShaderDefine(String pFlag) {
            if (this.definesBuilder.isEmpty()) {
                this.definesBuilder = Optional.of(ShaderDefines.builder());
            }

            this.definesBuilder.get().define(pFlag);
            return this;
        }

        public RenderPipeline.Builder withShaderDefine(String pKey, int pValue) {
            if (this.definesBuilder.isEmpty()) {
                this.definesBuilder = Optional.of(ShaderDefines.builder());
            }

            this.definesBuilder.get().define(pKey, pValue);
            return this;
        }

        public RenderPipeline.Builder withShaderDefine(String pKey, float pValue) {
            if (this.definesBuilder.isEmpty()) {
                this.definesBuilder = Optional.of(ShaderDefines.builder());
            }

            this.definesBuilder.get().define(pKey, pValue);
            return this;
        }

        public RenderPipeline.Builder withSampler(String pSampler) {
            if (this.samplers.isEmpty()) {
                this.samplers = Optional.of(new ArrayList<>());
            }

            this.samplers.get().add(pSampler);
            return this;
        }

        public RenderPipeline.Builder withUniform(String pUniform, UniformType pType) {
            if (this.uniforms.isEmpty()) {
                this.uniforms = Optional.of(new ArrayList<>());
            }

            this.uniforms.get().add(new RenderPipeline.UniformDescription(pUniform, pType));
            return this;
        }

        public RenderPipeline.Builder withDepthTestFunction(DepthTestFunction pDepthTestFunction) {
            this.depthTestFunction = Optional.of(pDepthTestFunction);
            return this;
        }

        public RenderPipeline.Builder withPolygonMode(PolygonMode pPolygonMode) {
            this.polygonMode = Optional.of(pPolygonMode);
            return this;
        }

        public RenderPipeline.Builder withCull(boolean pCull) {
            this.cull = Optional.of(pCull);
            return this;
        }

        public RenderPipeline.Builder withBlend(BlendFunction pBlendFunction) {
            this.blendFunction = Optional.of(pBlendFunction);
            return this;
        }

        public RenderPipeline.Builder withoutBlend() {
            this.blendFunction = Optional.empty();
            return this;
        }

        public RenderPipeline.Builder withColorWrite(boolean pWriteColor) {
            this.writeColor = Optional.of(pWriteColor);
            this.writeAlpha = Optional.of(pWriteColor);
            return this;
        }

        public RenderPipeline.Builder withColorWrite(boolean pWriteColor, boolean pWriteAlpha) {
            this.writeColor = Optional.of(pWriteColor);
            this.writeAlpha = Optional.of(pWriteAlpha);
            return this;
        }

        public RenderPipeline.Builder withDepthWrite(boolean pWriteDepth) {
            this.writeDepth = Optional.of(pWriteDepth);
            return this;
        }

        public RenderPipeline.Builder withColorLogic(LogicOp pColorLogic) {
            this.colorLogic = Optional.of(pColorLogic);
            return this;
        }

        public RenderPipeline.Builder withVertexFormat(VertexFormat pVertexFormat, VertexFormat.Mode pVertexFormatMode) {
            this.vertexFormat = Optional.of(pVertexFormat);
            this.vertexFormatMode = Optional.of(pVertexFormatMode);
            return this;
        }

        public RenderPipeline.Builder withDepthBias(float pScaleFactor, float pConstant) {
            this.depthBiasScaleFactor = pScaleFactor;
            this.depthBiasConstant = pConstant;
            return this;
        }

        void withSnippet(RenderPipeline.Snippet pSnippet) {
            if (pSnippet.vertexShader.isPresent()) {
                this.vertexShader = pSnippet.vertexShader;
            }

            if (pSnippet.fragmentShader.isPresent()) {
                this.fragmentShader = pSnippet.fragmentShader;
            }

            if (pSnippet.shaderDefines.isPresent()) {
                if (this.definesBuilder.isEmpty()) {
                    this.definesBuilder = Optional.of(ShaderDefines.builder());
                }

                ShaderDefines shaderdefines = pSnippet.shaderDefines.get();

                for (Entry<String, String> entry : shaderdefines.values().entrySet()) {
                    this.definesBuilder.get().define(entry.getKey(), entry.getValue());
                }

                for (String s : shaderdefines.flags()) {
                    this.definesBuilder.get().define(s);
                }
            }

            pSnippet.samplers.ifPresent(p_396787_ -> {
                if (this.samplers.isPresent()) {
                    this.samplers.get().addAll(p_396787_);
                } else {
                    this.samplers = Optional.of(new ArrayList<>(p_396787_));
                }
            });
            pSnippet.uniforms.ifPresent(p_393176_ -> {
                if (this.uniforms.isPresent()) {
                    this.uniforms.get().addAll(p_393176_);
                } else {
                    this.uniforms = Optional.of(new ArrayList<>(p_393176_));
                }
            });
            if (pSnippet.depthTestFunction.isPresent()) {
                this.depthTestFunction = pSnippet.depthTestFunction;
            }

            if (pSnippet.cull.isPresent()) {
                this.cull = pSnippet.cull;
            }

            if (pSnippet.writeColor.isPresent()) {
                this.writeColor = pSnippet.writeColor;
            }

            if (pSnippet.writeAlpha.isPresent()) {
                this.writeAlpha = pSnippet.writeAlpha;
            }

            if (pSnippet.writeDepth.isPresent()) {
                this.writeDepth = pSnippet.writeDepth;
            }

            if (pSnippet.colorLogic.isPresent()) {
                this.colorLogic = pSnippet.colorLogic;
            }

            if (pSnippet.blendFunction.isPresent()) {
                this.blendFunction = pSnippet.blendFunction;
            }

            if (pSnippet.vertexFormat.isPresent()) {
                this.vertexFormat = pSnippet.vertexFormat;
            }

            if (pSnippet.vertexFormatMode.isPresent()) {
                this.vertexFormatMode = pSnippet.vertexFormatMode;
            }
        }

        public RenderPipeline.Snippet buildSnippet() {
            return new RenderPipeline.Snippet(
                this.vertexShader,
                this.fragmentShader,
                this.definesBuilder.map(ShaderDefines.Builder::build),
                this.samplers.map(Collections::unmodifiableList),
                this.uniforms.map(Collections::unmodifiableList),
                this.blendFunction,
                this.depthTestFunction,
                this.polygonMode,
                this.cull,
                this.writeColor,
                this.writeAlpha,
                this.writeDepth,
                this.colorLogic,
                this.vertexFormat,
                this.vertexFormatMode
            );
        }

        public RenderPipeline build() {
            if (this.location.isEmpty()) {
                throw new IllegalStateException("Missing location");
            } else if (this.vertexShader.isEmpty()) {
                throw new IllegalStateException("Missing vertex shader");
            } else if (this.fragmentShader.isEmpty()) {
                throw new IllegalStateException("Missing fragment shader");
            } else if (this.vertexFormat.isEmpty()) {
                throw new IllegalStateException("Missing vertex buffer format");
            } else if (this.vertexFormatMode.isEmpty()) {
                throw new IllegalStateException("Missing vertex mode");
            } else {
                return new RenderPipeline(
                    this.location.get(),
                    this.vertexShader.get(),
                    this.fragmentShader.get(),
                    this.definesBuilder.orElse(ShaderDefines.builder()).build(),
                    List.copyOf(this.samplers.orElse(new ArrayList<>())),
                    this.uniforms.orElse(Collections.emptyList()),
                    this.blendFunction,
                    this.depthTestFunction.orElse(DepthTestFunction.LEQUAL_DEPTH_TEST),
                    this.polygonMode.orElse(PolygonMode.FILL),
                    this.cull.orElse(true),
                    this.writeColor.orElse(true),
                    this.writeAlpha.orElse(true),
                    this.writeDepth.orElse(true),
                    this.colorLogic.orElse(LogicOp.NONE),
                    this.vertexFormat.get(),
                    this.vertexFormatMode.get(),
                    this.depthBiasScaleFactor,
                    this.depthBiasConstant
                );
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    @DontObfuscate
    public record Snippet(
        Optional<ResourceLocation> vertexShader,
        Optional<ResourceLocation> fragmentShader,
        Optional<ShaderDefines> shaderDefines,
        Optional<List<String>> samplers,
        Optional<List<RenderPipeline.UniformDescription>> uniforms,
        Optional<BlendFunction> blendFunction,
        Optional<DepthTestFunction> depthTestFunction,
        Optional<PolygonMode> polygonMode,
        Optional<Boolean> cull,
        Optional<Boolean> writeColor,
        Optional<Boolean> writeAlpha,
        Optional<Boolean> writeDepth,
        Optional<LogicOp> colorLogic,
        Optional<VertexFormat> vertexFormat,
        Optional<VertexFormat.Mode> vertexFormatMode
    ) {
    }

    @OnlyIn(Dist.CLIENT)
    @DontObfuscate
    public record UniformDescription(String name, UniformType type) {
    }
}