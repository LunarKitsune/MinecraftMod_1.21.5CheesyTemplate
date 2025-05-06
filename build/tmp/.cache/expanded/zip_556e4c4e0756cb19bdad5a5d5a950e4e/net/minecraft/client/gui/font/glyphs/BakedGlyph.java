package net.minecraft.client.gui.font.glyphs;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.GlyphRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Style;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class BakedGlyph {
    public static final float Z_FIGHTER = 0.001F;
    private final GlyphRenderTypes renderTypes;
    private final float u0;
    private final float u1;
    private final float v0;
    private final float v1;
    private final float left;
    private final float right;
    private final float up;
    private final float down;

    public BakedGlyph(
        GlyphRenderTypes pRenderTypes,
        float pU0,
        float pU1,
        float pV0,
        float pV1,
        float pLeft,
        float pRight,
        float pUp,
        float pDown
    ) {
        this.renderTypes = pRenderTypes;
        this.u0 = pU0;
        this.u1 = pU1;
        this.v0 = pV0;
        this.v1 = pV1;
        this.left = pLeft;
        this.right = pRight;
        this.up = pUp;
        this.down = pDown;
    }

    public void renderChar(BakedGlyph.GlyphInstance pGlyph, Matrix4f pPose, VertexConsumer pBuffer, int pPackedLight) {
        Style style = pGlyph.style();
        boolean flag = style.isItalic();
        float f = pGlyph.x();
        float f1 = pGlyph.y();
        int i = pGlyph.color();
        int j = pGlyph.shadowColor();
        boolean flag1 = style.isBold();
        if (pGlyph.hasShadow()) {
            this.render(flag, f + pGlyph.shadowOffset(), f1 + pGlyph.shadowOffset(), pPose, pBuffer, j, flag1, pPackedLight);
            this.render(flag, f, f1, 0.03F, pPose, pBuffer, i, flag1, pPackedLight);
        } else {
            this.render(flag, f, f1, pPose, pBuffer, i, flag1, pPackedLight);
        }

        if (flag1) {
            if (pGlyph.hasShadow()) {
                this.render(
                    flag, f + pGlyph.boldOffset() + pGlyph.shadowOffset(), f1 + pGlyph.shadowOffset(), 0.001F, pPose, pBuffer, j, true, pPackedLight
                );
                this.render(flag, f + pGlyph.boldOffset(), f1, 0.03F, pPose, pBuffer, i, true, pPackedLight);
            } else {
                this.render(flag, f + pGlyph.boldOffset(), f1, pPose, pBuffer, i, true, pPackedLight);
            }
        }
    }

    private void render(
        boolean pItalic, float pX, float pY, Matrix4f pPose, VertexConsumer pBuffer, int pColor, boolean pBold, int pPackedLight
    ) {
        this.render(pItalic, pX, pY, 0.0F, pPose, pBuffer, pColor, pBold, pPackedLight);
    }

    private void render(
        boolean pItalic,
        float pX,
        float pY,
        float pZ,
        Matrix4f pPose,
        VertexConsumer pBuffer,
        int pColor,
        boolean pBold,
        int pPackedLight
    ) {
        float f = pX + this.left;
        float f1 = pX + this.right;
        float f2 = pY + this.up;
        float f3 = pY + this.down;
        float f4 = pItalic ? 1.0F - 0.25F * this.up : 0.0F;
        float f5 = pItalic ? 1.0F - 0.25F * this.down : 0.0F;
        float f6 = pBold ? 0.1F : 0.0F;
        pBuffer.addVertex(pPose, f + f4 - f6, f2 - f6, pZ).setColor(pColor).setUv(this.u0, this.v0).setLight(pPackedLight);
        pBuffer.addVertex(pPose, f + f5 - f6, f3 + f6, pZ).setColor(pColor).setUv(this.u0, this.v1).setLight(pPackedLight);
        pBuffer.addVertex(pPose, f1 + f5 + f6, f3 + f6, pZ).setColor(pColor).setUv(this.u1, this.v1).setLight(pPackedLight);
        pBuffer.addVertex(pPose, f1 + f4 + f6, f2 - f6, pZ).setColor(pColor).setUv(this.u1, this.v0).setLight(pPackedLight);
    }

    public void renderEffect(BakedGlyph.Effect pEffect, Matrix4f pPose, VertexConsumer pBuffer, int pPackedLight) {
        if (pEffect.hasShadow()) {
            this.buildEffect(pEffect, pEffect.shadowOffset(), 0.0F, pEffect.shadowColor(), pBuffer, pPackedLight, pPose);
            this.buildEffect(pEffect, 0.0F, 0.03F, pEffect.color, pBuffer, pPackedLight, pPose);
        } else {
            this.buildEffect(pEffect, 0.0F, 0.0F, pEffect.color, pBuffer, pPackedLight, pPose);
        }
    }

    private void buildEffect(
        BakedGlyph.Effect pEffect, float pShadowOffset, float pDepthOffset, int pShadowColor, VertexConsumer pBuffer, int pPackedLight, Matrix4f pPose
    ) {
        pBuffer.addVertex(pPose, pEffect.x0 + pShadowOffset, pEffect.y0 + pShadowOffset, pEffect.depth + pDepthOffset)
            .setColor(pShadowColor)
            .setUv(this.u0, this.v0)
            .setLight(pPackedLight);
        pBuffer.addVertex(pPose, pEffect.x1 + pShadowOffset, pEffect.y0 + pShadowOffset, pEffect.depth + pDepthOffset)
            .setColor(pShadowColor)
            .setUv(this.u0, this.v1)
            .setLight(pPackedLight);
        pBuffer.addVertex(pPose, pEffect.x1 + pShadowOffset, pEffect.y1 + pShadowOffset, pEffect.depth + pDepthOffset)
            .setColor(pShadowColor)
            .setUv(this.u1, this.v1)
            .setLight(pPackedLight);
        pBuffer.addVertex(pPose, pEffect.x0 + pShadowOffset, pEffect.y1 + pShadowOffset, pEffect.depth + pDepthOffset)
            .setColor(pShadowColor)
            .setUv(this.u1, this.v0)
            .setLight(pPackedLight);
    }

    public RenderType renderType(Font.DisplayMode pDisplayMode) {
        return this.renderTypes.select(pDisplayMode);
    }

    @OnlyIn(Dist.CLIENT)
    public record Effect(float x0, float y0, float x1, float y1, float depth, int color, int shadowColor, float shadowOffset) {
        public Effect(float pX0, float pY0, float pX1, float pY1, float pDepth, int pColor) {
            this(pX0, pY0, pX1, pY1, pDepth, pColor, 0, 0.0F);
        }

        boolean hasShadow() {
            return this.shadowColor() != 0;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public record GlyphInstance(
        float x, float y, int color, int shadowColor, BakedGlyph glyph, Style style, float boldOffset, float shadowOffset
    ) {
        boolean hasShadow() {
            return this.shadowColor() != 0;
        }
    }
}