package net.minecraft.client.gui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.tabs.Tab;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TabButton extends AbstractWidget {
    private static final WidgetSprites SPRITES = new WidgetSprites(
        ResourceLocation.withDefaultNamespace("widget/tab_selected"),
        ResourceLocation.withDefaultNamespace("widget/tab"),
        ResourceLocation.withDefaultNamespace("widget/tab_selected_highlighted"),
        ResourceLocation.withDefaultNamespace("widget/tab_highlighted")
    );
    private static final int SELECTED_OFFSET = 3;
    private static final int TEXT_MARGIN = 1;
    private static final int UNDERLINE_HEIGHT = 1;
    private static final int UNDERLINE_MARGIN_X = 4;
    private static final int UNDERLINE_MARGIN_BOTTOM = 2;
    private final TabManager tabManager;
    private final Tab tab;

    public TabButton(TabManager pTabManager, Tab pTab, int pWidth, int pHeight) {
        super(0, 0, pWidth, pHeight, pTab.getTabTitle());
        this.tabManager = pTabManager;
        this.tab = pTab;
    }

    @Override
    public void renderWidget(GuiGraphics p_283350_, int p_283437_, int p_281595_, float p_282117_) {
        p_283350_.blitSprite(
            RenderType::guiTextured, SPRITES.get(this.isSelected(), this.isHoveredOrFocused()), this.getX(), this.getY(), this.width, this.height
        );
        Font font = Minecraft.getInstance().font;
        int i = this.active ? -1 : -6250336;
        this.renderString(p_283350_, font, i);
        if (this.isSelected()) {
            this.renderMenuBackground(p_283350_, this.getX() + 2, this.getY() + 2, this.getRight() - 2, this.getBottom());
            this.renderFocusUnderline(p_283350_, font, i);
        }
    }

    protected void renderMenuBackground(GuiGraphics pGuiGraphics, int pMinX, int pMinY, int pMaxX, int pMaxY) {
        Screen.renderMenuBackgroundTexture(pGuiGraphics, Screen.MENU_BACKGROUND, pMinX, pMinY, 0.0F, 0.0F, pMaxX - pMinX, pMaxY - pMinY);
    }

    public void renderString(GuiGraphics pGuiGraphics, Font pFont, int pColor) {
        int i = this.getX() + 1;
        int j = this.getY() + (this.isSelected() ? 0 : 3);
        int k = this.getX() + this.getWidth() - 1;
        int l = this.getY() + this.getHeight();
        renderScrollingString(pGuiGraphics, pFont, this.getMessage(), i, j, k, l, pColor);
    }

    private void renderFocusUnderline(GuiGraphics pGuiGraphics, Font pFont, int pColor) {
        int i = Math.min(pFont.width(this.getMessage()), this.getWidth() - 4);
        int j = this.getX() + (this.getWidth() - i) / 2;
        int k = this.getY() + this.getHeight() - 2;
        pGuiGraphics.fill(j, k, j + i, k + 1, pColor);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput p_275465_) {
        p_275465_.add(NarratedElementType.TITLE, Component.translatable("gui.narrate.tab", this.tab.getTabTitle()));
    }

    @Override
    public void playDownSound(SoundManager p_276302_) {
    }

    public Tab tab() {
        return this.tab;
    }

    public boolean isSelected() {
        return this.tabManager.getCurrentTab() == this.tab;
    }
}