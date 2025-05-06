package net.minecraft.client.gui.components.toasts;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TutorialToast implements Toast {
    private static final ResourceLocation BACKGROUND_SPRITE = ResourceLocation.withDefaultNamespace("toast/tutorial");
    public static final int PROGRESS_BAR_WIDTH = 154;
    public static final int PROGRESS_BAR_HEIGHT = 1;
    public static final int PROGRESS_BAR_X = 3;
    public static final int PROGRESS_BAR_MARGIN_BOTTOM = 4;
    private static final int PADDING_TOP = 7;
    private static final int PADDING_BOTTOM = 3;
    private static final int LINE_SPACING = 11;
    private static final int TEXT_LEFT = 30;
    private static final int TEXT_WIDTH = 126;
    private final TutorialToast.Icons icon;
    private final List<FormattedCharSequence> lines;
    private Toast.Visibility visibility = Toast.Visibility.SHOW;
    private long lastSmoothingTime;
    private float smoothedProgress;
    private float progress;
    private final boolean progressable;
    private final int timeToDisplayMs;

    public TutorialToast(Font pFont, TutorialToast.Icons pIcon, Component pTitle, @Nullable Component pMessage, boolean pProgressable, int pTimeToDisplayMs) {
        this.icon = pIcon;
        this.lines = new ArrayList<>(2);
        this.lines.addAll(pFont.split(pTitle.copy().withColor(-11534256), 126));
        if (pMessage != null) {
            this.lines.addAll(pFont.split(pMessage, 126));
        }

        this.progressable = pProgressable;
        this.timeToDisplayMs = pTimeToDisplayMs;
    }

    public TutorialToast(Font pFont, TutorialToast.Icons pIcon, Component pTitle, @Nullable Component pMessage, boolean pProgressable) {
        this(pFont, pIcon, pTitle, pMessage, pProgressable, 0);
    }

    @Override
    public Toast.Visibility getWantedVisibility() {
        return this.visibility;
    }

    @Override
    public void update(ToastManager p_369846_, long p_364600_) {
        if (this.timeToDisplayMs > 0) {
            this.progress = Math.min((float)p_364600_ / this.timeToDisplayMs, 1.0F);
            this.smoothedProgress = this.progress;
            this.lastSmoothingTime = p_364600_;
            if (p_364600_ > this.timeToDisplayMs) {
                this.hide();
            }
        } else if (this.progressable) {
            this.smoothedProgress = Mth.clampedLerp(this.smoothedProgress, this.progress, (float)(p_364600_ - this.lastSmoothingTime) / 100.0F);
            this.lastSmoothingTime = p_364600_;
        }
    }

    @Override
    public int height() {
        return 7 + this.contentHeight() + 3;
    }

    private int contentHeight() {
        return Math.max(this.lines.size(), 2) * 11;
    }

    @Override
    public void render(GuiGraphics p_283197_, Font p_365679_, long p_281902_) {
        int i = this.height();
        p_283197_.blitSprite(RenderType::guiTextured, BACKGROUND_SPRITE, 0, 0, this.width(), i);
        this.icon.render(p_283197_, 6, 6);
        int j = this.lines.size() * 11;
        int k = 7 + (this.contentHeight() - j) / 2;

        for (int l = 0; l < this.lines.size(); l++) {
            p_283197_.drawString(p_365679_, this.lines.get(l), 30, k + l * 11, -16777216, false);
        }

        if (this.progressable) {
            int j1 = i - 4;
            p_283197_.fill(3, j1, 157, j1 + 1, -1);
            int i1;
            if (this.progress >= this.smoothedProgress) {
                i1 = -16755456;
            } else {
                i1 = -11206656;
            }

            p_283197_.fill(3, j1, (int)(3.0F + 154.0F * this.smoothedProgress), j1 + 1, i1);
        }
    }

    public void hide() {
        this.visibility = Toast.Visibility.HIDE;
    }

    public void updateProgress(float pProgress) {
        this.progress = pProgress;
    }

    @OnlyIn(Dist.CLIENT)
    public static enum Icons {
        MOVEMENT_KEYS(ResourceLocation.withDefaultNamespace("toast/movement_keys")),
        MOUSE(ResourceLocation.withDefaultNamespace("toast/mouse")),
        TREE(ResourceLocation.withDefaultNamespace("toast/tree")),
        RECIPE_BOOK(ResourceLocation.withDefaultNamespace("toast/recipe_book")),
        WOODEN_PLANKS(ResourceLocation.withDefaultNamespace("toast/wooden_planks")),
        SOCIAL_INTERACTIONS(ResourceLocation.withDefaultNamespace("toast/social_interactions")),
        RIGHT_CLICK(ResourceLocation.withDefaultNamespace("toast/right_click"));

        private final ResourceLocation sprite;

        private Icons(final ResourceLocation pSprite) {
            this.sprite = pSprite;
        }

        public void render(GuiGraphics pGuiGraphics, int pX, int pY) {
            pGuiGraphics.blitSprite(RenderType::guiTextured, this.sprite, pX, pY, 20, 20);
        }
    }
}