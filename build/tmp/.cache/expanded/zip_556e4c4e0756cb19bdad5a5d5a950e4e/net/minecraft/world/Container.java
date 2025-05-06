package net.minecraft.world;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface Container extends Clearable, Iterable<ItemStack> {
    float DEFAULT_DISTANCE_BUFFER = 4.0F;

    int getContainerSize();

    boolean isEmpty();

    ItemStack getItem(int pSlot);

    ItemStack removeItem(int pSlot, int pAmount);

    ItemStack removeItemNoUpdate(int pSlot);

    void setItem(int pSlot, ItemStack pStack);

    default int getMaxStackSize() {
        return 99;
    }

    default int getMaxStackSize(ItemStack pStack) {
        return Math.min(this.getMaxStackSize(), pStack.getMaxStackSize());
    }

    void setChanged();

    boolean stillValid(Player pPlayer);

    default void startOpen(Player pPlayer) {
    }

    default void stopOpen(Player pPlayer) {
    }

    default boolean canPlaceItem(int pSlot, ItemStack pStack) {
        return true;
    }

    default boolean canTakeItem(Container pTarget, int pSlot, ItemStack pStack) {
        return true;
    }

    default int countItem(Item pItem) {
        int i = 0;

        for (ItemStack itemstack : this) {
            if (itemstack.getItem().equals(pItem)) {
                i += itemstack.getCount();
            }
        }

        return i;
    }

    default boolean hasAnyOf(Set<Item> pSet) {
        return this.hasAnyMatching(p_216873_ -> !p_216873_.isEmpty() && pSet.contains(p_216873_.getItem()));
    }

    default boolean hasAnyMatching(Predicate<ItemStack> pPredicate) {
        for (ItemStack itemstack : this) {
            if (pPredicate.test(itemstack)) {
                return true;
            }
        }

        return false;
    }

    static boolean stillValidBlockEntity(BlockEntity pBlockEntity, Player pPlayer) {
        return stillValidBlockEntity(pBlockEntity, pPlayer, 4.0F);
    }

    static boolean stillValidBlockEntity(BlockEntity pBlockEntity, Player pPlayer, float pDistance) {
        Level level = pBlockEntity.getLevel();
        BlockPos blockpos = pBlockEntity.getBlockPos();
        if (level == null) {
            return false;
        } else {
            return level.getBlockEntity(blockpos) != pBlockEntity ? false : pPlayer.canInteractWithBlock(blockpos, pDistance);
        }
    }

    @Override
    default Iterator<ItemStack> iterator() {
        return new Container.ContainerIterator(this);
    }

    public static class ContainerIterator implements Iterator<ItemStack> {
        private final Container container;
        private int index;
        private final int size;

        public ContainerIterator(Container p_396630_) {
            this.container = p_396630_;
            this.size = p_396630_.getContainerSize();
        }

        @Override
        public boolean hasNext() {
            return this.index < this.size;
        }

        public ItemStack next() {
            if (!this.hasNext()) {
                throw new NoSuchElementException();
            } else {
                return this.container.getItem(this.index++);
            }
        }
    }
}