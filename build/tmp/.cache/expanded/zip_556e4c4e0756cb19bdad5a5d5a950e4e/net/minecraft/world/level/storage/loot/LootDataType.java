package net.minecraft.world.level.storage.loot;

import com.mojang.serialization.Codec;
import java.util.stream.Stream;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctions;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public record LootDataType<T>(ResourceKey<Registry<T>> registryKey, Codec<T> codec, LootDataType.Validator<T> validator) {
    public static final LootDataType<LootItemCondition> PREDICATE = new LootDataType<>(Registries.PREDICATE, LootItemCondition.DIRECT_CODEC, createSimpleValidator());
    public static final LootDataType<LootItemFunction> MODIFIER = new LootDataType<>(Registries.ITEM_MODIFIER, LootItemFunctions.ROOT_CODEC, createSimpleValidator());
    public static final LootDataType<LootTable> TABLE = new LootDataType<>(Registries.LOOT_TABLE, LootTable.DIRECT_CODEC, createLootTableValidator());

    public void runValidation(ValidationContext pContext, ResourceKey<T> pKey, T pValue) {
        this.validator.run(pContext, pKey, pValue);
    }

    public static Stream<LootDataType<?>> values() {
        return Stream.of(PREDICATE, MODIFIER, TABLE);
    }

    private static <T extends LootContextUser> LootDataType.Validator<T> createSimpleValidator() {
        return (p_327548_, p_327549_, p_327550_) -> p_327550_.validate(
            p_327548_.enterElement("{" + p_327549_.registry() + "/" + p_327549_.location() + "}", p_327549_)
        );
    }

    private static LootDataType.Validator<LootTable> createLootTableValidator() {
        return (p_327551_, p_327552_, p_327553_) -> p_327553_.validate(
            p_327551_.setContextKeySet(p_327553_.getParamSet()).enterElement("{" + p_327552_.registry() + "/" + p_327552_.location() + "}", p_327552_)
        );
    }

    @FunctionalInterface
    public interface Validator<T> {
        void run(ValidationContext pContext, ResourceKey<T> pKey, T pValue);
    }
}