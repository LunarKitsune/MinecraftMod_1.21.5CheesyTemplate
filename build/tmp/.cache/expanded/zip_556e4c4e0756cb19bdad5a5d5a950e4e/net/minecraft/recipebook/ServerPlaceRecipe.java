package net.minecraft.recipebook;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;

public class ServerPlaceRecipe<R extends Recipe<?>> {
    private static final int ITEM_NOT_FOUND = -1;
    private final Inventory inventory;
    private final ServerPlaceRecipe.CraftingMenuAccess<R> menu;
    private final boolean useMaxItems;
    private final int gridWidth;
    private final int gridHeight;
    private final List<Slot> inputGridSlots;
    private final List<Slot> slotsToClear;

    public static <I extends RecipeInput, R extends Recipe<I>> RecipeBookMenu.PostPlaceAction placeRecipe(
        ServerPlaceRecipe.CraftingMenuAccess<R> pMenu,
        int pGridWidth,
        int pGridHeight,
        List<Slot> pInputGridSlots,
        List<Slot> pSlotsToClear,
        Inventory pInventory,
        RecipeHolder<R> pRecipe,
        boolean pUseMaxItems,
        boolean pIsCreative
    ) {
        ServerPlaceRecipe<R> serverplacerecipe = new ServerPlaceRecipe<>(pMenu, pInventory, pUseMaxItems, pGridWidth, pGridHeight, pInputGridSlots, pSlotsToClear);
        if (!pIsCreative && !serverplacerecipe.testClearGrid()) {
            return RecipeBookMenu.PostPlaceAction.NOTHING;
        } else {
            StackedItemContents stackeditemcontents = new StackedItemContents();
            pInventory.fillStackedContents(stackeditemcontents);
            pMenu.fillCraftSlotsStackedContents(stackeditemcontents);
            return serverplacerecipe.tryPlaceRecipe(pRecipe, stackeditemcontents);
        }
    }

    private ServerPlaceRecipe(
        ServerPlaceRecipe.CraftingMenuAccess<R> pMenu,
        Inventory pInventory,
        boolean pUseMaxItems,
        int pGridWidth,
        int pGridHeight,
        List<Slot> pInputGridSlots,
        List<Slot> pSlotsToClear
    ) {
        this.menu = pMenu;
        this.inventory = pInventory;
        this.useMaxItems = pUseMaxItems;
        this.gridWidth = pGridWidth;
        this.gridHeight = pGridHeight;
        this.inputGridSlots = pInputGridSlots;
        this.slotsToClear = pSlotsToClear;
    }

    private RecipeBookMenu.PostPlaceAction tryPlaceRecipe(RecipeHolder<R> pRecipe, StackedItemContents pStackedItemContents) {
        if (pStackedItemContents.canCraft(pRecipe.value(), null)) {
            this.placeRecipe(pRecipe, pStackedItemContents);
            this.inventory.setChanged();
            return RecipeBookMenu.PostPlaceAction.NOTHING;
        } else {
            this.clearGrid();
            this.inventory.setChanged();
            return RecipeBookMenu.PostPlaceAction.PLACE_GHOST_RECIPE;
        }
    }

    private void clearGrid() {
        for (Slot slot : this.slotsToClear) {
            ItemStack itemstack = slot.getItem().copy();
            this.inventory.placeItemBackInInventory(itemstack, false);
            slot.set(itemstack);
        }

        this.menu.clearCraftingContent();
    }

    private void placeRecipe(RecipeHolder<R> pRecipe, StackedItemContents pStackedItemContents) {
        boolean flag = this.menu.recipeMatches(pRecipe);
        int i = pStackedItemContents.getBiggestCraftableStack(pRecipe.value(), null);
        if (flag) {
            for (Slot slot : this.inputGridSlots) {
                ItemStack itemstack = slot.getItem();
                if (!itemstack.isEmpty() && Math.min(i, itemstack.getMaxStackSize()) < itemstack.getCount() + 1) {
                    return;
                }
            }
        }

        int j = this.calculateAmountToCraft(i, flag);
        List<Holder<Item>> list = new ArrayList<>();
        if (pStackedItemContents.canCraft(pRecipe.value(), j, list::add)) {
            int k = clampToMaxStackSize(j, list);
            if (k != j) {
                list.clear();
                if (!pStackedItemContents.canCraft(pRecipe.value(), k, list::add)) {
                    return;
                }
            }

            this.clearGrid();
            PlaceRecipeHelper.placeRecipe(
                this.gridWidth,
                this.gridHeight,
                pRecipe.value(),
                pRecipe.value().placementInfo().slotsToIngredientIndex(),
                (p_374854_, p_374855_, p_374856_, p_374857_) -> {
                    if (p_374854_ != -1) {
                        Slot slot1 = this.inputGridSlots.get(p_374855_);
                        Holder<Item> holder = list.get(p_374854_);
                        int l = k;

                        while (l > 0) {
                            l = this.moveItemToGrid(slot1, holder, l);
                            if (l == -1) {
                                return;
                            }
                        }
                    }
                }
            );
        }
    }

    private static int clampToMaxStackSize(int pAmount, List<Holder<Item>> pItems) {
        for (Holder<Item> holder : pItems) {
            pAmount = Math.min(pAmount, holder.value().getDefaultMaxStackSize());
        }

        return pAmount;
    }

    private int calculateAmountToCraft(int pMax, boolean pRecipeMatches) {
        if (this.useMaxItems) {
            return pMax;
        } else if (pRecipeMatches) {
            int i = Integer.MAX_VALUE;

            for (Slot slot : this.inputGridSlots) {
                ItemStack itemstack = slot.getItem();
                if (!itemstack.isEmpty() && i > itemstack.getCount()) {
                    i = itemstack.getCount();
                }
            }

            if (i != Integer.MAX_VALUE) {
                i++;
            }

            return i;
        } else {
            return 1;
        }
    }

    private int moveItemToGrid(Slot pSlot, Holder<Item> pItem, int pCount) {
        ItemStack itemstack = pSlot.getItem();
        int i = this.inventory.findSlotMatchingCraftingIngredient(pItem, itemstack);
        if (i == -1) {
            return -1;
        } else {
            ItemStack itemstack1 = this.inventory.getItem(i);
            ItemStack itemstack2;
            if (pCount < itemstack1.getCount()) {
                itemstack2 = this.inventory.removeItem(i, pCount);
            } else {
                itemstack2 = this.inventory.removeItemNoUpdate(i);
            }

            int j = itemstack2.getCount();
            if (itemstack.isEmpty()) {
                pSlot.set(itemstack2);
            } else {
                itemstack.grow(j);
            }

            return pCount - j;
        }
    }

    private boolean testClearGrid() {
        List<ItemStack> list = Lists.newArrayList();
        int i = this.getAmountOfFreeSlotsInInventory();

        for (Slot slot : this.inputGridSlots) {
            ItemStack itemstack = slot.getItem().copy();
            if (!itemstack.isEmpty()) {
                int j = this.inventory.getSlotWithRemainingSpace(itemstack);
                if (j == -1 && list.size() <= i) {
                    for (ItemStack itemstack1 : list) {
                        if (ItemStack.isSameItem(itemstack1, itemstack)
                            && itemstack1.getCount() != itemstack1.getMaxStackSize()
                            && itemstack1.getCount() + itemstack.getCount() <= itemstack1.getMaxStackSize()) {
                            itemstack1.grow(itemstack.getCount());
                            itemstack.setCount(0);
                            break;
                        }
                    }

                    if (!itemstack.isEmpty()) {
                        if (list.size() >= i) {
                            return false;
                        }

                        list.add(itemstack);
                    }
                } else if (j == -1) {
                    return false;
                }
            }
        }

        return true;
    }

    private int getAmountOfFreeSlotsInInventory() {
        int i = 0;

        for (ItemStack itemstack : this.inventory.getNonEquipmentItems()) {
            if (itemstack.isEmpty()) {
                i++;
            }
        }

        return i;
    }

    public interface CraftingMenuAccess<T extends Recipe<?>> {
        void fillCraftSlotsStackedContents(StackedItemContents pStackedItemContents);

        void clearCraftingContent();

        boolean recipeMatches(RecipeHolder<T> pRecipe);
    }
}