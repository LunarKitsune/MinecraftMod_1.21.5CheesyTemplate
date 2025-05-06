package net.minecraft.world.level.storage.loot.functions;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.NumberProviders;

/**
 * LootItemFunction that adds a list of attribute modifiers to the stacks.
 */
public class SetAttributesFunction extends LootItemConditionalFunction {
    public static final MapCodec<SetAttributesFunction> CODEC = RecordCodecBuilder.mapCodec(
        p_341999_ -> commonFields(p_341999_)
            .and(
                p_341999_.group(
                    SetAttributesFunction.Modifier.CODEC.listOf().fieldOf("modifiers").forGetter(p_297111_ -> p_297111_.modifiers),
                    Codec.BOOL.optionalFieldOf("replace", true).forGetter(p_327579_ -> p_327579_.replace)
                )
            )
            .apply(p_341999_, SetAttributesFunction::new)
    );
    private final List<SetAttributesFunction.Modifier> modifiers;
    private final boolean replace;

    SetAttributesFunction(List<LootItemCondition> pConditions, List<SetAttributesFunction.Modifier> pModifiers, boolean pReplace) {
        super(pConditions);
        this.modifiers = List.copyOf(pModifiers);
        this.replace = pReplace;
    }

    @Override
    public LootItemFunctionType<SetAttributesFunction> getType() {
        return LootItemFunctions.SET_ATTRIBUTES;
    }

    @Override
    public Set<ContextKey<?>> getReferencedContextParams() {
        return this.modifiers.stream().flatMap(p_279080_ -> p_279080_.amount.getReferencedContextParams().stream()).collect(ImmutableSet.toImmutableSet());
    }

    @Override
    public ItemStack run(ItemStack pStack, LootContext pContext) {
        if (this.replace) {
            pStack.set(DataComponents.ATTRIBUTE_MODIFIERS, this.updateModifiers(pContext, ItemAttributeModifiers.EMPTY));
        } else {
            pStack.update(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY, p_360673_ -> this.updateModifiers(pContext, p_360673_));
        }

        return pStack;
    }

    private ItemAttributeModifiers updateModifiers(LootContext pContext, ItemAttributeModifiers pModifiers) {
        RandomSource randomsource = pContext.getRandom();

        for (SetAttributesFunction.Modifier setattributesfunction$modifier : this.modifiers) {
            EquipmentSlotGroup equipmentslotgroup = Util.getRandom(setattributesfunction$modifier.slots, randomsource);
            pModifiers = pModifiers.withModifierAdded(
                setattributesfunction$modifier.attribute,
                new AttributeModifier(
                    setattributesfunction$modifier.id,
                    setattributesfunction$modifier.amount.getFloat(pContext),
                    setattributesfunction$modifier.operation
                ),
                equipmentslotgroup
            );
        }

        return pModifiers;
    }

    public static SetAttributesFunction.ModifierBuilder modifier(
        ResourceLocation pId, Holder<Attribute> pAttribute, AttributeModifier.Operation pOperation, NumberProvider pAmount
    ) {
        return new SetAttributesFunction.ModifierBuilder(pId, pAttribute, pOperation, pAmount);
    }

    public static SetAttributesFunction.Builder setAttributes() {
        return new SetAttributesFunction.Builder();
    }

    public static class Builder extends LootItemConditionalFunction.Builder<SetAttributesFunction.Builder> {
        private final boolean replace;
        private final List<SetAttributesFunction.Modifier> modifiers = Lists.newArrayList();

        public Builder(boolean pReplace) {
            this.replace = pReplace;
        }

        public Builder() {
            this(false);
        }

        protected SetAttributesFunction.Builder getThis() {
            return this;
        }

        public SetAttributesFunction.Builder withModifier(SetAttributesFunction.ModifierBuilder pModifierBuilder) {
            this.modifiers.add(pModifierBuilder.build());
            return this;
        }

        @Override
        public LootItemFunction build() {
            return new SetAttributesFunction(this.getConditions(), this.modifiers, this.replace);
        }
    }

    record Modifier(
        ResourceLocation id, Holder<Attribute> attribute, AttributeModifier.Operation operation, NumberProvider amount, List<EquipmentSlotGroup> slots
    ) {
        private static final Codec<List<EquipmentSlotGroup>> SLOTS_CODEC = ExtraCodecs.nonEmptyList(ExtraCodecs.compactListCodec(EquipmentSlotGroup.CODEC));
        public static final Codec<SetAttributesFunction.Modifier> CODEC = RecordCodecBuilder.create(
            p_342000_ -> p_342000_.group(
                    ResourceLocation.CODEC.fieldOf("id").forGetter(SetAttributesFunction.Modifier::id),
                    Attribute.CODEC.fieldOf("attribute").forGetter(SetAttributesFunction.Modifier::attribute),
                    AttributeModifier.Operation.CODEC.fieldOf("operation").forGetter(SetAttributesFunction.Modifier::operation),
                    NumberProviders.CODEC.fieldOf("amount").forGetter(SetAttributesFunction.Modifier::amount),
                    SLOTS_CODEC.fieldOf("slot").forGetter(SetAttributesFunction.Modifier::slots)
                )
                .apply(p_342000_, SetAttributesFunction.Modifier::new)
        );
    }

    public static class ModifierBuilder {
        private final ResourceLocation id;
        private final Holder<Attribute> attribute;
        private final AttributeModifier.Operation operation;
        private final NumberProvider amount;
        private final Set<EquipmentSlotGroup> slots = EnumSet.noneOf(EquipmentSlotGroup.class);

        public ModifierBuilder(ResourceLocation pId, Holder<Attribute> pAttribute, AttributeModifier.Operation pOperation, NumberProvider pAmount) {
            this.id = pId;
            this.attribute = pAttribute;
            this.operation = pOperation;
            this.amount = pAmount;
        }

        public SetAttributesFunction.ModifierBuilder forSlot(EquipmentSlotGroup pSlot) {
            this.slots.add(pSlot);
            return this;
        }

        public SetAttributesFunction.Modifier build() {
            return new SetAttributesFunction.Modifier(this.id, this.attribute, this.operation, this.amount, List.copyOf(this.slots));
        }
    }
}