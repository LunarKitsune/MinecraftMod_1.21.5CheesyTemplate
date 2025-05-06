package net.minecraft.client.resources.model;

import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@FunctionalInterface
@OnlyIn(Dist.CLIENT)
public interface UnbakedGeometry {
    UnbakedGeometry EMPTY = (p_396370_, p_395488_, p_393569_, p_397842_) -> QuadCollection.EMPTY;

    QuadCollection bake(TextureSlots pTextureSlots, ModelBaker pBaker, ModelState pModelState, ModelDebugName pDebugName);

    default QuadCollection bake(TextureSlots slots, ModelBaker baker, ModelState state, ModelDebugName name, net.minecraftforge.client.model.geometry.IGeometryBakingContext context) {
        return bake(slots, baker, state, name);
    }
}
