package net.minecraft.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector2i;

@OnlyIn(Dist.CLIENT)
public class ScrollWheelHandler {
    private double accumulatedScrollX;
    private double accumulatedScrollY;

    public Vector2i onMouseScroll(double pXOffset, double pYOffset) {
        if (this.accumulatedScrollX != 0.0 && Math.signum(pXOffset) != Math.signum(this.accumulatedScrollX)) {
            this.accumulatedScrollX = 0.0;
        }

        if (this.accumulatedScrollY != 0.0 && Math.signum(pYOffset) != Math.signum(this.accumulatedScrollY)) {
            this.accumulatedScrollY = 0.0;
        }

        this.accumulatedScrollX += pXOffset;
        this.accumulatedScrollY += pYOffset;
        int i = (int)this.accumulatedScrollX;
        int j = (int)this.accumulatedScrollY;
        if (i == 0 && j == 0) {
            return new Vector2i(0, 0);
        } else {
            this.accumulatedScrollX -= i;
            this.accumulatedScrollY -= j;
            return new Vector2i(i, j);
        }
    }

    public static int getNextScrollWheelSelection(double pYOffset, int pSelected, int pSelectionSize) {
        int i = (int)Math.signum(pYOffset);
        pSelected -= i;
        pSelected = Math.max(-1, pSelected);

        while (pSelected < 0) {
            pSelected += pSelectionSize;
        }

        while (pSelected >= pSelectionSize) {
            pSelected -= pSelectionSize;
        }

        return pSelected;
    }
}