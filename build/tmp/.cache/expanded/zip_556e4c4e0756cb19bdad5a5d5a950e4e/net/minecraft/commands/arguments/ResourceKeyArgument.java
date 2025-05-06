package net.minecraft.commands.arguments;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

public class ResourceKeyArgument<T> implements ArgumentType<ResourceKey<T>> {
    private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo:bar", "012");
    private static final DynamicCommandExceptionType ERROR_INVALID_FEATURE = new DynamicCommandExceptionType(
        p_308366_ -> Component.translatableEscape("commands.place.feature.invalid", p_308366_)
    );
    private static final DynamicCommandExceptionType ERROR_INVALID_STRUCTURE = new DynamicCommandExceptionType(
        p_308367_ -> Component.translatableEscape("commands.place.structure.invalid", p_308367_)
    );
    private static final DynamicCommandExceptionType ERROR_INVALID_TEMPLATE_POOL = new DynamicCommandExceptionType(
        p_308365_ -> Component.translatableEscape("commands.place.jigsaw.invalid", p_308365_)
    );
    private static final DynamicCommandExceptionType ERROR_INVALID_RECIPE = new DynamicCommandExceptionType(p_358067_ -> Component.translatableEscape("recipe.notFound", p_358067_));
    private static final DynamicCommandExceptionType ERROR_INVALID_ADVANCEMENT = new DynamicCommandExceptionType(
        p_358063_ -> Component.translatableEscape("advancement.advancementNotFound", p_358063_)
    );
    final ResourceKey<? extends Registry<T>> registryKey;

    public ResourceKeyArgument(ResourceKey<? extends Registry<T>> pRegistryKey) {
        this.registryKey = pRegistryKey;
    }

    public static <T> ResourceKeyArgument<T> key(ResourceKey<? extends Registry<T>> pRegistryKey) {
        return new ResourceKeyArgument<>(pRegistryKey);
    }

    public static <T> ResourceKey<T> getRegistryKey(
        CommandContext<CommandSourceStack> pContext, String pArgument, ResourceKey<Registry<T>> pRegistryKey, DynamicCommandExceptionType pException
    ) throws CommandSyntaxException {
        ResourceKey<?> resourcekey = pContext.getArgument(pArgument, ResourceKey.class);
        Optional<ResourceKey<T>> optional = resourcekey.cast(pRegistryKey);
        return optional.orElseThrow(() -> pException.create(resourcekey.location()));
    }

    private static <T> Registry<T> getRegistry(CommandContext<CommandSourceStack> pContext, ResourceKey<? extends Registry<T>> pRegistryKey) {
        return pContext.getSource().getServer().registryAccess().lookupOrThrow(pRegistryKey);
    }

    private static <T> Holder.Reference<T> resolveKey(
        CommandContext<CommandSourceStack> pContext, String pArgument, ResourceKey<Registry<T>> pRegistryKey, DynamicCommandExceptionType pException
    ) throws CommandSyntaxException {
        ResourceKey<T> resourcekey = getRegistryKey(pContext, pArgument, pRegistryKey, pException);
        return getRegistry(pContext, pRegistryKey).get(resourcekey).orElseThrow(() -> pException.create(resourcekey.location()));
    }

    public static Holder.Reference<ConfiguredFeature<?, ?>> getConfiguredFeature(CommandContext<CommandSourceStack> pContext, String pArgument) throws CommandSyntaxException {
        return resolveKey(pContext, pArgument, Registries.CONFIGURED_FEATURE, ERROR_INVALID_FEATURE);
    }

    public static Holder.Reference<Structure> getStructure(CommandContext<CommandSourceStack> pContext, String pArgument) throws CommandSyntaxException {
        return resolveKey(pContext, pArgument, Registries.STRUCTURE, ERROR_INVALID_STRUCTURE);
    }

    public static Holder.Reference<StructureTemplatePool> getStructureTemplatePool(CommandContext<CommandSourceStack> pContext, String pArgument) throws CommandSyntaxException {
        return resolveKey(pContext, pArgument, Registries.TEMPLATE_POOL, ERROR_INVALID_TEMPLATE_POOL);
    }

    public static RecipeHolder<?> getRecipe(CommandContext<CommandSourceStack> pContext, String pArgument) throws CommandSyntaxException {
        RecipeManager recipemanager = pContext.getSource().getServer().getRecipeManager();
        ResourceKey<Recipe<?>> resourcekey = getRegistryKey(pContext, pArgument, Registries.RECIPE, ERROR_INVALID_RECIPE);
        return recipemanager.byKey(resourcekey).orElseThrow(() -> ERROR_INVALID_RECIPE.create(resourcekey.location()));
    }

    public static AdvancementHolder getAdvancement(CommandContext<CommandSourceStack> pContext, String pArgument) throws CommandSyntaxException {
        ResourceKey<Advancement> resourcekey = getRegistryKey(pContext, pArgument, Registries.ADVANCEMENT, ERROR_INVALID_ADVANCEMENT);
        AdvancementHolder advancementholder = pContext.getSource().getServer().getAdvancements().get(resourcekey.location());
        if (advancementholder == null) {
            throw ERROR_INVALID_ADVANCEMENT.create(resourcekey.location());
        } else {
            return advancementholder;
        }
    }

    public ResourceKey<T> parse(StringReader pReader) throws CommandSyntaxException {
        ResourceLocation resourcelocation = ResourceLocation.read(pReader);
        return ResourceKey.create(this.registryKey, resourcelocation);
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> pContext, SuggestionsBuilder pBuilder) {
        return pContext.getSource() instanceof SharedSuggestionProvider sharedsuggestionprovider
            ? sharedsuggestionprovider.suggestRegistryElements(this.registryKey, SharedSuggestionProvider.ElementSuggestionType.ELEMENTS, pBuilder, pContext)
            : pBuilder.buildFuture();
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    public static class Info<T> implements ArgumentTypeInfo<ResourceKeyArgument<T>, ResourceKeyArgument.Info<T>.Template> {
        public void serializeToNetwork(ResourceKeyArgument.Info<T>.Template p_233278_, FriendlyByteBuf p_233279_) {
            p_233279_.writeResourceKey(p_233278_.registryKey);
        }

        public ResourceKeyArgument.Info<T>.Template deserializeFromNetwork(FriendlyByteBuf p_233289_) {
            return new ResourceKeyArgument.Info.Template(p_233289_.readRegistryKey());
        }

        public void serializeToJson(ResourceKeyArgument.Info<T>.Template p_233275_, JsonObject p_233276_) {
            p_233276_.addProperty("registry", p_233275_.registryKey.location().toString());
        }

        public ResourceKeyArgument.Info<T>.Template unpack(ResourceKeyArgument<T> p_233281_) {
            return new ResourceKeyArgument.Info.Template(p_233281_.registryKey);
        }

        public final class Template implements ArgumentTypeInfo.Template<ResourceKeyArgument<T>> {
            final ResourceKey<? extends Registry<T>> registryKey;

            Template(final ResourceKey<? extends Registry<T>> pRegistryKey) {
                this.registryKey = pRegistryKey;
            }

            public ResourceKeyArgument<T> instantiate(CommandBuildContext p_233299_) {
                return new ResourceKeyArgument<>(this.registryKey);
            }

            @Override
            public ArgumentTypeInfo<ResourceKeyArgument<T>, ?> type() {
                return Info.this;
            }
        }
    }
}