package net.minecraft.client.gui;

import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface ItemSlotMouseAction {
    boolean matches(Slot pSlot);

    boolean onMouseScrolled(double pXOffset, double pYOffset, int pHoveredSlotIndex, ItemStack pHoveredSlotItem);

    void onStopHovering(Slot pSlot);

    void onSlotClicked(Slot pSlot, ClickType pClickType);
}