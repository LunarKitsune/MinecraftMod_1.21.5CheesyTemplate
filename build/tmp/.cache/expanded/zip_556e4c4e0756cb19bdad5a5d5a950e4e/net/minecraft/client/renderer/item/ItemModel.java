package net.minecraft.client.renderer.item;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.util.RegistryContextSwapper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface ItemModel {
    void update(
        ItemStackRenderState pRenderState,
        ItemStack pStack,
        ItemModelResolver pItemModelResolver,
        ItemDisplayContext pDisplayContext,
        @Nullable ClientLevel pLevel,
        @Nullable LivingEntity pEntity,
        int pSeed
    );

    @OnlyIn(Dist.CLIENT)
    public record BakingContext(ModelBaker blockModelBaker, EntityModelSet entityModelSet, ItemModel missingItemModel, @Nullable RegistryContextSwapper contextSwapper) {
    }

    @OnlyIn(Dist.CLIENT)
    public interface Unbaked extends ResolvableModel {
        MapCodec<? extends ItemModel.Unbaked> type();

        ItemModel bake(ItemModel.BakingContext pContext);
    }
}