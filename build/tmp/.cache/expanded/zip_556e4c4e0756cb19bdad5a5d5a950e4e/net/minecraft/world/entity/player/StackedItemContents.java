package net.minecraft.world.entity.player;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;

public class StackedItemContents {
    private final StackedContents<Holder<Item>> raw = new StackedContents<>();

    public void accountSimpleStack(ItemStack pStack) {
        if (Inventory.isUsableForCrafting(pStack)) {
            this.accountStack(pStack);
        }
    }

    public void accountStack(ItemStack pStack) {
        this.accountStack(pStack, pStack.getMaxStackSize());
    }

    public void accountStack(ItemStack pStack, int pMaxStackSize) {
        if (!pStack.isEmpty()) {
            int i = Math.min(pMaxStackSize, pStack.getCount());
            this.raw.account(pStack.getItemHolder(), i);
        }
    }

    public boolean canCraft(Recipe<?> pRecipe, @Nullable StackedContents.Output<Holder<Item>> pOutput) {
        return this.canCraft(pRecipe, 1, pOutput);
    }

    public boolean canCraft(Recipe<?> pRecipe, int pMaxCount, @Nullable StackedContents.Output<Holder<Item>> pOutput) {
        PlacementInfo placementinfo = pRecipe.placementInfo();
        return placementinfo.isImpossibleToPlace() ? false : this.canCraft(placementinfo.ingredients(), pMaxCount, pOutput);
    }

    public boolean canCraft(List<? extends StackedContents.IngredientInfo<Holder<Item>>> pIngredients, @Nullable StackedContents.Output<Holder<Item>> pOutput) {
        return this.canCraft(pIngredients, 1, pOutput);
    }

    private boolean canCraft(
        List<? extends StackedContents.IngredientInfo<Holder<Item>>> pIngredients, int pMaxCount, @Nullable StackedContents.Output<Holder<Item>> pOutput
    ) {
        return this.raw.tryPick(pIngredients, pMaxCount, pOutput);
    }

    public int getBiggestCraftableStack(Recipe<?> pRecipe, @Nullable StackedContents.Output<Holder<Item>> pOutput) {
        return this.getBiggestCraftableStack(pRecipe, Integer.MAX_VALUE, pOutput);
    }

    public int getBiggestCraftableStack(Recipe<?> pRecipe, int pMaxCount, @Nullable StackedContents.Output<Holder<Item>> pOutput) {
        return this.raw.tryPickAll(pRecipe.placementInfo().ingredients(), pMaxCount, pOutput);
    }

    public void clear() {
        this.raw.clear();
    }
}