package net.minecraft.util.random;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.RandomSource;

public final class WeightedList<E> implements net.minecraftforge.common.extensions.IForgeWeightedList<E> {
    private static final int FLAT_THRESHOLD = 64;
    private final int totalWeight;
    private final List<Weighted<E>> items;
    @Nullable
    private final WeightedList.Selector<E> selector;

    WeightedList(List<? extends Weighted<E>> pItems) {
        this.items = List.copyOf(pItems);
        this.totalWeight = WeightedRandom.getTotalWeight(pItems, Weighted::weight);
        if (this.totalWeight == 0) {
            this.selector = null;
        } else if (this.totalWeight < 64) {
            this.selector = new WeightedList.Flat<>(this.items, this.totalWeight);
        } else {
            this.selector = new WeightedList.Compact<>(this.items);
        }
    }

    public static <E> WeightedList<E> of() {
        return new WeightedList<>(List.of());
    }

    public static <E> WeightedList<E> of(E pElement) {
        return new WeightedList<>(List.of(new Weighted<>(pElement, 1)));
    }

    @SafeVarargs
    public static <E> WeightedList<E> of(Weighted<E>... pItems) {
        return new WeightedList<>(List.of(pItems));
    }

    public static <E> WeightedList<E> of(List<Weighted<E>> pItems) {
        return new WeightedList<>(pItems);
    }

    public static <E> WeightedList.Builder<E> builder() {
        return new WeightedList.Builder<>();
    }

    public boolean isEmpty() {
        return this.items.isEmpty();
    }

    public <T> WeightedList<T> map(Function<E, T> pMapper) {
        return new WeightedList(Lists.transform(this.items, p_392113_ -> p_392113_.map((Function<E, E>)pMapper)));
    }

    public Optional<E> getRandom(RandomSource pRandom) {
        if (this.selector == null) {
            return Optional.empty();
        } else {
            int i = pRandom.nextInt(this.totalWeight);
            return Optional.of(this.selector.get(i));
        }
    }

    public E getRandomOrThrow(RandomSource pRandom) {
        if (this.selector == null) {
            throw new IllegalStateException("Weighted list has no elements");
        } else {
            int i = pRandom.nextInt(this.totalWeight);
            return this.selector.get(i);
        }
    }

    public List<Weighted<E>> unwrap() {
        return this.items;
    }

    public static <E> Codec<WeightedList<E>> codec(Codec<E> pElementCodec) {
        return Weighted.codec(pElementCodec).listOf().xmap(WeightedList::of, WeightedList::unwrap);
    }

    public static <E> Codec<WeightedList<E>> codec(MapCodec<E> pElementCodec) {
        return Weighted.codec(pElementCodec).listOf().xmap(WeightedList::of, WeightedList::unwrap);
    }

    public static <E> Codec<WeightedList<E>> nonEmptyCodec(Codec<E> pElementCodec) {
        return ExtraCodecs.nonEmptyList(Weighted.codec(pElementCodec).listOf()).xmap(WeightedList::of, WeightedList::unwrap);
    }

    public static <E> Codec<WeightedList<E>> nonEmptyCodec(MapCodec<E> pElementCodec) {
        return ExtraCodecs.nonEmptyList(Weighted.codec(pElementCodec).listOf()).xmap(WeightedList::of, WeightedList::unwrap);
    }

    public boolean contains(E pElement) {
        for (Weighted<E> weighted : this.items) {
            if (weighted.value().equals(pElement)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean equals(@Nullable Object pOther) {
        if (this == pOther) {
            return true;
        } else {
            return !(pOther instanceof WeightedList<?> weightedlist)
                ? false
                : this.totalWeight == weightedlist.totalWeight && Objects.equals(this.items, weightedlist.items);
        }
    }

    @Override
    public int hashCode() {
        int i = this.totalWeight;
        return 31 * i + this.items.hashCode();
    }

    public static class Builder<E> implements net.minecraftforge.common.extensions.IForgeWeightedList.Builder<E> {
        private final ImmutableList.Builder<Weighted<E>> result = ImmutableList.builder();

        public WeightedList.Builder<E> add(E pElement) {
            return this.add(pElement, 1);
        }

        public WeightedList.Builder<E> add(E pElement, int pWeight) {
            this.result.add(new Weighted<>(pElement, pWeight));
            return this;
        }

        public WeightedList<E> build() {
            return new WeightedList<>(this.result.build());
        }
    }

    static class Compact<E> implements WeightedList.Selector<E> {
        private final Weighted<?>[] entries;

        Compact(List<Weighted<E>> pEntries) {
            this.entries = pEntries.toArray(Weighted[]::new);
        }

        @Override
        public E get(int p_395412_) {
            for (Weighted<?> weighted : this.entries) {
                p_395412_ -= weighted.weight();
                if (p_395412_ < 0) {
                    return (E)weighted.value();
                }
            }

            throw new IllegalStateException(p_395412_ + " exceeded total weight");
        }
    }

    static class Flat<E> implements WeightedList.Selector<E> {
        private final Object[] entries;

        Flat(List<Weighted<E>> pEntries, int pSize) {
            this.entries = new Object[pSize];
            int i = 0;

            for (Weighted<E> weighted : pEntries) {
                int j = weighted.weight();
                Arrays.fill(this.entries, i, i + j, weighted.value());
                i += j;
            }
        }

        @Override
        public E get(int p_395440_) {
            return (E)this.entries[p_395440_];
        }
    }

    interface Selector<E> {
        E get(int pIndex);
    }
}
