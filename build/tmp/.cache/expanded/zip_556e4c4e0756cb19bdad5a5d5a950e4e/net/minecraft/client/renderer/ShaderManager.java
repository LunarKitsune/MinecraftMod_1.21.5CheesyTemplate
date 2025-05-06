package net.minecraft.client.renderer;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.pipeline.CompiledRenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.FileUtil;
import net.minecraft.ResourceLocationException;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class ShaderManager extends SimplePreparableReloadListener<ShaderManager.Configs> implements AutoCloseable {
    static final Logger LOGGER = LogUtils.getLogger();
    public static final int MAX_LOG_LENGTH = 32768;
    public static final String SHADER_PATH = "shaders";
    private static final String SHADER_INCLUDE_PATH = "shaders/include/";
    private static final FileToIdConverter POST_CHAIN_ID_CONVERTER = FileToIdConverter.json("post_effect");
    final TextureManager textureManager;
    private final Consumer<Exception> recoveryHandler;
    private ShaderManager.CompilationCache compilationCache = new ShaderManager.CompilationCache(ShaderManager.Configs.EMPTY);

    public ShaderManager(TextureManager pTextureManager, Consumer<Exception> pRecoveryHandler) {
        this.textureManager = pTextureManager;
        this.recoveryHandler = pRecoveryHandler;
    }

    protected ShaderManager.Configs prepare(ResourceManager p_363890_, ProfilerFiller p_362646_) {
        Builder<ShaderManager.ShaderSourceKey, String> builder = ImmutableMap.builder();
        Map<ResourceLocation, Resource> map = p_363890_.listResources("shaders", ShaderManager::isShader);

        for (Entry<ResourceLocation, Resource> entry : map.entrySet()) {
            ResourceLocation resourcelocation = entry.getKey();
            ShaderType shadertype = ShaderType.byLocation(resourcelocation);
            if (shadertype != null) {
                loadShader(resourcelocation, entry.getValue(), shadertype, map, builder);
            }
        }

        Builder<ResourceLocation, PostChainConfig> builder1 = ImmutableMap.builder();

        for (Entry<ResourceLocation, Resource> entry1 : POST_CHAIN_ID_CONVERTER.listMatchingResources(p_363890_).entrySet()) {
            loadPostChain(entry1.getKey(), entry1.getValue(), builder1);
        }

        return new ShaderManager.Configs(builder.build(), builder1.build());
    }

    private static void loadShader(
        ResourceLocation pLocation,
        Resource pShader,
        ShaderType pType,
        Map<ResourceLocation, Resource> pShaderResources,
        Builder<ShaderManager.ShaderSourceKey, String> pOutput
    ) {
        ResourceLocation resourcelocation = pType.idConverter().fileToId(pLocation);
        GlslPreprocessor glslpreprocessor = createPreprocessor(pShaderResources, pLocation);

        try (Reader reader = pShader.openAsReader()) {
            String s = IOUtils.toString(reader);
            pOutput.put(new ShaderManager.ShaderSourceKey(resourcelocation, pType), String.join("", glslpreprocessor.process(s)));
        } catch (IOException ioexception) {
            LOGGER.error("Failed to load shader source at {}", pLocation, ioexception);
        }
    }

    private static GlslPreprocessor createPreprocessor(final Map<ResourceLocation, Resource> pShaderResources, ResourceLocation pShaderLocation) {
        final ResourceLocation resourcelocation = pShaderLocation.withPath(FileUtil::getFullResourcePath);
        return new GlslPreprocessor() {
            private final Set<ResourceLocation> importedLocations = new ObjectArraySet<>();

            @Override
            public String applyImport(boolean p_365562_, String p_361440_) {
                ResourceLocation resourcelocation1;
                try {
                    if (p_365562_) {
                        resourcelocation1 = resourcelocation.withPath(p_366909_ -> FileUtil.normalizeResourcePath(p_366909_ + p_361440_));
                    } else {
                        resourcelocation1 = ResourceLocation.parse(p_361440_).withPrefix("shaders/include/");
                    }
                } catch (ResourceLocationException resourcelocationexception) {
                    ShaderManager.LOGGER.error("Malformed GLSL import {}: {}", p_361440_, resourcelocationexception.getMessage());
                    return "#error " + resourcelocationexception.getMessage();
                }

                if (!this.importedLocations.add(resourcelocation1)) {
                    return null;
                } else {
                    try {
                        String s;
                        try (Reader reader = pShaderResources.get(resourcelocation1).openAsReader()) {
                            s = IOUtils.toString(reader);
                        }

                        return s;
                    } catch (IOException ioexception) {
                        ShaderManager.LOGGER.error("Could not open GLSL import {}: {}", resourcelocation1, ioexception.getMessage());
                        return "#error " + ioexception.getMessage();
                    }
                }
            }
        };
    }

    private static void loadPostChain(ResourceLocation pLocation, Resource pPostChain, Builder<ResourceLocation, PostChainConfig> pOutput) {
        ResourceLocation resourcelocation = POST_CHAIN_ID_CONVERTER.fileToId(pLocation);

        try (Reader reader = pPostChain.openAsReader()) {
            JsonElement jsonelement = JsonParser.parseReader(reader);
            pOutput.put(resourcelocation, PostChainConfig.CODEC.parse(JsonOps.INSTANCE, jsonelement).getOrThrow(JsonSyntaxException::new));
        } catch (JsonParseException | IOException ioexception) {
            LOGGER.error("Failed to parse post chain at {}", pLocation, ioexception);
        }
    }

    private static boolean isShader(ResourceLocation pLocation) {
        return ShaderType.byLocation(pLocation) != null || pLocation.getPath().endsWith(".glsl");
    }

    protected void apply(ShaderManager.Configs p_360858_, ResourceManager p_369986_, ProfilerFiller p_364135_) {
        ShaderManager.CompilationCache shadermanager$compilationcache = new ShaderManager.CompilationCache(p_360858_);
        Set<RenderPipeline> set = new HashSet<>(RenderPipelines.getStaticPipelines());
        List<ResourceLocation> list = new ArrayList<>();
        GpuDevice gpudevice = RenderSystem.getDevice();
        gpudevice.clearPipelineCache();

        for (RenderPipeline renderpipeline : set) {
            CompiledRenderPipeline compiledrenderpipeline = gpudevice.precompilePipeline(renderpipeline, shadermanager$compilationcache::getShaderSource);
            if (!compiledrenderpipeline.isValid()) {
                list.add(renderpipeline.getLocation());
            }
        }

        if (!list.isEmpty()) {
            gpudevice.clearPipelineCache();
            throw new RuntimeException(
                "Failed to load required shader programs:\n" + list.stream().map(p_389461_ -> " - " + p_389461_).collect(Collectors.joining("\n"))
            );
        } else {
            this.compilationCache.close();
            this.compilationCache = shadermanager$compilationcache;
        }
    }

    @Override
    public String getName() {
        return "Shader Loader";
    }

    private void tryTriggerRecovery(Exception pException) {
        if (!this.compilationCache.triggeredRecovery) {
            this.recoveryHandler.accept(pException);
            this.compilationCache.triggeredRecovery = true;
        }
    }

    @Nullable
    public PostChain getPostChain(ResourceLocation pId, Set<ResourceLocation> pExternalTargets) {
        try {
            return this.compilationCache.getOrLoadPostChain(pId, pExternalTargets);
        } catch (ShaderManager.CompilationException shadermanager$compilationexception) {
            LOGGER.error("Failed to load post chain: {}", pId, shadermanager$compilationexception);
            this.compilationCache.postChains.put(pId, Optional.empty());
            this.tryTriggerRecovery(shadermanager$compilationexception);
            return null;
        }
    }

    @Override
    public void close() {
        this.compilationCache.close();
    }

    public String getShader(ResourceLocation pId, ShaderType pType) {
        return this.compilationCache.getShaderSource(pId, pType);
    }

    @OnlyIn(Dist.CLIENT)
    class CompilationCache implements AutoCloseable {
        private final ShaderManager.Configs configs;
        final Map<ResourceLocation, Optional<PostChain>> postChains = new HashMap<>();
        boolean triggeredRecovery;

        CompilationCache(final ShaderManager.Configs pConfigs) {
            this.configs = pConfigs;
        }

        @Nullable
        public PostChain getOrLoadPostChain(ResourceLocation pName, Set<ResourceLocation> pExternalTargets) throws ShaderManager.CompilationException {
            Optional<PostChain> optional = this.postChains.get(pName);
            if (optional != null) {
                return optional.orElse(null);
            } else {
                PostChain postchain = this.loadPostChain(pName, pExternalTargets);
                this.postChains.put(pName, Optional.of(postchain));
                return postchain;
            }
        }

        private PostChain loadPostChain(ResourceLocation pName, Set<ResourceLocation> pExternalTargets) throws ShaderManager.CompilationException {
            PostChainConfig postchainconfig = this.configs.postChains.get(pName);
            if (postchainconfig == null) {
                throw new ShaderManager.CompilationException("Could not find post chain with id: " + pName);
            } else {
                return PostChain.load(postchainconfig, ShaderManager.this.textureManager, pExternalTargets, pName);
            }
        }

        @Override
        public void close() {
            this.postChains.clear();
        }

        public String getShaderSource(ResourceLocation pId, ShaderType pType) {
            return this.configs.shaderSources.get(new ShaderManager.ShaderSourceKey(pId, pType));
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class CompilationException extends Exception {
        public CompilationException(String pMessage) {
            super(pMessage);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public record Configs(Map<ShaderManager.ShaderSourceKey, String> shaderSources, Map<ResourceLocation, PostChainConfig> postChains) {
        public static final ShaderManager.Configs EMPTY = new ShaderManager.Configs(Map.of(), Map.of());
    }

    @OnlyIn(Dist.CLIENT)
    record ShaderSourceKey(ResourceLocation id, ShaderType type) {
        @Override
        public String toString() {
            return this.id + " (" + this.type + ")";
        }
    }
}