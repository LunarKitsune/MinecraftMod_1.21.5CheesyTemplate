package net.minecraft.data.recipes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.level.ItemLike;

public class SingleItemRecipeBuilder implements RecipeBuilder {
    private final RecipeCategory category;
    private final Item result;
    private final Ingredient ingredient;
    private final int count;
    private final Map<String, Criterion<?>> criteria = new LinkedHashMap<>();
    @Nullable
    private String group;
    private final SingleItemRecipe.Factory<?> factory;

    public SingleItemRecipeBuilder(RecipeCategory pCategory, SingleItemRecipe.Factory<?> pFactory, Ingredient pIngredient, ItemLike pResult, int pCount) {
        this.category = pCategory;
        this.factory = pFactory;
        this.result = pResult.asItem();
        this.ingredient = pIngredient;
        this.count = pCount;
    }

    public static SingleItemRecipeBuilder stonecutting(Ingredient pIngredient, RecipeCategory pCategory, ItemLike pResult) {
        return new SingleItemRecipeBuilder(pCategory, StonecutterRecipe::new, pIngredient, pResult, 1);
    }

    public static SingleItemRecipeBuilder stonecutting(Ingredient pIngredient, RecipeCategory pCategory, ItemLike pResult, int pCount) {
        return new SingleItemRecipeBuilder(pCategory, StonecutterRecipe::new, pIngredient, pResult, pCount);
    }

    public SingleItemRecipeBuilder unlockedBy(String p_176810_, Criterion<?> p_298188_) {
        this.criteria.put(p_176810_, p_298188_);
        return this;
    }

    public SingleItemRecipeBuilder group(@Nullable String p_176808_) {
        this.group = p_176808_;
        return this;
    }

    @Override
    public Item getResult() {
        return this.result;
    }

    @Override
    public void save(RecipeOutput p_298439_, ResourceKey<Recipe<?>> p_362425_) {
        this.ensureValid(p_362425_);
        Advancement.Builder advancement$builder = p_298439_.advancement()
            .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(p_362425_))
            .rewards(AdvancementRewards.Builder.recipe(p_362425_))
            .requirements(AdvancementRequirements.Strategy.OR);
        this.criteria.forEach(advancement$builder::addCriterion);
        SingleItemRecipe singleitemrecipe = this.factory
            .create(Objects.requireNonNullElse(this.group, ""), this.ingredient, new ItemStack(this.result, this.count));
        p_298439_.accept(
            p_362425_, singleitemrecipe, advancement$builder.build(p_362425_.location().withPrefix("recipes/" + this.category.getFolderName() + "/"))
        );
    }

    private void ensureValid(ResourceKey<Recipe<?>> pRecipe) {
        if (this.criteria.isEmpty()) {
            throw new IllegalStateException("No way of obtaining recipe " + pRecipe.location());
        }
    }
}