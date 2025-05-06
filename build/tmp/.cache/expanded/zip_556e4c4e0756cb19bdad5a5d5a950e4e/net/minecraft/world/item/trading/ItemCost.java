package net.minecraft.world.item.trading;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Optional;
import java.util.function.UnaryOperator;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentExactPredicate;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public record ItemCost(Holder<Item> item, int count, DataComponentExactPredicate components, ItemStack itemStack) {
    public static final Codec<ItemCost> CODEC = RecordCodecBuilder.create(
        p_390864_ -> p_390864_.group(
                Item.CODEC.fieldOf("id").forGetter(ItemCost::item),
                ExtraCodecs.POSITIVE_INT.fieldOf("count").orElse(1).forGetter(ItemCost::count),
                DataComponentExactPredicate.CODEC.optionalFieldOf("components", DataComponentExactPredicate.EMPTY).forGetter(ItemCost::components)
            )
            .apply(p_390864_, ItemCost::new)
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ItemCost> STREAM_CODEC = StreamCodec.composite(
        Item.STREAM_CODEC,
        ItemCost::item,
        ByteBufCodecs.VAR_INT,
        ItemCost::count,
        DataComponentExactPredicate.STREAM_CODEC,
        ItemCost::components,
        ItemCost::new
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, Optional<ItemCost>> OPTIONAL_STREAM_CODEC = STREAM_CODEC.apply(ByteBufCodecs::optional);

    public ItemCost(ItemLike pItem) {
        this(pItem, 1);
    }

    public ItemCost(ItemLike pItem, int pCount) {
        this(pItem.asItem().builtInRegistryHolder(), pCount, DataComponentExactPredicate.EMPTY);
    }

    public ItemCost(Holder<Item> pItem, int pCount, DataComponentExactPredicate pComponents) {
        this(pItem, pCount, pComponents, createStack(pItem, pCount, pComponents));
    }

    public ItemCost withComponents(UnaryOperator<DataComponentExactPredicate.Builder> pComponents) {
        return new ItemCost(this.item, this.count, pComponents.apply(DataComponentExactPredicate.builder()).build());
    }

    private static ItemStack createStack(Holder<Item> pItem, int pCount, DataComponentExactPredicate pComponents) {
        return new ItemStack(pItem, pCount, pComponents.asPatch());
    }

    public boolean test(ItemStack pStack) {
        return pStack.is(this.item) && this.components.test((DataComponentGetter)pStack);
    }
}