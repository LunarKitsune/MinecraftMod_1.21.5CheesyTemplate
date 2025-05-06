package net.minecraft.world.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ChestMenu extends AbstractContainerMenu {
    private final Container container;
    private final int containerRows;

    private ChestMenu(MenuType<?> pType, int pContainerId, Inventory pPlayerInventory, int pRows) {
        this(pType, pContainerId, pPlayerInventory, new SimpleContainer(9 * pRows), pRows);
    }

    public static ChestMenu oneRow(int pContainerId, Inventory pPlayerInventory) {
        return new ChestMenu(MenuType.GENERIC_9x1, pContainerId, pPlayerInventory, 1);
    }

    public static ChestMenu twoRows(int pContainerId, Inventory pPlayerInventory) {
        return new ChestMenu(MenuType.GENERIC_9x2, pContainerId, pPlayerInventory, 2);
    }

    public static ChestMenu threeRows(int pContainerId, Inventory pPlayerInventory) {
        return new ChestMenu(MenuType.GENERIC_9x3, pContainerId, pPlayerInventory, 3);
    }

    public static ChestMenu fourRows(int pContainerId, Inventory pPlayerInventory) {
        return new ChestMenu(MenuType.GENERIC_9x4, pContainerId, pPlayerInventory, 4);
    }

    public static ChestMenu fiveRows(int pContainerId, Inventory pPlayerInventory) {
        return new ChestMenu(MenuType.GENERIC_9x5, pContainerId, pPlayerInventory, 5);
    }

    public static ChestMenu sixRows(int pContainerId, Inventory pPlayerInventory) {
        return new ChestMenu(MenuType.GENERIC_9x6, pContainerId, pPlayerInventory, 6);
    }

    public static ChestMenu threeRows(int pContainerId, Inventory pPlayerInventory, Container pContainer) {
        return new ChestMenu(MenuType.GENERIC_9x3, pContainerId, pPlayerInventory, pContainer, 3);
    }

    public static ChestMenu sixRows(int pContainerId, Inventory pPlayerInventory, Container pContainer) {
        return new ChestMenu(MenuType.GENERIC_9x6, pContainerId, pPlayerInventory, pContainer, 6);
    }

    public ChestMenu(MenuType<?> pType, int pContainerId, Inventory pPlayerInventory, Container pContainer, int pRows) {
        super(pType, pContainerId);
        checkContainerSize(pContainer, pRows * 9);
        this.container = pContainer;
        this.containerRows = pRows;
        pContainer.startOpen(pPlayerInventory.player);
        int i = 18;
        this.addChestGrid(pContainer, 8, 18);
        int j = 18 + this.containerRows * 18 + 13;
        this.addStandardInventorySlots(pPlayerInventory, 8, j);
    }

    private void addChestGrid(Container pContainer, int pX, int pY) {
        for (int i = 0; i < this.containerRows; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(pContainer, j + i * 9, pX + j * 18, pY + i * 18));
            }
        }
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return this.container.stillValid(pPlayer);
    }

    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(pIndex);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (pIndex < this.containerRows * 9) {
                if (!this.moveItemStackTo(itemstack1, this.containerRows * 9, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(itemstack1, 0, this.containerRows * 9, false)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return itemstack;
    }

    @Override
    public void removed(Player pPlayer) {
        super.removed(pPlayer);
        this.container.stopOpen(pPlayer);
    }

    public Container getContainer() {
        return this.container;
    }

    public int getRowCount() {
        return this.containerRows;
    }
}