package net.minecraft.world.level;

public class GrassColor {
    private static int[] pixels = new int[65536];

    public static void init(int[] pGrassBuffer) {
        pixels = pGrassBuffer;
    }

    public static int get(double pTemperature, double pHumidity) {
        return ColorMapColorUtil.get(pTemperature, pHumidity, pixels, -65281);
    }

    public static int getDefaultColor() {
        return get(0.5, 1.0);
    }
}