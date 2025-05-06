package net.minecraft.client.gui.screens.recipebook;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RecipeCollection {
    public static final RecipeCollection EMPTY = new RecipeCollection(List.of());
    private final List<RecipeDisplayEntry> entries;
    private final Set<RecipeDisplayId> craftable = new HashSet<>();
    private final Set<RecipeDisplayId> selected = new HashSet<>();

    public RecipeCollection(List<RecipeDisplayEntry> pEntries) {
        this.entries = pEntries;
    }

    public void selectRecipes(StackedItemContents pStackedItemContents, Predicate<RecipeDisplay> pFilter) {
        for (RecipeDisplayEntry recipedisplayentry : this.entries) {
            boolean flag = pFilter.test(recipedisplayentry.display());
            if (flag) {
                this.selected.add(recipedisplayentry.id());
            } else {
                this.selected.remove(recipedisplayentry.id());
            }

            if (flag && recipedisplayentry.canCraft(pStackedItemContents)) {
                this.craftable.add(recipedisplayentry.id());
            } else {
                this.craftable.remove(recipedisplayentry.id());
            }
        }
    }

    public boolean isCraftable(RecipeDisplayId pRecipe) {
        return this.craftable.contains(pRecipe);
    }

    public boolean hasCraftable() {
        return !this.craftable.isEmpty();
    }

    public boolean hasAnySelected() {
        return !this.selected.isEmpty();
    }

    public List<RecipeDisplayEntry> getRecipes() {
        return this.entries;
    }

    public List<RecipeDisplayEntry> getSelectedRecipes(RecipeCollection.CraftableStatus pCraftableStatus) {
        Predicate<RecipeDisplayId> predicate = switch (pCraftableStatus) {
            case ANY -> this.selected::contains;
            case CRAFTABLE -> this.craftable::contains;
            case NOT_CRAFTABLE -> p_361783_ -> this.selected.contains(p_361783_) && !this.craftable.contains(p_361783_);
        };
        List<RecipeDisplayEntry> list = new ArrayList<>();

        for (RecipeDisplayEntry recipedisplayentry : this.entries) {
            if (predicate.test(recipedisplayentry.id())) {
                list.add(recipedisplayentry);
            }
        }

        return list;
    }

    @OnlyIn(Dist.CLIENT)
    public static enum CraftableStatus {
        ANY,
        CRAFTABLE,
        NOT_CRAFTABLE;
    }
}