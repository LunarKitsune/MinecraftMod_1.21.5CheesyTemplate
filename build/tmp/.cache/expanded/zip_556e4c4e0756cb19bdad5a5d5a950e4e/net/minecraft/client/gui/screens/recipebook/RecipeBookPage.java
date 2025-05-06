package net.minecraft.client.gui.screens.recipebook;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.StateSwitchingButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RecipeBookPage {
    public static final int ITEMS_PER_PAGE = 20;
    private static final WidgetSprites PAGE_FORWARD_SPRITES = new WidgetSprites(
        ResourceLocation.withDefaultNamespace("recipe_book/page_forward"), ResourceLocation.withDefaultNamespace("recipe_book/page_forward_highlighted")
    );
    private static final WidgetSprites PAGE_BACKWARD_SPRITES = new WidgetSprites(
        ResourceLocation.withDefaultNamespace("recipe_book/page_backward"), ResourceLocation.withDefaultNamespace("recipe_book/page_backward_highlighted")
    );
    private final List<RecipeButton> buttons = Lists.newArrayListWithCapacity(20);
    @Nullable
    private RecipeButton hoveredButton;
    private final OverlayRecipeComponent overlay;
    private Minecraft minecraft;
    private final RecipeBookComponent<?> parent;
    private List<RecipeCollection> recipeCollections = ImmutableList.of();
    private StateSwitchingButton forwardButton;
    private StateSwitchingButton backButton;
    private int totalPages;
    private int currentPage;
    private ClientRecipeBook recipeBook;
    @Nullable
    private RecipeDisplayId lastClickedRecipe;
    @Nullable
    private RecipeCollection lastClickedRecipeCollection;
    private boolean isFiltering;

    public RecipeBookPage(RecipeBookComponent<?> pParent, SlotSelectTime pSlotSelectTime, boolean pIsFurnaceMenu) {
        this.parent = pParent;
        this.overlay = new OverlayRecipeComponent(pSlotSelectTime, pIsFurnaceMenu);

        for (int i = 0; i < 20; i++) {
            this.buttons.add(new RecipeButton(pSlotSelectTime));
        }
    }

    public void init(Minecraft pMinecraft, int pX, int pY) {
        this.minecraft = pMinecraft;
        this.recipeBook = pMinecraft.player.getRecipeBook();

        for (int i = 0; i < this.buttons.size(); i++) {
            this.buttons.get(i).setPosition(pX + 11 + 25 * (i % 5), pY + 31 + 25 * (i / 5));
        }

        this.forwardButton = new StateSwitchingButton(pX + 93, pY + 137, 12, 17, false);
        this.forwardButton.initTextureValues(PAGE_FORWARD_SPRITES);
        this.backButton = new StateSwitchingButton(pX + 38, pY + 137, 12, 17, true);
        this.backButton.initTextureValues(PAGE_BACKWARD_SPRITES);
    }

    public void updateCollections(List<RecipeCollection> pRecipeCollections, boolean pResetPageNumber, boolean pIsFiltering) {
        this.recipeCollections = pRecipeCollections;
        this.isFiltering = pIsFiltering;
        this.totalPages = (int)Math.ceil(pRecipeCollections.size() / 20.0);
        if (this.totalPages <= this.currentPage || pResetPageNumber) {
            this.currentPage = 0;
        }

        this.updateButtonsForPage();
    }

    private void updateButtonsForPage() {
        int i = 20 * this.currentPage;
        ContextMap contextmap = SlotDisplayContext.fromLevel(this.minecraft.level);

        for (int j = 0; j < this.buttons.size(); j++) {
            RecipeButton recipebutton = this.buttons.get(j);
            if (i + j < this.recipeCollections.size()) {
                RecipeCollection recipecollection = this.recipeCollections.get(i + j);
                recipebutton.init(recipecollection, this.isFiltering, this, contextmap);
                recipebutton.visible = true;
            } else {
                recipebutton.visible = false;
            }
        }

        this.updateArrowButtons();
    }

    private void updateArrowButtons() {
        this.forwardButton.visible = this.totalPages > 1 && this.currentPage < this.totalPages - 1;
        this.backButton.visible = this.totalPages > 1 && this.currentPage > 0;
    }

    public void render(GuiGraphics pGuiGraphics, int pX, int pY, int pMouseX, int pMouseY, float pPartialTick) {
        if (this.totalPages > 1) {
            Component component = Component.translatable("gui.recipebook.page", this.currentPage + 1, this.totalPages);
            int i = this.minecraft.font.width(component);
            pGuiGraphics.drawString(this.minecraft.font, component, pX - i / 2 + 73, pY + 141, -1);
        }

        this.hoveredButton = null;

        for (RecipeButton recipebutton : this.buttons) {
            recipebutton.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
            if (recipebutton.visible && recipebutton.isHoveredOrFocused()) {
                this.hoveredButton = recipebutton;
            }
        }

        this.backButton.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        this.forwardButton.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        this.overlay.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
    }

    public void renderTooltip(GuiGraphics pGuiGraphics, int pX, int pY) {
        if (this.minecraft.screen != null && this.hoveredButton != null && !this.overlay.isVisible()) {
            ItemStack itemstack = this.hoveredButton.getDisplayStack();
            ResourceLocation resourcelocation = itemstack.get(DataComponents.TOOLTIP_STYLE);
            pGuiGraphics.renderComponentTooltip(this.minecraft.font, this.hoveredButton.getTooltipText(itemstack), pX, pY, resourcelocation);
        }
    }

    @Nullable
    public RecipeDisplayId getLastClickedRecipe() {
        return this.lastClickedRecipe;
    }

    @Nullable
    public RecipeCollection getLastClickedRecipeCollection() {
        return this.lastClickedRecipeCollection;
    }

    public void setInvisible() {
        this.overlay.setVisible(false);
    }

    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton, int pX, int pY, int pWidth, int pHeight) {
        this.lastClickedRecipe = null;
        this.lastClickedRecipeCollection = null;
        if (this.overlay.isVisible()) {
            if (this.overlay.mouseClicked(pMouseX, pMouseY, pButton)) {
                this.lastClickedRecipe = this.overlay.getLastRecipeClicked();
                this.lastClickedRecipeCollection = this.overlay.getRecipeCollection();
            } else {
                this.overlay.setVisible(false);
            }

            return true;
        } else if (this.forwardButton.mouseClicked(pMouseX, pMouseY, pButton)) {
            this.currentPage++;
            this.updateButtonsForPage();
            return true;
        } else if (this.backButton.mouseClicked(pMouseX, pMouseY, pButton)) {
            this.currentPage--;
            this.updateButtonsForPage();
            return true;
        } else {
            ContextMap contextmap = SlotDisplayContext.fromLevel(this.minecraft.level);

            for (RecipeButton recipebutton : this.buttons) {
                if (recipebutton.mouseClicked(pMouseX, pMouseY, pButton)) {
                    if (pButton == 0) {
                        this.lastClickedRecipe = recipebutton.getCurrentRecipe();
                        this.lastClickedRecipeCollection = recipebutton.getCollection();
                    } else if (pButton == 1 && !this.overlay.isVisible() && !recipebutton.isOnlyOption()) {
                        this.overlay
                            .init(
                                recipebutton.getCollection(),
                                contextmap,
                                this.isFiltering,
                                recipebutton.getX(),
                                recipebutton.getY(),
                                pX + pWidth / 2,
                                pY + 13 + pHeight / 2,
                                recipebutton.getWidth()
                            );
                    }

                    return true;
                }
            }

            return false;
        }
    }

    public void recipeShown(RecipeDisplayId pRecipe) {
        this.parent.recipeShown(pRecipe);
    }

    public ClientRecipeBook getRecipeBook() {
        return this.recipeBook;
    }

    protected void listButtons(Consumer<AbstractWidget> pConsumer) {
        pConsumer.accept(this.forwardButton);
        pConsumer.accept(this.backButton);
        this.buttons.forEach(pConsumer);
    }
}