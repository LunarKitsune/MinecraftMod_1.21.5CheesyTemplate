package net.minecraft.client.gui.screens.inventory;

import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.screens.recipebook.FurnaceRecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractFurnaceScreen<T extends AbstractFurnaceMenu> extends AbstractRecipeBookScreen<T> {
    private final ResourceLocation texture;
    private final ResourceLocation litProgressSprite;
    private final ResourceLocation burnProgressSprite;

    public AbstractFurnaceScreen(
        T pMenu,
        Inventory pPlayerInventory,
        Component pTitle,
        Component pRecipeFilterName,
        ResourceLocation pTexture,
        ResourceLocation pLitProgressSprite,
        ResourceLocation pBurnProgressSprite,
        List<RecipeBookComponent.TabInfo> pTabInfos
    ) {
        super(pMenu, new FurnaceRecipeBookComponent(pMenu, pRecipeFilterName, pTabInfos), pPlayerInventory, pTitle);
        this.texture = pTexture;
        this.litProgressSprite = pLitProgressSprite;
        this.burnProgressSprite = pBurnProgressSprite;
    }

    @Override
    public void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    protected ScreenPosition getRecipeBookButtonPosition() {
        return new ScreenPosition(this.leftPos + 20, this.height / 2 - 49);
    }

    @Override
    protected void renderBg(GuiGraphics p_282928_, float p_281631_, int p_281252_, int p_281891_) {
        int i = this.leftPos;
        int j = this.topPos;
        p_282928_.blit(RenderType::guiTextured, this.texture, i, j, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
        if (this.menu.isLit()) {
            int k = 14;
            int l = Mth.ceil(this.menu.getLitProgress() * 13.0F) + 1;
            p_282928_.blitSprite(RenderType::guiTextured, this.litProgressSprite, 14, 14, 0, 14 - l, i + 56, j + 36 + 14 - l, 14, l);
        }

        int i1 = 24;
        int j1 = Mth.ceil(this.menu.getBurnProgress() * 24.0F);
        p_282928_.blitSprite(RenderType::guiTextured, this.burnProgressSprite, 24, 16, 0, 0, i + 79, j + 34, j1, 16);
    }
}