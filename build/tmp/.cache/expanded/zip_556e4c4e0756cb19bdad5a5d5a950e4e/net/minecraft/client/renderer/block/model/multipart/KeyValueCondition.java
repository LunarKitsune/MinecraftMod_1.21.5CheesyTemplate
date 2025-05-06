package net.minecraft.client.renderer.block.model.multipart;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.Util;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public record KeyValueCondition(Map<String, KeyValueCondition.Terms> tests) implements Condition {
    static final Logger LOGGER = LogUtils.getLogger();
    public static final Codec<KeyValueCondition> CODEC = ExtraCodecs.nonEmptyMap(Codec.unboundedMap(Codec.STRING, KeyValueCondition.Terms.CODEC))
        .xmap(KeyValueCondition::new, KeyValueCondition::tests);

    @Override
    public <O, S extends StateHolder<O, S>> Predicate<S> instantiate(StateDefinition<O, S> p_393385_) {
        List<Predicate<S>> list = new ArrayList<>(this.tests.size());
        this.tests.forEach((p_389489_, p_389490_) -> list.add(instantiate(p_393385_, p_389489_, p_389490_)));
        return Util.allOf(list);
    }

    private static <O, S extends StateHolder<O, S>> Predicate<S> instantiate(StateDefinition<O, S> pStateDefinition, String pProperty, KeyValueCondition.Terms pTerms) {
        Property<?> property = pStateDefinition.getProperty(pProperty);
        if (property == null) {
            throw new IllegalArgumentException(String.format(Locale.ROOT, "Unknown property '%s' on '%s'", pProperty, pStateDefinition.getOwner()));
        } else {
            return pTerms.instantiate(pStateDefinition.getOwner(), property);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public record Term(String value, boolean negated) {
        private static final String NEGATE = "!";

        public Term(String value, boolean negated) {
            if (value.isEmpty()) {
                throw new IllegalArgumentException("Empty term");
            } else {
                this.value = value;
                this.negated = negated;
            }
        }

        public static KeyValueCondition.Term parse(String pText) {
            return pText.startsWith("!") ? new KeyValueCondition.Term(pText.substring(1), true) : new KeyValueCondition.Term(pText, false);
        }

        @Override
        public String toString() {
            return this.negated ? "!" + this.value : this.value;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public record Terms(List<KeyValueCondition.Term> entries) {
        private static final char SEPARATOR = '|';
        private static final Joiner JOINER = Joiner.on('|');
        private static final Splitter SPLITTER = Splitter.on('|');
        private static final Codec<String> LEGACY_REPRESENTATION_CODEC = Codec.either(Codec.INT, Codec.BOOL)
            .flatComapMap(
                p_397440_ -> p_397440_.map(String::valueOf, String::valueOf), p_393483_ -> DataResult.error(() -> "This codec can't be used for encoding")
            );
        public static final Codec<KeyValueCondition.Terms> CODEC = Codec.withAlternative(Codec.STRING, LEGACY_REPRESENTATION_CODEC)
            .comapFlatMap(KeyValueCondition.Terms::parse, KeyValueCondition.Terms::toString);

        public Terms(List<KeyValueCondition.Term> entries) {
            if (entries.isEmpty()) {
                throw new IllegalArgumentException("Empty value for property");
            } else {
                this.entries = entries;
            }
        }

        public static DataResult<KeyValueCondition.Terms> parse(String pText) {
            List<KeyValueCondition.Term> list = SPLITTER.splitToStream(pText).map(KeyValueCondition.Term::parse).toList();
            if (list.isEmpty()) {
                return DataResult.error(() -> "Empty value for property");
            } else {
                for (KeyValueCondition.Term keyvaluecondition$term : list) {
                    if (keyvaluecondition$term.value.isEmpty()) {
                        return DataResult.error(() -> "Empty term in value '" + pText + "'");
                    }
                }

                return DataResult.success(new KeyValueCondition.Terms(list));
            }
        }

        @Override
        public String toString() {
            return JOINER.join(this.entries);
        }

        public <O, S extends StateHolder<O, S>, T extends Comparable<T>> Predicate<S> instantiate(O pOwner, Property<T> pProperty) {
            Predicate<T> predicate = Util.anyOf(Lists.transform(this.entries, p_396639_ -> this.instantiate(pOwner, pProperty, p_396639_)));
            List<T> list = new ArrayList<>(pProperty.getPossibleValues());
            int i = list.size();
            list.removeIf(predicate.negate());
            int j = list.size();
            if (j == 0) {
                KeyValueCondition.LOGGER.warn("Condition {} for property {} on {} is always false", this, pProperty.getName(), pOwner);
                return p_397520_ -> false;
            } else {
                int k = i - j;
                if (k == 0) {
                    KeyValueCondition.LOGGER.warn("Condition {} for property {} on {} is always true", this, pProperty.getName(), pOwner);
                    return p_394455_ -> true;
                } else {
                    boolean flag;
                    List<T> list1;
                    if (j <= k) {
                        flag = false;
                        list1 = list;
                    } else {
                        flag = true;
                        List<T> list2 = new ArrayList<>(pProperty.getPossibleValues());
                        list2.removeIf(predicate);
                        list1 = list2;
                    }

                    if (list1.size() == 1) {
                        T t = (T)list1.getFirst();
                        return p_393769_ -> {
                            T t1 = p_393769_.getValue(pProperty);
                            return t.equals(t1) ^ flag;
                        };
                    } else {
                        return p_392728_ -> {
                            T t1 = p_392728_.getValue(pProperty);
                            return list1.contains(t1) ^ flag;
                        };
                    }
                }
            }
        }

        private <T extends Comparable<T>> T getValueOrThrow(Object pOwner, Property<T> pProperty, String pValue) {
            Optional<T> optional = pProperty.getValue(pValue);
            if (optional.isEmpty()) {
                throw new RuntimeException(
                    String.format(Locale.ROOT, "Unknown value '%s' for property '%s' on '%s' in '%s'", pValue, pProperty, pOwner, this)
                );
            } else {
                return optional.get();
            }
        }

        private <T extends Comparable<T>> Predicate<T> instantiate(Object pOwner, Property<T> pProperty, KeyValueCondition.Term pTerm) {
            T t = this.getValueOrThrow(pOwner, pProperty, pTerm.value);
            return pTerm.negated ? p_396166_ -> !p_396166_.equals(t) : p_392745_ -> p_392745_.equals(t);
        }
    }
}