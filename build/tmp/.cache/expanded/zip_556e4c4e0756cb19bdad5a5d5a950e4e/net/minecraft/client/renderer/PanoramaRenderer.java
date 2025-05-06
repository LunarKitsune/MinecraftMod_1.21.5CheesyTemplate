package net.minecraft.client.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PanoramaRenderer {
    public static final ResourceLocation PANORAMA_OVERLAY = ResourceLocation.withDefaultNamespace("textures/gui/title/background/panorama_overlay.png");
    private final Minecraft minecraft;
    private final CubeMap cubeMap;
    private float spin;

    public PanoramaRenderer(CubeMap pCubeMap) {
        this.cubeMap = pCubeMap;
        this.minecraft = Minecraft.getInstance();
    }

    public void render(GuiGraphics pGuiGraphics, int pWidth, int pHeight, float pFade, float pPartialTick) {
        float f = this.minecraft.getDeltaTracker().getRealtimeDeltaTicks();
        float f1 = (float)(f * this.minecraft.options.panoramaSpeed().get());
        this.spin = wrap(this.spin + f1 * 0.1F, 360.0F);
        pGuiGraphics.flush();
        this.cubeMap.render(this.minecraft, 10.0F, -this.spin, pFade);
        pGuiGraphics.flush();
        pGuiGraphics.blit(RenderType::guiTextured, PANORAMA_OVERLAY, 0, 0, 0.0F, 0.0F, pWidth, pHeight, 16, 128, 16, 128, ARGB.white(pFade));
    }

    private static float wrap(float pValue, float pMax) {
        return pValue > pMax ? pValue - pMax : pValue;
    }
}