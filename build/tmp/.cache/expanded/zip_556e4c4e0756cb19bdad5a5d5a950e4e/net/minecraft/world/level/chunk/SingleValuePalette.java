package net.minecraft.world.level.chunk;

import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.IdMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.VarInt;
import org.apache.commons.lang3.Validate;

public class SingleValuePalette<T> implements Palette<T> {
    private final IdMap<T> registry;
    @Nullable
    private T value;
    private final PaletteResize<T> resizeHandler;

    public SingleValuePalette(IdMap<T> pRegistry, PaletteResize<T> pResizeHandler, List<T> pValue) {
        this.registry = pRegistry;
        this.resizeHandler = pResizeHandler;
        if (pValue.size() > 0) {
            Validate.isTrue(pValue.size() <= 1, "Can't initialize SingleValuePalette with %d values.", (long)pValue.size());
            this.value = pValue.get(0);
        }
    }

    public static <A> Palette<A> create(int pBits, IdMap<A> pRegistry, PaletteResize<A> pResizeHandler, List<A> pValue) {
        return new SingleValuePalette<>(pRegistry, pResizeHandler, pValue);
    }

    @Override
    public int idFor(T p_188219_) {
        if (this.value != null && this.value != p_188219_) {
            return this.resizeHandler.onResize(1, p_188219_);
        } else {
            this.value = p_188219_;
            return 0;
        }
    }

    @Override
    public boolean maybeHas(Predicate<T> p_188221_) {
        if (this.value == null) {
            throw new IllegalStateException("Use of an uninitialized palette");
        } else {
            return p_188221_.test(this.value);
        }
    }

    @Override
    public T valueFor(int p_188212_) {
        if (this.value != null && p_188212_ == 0) {
            return this.value;
        } else {
            throw new IllegalStateException("Missing Palette entry for id " + p_188212_ + ".");
        }
    }

    @Override
    public void read(FriendlyByteBuf p_188223_) {
        this.value = this.registry.byIdOrThrow(p_188223_.readVarInt());
    }

    @Override
    public void write(FriendlyByteBuf p_188226_) {
        if (this.value == null) {
            throw new IllegalStateException("Use of an uninitialized palette");
        } else {
            p_188226_.writeVarInt(this.registry.getId(this.value));
        }
    }

    @Override
    public int getSerializedSize() {
        if (this.value == null) {
            throw new IllegalStateException("Use of an uninitialized palette");
        } else {
            return VarInt.getByteSize(this.registry.getId(this.value));
        }
    }

    @Override
    public int getSize() {
        return 1;
    }

    @Override
    public Palette<T> copy(PaletteResize<T> p_367270_) {
        if (this.value == null) {
            throw new IllegalStateException("Use of an uninitialized palette");
        } else {
            return this;
        }
    }
}