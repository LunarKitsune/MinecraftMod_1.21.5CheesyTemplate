package net.minecraft.world.inventory;

import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.GameRules;

public interface RecipeCraftingHolder {
    void setRecipeUsed(@Nullable RecipeHolder<?> pRecipe);

    @Nullable
    RecipeHolder<?> getRecipeUsed();

    default void awardUsedRecipes(Player pPlayer, List<ItemStack> pItems) {
        RecipeHolder<?> recipeholder = this.getRecipeUsed();
        if (recipeholder != null) {
            pPlayer.triggerRecipeCrafted(recipeholder, pItems);
            if (!recipeholder.value().isSpecial()) {
                pPlayer.awardRecipes(Collections.singleton(recipeholder));
                this.setRecipeUsed(null);
            }
        }
    }

    default boolean setRecipeUsed(ServerPlayer pPlayer, RecipeHolder<?> pRecipe) {
        if (!pRecipe.value().isSpecial()
            && pPlayer.serverLevel().getGameRules().getBoolean(GameRules.RULE_LIMITED_CRAFTING)
            && !pPlayer.getRecipeBook().contains(pRecipe.id())) {
            return false;
        } else {
            this.setRecipeUsed(pRecipe);
            return true;
        }
    }
}