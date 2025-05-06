package net.minecraft.world.level;

public class DryFoliageColor {
    public static final int FOLIAGE_DRY_DEFAULT = -10732494;
    private static int[] pixels = new int[65536];

    public static void init(int[] pPixels) {
        pixels = pPixels;
    }

    public static int get(double pX, double pY) {
        return ColorMapColorUtil.get(pX, pY, pixels, -10732494);
    }
}