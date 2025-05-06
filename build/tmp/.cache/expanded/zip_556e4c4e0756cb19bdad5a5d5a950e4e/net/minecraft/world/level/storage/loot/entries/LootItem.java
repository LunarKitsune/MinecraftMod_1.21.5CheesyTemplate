package net.minecraft.world.level.storage.loot.entries;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * A loot pool entry that always generates a given item.
 */
public class LootItem extends LootPoolSingletonContainer {
    public static final MapCodec<LootItem> CODEC = RecordCodecBuilder.mapCodec(
        p_360665_ -> p_360665_.group(Item.CODEC.fieldOf("name").forGetter(p_297028_ -> p_297028_.item))
            .and(singletonFields(p_360665_))
            .apply(p_360665_, LootItem::new)
    );
    private final Holder<Item> item;

    private LootItem(Holder<Item> pItem, int pWeight, int pQuality, List<LootItemCondition> pConditions, List<LootItemFunction> pFunctions) {
        super(pWeight, pQuality, pConditions, pFunctions);
        this.item = pItem;
    }

    @Override
    public LootPoolEntryType getType() {
        return LootPoolEntries.ITEM;
    }

    @Override
    public void createItemStack(Consumer<ItemStack> p_79590_, LootContext p_79591_) {
        p_79590_.accept(new ItemStack(this.item));
    }

    public static LootPoolSingletonContainer.Builder<?> lootTableItem(ItemLike pItem) {
        return simpleBuilder(
            (p_297030_, p_297031_, p_297032_, p_297033_) -> new LootItem(pItem.asItem().builtInRegistryHolder(), p_297030_, p_297031_, p_297032_, p_297033_)
        );
    }
}