package net.minecraft.client.gui.components;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class StringWidget extends AbstractStringWidget {
    private float alignX = 0.5F;

    public StringWidget(Component pMessage, Font pFont) {
        this(0, 0, pFont.width(pMessage.getVisualOrderText()), 9, pMessage, pFont);
    }

    public StringWidget(int pWidth, int pHeight, Component pMessage, Font pFont) {
        this(0, 0, pWidth, pHeight, pMessage, pFont);
    }

    public StringWidget(int p_268199_, int p_268137_, int p_268178_, int p_268169_, Component p_268285_, Font p_268047_) {
        super(p_268199_, p_268137_, p_268178_, p_268169_, p_268285_, p_268047_);
        this.active = false;
    }

    public StringWidget setColor(int p_270680_) {
        super.setColor(p_270680_);
        return this;
    }

    private StringWidget horizontalAlignment(float pHorizontalAlignment) {
        this.alignX = pHorizontalAlignment;
        return this;
    }

    public StringWidget alignLeft() {
        return this.horizontalAlignment(0.0F);
    }

    public StringWidget alignCenter() {
        return this.horizontalAlignment(0.5F);
    }

    public StringWidget alignRight() {
        return this.horizontalAlignment(1.0F);
    }

    @Override
    public void renderWidget(GuiGraphics p_281367_, int p_268221_, int p_268001_, float p_268214_) {
        Component component = this.getMessage();
        Font font = this.getFont();
        int i = this.getWidth();
        int j = font.width(component);
        int k = this.getX() + Math.round(this.alignX * (i - j));
        int l = this.getY() + (this.getHeight() - 9) / 2;
        FormattedCharSequence formattedcharsequence = j > i ? this.clipText(component, i) : component.getVisualOrderText();
        p_281367_.drawString(font, formattedcharsequence, k, l, this.getColor());
    }

    private FormattedCharSequence clipText(Component pMessage, int pWidth) {
        Font font = this.getFont();
        FormattedText formattedtext = font.substrByWidth(pMessage, pWidth - font.width(CommonComponents.ELLIPSIS));
        return Language.getInstance().getVisualOrder(FormattedText.composite(formattedtext, CommonComponents.ELLIPSIS));
    }
}