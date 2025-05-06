package net.minecraft.world.inventory;

import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

public class CraftingMenu extends AbstractCraftingMenu {
    private static final int CRAFTING_GRID_WIDTH = 3;
    private static final int CRAFTING_GRID_HEIGHT = 3;
    public static final int RESULT_SLOT = 0;
    private static final int CRAFT_SLOT_START = 1;
    private static final int CRAFT_SLOT_COUNT = 9;
    private static final int CRAFT_SLOT_END = 10;
    private static final int INV_SLOT_START = 10;
    private static final int INV_SLOT_END = 37;
    private static final int USE_ROW_SLOT_START = 37;
    private static final int USE_ROW_SLOT_END = 46;
    private final ContainerLevelAccess access;
    private final Player player;
    private boolean placingRecipe;

    public CraftingMenu(int pContainerId, Inventory pPlayerInventory) {
        this(pContainerId, pPlayerInventory, ContainerLevelAccess.NULL);
    }

    public CraftingMenu(int pContainerId, Inventory pPlayerInventory, ContainerLevelAccess pAccess) {
        super(MenuType.CRAFTING, pContainerId, 3, 3);
        this.access = pAccess;
        this.player = pPlayerInventory.player;
        this.addResultSlot(this.player, 124, 35);
        this.addCraftingGridSlots(30, 17);
        this.addStandardInventorySlots(pPlayerInventory, 8, 84);
    }

    protected static void slotChangedCraftingGrid(
        AbstractContainerMenu pMenu,
        ServerLevel pLevel,
        Player pPlayer,
        CraftingContainer pCraftSlots,
        ResultContainer pResultSlots,
        @Nullable RecipeHolder<CraftingRecipe> pRecipe
    ) {
        CraftingInput craftinginput = pCraftSlots.asCraftInput();
        ServerPlayer serverplayer = (ServerPlayer)pPlayer;
        ItemStack itemstack = ItemStack.EMPTY;
        Optional<RecipeHolder<CraftingRecipe>> optional = pLevel.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, craftinginput, pLevel, pRecipe);
        if (optional.isPresent()) {
            RecipeHolder<CraftingRecipe> recipeholder = optional.get();
            CraftingRecipe craftingrecipe = recipeholder.value();
            if (pResultSlots.setRecipeUsed(serverplayer, recipeholder)) {
                ItemStack itemstack1 = craftingrecipe.assemble(craftinginput, pLevel.registryAccess());
                if (itemstack1.isItemEnabled(pLevel.enabledFeatures())) {
                    itemstack = itemstack1;
                }
            }
        }

        pResultSlots.setItem(0, itemstack);
        pMenu.setRemoteSlot(0, itemstack);
        serverplayer.connection.send(new ClientboundContainerSetSlotPacket(pMenu.containerId, pMenu.incrementStateId(), 0, itemstack));
    }

    @Override
    public void slotsChanged(Container pInventory) {
        if (!this.placingRecipe) {
            this.access.execute((p_359372_, p_359373_) -> {
                if (p_359372_ instanceof ServerLevel serverlevel) {
                    slotChangedCraftingGrid(this, serverlevel, this.player, this.craftSlots, this.resultSlots, null);
                }
            });
        }
    }

    @Override
    public void beginPlacingRecipe() {
        this.placingRecipe = true;
    }

    @Override
    public void finishPlacingRecipe(ServerLevel p_368369_, RecipeHolder<CraftingRecipe> p_342309_) {
        this.placingRecipe = false;
        slotChangedCraftingGrid(this, p_368369_, this.player, this.craftSlots, this.resultSlots, p_342309_);
    }

    @Override
    public void removed(Player pPlayer) {
        super.removed(pPlayer);
        this.access.execute((p_39371_, p_39372_) -> this.clearContainer(pPlayer, this.craftSlots));
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return stillValid(this.access, pPlayer, Blocks.CRAFTING_TABLE);
    }

    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(pIndex);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (pIndex == 0) {
                itemstack1.getItem().onCraftedBy(itemstack1, pPlayer);
                if (!this.moveItemStackTo(itemstack1, 10, 46, true)) {
                    return ItemStack.EMPTY;
                }

                slot.onQuickCraft(itemstack1, itemstack);
            } else if (pIndex >= 10 && pIndex < 46) {
                if (!this.moveItemStackTo(itemstack1, 1, 10, false)) {
                    if (pIndex < 37) {
                        if (!this.moveItemStackTo(itemstack1, 37, 46, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (!this.moveItemStackTo(itemstack1, 10, 37, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            } else if (!this.moveItemStackTo(itemstack1, 10, 46, false)) {
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
            if (pIndex == 0) {
                pPlayer.drop(itemstack1, false);
            }
        }

        return itemstack;
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack pStack, Slot pSlot) {
        return pSlot.container != this.resultSlots && super.canTakeItemForPickAll(pStack, pSlot);
    }

    @Override
    public Slot getResultSlot() {
        return this.slots.get(0);
    }

    @Override
    public List<Slot> getInputGridSlots() {
        return this.slots.subList(1, 10);
    }

    @Override
    public RecipeBookType getRecipeBookType() {
        return RecipeBookType.CRAFTING;
    }

    @Override
    protected Player owner() {
        return this.player;
    }
}