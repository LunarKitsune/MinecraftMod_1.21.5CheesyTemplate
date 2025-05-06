package net.minecraft.world.level.block.entity;

import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntSortedMap;
import java.util.Collections;
import java.util.SequencedSet;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;

public class FuelValues {
    private final Object2IntSortedMap<Item> values;

    FuelValues(Object2IntSortedMap<Item> pValues) {
        this.values = pValues;
    }

    public boolean isFuel(ItemStack pStack) {
        return isFuel(pStack, null);
    }

    public boolean isFuel(ItemStack stack, @org.jetbrains.annotations.Nullable net.minecraft.world.item.crafting.RecipeType<?> type) {
        return burnDuration(stack, type) > 0;
    }

    public SequencedSet<Item> fuelItems() {
        return Collections.unmodifiableSequencedSet(this.values.keySet());
    }

    public int burnDuration(ItemStack pStack) {
        return burnDuration(pStack, null);
    }

    public int burnDuration(ItemStack stack, @org.jetbrains.annotations.Nullable net.minecraft.world.item.crafting.RecipeType<?> type) {
        if (stack.isEmpty())
            return 0;
        int ret = stack.getBurnTime(type);
        if (ret == -1)
            ret = this.values.getInt(stack.getItem());
        return net.minecraftforge.event.ForgeEventFactory.getItemBurnTime(stack, ret, type);
    }

    public static FuelValues vanillaBurnTimes(HolderLookup.Provider pRegistries, FeatureFlagSet pEnabledFeatures) {
        return vanillaBurnTimes(pRegistries, pEnabledFeatures, 200);
    }

    public static FuelValues vanillaBurnTimes(HolderLookup.Provider pRegistries, FeatureFlagSet pEnabledFeatures, int pSmeltingTime) {
        return new FuelValues.Builder(pRegistries, pEnabledFeatures)
            .add(Items.LAVA_BUCKET, pSmeltingTime * 100)
            .add(Blocks.COAL_BLOCK, pSmeltingTime * 8 * 10)
            .add(Items.BLAZE_ROD, pSmeltingTime * 12)
            .add(Items.COAL, pSmeltingTime * 8)
            .add(Items.CHARCOAL, pSmeltingTime * 8)
            .add(ItemTags.LOGS, pSmeltingTime * 3 / 2)
            .add(ItemTags.BAMBOO_BLOCKS, pSmeltingTime * 3 / 2)
            .add(ItemTags.PLANKS, pSmeltingTime * 3 / 2)
            .add(Blocks.BAMBOO_MOSAIC, pSmeltingTime * 3 / 2)
            .add(ItemTags.WOODEN_STAIRS, pSmeltingTime * 3 / 2)
            .add(Blocks.BAMBOO_MOSAIC_STAIRS, pSmeltingTime * 3 / 2)
            .add(ItemTags.WOODEN_SLABS, pSmeltingTime * 3 / 4)
            .add(Blocks.BAMBOO_MOSAIC_SLAB, pSmeltingTime * 3 / 4)
            .add(ItemTags.WOODEN_TRAPDOORS, pSmeltingTime * 3 / 2)
            .add(ItemTags.WOODEN_PRESSURE_PLATES, pSmeltingTime * 3 / 2)
            .add(ItemTags.WOODEN_FENCES, pSmeltingTime * 3 / 2)
            .add(ItemTags.FENCE_GATES, pSmeltingTime * 3 / 2)
            .add(Blocks.NOTE_BLOCK, pSmeltingTime * 3 / 2)
            .add(Blocks.BOOKSHELF, pSmeltingTime * 3 / 2)
            .add(Blocks.CHISELED_BOOKSHELF, pSmeltingTime * 3 / 2)
            .add(Blocks.LECTERN, pSmeltingTime * 3 / 2)
            .add(Blocks.JUKEBOX, pSmeltingTime * 3 / 2)
            .add(Blocks.CHEST, pSmeltingTime * 3 / 2)
            .add(Blocks.TRAPPED_CHEST, pSmeltingTime * 3 / 2)
            .add(Blocks.CRAFTING_TABLE, pSmeltingTime * 3 / 2)
            .add(Blocks.DAYLIGHT_DETECTOR, pSmeltingTime * 3 / 2)
            .add(ItemTags.BANNERS, pSmeltingTime * 3 / 2)
            .add(Items.BOW, pSmeltingTime * 3 / 2)
            .add(Items.FISHING_ROD, pSmeltingTime * 3 / 2)
            .add(Blocks.LADDER, pSmeltingTime * 3 / 2)
            .add(ItemTags.SIGNS, pSmeltingTime)
            .add(ItemTags.HANGING_SIGNS, pSmeltingTime * 4)
            .add(Items.WOODEN_SHOVEL, pSmeltingTime)
            .add(Items.WOODEN_SWORD, pSmeltingTime)
            .add(Items.WOODEN_HOE, pSmeltingTime)
            .add(Items.WOODEN_AXE, pSmeltingTime)
            .add(Items.WOODEN_PICKAXE, pSmeltingTime)
            .add(ItemTags.WOODEN_DOORS, pSmeltingTime)
            .add(ItemTags.BOATS, pSmeltingTime * 6)
            .add(ItemTags.WOOL, pSmeltingTime / 2)
            .add(ItemTags.WOODEN_BUTTONS, pSmeltingTime / 2)
            .add(Items.STICK, pSmeltingTime / 2)
            .add(ItemTags.SAPLINGS, pSmeltingTime / 2)
            .add(Items.BOWL, pSmeltingTime / 2)
            .add(ItemTags.WOOL_CARPETS, 1 + pSmeltingTime / 3)
            .add(Blocks.DRIED_KELP_BLOCK, 1 + pSmeltingTime * 20)
            .add(Items.CROSSBOW, pSmeltingTime * 3 / 2)
            .add(Blocks.BAMBOO, pSmeltingTime / 4)
            .add(Blocks.DEAD_BUSH, pSmeltingTime / 2)
            .add(Blocks.SHORT_DRY_GRASS, pSmeltingTime / 2)
            .add(Blocks.TALL_DRY_GRASS, pSmeltingTime / 2)
            .add(Blocks.SCAFFOLDING, pSmeltingTime / 4)
            .add(Blocks.LOOM, pSmeltingTime * 3 / 2)
            .add(Blocks.BARREL, pSmeltingTime * 3 / 2)
            .add(Blocks.CARTOGRAPHY_TABLE, pSmeltingTime * 3 / 2)
            .add(Blocks.FLETCHING_TABLE, pSmeltingTime * 3 / 2)
            .add(Blocks.SMITHING_TABLE, pSmeltingTime * 3 / 2)
            .add(Blocks.COMPOSTER, pSmeltingTime * 3 / 2)
            .add(Blocks.AZALEA, pSmeltingTime / 2)
            .add(Blocks.FLOWERING_AZALEA, pSmeltingTime / 2)
            .add(Blocks.MANGROVE_ROOTS, pSmeltingTime * 3 / 2)
            .add(Blocks.LEAF_LITTER, pSmeltingTime / 2)
            .remove(ItemTags.NON_FLAMMABLE_WOOD)
            .build();
    }

    public static class Builder {
        private final HolderLookup<Item> items;
        private final FeatureFlagSet enabledFeatures;
        private final Object2IntSortedMap<Item> values = new Object2IntLinkedOpenHashMap<>();

        public Builder(HolderLookup.Provider pRegistries, FeatureFlagSet pEnabledFeatures) {
            this.items = pRegistries.lookupOrThrow(Registries.ITEM);
            this.enabledFeatures = pEnabledFeatures;
        }

        public FuelValues build() {
            return new FuelValues(this.values);
        }

        public FuelValues.Builder remove(TagKey<Item> pTag) {
            this.values.keySet().removeIf(p_361506_ -> p_361506_.builtInRegistryHolder().is(pTag));
            return this;
        }

        public FuelValues.Builder add(TagKey<Item> pTag, int pValue) {
            this.items.get(pTag).ifPresent(p_361860_ -> {
                for (Holder<Item> holder : p_361860_) {
                    this.putInternal(pValue, holder.value());
                }
            });
            return this;
        }

        public FuelValues.Builder add(ItemLike pItem, int pValue) {
            Item item = pItem.asItem();
            this.putInternal(pValue, item);
            return this;
        }

        private void putInternal(int pValue, Item pItem) {
            if (pItem.isEnabled(this.enabledFeatures)) {
                this.values.put(pItem, pValue);
            }
        }
    }
}
