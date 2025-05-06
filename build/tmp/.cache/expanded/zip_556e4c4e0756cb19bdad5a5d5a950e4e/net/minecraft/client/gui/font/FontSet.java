package net.minecraft.client.gui.font;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.font.GlyphProvider;
import com.mojang.blaze3d.font.SheetGlyphInfo;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.IntFunction;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.gui.font.glyphs.SpecialGlyphs;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class FontSet implements AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final RandomSource RANDOM = RandomSource.create();
    private static final float LARGE_FORWARD_ADVANCE = 32.0F;
    private final TextureManager textureManager;
    private final ResourceLocation name;
    private BakedGlyph missingGlyph;
    private BakedGlyph whiteGlyph;
    private List<GlyphProvider.Conditional> allProviders = List.of();
    private List<GlyphProvider> activeProviders = List.of();
    private final CodepointMap<BakedGlyph> glyphs = new CodepointMap<>(BakedGlyph[]::new, BakedGlyph[][]::new);
    private final CodepointMap<FontSet.GlyphInfoFilter> glyphInfos = new CodepointMap<>(FontSet.GlyphInfoFilter[]::new, FontSet.GlyphInfoFilter[][]::new);
    private final Int2ObjectMap<IntList> glyphsByWidth = new Int2ObjectOpenHashMap<>();
    private final List<FontTexture> textures = Lists.newArrayList();
    private final IntFunction<FontSet.GlyphInfoFilter> glyphInfoGetter = this::computeGlyphInfo;
    private final IntFunction<BakedGlyph> glyphGetter = this::computeBakedGlyph;

    public FontSet(TextureManager pTextureManager, ResourceLocation pName) {
        this.textureManager = pTextureManager;
        this.name = pName;
    }

    public void reload(List<GlyphProvider.Conditional> pAllProviders, Set<FontOption> pOptions) {
        this.allProviders = pAllProviders;
        this.reload(pOptions);
    }

    public void reload(Set<FontOption> pOptions) {
        this.activeProviders = List.of();
        this.resetTextures();
        this.activeProviders = this.selectProviders(this.allProviders, pOptions);
    }

    private void resetTextures() {
        this.textures.clear();
        this.glyphs.clear();
        this.glyphInfos.clear();
        this.glyphsByWidth.clear();
        this.missingGlyph = SpecialGlyphs.MISSING.bake(this::stitch);
        this.whiteGlyph = SpecialGlyphs.WHITE.bake(this::stitch);
    }

    private List<GlyphProvider> selectProviders(List<GlyphProvider.Conditional> pProviders, Set<FontOption> pOptions) {
        IntSet intset = new IntOpenHashSet();
        List<GlyphProvider> list = new ArrayList<>();

        for (GlyphProvider.Conditional glyphprovider$conditional : pProviders) {
            if (glyphprovider$conditional.filter().apply(pOptions)) {
                list.add(glyphprovider$conditional.provider());
                intset.addAll(glyphprovider$conditional.provider().getSupportedGlyphs());
            }
        }

        Set<GlyphProvider> set = Sets.newHashSet();
        intset.forEach((int p_232561_) -> {
            for (GlyphProvider glyphprovider : list) {
                GlyphInfo glyphinfo = glyphprovider.getGlyph(p_232561_);
                if (glyphinfo != null) {
                    set.add(glyphprovider);
                    if (glyphinfo != SpecialGlyphs.MISSING) {
                        this.glyphsByWidth.computeIfAbsent(Mth.ceil(glyphinfo.getAdvance(false)), p_232567_ -> new IntArrayList()).add(p_232561_);
                    }
                    break;
                }
            }
        });
        return list.stream().filter(set::contains).toList();
    }

    @Override
    public void close() {
        this.textures.clear();
    }

    private static boolean hasFishyAdvance(GlyphInfo pGlyph) {
        float f = pGlyph.getAdvance(false);
        if (!(f < 0.0F) && !(f > 32.0F)) {
            float f1 = pGlyph.getAdvance(true);
            return f1 < 0.0F || f1 > 32.0F;
        } else {
            return true;
        }
    }

    private FontSet.GlyphInfoFilter computeGlyphInfo(int pCharacter) {
        GlyphInfo glyphinfo = null;

        for (GlyphProvider glyphprovider : this.activeProviders) {
            GlyphInfo glyphinfo1 = glyphprovider.getGlyph(pCharacter);
            if (glyphinfo1 != null) {
                if (glyphinfo == null) {
                    glyphinfo = glyphinfo1;
                }

                if (!hasFishyAdvance(glyphinfo1)) {
                    return new FontSet.GlyphInfoFilter(glyphinfo, glyphinfo1);
                }
            }
        }

        return glyphinfo != null ? new FontSet.GlyphInfoFilter(glyphinfo, SpecialGlyphs.MISSING) : FontSet.GlyphInfoFilter.MISSING;
    }

    public GlyphInfo getGlyphInfo(int pCharacter, boolean pFilterFishyGlyphs) {
        return this.glyphInfos.computeIfAbsent(pCharacter, this.glyphInfoGetter).select(pFilterFishyGlyphs);
    }

    private BakedGlyph computeBakedGlyph(int pCharacter) {
        for (GlyphProvider glyphprovider : this.activeProviders) {
            GlyphInfo glyphinfo = glyphprovider.getGlyph(pCharacter);
            if (glyphinfo != null) {
                return glyphinfo.bake(this::stitch);
            }
        }

        LOGGER.warn("Couldn't find glyph for character {} (\\u{})", Character.toString(pCharacter), String.format("%04x", pCharacter));
        return this.missingGlyph;
    }

    public BakedGlyph getGlyph(int pCharacter) {
        return this.glyphs.computeIfAbsent(pCharacter, this.glyphGetter);
    }

    private BakedGlyph stitch(SheetGlyphInfo pGlyphInfo) {
        for (FontTexture fonttexture : this.textures) {
            BakedGlyph bakedglyph = fonttexture.add(pGlyphInfo);
            if (bakedglyph != null) {
                return bakedglyph;
            }
        }

        ResourceLocation resourcelocation = this.name.withSuffix("/" + this.textures.size());
        boolean flag = pGlyphInfo.isColored();
        GlyphRenderTypes glyphrendertypes = flag ? GlyphRenderTypes.createForColorTexture(resourcelocation) : GlyphRenderTypes.createForIntensityTexture(resourcelocation);
        FontTexture fonttexture1 = new FontTexture(resourcelocation::toString, glyphrendertypes, flag);
        this.textures.add(fonttexture1);
        this.textureManager.register(resourcelocation, fonttexture1);
        BakedGlyph bakedglyph1 = fonttexture1.add(pGlyphInfo);
        return bakedglyph1 == null ? this.missingGlyph : bakedglyph1;
    }

    public BakedGlyph getRandomGlyph(GlyphInfo pGlyph) {
        IntList intlist = this.glyphsByWidth.get(Mth.ceil(pGlyph.getAdvance(false)));
        return intlist != null && !intlist.isEmpty() ? this.getGlyph(intlist.getInt(RANDOM.nextInt(intlist.size()))) : this.missingGlyph;
    }

    public ResourceLocation name() {
        return this.name;
    }

    public BakedGlyph whiteGlyph() {
        return this.whiteGlyph;
    }

    @OnlyIn(Dist.CLIENT)
    record GlyphInfoFilter(GlyphInfo glyphInfo, GlyphInfo glyphInfoNotFishy) {
        static final FontSet.GlyphInfoFilter MISSING = new FontSet.GlyphInfoFilter(SpecialGlyphs.MISSING, SpecialGlyphs.MISSING);

        GlyphInfo select(boolean pFilterFishyGlyphs) {
            return pFilterFishyGlyphs ? this.glyphInfoNotFishy : this.glyphInfo;
        }
    }
}