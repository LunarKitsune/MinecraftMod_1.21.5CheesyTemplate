package net.minecraft.world.item.component;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public record UseRemainder(ItemStack convertInto) {
    public static final Codec<UseRemainder> CODEC = ItemStack.CODEC.xmap(UseRemainder::new, UseRemainder::convertInto);
    public static final StreamCodec<RegistryFriendlyByteBuf, UseRemainder> STREAM_CODEC = StreamCodec.composite(
        ItemStack.STREAM_CODEC, UseRemainder::convertInto, UseRemainder::new
    );

    public ItemStack convertIntoRemainder(ItemStack pStack, int pCount, boolean pHasInfiniteMaterials, UseRemainder.OnExtraCreatedRemainder pOnExtraCreated) {
        if (pHasInfiniteMaterials) {
            return pStack;
        } else if (pStack.getCount() >= pCount) {
            return pStack;
        } else {
            ItemStack itemstack = this.convertInto.copy();
            if (pStack.isEmpty()) {
                return itemstack;
            } else {
                pOnExtraCreated.apply(itemstack);
                return pStack;
            }
        }
    }

    @Override
    public boolean equals(Object pOther) {
        if (this == pOther) {
            return true;
        } else if (pOther != null && this.getClass() == pOther.getClass()) {
            UseRemainder useremainder = (UseRemainder)pOther;
            return ItemStack.matches(this.convertInto, useremainder.convertInto);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return ItemStack.hashItemAndComponents(this.convertInto);
    }

    @FunctionalInterface
    public interface OnExtraCreatedRemainder {
        void apply(ItemStack pStack);
    }
}