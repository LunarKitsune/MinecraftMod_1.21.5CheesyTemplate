package net.minecraft.world.entity.npc;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

public interface InventoryCarrier {
    String TAG_INVENTORY = "Inventory";

    SimpleContainer getInventory();

    static void pickUpItem(ServerLevel pLevel, Mob pMob, InventoryCarrier pCarrier, ItemEntity pItemEntity) {
        ItemStack itemstack = pItemEntity.getItem();
        if (pMob.wantsToPickUp(pLevel, itemstack)) {
            SimpleContainer simplecontainer = pCarrier.getInventory();
            boolean flag = simplecontainer.canAddItem(itemstack);
            if (!flag) {
                return;
            }

            pMob.onItemPickup(pItemEntity);
            int i = itemstack.getCount();
            ItemStack itemstack1 = simplecontainer.addItem(itemstack);
            pMob.take(pItemEntity, i - itemstack1.getCount());
            if (itemstack1.isEmpty()) {
                pItemEntity.discard();
            } else {
                itemstack.setCount(itemstack1.getCount());
            }
        }
    }

    default void readInventoryFromTag(CompoundTag pTag, HolderLookup.Provider pLevelRegistry) {
        pTag.getList("Inventory").ifPresent(p_394888_ -> this.getInventory().fromTag(p_394888_, pLevelRegistry));
    }

    default void writeInventoryToTag(CompoundTag pTag, HolderLookup.Provider pLevelRegistry) {
        pTag.put("Inventory", this.getInventory().createTag(pLevelRegistry));
    }
}