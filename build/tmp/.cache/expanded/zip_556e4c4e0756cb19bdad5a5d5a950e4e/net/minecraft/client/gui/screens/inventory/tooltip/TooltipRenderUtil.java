package net.minecraft.client.gui.screens.inventory.tooltip;

import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TooltipRenderUtil {
    private static final ResourceLocation BACKGROUND_SPRITE = ResourceLocation.withDefaultNamespace("tooltip/background");
    private static final ResourceLocation FRAME_SPRITE = ResourceLocation.withDefaultNamespace("tooltip/frame");
    public static final int MOUSE_OFFSET = 12;
    private static final int PADDING = 3;
    public static final int PADDING_LEFT = 3;
    public static final int PADDING_RIGHT = 3;
    public static final int PADDING_TOP = 3;
    public static final int PADDING_BOTTOM = 3;
    private static final int MARGIN = 9;

    public static void renderTooltipBackground(
        GuiGraphics pGuiGraphics, int pX, int pY, int pWidth, int pHeight, int pZ, @Nullable ResourceLocation pSprite
    ) {
        int i = pX - 3 - 9;
        int j = pY - 3 - 9;
        int k = pWidth + 3 + 3 + 18;
        int l = pHeight + 3 + 3 + 18;
        pGuiGraphics.pose().pushPose();
        pGuiGraphics.pose().translate(0.0F, 0.0F, pZ);
        pGuiGraphics.blitSprite(RenderType::guiTextured, getBackgroundSprite(pSprite), i, j, k, l);
        pGuiGraphics.blitSprite(RenderType::guiTextured, getFrameSprite(pSprite), i, j, k, l);
        pGuiGraphics.pose().popPose();
    }

    private static ResourceLocation getBackgroundSprite(@Nullable ResourceLocation pName) {
        return pName == null ? BACKGROUND_SPRITE : pName.withPath(p_362641_ -> "tooltip/" + p_362641_ + "_background");
    }

    private static ResourceLocation getFrameSprite(@Nullable ResourceLocation pName) {
        return pName == null ? FRAME_SPRITE : pName.withPath(p_364578_ -> "tooltip/" + p_364578_ + "_frame");
    }
}