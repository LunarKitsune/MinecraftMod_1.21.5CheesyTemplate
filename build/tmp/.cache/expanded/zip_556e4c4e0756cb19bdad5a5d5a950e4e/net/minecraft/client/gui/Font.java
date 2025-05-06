package net.minecraft.client.gui;

import com.google.common.collect.Lists;
import com.ibm.icu.text.ArabicShaping;
import com.ibm.icu.text.ArabicShapingException;
import com.ibm.icu.text.Bidi;
import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.gui.font.glyphs.EmptyGlyph;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringDecomposer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class Font implements net.minecraftforge.client.extensions.IForgeFont {
    private static final float EFFECT_DEPTH = 0.01F;
    public static final float SHADOW_DEPTH = 0.03F;
    public static final int NO_SHADOW = 0;
    public static final int ALPHA_CUTOFF = 8;
    public final int lineHeight = 9;
    public final RandomSource random = RandomSource.create();
    private final Function<ResourceLocation, FontSet> fonts;
    final boolean filterFishyGlyphs;
    private final StringSplitter splitter;

    public Font(Function<ResourceLocation, FontSet> pFonts, boolean pFilterFishyGlyphs) {
        this.fonts = pFonts;
        this.filterFishyGlyphs = pFilterFishyGlyphs;
        this.splitter = new StringSplitter(
            (p_92722_, p_92723_) -> this.getFontSet(p_92723_.getFont()).getGlyphInfo(p_92722_, this.filterFishyGlyphs).getAdvance(p_92723_.isBold())
        );
    }

    FontSet getFontSet(ResourceLocation pFontLocation) {
        return this.fonts.apply(pFontLocation);
    }

    public String bidirectionalShaping(String pText) {
        try {
            Bidi bidi = new Bidi(new ArabicShaping(8).shape(pText), 127);
            bidi.setReorderingMode(0);
            return bidi.writeReordered(2);
        } catch (ArabicShapingException arabicshapingexception) {
            return pText;
        }
    }

    public int drawInBatch(
        String pText,
        float pX,
        float pY,
        int pColor,
        boolean pDropShadow,
        Matrix4f pPose,
        MultiBufferSource pBufferSource,
        Font.DisplayMode pDisplayMode,
        int pBackgroundColor,
        int pPackedLightCoords
    ) {
        if (this.isBidirectional()) {
            pText = this.bidirectionalShaping(pText);
        }

        return this.drawInternal(pText, pX, pY, pColor, pDropShadow, pPose, pBufferSource, pDisplayMode, pBackgroundColor, pPackedLightCoords, true);
    }

    public int drawInBatch(
        Component pText,
        float pX,
        float pY,
        int pColor,
        boolean pDropShadow,
        Matrix4f pPose,
        MultiBufferSource pBufferSource,
        Font.DisplayMode pDisplayMode,
        int pBackgroundColor,
        int pPackedLightCoords
    ) {
        return this.drawInBatch(pText, pX, pY, pColor, pDropShadow, pPose, pBufferSource, pDisplayMode, pBackgroundColor, pPackedLightCoords, true);
    }

    public int drawInBatch(
        Component pText,
        float pX,
        float pY,
        int pColor,
        boolean pDropShadow,
        Matrix4f pPose,
        MultiBufferSource pBufferSource,
        Font.DisplayMode pDisplayMode,
        int pBackgroundColor,
        int pPackedLightCoords,
        boolean pInverseDepth
    ) {
        return this.drawInternal(pText.getVisualOrderText(), pX, pY, pColor, pDropShadow, pPose, pBufferSource, pDisplayMode, pBackgroundColor, pPackedLightCoords, pInverseDepth);
    }

    public int drawInBatch(
        FormattedCharSequence pText,
        float pX,
        float pY,
        int pColor,
        boolean pDropShadow,
        Matrix4f pPose,
        MultiBufferSource pBufferSource,
        Font.DisplayMode pDisplayMode,
        int pBackgroundColor,
        int pPackedLightCoords
    ) {
        return this.drawInternal(pText, pX, pY, pColor, pDropShadow, pPose, pBufferSource, pDisplayMode, pBackgroundColor, pPackedLightCoords, true);
    }

    public void drawInBatch8xOutline(
        FormattedCharSequence pText,
        float pX,
        float pY,
        int pColor,
        int pBackgroundColor,
        Matrix4f pPose,
        MultiBufferSource pBufferSource,
        int pPackedLightCoords
    ) {
        int i = adjustColor(pBackgroundColor);
        Font.StringRenderOutput font$stringrenderoutput = new Font.StringRenderOutput(
            pBufferSource, 0.0F, 0.0F, i, false, pPose, Font.DisplayMode.NORMAL, pPackedLightCoords
        );

        for (int j = -1; j <= 1; j++) {
            for (int k = -1; k <= 1; k++) {
                if (j != 0 || k != 0) {
                    float[] afloat = new float[]{pX};
                    int l = j;
                    int i1 = k;
                    pText.accept((p_168661_, p_168662_, p_168663_) -> {
                        boolean flag = p_168662_.isBold();
                        FontSet fontset = this.getFontSet(p_168662_.getFont());
                        GlyphInfo glyphinfo = fontset.getGlyphInfo(p_168663_, this.filterFishyGlyphs);
                        font$stringrenderoutput.x = afloat[0] + l * glyphinfo.getShadowOffset();
                        font$stringrenderoutput.y = pY + i1 * glyphinfo.getShadowOffset();
                        afloat[0] += glyphinfo.getAdvance(flag);
                        return font$stringrenderoutput.accept(p_168661_, p_168662_.withColor(i), p_168663_);
                    });
                }
            }
        }

        font$stringrenderoutput.renderCharacters();
        Font.StringRenderOutput font$stringrenderoutput1 = new Font.StringRenderOutput(
            pBufferSource, pX, pY, adjustColor(pColor), false, pPose, Font.DisplayMode.POLYGON_OFFSET, pPackedLightCoords
        );
        pText.accept(font$stringrenderoutput1);
        font$stringrenderoutput1.finish(pX);
    }

    private static int adjustColor(int pColor) {
        return (pColor & -67108864) == 0 ? ARGB.opaque(pColor) : pColor;
    }

    private int drawInternal(
        String pText,
        float pX,
        float pY,
        int pColor,
        boolean pDropShadow,
        Matrix4f pPose,
        MultiBufferSource pBufferSource,
        Font.DisplayMode pDisplayMode,
        int pBackgroundColor,
        int pPackedLightCoords,
        boolean pInverseDepth
    ) {
        pColor = adjustColor(pColor);
        pX = this.renderText(pText, pX, pY, pColor, pDropShadow, pPose, pBufferSource, pDisplayMode, pBackgroundColor, pPackedLightCoords, pInverseDepth);
        return (int)pX + (pDropShadow ? 1 : 0);
    }

    private int drawInternal(
        FormattedCharSequence pText,
        float pX,
        float pY,
        int pColor,
        boolean pDropShadow,
        Matrix4f pPose,
        MultiBufferSource pBufferSource,
        Font.DisplayMode pDisplayMode,
        int pBackgroundColor,
        int pPackedLightCoords,
        boolean pInverseDepth
    ) {
        pColor = adjustColor(pColor);
        pX = this.renderText(pText, pX, pY, pColor, pDropShadow, pPose, pBufferSource, pDisplayMode, pBackgroundColor, pPackedLightCoords, pInverseDepth);
        return (int)pX + (pDropShadow ? 1 : 0);
    }

    private float renderText(
        String pText,
        float pX,
        float pY,
        int pColor,
        boolean pDropShadow,
        Matrix4f pPose,
        MultiBufferSource pBufferSource,
        Font.DisplayMode pDisplayMode,
        int pBackgroundColor,
        int pPackedLightCoords,
        boolean pInverseDepth
    ) {
        Font.StringRenderOutput font$stringrenderoutput = new Font.StringRenderOutput(
            pBufferSource, pX, pY, pColor, pBackgroundColor, pDropShadow, pPose, pDisplayMode, pPackedLightCoords, pInverseDepth
        );
        StringDecomposer.iterateFormatted(pText, Style.EMPTY, font$stringrenderoutput);
        return font$stringrenderoutput.finish(pX);
    }

    private float renderText(
        FormattedCharSequence pText,
        float pX,
        float pY,
        int pColor,
        boolean pDropShadow,
        Matrix4f pPose,
        MultiBufferSource pBufferSource,
        Font.DisplayMode pDisplayMode,
        int pBackgroundColor,
        int pPackedLightCoords,
        boolean pInverseDepth
    ) {
        Font.StringRenderOutput font$stringrenderoutput = new Font.StringRenderOutput(
            pBufferSource, pX, pY, pColor, pBackgroundColor, pDropShadow, pPose, pDisplayMode, pPackedLightCoords, pInverseDepth
        );
        pText.accept(font$stringrenderoutput);
        return font$stringrenderoutput.finish(pX);
    }

    public int width(String pText) {
        return Mth.ceil(this.splitter.stringWidth(pText));
    }

    public int width(FormattedText pText) {
        return Mth.ceil(this.splitter.stringWidth(pText));
    }

    public int width(FormattedCharSequence pText) {
        return Mth.ceil(this.splitter.stringWidth(pText));
    }

    public String plainSubstrByWidth(String pText, int pMaxWidth, boolean pTail) {
        return pTail ? this.splitter.plainTailByWidth(pText, pMaxWidth, Style.EMPTY) : this.splitter.plainHeadByWidth(pText, pMaxWidth, Style.EMPTY);
    }

    public String plainSubstrByWidth(String pText, int pMaxWidth) {
        return this.splitter.plainHeadByWidth(pText, pMaxWidth, Style.EMPTY);
    }

    public FormattedText substrByWidth(FormattedText pText, int pMaxWidth) {
        return this.splitter.headByWidth(pText, pMaxWidth, Style.EMPTY);
    }

    public int wordWrapHeight(String pText, int pMaxWidth) {
        return 9 * this.splitter.splitLines(pText, pMaxWidth, Style.EMPTY).size();
    }

    public int wordWrapHeight(FormattedText pText, int pMaxWidth) {
        return 9 * this.splitter.splitLines(pText, pMaxWidth, Style.EMPTY).size();
    }

    public List<FormattedCharSequence> split(FormattedText pText, int pMaxWidth) {
        return Language.getInstance().getVisualOrder(this.splitter.splitLines(pText, pMaxWidth, Style.EMPTY));
    }

    public boolean isBidirectional() {
        return Language.getInstance().isDefaultRightToLeft();
    }

    public StringSplitter getSplitter() {
        return this.splitter;
    }

    @OnlyIn(Dist.CLIENT)
    public static enum DisplayMode {
        NORMAL,
        SEE_THROUGH,
        POLYGON_OFFSET;
    }

    @OnlyIn(Dist.CLIENT)
    class StringRenderOutput implements FormattedCharSink {
        final MultiBufferSource bufferSource;
        private final boolean drawShadow;
        private final int color;
        private final int backgroundColor;
        private final Matrix4f pose;
        private final Font.DisplayMode mode;
        private final int packedLightCoords;
        private final boolean inverseDepth;
        float x;
        float y;
        private final List<BakedGlyph.GlyphInstance> glyphInstances = new ArrayList<>();
        @Nullable
        private List<BakedGlyph.Effect> effects;

        private void addEffect(BakedGlyph.Effect pEffect) {
            if (this.effects == null) {
                this.effects = Lists.newArrayList();
            }

            this.effects.add(pEffect);
        }

        public StringRenderOutput(
            final MultiBufferSource pBufferSource,
            final float pX,
            final float pY,
            final int pColor,
            final boolean pDropShadow,
            final Matrix4f pPose,
            final Font.DisplayMode pMode,
            final int pPackedLightCoords
        ) {
            this(pBufferSource, pX, pY, pColor, 0, pDropShadow, pPose, pMode, pPackedLightCoords, true);
        }

        public StringRenderOutput(
            final MultiBufferSource pBuferSource,
            final float pX,
            final float pY,
            final int pColor,
            final int pBackgroundColor,
            final boolean pDropShadow,
            final Matrix4f pPose,
            final Font.DisplayMode pDisplayMode,
            final int pPackedLightCoords,
            final boolean pInverseDepth
        ) {
            this.bufferSource = pBuferSource;
            this.x = pX;
            this.y = pY;
            this.drawShadow = pDropShadow;
            this.color = pColor;
            this.backgroundColor = pBackgroundColor;
            this.pose = pPose;
            this.mode = pDisplayMode;
            this.packedLightCoords = pPackedLightCoords;
            this.inverseDepth = pInverseDepth;
        }

        @Override
        public boolean accept(int p_92967_, Style p_92968_, int p_92969_) {
            FontSet fontset = Font.this.getFontSet(p_92968_.getFont());
            GlyphInfo glyphinfo = fontset.getGlyphInfo(p_92969_, Font.this.filterFishyGlyphs);
            BakedGlyph bakedglyph = p_92968_.isObfuscated() && p_92969_ != 32 ? fontset.getRandomGlyph(glyphinfo) : fontset.getGlyph(p_92969_);
            boolean flag = p_92968_.isBold();
            TextColor textcolor = p_92968_.getColor();
            int i = this.getTextColor(textcolor);
            int j = this.getShadowColor(p_92968_, i);
            float f = glyphinfo.getAdvance(flag);
            float f1 = p_92967_ == 0 ? this.x - 1.0F : this.x;
            float f2 = glyphinfo.getShadowOffset();
            if (!(bakedglyph instanceof EmptyGlyph)) {
                float f3 = flag ? glyphinfo.getBoldOffset() : 0.0F;
                this.glyphInstances.add(new BakedGlyph.GlyphInstance(this.x, this.y, i, j, bakedglyph, p_92968_, f3, f2));
            }

            if (p_92968_.isStrikethrough()) {
                this.addEffect(new BakedGlyph.Effect(f1, this.y + 4.5F, this.x + f, this.y + 4.5F - 1.0F, this.getOverTextEffectDepth(), i, j, f2));
            }

            if (p_92968_.isUnderlined()) {
                this.addEffect(new BakedGlyph.Effect(f1, this.y + 9.0F, this.x + f, this.y + 9.0F - 1.0F, this.getOverTextEffectDepth(), i, j, f2));
            }

            this.x += f;
            return true;
        }

        float finish(float pX) {
            BakedGlyph bakedglyph = null;
            if (this.backgroundColor != 0) {
                BakedGlyph.Effect bakedglyph$effect = new BakedGlyph.Effect(
                    pX - 1.0F, this.y + 9.0F, this.x, this.y - 1.0F, this.getUnderTextEffectDepth(), this.backgroundColor
                );
                bakedglyph = Font.this.getFontSet(Style.DEFAULT_FONT).whiteGlyph();
                VertexConsumer vertexconsumer = this.bufferSource.getBuffer(bakedglyph.renderType(this.mode));
                bakedglyph.renderEffect(bakedglyph$effect, this.pose, vertexconsumer, this.packedLightCoords);
            }

            this.renderCharacters();
            if (this.effects != null) {
                if (bakedglyph == null) {
                    bakedglyph = Font.this.getFontSet(Style.DEFAULT_FONT).whiteGlyph();
                }

                VertexConsumer vertexconsumer1 = this.bufferSource.getBuffer(bakedglyph.renderType(this.mode));

                for (BakedGlyph.Effect bakedglyph$effect1 : this.effects) {
                    bakedglyph.renderEffect(bakedglyph$effect1, this.pose, vertexconsumer1, this.packedLightCoords);
                }
            }

            return this.x;
        }

        private int getTextColor(@Nullable TextColor pTextColor) {
            if (pTextColor != null) {
                int i = ARGB.alpha(this.color);
                int j = pTextColor.getValue();
                return ARGB.color(i, j);
            } else {
                return this.color;
            }
        }

        private int getShadowColor(Style pStyle, int pTextColor) {
            Integer integer = pStyle.getShadowColor();
            if (integer != null) {
                float f = ARGB.alphaFloat(pTextColor);
                float f1 = ARGB.alphaFloat(integer);
                return f != 1.0F ? ARGB.color(ARGB.as8BitChannel(f * f1), integer) : integer;
            } else {
                return this.drawShadow ? ARGB.scaleRGB(pTextColor, 0.25F) : 0;
            }
        }

        void renderCharacters() {
            for (BakedGlyph.GlyphInstance bakedglyph$glyphinstance : this.glyphInstances) {
                BakedGlyph bakedglyph = bakedglyph$glyphinstance.glyph();
                VertexConsumer vertexconsumer = this.bufferSource.getBuffer(bakedglyph.renderType(this.mode));
                bakedglyph.renderChar(bakedglyph$glyphinstance, this.pose, vertexconsumer, this.packedLightCoords);
            }
        }

        private float getOverTextEffectDepth() {
            return this.inverseDepth ? 0.01F : -0.01F;
        }

        private float getUnderTextEffectDepth() {
            return this.inverseDepth ? -0.01F : 0.01F;
        }
    }
}
