package net.minecraft.client.gui.screens.inventory.tooltip;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.math.Fraction;

@OnlyIn(Dist.CLIENT)
public class ClientBundleTooltip implements ClientTooltipComponent {
    private static final ResourceLocation PROGRESSBAR_BORDER_SPRITE = ResourceLocation.withDefaultNamespace("container/bundle/bundle_progressbar_border");
    private static final ResourceLocation PROGRESSBAR_FILL_SPRITE = ResourceLocation.withDefaultNamespace("container/bundle/bundle_progressbar_fill");
    private static final ResourceLocation PROGRESSBAR_FULL_SPRITE = ResourceLocation.withDefaultNamespace("container/bundle/bundle_progressbar_full");
    private static final ResourceLocation SLOT_HIGHLIGHT_BACK_SPRITE = ResourceLocation.withDefaultNamespace("container/bundle/slot_highlight_back");
    private static final ResourceLocation SLOT_HIGHLIGHT_FRONT_SPRITE = ResourceLocation.withDefaultNamespace("container/bundle/slot_highlight_front");
    private static final ResourceLocation SLOT_BACKGROUND_SPRITE = ResourceLocation.withDefaultNamespace("container/bundle/slot_background");
    private static final int SLOT_MARGIN = 4;
    private static final int SLOT_SIZE = 24;
    private static final int GRID_WIDTH = 96;
    private static final int PROGRESSBAR_HEIGHT = 13;
    private static final int PROGRESSBAR_WIDTH = 96;
    private static final int PROGRESSBAR_BORDER = 1;
    private static final int PROGRESSBAR_FILL_MAX = 94;
    private static final int PROGRESSBAR_MARGIN_Y = 4;
    private static final Component BUNDLE_FULL_TEXT = Component.translatable("item.minecraft.bundle.full");
    private static final Component BUNDLE_EMPTY_TEXT = Component.translatable("item.minecraft.bundle.empty");
    private static final Component BUNDLE_EMPTY_DESCRIPTION = Component.translatable("item.minecraft.bundle.empty.description");
    private final BundleContents contents;

    public ClientBundleTooltip(BundleContents pContents) {
        this.contents = pContents;
    }

    @Override
    public int getHeight(Font p_362861_) {
        return this.contents.isEmpty() ? getEmptyBundleBackgroundHeight(p_362861_) : this.backgroundHeight();
    }

    @Override
    public int getWidth(Font p_169901_) {
        return 96;
    }

    @Override
    public boolean showTooltipWithItemInHand() {
        return true;
    }

    private static int getEmptyBundleBackgroundHeight(Font pFont) {
        return getEmptyBundleDescriptionTextHeight(pFont) + 13 + 8;
    }

    private int backgroundHeight() {
        return this.itemGridHeight() + 13 + 8;
    }

    private int itemGridHeight() {
        return this.gridSizeY() * 24;
    }

    private int getContentXOffset(int pWidth) {
        return (pWidth - 96) / 2;
    }

    private int gridSizeY() {
        return Mth.positiveCeilDiv(this.slotCount(), 4);
    }

    private int slotCount() {
        return Math.min(12, this.contents.size());
    }

    @Override
    public void renderImage(Font p_194042_, int p_194043_, int p_194044_, int p_369638_, int p_364312_, GuiGraphics p_282522_) {
        if (this.contents.isEmpty()) {
            this.renderEmptyBundleTooltip(p_194042_, p_194043_, p_194044_, p_369638_, p_364312_, p_282522_);
        } else {
            this.renderBundleWithItemsTooltip(p_194042_, p_194043_, p_194044_, p_369638_, p_364312_, p_282522_);
        }
    }

    private void renderEmptyBundleTooltip(Font pFont, int pX, int pY, int pWidth, int pHeight, GuiGraphics pGuiGraphics) {
        drawEmptyBundleDescriptionText(pX + this.getContentXOffset(pWidth), pY, pFont, pGuiGraphics);
        this.drawProgressbar(pX + this.getContentXOffset(pWidth), pY + getEmptyBundleDescriptionTextHeight(pFont) + 4, pFont, pGuiGraphics);
    }

    private void renderBundleWithItemsTooltip(Font pFont, int pX, int pY, int pWidth, int pHeight, GuiGraphics pGuiGraphics) {
        boolean flag = this.contents.size() > 12;
        List<ItemStack> list = this.getShownItems(this.contents.getNumberOfItemsToShow());
        int i = pX + this.getContentXOffset(pWidth) + 96;
        int j = pY + this.gridSizeY() * 24;
        int k = 1;

        for (int l = 1; l <= this.gridSizeY(); l++) {
            for (int i1 = 1; i1 <= 4; i1++) {
                int j1 = i - i1 * 24;
                int k1 = j - l * 24;
                if (shouldRenderSurplusText(flag, i1, l)) {
                    renderCount(j1, k1, this.getAmountOfHiddenItems(list), pFont, pGuiGraphics);
                } else if (shouldRenderItemSlot(list, k)) {
                    this.renderSlot(k, j1, k1, list, k, pFont, pGuiGraphics);
                    k++;
                }
            }
        }

        this.drawSelectedItemTooltip(pFont, pGuiGraphics, pX, pY, pWidth);
        this.drawProgressbar(pX + this.getContentXOffset(pWidth), pY + this.itemGridHeight() + 4, pFont, pGuiGraphics);
    }

    private List<ItemStack> getShownItems(int pItemsToShow) {
        int i = Math.min(this.contents.size(), pItemsToShow);
        return this.contents.itemCopyStream().toList().subList(0, i);
    }

    private static boolean shouldRenderSurplusText(boolean pHasEnoughItems, int pCellX, int pCellY) {
        return pHasEnoughItems && pCellX * pCellY == 1;
    }

    private static boolean shouldRenderItemSlot(List<ItemStack> pShownItems, int pSlotIndex) {
        return pShownItems.size() >= pSlotIndex;
    }

    private int getAmountOfHiddenItems(List<ItemStack> pShownItems) {
        return this.contents.itemCopyStream().skip(pShownItems.size()).mapToInt(ItemStack::getCount).sum();
    }

    private void renderSlot(int pSlotIndex, int pX, int pY, List<ItemStack> pShownItems, int pSeed, Font pFont, GuiGraphics pGuiGraphics) {
        int i = pShownItems.size() - pSlotIndex;
        boolean flag = i == this.contents.getSelectedItem();
        ItemStack itemstack = pShownItems.get(i);
        if (flag) {
            pGuiGraphics.blitSprite(RenderType::guiTextured, SLOT_HIGHLIGHT_BACK_SPRITE, pX, pY, 24, 24);
        } else {
            pGuiGraphics.blitSprite(RenderType::guiTextured, SLOT_BACKGROUND_SPRITE, pX, pY, 24, 24);
        }

        pGuiGraphics.renderItem(itemstack, pX + 4, pY + 4, pSeed);
        pGuiGraphics.renderItemDecorations(pFont, itemstack, pX + 4, pY + 4);
        if (flag) {
            pGuiGraphics.blitSprite(RenderType::guiTexturedOverlay, SLOT_HIGHLIGHT_FRONT_SPRITE, pX, pY, 24, 24);
        }
    }

    private static void renderCount(int pSlotX, int pSlotY, int pCount, Font pFont, GuiGraphics pGuiGraphics) {
        pGuiGraphics.drawCenteredString(pFont, "+" + pCount, pSlotX + 12, pSlotY + 10, 16777215);
    }

    private void drawSelectedItemTooltip(Font pFont, GuiGraphics pGuiGraphics, int pX, int pY, int pWidth) {
        if (this.contents.hasSelectedItem()) {
            ItemStack itemstack = this.contents.getItemUnsafe(this.contents.getSelectedItem());
            Component component = itemstack.getStyledHoverName();
            int i = pFont.width(component.getVisualOrderText());
            int j = pX + pWidth / 2 - 12;
            pGuiGraphics.renderTooltip(pFont, component, j - i / 2, pY - 15, itemstack.get(DataComponents.TOOLTIP_STYLE));
        }
    }

    private void drawProgressbar(int pX, int pY, Font pFont, GuiGraphics pGuiGraphics) {
        pGuiGraphics.blitSprite(RenderType::guiTextured, this.getProgressBarTexture(), pX + 1, pY, this.getProgressBarFill(), 13);
        pGuiGraphics.blitSprite(RenderType::guiTextured, PROGRESSBAR_BORDER_SPRITE, pX, pY, 96, 13);
        Component component = this.getProgressBarFillText();
        if (component != null) {
            pGuiGraphics.drawCenteredString(pFont, component, pX + 48, pY + 3, 16777215);
        }
    }

    private static void drawEmptyBundleDescriptionText(int pX, int pY, Font pFont, GuiGraphics pGuiGraphics) {
        pGuiGraphics.drawWordWrap(pFont, BUNDLE_EMPTY_DESCRIPTION, pX, pY, 96, 11184810);
    }

    private static int getEmptyBundleDescriptionTextHeight(Font pFont) {
        return pFont.split(BUNDLE_EMPTY_DESCRIPTION, 96).size() * 9;
    }

    private int getProgressBarFill() {
        return Mth.clamp(Mth.mulAndTruncate(this.contents.weight(), 94), 0, 94);
    }

    private ResourceLocation getProgressBarTexture() {
        return this.contents.weight().compareTo(Fraction.ONE) >= 0 ? PROGRESSBAR_FULL_SPRITE : PROGRESSBAR_FILL_SPRITE;
    }

    @Nullable
    private Component getProgressBarFillText() {
        if (this.contents.isEmpty()) {
            return BUNDLE_EMPTY_TEXT;
        } else {
            return this.contents.weight().compareTo(Fraction.ONE) >= 0 ? BUNDLE_FULL_TEXT : null;
        }
    }
}