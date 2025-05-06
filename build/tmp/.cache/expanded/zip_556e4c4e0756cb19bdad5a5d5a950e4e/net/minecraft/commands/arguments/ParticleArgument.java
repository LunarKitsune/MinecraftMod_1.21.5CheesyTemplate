package net.minecraft.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

public class ParticleArgument implements ArgumentType<ParticleOptions> {
    private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo:bar", "particle{foo:bar}");
    public static final DynamicCommandExceptionType ERROR_UNKNOWN_PARTICLE = new DynamicCommandExceptionType(
        p_308358_ -> Component.translatableEscape("particle.notFound", p_308358_)
    );
    public static final DynamicCommandExceptionType ERROR_INVALID_OPTIONS = new DynamicCommandExceptionType(
        p_325596_ -> Component.translatableEscape("particle.invalidOptions", p_325596_)
    );
    private final HolderLookup.Provider registries;
    private static final TagParser<?> VALUE_PARSER = TagParser.create(NbtOps.INSTANCE);

    public ParticleArgument(CommandBuildContext pBuildContext) {
        this.registries = pBuildContext;
    }

    public static ParticleArgument particle(CommandBuildContext pBuildContext) {
        return new ParticleArgument(pBuildContext);
    }

    public static ParticleOptions getParticle(CommandContext<CommandSourceStack> pContext, String pName) {
        return pContext.getArgument(pName, ParticleOptions.class);
    }

    public ParticleOptions parse(StringReader pReader) throws CommandSyntaxException {
        return readParticle(pReader, this.registries);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    public static ParticleOptions readParticle(StringReader pReader, HolderLookup.Provider pRegistries) throws CommandSyntaxException {
        ParticleType<?> particletype = readParticleType(pReader, pRegistries.lookupOrThrow(Registries.PARTICLE_TYPE));
        return readParticle(VALUE_PARSER, pReader, (ParticleType<ParticleOptions>)particletype, pRegistries);
    }

    private static ParticleType<?> readParticleType(StringReader pReader, HolderLookup<ParticleType<?>> pParticleTypeLookup) throws CommandSyntaxException {
        ResourceLocation resourcelocation = ResourceLocation.read(pReader);
        ResourceKey<ParticleType<?>> resourcekey = ResourceKey.create(Registries.PARTICLE_TYPE, resourcelocation);
        return pParticleTypeLookup.get(resourcekey).orElseThrow(() -> ERROR_UNKNOWN_PARTICLE.createWithContext(pReader, resourcelocation)).value();
    }

    private static <T extends ParticleOptions, O> T readParticle(
        TagParser<O> pParser, StringReader pReader, ParticleType<T> pParticleType, HolderLookup.Provider pRegistries
    ) throws CommandSyntaxException {
        RegistryOps<O> registryops = pRegistries.createSerializationContext(pParser.getOps());
        O o;
        if (pReader.canRead() && pReader.peek() == '{') {
            o = pParser.parseAsArgument(pReader);
        } else {
            o = registryops.emptyMap();
        }

        return pParticleType.codec().codec().parse(registryops, o).getOrThrow(ERROR_INVALID_OPTIONS::create);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> pContext, SuggestionsBuilder pBuilder) {
        HolderLookup.RegistryLookup<ParticleType<?>> registrylookup = this.registries.lookupOrThrow(Registries.PARTICLE_TYPE);
        return SharedSuggestionProvider.suggestResource(registrylookup.listElementIds().map(ResourceKey::location), pBuilder);
    }
}