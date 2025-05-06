package net.minecraft.client.gui.components;

import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.CommonComponents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class StateSwitchingButton extends AbstractWidget {
    @Nullable
    protected WidgetSprites sprites;
    protected boolean isStateTriggered;

    public StateSwitchingButton(int pX, int pY, int pWidth, int pHeight, boolean pInitialState) {
        super(pX, pY, pWidth, pHeight, CommonComponents.EMPTY);
        this.isStateTriggered = pInitialState;
    }

    public void initTextureValues(WidgetSprites pSprites) {
        this.sprites = pSprites;
    }

    public void setStateTriggered(boolean pTriggered) {
        this.isStateTriggered = pTriggered;
    }

    public boolean isStateTriggered() {
        return this.isStateTriggered;
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput p_259073_) {
        this.defaultButtonNarrationText(p_259073_);
    }

    @Override
    public void renderWidget(GuiGraphics p_283051_, int p_283010_, int p_281379_, float p_283453_) {
        if (this.sprites != null) {
            p_283051_.blitSprite(
                RenderType::guiTextured,
                this.sprites.get(this.isStateTriggered, this.isHoveredOrFocused()),
                this.getX(),
                this.getY(),
                this.width,
                this.height
            );
        }
    }
}