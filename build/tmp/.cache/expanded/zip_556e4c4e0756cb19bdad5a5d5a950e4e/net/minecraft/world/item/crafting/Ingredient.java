package net.minecraft.world.item.crafting;

import com.mojang.serialization.Codec;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.HolderSetCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.player.StackedContents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.level.ItemLike;

public class Ingredient implements StackedContents.IngredientInfo<Holder<Item>>, Predicate<ItemStack> {
    private static final StreamCodec<RegistryFriendlyByteBuf, Ingredient> VANILLA_CONTENTS_STREAM_CODEC = ByteBufCodecs.holderSet(Registries.ITEM)
        .map(Ingredient::new, p_359816_ -> p_359816_.values);
    public static final StreamCodec<RegistryFriendlyByteBuf, Ingredient> CONTENTS_STREAM_CODEC = net.minecraftforge.common.ForgeHooks.ingredientStreamCodec();
    public static final StreamCodec<RegistryFriendlyByteBuf, Optional<Ingredient>> OPTIONAL_CONTENTS_STREAM_CODEC = CONTENTS_STREAM_CODEC
        .map(
            ingredient -> ingredient.items().count() == 0 ? Optional.empty() : Optional.of(ingredient),
            ingredient -> ingredient.orElseGet(() -> Ingredient.of())
        );
    public static final Codec<HolderSet<Item>> NON_AIR_HOLDER_SET_CODEC = HolderSetCodec.create(Registries.ITEM, Item.CODEC, false);
    private static final Codec<Ingredient> VANILLA_CODEC = ExtraCodecs.nonEmptyHolderSet(NON_AIR_HOLDER_SET_CODEC).xmap(Ingredient::new, p_359811_ -> p_359811_.values);
    private static final com.mojang.serialization.MapCodec<Ingredient> VANILLA_MAP_CODEC = VANILLA_CODEC.fieldOf("value");
    public static final Codec<Ingredient> CODEC = net.minecraftforge.common.ForgeHooks.ingredientBaseCodec(VANILLA_CODEC);
    protected final HolderSet<Item> values;

    protected Ingredient(HolderSet<Item> pValues) {
        this(pValues, true);
    }

    protected Ingredient(HolderSet<Item> pValues, boolean validate) {
        if (validate)
        pValues.unwrap().ifRight(p_359817_ -> {
            if (p_359817_.isEmpty()) {
                throw new UnsupportedOperationException("Ingredients can't be empty");
            } else if (p_359817_.contains(Items.AIR.builtInRegistryHolder())) {
                throw new UnsupportedOperationException("Ingredient can't contain air");
            }
        });
        this.values = pValues;
    }

    public static boolean testOptionalIngredient(Optional<Ingredient> pIngredient, ItemStack pStack) {
        return pIngredient.<Boolean>map(p_359819_ -> p_359819_.test(pStack)).orElseGet(pStack::isEmpty);
    }

    @Deprecated
    public Stream<Holder<Item>> items() {
        return this.values.stream();
    }

    public boolean isEmpty() {
        return this.values.size() == 0;
    }

    public boolean test(ItemStack pStack) {
        return pStack.is(this.values);
    }

    public boolean acceptsItem(Holder<Item> p_378483_) {
        return this.values.contains(p_378483_);
    }

    @Override
    public boolean equals(Object pOther) {
        return pOther instanceof Ingredient ingredient ? Objects.equals(this.values, ingredient.values) : false;
    }

    public static Ingredient of(ItemLike pItem) {
        return new Ingredient(HolderSet.direct(pItem.asItem().builtInRegistryHolder()));
    }

    public static Ingredient of(ItemLike... pItems) {
        return of(Arrays.stream(pItems));
    }

    public static Ingredient of(Stream<? extends ItemLike> pItems) {
        return new Ingredient(HolderSet.direct(pItems.map(p_359813_ -> p_359813_.asItem().builtInRegistryHolder()).toList()));
    }

    public static Ingredient of(HolderSet<Item> pItems) {
        return new Ingredient(pItems);
    }

    public SlotDisplay display() {
        return (SlotDisplay)this.values
            .unwrap()
            .map(SlotDisplay.TagSlotDisplay::new, p_359812_ -> new SlotDisplay.Composite(p_359812_.stream().map(Ingredient::displayForSingleItem).toList()));
    }

    public static SlotDisplay optionalIngredientToDisplay(Optional<Ingredient> pIngredient) {
        return pIngredient.map(Ingredient::display).orElse(SlotDisplay.Empty.INSTANCE);
    }

    private static SlotDisplay displayForSingleItem(Holder<Item> pItem) {
        SlotDisplay slotdisplay = new SlotDisplay.ItemSlotDisplay(pItem);
        ItemStack itemstack = pItem.value().getCraftingRemainder();
        if (!itemstack.isEmpty()) {
            SlotDisplay slotdisplay1 = new SlotDisplay.ItemStackSlotDisplay(itemstack);
            return new SlotDisplay.WithRemainder(slotdisplay, slotdisplay1);
        } else {
            return slotdisplay;
        }
    }

    public boolean isSimple() {
        return true;
    }

    private final boolean isVanilla = this.getClass() == Ingredient.class;
    public final boolean isVanilla() {
        return isVanilla;
    }

    public static final net.minecraftforge.common.crafting.ingredients.IIngredientSerializer<Ingredient> VANILLA_SERIALIZER =
        new net.minecraftforge.common.crafting.ingredients.IIngredientSerializer<Ingredient>() {
            @Override
            public com.mojang.serialization.MapCodec<? extends Ingredient> codec() {
                return VANILLA_MAP_CODEC;
            }

            @Override
            public void write(RegistryFriendlyByteBuf buffer, Ingredient value) {
                VANILLA_CONTENTS_STREAM_CODEC.encode(buffer, value);
            }

            @Override
            public Ingredient read(RegistryFriendlyByteBuf buffer) {
                return VANILLA_CONTENTS_STREAM_CODEC.decode(buffer);
            }
        };

    public net.minecraftforge.common.crafting.ingredients.IIngredientSerializer<? extends Ingredient> serializer() {
        if (!isVanilla()) throw new IllegalStateException("Modders must implement Ingredient.codec in their custom Ingredients: " + getClass());
        return VANILLA_SERIALIZER;
    }

    @Override
    public String toString() {
        var buf = new StringBuilder();
        buf.append("Ingredient[");
        for (int x = 0; x < values.size(); x++) {
            if (x != 0)
                buf.append(", ");
            buf.append(values.get(x));
        }
        buf.append(']');
        return buf.toString();
    }
}
