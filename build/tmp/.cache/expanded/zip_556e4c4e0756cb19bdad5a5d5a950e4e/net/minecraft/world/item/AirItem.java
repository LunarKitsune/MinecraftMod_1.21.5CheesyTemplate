package net.minecraft.world.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;

public class AirItem extends Item {
    public AirItem(Block pBlock, Item.Properties pProperties) {
        super(pProperties);
    }

    @Override
    public Component getName(ItemStack p_365938_) {
        return this.getName();
    }
}