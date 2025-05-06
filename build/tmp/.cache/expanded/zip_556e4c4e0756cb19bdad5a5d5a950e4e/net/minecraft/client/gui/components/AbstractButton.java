package net.minecraft.client.gui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.CommonInputs;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractButton extends AbstractWidget {
    protected static final int TEXT_MARGIN = 2;
    protected static final WidgetSprites SPRITES = new WidgetSprites(
        ResourceLocation.withDefaultNamespace("widget/button"),
        ResourceLocation.withDefaultNamespace("widget/button_disabled"),
        ResourceLocation.withDefaultNamespace("widget/button_highlighted")
    );

    public AbstractButton(int p_93365_, int p_93366_, int p_93367_, int p_93368_, Component p_93369_) {
        super(p_93365_, p_93366_, p_93367_, p_93368_, p_93369_);
    }

    public abstract void onPress();

    @Override
    protected void renderWidget(GuiGraphics p_281670_, int p_282682_, int p_281714_, float p_282542_) {
        Minecraft minecraft = Minecraft.getInstance();
        p_281670_.blitSprite(
            RenderType::guiTextured,
            SPRITES.get(this.active, this.isHoveredOrFocused()),
            this.getX(),
            this.getY(),
            this.getWidth(),
            this.getHeight(),
            ARGB.white(this.alpha)
        );
        int i = getFGColor();
        this.renderString(p_281670_, minecraft.font, i | Mth.ceil(this.alpha * 255.0F) << 24);
    }

    public void renderString(GuiGraphics pGuiGraphics, Font pFont, int pColor) {
        this.renderScrollingString(pGuiGraphics, pFont, 2, pColor);
    }

    @Override
    public void onClick(double pMouseX, double pMouseY) {
        this.onPress();
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if (!this.active || !this.visible) {
            return false;
        } else if (CommonInputs.selected(pKeyCode)) {
            this.playDownSound(Minecraft.getInstance().getSoundManager());
            this.onPress();
            return true;
        } else {
            return false;
        }
    }
}
