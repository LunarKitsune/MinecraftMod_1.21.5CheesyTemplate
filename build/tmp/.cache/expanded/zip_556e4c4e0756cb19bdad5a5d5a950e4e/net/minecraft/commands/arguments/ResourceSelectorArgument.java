package net.minecraft.commands.arguments;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.io.FilenameUtils;

public class ResourceSelectorArgument<T> implements ArgumentType<Collection<Holder.Reference<T>>> {
    private static final Collection<String> EXAMPLES = List.of("minecraft:*", "*:asset", "*");
    public static final Dynamic2CommandExceptionType ERROR_NO_MATCHES = new Dynamic2CommandExceptionType(
        (p_396458_, p_395304_) -> Component.translatableEscape("argument.resource_selector.not_found", p_396458_, p_395304_)
    );
    final ResourceKey<? extends Registry<T>> registryKey;
    private final HolderLookup<T> registryLookup;

    ResourceSelectorArgument(CommandBuildContext pBuildContext, ResourceKey<? extends Registry<T>> pRegistryKey) {
        this.registryKey = pRegistryKey;
        this.registryLookup = pBuildContext.lookupOrThrow(pRegistryKey);
    }

    public Collection<Holder.Reference<T>> parse(StringReader p_393369_) throws CommandSyntaxException {
        String s = ensureNamespaced(readPattern(p_393369_));
        List<Holder.Reference<T>> list = this.registryLookup.listElements().filter(p_392826_ -> matches(s, p_392826_.key().location())).toList();
        if (list.isEmpty()) {
            throw ERROR_NO_MATCHES.createWithContext(p_393369_, s, this.registryKey.location());
        } else {
            return list;
        }
    }

    public static <T> Collection<Holder.Reference<T>> parse(StringReader pParse, HolderLookup<T> pLookup) {
        String s = ensureNamespaced(readPattern(pParse));
        return pLookup.listElements().filter(p_397609_ -> matches(s, p_397609_.key().location())).toList();
    }

    private static String readPattern(StringReader pReader) {
        int i = pReader.getCursor();

        while (pReader.canRead() && isAllowedPatternCharacter(pReader.peek())) {
            pReader.skip();
        }

        return pReader.getString().substring(i, pReader.getCursor());
    }

    private static boolean isAllowedPatternCharacter(char pC) {
        return ResourceLocation.isAllowedInResourceLocation(pC) || pC == '*' || pC == '?';
    }

    private static String ensureNamespaced(String pName) {
        return !pName.contains(":") ? "minecraft:" + pName : pName;
    }

    private static boolean matches(String pString, ResourceLocation pLocation) {
        return FilenameUtils.wildcardMatch(pLocation.toString(), pString);
    }

    public static <T> ResourceSelectorArgument<T> resourceSelector(CommandBuildContext pBuildContext, ResourceKey<? extends Registry<T>> pRegistryKey) {
        return new ResourceSelectorArgument<>(pBuildContext, pRegistryKey);
    }

    public static <T> Collection<Holder.Reference<T>> getSelectedResources(
        CommandContext<CommandSourceStack> pContext, String pArgument, ResourceKey<? extends Registry<T>> pRegistryKey
    ) {
        return pContext.getArgument(pArgument, Collection.class);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> pContext, SuggestionsBuilder pBuilder) {
        return pContext.getSource() instanceof SharedSuggestionProvider sharedsuggestionprovider
            ? sharedsuggestionprovider.suggestRegistryElements(this.registryKey, SharedSuggestionProvider.ElementSuggestionType.ELEMENTS, pBuilder, pContext)
            : SharedSuggestionProvider.suggest(this.registryLookup.listElementIds().map(ResourceKey::location).map(ResourceLocation::toString), pBuilder);
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    public static class Info<T> implements ArgumentTypeInfo<ResourceSelectorArgument<T>, ResourceSelectorArgument.Info<T>.Template> {
        public void serializeToNetwork(ResourceSelectorArgument.Info<T>.Template p_395331_, FriendlyByteBuf p_392665_) {
            p_392665_.writeResourceKey(p_395331_.registryKey);
        }

        public ResourceSelectorArgument.Info<T>.Template deserializeFromNetwork(FriendlyByteBuf p_395716_) {
            return new ResourceSelectorArgument.Info.Template(p_395716_.readRegistryKey());
        }

        public void serializeToJson(ResourceSelectorArgument.Info<T>.Template p_397745_, JsonObject p_391870_) {
            p_391870_.addProperty("registry", p_397745_.registryKey.location().toString());
        }

        public ResourceSelectorArgument.Info<T>.Template unpack(ResourceSelectorArgument<T> p_391303_) {
            return new ResourceSelectorArgument.Info.Template(p_391303_.registryKey);
        }

        public final class Template implements ArgumentTypeInfo.Template<ResourceSelectorArgument<T>> {
            final ResourceKey<? extends Registry<T>> registryKey;

            Template(final ResourceKey<? extends Registry<T>> pRegistryKey) {
                this.registryKey = pRegistryKey;
            }

            public ResourceSelectorArgument<T> instantiate(CommandBuildContext p_397803_) {
                return new ResourceSelectorArgument<>(p_397803_, this.registryKey);
            }

            @Override
            public ArgumentTypeInfo<ResourceSelectorArgument<T>, ?> type() {
                return Info.this;
            }
        }
    }
}