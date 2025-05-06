package net.minecraft.client.renderer.special;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface SpecialModelRenderer<T> {
    void render(
        @Nullable T pPatterns, ItemDisplayContext pDisplayContext, PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, int pPackedOverlay, boolean pHasFoilType
    );

    @Nullable
    T extractArgument(ItemStack pStack);

    @OnlyIn(Dist.CLIENT)
    public interface Unbaked {
        @Nullable
        SpecialModelRenderer<?> bake(EntityModelSet pModelSet);

        MapCodec<? extends SpecialModelRenderer.Unbaked> type();
    }
}