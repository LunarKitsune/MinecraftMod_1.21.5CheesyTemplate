package net.minecraft.world.inventory;

import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.item.ItemStack;

public class TransientCraftingContainer implements CraftingContainer {
    private final NonNullList<ItemStack> items;
    private final int width;
    private final int height;
    private final AbstractContainerMenu menu;

    public TransientCraftingContainer(AbstractContainerMenu pMenu, int pWidth, int pHeight) {
        this(pMenu, pWidth, pHeight, NonNullList.withSize(pWidth * pHeight, ItemStack.EMPTY));
    }

    private TransientCraftingContainer(AbstractContainerMenu pMenu, int pWidth, int pHeight, NonNullList<ItemStack> pItems) {
        this.items = pItems;
        this.menu = pMenu;
        this.width = pWidth;
        this.height = pHeight;
    }

    @Override
    public int getContainerSize() {
        return this.items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack itemstack : this.items) {
            if (!itemstack.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public ItemStack getItem(int p_287712_) {
        return p_287712_ >= this.getContainerSize() ? ItemStack.EMPTY : this.items.get(p_287712_);
    }

    @Override
    public ItemStack removeItemNoUpdate(int p_287637_) {
        return ContainerHelper.takeItem(this.items, p_287637_);
    }

    @Override
    public ItemStack removeItem(int p_287682_, int p_287576_) {
        ItemStack itemstack = ContainerHelper.removeItem(this.items, p_287682_, p_287576_);
        if (!itemstack.isEmpty()) {
            this.menu.slotsChanged(this);
        }

        return itemstack;
    }

    @Override
    public void setItem(int p_287681_, ItemStack p_287620_) {
        this.items.set(p_287681_, p_287620_);
        this.menu.slotsChanged(this);
    }

    @Override
    public void setChanged() {
    }

    @Override
    public boolean stillValid(Player p_287774_) {
        return true;
    }

    @Override
    public void clearContent() {
        this.items.clear();
    }

    @Override
    public int getHeight() {
        return this.height;
    }

    @Override
    public int getWidth() {
        return this.width;
    }

    @Override
    public List<ItemStack> getItems() {
        return List.copyOf(this.items);
    }

    @Override
    public void fillStackedContents(StackedItemContents p_368219_) {
        for (ItemStack itemstack : this.items) {
            p_368219_.accountSimpleStack(itemstack);
        }
    }
}