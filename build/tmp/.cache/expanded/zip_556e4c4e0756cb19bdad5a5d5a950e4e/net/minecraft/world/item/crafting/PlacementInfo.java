package net.minecraft.world.item.crafting;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PlacementInfo {
    public static final int EMPTY_SLOT = -1;
    public static final PlacementInfo NOT_PLACEABLE = new PlacementInfo(List.of(), IntList.of());
    private final List<Ingredient> ingredients;
    private final IntList slotsToIngredientIndex;

    private PlacementInfo(List<Ingredient> pIngredients, IntList pSlotsToIngredientIndex) {
        this.ingredients = pIngredients;
        this.slotsToIngredientIndex = pSlotsToIngredientIndex;
    }

    public static PlacementInfo create(Ingredient pIngredient) {
        return pIngredient.isEmpty() ? NOT_PLACEABLE : new PlacementInfo(List.of(pIngredient), IntList.of(0));
    }

    public static PlacementInfo createFromOptionals(List<Optional<Ingredient>> pOptionals) {
        int i = pOptionals.size();
        List<Ingredient> list = new ArrayList<>(i);
        IntList intlist = new IntArrayList(i);
        int j = 0;

        for (Optional<Ingredient> optional : pOptionals) {
            if (optional.isPresent()) {
                Ingredient ingredient = optional.get();
                if (ingredient.isEmpty()) {
                    return NOT_PLACEABLE;
                }

                list.add(ingredient);
                intlist.add(j++);
            } else {
                intlist.add(-1);
            }
        }

        return new PlacementInfo(list, intlist);
    }

    public static PlacementInfo create(List<Ingredient> pIngredients) {
        int i = pIngredients.size();
        IntList intlist = new IntArrayList(i);

        for (int j = 0; j < i; j++) {
            Ingredient ingredient = pIngredients.get(j);
            if (ingredient.isEmpty()) {
                return NOT_PLACEABLE;
            }

            intlist.add(j);
        }

        return new PlacementInfo(pIngredients, intlist);
    }

    public IntList slotsToIngredientIndex() {
        return this.slotsToIngredientIndex;
    }

    public List<Ingredient> ingredients() {
        return this.ingredients;
    }

    public boolean isImpossibleToPlace() {
        return this.slotsToIngredientIndex.isEmpty();
    }
}