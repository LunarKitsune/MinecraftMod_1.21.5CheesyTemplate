package net.minecraft.world.inventory;

import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

class ArmorSlot extends Slot {
    private final LivingEntity owner;
    private final EquipmentSlot slot;
    @Nullable
    private final ResourceLocation emptyIcon;

    public ArmorSlot(
        Container pContainer, LivingEntity pOwner, EquipmentSlot pSlot, int pSlotIndex, int pX, int pY, @Nullable ResourceLocation pEmptyIcon
    ) {
        super(pContainer, pSlotIndex, pX, pY);
        this.owner = pOwner;
        this.slot = pSlot;
        this.emptyIcon = pEmptyIcon;
    }

    @Override
    public void setByPlayer(ItemStack p_342337_, ItemStack p_345204_) {
        this.owner.onEquipItem(this.slot, p_345204_, p_342337_);
        super.setByPlayer(p_342337_, p_345204_);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean mayPlace(ItemStack p_344267_) {
        return p_344267_.canEquip(slot, owner);
    }

    @Override
    public boolean isActive() {
        return this.owner.canUseSlot(this.slot);
    }

    @Override
    public boolean mayPickup(Player p_344552_) {
        ItemStack itemstack = this.getItem();
        return !itemstack.isEmpty() && !p_344552_.isCreative() && EnchantmentHelper.has(itemstack, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)
            ? false
            : super.mayPickup(p_344552_);
    }

    @Nullable
    @Override
    public ResourceLocation getNoItemIcon() {
        return this.emptyIcon;
    }
}
