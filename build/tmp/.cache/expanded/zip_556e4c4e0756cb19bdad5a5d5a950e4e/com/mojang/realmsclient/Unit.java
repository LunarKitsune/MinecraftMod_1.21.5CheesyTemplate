package com.mojang.realmsclient;

import java.util.Locale;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public enum Unit {
    B,
    KB,
    MB,
    GB;

    private static final int BASE_UNIT = 1024;

    public static Unit getLargest(long pBytes) {
        if (pBytes < 1024L) {
            return B;
        } else {
            try {
                int i = (int)(Math.log(pBytes) / Math.log(1024.0));
                String s = String.valueOf("KMGTPE".charAt(i - 1));
                return valueOf(s + "B");
            } catch (Exception exception) {
                return GB;
            }
        }
    }

    public static double convertTo(long pBytes, Unit pUnit) {
        return pUnit == B ? pBytes : pBytes / Math.pow(1024.0, pUnit.ordinal());
    }

    public static String humanReadable(long pBytes) {
        int i = 1024;
        if (pBytes < 1024L) {
            return pBytes + " B";
        } else {
            int j = (int)(Math.log(pBytes) / Math.log(1024.0));
            String s = "KMGTPE".charAt(j - 1) + "";
            return String.format(Locale.ROOT, "%.1f %sB", pBytes / Math.pow(1024.0, j), s);
        }
    }

    public static String humanReadable(long pBytes, Unit pUnit) {
        return String.format(Locale.ROOT, "%." + (pUnit == GB ? "1" : "0") + "f %s", convertTo(pBytes, pUnit), pUnit.name());
    }
}