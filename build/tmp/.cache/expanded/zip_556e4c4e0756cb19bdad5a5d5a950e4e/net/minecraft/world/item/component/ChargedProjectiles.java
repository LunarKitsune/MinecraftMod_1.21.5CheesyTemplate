package net.minecraft.world.item.component;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public final class ChargedProjectiles implements TooltipProvider {
    public static final ChargedProjectiles EMPTY = new ChargedProjectiles(List.of());
    public static final Codec<ChargedProjectiles> CODEC = ItemStack.CODEC.listOf().xmap(ChargedProjectiles::new, p_333238_ -> p_333238_.items);
    public static final StreamCodec<RegistryFriendlyByteBuf, ChargedProjectiles> STREAM_CODEC = ItemStack.STREAM_CODEC
        .apply(ByteBufCodecs.list())
        .map(ChargedProjectiles::new, p_330449_ -> p_330449_.items);
    private final List<ItemStack> items;

    private ChargedProjectiles(List<ItemStack> pItems) {
        this.items = pItems;
    }

    public static ChargedProjectiles of(ItemStack pStack) {
        return new ChargedProjectiles(List.of(pStack.copy()));
    }

    public static ChargedProjectiles of(List<ItemStack> pStack) {
        return new ChargedProjectiles(List.copyOf(Lists.transform(pStack, ItemStack::copy)));
    }

    public boolean contains(Item pItem) {
        for (ItemStack itemstack : this.items) {
            if (itemstack.is(pItem)) {
                return true;
            }
        }

        return false;
    }

    public List<ItemStack> getItems() {
        return Lists.transform(this.items, ItemStack::copy);
    }

    public boolean isEmpty() {
        return this.items.isEmpty();
    }

    @Override
    public boolean equals(Object pOther) {
        return this == pOther
            ? true
            : pOther instanceof ChargedProjectiles chargedprojectiles && ItemStack.listMatches(this.items, chargedprojectiles.items);
    }

    @Override
    public int hashCode() {
        return ItemStack.hashStackList(this.items);
    }

    @Override
    public String toString() {
        return "ChargedProjectiles[items=" + this.items + "]";
    }

    @Override
    public void addToTooltip(Item.TooltipContext p_391340_, Consumer<Component> p_393178_, TooltipFlag p_392958_, DataComponentGetter p_396521_) {
        ItemStack itemstack = null;
        int i = 0;

        for (ItemStack itemstack1 : this.items) {
            if (itemstack == null) {
                itemstack = itemstack1;
                i = 1;
            } else if (ItemStack.matches(itemstack, itemstack1)) {
                i++;
            } else {
                addProjectileTooltip(p_391340_, p_393178_, itemstack, i);
                itemstack = itemstack1;
                i = 1;
            }
        }

        if (itemstack != null) {
            addProjectileTooltip(p_391340_, p_393178_, itemstack, i);
        }
    }

    private static void addProjectileTooltip(Item.TooltipContext pContext, Consumer<Component> pTooltipAdder, ItemStack pStack, int pCount) {
        if (pCount == 1) {
            pTooltipAdder.accept(Component.translatable("item.minecraft.crossbow.projectile.single", pStack.getDisplayName()));
        } else {
            pTooltipAdder.accept(Component.translatable("item.minecraft.crossbow.projectile.multiple", pCount, pStack.getDisplayName()));
        }

        TooltipDisplay tooltipdisplay = pStack.getOrDefault(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT);
        pStack.addDetailsToTooltip(
            pContext,
            tooltipdisplay,
            null,
            TooltipFlag.NORMAL,
            p_390820_ -> pTooltipAdder.accept(Component.literal("  ").append(p_390820_).withStyle(ChatFormatting.GRAY))
        );
    }
}