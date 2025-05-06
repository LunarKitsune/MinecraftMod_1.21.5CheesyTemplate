package net.minecraft.world.item;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.AbstractBannerBlock;
import net.minecraft.world.level.block.Block;
import org.apache.commons.lang3.Validate;

public class BannerItem extends StandingAndWallBlockItem {
    public BannerItem(Block pBlock, Block pWallBlock, Item.Properties pProperties) {
        super(pBlock, pWallBlock, Direction.DOWN, pProperties);
        Validate.isInstanceOf(AbstractBannerBlock.class, pBlock);
        Validate.isInstanceOf(AbstractBannerBlock.class, pWallBlock);
    }

    public DyeColor getColor() {
        return ((AbstractBannerBlock)this.getBlock()).getColor();
    }
}