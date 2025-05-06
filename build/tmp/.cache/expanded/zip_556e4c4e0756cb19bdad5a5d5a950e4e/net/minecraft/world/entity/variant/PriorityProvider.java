package net.minecraft.world.entity.variant;

import com.mojang.datafixers.DataFixUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import net.minecraft.Util;
import net.minecraft.util.RandomSource;

public interface PriorityProvider<Context, Condition extends PriorityProvider.SelectorCondition<Context>> {
    List<PriorityProvider.Selector<Context, Condition>> selectors();

    static <C, T> Stream<T> select(Stream<T> pElements, Function<T, PriorityProvider<C, ?>> pEntryGetter, C pContext) {
        List<PriorityProvider.UnpackedEntry<C, T>> list = new ArrayList<>();
        pElements.forEach(
            p_393783_ -> {
                PriorityProvider<C, ?> priorityprovider = pEntryGetter.apply((T)p_393783_);

                for (PriorityProvider.Selector<C, ?> selector : priorityprovider.selectors()) {
                    list.add(
                        new PriorityProvider.UnpackedEntry<>(
                            (T)p_393783_,
                            selector.priority(),
                            DataFixUtils.orElseGet(
                                (Optional<? extends PriorityProvider.SelectorCondition<C>>)selector.condition(), PriorityProvider.SelectorCondition::alwaysTrue
                            )
                        )
                    );
                }
            }
        );
        list.sort(PriorityProvider.UnpackedEntry.HIGHEST_PRIORITY_FIRST);
        Iterator<PriorityProvider.UnpackedEntry<C, T>> iterator = list.iterator();
        int i = Integer.MIN_VALUE;

        while (iterator.hasNext()) {
            PriorityProvider.UnpackedEntry<C, T> unpackedentry = iterator.next();
            if (unpackedentry.priority < i) {
                iterator.remove();
            } else if (unpackedentry.condition.test(pContext)) {
                i = unpackedentry.priority;
            } else {
                iterator.remove();
            }
        }

        return list.stream().map(PriorityProvider.UnpackedEntry::entry);
    }

    static <C, T> Optional<T> pick(Stream<T> pElements, Function<T, PriorityProvider<C, ?>> pEntryGetter, RandomSource pRandom, C pContext) {
        List<T> list = select(pElements, pEntryGetter, pContext).toList();
        return Util.getRandomSafe(list, pRandom);
    }

    static <Context, Condition extends PriorityProvider.SelectorCondition<Context>> List<PriorityProvider.Selector<Context, Condition>> single(
        Condition pCondition, int pPriority
    ) {
        return List.of(new PriorityProvider.Selector<>(pCondition, pPriority));
    }

    static <Context, Condition extends PriorityProvider.SelectorCondition<Context>> List<PriorityProvider.Selector<Context, Condition>> alwaysTrue(int pPriority) {
        return List.of(new PriorityProvider.Selector<>(Optional.empty(), pPriority));
    }

    public record Selector<Context, Condition extends PriorityProvider.SelectorCondition<Context>>(Optional<Condition> condition, int priority) {
        public Selector(Condition pCondition, int pPriority) {
            this(Optional.of(pCondition), pPriority);
        }

        public Selector(int pPriority) {
            this(Optional.empty(), pPriority);
        }

        public static <Context, Condition extends PriorityProvider.SelectorCondition<Context>> Codec<PriorityProvider.Selector<Context, Condition>> codec(
            Codec<Condition> pConditionCodec
        ) {
            return RecordCodecBuilder.create(
                p_394411_ -> p_394411_.group(
                        pConditionCodec.optionalFieldOf("condition").forGetter(PriorityProvider.Selector::condition),
                        Codec.INT.fieldOf("priority").forGetter(PriorityProvider.Selector::priority)
                    )
                    .apply(p_394411_, PriorityProvider.Selector::new)
            );
        }
    }

    @FunctionalInterface
    public interface SelectorCondition<C> extends Predicate<C> {
        static <C> PriorityProvider.SelectorCondition<C> alwaysTrue() {
            return p_397254_ -> true;
        }
    }

    public record UnpackedEntry<C, T>(T entry, int priority, PriorityProvider.SelectorCondition<C> condition) {
        public static final Comparator<PriorityProvider.UnpackedEntry<?, ?>> HIGHEST_PRIORITY_FIRST = Comparator.<PriorityProvider.UnpackedEntry<?, ?>>comparingInt(PriorityProvider.UnpackedEntry::priority)
            .reversed();
    }
}