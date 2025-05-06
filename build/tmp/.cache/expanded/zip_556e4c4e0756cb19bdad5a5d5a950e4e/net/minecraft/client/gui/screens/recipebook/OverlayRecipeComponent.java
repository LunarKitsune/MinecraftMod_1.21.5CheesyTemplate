package net.minecraft.client.gui.screens.recipebook;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.recipebook.PlaceRecipeHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.FurnaceRecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class OverlayRecipeComponent implements Renderable, GuiEventListener {
    private static final ResourceLocation OVERLAY_RECIPE_SPRITE = ResourceLocation.withDefaultNamespace("recipe_book/overlay_recipe");
    private static final int MAX_ROW = 4;
    private static final int MAX_ROW_LARGE = 5;
    private static final float ITEM_RENDER_SCALE = 0.375F;
    public static final int BUTTON_SIZE = 25;
    private final List<OverlayRecipeComponent.OverlayRecipeButton> recipeButtons = Lists.newArrayList();
    private boolean isVisible;
    private int x;
    private int y;
    private RecipeCollection collection = RecipeCollection.EMPTY;
    @Nullable
    private RecipeDisplayId lastRecipeClicked;
    final SlotSelectTime slotSelectTime;
    private final boolean isFurnaceMenu;

    public OverlayRecipeComponent(SlotSelectTime pSlotSelectTime, boolean pIsFurnaceMenu) {
        this.slotSelectTime = pSlotSelectTime;
        this.isFurnaceMenu = pIsFurnaceMenu;
    }

    public void init(
        RecipeCollection pCollection, ContextMap pContextMap, boolean pIsFiltering, int pX, int pY, int pOverlayX, int pOverlayY, float pWidth
    ) {
        this.collection = pCollection;
        List<RecipeDisplayEntry> list = pCollection.getSelectedRecipes(RecipeCollection.CraftableStatus.CRAFTABLE);
        List<RecipeDisplayEntry> list1 = pIsFiltering ? Collections.emptyList() : pCollection.getSelectedRecipes(RecipeCollection.CraftableStatus.NOT_CRAFTABLE);
        int i = list.size();
        int j = i + list1.size();
        int k = j <= 16 ? 4 : 5;
        int l = (int)Math.ceil((float)j / k);
        this.x = pX;
        this.y = pY;
        float f = this.x + Math.min(j, k) * 25;
        float f1 = pOverlayX + 50;
        if (f > f1) {
            this.x = (int)(this.x - pWidth * (int)((f - f1) / pWidth));
        }

        float f2 = this.y + l * 25;
        float f3 = pOverlayY + 50;
        if (f2 > f3) {
            this.y = (int)(this.y - pWidth * Mth.ceil((f2 - f3) / pWidth));
        }

        float f4 = this.y;
        float f5 = pOverlayY - 100;
        if (f4 < f5) {
            this.y = (int)(this.y - pWidth * Mth.ceil((f4 - f5) / pWidth));
        }

        this.isVisible = true;
        this.recipeButtons.clear();

        for (int i1 = 0; i1 < j; i1++) {
            boolean flag = i1 < i;
            RecipeDisplayEntry recipedisplayentry = flag ? list.get(i1) : list1.get(i1 - i);
            int j1 = this.x + 4 + 25 * (i1 % k);
            int k1 = this.y + 5 + 25 * (i1 / k);
            if (this.isFurnaceMenu) {
                this.recipeButtons
                    .add(
                        new OverlayRecipeComponent.OverlaySmeltingRecipeButton(
                            j1, k1, recipedisplayentry.id(), recipedisplayentry.display(), pContextMap, flag
                        )
                    );
            } else {
                this.recipeButtons
                    .add(
                        new OverlayRecipeComponent.OverlayCraftingRecipeButton(
                            j1, k1, recipedisplayentry.id(), recipedisplayentry.display(), pContextMap, flag
                        )
                    );
            }
        }

        this.lastRecipeClicked = null;
    }

    public RecipeCollection getRecipeCollection() {
        return this.collection;
    }

    @Nullable
    public RecipeDisplayId getLastRecipeClicked() {
        return this.lastRecipeClicked;
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        if (pButton != 0) {
            return false;
        } else {
            for (OverlayRecipeComponent.OverlayRecipeButton overlayrecipecomponent$overlayrecipebutton : this.recipeButtons) {
                if (overlayrecipecomponent$overlayrecipebutton.mouseClicked(pMouseX, pMouseY, pButton)) {
                    this.lastRecipeClicked = overlayrecipecomponent$overlayrecipebutton.recipe;
                    return true;
                }
            }

            return false;
        }
    }

    @Override
    public boolean isMouseOver(double pMouseX, double pMouseY) {
        return false;
    }

    @Override
    public void render(GuiGraphics p_281618_, int p_282646_, int p_283687_, float p_283147_) {
        if (this.isVisible) {
            p_281618_.pose().pushPose();
            p_281618_.pose().translate(0.0F, 0.0F, 1000.0F);
            int i = this.recipeButtons.size() <= 16 ? 4 : 5;
            int j = Math.min(this.recipeButtons.size(), i);
            int k = Mth.ceil((float)this.recipeButtons.size() / i);
            int l = 4;
            p_281618_.blitSprite(RenderType::guiTextured, OVERLAY_RECIPE_SPRITE, this.x, this.y, j * 25 + 8, k * 25 + 8);

            for (OverlayRecipeComponent.OverlayRecipeButton overlayrecipecomponent$overlayrecipebutton : this.recipeButtons) {
                overlayrecipecomponent$overlayrecipebutton.render(p_281618_, p_282646_, p_283687_, p_283147_);
            }

            p_281618_.pose().popPose();
        }
    }

    public void setVisible(boolean pIsVisible) {
        this.isVisible = pIsVisible;
    }

    public boolean isVisible() {
        return this.isVisible;
    }

    @Override
    public void setFocused(boolean p_265597_) {
    }

    @Override
    public boolean isFocused() {
        return false;
    }

    @OnlyIn(Dist.CLIENT)
    class OverlayCraftingRecipeButton extends OverlayRecipeComponent.OverlayRecipeButton {
        private static final ResourceLocation ENABLED_SPRITE = ResourceLocation.withDefaultNamespace("recipe_book/crafting_overlay");
        private static final ResourceLocation HIGHLIGHTED_ENABLED_SPRITE = ResourceLocation.withDefaultNamespace("recipe_book/crafting_overlay_highlighted");
        private static final ResourceLocation DISABLED_SPRITE = ResourceLocation.withDefaultNamespace("recipe_book/crafting_overlay_disabled");
        private static final ResourceLocation HIGHLIGHTED_DISABLED_SPRITE = ResourceLocation.withDefaultNamespace("recipe_book/crafting_overlay_disabled_highlighted");
        private static final int GRID_WIDTH = 3;
        private static final int GRID_HEIGHT = 3;

        public OverlayCraftingRecipeButton(
            final int pX,
            final int pY,
            final RecipeDisplayId pRecipe,
            final RecipeDisplay pRecipeDisplay,
            final ContextMap pContextMap,
            final boolean pIsCraftable
        ) {
            super(pX, pY, pRecipe, pIsCraftable, calculateIngredientsPositions(pRecipeDisplay, pContextMap));
        }

        private static List<OverlayRecipeComponent.OverlayRecipeButton.Pos> calculateIngredientsPositions(RecipeDisplay pRecipeDisplay, ContextMap pContextMap) {
            List<OverlayRecipeComponent.OverlayRecipeButton.Pos> list = new ArrayList<>();
            switch (pRecipeDisplay) {
                case ShapedCraftingRecipeDisplay shapedcraftingrecipedisplay:
                    PlaceRecipeHelper.placeRecipe(
                        3,
                        3,
                        shapedcraftingrecipedisplay.width(),
                        shapedcraftingrecipedisplay.height(),
                        shapedcraftingrecipedisplay.ingredients(),
                        (p_369553_, p_363692_, p_370132_, p_368389_) -> {
                            List<ItemStack> list3 = p_369553_.resolveForStacks(pContextMap);
                            if (!list3.isEmpty()) {
                                list.add(createGridPos(p_370132_, p_368389_, list3));
                            }
                        }
                    );
                    break;
                case ShapelessCraftingRecipeDisplay shapelesscraftingrecipedisplay:
                    label19: {
                        List<SlotDisplay> list1 = shapelesscraftingrecipedisplay.ingredients();

                        for (int i = 0; i < list1.size(); i++) {
                            List<ItemStack> list2 = list1.get(i).resolveForStacks(pContextMap);
                            if (!list2.isEmpty()) {
                                list.add(createGridPos(i % 3, i / 3, list2));
                            }
                        }
                        break label19;
                    }
                default:
            }

            return list;
        }

        @Override
        protected ResourceLocation getSprite(boolean p_368159_) {
            if (p_368159_) {
                return this.isHoveredOrFocused() ? HIGHLIGHTED_ENABLED_SPRITE : ENABLED_SPRITE;
            } else {
                return this.isHoveredOrFocused() ? HIGHLIGHTED_DISABLED_SPRITE : DISABLED_SPRITE;
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    abstract class OverlayRecipeButton extends AbstractWidget {
        final RecipeDisplayId recipe;
        private final boolean isCraftable;
        private final List<OverlayRecipeComponent.OverlayRecipeButton.Pos> slots;

        public OverlayRecipeButton(
            final int pX,
            final int pY,
            final RecipeDisplayId pRecipe,
            final boolean pIsCraftable,
            final List<OverlayRecipeComponent.OverlayRecipeButton.Pos> pSlots
        ) {
            super(pX, pY, 24, 24, CommonComponents.EMPTY);
            this.slots = pSlots;
            this.recipe = pRecipe;
            this.isCraftable = pIsCraftable;
        }

        protected static OverlayRecipeComponent.OverlayRecipeButton.Pos createGridPos(int pX, int pY, List<ItemStack> pPossibleItems) {
            return new OverlayRecipeComponent.OverlayRecipeButton.Pos(3 + pX * 7, 3 + pY * 7, pPossibleItems);
        }

        protected abstract ResourceLocation getSprite(boolean pEnabled);

        @Override
        public void updateWidgetNarration(NarrationElementOutput p_259646_) {
            this.defaultButtonNarrationText(p_259646_);
        }

        @Override
        public void renderWidget(GuiGraphics p_283557_, int p_283483_, int p_282919_, float p_282165_) {
            p_283557_.blitSprite(RenderType::guiTextured, this.getSprite(this.isCraftable), this.getX(), this.getY(), this.width, this.height);
            float f = this.getX() + 2;
            float f1 = this.getY() + 2;
            float f2 = 150.0F;

            for (OverlayRecipeComponent.OverlayRecipeButton.Pos overlayrecipecomponent$overlayrecipebutton$pos : this.slots) {
                p_283557_.pose().pushPose();
                p_283557_.pose()
                    .translate(
                        f + overlayrecipecomponent$overlayrecipebutton$pos.x, f1 + overlayrecipecomponent$overlayrecipebutton$pos.y, 150.0F
                    );
                p_283557_.pose().scale(0.375F, 0.375F, 1.0F);
                p_283557_.pose().translate(-8.0F, -8.0F, 0.0F);
                p_283557_.renderItem(overlayrecipecomponent$overlayrecipebutton$pos.selectIngredient(OverlayRecipeComponent.this.slotSelectTime.currentIndex()), 0, 0);
                p_283557_.pose().popPose();
            }
        }

        @OnlyIn(Dist.CLIENT)
        protected record Pos(int x, int y, List<ItemStack> ingredients) {
            public Pos(int x, int y, List<ItemStack> ingredients) {
                if (ingredients.isEmpty()) {
                    throw new IllegalArgumentException("Ingredient list must be non-empty");
                } else {
                    this.x = x;
                    this.y = y;
                    this.ingredients = ingredients;
                }
            }

            public ItemStack selectIngredient(int pIndex) {
                return this.ingredients.get(pIndex % this.ingredients.size());
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    class OverlaySmeltingRecipeButton extends OverlayRecipeComponent.OverlayRecipeButton {
        private static final ResourceLocation ENABLED_SPRITE = ResourceLocation.withDefaultNamespace("recipe_book/furnace_overlay");
        private static final ResourceLocation HIGHLIGHTED_ENABLED_SPRITE = ResourceLocation.withDefaultNamespace("recipe_book/furnace_overlay_highlighted");
        private static final ResourceLocation DISABLED_SPRITE = ResourceLocation.withDefaultNamespace("recipe_book/furnace_overlay_disabled");
        private static final ResourceLocation HIGHLIGHTED_DISABLED_SPRITE = ResourceLocation.withDefaultNamespace("recipe_book/furnace_overlay_disabled_highlighted");

        public OverlaySmeltingRecipeButton(
            final int pX,
            final int pY,
            final RecipeDisplayId pRecipe,
            final RecipeDisplay pRecipeDisplay,
            final ContextMap pContextMap,
            final boolean pIsCraftable
        ) {
            super(pX, pY, pRecipe, pIsCraftable, calculateIngredientsPositions(pRecipeDisplay, pContextMap));
        }

        private static List<OverlayRecipeComponent.OverlayRecipeButton.Pos> calculateIngredientsPositions(RecipeDisplay pRecipeDisplay, ContextMap pContextMap) {
            if (pRecipeDisplay instanceof FurnaceRecipeDisplay furnacerecipedisplay) {
                List<ItemStack> list = furnacerecipedisplay.ingredient().resolveForStacks(pContextMap);
                if (!list.isEmpty()) {
                    return List.of(createGridPos(1, 1, list));
                }
            }

            return List.of();
        }

        @Override
        protected ResourceLocation getSprite(boolean p_361174_) {
            if (p_361174_) {
                return this.isHoveredOrFocused() ? HIGHLIGHTED_ENABLED_SPRITE : ENABLED_SPRITE;
            } else {
                return this.isHoveredOrFocused() ? HIGHLIGHTED_DISABLED_SPRITE : DISABLED_SPRITE;
            }
        }
    }
}