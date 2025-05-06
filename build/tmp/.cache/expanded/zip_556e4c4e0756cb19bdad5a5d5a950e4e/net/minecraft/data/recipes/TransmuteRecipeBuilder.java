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
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.TransmuteRecipe;
import net.minecraft.world.item.crafting.TransmuteResult;

public class TransmuteRecipeBuilder implements RecipeBuilder {
    private final RecipeCategory category;
    private final Holder<Item> result;
    private final Ingredient input;
    private final Ingredient material;
    private final Map<String, Criterion<?>> criteria = new LinkedHashMap<>();
    @Nullable
    private String group;

    private TransmuteRecipeBuilder(RecipeCategory pCategory, Holder<Item> pResult, Ingredient pInput, Ingredient pMaterial) {
        this.category = pCategory;
        this.result = pResult;
        this.input = pInput;
        this.material = pMaterial;
    }

    public static TransmuteRecipeBuilder transmute(RecipeCategory pCategory, Ingredient pInput, Ingredient pMaterial, Item pResult) {
        return new TransmuteRecipeBuilder(pCategory, pResult.builtInRegistryHolder(), pInput, pMaterial);
    }

    public TransmuteRecipeBuilder unlockedBy(String p_361429_, Criterion<?> p_362231_) {
        this.criteria.put(p_361429_, p_362231_);
        return this;
    }

    public TransmuteRecipeBuilder group(@Nullable String p_364491_) {
        this.group = p_364491_;
        return this;
    }

    @Override
    public Item getResult() {
        return this.result.value();
    }

    @Override
    public void save(RecipeOutput p_369743_, ResourceKey<Recipe<?>> p_369659_) {
        this.ensureValid(p_369659_);
        Advancement.Builder advancement$builder = p_369743_.advancement()
            .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(p_369659_))
            .rewards(AdvancementRewards.Builder.recipe(p_369659_))
            .requirements(AdvancementRequirements.Strategy.OR);
        this.criteria.forEach(advancement$builder::addCriterion);
        TransmuteRecipe transmuterecipe = new TransmuteRecipe(
            Objects.requireNonNullElse(this.group, ""),
            RecipeBuilder.determineBookCategory(this.category),
            this.input,
            this.material,
            new TransmuteResult(this.result.value())
        );
        p_369743_.accept(
            p_369659_, transmuterecipe, advancement$builder.build(p_369659_.location().withPrefix("recipes/" + this.category.getFolderName() + "/"))
        );
    }

    private void ensureValid(ResourceKey<Recipe<?>> pRecipe) {
        if (this.criteria.isEmpty()) {
            throw new IllegalStateException("No way of obtaining recipe " + pRecipe.location());
        }
    }
}