package net.minecraft.client.renderer.entity.state;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record HitboxRenderState(
    double x0,
    double y0,
    double z0,
    double x1,
    double y1,
    double z1,
    float offsetX,
    float offsetY,
    float offsetZ,
    float red,
    float green,
    float blue
) {
    public HitboxRenderState(
        double pX0,
        double pY0,
        double pZ0,
        double pX1,
        double pY1,
        double pZ1,
        float pRed,
        float pGreen,
        float pBlue
    ) {
        this(pX0, pY0, pZ0, pX1, pY1, pZ1, 0.0F, 0.0F, 0.0F, pRed, pGreen, pBlue);
    }
}