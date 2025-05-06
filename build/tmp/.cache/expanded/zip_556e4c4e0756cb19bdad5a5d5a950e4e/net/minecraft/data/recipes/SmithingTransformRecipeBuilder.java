package net.minecraft.data.recipes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.minecraft.world.item.crafting.TransmuteResult;

public class SmithingTransformRecipeBuilder {
    private final Ingredient template;
    private final Ingredient base;
    private final Ingredient addition;
    private final RecipeCategory category;
    private final Item result;
    private final Map<String, Criterion<?>> criteria = new LinkedHashMap<>();

    public SmithingTransformRecipeBuilder(Ingredient pTemplate, Ingredient pBase, Ingredient pAddition, RecipeCategory pCategory, Item pResult) {
        this.category = pCategory;
        this.template = pTemplate;
        this.base = pBase;
        this.addition = pAddition;
        this.result = pResult;
    }

    public static SmithingTransformRecipeBuilder smithing(
        Ingredient pTemplate, Ingredient pBase, Ingredient pAddition, RecipeCategory pCategory, Item pResult
    ) {
        return new SmithingTransformRecipeBuilder(pTemplate, pBase, pAddition, pCategory, pResult);
    }

    public SmithingTransformRecipeBuilder unlocks(String pKey, Criterion<?> pCriterion) {
        this.criteria.put(pKey, pCriterion);
        return this;
    }

    public void save(RecipeOutput pRecipeOutput, String pRecipeId) {
        this.save(pRecipeOutput, ResourceKey.create(Registries.RECIPE, ResourceLocation.parse(pRecipeId)));
    }

    public void save(RecipeOutput pOutput, ResourceKey<Recipe<?>> pResourceKey) {
        this.ensureValid(pResourceKey);
        Advancement.Builder advancement$builder = pOutput.advancement()
            .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(pResourceKey))
            .rewards(AdvancementRewards.Builder.recipe(pResourceKey))
            .requirements(AdvancementRequirements.Strategy.OR);
        this.criteria.forEach(advancement$builder::addCriterion);
        SmithingTransformRecipe smithingtransformrecipe = new SmithingTransformRecipe(
            Optional.of(this.template), this.base, Optional.of(this.addition), new TransmuteResult(this.result)
        );
        pOutput.accept(
            pResourceKey, smithingtransformrecipe, advancement$builder.build(pResourceKey.location().withPrefix("recipes/" + this.category.getFolderName() + "/"))
        );
    }

    private void ensureValid(ResourceKey<Recipe<?>> pRecipe) {
        if (this.criteria.isEmpty()) {
            throw new IllegalStateException("No way of obtaining recipe " + pRecipe.location());
        }
    }
}