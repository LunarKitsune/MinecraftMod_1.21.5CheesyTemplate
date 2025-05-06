package net.minecraft.data.recipes;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
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
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.ItemLike;

public class ShapedRecipeBuilder implements RecipeBuilder {
    private final HolderGetter<Item> items;
    private final RecipeCategory category;
    private final Item result;
    private final int count;
    private final List<String> rows = Lists.newArrayList();
    private final Map<Character, Ingredient> key = Maps.newLinkedHashMap();
    private final Map<String, Criterion<?>> criteria = new LinkedHashMap<>();
    @Nullable
    private String group;
    private boolean showNotification = true;

    private ShapedRecipeBuilder(HolderGetter<Item> pItems, RecipeCategory pCategory, ItemLike pResult, int pCount) {
        this.items = pItems;
        this.category = pCategory;
        this.result = pResult.asItem();
        this.count = pCount;
    }

    public static ShapedRecipeBuilder shaped(HolderGetter<Item> pItems, RecipeCategory pCategory, ItemLike pResult) {
        return shaped(pItems, pCategory, pResult, 1);
    }

    public static ShapedRecipeBuilder shaped(HolderGetter<Item> pItems, RecipeCategory pCategory, ItemLike pResult, int pCount) {
        return new ShapedRecipeBuilder(pItems, pCategory, pResult, pCount);
    }

    public ShapedRecipeBuilder define(Character pSymbol, TagKey<Item> pTag) {
        return this.define(pSymbol, Ingredient.of(this.items.getOrThrow(pTag)));
    }

    public ShapedRecipeBuilder define(Character pSymbol, ItemLike pItem) {
        return this.define(pSymbol, Ingredient.of(pItem));
    }

    public ShapedRecipeBuilder define(Character pSymbol, Ingredient pIngredient) {
        if (this.key.containsKey(pSymbol)) {
            throw new IllegalArgumentException("Symbol '" + pSymbol + "' is already defined!");
        } else if (pSymbol == ' ') {
            throw new IllegalArgumentException("Symbol ' ' (whitespace) is reserved and cannot be defined");
        } else {
            this.key.put(pSymbol, pIngredient);
            return this;
        }
    }

    public ShapedRecipeBuilder pattern(String pPattern) {
        if (!this.rows.isEmpty() && pPattern.length() != this.rows.get(0).length()) {
            throw new IllegalArgumentException("Pattern must be the same width on every line!");
        } else {
            this.rows.add(pPattern);
            return this;
        }
    }

    public ShapedRecipeBuilder unlockedBy(String p_176751_, Criterion<?> p_300780_) {
        this.criteria.put(p_176751_, p_300780_);
        return this;
    }

    public ShapedRecipeBuilder group(@Nullable String p_126146_) {
        this.group = p_126146_;
        return this;
    }

    public ShapedRecipeBuilder showNotification(boolean pShowNotification) {
        this.showNotification = pShowNotification;
        return this;
    }

    @Override
    public Item getResult() {
        return this.result;
    }

    @Override
    public void save(RecipeOutput p_298334_, ResourceKey<Recipe<?>> p_366310_) {
        ShapedRecipePattern shapedrecipepattern = this.ensureValid(p_366310_);
        Advancement.Builder advancement$builder = p_298334_.advancement()
            .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(p_366310_))
            .rewards(AdvancementRewards.Builder.recipe(p_366310_))
            .requirements(AdvancementRequirements.Strategy.OR);
        this.criteria.forEach(advancement$builder::addCriterion);
        ShapedRecipe shapedrecipe = new ShapedRecipe(
            Objects.requireNonNullElse(this.group, ""),
            RecipeBuilder.determineBookCategory(this.category),
            shapedrecipepattern,
            new ItemStack(this.result, this.count),
            this.showNotification
        );
        p_298334_.accept(
            p_366310_, shapedrecipe, advancement$builder.build(p_366310_.location().withPrefix("recipes/" + this.category.getFolderName() + "/"))
        );
    }

    private ShapedRecipePattern ensureValid(ResourceKey<Recipe<?>> pRecipe) {
        if (this.criteria.isEmpty()) {
            throw new IllegalStateException("No way of obtaining recipe " + pRecipe.location());
        } else {
            return ShapedRecipePattern.of(this.key, this.rows);
        }
    }
}