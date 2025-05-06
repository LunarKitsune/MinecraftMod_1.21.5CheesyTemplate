package com.mojang.blaze3d.font;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Locale;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.client.gui.font.CodepointMap;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.gui.font.providers.FreeTypeUtil;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.freetype.FT_Bitmap;
import org.lwjgl.util.freetype.FT_Face;
import org.lwjgl.util.freetype.FT_GlyphSlot;
import org.lwjgl.util.freetype.FT_Vector;
import org.lwjgl.util.freetype.FreeType;

@OnlyIn(Dist.CLIENT)
public class TrueTypeGlyphProvider implements GlyphProvider {
    @Nullable
    private ByteBuffer fontMemory;
    @Nullable
    private FT_Face face;
    final float oversample;
    private final CodepointMap<TrueTypeGlyphProvider.GlyphEntry> glyphs = new CodepointMap<>(
        TrueTypeGlyphProvider.GlyphEntry[]::new, TrueTypeGlyphProvider.GlyphEntry[][]::new
    );

    public TrueTypeGlyphProvider(ByteBuffer pFontMemory, FT_Face pFace, float pSize, float pOversample, float pShiftX, float pShiftY, String pSkip) {
        this.fontMemory = pFontMemory;
        this.face = pFace;
        this.oversample = pOversample;
        IntSet intset = new IntArraySet();
        pSkip.codePoints().forEach(intset::add);
        int i = Math.round(pSize * pOversample);
        FreeType.FT_Set_Pixel_Sizes(pFace, i, i);
        float f = pShiftX * pOversample;
        float f1 = -pShiftY * pOversample;

        try (MemoryStack memorystack = MemoryStack.stackPush()) {
            FT_Vector ft_vector = FreeTypeUtil.setVector(FT_Vector.malloc(memorystack), f, f1);
            FreeType.FT_Set_Transform(pFace, null, ft_vector);
            IntBuffer intbuffer = memorystack.mallocInt(1);
            int j = (int)FreeType.FT_Get_First_Char(pFace, intbuffer);

            while (true) {
                int k = intbuffer.get(0);
                if (k == 0) {
                    return;
                }

                if (!intset.contains(j)) {
                    this.glyphs.put(j, new TrueTypeGlyphProvider.GlyphEntry(k));
                }

                j = (int)FreeType.FT_Get_Next_Char(pFace, j, intbuffer);
            }
        }
    }

    @Nullable
    @Override
    public GlyphInfo getGlyph(int p_231116_) {
        TrueTypeGlyphProvider.GlyphEntry truetypeglyphprovider$glyphentry = this.glyphs.get(p_231116_);
        return truetypeglyphprovider$glyphentry != null ? this.getOrLoadGlyphInfo(p_231116_, truetypeglyphprovider$glyphentry) : null;
    }

    private GlyphInfo getOrLoadGlyphInfo(int pCharacter, TrueTypeGlyphProvider.GlyphEntry pGlyphEntry) {
        GlyphInfo glyphinfo = pGlyphEntry.glyph;
        if (glyphinfo == null) {
            FT_Face ft_face = this.validateFontOpen();
            synchronized (ft_face) {
                glyphinfo = pGlyphEntry.glyph;
                if (glyphinfo == null) {
                    glyphinfo = this.loadGlyph(pCharacter, ft_face, pGlyphEntry.index);
                    pGlyphEntry.glyph = glyphinfo;
                }
            }
        }

        return glyphinfo;
    }

    private GlyphInfo loadGlyph(int pCharacter, FT_Face pFace, int pIndex) {
        int i = FreeType.FT_Load_Glyph(pFace, pIndex, 4194312);
        if (i != 0) {
            FreeTypeUtil.assertError(i, String.format(Locale.ROOT, "Loading glyph U+%06X", pCharacter));
        }

        FT_GlyphSlot ft_glyphslot = pFace.glyph();
        if (ft_glyphslot == null) {
            throw new NullPointerException(String.format(Locale.ROOT, "Glyph U+%06X not initialized", pCharacter));
        } else {
            float f = FreeTypeUtil.x(ft_glyphslot.advance());
            FT_Bitmap ft_bitmap = ft_glyphslot.bitmap();
            int j = ft_glyphslot.bitmap_left();
            int k = ft_glyphslot.bitmap_top();
            int l = ft_bitmap.width();
            int i1 = ft_bitmap.rows();
            return (GlyphInfo)(l > 0 && i1 > 0 ? new TrueTypeGlyphProvider.Glyph(j, k, l, i1, f, pIndex) : (GlyphInfo.SpaceGlyphInfo)() -> f / this.oversample);
        }
    }

    FT_Face validateFontOpen() {
        if (this.fontMemory != null && this.face != null) {
            return this.face;
        } else {
            throw new IllegalStateException("Provider already closed");
        }
    }

    @Override
    public void close() {
        if (this.face != null) {
            synchronized (FreeTypeUtil.LIBRARY_LOCK) {
                FreeTypeUtil.checkError(FreeType.FT_Done_Face(this.face), "Deleting face");
            }

            this.face = null;
        }

        MemoryUtil.memFree(this.fontMemory);
        this.fontMemory = null;
    }

    @Override
    public IntSet getSupportedGlyphs() {
        return this.glyphs.keySet();
    }

    @OnlyIn(Dist.CLIENT)
    class Glyph implements GlyphInfo {
        final int width;
        final int height;
        final float bearingX;
        final float bearingY;
        private final float advance;
        final int index;

        Glyph(final float pBearingX, final float pBearingY, final int pWidth, final int pHeight, final float pAdvance, final int pIndex) {
            this.width = pWidth;
            this.height = pHeight;
            this.advance = pAdvance / TrueTypeGlyphProvider.this.oversample;
            this.bearingX = pBearingX / TrueTypeGlyphProvider.this.oversample;
            this.bearingY = pBearingY / TrueTypeGlyphProvider.this.oversample;
            this.index = pIndex;
        }

        @Override
        public float getAdvance() {
            return this.advance;
        }

        @Override
        public BakedGlyph bake(Function<SheetGlyphInfo, BakedGlyph> p_231120_) {
            return p_231120_.apply(
                new SheetGlyphInfo() {
                    @Override
                    public int getPixelWidth() {
                        return Glyph.this.width;
                    }

                    @Override
                    public int getPixelHeight() {
                        return Glyph.this.height;
                    }

                    @Override
                    public float getOversample() {
                        return TrueTypeGlyphProvider.this.oversample;
                    }

                    @Override
                    public float getBearingLeft() {
                        return Glyph.this.bearingX;
                    }

                    @Override
                    public float getBearingTop() {
                        return Glyph.this.bearingY;
                    }

                    @Override
                    public void upload(int p_231126_, int p_231127_, GpuTexture p_391563_) {
                        FT_Face ft_face = TrueTypeGlyphProvider.this.validateFontOpen();

                        try (NativeImage nativeimage = new NativeImage(NativeImage.Format.LUMINANCE, Glyph.this.width, Glyph.this.height, false)) {
                            if (nativeimage.copyFromFont(ft_face, Glyph.this.index)) {
                                RenderSystem.getDevice()
                                    .createCommandEncoder()
                                    .writeToTexture(p_391563_, nativeimage, 0, p_231126_, p_231127_, Glyph.this.width, Glyph.this.height, 0, 0);
                            }
                        }
                    }

                    @Override
                    public boolean isColored() {
                        return false;
                    }
                }
            );
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class GlyphEntry {
        final int index;
        @Nullable
        volatile GlyphInfo glyph;

        GlyphEntry(int pIndex) {
            this.index = pIndex;
        }
    }
}