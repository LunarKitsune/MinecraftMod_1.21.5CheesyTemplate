package net.minecraft.world.inventory;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public abstract class ItemCombinerMenu extends AbstractContainerMenu {
    private static final int INVENTORY_SLOTS_PER_ROW = 9;
    private static final int INVENTORY_ROWS = 3;
    private static final int INPUT_SLOT_START = 0;
    protected final ContainerLevelAccess access;
    protected final Player player;
    protected final Container inputSlots;
    protected final ResultContainer resultSlots = new ResultContainer() {
        @Override
        public void setChanged() {
            ItemCombinerMenu.this.slotsChanged(this);
        }
    };
    private final int resultSlotIndex;

    protected boolean mayPickup(Player pPlayer, boolean pHasStack) {
        return true;
    }

    protected abstract void onTake(Player pPlayer, ItemStack pStack);

    protected abstract boolean isValidBlock(BlockState pState);

    public ItemCombinerMenu(
        @Nullable MenuType<?> pMenuType, int pContainerId, Inventory pInventory, ContainerLevelAccess pAccess, ItemCombinerMenuSlotDefinition pSlotDefinition
    ) {
        super(pMenuType, pContainerId);
        this.access = pAccess;
        this.player = pInventory.player;
        this.inputSlots = this.createContainer(pSlotDefinition.getNumOfInputSlots());
        this.resultSlotIndex = pSlotDefinition.getResultSlotIndex();
        this.createInputSlots(pSlotDefinition);
        this.createResultSlot(pSlotDefinition);
        this.addStandardInventorySlots(pInventory, 8, 84);
    }

    private void createInputSlots(ItemCombinerMenuSlotDefinition pSlotDefinition) {
        for (final ItemCombinerMenuSlotDefinition.SlotDefinition itemcombinermenuslotdefinition$slotdefinition : pSlotDefinition.getSlots()) {
            this.addSlot(
                new Slot(
                    this.inputSlots,
                    itemcombinermenuslotdefinition$slotdefinition.slotIndex(),
                    itemcombinermenuslotdefinition$slotdefinition.x(),
                    itemcombinermenuslotdefinition$slotdefinition.y()
                ) {
                    @Override
                    public boolean mayPlace(ItemStack p_39818_) {
                        return itemcombinermenuslotdefinition$slotdefinition.mayPlace().test(p_39818_);
                    }
                }
            );
        }
    }

    private void createResultSlot(ItemCombinerMenuSlotDefinition pSlotDefinition) {
        this.addSlot(new Slot(this.resultSlots, pSlotDefinition.getResultSlot().slotIndex(), pSlotDefinition.getResultSlot().x(), pSlotDefinition.getResultSlot().y()) {
            /**
             * Check if the stack is allowed to be placed in this slot, used for armor slots as well as furnace fuel.
             */
            @Override
            public boolean mayPlace(ItemStack p_365170_) {
                return false;
            }

            /**
             * Return whether this slot's stack can be taken from this slot.
             */
            @Override
            public boolean mayPickup(Player p_361935_) {
                return ItemCombinerMenu.this.mayPickup(p_361935_, this.hasItem());
            }

            @Override
            public void onTake(Player p_365786_, ItemStack p_370147_) {
                ItemCombinerMenu.this.onTake(p_365786_, p_370147_);
            }
        });
    }

    public abstract void createResult();

    private SimpleContainer createContainer(int pSize) {
        return new SimpleContainer(pSize) {
            /**
             * For block entities, ensures the chunk containing the block entity is saved to disk later - the game won't
             * think it hasn't changed and skip it.
             */
            @Override
            public void setChanged() {
                super.setChanged();
                ItemCombinerMenu.this.slotsChanged(this);
            }
        };
    }

    @Override
    public void slotsChanged(Container pInventory) {
        super.slotsChanged(pInventory);
        if (pInventory == this.inputSlots) {
            this.createResult();
        }
    }

    @Override
    public void removed(Player pPlayer) {
        super.removed(pPlayer);
        this.access.execute((p_39796_, p_39797_) -> this.clearContainer(pPlayer, this.inputSlots));
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return this.access.evaluate((p_327088_, p_327089_) -> !this.isValidBlock(p_327088_.getBlockState(p_327089_)) ? false : pPlayer.canInteractWithBlock(p_327089_, 4.0), true);
    }

    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(pIndex);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            int i = this.getInventorySlotStart();
            int j = this.getUseRowEnd();
            if (pIndex == this.getResultSlot()) {
                if (!this.moveItemStackTo(itemstack1, i, j, true)) {
                    return ItemStack.EMPTY;
                }

                slot.onQuickCraft(itemstack1, itemstack);
            } else if (pIndex >= 0 && pIndex < this.getResultSlot()) {
                if (!this.moveItemStackTo(itemstack1, i, j, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (this.canMoveIntoInputSlots(itemstack1) && pIndex >= this.getInventorySlotStart() && pIndex < this.getUseRowEnd()) {
                if (!this.moveItemStackTo(itemstack1, 0, this.getResultSlot(), false)) {
                    return ItemStack.EMPTY;
                }
            } else if (pIndex >= this.getInventorySlotStart() && pIndex < this.getInventorySlotEnd()) {
                if (!this.moveItemStackTo(itemstack1, this.getUseRowStart(), this.getUseRowEnd(), false)) {
                    return ItemStack.EMPTY;
                }
            } else if (pIndex >= this.getUseRowStart() && pIndex < this.getUseRowEnd() && !this.moveItemStackTo(itemstack1, this.getInventorySlotStart(), this.getInventorySlotEnd(), false)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(pPlayer, itemstack1);
        }

        return itemstack;
    }

    protected boolean canMoveIntoInputSlots(ItemStack pStack) {
        return true;
    }

    public int getResultSlot() {
        return this.resultSlotIndex;
    }

    private int getInventorySlotStart() {
        return this.getResultSlot() + 1;
    }

    private int getInventorySlotEnd() {
        return this.getInventorySlotStart() + 27;
    }

    private int getUseRowStart() {
        return this.getInventorySlotEnd();
    }

    private int getUseRowEnd() {
        return this.getUseRowStart() + 9;
    }
}