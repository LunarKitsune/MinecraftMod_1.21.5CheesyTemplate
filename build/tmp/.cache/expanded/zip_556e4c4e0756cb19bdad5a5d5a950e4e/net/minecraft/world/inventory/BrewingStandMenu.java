package net.minecraft.world.inventory;

import java.util.Optional;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionContents;

public class BrewingStandMenu extends AbstractContainerMenu {
    static final ResourceLocation EMPTY_SLOT_FUEL = ResourceLocation.withDefaultNamespace("container/slot/brewing_fuel");
    static final ResourceLocation EMPTY_SLOT_POTION = ResourceLocation.withDefaultNamespace("container/slot/potion");
    private static final int BOTTLE_SLOT_START = 0;
    private static final int BOTTLE_SLOT_END = 2;
    private static final int INGREDIENT_SLOT = 3;
    private static final int FUEL_SLOT = 4;
    private static final int SLOT_COUNT = 5;
    private static final int DATA_COUNT = 2;
    private static final int INV_SLOT_START = 5;
    private static final int INV_SLOT_END = 32;
    private static final int USE_ROW_SLOT_START = 32;
    private static final int USE_ROW_SLOT_END = 41;
    private final Container brewingStand;
    private final ContainerData brewingStandData;
    private final Slot ingredientSlot;

    public BrewingStandMenu(int pContainerId, Inventory pPlayerInventory) {
        this(pContainerId, pPlayerInventory, new SimpleContainer(5), new SimpleContainerData(2));
    }

    public BrewingStandMenu(int pContainerId, Inventory pPlayerInventory, Container pBrewingStandContainer, ContainerData pBrewingStandData) {
        super(MenuType.BREWING_STAND, pContainerId);
        checkContainerSize(pBrewingStandContainer, 5);
        checkContainerDataCount(pBrewingStandData, 2);
        this.brewingStand = pBrewingStandContainer;
        this.brewingStandData = pBrewingStandData;
        PotionBrewing potionbrewing = pPlayerInventory.player.level().potionBrewing();
        this.addSlot(new BrewingStandMenu.PotionSlot(pBrewingStandContainer, 0, 56, 51, potionbrewing));
        this.addSlot(new BrewingStandMenu.PotionSlot(pBrewingStandContainer, 1, 79, 58, potionbrewing));
        this.addSlot(new BrewingStandMenu.PotionSlot(pBrewingStandContainer, 2, 102, 51, potionbrewing));
        this.ingredientSlot = this.addSlot(new BrewingStandMenu.IngredientsSlot(potionbrewing, pBrewingStandContainer, 3, 79, 17));
        this.addSlot(new BrewingStandMenu.FuelSlot(pBrewingStandContainer, 4, 17, 17));
        this.addDataSlots(pBrewingStandData);
        this.addStandardInventorySlots(pPlayerInventory, 8, 84);
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return this.brewingStand.stillValid(pPlayer);
    }

    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(pIndex);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if ((pIndex < 0 || pIndex > 2) && pIndex != 3 && pIndex != 4) {
                if (BrewingStandMenu.FuelSlot.mayPlaceItem(itemstack)) {
                    if (this.moveItemStackTo(itemstack1, 4, 5, false) || this.ingredientSlot.mayPlace(itemstack1) && !this.moveItemStackTo(itemstack1, 3, 4, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (this.ingredientSlot.mayPlace(itemstack1)) {
                    if (!this.moveItemStackTo(itemstack1, 3, 4, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (this.slots.get(0).mayPlace(itemstack)) { // Forge: Use the slot's place which goes through custom recipes.
                    if (!this.moveItemStackTo(itemstack1, 0, 3, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (pIndex >= 5 && pIndex < 32) {
                    if (!this.moveItemStackTo(itemstack1, 32, 41, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (pIndex >= 32 && pIndex < 41) {
                    if (!this.moveItemStackTo(itemstack1, 5, 32, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(itemstack1, 5, 41, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!this.moveItemStackTo(itemstack1, 5, 41, true)) {
                    return ItemStack.EMPTY;
                }

                slot.onQuickCraft(itemstack1, itemstack);
            }

            if (itemstack1.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(pPlayer, itemstack);
        }

        return itemstack;
    }

    public int getFuel() {
        return this.brewingStandData.get(1);
    }

    public int getBrewingTicks() {
        return this.brewingStandData.get(0);
    }

    static class FuelSlot extends Slot {
        public FuelSlot(Container p_39105_, int p_39106_, int p_39107_, int p_39108_) {
            super(p_39105_, p_39106_, p_39107_, p_39108_);
        }

        @Override
        public boolean mayPlace(ItemStack pStack) {
            return mayPlaceItem(pStack);
        }

        public static boolean mayPlaceItem(ItemStack pItemStack) {
            return pItemStack.is(ItemTags.BREWING_FUEL);
        }

        @Override
        public ResourceLocation getNoItemIcon() {
            return BrewingStandMenu.EMPTY_SLOT_FUEL;
        }
    }

    static class IngredientsSlot extends Slot {
        private final PotionBrewing potionBrewing;

        public IngredientsSlot(PotionBrewing pPotionBrewing, Container pContainer, int pSlot, int pX, int pY) {
            super(pContainer, pSlot, pX, pY);
            this.potionBrewing = pPotionBrewing;
        }

        @Override
        public boolean mayPlace(ItemStack pStack) {
            return this.potionBrewing.isIngredient(pStack);
        }
    }

    public static class PotionSlot extends Slot {
        private final PotionBrewing potionBrewing;

        /** @deprecated Use the one that specifes a PotionBrewing */
        public PotionSlot(Container p_39123_, int p_39124_, int p_39125_, int p_39126_) {
            this(p_39123_, p_39124_, p_39125_, p_39126_, null);
        }

        public PotionSlot(Container p_39123_, int p_39124_, int p_39125_, int p_39126_, PotionBrewing potionBrewing) {
            super(p_39123_, p_39124_, p_39125_, p_39126_);
            this.potionBrewing = potionBrewing;
        }

        @Override
        public boolean mayPlace(ItemStack pStack) {
            if (this.potionBrewing != null) {
                return this.potionBrewing.isValidInput(pStack);
            }
            return mayPlaceItem(pStack);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public void onTake(Player p_150499_, ItemStack p_150500_) {
            Optional<Holder<Potion>> optional = p_150500_.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY).potion();
            if (optional.isPresent() && p_150499_ instanceof ServerPlayer serverplayer) {
                net.minecraftforge.event.ForgeEventFactory.onPlayerBrewedPotion(p_150499_, p_150500_);
                CriteriaTriggers.BREWED_POTION.trigger(serverplayer, optional.get());
            }

            super.onTake(p_150499_, p_150500_);
        }

        public static boolean mayPlaceItem(ItemStack pStack) {
            return pStack.is(Items.POTION)
                || pStack.is(Items.SPLASH_POTION)
                || pStack.is(Items.LINGERING_POTION)
                || pStack.is(Items.GLASS_BOTTLE);
        }

        @Override
        public ResourceLocation getNoItemIcon() {
            return BrewingStandMenu.EMPTY_SLOT_POTION;
        }
    }
}
