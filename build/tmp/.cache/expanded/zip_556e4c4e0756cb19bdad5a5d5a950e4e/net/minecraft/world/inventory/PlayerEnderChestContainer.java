package net.minecraft.world.inventory;

import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;

public class PlayerEnderChestContainer extends SimpleContainer {
    @Nullable
    private EnderChestBlockEntity activeChest;

    public PlayerEnderChestContainer() {
        super(27);
    }

    public void setActiveChest(EnderChestBlockEntity pEnderChestBlockEntity) {
        this.activeChest = pEnderChestBlockEntity;
    }

    public boolean isActiveChest(EnderChestBlockEntity pEnderChest) {
        return this.activeChest == pEnderChest;
    }

    @Override
    public void fromTag(ListTag p_40108_, HolderLookup.Provider p_333103_) {
        for (int i = 0; i < this.getContainerSize(); i++) {
            this.setItem(i, ItemStack.EMPTY);
        }

        for (int k = 0; k < p_40108_.size(); k++) {
            CompoundTag compoundtag = p_40108_.getCompoundOrEmpty(k);
            int j = compoundtag.getByteOr("Slot", (byte)0) & 255;
            if (j >= 0 && j < this.getContainerSize()) {
                this.setItem(j, ItemStack.parse(p_333103_, compoundtag).orElse(ItemStack.EMPTY));
            }
        }
    }

    @Override
    public ListTag createTag(HolderLookup.Provider p_335225_) {
        ListTag listtag = new ListTag();

        for (int i = 0; i < this.getContainerSize(); i++) {
            ItemStack itemstack = this.getItem(i);
            if (!itemstack.isEmpty()) {
                CompoundTag compoundtag = new CompoundTag();
                compoundtag.putByte("Slot", (byte)i);
                listtag.add(itemstack.save(p_335225_, compoundtag));
            }
        }

        return listtag;
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return this.activeChest != null && !this.activeChest.stillValid(pPlayer) ? false : super.stillValid(pPlayer);
    }

    @Override
    public void startOpen(Player pPlayer) {
        if (this.activeChest != null) {
            this.activeChest.startOpen(pPlayer);
        }

        super.startOpen(pPlayer);
    }

    @Override
    public void stopOpen(Player pPlayer) {
        if (this.activeChest != null) {
            this.activeChest.stopOpen(pPlayer);
        }

        super.stopOpen(pPlayer);
        this.activeChest = null;
    }
}