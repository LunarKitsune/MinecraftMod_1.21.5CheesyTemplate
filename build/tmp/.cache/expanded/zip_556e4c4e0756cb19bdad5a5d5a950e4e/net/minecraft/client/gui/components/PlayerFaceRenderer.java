package net.minecraft.client.gui.components;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PlayerFaceRenderer {
    public static final int SKIN_HEAD_U = 8;
    public static final int SKIN_HEAD_V = 8;
    public static final int SKIN_HEAD_WIDTH = 8;
    public static final int SKIN_HEAD_HEIGHT = 8;
    public static final int SKIN_HAT_U = 40;
    public static final int SKIN_HAT_V = 8;
    public static final int SKIN_HAT_WIDTH = 8;
    public static final int SKIN_HAT_HEIGHT = 8;
    public static final int SKIN_TEX_WIDTH = 64;
    public static final int SKIN_TEX_HEIGHT = 64;

    public static void draw(GuiGraphics pGuiGraphics, PlayerSkin pSkin, int pX, int pY, int pSize) {
        draw(pGuiGraphics, pSkin, pX, pY, pSize, -1);
    }

    public static void draw(GuiGraphics pGuiGraphics, PlayerSkin pSkin, int pX, int pY, int pSize, int pColor) {
        draw(pGuiGraphics, pSkin.texture(), pX, pY, pSize, true, false, pColor);
    }

    public static void draw(
        GuiGraphics pGuiGraphics, ResourceLocation pSkinTexture, int pX, int pY, int pSize, boolean pDrawHat, boolean pUpsideDown, int pColor
    ) {
        int i = 8 + (pUpsideDown ? 8 : 0);
        int j = 8 * (pUpsideDown ? -1 : 1);
        pGuiGraphics.blit(RenderType::guiTextured, pSkinTexture, pX, pY, 8.0F, i, pSize, pSize, 8, j, 64, 64, pColor);
        if (pDrawHat) {
            drawHat(pGuiGraphics, pSkinTexture, pX, pY, pSize, pUpsideDown, pColor);
        }
    }

    private static void drawHat(
        GuiGraphics pGuiGraphics, ResourceLocation pSkinTexture, int pX, int pY, int pSize, boolean pUpsideDown, int pColor
    ) {
        int i = 8 + (pUpsideDown ? 8 : 0);
        int j = 8 * (pUpsideDown ? -1 : 1);
        pGuiGraphics.blit(RenderType::guiTextured, pSkinTexture, pX, pY, 40.0F, i, pSize, pSize, 8, j, 64, 64, pColor);
    }
}