package net.minecraft.client.data.models;

import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface ItemModelOutput {
    void accept(Item pItem, ItemModel.Unbaked pModel);

    void copy(Item pItem1, Item pItem2);
}