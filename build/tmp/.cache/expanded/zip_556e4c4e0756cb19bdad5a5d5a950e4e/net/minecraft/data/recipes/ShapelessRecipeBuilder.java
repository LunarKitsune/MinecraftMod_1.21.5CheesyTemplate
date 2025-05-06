package net.minecraft.data.recipes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.core.HolderGetter;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.ItemLike;

public class ShapelessRecipeBuilder implements RecipeBuilder {
    private final HolderGetter<Item> items;
    private final RecipeCategory category;
    private final ItemStack result;
    private final List<Ingredient> ingredients = new ArrayList<>();
    private final Map<String, Criterion<?>> criteria = new LinkedHashMap<>();
    @Nullable
    private String group;

    private ShapelessRecipeBuilder(HolderGetter<Item> pItems, RecipeCategory pCategory, ItemStack pResult) {
        this.items = pItems;
        this.category = pCategory;
        this.result = pResult;
    }

    public static ShapelessRecipeBuilder shapeless(HolderGetter<Item> pItems, RecipeCategory pCategory, ItemStack pResult) {
        return new ShapelessRecipeBuilder(pItems, pCategory, pResult);
    }

    public static ShapelessRecipeBuilder shapeless(HolderGetter<Item> pItems, RecipeCategory pCategory, ItemLike pResult) {
        return shapeless(pItems, pCategory, pResult, 1);
    }

    public static ShapelessRecipeBuilder shapeless(HolderGetter<Item> pItems, RecipeCategory pCategory, ItemLike pResult, int pCount) {
        return new ShapelessRecipeBuilder(pItems, pCategory, pResult.asItem().getDefaultInstance().copyWithCount(pCount));
    }

    public ShapelessRecipeBuilder requires(TagKey<Item> pTag) {
        return this.requires(Ingredient.of(this.items.getOrThrow(pTag)));
    }

    public ShapelessRecipeBuilder requires(ItemLike pItem) {
        return this.requires(pItem, 1);
    }

    public ShapelessRecipeBuilder requires(ItemLike pItem, int pQuantity) {
        for (int i = 0; i < pQuantity; i++) {
            this.requires(Ingredient.of(pItem));
        }

        return this;
    }

    public ShapelessRecipeBuilder requires(Ingredient pIngredient) {
        return this.requires(pIngredient, 1);
    }

    public ShapelessRecipeBuilder requires(Ingredient pIngredient, int pQuantity) {
        for (int i = 0; i < pQuantity; i++) {
            this.ingredients.add(pIngredient);
        }

        return this;
    }

    public ShapelessRecipeBuilder unlockedBy(String p_176781_, Criterion<?> p_300919_) {
        this.criteria.put(p_176781_, p_300919_);
        return this;
    }

    public ShapelessRecipeBuilder group(@Nullable String p_126195_) {
        this.group = p_126195_;
        return this;
    }

    @Override
    public Item getResult() {
        return this.result.getItem();
    }

    @Override
    public void save(RecipeOutput p_300117_, ResourceKey<Recipe<?>> p_364714_) {
        this.ensureValid(p_364714_);
        Advancement.Builder advancement$builder = p_300117_.advancement()
            .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(p_364714_))
            .rewards(AdvancementRewards.Builder.recipe(p_364714_))
            .requirements(AdvancementRequirements.Strategy.OR);
        this.criteria.forEach(advancement$builder::addCriterion);
        ShapelessRecipe shapelessrecipe = new ShapelessRecipe(
            Objects.requireNonNullElse(this.group, ""), RecipeBuilder.determineBookCategory(this.category), this.result, this.ingredients
        );
        p_300117_.accept(
            p_364714_, shapelessrecipe, advancement$builder.build(p_364714_.location().withPrefix("recipes/" + this.category.getFolderName() + "/"))
        );
    }

    private void ensureValid(ResourceKey<Recipe<?>> pRecipe) {
        if (this.criteria.isEmpty()) {
            throw new IllegalStateException("No way of obtaining recipe " + pRecipe.location());
        }
    }
}