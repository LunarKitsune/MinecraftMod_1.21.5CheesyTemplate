package net.minecraft.client.renderer.block.model;

import com.mojang.math.Quadrant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.QuadCollection;
import net.minecraft.client.resources.model.SpriteGetter;
import net.minecraft.client.resources.model.UnbakedGeometry;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class ItemModelGenerator implements UnbakedModel {
    public static final ResourceLocation GENERATED_ITEM_MODEL_ID = ResourceLocation.withDefaultNamespace("builtin/generated");
    public static final List<String> LAYERS = List.of("layer0", "layer1", "layer2", "layer3", "layer4");
    private static final float MIN_Z = 7.5F;
    private static final float MAX_Z = 8.5F;
    private static final TextureSlots.Data TEXTURE_SLOTS = new TextureSlots.Data.Builder().addReference("particle", "layer0").build();
    private static final BlockElementFace.UVs SOUTH_FACE_UVS = new BlockElementFace.UVs(0.0F, 0.0F, 16.0F, 16.0F);
    private static final BlockElementFace.UVs NORTH_FACE_UVS = new BlockElementFace.UVs(16.0F, 0.0F, 0.0F, 16.0F);

    @Override
    public TextureSlots.Data textureSlots() {
        return TEXTURE_SLOTS;
    }

    @Override
    public UnbakedGeometry geometry() {
        return ItemModelGenerator::bake;
    }

    @Nullable
    @Override
    public UnbakedModel.GuiLight guiLight() {
        return UnbakedModel.GuiLight.FRONT;
    }

    private static QuadCollection bake(TextureSlots pTextureSlots, ModelBaker pBaker, ModelState pModelState, ModelDebugName pDebugName) {
        return bake(pTextureSlots, pBaker.sprites(), pModelState, pDebugName);
    }

    private static QuadCollection bake(TextureSlots pTextureSlots, SpriteGetter pSprites, ModelState pModelState, ModelDebugName pDebugName) {
        List<BlockElement> list = new ArrayList<>();

        for (int i = 0; i < LAYERS.size(); i++) {
            String s = LAYERS.get(i);
            Material material = pTextureSlots.getMaterial(s);
            if (material == null) {
                break;
            }

            SpriteContents spritecontents = pSprites.get(material, pDebugName).contents();
            list.addAll(processFrames(i, s, spritecontents));
        }

        return SimpleUnbakedGeometry.bake(list, pTextureSlots, pSprites, pModelState, pDebugName);
    }

    public static List<BlockElement> processFrames(int pTintIndex, String pTexture, SpriteContents pSprite) {
        Map<Direction, BlockElementFace> map = Map.of(
            Direction.SOUTH,
            new BlockElementFace(null, pTintIndex, pTexture, SOUTH_FACE_UVS, Quadrant.R0),
            Direction.NORTH,
            new BlockElementFace(null, pTintIndex, pTexture, NORTH_FACE_UVS, Quadrant.R0)
        );
        List<BlockElement> list = new ArrayList<>();
        list.add(new BlockElement(new Vector3f(0.0F, 0.0F, 7.5F), new Vector3f(16.0F, 16.0F, 8.5F), map));
        list.addAll(createSideElements(pSprite, pTexture, pTintIndex));
        return list;
    }

    private static List<BlockElement> createSideElements(SpriteContents pSprite, String pTexture, int pTintIndex) {
        float f = pSprite.width();
        float f1 = pSprite.height();
        List<BlockElement> list = new ArrayList<>();

        for (ItemModelGenerator.Span itemmodelgenerator$span : getSpans(pSprite)) {
            float f2 = 0.0F;
            float f3 = 0.0F;
            float f4 = 0.0F;
            float f5 = 0.0F;
            float f6 = 0.0F;
            float f7 = 0.0F;
            float f8 = 0.0F;
            float f9 = 0.0F;
            float f10 = 16.0F / f;
            float f11 = 16.0F / f1;
            float f12 = itemmodelgenerator$span.getMin();
            float f13 = itemmodelgenerator$span.getMax();
            float f14 = itemmodelgenerator$span.getAnchor();
            ItemModelGenerator.SpanFacing itemmodelgenerator$spanfacing = itemmodelgenerator$span.getFacing();
            switch (itemmodelgenerator$spanfacing) {
                case UP:
                    f6 = f12;
                    f2 = f12;
                    f4 = f7 = f13 + 1.0F;
                    f8 = f14;
                    f3 = f14;
                    f5 = f14;
                    f9 = f14 + 1.0F;
                    break;
                case DOWN:
                    f8 = f14;
                    f9 = f14 + 1.0F;
                    f6 = f12;
                    f2 = f12;
                    f4 = f7 = f13 + 1.0F;
                    f3 = f14 + 1.0F;
                    f5 = f14 + 1.0F;
                    break;
                case LEFT:
                    f6 = f14;
                    f2 = f14;
                    f4 = f14;
                    f7 = f14 + 1.0F;
                    f9 = f12;
                    f3 = f12;
                    f5 = f8 = f13 + 1.0F;
                    break;
                case RIGHT:
                    f6 = f14;
                    f7 = f14 + 1.0F;
                    f2 = f14 + 1.0F;
                    f4 = f14 + 1.0F;
                    f9 = f12;
                    f3 = f12;
                    f5 = f8 = f13 + 1.0F;
            }

            f2 *= f10;
            f4 *= f10;
            f3 *= f11;
            f5 *= f11;
            f3 = 16.0F - f3;
            f5 = 16.0F - f5;
            f6 *= f10;
            f7 *= f10;
            f8 *= f11;
            f9 *= f11;
            Map<Direction, BlockElementFace> map = Map.of(
                itemmodelgenerator$spanfacing.getDirection(),
                new BlockElementFace(null, pTintIndex, pTexture, new BlockElementFace.UVs(f6, f8, f7, f9), Quadrant.R0)
            );
            switch (itemmodelgenerator$spanfacing) {
                case UP:
                    list.add(new BlockElement(new Vector3f(f2, f3, 7.5F), new Vector3f(f4, f3, 8.5F), map));
                    break;
                case DOWN:
                    list.add(new BlockElement(new Vector3f(f2, f5, 7.5F), new Vector3f(f4, f5, 8.5F), map));
                    break;
                case LEFT:
                    list.add(new BlockElement(new Vector3f(f2, f3, 7.5F), new Vector3f(f2, f5, 8.5F), map));
                    break;
                case RIGHT:
                    list.add(new BlockElement(new Vector3f(f4, f3, 7.5F), new Vector3f(f4, f5, 8.5F), map));
            }
        }

        return list;
    }

    private static List<ItemModelGenerator.Span> getSpans(SpriteContents pSprite) {
        int i = pSprite.width();
        int j = pSprite.height();
        List<ItemModelGenerator.Span> list = new ArrayList<>();
        pSprite.getUniqueFrames().forEach(p_389479_ -> {
            for (int k = 0; k < j; k++) {
                for (int l = 0; l < i; l++) {
                    boolean flag = !isTransparent(pSprite, p_389479_, l, k, i, j);
                    checkTransition(ItemModelGenerator.SpanFacing.UP, list, pSprite, p_389479_, l, k, i, j, flag);
                    checkTransition(ItemModelGenerator.SpanFacing.DOWN, list, pSprite, p_389479_, l, k, i, j, flag);
                    checkTransition(ItemModelGenerator.SpanFacing.LEFT, list, pSprite, p_389479_, l, k, i, j, flag);
                    checkTransition(ItemModelGenerator.SpanFacing.RIGHT, list, pSprite, p_389479_, l, k, i, j, flag);
                }
            }
        });
        return list;
    }

    private static void checkTransition(
        ItemModelGenerator.SpanFacing pSpanFacing,
        List<ItemModelGenerator.Span> pListSpans,
        SpriteContents pContents,
        int pFrameIndex,
        int pPixelX,
        int pPixelY,
        int pSpriteWidth,
        int pSpriteHeight,
        boolean pTransparent
    ) {
        boolean flag = isTransparent(pContents, pFrameIndex, pPixelX + pSpanFacing.getXOffset(), pPixelY + pSpanFacing.getYOffset(), pSpriteWidth, pSpriteHeight) && pTransparent;
        if (flag) {
            createOrExpandSpan(pListSpans, pSpanFacing, pPixelX, pPixelY);
        }
    }

    private static void createOrExpandSpan(List<ItemModelGenerator.Span> pListSpans, ItemModelGenerator.SpanFacing pSpanFacing, int pPixelX, int pPixelY) {
        ItemModelGenerator.Span itemmodelgenerator$span = null;

        for (ItemModelGenerator.Span itemmodelgenerator$span1 : pListSpans) {
            if (itemmodelgenerator$span1.getFacing() == pSpanFacing) {
                int i = pSpanFacing.isHorizontal() ? pPixelY : pPixelX;
                if (itemmodelgenerator$span1.getAnchor() == i) {
                    itemmodelgenerator$span = itemmodelgenerator$span1;
                    break;
                }
            }
        }

        int j = pSpanFacing.isHorizontal() ? pPixelY : pPixelX;
        int k = pSpanFacing.isHorizontal() ? pPixelX : pPixelY;
        if (itemmodelgenerator$span == null) {
            pListSpans.add(new ItemModelGenerator.Span(pSpanFacing, k, j));
        } else {
            itemmodelgenerator$span.expand(k);
        }
    }

    private static boolean isTransparent(SpriteContents pSprite, int pFrameIndex, int pPixelX, int pPixelY, int pSpriteWidth, int pSpriteHeight) {
        return pPixelX >= 0 && pPixelY >= 0 && pPixelX < pSpriteWidth && pPixelY < pSpriteHeight ? pSprite.isTransparent(pFrameIndex, pPixelX, pPixelY) : true;
    }

    @OnlyIn(Dist.CLIENT)
    static class Span {
        private final ItemModelGenerator.SpanFacing facing;
        private int min;
        private int max;
        private final int anchor;

        public Span(ItemModelGenerator.SpanFacing pFacing, int pMinMax, int pAnchor) {
            this.facing = pFacing;
            this.min = pMinMax;
            this.max = pMinMax;
            this.anchor = pAnchor;
        }

        public void expand(int pPos) {
            if (pPos < this.min) {
                this.min = pPos;
            } else if (pPos > this.max) {
                this.max = pPos;
            }
        }

        public ItemModelGenerator.SpanFacing getFacing() {
            return this.facing;
        }

        public int getMin() {
            return this.min;
        }

        public int getMax() {
            return this.max;
        }

        public int getAnchor() {
            return this.anchor;
        }
    }

    @OnlyIn(Dist.CLIENT)
    static enum SpanFacing {
        UP(Direction.UP, 0, -1),
        DOWN(Direction.DOWN, 0, 1),
        LEFT(Direction.EAST, -1, 0),
        RIGHT(Direction.WEST, 1, 0);

        private final Direction direction;
        private final int xOffset;
        private final int yOffset;

        private SpanFacing(final Direction pDirection, final int pXOffset, final int pYOffset) {
            this.direction = pDirection;
            this.xOffset = pXOffset;
            this.yOffset = pYOffset;
        }

        public Direction getDirection() {
            return this.direction;
        }

        public int getXOffset() {
            return this.xOffset;
        }

        public int getYOffset() {
            return this.yOffset;
        }

        boolean isHorizontal() {
            return this == DOWN || this == UP;
        }
    }
}