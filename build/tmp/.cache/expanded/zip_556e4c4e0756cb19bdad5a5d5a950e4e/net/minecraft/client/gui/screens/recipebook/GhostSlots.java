package net.minecraft.client.gui.screens.recipebook;

import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GhostSlots {
    private final Reference2ObjectMap<Slot, GhostSlots.GhostSlot> ingredients = new Reference2ObjectArrayMap<>();
    private final SlotSelectTime slotSelectTime;

    public GhostSlots(SlotSelectTime pSlotSelectTime) {
        this.slotSelectTime = pSlotSelectTime;
    }

    public void clear() {
        this.ingredients.clear();
    }

    private void setSlot(Slot pSlot, ContextMap pContextMap, SlotDisplay pSlotDisplay, boolean pIsResultSlot) {
        List<ItemStack> list = pSlotDisplay.resolveForStacks(pContextMap);
        if (!list.isEmpty()) {
            this.ingredients.put(pSlot, new GhostSlots.GhostSlot(list, pIsResultSlot));
        }
    }

    protected void setInput(Slot pSlot, ContextMap pContextMap, SlotDisplay pSlotDisplay) {
        this.setSlot(pSlot, pContextMap, pSlotDisplay, false);
    }

    protected void setResult(Slot pSlot, ContextMap pContextMap, SlotDisplay pSlotDisplay) {
        this.setSlot(pSlot, pContextMap, pSlotDisplay, true);
    }

    public void render(GuiGraphics pGuiGraphics, Minecraft pMinecraft, boolean pIsBiggerResultSlot) {
        this.ingredients.forEach((p_365858_, p_367422_) -> {
            int i = p_365858_.x;
            int j = p_365858_.y;
            if (p_367422_.isResultSlot && pIsBiggerResultSlot) {
                pGuiGraphics.fill(i - 4, j - 4, i + 20, j + 20, 822018048);
            } else {
                pGuiGraphics.fill(i, j, i + 16, j + 16, 822018048);
            }

            ItemStack itemstack = p_367422_.getItem(this.slotSelectTime.currentIndex());
            pGuiGraphics.renderFakeItem(itemstack, i, j);
            pGuiGraphics.fill(RenderType.guiGhostRecipeOverlay(), i, j, i + 16, j + 16, 822083583);
            if (p_367422_.isResultSlot) {
                pGuiGraphics.renderItemDecorations(pMinecraft.font, itemstack, i, j);
            }
        });
    }

    public void renderTooltip(GuiGraphics pGuiGraphics, Minecraft pMinecraft, int pMouseX, int pMouseY, @Nullable Slot pSlot) {
        if (pSlot != null) {
            GhostSlots.GhostSlot ghostslots$ghostslot = this.ingredients.get(pSlot);
            if (ghostslots$ghostslot != null) {
                ItemStack itemstack = ghostslots$ghostslot.getItem(this.slotSelectTime.currentIndex());
                pGuiGraphics.renderComponentTooltip(
                    pMinecraft.font, Screen.getTooltipFromItem(pMinecraft, itemstack), pMouseX, pMouseY, itemstack.get(DataComponents.TOOLTIP_STYLE)
                );
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    record GhostSlot(List<ItemStack> items, boolean isResultSlot) {
        public ItemStack getItem(int pIndex) {
            int i = this.items.size();
            return i == 0 ? ItemStack.EMPTY : this.items.get(pIndex % i);
        }
    }
}