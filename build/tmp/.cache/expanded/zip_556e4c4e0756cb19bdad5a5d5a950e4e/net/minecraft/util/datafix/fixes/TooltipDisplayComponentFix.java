package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.OptionalDynamic;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;
import net.minecraft.Util;

public class TooltipDisplayComponentFix extends DataFix {
    private static final List<String> CONVERTED_ADDITIONAL_TOOLTIP_TYPES = List.of(
        "minecraft:banner_patterns",
        "minecraft:bees",
        "minecraft:block_entity_data",
        "minecraft:block_state",
        "minecraft:bundle_contents",
        "minecraft:charged_projectiles",
        "minecraft:container",
        "minecraft:container_loot",
        "minecraft:firework_explosion",
        "minecraft:fireworks",
        "minecraft:instrument",
        "minecraft:map_id",
        "minecraft:painting/variant",
        "minecraft:pot_decorations",
        "minecraft:potion_contents",
        "minecraft:tropical_fish/pattern",
        "minecraft:written_book_content"
    );

    public TooltipDisplayComponentFix(Schema pOutputSchema) {
        super(pOutputSchema, true);
    }

    @Override
    protected TypeRewriteRule makeRule() {
        Type<?> type = this.getInputSchema().getType(References.DATA_COMPONENTS);
        Type<?> type1 = this.getOutputSchema().getType(References.DATA_COMPONENTS);
        OpticFinder<?> opticfinder = type.findField("minecraft:can_place_on");
        OpticFinder<?> opticfinder1 = type.findField("minecraft:can_break");
        Type<?> type2 = type1.findFieldType("minecraft:can_place_on");
        Type<?> type3 = type1.findFieldType("minecraft:can_break");
        return this.fixTypeEverywhereTyped(
            "TooltipDisplayComponentFix", type, type1, p_396988_ -> fix(p_396988_, opticfinder, opticfinder1, type2, type3)
        );
    }

    private static Typed<?> fix(Typed<?> pData, OpticFinder<?> pCanPlaceOnOptic, OpticFinder<?> pCanBreakOptic, Type<?> pCanPlaceOnType, Type<?> pCanBreakType) {
        Set<String> set = new HashSet<>();
        pData = fixAdventureModePredicate(pData, pCanPlaceOnOptic, pCanPlaceOnType, "minecraft:can_place_on", set);
        pData = fixAdventureModePredicate(pData, pCanBreakOptic, pCanBreakType, "minecraft:can_break", set);
        return pData.update(
            DSL.remainderFinder(),
            p_395311_ -> {
                p_395311_ = fixSimpleComponent(p_395311_, "minecraft:trim", set);
                p_395311_ = fixSimpleComponent(p_395311_, "minecraft:unbreakable", set);
                p_395311_ = fixComponentAndUnwrap(p_395311_, "minecraft:dyed_color", "rgb", set);
                p_395311_ = fixComponentAndUnwrap(p_395311_, "minecraft:attribute_modifiers", "modifiers", set);
                p_395311_ = fixComponentAndUnwrap(p_395311_, "minecraft:enchantments", "levels", set);
                p_395311_ = fixComponentAndUnwrap(p_395311_, "minecraft:stored_enchantments", "levels", set);
                boolean flag = p_395311_.get("minecraft:hide_tooltip").result().isPresent();
                p_395311_ = p_395311_.remove("minecraft:hide_tooltip");
                boolean flag1 = p_395311_.get("minecraft:hide_additional_tooltip").result().isPresent();
                p_395311_ = p_395311_.remove("minecraft:hide_additional_tooltip");
                if (flag1) {
                    for (String s : CONVERTED_ADDITIONAL_TOOLTIP_TYPES) {
                        if (p_395311_.get(s).result().isPresent()) {
                            set.add(s);
                        }
                    }
                }

                return set.isEmpty() && !flag
                    ? p_395311_
                    : p_395311_.set(
                        "minecraft:tooltip_display",
                        p_395311_.createMap(
                            Map.of(
                                p_395311_.createString("hide_tooltip"),
                                p_395311_.createBoolean(flag),
                                p_395311_.createString("hidden_components"),
                                p_395311_.createList(set.stream().map(p_395311_::createString))
                            )
                        )
                    );
            }
        );
    }

    private static Dynamic<?> fixSimpleComponent(Dynamic<?> pData, String pName, Set<String> pProcessedComponents) {
        return fixRemainderComponent(pData, pName, pProcessedComponents, UnaryOperator.identity());
    }

    private static Dynamic<?> fixComponentAndUnwrap(Dynamic<?> pData, String pName, String pInnerFieldName, Set<String> pProcessedComponents) {
        return fixRemainderComponent(pData, pName, pProcessedComponents, p_394957_ -> DataFixUtils.orElse(p_394957_.get(pInnerFieldName).result(), p_394957_));
    }

    private static Dynamic<?> fixRemainderComponent(Dynamic<?> pData, String pName, Set<String> pProcessedComponents, UnaryOperator<Dynamic<?>> pUnwrapper) {
        return pData.update(pName, p_391205_ -> {
            boolean flag = p_391205_.get("show_in_tooltip").asBoolean(true);
            if (!flag) {
                pProcessedComponents.add(pName);
            }

            return pUnwrapper.apply(p_391205_.remove("show_in_tooltip"));
        });
    }

    private static Typed<?> fixAdventureModePredicate(Typed<?> pData, OpticFinder<?> pOptic, Type<?> pType, String pName, Set<String> pProcessedComponents) {
        return pData.updateTyped(pOptic, pType, p_395885_ -> Util.writeAndReadTypedOrThrow(p_395885_, pType, p_397322_ -> {
            OptionalDynamic<?> optionaldynamic = p_397322_.get("predicates");
            if (optionaldynamic.result().isEmpty()) {
                return p_397322_;
            } else {
                boolean flag = p_397322_.get("show_in_tooltip").asBoolean(true);
                if (!flag) {
                    pProcessedComponents.add(pName);
                }

                return optionaldynamic.result().get();
            }
        }));
    }
}