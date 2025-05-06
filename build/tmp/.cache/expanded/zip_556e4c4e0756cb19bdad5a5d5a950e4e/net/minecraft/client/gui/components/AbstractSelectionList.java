package net.minecraft.client.gui.components;

import com.google.common.collect.Lists;
import java.util.AbstractList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractSelectionList<E extends AbstractSelectionList.Entry<E>> extends AbstractContainerWidget {
    private static final ResourceLocation MENU_LIST_BACKGROUND = ResourceLocation.withDefaultNamespace("textures/gui/menu_list_background.png");
    private static final ResourceLocation INWORLD_MENU_LIST_BACKGROUND = ResourceLocation.withDefaultNamespace("textures/gui/inworld_menu_list_background.png");
    protected final Minecraft minecraft;
    protected final int itemHeight;
    private final List<E> children = new AbstractSelectionList.TrackedList();
    protected boolean centerListVertically = true;
    private boolean renderHeader;
    protected int headerHeight;
    @Nullable
    private E selected;
    @Nullable
    private E hovered;

    public AbstractSelectionList(Minecraft pMinecraft, int pWidth, int pHeight, int pY, int pItemHeight) {
        super(0, pY, pWidth, pHeight, CommonComponents.EMPTY);
        this.minecraft = pMinecraft;
        this.itemHeight = pItemHeight;
    }

    public AbstractSelectionList(Minecraft pMinecraft, int pWidth, int pHeight, int pY, int pItemHeight, int pHeaderHeight) {
        this(pMinecraft, pWidth, pHeight, pY, pItemHeight);
        this.renderHeader = true;
        this.headerHeight = pHeaderHeight;
    }

    @Nullable
    public E getSelected() {
        return this.selected;
    }

    public void setSelectedIndex(int pSelected) {
        if (pSelected == -1) {
            this.setSelected(null);
        } else if (this.getItemCount() != 0) {
            this.setSelected(this.getEntry(pSelected));
        }
    }

    public void setSelected(@Nullable E pSelected) {
        this.selected = pSelected;
    }

    public E getFirstElement() {
        return this.children.get(0);
    }

    @Nullable
    public E getFocused() {
        return (E)super.getFocused();
    }

    @Override
    public final List<E> children() {
        return this.children;
    }

    protected void clearEntries() {
        this.children.clear();
        this.selected = null;
    }

    public void replaceEntries(Collection<E> pEntries) {
        this.clearEntries();
        this.children.addAll(pEntries);
    }

    protected E getEntry(int pIndex) {
        return this.children().get(pIndex);
    }

    protected int addEntry(E pEntry) {
        this.children.add(pEntry);
        return this.children.size() - 1;
    }

    protected void addEntryToTop(E pEntry) {
        double d0 = this.maxScrollAmount() - this.scrollAmount();
        this.children.add(0, pEntry);
        this.setScrollAmount(this.maxScrollAmount() - d0);
    }

    protected boolean removeEntryFromTop(E pEntry) {
        double d0 = this.maxScrollAmount() - this.scrollAmount();
        boolean flag = this.removeEntry(pEntry);
        this.setScrollAmount(this.maxScrollAmount() - d0);
        return flag;
    }

    protected int getItemCount() {
        return this.children().size();
    }

    protected boolean isSelectedItem(int pIndex) {
        return Objects.equals(this.getSelected(), this.children().get(pIndex));
    }

    @Nullable
    protected final E getEntryAtPosition(double pMouseX, double pMouseY) {
        int i = this.getRowWidth() / 2;
        int j = this.getX() + this.width / 2;
        int k = j - i;
        int l = j + i;
        int i1 = Mth.floor(pMouseY - this.getY()) - this.headerHeight + (int)this.scrollAmount() - 4;
        int j1 = i1 / this.itemHeight;
        return pMouseX >= k && pMouseX <= l && j1 >= 0 && i1 >= 0 && j1 < this.getItemCount() ? this.children().get(j1) : null;
    }

    public void updateSize(int pWidth, HeaderAndFooterLayout pLayout) {
        this.updateSizeAndPosition(pWidth, pLayout.getContentHeight(), pLayout.getHeaderHeight());
    }

    public void updateSizeAndPosition(int pWidth, int pHeight, int pY) {
        this.setSize(pWidth, pHeight);
        this.setPosition(0, pY);
        this.refreshScrollAmount();
    }

    @Override
    protected int contentHeight() {
        return this.getItemCount() * this.itemHeight + this.headerHeight + 4;
    }

    protected void renderHeader(GuiGraphics pGuiGraphics, int pX, int pY) {
    }

    protected void renderDecorations(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
    }

    @Override
    public void renderWidget(GuiGraphics p_282708_, int p_283242_, int p_282891_, float p_283683_) {
        this.hovered = this.isMouseOver(p_283242_, p_282891_) ? this.getEntryAtPosition(p_283242_, p_282891_) : null;
        this.renderListBackground(p_282708_);
        this.enableScissor(p_282708_);
        if (this.renderHeader) {
            int i = this.getRowLeft();
            int j = this.getY() + 4 - (int)this.scrollAmount();
            this.renderHeader(p_282708_, i, j);
        }

        this.renderListItems(p_282708_, p_283242_, p_282891_, p_283683_);
        p_282708_.disableScissor();
        this.renderListSeparators(p_282708_);
        this.renderScrollbar(p_282708_);
        this.renderDecorations(p_282708_, p_283242_, p_282891_);
    }

    protected void renderListSeparators(GuiGraphics pGuiGraphics) {
        ResourceLocation resourcelocation = this.minecraft.level == null ? Screen.HEADER_SEPARATOR : Screen.INWORLD_HEADER_SEPARATOR;
        ResourceLocation resourcelocation1 = this.minecraft.level == null ? Screen.FOOTER_SEPARATOR : Screen.INWORLD_FOOTER_SEPARATOR;
        pGuiGraphics.blit(RenderType::guiTextured, resourcelocation, this.getX(), this.getY() - 2, 0.0F, 0.0F, this.getWidth(), 2, 32, 2);
        pGuiGraphics.blit(RenderType::guiTextured, resourcelocation1, this.getX(), this.getBottom(), 0.0F, 0.0F, this.getWidth(), 2, 32, 2);
    }

    protected void renderListBackground(GuiGraphics pGuiGraphics) {
        ResourceLocation resourcelocation = this.minecraft.level == null ? MENU_LIST_BACKGROUND : INWORLD_MENU_LIST_BACKGROUND;
        pGuiGraphics.blit(
            RenderType::guiTextured,
            resourcelocation,
            this.getX(),
            this.getY(),
            this.getRight(),
            this.getBottom() + (int)this.scrollAmount(),
            this.getWidth(),
            this.getHeight(),
            32,
            32
        );
    }

    protected void enableScissor(GuiGraphics pGuiGraphics) {
        pGuiGraphics.enableScissor(this.getX(), this.getY(), this.getRight(), this.getBottom());
    }

    protected void centerScrollOn(E pEntry) {
        this.setScrollAmount(this.children().indexOf(pEntry) * this.itemHeight + this.itemHeight / 2 - this.height / 2);
    }

    protected void ensureVisible(E pEntry) {
        int i = this.getRowTop(this.children().indexOf(pEntry));
        int j = i - this.getY() - 4 - this.itemHeight;
        if (j < 0) {
            this.scroll(j);
        }

        int k = this.getBottom() - i - this.itemHeight - this.itemHeight;
        if (k < 0) {
            this.scroll(-k);
        }
    }

    private void scroll(int pScroll) {
        this.setScrollAmount(this.scrollAmount() + pScroll);
    }

    @Override
    protected double scrollRate() {
        return this.itemHeight / 2.0;
    }

    @Override
    protected int scrollBarX() {
        return this.getRowRight() + 6 + 2;
    }

    @Override
    public Optional<GuiEventListener> getChildAt(double p_376745_, double p_377088_) {
        return Optional.ofNullable(this.getEntryAtPosition(p_376745_, p_377088_));
    }

    @Override
    public void setFocused(@Nullable GuiEventListener p_265738_) {
        E e = this.getFocused();
        if (e != p_265738_ && e instanceof ContainerEventHandler containereventhandler) {
            containereventhandler.setFocused(null);
        }

        super.setFocused(p_265738_);
        int i = this.children.indexOf(p_265738_);
        if (i >= 0) {
            E e1 = this.children.get(i);
            this.setSelected(e1);
            if (this.minecraft.getLastInputType().isKeyboard()) {
                this.ensureVisible(e1);
            }
        }
    }

    @Nullable
    protected E nextEntry(ScreenDirection pDirection) {
        return this.nextEntry(pDirection, p_93510_ -> true);
    }

    @Nullable
    protected E nextEntry(ScreenDirection pDirection, Predicate<E> pPredicate) {
        return this.nextEntry(pDirection, pPredicate, this.getSelected());
    }

    @Nullable
    protected E nextEntry(ScreenDirection pDirection, Predicate<E> pPredicate, @Nullable E pSelected) {
        int i = switch (pDirection) {
            case RIGHT, LEFT -> 0;
            case UP -> -1;
            case DOWN -> 1;
        };
        if (!this.children().isEmpty() && i != 0) {
            int j;
            if (pSelected == null) {
                j = i > 0 ? 0 : this.children().size() - 1;
            } else {
                j = this.children().indexOf(pSelected) + i;
            }

            for (int k = j; k >= 0 && k < this.children.size(); k += i) {
                E e = this.children().get(k);
                if (pPredicate.test(e)) {
                    return e;
                }
            }
        }

        return null;
    }

    protected void renderListItems(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        int i = this.getRowLeft();
        int j = this.getRowWidth();
        int k = this.itemHeight - 4;
        int l = this.getItemCount();

        for (int i1 = 0; i1 < l; i1++) {
            int j1 = this.getRowTop(i1);
            int k1 = this.getRowBottom(i1);
            if (k1 >= this.getY() && j1 <= this.getBottom()) {
                this.renderItem(pGuiGraphics, pMouseX, pMouseY, pPartialTick, i1, i, j1, j, k);
            }
        }
    }

    protected void renderItem(
        GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick, int pIndex, int pLeft, int pTop, int pWidth, int pHeight
    ) {
        E e = this.getEntry(pIndex);
        e.renderBack(pGuiGraphics, pIndex, pTop, pLeft, pWidth, pHeight, pMouseX, pMouseY, Objects.equals(this.hovered, e), pPartialTick);
        if (this.isSelectedItem(pIndex)) {
            int i = this.isFocused() ? -1 : -8355712;
            this.renderSelection(pGuiGraphics, pTop, pWidth, pHeight, i, -16777216);
        }

        e.render(pGuiGraphics, pIndex, pTop, pLeft, pWidth, pHeight, pMouseX, pMouseY, Objects.equals(this.hovered, e), pPartialTick);
    }

    protected void renderSelection(GuiGraphics pGuiGraphics, int pTop, int pWidth, int pHeight, int pOuterColor, int pInnerColor) {
        int i = this.getX() + (this.width - pWidth) / 2;
        int j = this.getX() + (this.width + pWidth) / 2;
        pGuiGraphics.fill(i, pTop - 2, j, pTop + pHeight + 2, pOuterColor);
        pGuiGraphics.fill(i + 1, pTop - 1, j - 1, pTop + pHeight + 1, pInnerColor);
    }

    public int getRowLeft() {
        return this.getX() + this.width / 2 - this.getRowWidth() / 2 + 2;
    }

    public int getRowRight() {
        return this.getRowLeft() + this.getRowWidth();
    }

    public int getRowTop(int pIndex) {
        return this.getY() + 4 - (int)this.scrollAmount() + pIndex * this.itemHeight + this.headerHeight;
    }

    public int getRowBottom(int pIndex) {
        return this.getRowTop(pIndex) + this.itemHeight;
    }

    public int getRowWidth() {
        return 220;
    }

    @Override
    public NarratableEntry.NarrationPriority narrationPriority() {
        if (this.isFocused()) {
            return NarratableEntry.NarrationPriority.FOCUSED;
        } else {
            return this.hovered != null ? NarratableEntry.NarrationPriority.HOVERED : NarratableEntry.NarrationPriority.NONE;
        }
    }

    @Nullable
    protected E remove(int pIndex) {
        E e = this.children.get(pIndex);
        return this.removeEntry(this.children.get(pIndex)) ? e : null;
    }

    protected boolean removeEntry(E pEntry) {
        boolean flag = this.children.remove(pEntry);
        if (flag && pEntry == this.getSelected()) {
            this.setSelected(null);
        }

        return flag;
    }

    @Nullable
    protected E getHovered() {
        return this.hovered;
    }

    void bindEntryToSelf(AbstractSelectionList.Entry<E> pEntry) {
        pEntry.list = this;
    }

    protected void narrateListElementPosition(NarrationElementOutput pNarrationElementOutput, E pEntry) {
        List<E> list = this.children();
        if (list.size() > 1) {
            int i = list.indexOf(pEntry);
            if (i != -1) {
                pNarrationElementOutput.add(NarratedElementType.POSITION, Component.translatable("narrator.position.list", i + 1, list.size()));
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    protected abstract static class Entry<E extends AbstractSelectionList.Entry<E>> implements GuiEventListener {
        @Deprecated
        protected AbstractSelectionList<E> list;

        @Override
        public void setFocused(boolean p_265302_) {
        }

        @Override
        public boolean isFocused() {
            return this.list.getFocused() == this;
        }

        public abstract void render(
            GuiGraphics pGuiGraphics,
            int pIndex,
            int pTop,
            int pLeft,
            int pWidth,
            int pHeight,
            int pMouseX,
            int pMouseY,
            boolean pHovering,
            float pPartialTick
        );

        public void renderBack(
            GuiGraphics pGuiGraphics,
            int pIndex,
            int pTop,
            int pLeft,
            int pWidth,
            int pHeight,
            int pMouseX,
            int pMouseY,
            boolean pIsMouseOver,
            float pPartialTick
        ) {
        }

        @Override
        public boolean isMouseOver(double pMouseX, double pMouseY) {
            return Objects.equals(this.list.getEntryAtPosition(pMouseX, pMouseY), this);
        }
    }

    @OnlyIn(Dist.CLIENT)
    class TrackedList extends AbstractList<E> {
        private final List<E> delegate = Lists.newArrayList();

        public E get(int pIndex) {
            return this.delegate.get(pIndex);
        }

        @Override
        public int size() {
            return this.delegate.size();
        }

        public E set(int pIndex, E pEntry) {
            E e = this.delegate.set(pIndex, pEntry);
            AbstractSelectionList.this.bindEntryToSelf(pEntry);
            return e;
        }

        public void add(int pIndex, E pEntry) {
            this.delegate.add(pIndex, pEntry);
            AbstractSelectionList.this.bindEntryToSelf(pEntry);
        }

        public E remove(int pIndex) {
            return this.delegate.remove(pIndex);
        }
    }
}