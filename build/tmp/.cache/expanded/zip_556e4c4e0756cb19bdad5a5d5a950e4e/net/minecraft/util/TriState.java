package net.minecraft.util;

public enum TriState {
    TRUE,
    FALSE,
    DEFAULT;

    public boolean toBoolean(boolean pDefaultValue) {
        return switch (this) {
            case TRUE -> true;
            case FALSE -> false;
            default -> pDefaultValue;
        };
    }
}