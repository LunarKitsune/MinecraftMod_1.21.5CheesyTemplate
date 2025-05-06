package net.minecraft.world.inventory;

import java.util.List;
import net.minecraft.world.item.ItemStack;

public interface ContainerSynchronizer {
    void sendInitialData(AbstractContainerMenu pContainer, List<ItemStack> pItems, ItemStack pCarried, int[] pRemoteDataSlots);

    void sendSlotChange(AbstractContainerMenu pContainer, int pSlot, ItemStack pItemStack);

    void sendCarriedChange(AbstractContainerMenu pContainerMenu, ItemStack pStack);

    void sendDataChange(AbstractContainerMenu pContainer, int pId, int pValue);

    RemoteSlot createSlot();
}