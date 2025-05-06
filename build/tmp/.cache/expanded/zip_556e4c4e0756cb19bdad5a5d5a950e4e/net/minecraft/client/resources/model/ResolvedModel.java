package net.minecraft.client.resources.model;

import javax.annotation.Nullable;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface ResolvedModel extends ModelDebugName {
    boolean DEFAULT_AMBIENT_OCCLUSION = true;
    UnbakedModel.GuiLight DEFAULT_GUI_LIGHT = UnbakedModel.GuiLight.SIDE;

    UnbakedModel wrapped();

    @Nullable
    ResolvedModel parent();

    static TextureSlots findTopTextureSlots(ResolvedModel pModel) {
        ResolvedModel resolvedmodel = pModel;

        TextureSlots.Resolver textureslots$resolver;
        for (textureslots$resolver = new TextureSlots.Resolver(); resolvedmodel != null; resolvedmodel = resolvedmodel.parent()) {
            textureslots$resolver.addLast(resolvedmodel.wrapped().textureSlots());
        }

        return textureslots$resolver.resolve(pModel);
    }

    default TextureSlots getTopTextureSlots() {
        return findTopTextureSlots(this);
    }

    static boolean findTopAmbientOcclusion(ResolvedModel pModel) {
        while (pModel != null) {
            Boolean obool = pModel.wrapped().ambientOcclusion();
            if (obool != null) {
                return obool;
            }

            pModel = pModel.parent();
        }

        return true;
    }

    default boolean getTopAmbientOcclusion() {
        return findTopAmbientOcclusion(this);
    }

    static UnbakedModel.GuiLight findTopGuiLight(ResolvedModel pModel) {
        while (pModel != null) {
            UnbakedModel.GuiLight unbakedmodel$guilight = pModel.wrapped().guiLight();
            if (unbakedmodel$guilight != null) {
                return unbakedmodel$guilight;
            }

            pModel = pModel.parent();
        }

        return DEFAULT_GUI_LIGHT;
    }

    default UnbakedModel.GuiLight getTopGuiLight() {
        return findTopGuiLight(this);
    }

    static UnbakedGeometry findTopGeometry(ResolvedModel pModel) {
        while (pModel != null) {
            UnbakedGeometry unbakedgeometry = pModel.wrapped().geometry();
            if (unbakedgeometry != null) {
                return unbakedgeometry;
            }

            pModel = pModel.parent();
        }

        return UnbakedGeometry.EMPTY;
    }

    default UnbakedGeometry getTopGeometry() {
        return findTopGeometry(this);
    }

    default QuadCollection bakeTopGeometry(TextureSlots pTextureSlots, ModelBaker pModelBaker, ModelState pModelState) {
        return this.getTopGeometry().bake(pTextureSlots, pModelBaker, pModelState, this, getContext());
    }

    static TextureAtlasSprite resolveParticleSprite(TextureSlots pTextureSlots, ModelBaker pModelBaker, ModelDebugName pDebugName) {
        return pModelBaker.sprites().resolveSlot(pTextureSlots, "particle", pDebugName);
    }

    default TextureAtlasSprite resolveParticleSprite(TextureSlots pTextureSlots, ModelBaker pModelBaker) {
        return resolveParticleSprite(pTextureSlots, pModelBaker, this);
    }

    static ItemTransform findTopTransform(ResolvedModel pModel, ItemDisplayContext pDisplayContext) {
        while (pModel != null) {
            ItemTransforms itemtransforms = pModel.wrapped().transforms();
            if (itemtransforms != null) {
                ItemTransform itemtransform = itemtransforms.getTransform(pDisplayContext);
                if (itemtransform != ItemTransform.NO_TRANSFORM) {
                    return itemtransform;
                }
            }

            pModel = pModel.parent();
        }

        return ItemTransform.NO_TRANSFORM;
    }

    static ItemTransforms findTopTransforms(ResolvedModel pModel) {
        ItemTransform itemtransform = findTopTransform(pModel, ItemDisplayContext.THIRD_PERSON_LEFT_HAND);
        ItemTransform itemtransform1 = findTopTransform(pModel, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND);
        ItemTransform itemtransform2 = findTopTransform(pModel, ItemDisplayContext.FIRST_PERSON_LEFT_HAND);
        ItemTransform itemtransform3 = findTopTransform(pModel, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND);
        ItemTransform itemtransform4 = findTopTransform(pModel, ItemDisplayContext.HEAD);
        ItemTransform itemtransform5 = findTopTransform(pModel, ItemDisplayContext.GUI);
        ItemTransform itemtransform6 = findTopTransform(pModel, ItemDisplayContext.GROUND);
        ItemTransform itemtransform7 = findTopTransform(pModel, ItemDisplayContext.FIXED);
        return new ItemTransforms(itemtransform, itemtransform1, itemtransform2, itemtransform3, itemtransform4, itemtransform5, itemtransform6, itemtransform7);
    }

    default ItemTransforms getTopTransforms() {
        return findTopTransforms(this);
    }

    default net.minecraftforge.client.model.geometry.IGeometryBakingContext getContext() {
        return new net.minecraftforge.client.model.geometry.ModelContext(this);
    }
}
