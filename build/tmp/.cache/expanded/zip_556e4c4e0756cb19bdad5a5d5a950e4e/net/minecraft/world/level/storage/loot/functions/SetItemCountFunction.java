package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.Set;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;

/**
 * LootItemFunction that sets the stack's count based on a {@link NumberProvider}, optionally adding to any existing
 * count.
 */
public class SetItemCountFunction extends LootItemConditionalFunction {
    public static final MapCodec<SetItemCountFunction> CODEC = RecordCodecBuilder.mapCodec(
        p_297145_ -> commonFields(p_297145_)
            .and(
                p_297145_.group(
                    NumberProviders.CODEC.fieldOf("count").forGetter(p_297138_ -> p_297138_.value),
                    Codec.BOOL.fieldOf("add").orElse(false).forGetter(p_297139_ -> p_297139_.add)
                )
            )
            .apply(p_297145_, SetItemCountFunction::new)
    );
    private final NumberProvider value;
    private final boolean add;

    private SetItemCountFunction(List<LootItemCondition> pConditions, NumberProvider pValue, boolean pAdd) {
        super(pConditions);
        this.value = pValue;
        this.add = pAdd;
    }

    @Override
    public LootItemFunctionType<SetItemCountFunction> getType() {
        return LootItemFunctions.SET_COUNT;
    }

    @Override
    public Set<ContextKey<?>> getReferencedContextParams() {
        return this.value.getReferencedContextParams();
    }

    @Override
    public ItemStack run(ItemStack pStack, LootContext pContext) {
        int i = this.add ? pStack.getCount() : 0;
        pStack.setCount(i + this.value.getInt(pContext));
        return pStack;
    }

    public static LootItemConditionalFunction.Builder<?> setCount(NumberProvider pCountValue) {
        return simpleBuilder(p_297144_ -> new SetItemCountFunction(p_297144_, pCountValue, false));
    }

    public static LootItemConditionalFunction.Builder<?> setCount(NumberProvider pCountValue, boolean pAdd) {
        return simpleBuilder(p_297142_ -> new SetItemCountFunction(p_297142_, pCountValue, pAdd));
    }
}