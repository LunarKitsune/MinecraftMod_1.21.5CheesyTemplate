package net.minecraft.world.level;

public interface ColorMapColorUtil {
    static int get(double pX, double pY, int[] pPixels, int pDefaultValue) {
        pY *= pX;
        int i = (int)((1.0 - pX) * 255.0);
        int j = (int)((1.0 - pY) * 255.0);
        int k = j << 8 | i;
        return k >= pPixels.length ? pDefaultValue : pPixels[k];
    }
}