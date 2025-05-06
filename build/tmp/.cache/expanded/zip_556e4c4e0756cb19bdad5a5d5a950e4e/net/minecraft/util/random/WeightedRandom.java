package net.minecraft.util.random;

import java.util.List;
import java.util.Optional;
import java.util.function.ToIntFunction;
import net.minecraft.Util;
import net.minecraft.util.RandomSource;

public class WeightedRandom {
    private WeightedRandom() {
    }

    public static <T> int getTotalWeight(List<T> pElements, ToIntFunction<T> pWeightGetter) {
        long i = 0L;

        for (T t : pElements) {
            i += pWeightGetter.applyAsInt(t);
        }

        if (i > 2147483647L) {
            throw new IllegalArgumentException("Sum of weights must be <= 2147483647");
        } else {
            return (int)i;
        }
    }

    public static <T> Optional<T> getRandomItem(RandomSource pRandom, List<T> pElements, int pTotalWeight, ToIntFunction<T> pWeightGetter) {
        if (pTotalWeight < 0) {
            throw (IllegalArgumentException)Util.pauseInIde(new IllegalArgumentException("Negative total weight in getRandomItem"));
        } else if (pTotalWeight == 0) {
            return Optional.empty();
        } else {
            int i = pRandom.nextInt(pTotalWeight);
            return getWeightedItem(pElements, i, pWeightGetter);
        }
    }

    public static <T> Optional<T> getWeightedItem(List<T> pElements, int pIndex, ToIntFunction<T> pWeightGetter) {
        for (T t : pElements) {
            pIndex -= pWeightGetter.applyAsInt(t);
            if (pIndex < 0) {
                return Optional.of(t);
            }
        }

        return Optional.empty();
    }

    public static <T> Optional<T> getRandomItem(RandomSource pRandom, List<T> pElements, ToIntFunction<T> pWeightGetter) {
        return getRandomItem(pRandom, pElements, getTotalWeight(pElements, pWeightGetter), pWeightGetter);
    }
}