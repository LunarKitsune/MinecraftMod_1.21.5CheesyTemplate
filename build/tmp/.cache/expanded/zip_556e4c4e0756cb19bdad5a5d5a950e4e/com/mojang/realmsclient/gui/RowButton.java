package com.mojang.realmsclient.gui;

import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class RowButton {
    public final int width;
    public final int height;
    public final int xOffset;
    public final int yOffset;

    public RowButton(int pWidth, int pHeight, int pXOffset, int pYOffset) {
        this.width = pWidth;
        this.height = pHeight;
        this.xOffset = pXOffset;
        this.yOffset = pYOffset;
    }

    public void drawForRowAt(GuiGraphics pGuiGraphics, int pX, int pY, int pMouseX, int pMouseY) {
        int i = pX + this.xOffset;
        int j = pY + this.yOffset;
        boolean flag = pMouseX >= i && pMouseX <= i + this.width && pMouseY >= j && pMouseY <= j + this.height;
        this.draw(pGuiGraphics, i, j, flag);
    }

    protected abstract void draw(GuiGraphics pGuiGraphics, int pX, int pY, boolean pShowTooltip);

    public int getRight() {
        return this.xOffset + this.width;
    }

    public int getBottom() {
        return this.yOffset + this.height;
    }

    public abstract void onClick(int pIndex);

    public static void drawButtonsInRow(
        GuiGraphics pGuiGraphics, List<RowButton> pRowButtons, AbstractSelectionList<?> pPendingInvitations, int pX, int pY, int pMouseX, int pMouseY
    ) {
        for (RowButton rowbutton : pRowButtons) {
            if (pPendingInvitations.getRowWidth() > rowbutton.getRight()) {
                rowbutton.drawForRowAt(pGuiGraphics, pX, pY, pMouseX, pMouseY);
            }
        }
    }

    public static void rowButtonMouseClicked(
        AbstractSelectionList<?> pPendingInvitations, ObjectSelectionList.Entry<?> pEntry, List<RowButton> pRowButtons, int pButton, double pMouseX, double pMouseY
    ) {
        int i = pPendingInvitations.children().indexOf(pEntry);
        if (i > -1) {
            pPendingInvitations.setSelectedIndex(i);
            int j = pPendingInvitations.getRowLeft();
            int k = pPendingInvitations.getRowTop(i);
            int l = (int)(pMouseX - j);
            int i1 = (int)(pMouseY - k);

            for (RowButton rowbutton : pRowButtons) {
                if (l >= rowbutton.xOffset && l <= rowbutton.getRight() && i1 >= rowbutton.yOffset && i1 <= rowbutton.getBottom()) {
                    rowbutton.onClick(i);
                }
            }
        }
    }
}