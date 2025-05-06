package net.minecraft.world.inventory;

import java.util.List;
import net.minecraft.recipebook.ServerPlaceRecipe;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.StackedItemContents;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;

public abstract class AbstractCraftingMenu extends RecipeBookMenu {
    private final int width;
    private final int height;
    protected final CraftingContainer craftSlots;
    protected final ResultContainer resultSlots = new ResultContainer();

    public AbstractCraftingMenu(MenuType<?> pMenuType, int pContainerId, int pWidth, int pHeight) {
        super(pMenuType, pContainerId);
        this.width = pWidth;
        this.height = pHeight;
        this.craftSlots = new TransientCraftingContainer(this, pWidth, pHeight);
    }

    protected Slot addResultSlot(Player pPlayer, int pX, int pY) {
        return this.addSlot(new ResultSlot(pPlayer, this.craftSlots, this.resultSlots, 0, pX, pY));
    }

    protected void addCraftingGridSlots(int pX, int pY) {
        for (int i = 0; i < this.width; i++) {
            for (int j = 0; j < this.height; j++) {
                this.addSlot(new Slot(this.craftSlots, j + i * this.width, pX + j * 18, pY + i * 18));
            }
        }
    }

    @Override
    public RecipeBookMenu.PostPlaceAction handlePlacement(boolean p_367003_, boolean p_360772_, RecipeHolder<?> p_361387_, ServerLevel p_365408_, Inventory p_368520_) {
        RecipeHolder<CraftingRecipe> recipeholder = (RecipeHolder<CraftingRecipe>)p_361387_;
        this.beginPlacingRecipe();

        RecipeBookMenu.PostPlaceAction recipebookmenu$postplaceaction;
        try {
            List<Slot> list = this.getInputGridSlots();
            recipebookmenu$postplaceaction = ServerPlaceRecipe.placeRecipe(new ServerPlaceRecipe.CraftingMenuAccess<CraftingRecipe>() {
                @Override
                public void fillCraftSlotsStackedContents(StackedItemContents p_367296_) {
                    AbstractCraftingMenu.this.fillCraftSlotsStackedContents(p_367296_);
                }

                @Override
                public void clearCraftingContent() {
                    AbstractCraftingMenu.this.resultSlots.clearContent();
                    AbstractCraftingMenu.this.craftSlots.clearContent();
                }

                @Override
                public boolean recipeMatches(RecipeHolder<CraftingRecipe> p_368304_) {
                    return p_368304_.value().matches(AbstractCraftingMenu.this.craftSlots.asCraftInput(), AbstractCraftingMenu.this.owner().level());
                }
            }, this.width, this.height, list, list, p_368520_, recipeholder, p_367003_, p_360772_);
        } finally {
            this.finishPlacingRecipe(p_365408_, (RecipeHolder<CraftingRecipe>)p_361387_);
        }

        return recipebookmenu$postplaceaction;
    }

    protected void beginPlacingRecipe() {
    }

    protected void finishPlacingRecipe(ServerLevel pLevel, RecipeHolder<CraftingRecipe> pRecipe) {
    }

    public abstract Slot getResultSlot();

    public abstract List<Slot> getInputGridSlots();

    public int getGridWidth() {
        return this.width;
    }

    public int getGridHeight() {
        return this.height;
    }

    protected abstract Player owner();

    @Override
    public void fillCraftSlotsStackedContents(StackedItemContents p_365758_) {
        this.craftSlots.fillStackedContents(p_365758_);
    }
}