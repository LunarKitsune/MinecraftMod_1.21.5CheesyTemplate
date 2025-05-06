package net.minecraft.server.packs;

import java.util.Map;
import net.minecraft.server.packs.metadata.MetadataSectionType;

public class BuiltInMetadata {
    private static final BuiltInMetadata EMPTY = new BuiltInMetadata(Map.of());
    private final Map<MetadataSectionType<?>, ?> values;

    private BuiltInMetadata(Map<MetadataSectionType<?>, ?> pValues) {
        this.values = pValues;
    }

    public <T> T get(MetadataSectionType<T> pType) {
        return (T)this.values.get(pType);
    }

    public static BuiltInMetadata of() {
        return EMPTY;
    }

    public static <T> BuiltInMetadata of(MetadataSectionType<T> pType, T pValue) {
        return new BuiltInMetadata(Map.of(pType, pValue));
    }

    public static <T1, T2> BuiltInMetadata of(MetadataSectionType<T1> pType1, T1 pValue1, MetadataSectionType<T2> pType2, T2 pValue2) {
        return new BuiltInMetadata(Map.of(pType1, pValue1, pType2, (T1)pValue2));
    }
}