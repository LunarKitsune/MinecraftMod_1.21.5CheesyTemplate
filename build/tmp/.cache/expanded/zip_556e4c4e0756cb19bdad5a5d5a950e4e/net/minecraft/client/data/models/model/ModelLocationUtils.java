package net.minecraft.client.data.models.model;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ModelLocationUtils {
    @Deprecated
    public static ResourceLocation decorateBlockModelLocation(String pName) {
        return ResourceLocation.withDefaultNamespace("block/" + pName);
    }

    public static ResourceLocation decorateItemModelLocation(String pName) {
        return ResourceLocation.withDefaultNamespace("item/" + pName);
    }

    public static ResourceLocation getModelLocation(Block pBlock, String pSuffix) {
        ResourceLocation resourcelocation = BuiltInRegistries.BLOCK.getKey(pBlock);
        return resourcelocation.withPath(p_375700_ -> "block/" + p_375700_ + pSuffix);
    }

    public static ResourceLocation getModelLocation(Block pBlock) {
        ResourceLocation resourcelocation = BuiltInRegistries.BLOCK.getKey(pBlock);
        return resourcelocation.withPrefix("block/");
    }

    public static ResourceLocation getModelLocation(Item pItem) {
        ResourceLocation resourcelocation = BuiltInRegistries.ITEM.getKey(pItem);
        return resourcelocation.withPrefix("item/");
    }

    public static ResourceLocation getModelLocation(Item pItem, String pSuffix) {
        ResourceLocation resourcelocation = BuiltInRegistries.ITEM.getKey(pItem);
        return resourcelocation.withPath(p_376725_ -> "item/" + p_376725_ + pSuffix);
    }
}