package net.minecraft.data.recipes;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.SmithingTrimRecipe;
import net.minecraft.world.item.equipment.trim.TrimPattern;

public class SmithingTrimRecipeBuilder {
    private final RecipeCategory category;
    private final Ingredient template;
    private final Ingredient base;
    private final Ingredient addition;
    private final Holder<TrimPattern> pattern;
    private final Map<String, Criterion<?>> criteria = new LinkedHashMap<>();

    public SmithingTrimRecipeBuilder(RecipeCategory pCategory, Ingredient pTemplate, Ingredient pBase, Ingredient pAddition, Holder<TrimPattern> pPattern) {
        this.category = pCategory;
        this.template = pTemplate;
        this.base = pBase;
        this.addition = pAddition;
        this.pattern = pPattern;
    }

    public static SmithingTrimRecipeBuilder smithingTrim(
        Ingredient pTemplate, Ingredient pBase, Ingredient pAddition, Holder<TrimPattern> pPattern, RecipeCategory pCategory
    ) {
        return new SmithingTrimRecipeBuilder(pCategory, pTemplate, pBase, pAddition, pPattern);
    }

    public SmithingTrimRecipeBuilder unlocks(String pKey, Criterion<?> pCriterion) {
        this.criteria.put(pKey, pCriterion);
        return this;
    }

    public void save(RecipeOutput pOutput, ResourceKey<Recipe<?>> pResourceKey) {
        this.ensureValid(pResourceKey);
        Advancement.Builder advancement$builder = pOutput.advancement()
            .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(pResourceKey))
            .rewards(AdvancementRewards.Builder.recipe(pResourceKey))
            .requirements(AdvancementRequirements.Strategy.OR);
        this.criteria.forEach(advancement$builder::addCriterion);
        SmithingTrimRecipe smithingtrimrecipe = new SmithingTrimRecipe(this.template, this.base, this.addition, this.pattern);
        pOutput.accept(
            pResourceKey, smithingtrimrecipe, advancement$builder.build(pResourceKey.location().withPrefix("recipes/" + this.category.getFolderName() + "/"))
        );
    }

    private void ensureValid(ResourceKey<Recipe<?>> pRecipe) {
        if (this.criteria.isEmpty()) {
            throw new IllegalStateException("No way of obtaining recipe " + pRecipe.location());
        }
    }
}