package net.minecraft.client.renderer.entity.state;

import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ItemClusterRenderState extends EntityRenderState {
    public final ItemStackRenderState item = new ItemStackRenderState();
    public int count;
    public int seed;

    public void extractItemGroupRenderState(Entity pEntity, ItemStack pStack, ItemModelResolver pItemModelResolver) {
        pItemModelResolver.updateForNonLiving(this.item, pStack, ItemDisplayContext.GROUND, pEntity);
        this.count = getRenderedAmount(pStack.getCount());
        this.seed = getSeedForItemStack(pStack);
    }

    public static int getSeedForItemStack(ItemStack pStack) {
        return pStack.isEmpty() ? 187 : Item.getId(pStack.getItem()) + pStack.getDamageValue();
    }

    public static int getRenderedAmount(int pCount) {
        if (pCount <= 1) {
            return 1;
        } else if (pCount <= 16) {
            return 2;
        } else if (pCount <= 32) {
            return 3;
        } else {
            return pCount <= 48 ? 4 : 5;
        }
    }
}