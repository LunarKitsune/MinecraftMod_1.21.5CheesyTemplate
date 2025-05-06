package net.minecraft.client.renderer.block.model;

import java.util.List;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.QuadCollection;
import net.minecraft.client.resources.model.SpriteGetter;
import net.minecraft.client.resources.model.UnbakedGeometry;
import net.minecraft.core.Direction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record SimpleUnbakedGeometry(List<BlockElement> elements) implements UnbakedGeometry {
    @Override
    public QuadCollection bake(TextureSlots p_397805_, ModelBaker p_395314_, ModelState p_394240_, ModelDebugName p_392934_) {
        return bake(this.elements, p_397805_, p_395314_.sprites(), p_394240_, p_392934_);
    }

    public static QuadCollection bake(
        List<BlockElement> pElements, TextureSlots pTextureSlots, SpriteGetter pSprites, ModelState pModelState, ModelDebugName pDebugName
    ) {
        QuadCollection.Builder quadcollection$builder = new QuadCollection.Builder();

        for (BlockElement blockelement : pElements) {
            blockelement.faces()
                .forEach(
                    (p_392025_, p_394051_) -> {
                        TextureAtlasSprite textureatlassprite = pSprites.resolveSlot(pTextureSlots, p_394051_.texture(), pDebugName);
                        if (p_394051_.cullForDirection() == null) {
                            quadcollection$builder.addUnculledFace(bakeFace(blockelement, p_394051_, textureatlassprite, p_392025_, pModelState));
                        } else {
                            quadcollection$builder.addCulledFace(
                                Direction.rotate(pModelState.transformation().getMatrix(), p_394051_.cullForDirection()),
                                bakeFace(blockelement, p_394051_, textureatlassprite, p_392025_, pModelState)
                            );
                        }
                    }
                );
        }

        return quadcollection$builder.build();
    }

    private static BakedQuad bakeFace(
        BlockElement pElement, BlockElementFace pFace, TextureAtlasSprite pSprite, Direction pDirection, ModelState pModelState
    ) {
        return FaceBakery.bakeQuad(
            pElement.from(),
            pElement.to(),
            pFace,
            pSprite,
            pDirection,
            pModelState,
            pElement.rotation(),
            pElement.shade(),
            pElement.lightEmission()
        );
    }
}