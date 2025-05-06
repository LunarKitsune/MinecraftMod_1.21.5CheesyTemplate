package net.minecraft.client.gui.font;

import com.mojang.blaze3d.font.SheetGlyphInfo;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.TextureFormat;
import java.nio.file.Path;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.Dumpable;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FontTexture extends AbstractTexture implements Dumpable {
    private static final int SIZE = 256;
    private final GlyphRenderTypes renderTypes;
    private final boolean colored;
    private final FontTexture.Node root;

    public FontTexture(Supplier<String> pLabel, GlyphRenderTypes pRenderTypes, boolean pColored) {
        this.colored = pColored;
        this.root = new FontTexture.Node(0, 0, 256, 256);
        this.texture = RenderSystem.getDevice().createTexture(pLabel, pColored ? TextureFormat.RGBA8 : TextureFormat.RED8, 256, 256, 1);
        this.texture.setTextureFilter(FilterMode.NEAREST, false);
        this.renderTypes = pRenderTypes;
    }

    @Nullable
    public BakedGlyph add(SheetGlyphInfo pGlyphInfo) {
        if (pGlyphInfo.isColored() != this.colored) {
            return null;
        } else {
            FontTexture.Node fonttexture$node = this.root.insert(pGlyphInfo);
            if (fonttexture$node != null && this.texture != null) {
                pGlyphInfo.upload(fonttexture$node.x, fonttexture$node.y, this.texture);
                float f = 256.0F;
                float f1 = 256.0F;
                float f2 = 0.01F;
                return new BakedGlyph(
                    this.renderTypes,
                    (fonttexture$node.x + 0.01F) / 256.0F,
                    (fonttexture$node.x - 0.01F + pGlyphInfo.getPixelWidth()) / 256.0F,
                    (fonttexture$node.y + 0.01F) / 256.0F,
                    (fonttexture$node.y - 0.01F + pGlyphInfo.getPixelHeight()) / 256.0F,
                    pGlyphInfo.getLeft(),
                    pGlyphInfo.getRight(),
                    pGlyphInfo.getTop(),
                    pGlyphInfo.getBottom()
                );
            } else {
                return null;
            }
        }
    }

    @Override
    public void dumpContents(ResourceLocation p_285121_, Path p_285511_) {
        if (this.texture != null) {
            String s = p_285121_.toDebugFileName();
            TextureUtil.writeAsPNG(p_285511_, s, this.texture, 0, p_285145_ -> (p_285145_ & 0xFF000000) == 0 ? -16777216 : p_285145_);
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class Node {
        final int x;
        final int y;
        private final int width;
        private final int height;
        @Nullable
        private FontTexture.Node left;
        @Nullable
        private FontTexture.Node right;
        private boolean occupied;

        Node(int pX, int pY, int pWidth, int pHeight) {
            this.x = pX;
            this.y = pY;
            this.width = pWidth;
            this.height = pHeight;
        }

        @Nullable
        FontTexture.Node insert(SheetGlyphInfo pGlyphInfo) {
            if (this.left != null && this.right != null) {
                FontTexture.Node fonttexture$node = this.left.insert(pGlyphInfo);
                if (fonttexture$node == null) {
                    fonttexture$node = this.right.insert(pGlyphInfo);
                }

                return fonttexture$node;
            } else if (this.occupied) {
                return null;
            } else {
                int i = pGlyphInfo.getPixelWidth();
                int j = pGlyphInfo.getPixelHeight();
                if (i > this.width || j > this.height) {
                    return null;
                } else if (i == this.width && j == this.height) {
                    this.occupied = true;
                    return this;
                } else {
                    int k = this.width - i;
                    int l = this.height - j;
                    if (k > l) {
                        this.left = new FontTexture.Node(this.x, this.y, i, this.height);
                        this.right = new FontTexture.Node(this.x + i + 1, this.y, this.width - i - 1, this.height);
                    } else {
                        this.left = new FontTexture.Node(this.x, this.y, this.width, j);
                        this.right = new FontTexture.Node(this.x, this.y + j + 1, this.width, this.height - j - 1);
                    }

                    return this.left.insert(pGlyphInfo);
                }
            }
        }
    }
}