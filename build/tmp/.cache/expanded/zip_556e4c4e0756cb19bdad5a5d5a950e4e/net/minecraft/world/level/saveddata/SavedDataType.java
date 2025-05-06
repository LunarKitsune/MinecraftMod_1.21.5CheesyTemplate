package net.minecraft.world.level.saveddata;

import com.mojang.serialization.Codec;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.util.datafix.DataFixTypes;

public record SavedDataType<T extends SavedData>(
    String id, Function<SavedData.Context, T> constructor, Function<SavedData.Context, Codec<T>> codec, DataFixTypes dataFixType
) {
    public SavedDataType(String pId, Supplier<T> pSupplier, Codec<T> pCodec, DataFixTypes pDataFixType) {
        this(pId, p_393677_ -> pSupplier.get(), p_393917_ -> pCodec, pDataFixType);
    }

    @Override
    public boolean equals(Object pOther) {
        return pOther instanceof SavedDataType<?> saveddatatype && this.id.equals(saveddatatype.id);
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public String toString() {
        return "SavedDataType[" + this.id + "]";
    }
}