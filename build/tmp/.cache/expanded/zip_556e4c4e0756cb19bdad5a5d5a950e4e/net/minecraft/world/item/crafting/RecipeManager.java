package net.minecraft.world.item.crafting;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;

public class RecipeManager extends SimplePreparableReloadListener<RecipeMap> implements RecipeAccess {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<ResourceKey<RecipePropertySet>, RecipeManager.IngredientExtractor> RECIPE_PROPERTY_SETS = Map.of(
        RecipePropertySet.SMITHING_ADDITION,
        p_359832_ -> p_359832_ instanceof SmithingRecipe smithingrecipe ? smithingrecipe.additionIngredient() : Optional.empty(),
        RecipePropertySet.SMITHING_BASE,
        p_390824_ -> p_390824_ instanceof SmithingRecipe smithingrecipe ? Optional.of(smithingrecipe.baseIngredient()) : Optional.empty(),
        RecipePropertySet.SMITHING_TEMPLATE,
        p_359833_ -> p_359833_ instanceof SmithingRecipe smithingrecipe ? smithingrecipe.templateIngredient() : Optional.empty(),
        RecipePropertySet.FURNACE_INPUT,
        forSingleInput(RecipeType.SMELTING),
        RecipePropertySet.BLAST_FURNACE_INPUT,
        forSingleInput(RecipeType.BLASTING),
        RecipePropertySet.SMOKER_INPUT,
        forSingleInput(RecipeType.SMOKING),
        RecipePropertySet.CAMPFIRE_INPUT,
        forSingleInput(RecipeType.CAMPFIRE_COOKING)
    );
    private static final FileToIdConverter RECIPE_LISTER = FileToIdConverter.registry(Registries.RECIPE);
    private final HolderLookup.Provider registries;
    private RecipeMap recipes = RecipeMap.EMPTY;
    private Map<ResourceKey<RecipePropertySet>, RecipePropertySet> propertySets = Map.of();
    private SelectableRecipe.SingleInputSet<StonecutterRecipe> stonecutterRecipes = SelectableRecipe.SingleInputSet.empty();
    private List<RecipeManager.ServerDisplayInfo> allDisplays = List.of();
    private Map<ResourceKey<Recipe<?>>, List<RecipeManager.ServerDisplayInfo>> recipeToDisplay = Map.of();
    private final net.minecraftforge.common.crafting.conditions.ICondition.IContext context; //Forge: add context

    /** @deprecated Forge: use {@linkplain RecipeManager#RecipeManager(net.minecraftforge.common.crafting.conditions.ICondition.IContext) constructor with context}. */
    public RecipeManager(HolderLookup.Provider pRegistries) {
        this(pRegistries, net.minecraftforge.common.crafting.conditions.ICondition.IContext.EMPTY);
    }

    public RecipeManager(HolderLookup.Provider pRegistries, net.minecraftforge.common.crafting.conditions.ICondition.IContext context) {
        this.registries = pRegistries;
        this.context = context;
    }

    protected RecipeMap prepare(ResourceManager p_368640_, ProfilerFiller p_361102_) {
        SortedMap<ResourceLocation, Recipe<?>> sortedmap = new TreeMap<>();
        SimpleJsonResourceReloadListener.scanDirectory(p_368640_, RECIPE_LISTER, this.context.wrap(this.registries.createSerializationContext(JsonOps.INSTANCE)), Recipe.CODEC, sortedmap);
        List<RecipeHolder<?>> list = new ArrayList<>(sortedmap.size());
        sortedmap.forEach((p_359835_, p_359836_) -> {
            ResourceKey<Recipe<?>> resourcekey = ResourceKey.create(Registries.RECIPE, p_359835_);
            RecipeHolder<?> recipeholder = new RecipeHolder<>(resourcekey, p_359836_);
            list.add(recipeholder);
        });
        return RecipeMap.create(list);
    }

    protected void apply(RecipeMap p_369166_, ResourceManager p_44038_, ProfilerFiller p_44039_) {
        this.recipes = p_369166_;
        LOGGER.info("Loaded {} recipes", p_369166_.values().size());
    }

    public void finalizeRecipeLoading(FeatureFlagSet pEnabledFeatures) {
        //net.minecraftforge.event.ForgeEventFactory.onTagsUpdated(this.registries, false, false);
        List<SelectableRecipe.SingleInputEntry<StonecutterRecipe>> list = new ArrayList<>();
        List<RecipeManager.IngredientCollector> list1 = RECIPE_PROPERTY_SETS.entrySet()
            .stream()
            .map(p_359831_ -> new RecipeManager.IngredientCollector(p_359831_.getKey(), p_359831_.getValue()))
            .toList();
        this.recipes
            .values()
            .forEach(
                p_359840_ -> {
                    Recipe<?> recipe = p_359840_.value();
                    if (!recipe.isSpecial() && recipe.placementInfo().isImpossibleToPlace()) {
                        LOGGER.warn("Recipe {} can't be placed due to empty ingredients and will be ignored", p_359840_.id().location());
                    } else {
                        list1.forEach(p_359842_ -> p_359842_.accept(recipe));
                        if (recipe instanceof StonecutterRecipe stonecutterrecipe
                            && isIngredientEnabled(pEnabledFeatures, stonecutterrecipe.input())
                            && stonecutterrecipe.resultDisplay().isEnabled(pEnabledFeatures)) {
                            list.add(
                                new SelectableRecipe.SingleInputEntry<>(
                                    stonecutterrecipe.input(),
                                    new SelectableRecipe<>(stonecutterrecipe.resultDisplay(), Optional.of((RecipeHolder<StonecutterRecipe>)p_359840_))
                                )
                            );
                        }
                    }
                }
            );
        this.propertySets = list1.stream().collect(Collectors.toUnmodifiableMap(p_359830_ -> p_359830_.key, p_359826_ -> p_359826_.asPropertySet(pEnabledFeatures)));
        this.stonecutterRecipes = new SelectableRecipe.SingleInputSet<>(list);
        this.allDisplays = unpackRecipeInfo(this.recipes.values(), pEnabledFeatures);
        this.recipeToDisplay = this.allDisplays
            .stream()
            .collect(Collectors.groupingBy(p_359820_ -> p_359820_.parent.id(), IdentityHashMap::new, Collectors.toList()));
    }

    static List<Ingredient> filterDisabled(FeatureFlagSet pEnabledFeatures, List<Ingredient> pIngredients) {
        pIngredients.removeIf(p_359829_ -> !isIngredientEnabled(pEnabledFeatures, p_359829_));
        return pIngredients;
    }

    private static boolean isIngredientEnabled(FeatureFlagSet pEnabledFeatures, Ingredient pIngredient) {
        return pIngredient.items().allMatch(p_359822_ -> p_359822_.value().isEnabled(pEnabledFeatures));
    }

    public <I extends RecipeInput, T extends Recipe<I>> Optional<RecipeHolder<T>> getRecipeFor(
        RecipeType<T> pRecipeType, I pInput, Level pLevel, @Nullable ResourceKey<Recipe<?>> pRecipe
    ) {
        RecipeHolder<T> recipeholder = pRecipe != null ? this.byKeyTyped(pRecipeType, pRecipe) : null;
        return this.getRecipeFor(pRecipeType, pInput, pLevel, recipeholder);
    }

    public <I extends RecipeInput, T extends Recipe<I>> Optional<RecipeHolder<T>> getRecipeFor(
        RecipeType<T> pRecipeType, I pInput, Level pLevel, @Nullable RecipeHolder<T> pLastRecipe
    ) {
        return pLastRecipe != null && pLastRecipe.value().matches(pInput, pLevel)
            ? Optional.of(pLastRecipe)
            : this.getRecipeFor(pRecipeType, pInput, pLevel);
    }

    public <I extends RecipeInput, T extends Recipe<I>> Optional<RecipeHolder<T>> getRecipeFor(RecipeType<T> pRecipeType, I pInput, Level pLevel) {
        return this.recipes.getRecipesFor(pRecipeType, pInput, pLevel).findFirst();
    }

    public Optional<RecipeHolder<?>> byKey(ResourceKey<Recipe<?>> pKey) {
        return Optional.ofNullable(this.recipes.byKey(pKey));
    }

    @Nullable
    private <T extends Recipe<?>> RecipeHolder<T> byKeyTyped(RecipeType<T> pType, ResourceKey<Recipe<?>> pKey) {
        RecipeHolder<?> recipeholder = this.recipes.byKey(pKey);
        return (RecipeHolder<T>)(recipeholder != null && recipeholder.value().getType().equals(pType) ? recipeholder : null);
    }

    public Map<ResourceKey<RecipePropertySet>, RecipePropertySet> getSynchronizedItemProperties() {
        return this.propertySets;
    }

    public SelectableRecipe.SingleInputSet<StonecutterRecipe> getSynchronizedStonecutterRecipes() {
        return this.stonecutterRecipes;
    }

    @Override
    public RecipePropertySet propertySet(ResourceKey<RecipePropertySet> p_367484_) {
        return this.propertySets.getOrDefault(p_367484_, RecipePropertySet.EMPTY);
    }

    @Override
    public SelectableRecipe.SingleInputSet<StonecutterRecipe> stonecutterRecipes() {
        return this.stonecutterRecipes;
    }

    public Collection<RecipeHolder<?>> getRecipes() {
        return this.recipes.values();
    }

    @Nullable
    public RecipeManager.ServerDisplayInfo getRecipeFromDisplay(RecipeDisplayId pDisplay) {
        return this.allDisplays.get(pDisplay.index());
    }

    public void listDisplaysForRecipe(ResourceKey<Recipe<?>> pRecipe, Consumer<RecipeDisplayEntry> pOutput) {
        List<RecipeManager.ServerDisplayInfo> list = this.recipeToDisplay.get(pRecipe);
        if (list != null) {
            list.forEach(p_359824_ -> pOutput.accept(p_359824_.display));
        }
    }

    @VisibleForTesting
    protected static RecipeHolder<?> fromJson(ResourceKey<Recipe<?>> pRecipe, JsonObject pJson, HolderLookup.Provider pRegistries) {
        Recipe<?> recipe = Recipe.CODEC.parse(pRegistries.createSerializationContext(JsonOps.INSTANCE), pJson).getOrThrow(JsonParseException::new);
        return new RecipeHolder<>(pRecipe, recipe);
    }

    public static <I extends RecipeInput, T extends Recipe<I>> RecipeManager.CachedCheck<I, T> createCheck(final RecipeType<T> pRecipeType) {
        return new RecipeManager.CachedCheck<I, T>() {
            @Nullable
            private ResourceKey<Recipe<?>> lastRecipe;

            @Override
            public Optional<RecipeHolder<T>> getRecipeFor(I p_343525_, ServerLevel p_364008_) {
                RecipeManager recipemanager = p_364008_.recipeAccess();
                Optional<RecipeHolder<T>> optional = recipemanager.getRecipeFor(pRecipeType, p_343525_, p_364008_, this.lastRecipe);
                if (optional.isPresent()) {
                    RecipeHolder<T> recipeholder = optional.get();
                    this.lastRecipe = recipeholder.id();
                    return Optional.of(recipeholder);
                } else {
                    return Optional.empty();
                }
            }
        };
    }

    private static List<RecipeManager.ServerDisplayInfo> unpackRecipeInfo(Iterable<RecipeHolder<?>> pRecipes, FeatureFlagSet pEnabledFeatures) {
        List<RecipeManager.ServerDisplayInfo> list = new ArrayList<>();
        Object2IntMap<String> object2intmap = new Object2IntOpenHashMap<>();

        for (RecipeHolder<?> recipeholder : pRecipes) {
            Recipe<?> recipe = recipeholder.value();
            OptionalInt optionalint;
            if (recipe.group().isEmpty()) {
                optionalint = OptionalInt.empty();
            } else {
                optionalint = OptionalInt.of(object2intmap.computeIfAbsent(recipe.group(), p_359844_ -> object2intmap.size()));
            }

            Optional<List<Ingredient>> optional;
            if (recipe.isSpecial()) {
                optional = Optional.empty();
            } else {
                optional = Optional.of(recipe.placementInfo().ingredients());
            }

            for (RecipeDisplay recipedisplay : recipe.display()) {
                if (recipedisplay.isEnabled(pEnabledFeatures)) {
                    int i = list.size();
                    RecipeDisplayId recipedisplayid = new RecipeDisplayId(i);
                    RecipeDisplayEntry recipedisplayentry = new RecipeDisplayEntry(recipedisplayid, recipedisplay, optionalint, recipe.recipeBookCategory(), optional);
                    list.add(new RecipeManager.ServerDisplayInfo(recipedisplayentry, recipeholder));
                }
            }
        }

        return list;
    }

    private static RecipeManager.IngredientExtractor forSingleInput(RecipeType<? extends SingleItemRecipe> pRecipeType) {
        return p_359846_ -> p_359846_.getType() == pRecipeType && p_359846_ instanceof SingleItemRecipe singleitemrecipe
            ? Optional.of(singleitemrecipe.input())
            : Optional.empty();
    }

    public interface CachedCheck<I extends RecipeInput, T extends Recipe<I>> {
        Optional<RecipeHolder<T>> getRecipeFor(I pInput, ServerLevel pLevel);
    }

    public static class IngredientCollector implements Consumer<Recipe<?>> {
        final ResourceKey<RecipePropertySet> key;
        private final RecipeManager.IngredientExtractor extractor;
        private final List<Ingredient> ingredients = new ArrayList<>();

        protected IngredientCollector(ResourceKey<RecipePropertySet> pKey, RecipeManager.IngredientExtractor pExtractor) {
            this.key = pKey;
            this.extractor = pExtractor;
        }

        public void accept(Recipe<?> p_361793_) {
            this.extractor.apply(p_361793_).ifPresent(this.ingredients::add);
        }

        public RecipePropertySet asPropertySet(FeatureFlagSet pEnabledFeatures) {
            return RecipePropertySet.create(RecipeManager.filterDisabled(pEnabledFeatures, this.ingredients));
        }
    }

    @FunctionalInterface
    public interface IngredientExtractor {
        Optional<Ingredient> apply(Recipe<?> pRecipe);
    }

    public record ServerDisplayInfo(RecipeDisplayEntry display, RecipeHolder<?> parent) {
    }
}
