package net.minecraft.client.gui.screens.inventory;

import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MapRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.state.MapRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CartographyTableScreen extends AbstractContainerScreen<CartographyTableMenu> {
    private static final ResourceLocation ERROR_SPRITE = ResourceLocation.withDefaultNamespace("container/cartography_table/error");
    private static final ResourceLocation SCALED_MAP_SPRITE = ResourceLocation.withDefaultNamespace("container/cartography_table/scaled_map");
    private static final ResourceLocation DUPLICATED_MAP_SPRITE = ResourceLocation.withDefaultNamespace("container/cartography_table/duplicated_map");
    private static final ResourceLocation MAP_SPRITE = ResourceLocation.withDefaultNamespace("container/cartography_table/map");
    private static final ResourceLocation LOCKED_SPRITE = ResourceLocation.withDefaultNamespace("container/cartography_table/locked");
    private static final ResourceLocation BG_LOCATION = ResourceLocation.withDefaultNamespace("textures/gui/container/cartography_table.png");
    private final MapRenderState mapRenderState = new MapRenderState();

    public CartographyTableScreen(CartographyTableMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        this.titleLabelY -= 2;
    }

    @Override
    public void render(GuiGraphics p_281331_, int p_281706_, int p_282996_, float p_283037_) {
        super.render(p_281331_, p_281706_, p_282996_, p_283037_);
        this.renderTooltip(p_281331_, p_281706_, p_282996_);
    }

    @Override
    protected void renderBg(GuiGraphics p_282101_, float p_282697_, int p_282380_, int p_282327_) {
        int i = this.leftPos;
        int j = this.topPos;
        p_282101_.blit(RenderType::guiTextured, BG_LOCATION, i, j, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
        ItemStack itemstack = this.menu.getSlot(1).getItem();
        boolean flag = itemstack.is(Items.MAP);
        boolean flag1 = itemstack.is(Items.PAPER);
        boolean flag2 = itemstack.is(Items.GLASS_PANE);
        ItemStack itemstack1 = this.menu.getSlot(0).getItem();
        MapId mapid = itemstack1.get(DataComponents.MAP_ID);
        boolean flag3 = false;
        MapItemSavedData mapitemsaveddata;
        if (mapid != null) {
            mapitemsaveddata = MapItem.getSavedData(mapid, this.minecraft.level);
            if (mapitemsaveddata != null) {
                if (mapitemsaveddata.locked) {
                    flag3 = true;
                    if (flag1 || flag2) {
                        p_282101_.blitSprite(RenderType::guiTextured, ERROR_SPRITE, i + 35, j + 31, 28, 21);
                    }
                }

                if (flag1 && mapitemsaveddata.scale >= 4) {
                    flag3 = true;
                    p_282101_.blitSprite(RenderType::guiTextured, ERROR_SPRITE, i + 35, j + 31, 28, 21);
                }
            }
        } else {
            mapitemsaveddata = null;
        }

        this.renderResultingMap(p_282101_, mapid, mapitemsaveddata, flag, flag1, flag2, flag3);
    }

    private void renderResultingMap(
        GuiGraphics pGuiGraphics,
        @Nullable MapId pMapId,
        @Nullable MapItemSavedData pMapData,
        boolean pHasMap,
        boolean pHasPaper,
        boolean pHasGlassPane,
        boolean pIsMaxSize
    ) {
        int i = this.leftPos;
        int j = this.topPos;
        if (pHasPaper && !pIsMaxSize) {
            pGuiGraphics.blitSprite(RenderType::guiTextured, SCALED_MAP_SPRITE, i + 67, j + 13, 66, 66);
            this.renderMap(pGuiGraphics, pMapId, pMapData, i + 85, j + 31, 0.226F);
        } else if (pHasMap) {
            pGuiGraphics.blitSprite(RenderType::guiTextured, DUPLICATED_MAP_SPRITE, i + 67 + 16, j + 13, 50, 66);
            this.renderMap(pGuiGraphics, pMapId, pMapData, i + 86, j + 16, 0.34F);
            pGuiGraphics.pose().pushPose();
            pGuiGraphics.pose().translate(0.0F, 0.0F, 1.0F);
            pGuiGraphics.blitSprite(RenderType::guiTextured, DUPLICATED_MAP_SPRITE, i + 67, j + 13 + 16, 50, 66);
            this.renderMap(pGuiGraphics, pMapId, pMapData, i + 70, j + 32, 0.34F);
            pGuiGraphics.pose().popPose();
        } else if (pHasGlassPane) {
            pGuiGraphics.blitSprite(RenderType::guiTextured, MAP_SPRITE, i + 67, j + 13, 66, 66);
            this.renderMap(pGuiGraphics, pMapId, pMapData, i + 71, j + 17, 0.45F);
            pGuiGraphics.pose().pushPose();
            pGuiGraphics.pose().translate(0.0F, 0.0F, 1.0F);
            pGuiGraphics.blitSprite(RenderType::guiTextured, LOCKED_SPRITE, i + 118, j + 60, 10, 14);
            pGuiGraphics.pose().popPose();
        } else {
            pGuiGraphics.blitSprite(RenderType::guiTextured, MAP_SPRITE, i + 67, j + 13, 66, 66);
            this.renderMap(pGuiGraphics, pMapId, pMapData, i + 71, j + 17, 0.45F);
        }
    }

    private void renderMap(
        GuiGraphics pGuiGraphics, @Nullable MapId pMapId, @Nullable MapItemSavedData pMapData, int pX, int pY, float pScale
    ) {
        if (pMapId != null && pMapData != null) {
            pGuiGraphics.pose().pushPose();
            pGuiGraphics.pose().translate(pX, pY, 1.0F);
            pGuiGraphics.pose().scale(pScale, pScale, 1.0F);
            MapRenderer maprenderer = this.minecraft.getMapRenderer();
            maprenderer.extractRenderState(pMapId, pMapData, this.mapRenderState);
            pGuiGraphics.drawSpecial(p_361600_ -> maprenderer.render(this.mapRenderState, pGuiGraphics.pose(), p_361600_, true, 15728880));
            pGuiGraphics.pose().popPose();
        }
    }
}