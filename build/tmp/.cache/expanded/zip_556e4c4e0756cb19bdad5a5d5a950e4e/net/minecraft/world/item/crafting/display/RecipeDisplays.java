package net.minecraft.world.item.crafting.display;

import net.minecraft.core.Registry;

public class RecipeDisplays {
    public static RecipeDisplay.Type<?> bootstrap(Registry<RecipeDisplay.Type<?>> pRegistry) {
        Registry.register(pRegistry, "crafting_shapeless", ShapelessCraftingRecipeDisplay.TYPE);
        Registry.register(pRegistry, "crafting_shaped", ShapedCraftingRecipeDisplay.TYPE);
        Registry.register(pRegistry, "furnace", FurnaceRecipeDisplay.TYPE);
        Registry.register(pRegistry, "stonecutter", StonecutterRecipeDisplay.TYPE);
        return Registry.register(pRegistry, "smithing", SmithingRecipeDisplay.TYPE);
    }
}