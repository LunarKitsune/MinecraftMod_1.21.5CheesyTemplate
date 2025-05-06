package net.minecraft.world.item.crafting;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.ImmutableMultimap.Builder;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public class RecipeMap {
    public static final RecipeMap EMPTY = new RecipeMap(ImmutableMultimap.of(), Map.of());
    private final Multimap<RecipeType<?>, RecipeHolder<?>> byType;
    private final Map<ResourceKey<Recipe<?>>, RecipeHolder<?>> byKey;

    private RecipeMap(Multimap<RecipeType<?>, RecipeHolder<?>> pByType, Map<ResourceKey<Recipe<?>>, RecipeHolder<?>> pByKey) {
        this.byType = pByType;
        this.byKey = pByKey;
    }

    public static RecipeMap create(Iterable<RecipeHolder<?>> pRecipes) {
        Builder<RecipeType<?>, RecipeHolder<?>> builder = ImmutableMultimap.builder();
        com.google.common.collect.ImmutableMap.Builder<ResourceKey<Recipe<?>>, RecipeHolder<?>> builder1 = ImmutableMap.builder();

        for (RecipeHolder<?> recipeholder : pRecipes) {
            builder.put(recipeholder.value().getType(), recipeholder);
            builder1.put(recipeholder.id(), recipeholder);
        }

        return new RecipeMap(builder.build(), builder1.build());
    }

    public <I extends RecipeInput, T extends Recipe<I>> Collection<RecipeHolder<T>> byType(RecipeType<T> pType) {
        return (Collection<RecipeHolder<T>>)(Collection)this.byType.get(pType);
    }

    public Collection<RecipeHolder<?>> values() {
        return this.byKey.values();
    }

    @Nullable
    public RecipeHolder<?> byKey(ResourceKey<Recipe<?>> pKey) {
        return this.byKey.get(pKey);
    }

    public <I extends RecipeInput, T extends Recipe<I>> Stream<RecipeHolder<T>> getRecipesFor(RecipeType<T> pType, I pInput, Level pLevel) {
        return pInput.isEmpty()
            ? Stream.empty()
            : this.byType(pType).stream().filter(p_363912_ -> p_363912_.value().matches(pInput, pLevel));
    }
}