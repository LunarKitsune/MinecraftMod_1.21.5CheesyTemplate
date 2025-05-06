package net.minecraft.world.item.component;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import org.apache.commons.lang3.math.Fraction;

public final class BundleContents implements TooltipComponent {
    public static final BundleContents EMPTY = new BundleContents(List.of());
    public static final Codec<BundleContents> CODEC = ItemStack.CODEC
        .listOf()
        .flatXmap(BundleContents::checkAndCreate, p_359809_ -> DataResult.success(p_359809_.items));
    public static final StreamCodec<RegistryFriendlyByteBuf, BundleContents> STREAM_CODEC = ItemStack.STREAM_CODEC
        .apply(ByteBufCodecs.list())
        .map(BundleContents::new, p_332949_ -> p_332949_.items);
    private static final Fraction BUNDLE_IN_BUNDLE_WEIGHT = Fraction.getFraction(1, 16);
    private static final int NO_STACK_INDEX = -1;
    public static final int NO_SELECTED_ITEM_INDEX = -1;
    final List<ItemStack> items;
    final Fraction weight;
    final int selectedItem;

    BundleContents(List<ItemStack> pItems, Fraction pWeight, int pSelectedItem) {
        this.items = pItems;
        this.weight = pWeight;
        this.selectedItem = pSelectedItem;
    }

    private static DataResult<BundleContents> checkAndCreate(List<ItemStack> pItems) {
        try {
            Fraction fraction = computeContentWeight(pItems);
            return DataResult.success(new BundleContents(pItems, fraction, -1));
        } catch (ArithmeticException arithmeticexception) {
            return DataResult.error(() -> "Excessive total bundle weight");
        }
    }

    public BundleContents(List<ItemStack> pItems) {
        this(pItems, computeContentWeight(pItems), -1);
    }

    private static Fraction computeContentWeight(List<ItemStack> pContent) {
        Fraction fraction = Fraction.ZERO;

        for (ItemStack itemstack : pContent) {
            fraction = fraction.add(getWeight(itemstack).multiplyBy(Fraction.getFraction(itemstack.getCount(), 1)));
        }

        return fraction;
    }

    static Fraction getWeight(ItemStack pStack) {
        BundleContents bundlecontents = pStack.get(DataComponents.BUNDLE_CONTENTS);
        if (bundlecontents != null) {
            return BUNDLE_IN_BUNDLE_WEIGHT.add(bundlecontents.weight());
        } else {
            List<BeehiveBlockEntity.Occupant> list = pStack.getOrDefault(DataComponents.BEES, Bees.EMPTY).bees();
            return !list.isEmpty() ? Fraction.ONE : Fraction.getFraction(1, pStack.getMaxStackSize());
        }
    }

    public static boolean canItemBeInBundle(ItemStack pStack) {
        return !pStack.isEmpty() && pStack.getItem().canFitInsideContainerItems();
    }

    public int getNumberOfItemsToShow() {
        int i = this.size();
        int j = i > 12 ? 11 : 12;
        int k = i % 4;
        int l = k == 0 ? 0 : 4 - k;
        return Math.min(i, j - l);
    }

    public ItemStack getItemUnsafe(int pIndex) {
        return this.items.get(pIndex);
    }

    public Stream<ItemStack> itemCopyStream() {
        return this.items.stream().map(ItemStack::copy);
    }

    public Iterable<ItemStack> items() {
        return this.items;
    }

    public Iterable<ItemStack> itemsCopy() {
        return Lists.transform(this.items, ItemStack::copy);
    }

    public int size() {
        return this.items.size();
    }

    public Fraction weight() {
        return this.weight;
    }

    public boolean isEmpty() {
        return this.items.isEmpty();
    }

    public int getSelectedItem() {
        return this.selectedItem;
    }

    public boolean hasSelectedItem() {
        return this.selectedItem != -1;
    }

    @Override
    public boolean equals(Object pOther) {
        if (this == pOther) {
            return true;
        } else {
            return !(pOther instanceof BundleContents bundlecontents)
                ? false
                : this.weight.equals(bundlecontents.weight) && ItemStack.listMatches(this.items, bundlecontents.items);
        }
    }

    @Override
    public int hashCode() {
        return ItemStack.hashStackList(this.items);
    }

    @Override
    public String toString() {
        return "BundleContents" + this.items;
    }

    public static class Mutable {
        private final List<ItemStack> items;
        private Fraction weight;
        private int selectedItem;

        public Mutable(BundleContents pContents) {
            this.items = new ArrayList<>(pContents.items);
            this.weight = pContents.weight;
            this.selectedItem = pContents.selectedItem;
        }

        public BundleContents.Mutable clearItems() {
            this.items.clear();
            this.weight = Fraction.ZERO;
            this.selectedItem = -1;
            return this;
        }

        private int findStackIndex(ItemStack pStack) {
            if (!pStack.isStackable()) {
                return -1;
            } else {
                for (int i = 0; i < this.items.size(); i++) {
                    if (ItemStack.isSameItemSameComponents(this.items.get(i), pStack)) {
                        return i;
                    }
                }

                return -1;
            }
        }

        private int getMaxAmountToAdd(ItemStack pStack) {
            Fraction fraction = Fraction.ONE.subtract(this.weight);
            return Math.max(fraction.divideBy(BundleContents.getWeight(pStack)).intValue(), 0);
        }

        public int tryInsert(ItemStack pStack) {
            if (!BundleContents.canItemBeInBundle(pStack)) {
                return 0;
            } else {
                int i = Math.min(pStack.getCount(), this.getMaxAmountToAdd(pStack));
                if (i == 0) {
                    return 0;
                } else {
                    this.weight = this.weight.add(BundleContents.getWeight(pStack).multiplyBy(Fraction.getFraction(i, 1)));
                    int j = this.findStackIndex(pStack);
                    if (j != -1) {
                        ItemStack itemstack = this.items.remove(j);
                        ItemStack itemstack1 = itemstack.copyWithCount(itemstack.getCount() + i);
                        pStack.shrink(i);
                        this.items.add(0, itemstack1);
                    } else {
                        this.items.add(0, pStack.split(i));
                    }

                    return i;
                }
            }
        }

        public int tryTransfer(Slot pSlot, Player pPlayer) {
            ItemStack itemstack = pSlot.getItem();
            int i = this.getMaxAmountToAdd(itemstack);
            return BundleContents.canItemBeInBundle(itemstack) ? this.tryInsert(pSlot.safeTake(itemstack.getCount(), i, pPlayer)) : 0;
        }

        public void toggleSelectedItem(int pSelectedItem) {
            this.selectedItem = this.selectedItem != pSelectedItem && !this.indexIsOutsideAllowedBounds(pSelectedItem) ? pSelectedItem : -1;
        }

        private boolean indexIsOutsideAllowedBounds(int pIndex) {
            return pIndex < 0 || pIndex >= this.items.size();
        }

        @Nullable
        public ItemStack removeOne() {
            if (this.items.isEmpty()) {
                return null;
            } else {
                int i = this.indexIsOutsideAllowedBounds(this.selectedItem) ? 0 : this.selectedItem;
                ItemStack itemstack = this.items.remove(i).copy();
                this.weight = this.weight.subtract(BundleContents.getWeight(itemstack).multiplyBy(Fraction.getFraction(itemstack.getCount(), 1)));
                this.toggleSelectedItem(-1);
                return itemstack;
            }
        }

        public Fraction weight() {
            return this.weight;
        }

        public BundleContents toImmutable() {
            return new BundleContents(List.copyOf(this.items), this.weight, this.selectedItem);
        }
    }
}