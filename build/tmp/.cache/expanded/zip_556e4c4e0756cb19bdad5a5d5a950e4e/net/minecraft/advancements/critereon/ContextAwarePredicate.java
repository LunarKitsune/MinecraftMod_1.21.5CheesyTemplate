package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.Util;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class ContextAwarePredicate {
    public static final Codec<ContextAwarePredicate> CODEC = LootItemCondition.DIRECT_CODEC
        .listOf()
        .xmap(ContextAwarePredicate::new, p_309450_ -> p_309450_.conditions);
    private final List<LootItemCondition> conditions;
    private final Predicate<LootContext> compositePredicates;

    ContextAwarePredicate(List<LootItemCondition> pConditions) {
        this.conditions = pConditions;
        this.compositePredicates = Util.allOf(pConditions);
    }

    public static ContextAwarePredicate create(LootItemCondition... pConditions) {
        return new ContextAwarePredicate(List.of(pConditions));
    }

    public boolean matches(LootContext pContext) {
        return this.compositePredicates.test(pContext);
    }

    public void validate(ValidationContext pContext) {
        for (int i = 0; i < this.conditions.size(); i++) {
            LootItemCondition lootitemcondition = this.conditions.get(i);
            lootitemcondition.validate(pContext.forChild("[" + i + "]"));
        }
    }
}