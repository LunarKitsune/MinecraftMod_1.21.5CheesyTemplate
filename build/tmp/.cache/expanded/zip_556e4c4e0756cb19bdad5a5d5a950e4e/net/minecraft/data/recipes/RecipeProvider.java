package net.minecraft.data.recipes;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.EnterBlockTrigger;
import net.minecraft.advancements.critereon.ImpossibleTrigger;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.BlockFamilies;
import net.minecraft.data.BlockFamily;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.HoneycombItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.equipment.trim.TrimPattern;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SuspiciousEffectHolder;

public abstract class RecipeProvider {
    protected final HolderLookup.Provider registries;
    private final HolderGetter<Item> items;
    protected final RecipeOutput output;
    private static final Map<BlockFamily.Variant, RecipeProvider.FamilyRecipeProvider> SHAPE_BUILDERS = ImmutableMap.<BlockFamily.Variant, RecipeProvider.FamilyRecipeProvider>builder()
        .put(BlockFamily.Variant.BUTTON, (p_358405_, p_358406_, p_358407_) -> p_358405_.buttonBuilder(p_358406_, Ingredient.of(p_358407_)))
        .put(
            BlockFamily.Variant.CHISELED,
            (p_358408_, p_358409_, p_358410_) -> p_358408_.chiseledBuilder(RecipeCategory.BUILDING_BLOCKS, p_358409_, Ingredient.of(p_358410_))
        )
        .put(
            BlockFamily.Variant.CUT,
            (p_358435_, p_358436_, p_358437_) -> p_358435_.cutBuilder(RecipeCategory.BUILDING_BLOCKS, p_358436_, Ingredient.of(p_358437_))
        )
        .put(BlockFamily.Variant.DOOR, (p_358411_, p_358412_, p_358413_) -> p_358411_.doorBuilder(p_358412_, Ingredient.of(p_358413_)))
        .put(BlockFamily.Variant.CUSTOM_FENCE, (p_358423_, p_358424_, p_358425_) -> p_358423_.fenceBuilder(p_358424_, Ingredient.of(p_358425_)))
        .put(BlockFamily.Variant.FENCE, (p_358450_, p_358451_, p_358452_) -> p_358450_.fenceBuilder(p_358451_, Ingredient.of(p_358452_)))
        .put(BlockFamily.Variant.CUSTOM_FENCE_GATE, (p_358429_, p_358430_, p_358431_) -> p_358429_.fenceGateBuilder(p_358430_, Ingredient.of(p_358431_)))
        .put(BlockFamily.Variant.FENCE_GATE, (p_358417_, p_358418_, p_358419_) -> p_358417_.fenceGateBuilder(p_358418_, Ingredient.of(p_358419_)))
        .put(BlockFamily.Variant.SIGN, (p_358420_, p_358421_, p_358422_) -> p_358420_.signBuilder(p_358421_, Ingredient.of(p_358422_)))
        .put(
            BlockFamily.Variant.SLAB,
            (p_358414_, p_358415_, p_358416_) -> p_358414_.slabBuilder(RecipeCategory.BUILDING_BLOCKS, p_358415_, Ingredient.of(p_358416_))
        )
        .put(BlockFamily.Variant.STAIRS, (p_358426_, p_358427_, p_358428_) -> p_358426_.stairBuilder(p_358427_, Ingredient.of(p_358428_)))
        .put(
            BlockFamily.Variant.PRESSURE_PLATE,
            (p_358447_, p_358448_, p_358449_) -> p_358447_.pressurePlateBuilder(RecipeCategory.REDSTONE, p_358448_, Ingredient.of(p_358449_))
        )
        .put(
            BlockFamily.Variant.POLISHED,
            (p_358399_, p_358400_, p_358401_) -> p_358399_.polishedBuilder(RecipeCategory.BUILDING_BLOCKS, p_358400_, Ingredient.of(p_358401_))
        )
        .put(BlockFamily.Variant.TRAPDOOR, (p_358432_, p_358433_, p_358434_) -> p_358432_.trapdoorBuilder(p_358433_, Ingredient.of(p_358434_)))
        .put(
            BlockFamily.Variant.WALL,
            (p_358402_, p_358403_, p_358404_) -> p_358402_.wallBuilder(RecipeCategory.DECORATIONS, p_358403_, Ingredient.of(p_358404_))
        )
        .build();

    protected RecipeProvider(HolderLookup.Provider pRegistries, RecipeOutput pOutput) {
        this.registries = pRegistries;
        this.items = pRegistries.lookupOrThrow(Registries.ITEM);
        this.output = pOutput;
    }

    protected abstract void buildRecipes();

    protected void generateForEnabledBlockFamilies(FeatureFlagSet pEnabledFeatures) {
        BlockFamilies.getAllFamilies().filter(BlockFamily::shouldGenerateRecipe).forEach(p_358446_ -> this.generateRecipes(p_358446_, pEnabledFeatures));
    }

    protected void oneToOneConversionRecipe(ItemLike pResult, ItemLike pIngredient, @Nullable String pGroup) {
        this.oneToOneConversionRecipe(pResult, pIngredient, pGroup, 1);
    }

    protected void oneToOneConversionRecipe(ItemLike pResult, ItemLike pIngredient, @Nullable String pGroup, int pResultCount) {
        this.shapeless(RecipeCategory.MISC, pResult, pResultCount)
            .requires(pIngredient)
            .group(pGroup)
            .unlockedBy(getHasName(pIngredient), this.has(pIngredient))
            .save(this.output, getConversionRecipeName(pResult, pIngredient));
    }

    protected void oreSmelting(List<ItemLike> pIngredients, RecipeCategory pCategory, ItemLike pResult, float pExperience, int pCookingTime, String pGroup) {
        this.oreCooking(RecipeSerializer.SMELTING_RECIPE, SmeltingRecipe::new, pIngredients, pCategory, pResult, pExperience, pCookingTime, pGroup, "_from_smelting");
    }

    protected void oreBlasting(List<ItemLike> pIngredients, RecipeCategory pCategory, ItemLike pResult, float pExperience, int pCookingTime, String pGroup) {
        this.oreCooking(RecipeSerializer.BLASTING_RECIPE, BlastingRecipe::new, pIngredients, pCategory, pResult, pExperience, pCookingTime, pGroup, "_from_blasting");
    }

    private <T extends AbstractCookingRecipe> void oreCooking(
        RecipeSerializer<T> pSerializer,
        AbstractCookingRecipe.Factory<T> pRecipeFactory,
        List<ItemLike> pIngredients,
        RecipeCategory pCategory,
        ItemLike pResult,
        float pExperience,
        int pCookingTime,
        String pGroup,
        String pSuffix
    ) {
        for (ItemLike itemlike : pIngredients) {
            SimpleCookingRecipeBuilder.generic(Ingredient.of(itemlike), pCategory, pResult, pExperience, pCookingTime, pSerializer, pRecipeFactory)
                .group(pGroup)
                .unlockedBy(getHasName(itemlike), this.has(itemlike))
                .save(this.output, getItemName(pResult) + pSuffix + "_" + getItemName(itemlike));
        }
    }

    protected void netheriteSmithing(Item pIngredientItem, RecipeCategory pCategory, Item pResultItem) {
        SmithingTransformRecipeBuilder.smithing(
                Ingredient.of(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), Ingredient.of(pIngredientItem), this.tag(ItemTags.NETHERITE_TOOL_MATERIALS), pCategory, pResultItem
            )
            .unlocks("has_netherite_ingot", this.has(ItemTags.NETHERITE_TOOL_MATERIALS))
            .save(this.output, getItemName(pResultItem) + "_smithing");
    }

    protected void trimSmithing(Item pTemplate, ResourceKey<TrimPattern> pPattern, ResourceKey<Recipe<?>> pRecipe) {
        Holder.Reference<TrimPattern> reference = this.registries.lookupOrThrow(Registries.TRIM_PATTERN).getOrThrow(pPattern);
        SmithingTrimRecipeBuilder.smithingTrim(
                Ingredient.of(pTemplate), this.tag(ItemTags.TRIMMABLE_ARMOR), this.tag(ItemTags.TRIM_MATERIALS), reference, RecipeCategory.MISC
            )
            .unlocks("has_smithing_trim_template", this.has(pTemplate))
            .save(this.output, pRecipe);
    }

    protected void twoByTwoPacker(RecipeCategory pCategory, ItemLike pPacked, ItemLike pUnpacked) {
        this.shaped(pCategory, pPacked, 1)
            .define('#', pUnpacked)
            .pattern("##")
            .pattern("##")
            .unlockedBy(getHasName(pUnpacked), this.has(pUnpacked))
            .save(this.output);
    }

    protected void threeByThreePacker(RecipeCategory pCategory, ItemLike pPacked, ItemLike pUnpacked, String pCriterionName) {
        this.shapeless(pCategory, pPacked).requires(pUnpacked, 9).unlockedBy(pCriterionName, this.has(pUnpacked)).save(this.output);
    }

    protected void threeByThreePacker(RecipeCategory pCategory, ItemLike pPacked, ItemLike pUnpacked) {
        this.threeByThreePacker(pCategory, pPacked, pUnpacked, getHasName(pUnpacked));
    }

    protected void planksFromLog(ItemLike pPlanks, TagKey<Item> pLogs, int pResultCount) {
        this.shapeless(RecipeCategory.BUILDING_BLOCKS, pPlanks, pResultCount)
            .requires(pLogs)
            .group("planks")
            .unlockedBy("has_log", this.has(pLogs))
            .save(this.output);
    }

    protected void planksFromLogs(ItemLike pPlanks, TagKey<Item> pLogs, int pResult) {
        this.shapeless(RecipeCategory.BUILDING_BLOCKS, pPlanks, pResult)
            .requires(pLogs)
            .group("planks")
            .unlockedBy("has_logs", this.has(pLogs))
            .save(this.output);
    }

    protected void woodFromLogs(ItemLike pWood, ItemLike pLog) {
        this.shaped(RecipeCategory.BUILDING_BLOCKS, pWood, 3)
            .define('#', pLog)
            .pattern("##")
            .pattern("##")
            .group("bark")
            .unlockedBy("has_log", this.has(pLog))
            .save(this.output);
    }

    protected void woodenBoat(ItemLike pBoat, ItemLike pMaterial) {
        this.shaped(RecipeCategory.TRANSPORTATION, pBoat)
            .define('#', pMaterial)
            .pattern("# #")
            .pattern("###")
            .group("boat")
            .unlockedBy("in_water", insideOf(Blocks.WATER))
            .save(this.output);
    }

    protected void chestBoat(ItemLike pBoat, ItemLike pMaterial) {
        this.shapeless(RecipeCategory.TRANSPORTATION, pBoat)
            .requires(Blocks.CHEST)
            .requires(pMaterial)
            .group("chest_boat")
            .unlockedBy("has_boat", this.has(ItemTags.BOATS))
            .save(this.output);
    }

    protected RecipeBuilder buttonBuilder(ItemLike pButton, Ingredient pMaterial) {
        return this.shapeless(RecipeCategory.REDSTONE, pButton).requires(pMaterial);
    }

    protected RecipeBuilder doorBuilder(ItemLike pDoor, Ingredient pMaterial) {
        return this.shaped(RecipeCategory.REDSTONE, pDoor, 3).define('#', pMaterial).pattern("##").pattern("##").pattern("##");
    }

    protected RecipeBuilder fenceBuilder(ItemLike pFence, Ingredient pMaterial) {
        int i = pFence == Blocks.NETHER_BRICK_FENCE ? 6 : 3;
        Item item = pFence == Blocks.NETHER_BRICK_FENCE ? Items.NETHER_BRICK : Items.STICK;
        return this.shaped(RecipeCategory.DECORATIONS, pFence, i).define('W', pMaterial).define('#', item).pattern("W#W").pattern("W#W");
    }

    protected RecipeBuilder fenceGateBuilder(ItemLike pFenceGate, Ingredient pMaterial) {
        return this.shaped(RecipeCategory.REDSTONE, pFenceGate).define('#', Items.STICK).define('W', pMaterial).pattern("#W#").pattern("#W#");
    }

    protected void pressurePlate(ItemLike pPressurePlate, ItemLike pMaterial) {
        this.pressurePlateBuilder(RecipeCategory.REDSTONE, pPressurePlate, Ingredient.of(pMaterial))
            .unlockedBy(getHasName(pMaterial), this.has(pMaterial))
            .save(this.output);
    }

    protected RecipeBuilder pressurePlateBuilder(RecipeCategory pCategory, ItemLike pPressurePlate, Ingredient pMaterial) {
        return this.shaped(pCategory, pPressurePlate).define('#', pMaterial).pattern("##");
    }

    protected void slab(RecipeCategory pCategory, ItemLike pSlab, ItemLike pMaterial) {
        this.slabBuilder(pCategory, pSlab, Ingredient.of(pMaterial))
            .unlockedBy(getHasName(pMaterial), this.has(pMaterial))
            .save(this.output);
    }

    protected RecipeBuilder slabBuilder(RecipeCategory pCategory, ItemLike pSlab, Ingredient pMaterial) {
        return this.shaped(pCategory, pSlab, 6).define('#', pMaterial).pattern("###");
    }

    protected RecipeBuilder stairBuilder(ItemLike pStairs, Ingredient pMaterial) {
        return this.shaped(RecipeCategory.BUILDING_BLOCKS, pStairs, 4).define('#', pMaterial).pattern("#  ").pattern("## ").pattern("###");
    }

    protected RecipeBuilder trapdoorBuilder(ItemLike pTrapdoor, Ingredient pMaterial) {
        return this.shaped(RecipeCategory.REDSTONE, pTrapdoor, 2).define('#', pMaterial).pattern("###").pattern("###");
    }

    protected RecipeBuilder signBuilder(ItemLike pSign, Ingredient pMaterial) {
        return this.shaped(RecipeCategory.DECORATIONS, pSign, 3)
            .group("sign")
            .define('#', pMaterial)
            .define('X', Items.STICK)
            .pattern("###")
            .pattern("###")
            .pattern(" X ");
    }

    protected void hangingSign(ItemLike pSign, ItemLike pMaterial) {
        this.shaped(RecipeCategory.DECORATIONS, pSign, 6)
            .group("hanging_sign")
            .define('#', pMaterial)
            .define('X', Items.CHAIN)
            .pattern("X X")
            .pattern("###")
            .pattern("###")
            .unlockedBy("has_stripped_logs", this.has(pMaterial))
            .save(this.output);
    }

    protected void colorBlockWithDye(List<Item> pDyes, List<Item> pDyeableItems, String pGroup) {
        this.colorWithDye(pDyes, pDyeableItems, null, pGroup, RecipeCategory.BUILDING_BLOCKS);
    }

    protected void colorWithDye(List<Item> pDyes, List<Item> pDyeableItems, @Nullable Item pDye, String pGroup, RecipeCategory pCategory) {
        for (int i = 0; i < pDyes.size(); i++) {
            Item item = pDyes.get(i);
            Item item1 = pDyeableItems.get(i);
            Stream<Item> stream = pDyeableItems.stream().filter(p_288265_ -> !p_288265_.equals(item1));
            if (pDye != null) {
                stream = Stream.concat(stream, Stream.of(pDye));
            }

            this.shapeless(pCategory, item1)
                .requires(item)
                .requires(Ingredient.of(stream))
                .group(pGroup)
                .unlockedBy("has_needed_dye", this.has(item))
                .save(this.output, "dye_" + getItemName(item1));
        }
    }

    protected void carpet(ItemLike pCarpet, ItemLike pMaterial) {
        this.shaped(RecipeCategory.DECORATIONS, pCarpet, 3)
            .define('#', pMaterial)
            .pattern("##")
            .group("carpet")
            .unlockedBy(getHasName(pMaterial), this.has(pMaterial))
            .save(this.output);
    }

    protected void bedFromPlanksAndWool(ItemLike pBed, ItemLike pWool) {
        this.shaped(RecipeCategory.DECORATIONS, pBed)
            .define('#', pWool)
            .define('X', ItemTags.PLANKS)
            .pattern("###")
            .pattern("XXX")
            .group("bed")
            .unlockedBy(getHasName(pWool), this.has(pWool))
            .save(this.output);
    }

    protected void banner(ItemLike pBanner, ItemLike pMaterial) {
        this.shaped(RecipeCategory.DECORATIONS, pBanner)
            .define('#', pMaterial)
            .define('|', Items.STICK)
            .pattern("###")
            .pattern("###")
            .pattern(" | ")
            .group("banner")
            .unlockedBy(getHasName(pMaterial), this.has(pMaterial))
            .save(this.output);
    }

    protected void stainedGlassFromGlassAndDye(ItemLike pStainedGlass, ItemLike pDye) {
        this.shaped(RecipeCategory.BUILDING_BLOCKS, pStainedGlass, 8)
            .define('#', Blocks.GLASS)
            .define('X', pDye)
            .pattern("###")
            .pattern("#X#")
            .pattern("###")
            .group("stained_glass")
            .unlockedBy("has_glass", this.has(Blocks.GLASS))
            .save(this.output);
    }

    protected void stainedGlassPaneFromStainedGlass(ItemLike pStainedGlassPane, ItemLike pStainedGlass) {
        this.shaped(RecipeCategory.DECORATIONS, pStainedGlassPane, 16)
            .define('#', pStainedGlass)
            .pattern("###")
            .pattern("###")
            .group("stained_glass_pane")
            .unlockedBy("has_glass", this.has(pStainedGlass))
            .save(this.output);
    }

    protected void stainedGlassPaneFromGlassPaneAndDye(ItemLike pStainedGlassPane, ItemLike pDye) {
        this.shaped(RecipeCategory.DECORATIONS, pStainedGlassPane, 8)
            .define('#', Blocks.GLASS_PANE)
            .define('$', pDye)
            .pattern("###")
            .pattern("#$#")
            .pattern("###")
            .group("stained_glass_pane")
            .unlockedBy("has_glass_pane", this.has(Blocks.GLASS_PANE))
            .unlockedBy(getHasName(pDye), this.has(pDye))
            .save(this.output, getConversionRecipeName(pStainedGlassPane, Blocks.GLASS_PANE));
    }

    protected void coloredTerracottaFromTerracottaAndDye(ItemLike pTerracotta, ItemLike pDye) {
        this.shaped(RecipeCategory.BUILDING_BLOCKS, pTerracotta, 8)
            .define('#', Blocks.TERRACOTTA)
            .define('X', pDye)
            .pattern("###")
            .pattern("#X#")
            .pattern("###")
            .group("stained_terracotta")
            .unlockedBy("has_terracotta", this.has(Blocks.TERRACOTTA))
            .save(this.output);
    }

    protected void concretePowder(ItemLike pConcretePowder, ItemLike pDye) {
        this.shapeless(RecipeCategory.BUILDING_BLOCKS, pConcretePowder, 8)
            .requires(pDye)
            .requires(Blocks.SAND, 4)
            .requires(Blocks.GRAVEL, 4)
            .group("concrete_powder")
            .unlockedBy("has_sand", this.has(Blocks.SAND))
            .unlockedBy("has_gravel", this.has(Blocks.GRAVEL))
            .save(this.output);
    }

    protected void candle(ItemLike pCandle, ItemLike pDye) {
        this.shapeless(RecipeCategory.DECORATIONS, pCandle)
            .requires(Blocks.CANDLE)
            .requires(pDye)
            .group("dyed_candle")
            .unlockedBy(getHasName(pDye), this.has(pDye))
            .save(this.output);
    }

    protected void wall(RecipeCategory pCategory, ItemLike pWall, ItemLike pMaterial) {
        this.wallBuilder(pCategory, pWall, Ingredient.of(pMaterial))
            .unlockedBy(getHasName(pMaterial), this.has(pMaterial))
            .save(this.output);
    }

    protected RecipeBuilder wallBuilder(RecipeCategory pCategory, ItemLike pWall, Ingredient pMaterial) {
        return this.shaped(pCategory, pWall, 6).define('#', pMaterial).pattern("###").pattern("###");
    }

    protected void polished(RecipeCategory pCategory, ItemLike pResult, ItemLike pMaterial) {
        this.polishedBuilder(pCategory, pResult, Ingredient.of(pMaterial))
            .unlockedBy(getHasName(pMaterial), this.has(pMaterial))
            .save(this.output);
    }

    protected RecipeBuilder polishedBuilder(RecipeCategory pCategory, ItemLike pResult, Ingredient pMaterial) {
        return this.shaped(pCategory, pResult, 4).define('S', pMaterial).pattern("SS").pattern("SS");
    }

    protected void cut(RecipeCategory pCategory, ItemLike pCutResult, ItemLike pMaterial) {
        this.cutBuilder(pCategory, pCutResult, Ingredient.of(pMaterial))
            .unlockedBy(getHasName(pMaterial), this.has(pMaterial))
            .save(this.output);
    }

    protected ShapedRecipeBuilder cutBuilder(RecipeCategory pCategory, ItemLike pCutResult, Ingredient pMaterial) {
        return this.shaped(pCategory, pCutResult, 4).define('#', pMaterial).pattern("##").pattern("##");
    }

    protected void chiseled(RecipeCategory pCategory, ItemLike pChiseledResult, ItemLike pMaterial) {
        this.chiseledBuilder(pCategory, pChiseledResult, Ingredient.of(pMaterial))
            .unlockedBy(getHasName(pMaterial), this.has(pMaterial))
            .save(this.output);
    }

    protected void mosaicBuilder(RecipeCategory pCategory, ItemLike pResult, ItemLike pMaterial) {
        this.shaped(pCategory, pResult)
            .define('#', pMaterial)
            .pattern("#")
            .pattern("#")
            .unlockedBy(getHasName(pMaterial), this.has(pMaterial))
            .save(this.output);
    }

    protected ShapedRecipeBuilder chiseledBuilder(RecipeCategory pCategory, ItemLike pChiseledResult, Ingredient pMaterial) {
        return this.shaped(pCategory, pChiseledResult).define('#', pMaterial).pattern("#").pattern("#");
    }

    protected void stonecutterResultFromBase(RecipeCategory pCategory, ItemLike pResult, ItemLike pMaterial) {
        this.stonecutterResultFromBase(pCategory, pResult, pMaterial, 1);
    }

    protected void stonecutterResultFromBase(RecipeCategory pCategory, ItemLike pResult, ItemLike pMaterial, int pResultCount) {
        SingleItemRecipeBuilder.stonecutting(Ingredient.of(pMaterial), pCategory, pResult, pResultCount)
            .unlockedBy(getHasName(pMaterial), this.has(pMaterial))
            .save(this.output, getConversionRecipeName(pResult, pMaterial) + "_stonecutting");
    }

    private void smeltingResultFromBase(ItemLike pResult, ItemLike pIngredient) {
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(pIngredient), RecipeCategory.BUILDING_BLOCKS, pResult, 0.1F, 200)
            .unlockedBy(getHasName(pIngredient), this.has(pIngredient))
            .save(this.output);
    }

    protected void nineBlockStorageRecipes(RecipeCategory pUnpackedCategory, ItemLike pUnpacked, RecipeCategory pPackedCategory, ItemLike pPacked) {
        this.nineBlockStorageRecipes(pUnpackedCategory, pUnpacked, pPackedCategory, pPacked, getSimpleRecipeName(pPacked), null, getSimpleRecipeName(pUnpacked), null);
    }

    protected void nineBlockStorageRecipesWithCustomPacking(RecipeCategory pUnpackedCategory, ItemLike pUnpacked, RecipeCategory pPackedCategory, ItemLike pPacked, String pPackedName, String pPackedGroup) {
        this.nineBlockStorageRecipes(pUnpackedCategory, pUnpacked, pPackedCategory, pPacked, pPackedName, pPackedGroup, getSimpleRecipeName(pUnpacked), null);
    }

    protected void nineBlockStorageRecipesRecipesWithCustomUnpacking(RecipeCategory pUnpackedCategory, ItemLike pUnpacked, RecipeCategory pPackedCategory, ItemLike pPacked, String pUnpackedName, String pUnpackedGroup) {
        this.nineBlockStorageRecipes(pUnpackedCategory, pUnpacked, pPackedCategory, pPacked, getSimpleRecipeName(pPacked), null, pUnpackedName, pUnpackedGroup);
    }

    private void nineBlockStorageRecipes(
        RecipeCategory pUnpackedCategory,
        ItemLike pUnpacked,
        RecipeCategory pPackedCategory,
        ItemLike pPacked,
        String pPackedName,
        @Nullable String pPackedGroup,
        String pUnpackedName,
        @Nullable String pUnpackedGroup
    ) {
        this.shapeless(pUnpackedCategory, pUnpacked, 9)
            .requires(pPacked)
            .group(pUnpackedGroup)
            .unlockedBy(getHasName(pPacked), this.has(pPacked))
            .save(this.output, ResourceKey.create(Registries.RECIPE, ResourceLocation.parse(pUnpackedName)));
        this.shaped(pPackedCategory, pPacked)
            .define('#', pUnpacked)
            .pattern("###")
            .pattern("###")
            .pattern("###")
            .group(pPackedGroup)
            .unlockedBy(getHasName(pUnpacked), this.has(pUnpacked))
            .save(this.output, ResourceKey.create(Registries.RECIPE, ResourceLocation.parse(pPackedName)));
    }

    protected void copySmithingTemplate(ItemLike pTemplate, ItemLike pBaseItem) {
        this.shaped(RecipeCategory.MISC, pTemplate, 2)
            .define('#', Items.DIAMOND)
            .define('C', pBaseItem)
            .define('S', pTemplate)
            .pattern("#S#")
            .pattern("#C#")
            .pattern("###")
            .unlockedBy(getHasName(pTemplate), this.has(pTemplate))
            .save(this.output);
    }

    protected void copySmithingTemplate(ItemLike pTemplate, Ingredient pBaseItem) {
        this.shaped(RecipeCategory.MISC, pTemplate, 2)
            .define('#', Items.DIAMOND)
            .define('C', pBaseItem)
            .define('S', pTemplate)
            .pattern("#S#")
            .pattern("#C#")
            .pattern("###")
            .unlockedBy(getHasName(pTemplate), this.has(pTemplate))
            .save(this.output);
    }

    protected <T extends AbstractCookingRecipe> void cookRecipes(
        String pCookingMethod, RecipeSerializer<T> pCookingSerializer, AbstractCookingRecipe.Factory<T> pRecipeFactory, int pCookingTime
    ) {
        this.simpleCookingRecipe(pCookingMethod, pCookingSerializer, pRecipeFactory, pCookingTime, Items.BEEF, Items.COOKED_BEEF, 0.35F);
        this.simpleCookingRecipe(pCookingMethod, pCookingSerializer, pRecipeFactory, pCookingTime, Items.CHICKEN, Items.COOKED_CHICKEN, 0.35F);
        this.simpleCookingRecipe(pCookingMethod, pCookingSerializer, pRecipeFactory, pCookingTime, Items.COD, Items.COOKED_COD, 0.35F);
        this.simpleCookingRecipe(pCookingMethod, pCookingSerializer, pRecipeFactory, pCookingTime, Items.KELP, Items.DRIED_KELP, 0.1F);
        this.simpleCookingRecipe(pCookingMethod, pCookingSerializer, pRecipeFactory, pCookingTime, Items.SALMON, Items.COOKED_SALMON, 0.35F);
        this.simpleCookingRecipe(pCookingMethod, pCookingSerializer, pRecipeFactory, pCookingTime, Items.MUTTON, Items.COOKED_MUTTON, 0.35F);
        this.simpleCookingRecipe(pCookingMethod, pCookingSerializer, pRecipeFactory, pCookingTime, Items.PORKCHOP, Items.COOKED_PORKCHOP, 0.35F);
        this.simpleCookingRecipe(pCookingMethod, pCookingSerializer, pRecipeFactory, pCookingTime, Items.POTATO, Items.BAKED_POTATO, 0.35F);
        this.simpleCookingRecipe(pCookingMethod, pCookingSerializer, pRecipeFactory, pCookingTime, Items.RABBIT, Items.COOKED_RABBIT, 0.35F);
    }

    private <T extends AbstractCookingRecipe> void simpleCookingRecipe(
        String pCookingMethod,
        RecipeSerializer<T> pCookingSerializer,
        AbstractCookingRecipe.Factory<T> pRecipeFactory,
        int pCookingTime,
        ItemLike pMaterial,
        ItemLike pResult,
        float pExperience
    ) {
        SimpleCookingRecipeBuilder.generic(Ingredient.of(pMaterial), RecipeCategory.FOOD, pResult, pExperience, pCookingTime, pCookingSerializer, pRecipeFactory)
            .unlockedBy(getHasName(pMaterial), this.has(pMaterial))
            .save(this.output, getItemName(pResult) + "_from_" + pCookingMethod);
    }

    protected void waxRecipes(FeatureFlagSet pRequiredFeatures) {
        HoneycombItem.WAXABLES
            .get()
            .forEach(
                (p_389726_, p_389727_) -> {
                    if (p_389727_.requiredFeatures().isSubsetOf(pRequiredFeatures)) {
                        this.shapeless(RecipeCategory.BUILDING_BLOCKS, p_389727_)
                            .requires(p_389726_)
                            .requires(Items.HONEYCOMB)
                            .group(getItemName(p_389727_))
                            .unlockedBy(getHasName(p_389726_), this.has(p_389726_))
                            .save(this.output, getConversionRecipeName(p_389727_, Items.HONEYCOMB));
                    }
                }
            );
    }

    protected void grate(Block pGrateBlock, Block pMaterial) {
        this.shaped(RecipeCategory.BUILDING_BLOCKS, pGrateBlock, 4)
            .define('M', pMaterial)
            .pattern(" M ")
            .pattern("M M")
            .pattern(" M ")
            .unlockedBy(getHasName(pMaterial), this.has(pMaterial))
            .save(this.output);
    }

    protected void copperBulb(Block pBulbBlock, Block pMaterial) {
        this.shaped(RecipeCategory.REDSTONE, pBulbBlock, 4)
            .define('C', pMaterial)
            .define('R', Items.REDSTONE)
            .define('B', Items.BLAZE_ROD)
            .pattern(" C ")
            .pattern("CBC")
            .pattern(" R ")
            .unlockedBy(getHasName(pMaterial), this.has(pMaterial))
            .save(this.output);
    }

    protected void suspiciousStew(Item pFlowerItem, SuspiciousEffectHolder pEffect) {
        ItemStack itemstack = new ItemStack(
            Items.SUSPICIOUS_STEW.builtInRegistryHolder(), 1, DataComponentPatch.builder().set(DataComponents.SUSPICIOUS_STEW_EFFECTS, pEffect.getSuspiciousEffects()).build()
        );
        this.shapeless(RecipeCategory.FOOD, itemstack)
            .requires(Items.BOWL)
            .requires(Items.BROWN_MUSHROOM)
            .requires(Items.RED_MUSHROOM)
            .requires(pFlowerItem)
            .group("suspicious_stew")
            .unlockedBy(getHasName(pFlowerItem), this.has(pFlowerItem))
            .save(this.output, getItemName(itemstack.getItem()) + "_from_" + getItemName(pFlowerItem));
    }

    protected void generateRecipes(BlockFamily pBlockFamily, FeatureFlagSet pRequiredFeatures) {
        pBlockFamily.getVariants()
            .forEach(
                (p_358443_, p_358444_) -> {
                    if (p_358444_.requiredFeatures().isSubsetOf(pRequiredFeatures)) {
                        RecipeProvider.FamilyRecipeProvider recipeprovider$familyrecipeprovider = SHAPE_BUILDERS.get(p_358443_);
                        ItemLike itemlike = this.getBaseBlock(pBlockFamily, p_358443_);
                        if (recipeprovider$familyrecipeprovider != null) {
                            RecipeBuilder recipebuilder = recipeprovider$familyrecipeprovider.create(this, p_358444_, itemlike);
                            pBlockFamily.getRecipeGroupPrefix()
                                .ifPresent(
                                    p_296361_ -> recipebuilder.group(p_296361_ + (p_358443_ == BlockFamily.Variant.CUT ? "" : "_" + p_358443_.getRecipeGroup()))
                                );
                            recipebuilder.unlockedBy(pBlockFamily.getRecipeUnlockedBy().orElseGet(() -> getHasName(itemlike)), this.has(itemlike));
                            recipebuilder.save(this.output);
                        }

                        if (p_358443_ == BlockFamily.Variant.CRACKED) {
                            this.smeltingResultFromBase(p_358444_, itemlike);
                        }
                    }
                }
            );
    }

    protected Block getBaseBlock(BlockFamily pFamily, BlockFamily.Variant pVariant) {
        if (pVariant == BlockFamily.Variant.CHISELED) {
            if (!pFamily.getVariants().containsKey(BlockFamily.Variant.SLAB)) {
                throw new IllegalStateException("Slab is not defined for the family.");
            } else {
                return pFamily.get(BlockFamily.Variant.SLAB);
            }
        } else {
            return pFamily.getBaseBlock();
        }
    }

    private static Criterion<EnterBlockTrigger.TriggerInstance> insideOf(Block pBlock) {
        return CriteriaTriggers.ENTER_BLOCK
            .createCriterion(new EnterBlockTrigger.TriggerInstance(Optional.empty(), Optional.of(pBlock.builtInRegistryHolder()), Optional.empty()));
    }

    private Criterion<InventoryChangeTrigger.TriggerInstance> has(MinMaxBounds.Ints pCount, ItemLike pItem) {
        return inventoryTrigger(ItemPredicate.Builder.item().of(this.items, pItem).withCount(pCount));
    }

    protected Criterion<InventoryChangeTrigger.TriggerInstance> has(ItemLike pItemLike) {
        return inventoryTrigger(ItemPredicate.Builder.item().of(this.items, pItemLike));
    }

    protected Criterion<InventoryChangeTrigger.TriggerInstance> has(TagKey<Item> pTag) {
        return inventoryTrigger(ItemPredicate.Builder.item().of(this.items, pTag));
    }

    private static Criterion<InventoryChangeTrigger.TriggerInstance> inventoryTrigger(ItemPredicate.Builder... pItems) {
        return inventoryTrigger(Arrays.stream(pItems).map(ItemPredicate.Builder::build).toArray(ItemPredicate[]::new));
    }

    private static Criterion<InventoryChangeTrigger.TriggerInstance> inventoryTrigger(ItemPredicate... pPredicates) {
        return CriteriaTriggers.INVENTORY_CHANGED
            .createCriterion(new InventoryChangeTrigger.TriggerInstance(Optional.empty(), InventoryChangeTrigger.TriggerInstance.Slots.ANY, List.of(pPredicates)));
    }

    protected static String getHasName(ItemLike pItemLike) {
        return "has_" + getItemName(pItemLike);
    }

    protected static String getItemName(ItemLike pItemLike) {
        return BuiltInRegistries.ITEM.getKey(pItemLike.asItem()).getPath();
    }

    protected static String getSimpleRecipeName(ItemLike pItemLike) {
        return getItemName(pItemLike);
    }

    protected static String getConversionRecipeName(ItemLike pResult, ItemLike pIngredient) {
        return getItemName(pResult) + "_from_" + getItemName(pIngredient);
    }

    protected static String getSmeltingRecipeName(ItemLike pItemLike) {
        return getItemName(pItemLike) + "_from_smelting";
    }

    protected static String getBlastingRecipeName(ItemLike pItemLike) {
        return getItemName(pItemLike) + "_from_blasting";
    }

    protected Ingredient tag(TagKey<Item> pTag) {
        return Ingredient.of(this.items.getOrThrow(pTag));
    }

    protected ShapedRecipeBuilder shaped(RecipeCategory pCategory, ItemLike pResult) {
        return ShapedRecipeBuilder.shaped(this.items, pCategory, pResult);
    }

    protected ShapedRecipeBuilder shaped(RecipeCategory pCategory, ItemLike pResult, int pCount) {
        return ShapedRecipeBuilder.shaped(this.items, pCategory, pResult, pCount);
    }

    protected ShapelessRecipeBuilder shapeless(RecipeCategory pCategory, ItemStack pResult) {
        return ShapelessRecipeBuilder.shapeless(this.items, pCategory, pResult);
    }

    protected ShapelessRecipeBuilder shapeless(RecipeCategory pCategory, ItemLike pResult) {
        return ShapelessRecipeBuilder.shapeless(this.items, pCategory, pResult);
    }

    protected ShapelessRecipeBuilder shapeless(RecipeCategory pCategory, ItemLike pResult, int pCount) {
        return ShapelessRecipeBuilder.shapeless(this.items, pCategory, pResult, pCount);
    }

    @FunctionalInterface
    interface FamilyRecipeProvider {
        RecipeBuilder create(RecipeProvider pRecipeProvider, ItemLike pIngredient, ItemLike pResult);
    }

    public abstract static class Runner implements DataProvider {
        private final PackOutput packOutput;
        private final CompletableFuture<HolderLookup.Provider> registries;

        protected Runner(PackOutput pPackOutput, CompletableFuture<HolderLookup.Provider> pRegistries) {
            this.packOutput = pPackOutput;
            this.registries = pRegistries;
        }

        @Override
        public final CompletableFuture<?> run(CachedOutput p_363906_) {
            return this.registries
                .thenCompose(
                    p_362805_ -> {
                        final PackOutput.PathProvider packoutput$pathprovider = this.packOutput.createRegistryElementsPathProvider(Registries.RECIPE);
                        final PackOutput.PathProvider packoutput$pathprovider1 = this.packOutput.createRegistryElementsPathProvider(Registries.ADVANCEMENT);
                        final Set<ResourceKey<Recipe<?>>> set = Sets.newHashSet();
                        final List<CompletableFuture<?>> list = new ArrayList<>();
                        RecipeOutput recipeoutput = new RecipeOutput() {
                            @Override
                            public void accept(ResourceKey<Recipe<?>> p_361204_, Recipe<?> p_363495_, @Nullable net.minecraft.resources.ResourceLocation advancementId, @Nullable com.google.gson.JsonElement advancement) {
                                if (!set.add(p_361204_)) {
                                    throw new IllegalStateException("Duplicate recipe " + p_361204_.location());
                                } else {
                                    this.saveRecipe(p_361204_, p_363495_);
                                    if (advancement != null && advancementId != null) {
                                        this.saveAdvancement(advancementId, advancement);
                                    }
                                }
                            }

                            @Override
                            public Advancement.Builder advancement() {
                                return Advancement.Builder.recipeAdvancement().parent(RecipeBuilder.ROOT_RECIPE_ADVANCEMENT);
                            }

                            @Override
                            public void includeRootAdvancement() {
                                AdvancementHolder advancementholder = Advancement.Builder.recipeAdvancement()
                                    .addCriterion("impossible", CriteriaTriggers.IMPOSSIBLE.createCriterion(new ImpossibleTrigger.TriggerInstance()))
                                    .build(RecipeBuilder.ROOT_RECIPE_ADVANCEMENT);
                                var ops = registry().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE);
                                var json = Advancement.CODEC.encodeStart(ops, advancementholder.value()).getOrThrow(IllegalStateException::new);
                                this.saveAdvancement(advancementholder.id(), json);
                            }

                            private void saveRecipe(ResourceKey<Recipe<?>> p_368864_, Recipe<?> p_368184_) {
                                list.add(
                                    DataProvider.saveStable(
                                        p_363906_, p_362805_, Recipe.CODEC, p_368184_, packoutput$pathprovider.json(p_368864_.location())
                                    )
                                );
                            }

                            private void saveAdvancement(net.minecraft.resources.ResourceLocation id, com.google.gson.JsonElement advancement) {
                                list.add(
                                    DataProvider.saveStable(
                                        p_363906_,
                                        advancement,
                                        packoutput$pathprovider1.json(id)
                                    )
                                );
                            }

                            @Override
                            public HolderLookup.Provider registry() {
                                return p_362805_;
                            }
                        };
                        this.createRecipeProvider(p_362805_, recipeoutput).buildRecipes();
                        return CompletableFuture.allOf(list.toArray(CompletableFuture[]::new));
                    }
                );
        }

        protected abstract RecipeProvider createRecipeProvider(HolderLookup.Provider pRegistries, RecipeOutput pOutput);
    }
}
