package net.minecraft.world.item.crafting.display;

import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public interface DisplayContentsFactory<T> {
    public interface ForRemainders<T> extends DisplayContentsFactory<T> {
        T addRemainder(T pRemainder, List<T> pRemainderItems);
    }

    public interface ForStacks<T> extends DisplayContentsFactory<T> {
        default T forStack(Holder<Item> pItem) {
            return this.forStack(new ItemStack(pItem));
        }

        default T forStack(Item pItem) {
            return this.forStack(new ItemStack(pItem));
        }

        T forStack(ItemStack pStack);
    }
}