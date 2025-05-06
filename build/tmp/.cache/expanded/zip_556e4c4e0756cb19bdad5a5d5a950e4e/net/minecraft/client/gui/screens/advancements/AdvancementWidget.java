package net.minecraft.client.gui.screens.advancements;

import com.google.common.collect.Lists;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AdvancementWidget {
    private static final ResourceLocation TITLE_BOX_SPRITE = ResourceLocation.withDefaultNamespace("advancements/title_box");
    private static final int HEIGHT = 26;
    private static final int BOX_X = 0;
    private static final int BOX_WIDTH = 200;
    private static final int FRAME_WIDTH = 26;
    private static final int ICON_X = 8;
    private static final int ICON_Y = 5;
    private static final int ICON_WIDTH = 26;
    private static final int TITLE_PADDING_LEFT = 3;
    private static final int TITLE_PADDING_RIGHT = 5;
    private static final int TITLE_X = 32;
    private static final int TITLE_PADDING_TOP = 9;
    private static final int TITLE_PADDING_BOTTOM = 8;
    private static final int TITLE_MAX_WIDTH = 163;
    private static final int TITLE_MIN_WIDTH = 80;
    private static final int[] TEST_SPLIT_OFFSETS = new int[]{0, 10, -10, 25, -25};
    private final AdvancementTab tab;
    private final AdvancementNode advancementNode;
    private final DisplayInfo display;
    private final List<FormattedCharSequence> titleLines;
    private final int width;
    private final List<FormattedCharSequence> description;
    private final Minecraft minecraft;
    @Nullable
    private AdvancementWidget parent;
    private final List<AdvancementWidget> children = Lists.newArrayList();
    @Nullable
    private AdvancementProgress progress;
    private final int x;
    private final int y;

    public AdvancementWidget(AdvancementTab pTab, Minecraft pMinecraft, AdvancementNode pAdvancementNode, DisplayInfo pDisplay) {
        this.tab = pTab;
        this.advancementNode = pAdvancementNode;
        this.display = pDisplay;
        this.minecraft = pMinecraft;
        this.titleLines = pMinecraft.font.split(pDisplay.getTitle(), 163);
        this.x = Mth.floor(pDisplay.getX() * 28.0F);
        this.y = Mth.floor(pDisplay.getY() * 27.0F);
        int i = Math.max(this.titleLines.stream().mapToInt(pMinecraft.font::width).max().orElse(0), 80);
        int j = this.getMaxProgressWidth();
        int k = 29 + i + j;
        this.description = Language.getInstance()
            .getVisualOrder(this.findOptimalLines(ComponentUtils.mergeStyles(pDisplay.getDescription().copy(), Style.EMPTY.withColor(pDisplay.getType().getChatColor())), k));

        for (FormattedCharSequence formattedcharsequence : this.description) {
            k = Math.max(k, pMinecraft.font.width(formattedcharsequence));
        }

        this.width = k + 3 + 5;
    }

    private int getMaxProgressWidth() {
        int i = this.advancementNode.advancement().requirements().size();
        if (i <= 1) {
            return 0;
        } else {
            int j = 8;
            Component component = Component.translatable("advancements.progress", i, i);
            return this.minecraft.font.width(component) + 8;
        }
    }

    private static float getMaxWidth(StringSplitter pManager, List<FormattedText> pText) {
        return (float)pText.stream().mapToDouble(pManager::stringWidth).max().orElse(0.0);
    }

    private List<FormattedText> findOptimalLines(Component pComponent, int pMaxWidth) {
        StringSplitter stringsplitter = this.minecraft.font.getSplitter();
        List<FormattedText> list = null;
        float f = Float.MAX_VALUE;

        for (int i : TEST_SPLIT_OFFSETS) {
            List<FormattedText> list1 = stringsplitter.splitLines(pComponent, pMaxWidth - i, Style.EMPTY);
            float f1 = Math.abs(getMaxWidth(stringsplitter, list1) - pMaxWidth);
            if (f1 <= 10.0F) {
                return list1;
            }

            if (f1 < f) {
                f = f1;
                list = list1;
            }
        }

        return list;
    }

    @Nullable
    private AdvancementWidget getFirstVisibleParent(AdvancementNode pAdvancement) {
        do {
            pAdvancement = pAdvancement.parent();
        } while (pAdvancement != null && pAdvancement.advancement().display().isEmpty());

        return pAdvancement != null && !pAdvancement.advancement().display().isEmpty() ? this.tab.getWidget(pAdvancement.holder()) : null;
    }

    public void drawConnectivity(GuiGraphics pGuiGraphics, int pX, int pY, boolean pDropShadow) {
        if (this.parent != null) {
            int i = pX + this.parent.x + 13;
            int j = pX + this.parent.x + 26 + 4;
            int k = pY + this.parent.y + 13;
            int l = pX + this.x + 13;
            int i1 = pY + this.y + 13;
            int j1 = pDropShadow ? -16777216 : -1;
            if (pDropShadow) {
                pGuiGraphics.hLine(j, i, k - 1, j1);
                pGuiGraphics.hLine(j + 1, i, k, j1);
                pGuiGraphics.hLine(j, i, k + 1, j1);
                pGuiGraphics.hLine(l, j - 1, i1 - 1, j1);
                pGuiGraphics.hLine(l, j - 1, i1, j1);
                pGuiGraphics.hLine(l, j - 1, i1 + 1, j1);
                pGuiGraphics.vLine(j - 1, i1, k, j1);
                pGuiGraphics.vLine(j + 1, i1, k, j1);
            } else {
                pGuiGraphics.hLine(j, i, k, j1);
                pGuiGraphics.hLine(l, j, i1, j1);
                pGuiGraphics.vLine(j, i1, k, j1);
            }
        }

        for (AdvancementWidget advancementwidget : this.children) {
            advancementwidget.drawConnectivity(pGuiGraphics, pX, pY, pDropShadow);
        }
    }

    public void draw(GuiGraphics pGuiGraphics, int pX, int pY) {
        if (!this.display.isHidden() || this.progress != null && this.progress.isDone()) {
            float f = this.progress == null ? 0.0F : this.progress.getPercent();
            AdvancementWidgetType advancementwidgettype;
            if (f >= 1.0F) {
                advancementwidgettype = AdvancementWidgetType.OBTAINED;
            } else {
                advancementwidgettype = AdvancementWidgetType.UNOBTAINED;
            }

            pGuiGraphics.blitSprite(
                RenderType::guiTextured,
                advancementwidgettype.frameSprite(this.display.getType()),
                pX + this.x + 3,
                pY + this.y,
                26,
                26
            );
            pGuiGraphics.renderFakeItem(this.display.getIcon(), pX + this.x + 8, pY + this.y + 5);
        }

        for (AdvancementWidget advancementwidget : this.children) {
            advancementwidget.draw(pGuiGraphics, pX, pY);
        }
    }

    public int getWidth() {
        return this.width;
    }

    public void setProgress(AdvancementProgress pProgress) {
        this.progress = pProgress;
    }

    public void addChild(AdvancementWidget pAdvancementWidget) {
        this.children.add(pAdvancementWidget);
    }

    public void drawHover(GuiGraphics pGuiGraphics, int pX, int pY, float pFade, int pWidth, int pHeight) {
        Font font = this.minecraft.font;
        int i = 9 * this.titleLines.size() + 9 + 8;
        int j = pY + this.y + (26 - i) / 2;
        int k = j + i;
        int l = this.description.size() * 9;
        int i1 = 6 + l;
        boolean flag = pWidth + pX + this.x + this.width + 26 >= this.tab.getScreen().width;
        Component component = this.progress == null ? null : this.progress.getProgressText();
        int j1 = component == null ? 0 : font.width(component);
        boolean flag1 = k + i1 >= 113;
        float f = this.progress == null ? 0.0F : this.progress.getPercent();
        int k1 = Mth.floor(f * this.width);
        AdvancementWidgetType advancementwidgettype;
        AdvancementWidgetType advancementwidgettype1;
        AdvancementWidgetType advancementwidgettype2;
        if (f >= 1.0F) {
            k1 = this.width / 2;
            advancementwidgettype = AdvancementWidgetType.OBTAINED;
            advancementwidgettype1 = AdvancementWidgetType.OBTAINED;
            advancementwidgettype2 = AdvancementWidgetType.OBTAINED;
        } else if (k1 < 2) {
            k1 = this.width / 2;
            advancementwidgettype = AdvancementWidgetType.UNOBTAINED;
            advancementwidgettype1 = AdvancementWidgetType.UNOBTAINED;
            advancementwidgettype2 = AdvancementWidgetType.UNOBTAINED;
        } else if (k1 > this.width - 2) {
            k1 = this.width / 2;
            advancementwidgettype = AdvancementWidgetType.OBTAINED;
            advancementwidgettype1 = AdvancementWidgetType.OBTAINED;
            advancementwidgettype2 = AdvancementWidgetType.UNOBTAINED;
        } else {
            advancementwidgettype = AdvancementWidgetType.OBTAINED;
            advancementwidgettype1 = AdvancementWidgetType.UNOBTAINED;
            advancementwidgettype2 = AdvancementWidgetType.UNOBTAINED;
        }

        int l1 = this.width - k1;
        int i2;
        if (flag) {
            i2 = pX + this.x - this.width + 26 + 6;
        } else {
            i2 = pX + this.x;
        }

        int j2 = i + i1;
        if (!this.description.isEmpty()) {
            if (flag1) {
                pGuiGraphics.blitSprite(RenderType::guiTextured, TITLE_BOX_SPRITE, i2, k - j2, this.width, j2);
            } else {
                pGuiGraphics.blitSprite(RenderType::guiTextured, TITLE_BOX_SPRITE, i2, j, this.width, j2);
            }
        }

        if (advancementwidgettype != advancementwidgettype1) {
            pGuiGraphics.blitSprite(RenderType::guiTextured, advancementwidgettype.boxSprite(), 200, i, 0, 0, i2, j, k1, i);
            pGuiGraphics.blitSprite(RenderType::guiTextured, advancementwidgettype1.boxSprite(), 200, i, 200 - l1, 0, i2 + k1, j, l1, i);
        } else {
            pGuiGraphics.blitSprite(RenderType::guiTextured, advancementwidgettype.boxSprite(), i2, j, this.width, i);
        }

        pGuiGraphics.blitSprite(
            RenderType::guiTextured,
            advancementwidgettype2.frameSprite(this.display.getType()),
            pX + this.x + 3,
            pY + this.y,
            26,
            26
        );
        int k2 = i2 + 5;
        if (flag) {
            this.drawMultilineText(pGuiGraphics, this.titleLines, k2, j + 9, -1);
            if (component != null) {
                pGuiGraphics.drawString(font, component, pX + this.x - j1, j + 9, -1);
            }
        } else {
            this.drawMultilineText(pGuiGraphics, this.titleLines, pX + this.x + 32, j + 9, -1);
            if (component != null) {
                pGuiGraphics.drawString(font, component, pX + this.x + this.width - j1 - 5, j + 9, -1);
            }
        }

        if (flag1) {
            this.drawMultilineText(pGuiGraphics, this.description, k2, j - l + 1, -16711936);
        } else {
            this.drawMultilineText(pGuiGraphics, this.description, k2, k, -16711936);
        }

        pGuiGraphics.renderFakeItem(this.display.getIcon(), pX + this.x + 8, pY + this.y + 5);
    }

    private void drawMultilineText(GuiGraphics pGuiGraphics, List<FormattedCharSequence> pText, int pX, int pY, int pColor) {
        Font font = this.minecraft.font;

        for (int i = 0; i < pText.size(); i++) {
            pGuiGraphics.drawString(font, pText.get(i), pX, pY + i * 9, pColor);
        }
    }

    public boolean isMouseOver(int pX, int pY, int pMouseX, int pMouseY) {
        if (!this.display.isHidden() || this.progress != null && this.progress.isDone()) {
            int i = pX + this.x;
            int j = i + 26;
            int k = pY + this.y;
            int l = k + 26;
            return pMouseX >= i && pMouseX <= j && pMouseY >= k && pMouseY <= l;
        } else {
            return false;
        }
    }

    public void attachToParent() {
        if (this.parent == null && this.advancementNode.parent() != null) {
            this.parent = this.getFirstVisibleParent(this.advancementNode);
            if (this.parent != null) {
                this.parent.addChild(this);
            }
        }
    }

    public int getY() {
        return this.y;
    }

    public int getX() {
        return this.x;
    }
}