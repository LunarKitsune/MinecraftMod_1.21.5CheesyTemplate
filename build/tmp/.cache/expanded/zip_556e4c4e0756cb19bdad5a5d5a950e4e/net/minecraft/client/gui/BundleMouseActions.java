package net.minecraft.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.ScrollWheelHandler;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ServerboundSelectBundleItemPacket;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector2i;

@OnlyIn(Dist.CLIENT)
public class BundleMouseActions implements ItemSlotMouseAction {
    private final Minecraft minecraft;
    private final ScrollWheelHandler scrollWheelHandler;

    public BundleMouseActions(Minecraft pMinecraft) {
        this.minecraft = pMinecraft;
        this.scrollWheelHandler = new ScrollWheelHandler();
    }

    @Override
    public boolean matches(Slot p_366311_) {
        return p_366311_.getItem().is(ItemTags.BUNDLES);
    }

    @Override
    public boolean onMouseScrolled(double p_366081_, double p_361601_, int p_361414_, ItemStack p_365693_) {
        int i = BundleItem.getNumberOfItemsToShow(p_365693_);
        if (i == 0) {
            return false;
        } else {
            Vector2i vector2i = this.scrollWheelHandler.onMouseScroll(p_366081_, p_361601_);
            int j = vector2i.y == 0 ? -vector2i.x : vector2i.y;
            if (j != 0) {
                int k = BundleItem.getSelectedItem(p_365693_);
                int l = ScrollWheelHandler.getNextScrollWheelSelection(j, k, i);
                if (k != l) {
                    this.toggleSelectedBundleItem(p_365693_, p_361414_, l);
                }
            }

            return true;
        }
    }

    @Override
    public void onStopHovering(Slot p_366465_) {
        this.unselectedBundleItem(p_366465_.getItem(), p_366465_.index);
    }

    @Override
    public void onSlotClicked(Slot p_367625_, ClickType p_368460_) {
        if (p_368460_ == ClickType.QUICK_MOVE || p_368460_ == ClickType.SWAP) {
            this.unselectedBundleItem(p_367625_.getItem(), p_367625_.index);
        }
    }

    private void toggleSelectedBundleItem(ItemStack pStack, int pIndex, int pNextIndex) {
        if (this.minecraft.getConnection() != null && pNextIndex < BundleItem.getNumberOfItemsToShow(pStack)) {
            ClientPacketListener clientpacketlistener = this.minecraft.getConnection();
            BundleItem.toggleSelectedItem(pStack, pNextIndex);
            clientpacketlistener.send(new ServerboundSelectBundleItemPacket(pIndex, pNextIndex));
        }
    }

    public void unselectedBundleItem(ItemStack pBundle, int pSlotIndex) {
        this.toggleSelectedBundleItem(pBundle, pSlotIndex, -1);
    }
}