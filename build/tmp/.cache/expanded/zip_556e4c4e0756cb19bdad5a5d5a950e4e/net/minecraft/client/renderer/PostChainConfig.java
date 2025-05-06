package net.minecraft.client.renderer;

import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record PostChainConfig(Map<ResourceLocation, PostChainConfig.InternalTarget> internalTargets, List<PostChainConfig.Pass> passes) {
    public static final Codec<PostChainConfig> CODEC = RecordCodecBuilder.create(
        p_362023_ -> p_362023_.group(
                Codec.unboundedMap(ResourceLocation.CODEC, PostChainConfig.InternalTarget.CODEC)
                    .optionalFieldOf("targets", Map.of())
                    .forGetter(PostChainConfig::internalTargets),
                PostChainConfig.Pass.CODEC.listOf().optionalFieldOf("passes", List.of()).forGetter(PostChainConfig::passes)
            )
            .apply(p_362023_, PostChainConfig::new)
    );

    @OnlyIn(Dist.CLIENT)
    public record FixedSizedTarget(int width, int height) implements PostChainConfig.InternalTarget {
        public static final Codec<PostChainConfig.FixedSizedTarget> CODEC = RecordCodecBuilder.create(
            p_363084_ -> p_363084_.group(
                    ExtraCodecs.POSITIVE_INT.fieldOf("width").forGetter(PostChainConfig.FixedSizedTarget::width),
                    ExtraCodecs.POSITIVE_INT.fieldOf("height").forGetter(PostChainConfig.FixedSizedTarget::height)
                )
                .apply(p_363084_, PostChainConfig.FixedSizedTarget::new)
        );
    }

    @OnlyIn(Dist.CLIENT)
    public record FullScreenTarget() implements PostChainConfig.InternalTarget {
        public static final Codec<PostChainConfig.FullScreenTarget> CODEC = Codec.unit(PostChainConfig.FullScreenTarget::new);
    }

    @OnlyIn(Dist.CLIENT)
    public sealed interface Input permits PostChainConfig.TextureInput, PostChainConfig.TargetInput {
        Codec<PostChainConfig.Input> CODEC = Codec.xor(PostChainConfig.TextureInput.CODEC, PostChainConfig.TargetInput.CODEC)
            .xmap(p_368743_ -> p_368743_.map(Function.identity(), Function.identity()), p_368212_ -> {
                return switch (p_368212_) {
                    case PostChainConfig.TextureInput postchainconfig$textureinput -> Either.left(postchainconfig$textureinput);
                    case PostChainConfig.TargetInput postchainconfig$targetinput -> Either.right(postchainconfig$targetinput);
                    default -> throw new MatchException(null, null);
                };
            });

        String samplerName();

        Set<ResourceLocation> referencedTargets();
    }

    @OnlyIn(Dist.CLIENT)
    public sealed interface InternalTarget permits PostChainConfig.FullScreenTarget, PostChainConfig.FixedSizedTarget {
        Codec<PostChainConfig.InternalTarget> CODEC = Codec.either(PostChainConfig.FixedSizedTarget.CODEC, PostChainConfig.FullScreenTarget.CODEC)
            .xmap(p_364357_ -> p_364357_.map(Function.identity(), Function.identity()), p_362739_ -> {
                return switch (p_362739_) {
                    case PostChainConfig.FixedSizedTarget postchainconfig$fixedsizedtarget -> Either.left(postchainconfig$fixedsizedtarget);
                    case PostChainConfig.FullScreenTarget postchainconfig$fullscreentarget -> Either.right(postchainconfig$fullscreentarget);
                    default -> throw new MatchException(null, null);
                };
            });
    }

    @OnlyIn(Dist.CLIENT)
    public record Pass(
        ResourceLocation vertexShaderId,
        ResourceLocation fragmentShaderId,
        List<PostChainConfig.Input> inputs,
        ResourceLocation outputTarget,
        List<PostChainConfig.Uniform> uniforms
    ) {
        private static final Codec<List<PostChainConfig.Input>> INPUTS_CODEC = PostChainConfig.Input.CODEC.listOf().validate(p_363610_ -> {
            Set<String> set = new ObjectArraySet<>(p_363610_.size());

            for (PostChainConfig.Input postchainconfig$input : p_363610_) {
                if (!set.add(postchainconfig$input.samplerName())) {
                    return DataResult.error(() -> "Encountered repeated sampler name: " + postchainconfig$input.samplerName());
                }
            }

            return DataResult.success(p_363610_);
        });
        public static final Codec<PostChainConfig.Pass> CODEC = RecordCodecBuilder.create(
            p_389402_ -> p_389402_.group(
                    ResourceLocation.CODEC.fieldOf("vertex_shader").forGetter(PostChainConfig.Pass::vertexShaderId),
                    ResourceLocation.CODEC.fieldOf("fragment_shader").forGetter(PostChainConfig.Pass::fragmentShaderId),
                    INPUTS_CODEC.optionalFieldOf("inputs", List.of()).forGetter(PostChainConfig.Pass::inputs),
                    ResourceLocation.CODEC.fieldOf("output").forGetter(PostChainConfig.Pass::outputTarget),
                    PostChainConfig.Uniform.CODEC.listOf().optionalFieldOf("uniforms", List.of()).forGetter(PostChainConfig.Pass::uniforms)
                )
                .apply(p_389402_, PostChainConfig.Pass::new)
        );

        public Stream<ResourceLocation> referencedTargets() {
            Stream<ResourceLocation> stream = this.inputs.stream().flatMap(p_389401_ -> p_389401_.referencedTargets().stream());
            return Stream.concat(stream, Stream.of(this.outputTarget));
        }
    }

    @OnlyIn(Dist.CLIENT)
    public record TargetInput(String samplerName, ResourceLocation targetId, boolean useDepthBuffer, boolean bilinear) implements PostChainConfig.Input {
        public static final Codec<PostChainConfig.TargetInput> CODEC = RecordCodecBuilder.create(
            p_361775_ -> p_361775_.group(
                    Codec.STRING.fieldOf("sampler_name").forGetter(PostChainConfig.TargetInput::samplerName),
                    ResourceLocation.CODEC.fieldOf("target").forGetter(PostChainConfig.TargetInput::targetId),
                    Codec.BOOL.optionalFieldOf("use_depth_buffer", false).forGetter(PostChainConfig.TargetInput::useDepthBuffer),
                    Codec.BOOL.optionalFieldOf("bilinear", false).forGetter(PostChainConfig.TargetInput::bilinear)
                )
                .apply(p_361775_, PostChainConfig.TargetInput::new)
        );

        @Override
        public Set<ResourceLocation> referencedTargets() {
            return Set.of(this.targetId);
        }

        @Override
        public String samplerName() {
            return this.samplerName;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public record TextureInput(String samplerName, ResourceLocation location, int width, int height, boolean bilinear) implements PostChainConfig.Input {
        public static final Codec<PostChainConfig.TextureInput> CODEC = RecordCodecBuilder.create(
            p_368377_ -> p_368377_.group(
                    Codec.STRING.fieldOf("sampler_name").forGetter(PostChainConfig.TextureInput::samplerName),
                    ResourceLocation.CODEC.fieldOf("location").forGetter(PostChainConfig.TextureInput::location),
                    ExtraCodecs.POSITIVE_INT.fieldOf("width").forGetter(PostChainConfig.TextureInput::width),
                    ExtraCodecs.POSITIVE_INT.fieldOf("height").forGetter(PostChainConfig.TextureInput::height),
                    Codec.BOOL.optionalFieldOf("bilinear", false).forGetter(PostChainConfig.TextureInput::bilinear)
                )
                .apply(p_368377_, PostChainConfig.TextureInput::new)
        );

        @Override
        public Set<ResourceLocation> referencedTargets() {
            return Set.of();
        }

        @Override
        public String samplerName() {
            return this.samplerName;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public record Uniform(String name, String type, Optional<List<Float>> values) {
        public static final Codec<PostChainConfig.Uniform> CODEC = RecordCodecBuilder.create(
            p_389403_ -> p_389403_.group(
                    Codec.STRING.fieldOf("name").forGetter(PostChainConfig.Uniform::name),
                    Codec.STRING.fieldOf("type").forGetter(PostChainConfig.Uniform::type),
                    Codec.FLOAT.sizeLimitedListOf(4).optionalFieldOf("values").forGetter(PostChainConfig.Uniform::values)
                )
                .apply(p_389403_, PostChainConfig.Uniform::new)
        );

        public void setOnRenderPass(RenderPass pRenderPass) {
            UniformType uniformtype = UniformType.CODEC.byName(this.type);
            if (!this.values.isEmpty() && uniformtype != null && !this.values.get().isEmpty()) {
                List<Float> list = this.values.get();
                if (uniformtype.isIntStorage()) {
                    pRenderPass.setUniform(this.name, (int)list.getFirst().floatValue());
                } else {
                    float[] afloat = new float[uniformtype.getCount()];
                    if (list.size() == 1) {
                        Arrays.fill(afloat, list.getFirst().floatValue());
                    } else {
                        for (int i = 0; i < Math.min(list.size(), uniformtype.getCount()); i++) {
                            afloat[i] = list.get(i);
                        }
                    }

                    pRenderPass.setUniform(this.name, afloat);
                }
            }
        }
    }
}