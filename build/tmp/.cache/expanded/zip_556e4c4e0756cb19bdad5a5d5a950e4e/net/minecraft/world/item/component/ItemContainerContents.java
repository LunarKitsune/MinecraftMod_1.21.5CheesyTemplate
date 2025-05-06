package net.minecraft.world.item.component;

import com.google.common.collect.Iterables;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.Consumer;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public final class ItemContainerContents implements TooltipProvider {
    private static final int NO_SLOT = -1;
    private static final int MAX_SIZE = 256;
    public static final ItemContainerContents EMPTY = new ItemContainerContents(NonNullList.create());
    public static final Codec<ItemContainerContents> CODEC = ItemContainerContents.Slot.CODEC
        .sizeLimitedListOf(256)
        .xmap(ItemContainerContents::fromSlots, ItemContainerContents::asSlots);
    public static final StreamCodec<RegistryFriendlyByteBuf, ItemContainerContents> STREAM_CODEC = ItemStack.OPTIONAL_STREAM_CODEC
        .apply(ByteBufCodecs.list(256))
        .map(ItemContainerContents::new, p_333580_ -> p_333580_.items);
    private final NonNullList<ItemStack> items;
    private final int hashCode;

    private ItemContainerContents(NonNullList<ItemStack> pItems) {
        if (pItems.size() > 256) {
            throw new IllegalArgumentException("Got " + pItems.size() + " items, but maximum is 256");
        } else {
            this.items = pItems;
            this.hashCode = ItemStack.hashStackList(pItems);
        }
    }

    private ItemContainerContents(int pSize) {
        this(NonNullList.withSize(pSize, ItemStack.EMPTY));
    }

    private ItemContainerContents(List<ItemStack> pItems) {
        this(pItems.size());

        for (int i = 0; i < pItems.size(); i++) {
            this.items.set(i, pItems.get(i));
        }
    }

    private static ItemContainerContents fromSlots(List<ItemContainerContents.Slot> pSlots) {
        OptionalInt optionalint = pSlots.stream().mapToInt(ItemContainerContents.Slot::index).max();
        if (optionalint.isEmpty()) {
            return EMPTY;
        } else {
            ItemContainerContents itemcontainercontents = new ItemContainerContents(optionalint.getAsInt() + 1);

            for (ItemContainerContents.Slot itemcontainercontents$slot : pSlots) {
                itemcontainercontents.items.set(itemcontainercontents$slot.index(), itemcontainercontents$slot.item());
            }

            return itemcontainercontents;
        }
    }

    public static ItemContainerContents fromItems(List<ItemStack> pItems) {
        int i = findLastNonEmptySlot(pItems);
        if (i == -1) {
            return EMPTY;
        } else {
            ItemContainerContents itemcontainercontents = new ItemContainerContents(i + 1);

            for (int j = 0; j <= i; j++) {
                itemcontainercontents.items.set(j, pItems.get(j).copy());
            }

            return itemcontainercontents;
        }
    }

    private static int findLastNonEmptySlot(List<ItemStack> pItems) {
        for (int i = pItems.size() - 1; i >= 0; i--) {
            if (!pItems.get(i).isEmpty()) {
                return i;
            }
        }

        return -1;
    }

    private List<ItemContainerContents.Slot> asSlots() {
        List<ItemContainerContents.Slot> list = new ArrayList<>();

        for (int i = 0; i < this.items.size(); i++) {
            ItemStack itemstack = this.items.get(i);
            if (!itemstack.isEmpty()) {
                list.add(new ItemContainerContents.Slot(i, itemstack));
            }
        }

        return list;
    }

    public void copyInto(NonNullList<ItemStack> pList) {
        for (int i = 0; i < pList.size(); i++) {
            ItemStack itemstack = i < this.items.size() ? this.items.get(i) : ItemStack.EMPTY;
            pList.set(i, itemstack.copy());
        }
    }

    public ItemStack copyOne() {
        return this.items.isEmpty() ? ItemStack.EMPTY : this.items.get(0).copy();
    }

    public Stream<ItemStack> stream() {
        return this.items.stream().map(ItemStack::copy);
    }

    public Stream<ItemStack> nonEmptyStream() {
        return this.items.stream().filter(p_332163_ -> !p_332163_.isEmpty()).map(ItemStack::copy);
    }

    public Iterable<ItemStack> nonEmptyItems() {
        return Iterables.filter(this.items, p_330818_ -> !p_330818_.isEmpty());
    }

    public Iterable<ItemStack> nonEmptyItemsCopy() {
        return Iterables.transform(this.nonEmptyItems(), ItemStack::copy);
    }

    @Override
    public boolean equals(Object pOther) {
        return this == pOther
            ? true
            : pOther instanceof ItemContainerContents itemcontainercontents && ItemStack.listMatches(this.items, itemcontainercontents.items);
    }

    @Override
    public int hashCode() {
        return this.hashCode;
    }

    @Override
    public void addToTooltip(Item.TooltipContext p_391555_, Consumer<Component> p_397087_, TooltipFlag p_395634_, DataComponentGetter p_398037_) {
        int i = 0;
        int j = 0;

        for (ItemStack itemstack : this.nonEmptyItems()) {
            j++;
            if (i <= 4) {
                i++;
                p_397087_.accept(Component.translatable("item.container.item_count", itemstack.getHoverName(), itemstack.getCount()));
            }
        }

        if (j - i > 0) {
            p_397087_.accept(Component.translatable("item.container.more_items", j - i).withStyle(ChatFormatting.ITALIC));
        }
    }

    record Slot(int index, ItemStack item) {
        public static final Codec<ItemContainerContents.Slot> CODEC = RecordCodecBuilder.create(
            p_327964_ -> p_327964_.group(
                    Codec.intRange(0, 255).fieldOf("slot").forGetter(ItemContainerContents.Slot::index),
                    ItemStack.CODEC.fieldOf("item").forGetter(ItemContainerContents.Slot::item)
                )
                .apply(p_327964_, ItemContainerContents.Slot::new)
        );
    }
}