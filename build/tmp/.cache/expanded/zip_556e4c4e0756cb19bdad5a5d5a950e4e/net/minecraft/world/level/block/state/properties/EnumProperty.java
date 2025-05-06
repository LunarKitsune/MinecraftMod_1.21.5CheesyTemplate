package net.minecraft.world.level.block.state.properties;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.minecraft.util.StringRepresentable;

public final class EnumProperty<T extends Enum<T> & StringRepresentable> extends Property<T> {
    private final List<T> values;
    private final Map<String, T> names;
    private final int[] ordinalToIndex;

    private EnumProperty(String pName, Class<T> pClazz, List<T> pValues) {
        super(pName, pClazz);
        if (pValues.isEmpty()) {
            throw new IllegalArgumentException("Trying to make empty EnumProperty '" + pName + "'");
        } else {
            this.values = List.copyOf(pValues);
            T[] at = pClazz.getEnumConstants();
            this.ordinalToIndex = new int[at.length];

            for (T t : at) {
                this.ordinalToIndex[t.ordinal()] = pValues.indexOf(t);
            }

            Builder<String, T> builder = ImmutableMap.builder();

            for (T t1 : pValues) {
                String s = t1.getSerializedName();
                builder.put(s, t1);
            }

            this.names = builder.buildOrThrow();
        }
    }

    @Override
    public List<T> getPossibleValues() {
        return this.values;
    }

    @Override
    public Optional<T> getValue(String pValue) {
        return Optional.ofNullable(this.names.get(pValue));
    }

    public String getName(T pValue) {
        return pValue.getSerializedName();
    }

    public int getInternalIndex(T p_363721_) {
        return this.ordinalToIndex[p_363721_.ordinal()];
    }

    @Override
    public boolean equals(Object p_61606_) {
        if (this == p_61606_) {
            return true;
        } else {
            return p_61606_ instanceof EnumProperty<?> enumproperty && super.equals(p_61606_) ? this.values.equals(enumproperty.values) : false;
        }
    }

    @Override
    public int generateHashCode() {
        int i = super.generateHashCode();
        return 31 * i + this.values.hashCode();
    }

    public static <T extends Enum<T> & StringRepresentable> EnumProperty<T> create(String pName, Class<T> pClazz) {
        return create(pName, pClazz, p_187560_ -> true);
    }

    public static <T extends Enum<T> & StringRepresentable> EnumProperty<T> create(String pName, Class<T> pClazz, Predicate<T> pFilter) {
        return create(pName, pClazz, Arrays.<T>stream(pClazz.getEnumConstants()).filter(pFilter).collect(Collectors.toList()));
    }

    @SafeVarargs
    public static <T extends Enum<T> & StringRepresentable> EnumProperty<T> create(String pName, Class<T> pClazz, T... pValues) {
        return create(pName, pClazz, List.of(pValues));
    }

    public static <T extends Enum<T> & StringRepresentable> EnumProperty<T> create(String pName, Class<T> pClazz, List<T> pValues) {
        return new EnumProperty<>(pName, pClazz, pValues);
    }
}