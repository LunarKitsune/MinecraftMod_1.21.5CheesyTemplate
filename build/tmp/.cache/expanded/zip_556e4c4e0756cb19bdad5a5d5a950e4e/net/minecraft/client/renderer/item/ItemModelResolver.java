package net.minecraft.client.renderer.item;

import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ItemModelResolver {
    private final Function<ResourceLocation, ItemModel> modelGetter;
    private final Function<ResourceLocation, ClientItem.Properties> clientProperties;

    public ItemModelResolver(ModelManager pModelManager) {
        this.modelGetter = pModelManager::getItemModel;
        this.clientProperties = pModelManager::getItemProperties;
    }

    public void updateForLiving(ItemStackRenderState pRenderState, ItemStack pStack, ItemDisplayContext pDisplayContext, LivingEntity pEntity) {
        this.updateForTopItem(pRenderState, pStack, pDisplayContext, pEntity.level(), pEntity, pEntity.getId() + pDisplayContext.ordinal());
    }

    public void updateForNonLiving(ItemStackRenderState pRenderState, ItemStack pStack, ItemDisplayContext pDisplayContext, Entity pEntity) {
        this.updateForTopItem(pRenderState, pStack, pDisplayContext, pEntity.level(), null, pEntity.getId());
    }

    public void updateForTopItem(
        ItemStackRenderState pRenderState,
        ItemStack pStack,
        ItemDisplayContext pDisplayContext,
        @Nullable Level pLevel,
        @Nullable LivingEntity pEntity,
        int pSeed
    ) {
        pRenderState.clear();
        if (!pStack.isEmpty()) {
            pRenderState.displayContext = pDisplayContext;
            this.appendItemLayers(pRenderState, pStack, pDisplayContext, pLevel, pEntity, pSeed);
        }
    }

    private static void fixupSkullProfile(ItemStack pStack) {
        if (pStack.getItem() instanceof BlockItem blockitem && blockitem.getBlock() instanceof AbstractSkullBlock) {
            ResolvableProfile resolvableprofile = pStack.get(DataComponents.PROFILE);
            if (resolvableprofile != null && !resolvableprofile.isResolved()) {
                pStack.remove(DataComponents.PROFILE);
                resolvableprofile.resolve().thenAcceptAsync(p_376419_ -> pStack.set(DataComponents.PROFILE, p_376419_), Minecraft.getInstance());
            }
        }
    }

    public void appendItemLayers(
        ItemStackRenderState pRenderState,
        ItemStack pStack,
        ItemDisplayContext pDisplayContext,
        @Nullable Level pLevel,
        @Nullable LivingEntity pEntity,
        int pSeed
    ) {
        fixupSkullProfile(pStack);
        ResourceLocation resourcelocation = pStack.get(DataComponents.ITEM_MODEL);
        if (resourcelocation != null) {
            this.modelGetter
                .apply(resourcelocation)
                .update(pRenderState, pStack, this, pDisplayContext, pLevel instanceof ClientLevel clientlevel ? clientlevel : null, pEntity, pSeed);
        }
    }

    public boolean shouldPlaySwapAnimation(ItemStack pStack) {
        ResourceLocation resourcelocation = pStack.get(DataComponents.ITEM_MODEL);
        return resourcelocation == null ? true : this.clientProperties.apply(resourcelocation).handAnimationOnSwap();
    }
}