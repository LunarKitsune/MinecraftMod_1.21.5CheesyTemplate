package net.minecraft.client.gui.layouts;

import java.util.function.Consumer;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SpacerElement implements LayoutElement {
    private int x;
    private int y;
    private final int width;
    private final int height;

    public SpacerElement(int pWidth, int pHeight) {
        this(0, 0, pWidth, pHeight);
    }

    public SpacerElement(int pX, int pY, int pWidth, int pHeight) {
        this.x = pX;
        this.y = pY;
        this.width = pWidth;
        this.height = pHeight;
    }

    public static SpacerElement width(int pWidth) {
        return new SpacerElement(pWidth, 0);
    }

    public static SpacerElement height(int pHeight) {
        return new SpacerElement(0, pHeight);
    }

    @Override
    public void setX(int p_265605_) {
        this.x = p_265605_;
    }

    @Override
    public void setY(int p_265406_) {
        this.y = p_265406_;
    }

    @Override
    public int getX() {
        return this.x;
    }

    @Override
    public int getY() {
        return this.y;
    }

    @Override
    public int getWidth() {
        return this.width;
    }

    @Override
    public int getHeight() {
        return this.height;
    }

    @Override
    public void visitWidgets(Consumer<AbstractWidget> p_265477_) {
    }
}