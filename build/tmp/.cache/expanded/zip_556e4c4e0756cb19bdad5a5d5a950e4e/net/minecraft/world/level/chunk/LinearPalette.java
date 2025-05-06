package net.minecraft.world.level.chunk;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.IdMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.VarInt;
import org.apache.commons.lang3.Validate;

public class LinearPalette<T> implements Palette<T> {
    private final IdMap<T> registry;
    private final T[] values;
    private final PaletteResize<T> resizeHandler;
    private final int bits;
    private int size;

    private LinearPalette(IdMap<T> pRegistry, int pBits, PaletteResize<T> pResizeHandler, List<T> pValues) {
        this.registry = pRegistry;
        this.values = (T[])(new Object[1 << pBits]);
        this.bits = pBits;
        this.resizeHandler = pResizeHandler;
        Validate.isTrue(
            pValues.size() <= this.values.length, "Can't initialize LinearPalette of size %d with %d entries", this.values.length, pValues.size()
        );

        for (int i = 0; i < pValues.size(); i++) {
            this.values[i] = pValues.get(i);
        }

        this.size = pValues.size();
    }

    private LinearPalette(IdMap<T> pRegistry, T[] pValues, PaletteResize<T> pResizeHandler, int pBits, int pSize) {
        this.registry = pRegistry;
        this.values = pValues;
        this.resizeHandler = pResizeHandler;
        this.bits = pBits;
        this.size = pSize;
    }

    public static <A> Palette<A> create(int pBits, IdMap<A> pRegistry, PaletteResize<A> pResizeHandler, List<A> pValues) {
        return new LinearPalette<>(pRegistry, pBits, pResizeHandler, pValues);
    }

    @Override
    public int idFor(T p_63040_) {
        for (int i = 0; i < this.size; i++) {
            if (this.values[i] == p_63040_) {
                return i;
            }
        }

        int j = this.size;
        if (j < this.values.length) {
            this.values[j] = p_63040_;
            this.size++;
            return j;
        } else {
            return this.resizeHandler.onResize(this.bits + 1, p_63040_);
        }
    }

    @Override
    public boolean maybeHas(Predicate<T> p_63042_) {
        for (int i = 0; i < this.size; i++) {
            if (p_63042_.test(this.values[i])) {
                return true;
            }
        }

        return false;
    }

    @Override
    public T valueFor(int p_63038_) {
        if (p_63038_ >= 0 && p_63038_ < this.size) {
            return this.values[p_63038_];
        } else {
            throw new MissingPaletteEntryException(p_63038_);
        }
    }

    @Override
    public void read(FriendlyByteBuf p_63046_) {
        this.size = p_63046_.readVarInt();

        for (int i = 0; i < this.size; i++) {
            this.values[i] = this.registry.byIdOrThrow(p_63046_.readVarInt());
        }
    }

    @Override
    public void write(FriendlyByteBuf p_63049_) {
        p_63049_.writeVarInt(this.size);

        for (int i = 0; i < this.size; i++) {
            p_63049_.writeVarInt(this.registry.getId(this.values[i]));
        }
    }

    @Override
    public int getSerializedSize() {
        int i = VarInt.getByteSize(this.getSize());

        for (int j = 0; j < this.getSize(); j++) {
            i += VarInt.getByteSize(this.registry.getId(this.values[j]));
        }

        return i;
    }

    @Override
    public int getSize() {
        return this.size;
    }

    @Override
    public Palette<T> copy(PaletteResize<T> p_361465_) {
        return new LinearPalette<>(this.registry, (T[])((Object[])this.values.clone()), p_361465_, this.bits, this.size);
    }
}