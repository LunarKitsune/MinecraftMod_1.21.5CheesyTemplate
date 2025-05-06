package net.minecraft.data.recipes;

import javax.annotation.Nullable;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;

public interface RecipeOutput {
    default void accept(ResourceKey<Recipe<?>> pKey, Recipe<?> pRecipe, @Nullable AdvancementHolder pAdvancement) {
        if (pAdvancement == null) {
            accept(pKey, pRecipe, null, null);
        } else {
            var ops = registry().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE);
            var json = Advancement.CODEC.encodeStart(ops, pAdvancement.value()).getOrThrow(IllegalStateException::new);
            accept(pKey, pRecipe, pAdvancement.id(), json);
        }
    }

    void accept(ResourceKey<Recipe<?>> id, Recipe<?> recipe, @Nullable net.minecraft.resources.ResourceLocation advancementId, @Nullable com.google.gson.JsonElement advancement);

    net.minecraft.core.HolderLookup.Provider registry();

    Advancement.Builder advancement();

    void includeRootAdvancement();
}
