package net.minecraft.world.level.block.state;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.objects.Reference2ObjectArrayMap;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.world.level.block.state.properties.Property;

public abstract class StateHolder<O, S> {
    public static final String NAME_TAG = "Name";
    public static final String PROPERTIES_TAG = "Properties";
    private static final Function<Entry<Property<?>, Comparable<?>>, String> PROPERTY_ENTRY_TO_STRING_FUNCTION = new Function<Entry<Property<?>, Comparable<?>>, String>() {
        public String apply(@Nullable Entry<Property<?>, Comparable<?>> p_61155_) {
            if (p_61155_ == null) {
                return "<NULL>";
            } else {
                Property<?> property = p_61155_.getKey();
                return property.getName() + "=" + this.getName(property, p_61155_.getValue());
            }
        }

        private <T extends Comparable<T>> String getName(Property<T> p_61152_, Comparable<?> p_61153_) {
            return p_61152_.getName((T)p_61153_);
        }
    };
    protected final O owner;
    private final Reference2ObjectArrayMap<Property<?>, Comparable<?>> values;
    private Map<Property<?>, S[]> neighbours;
    protected final MapCodec<S> propertiesCodec;

    protected StateHolder(O pOwner, Reference2ObjectArrayMap<Property<?>, Comparable<?>> pValues, MapCodec<S> pPropertiesCodec) {
        this.owner = pOwner;
        this.values = pValues;
        this.propertiesCodec = pPropertiesCodec;
    }

    public <T extends Comparable<T>> S cycle(Property<T> pProperty) {
        return this.setValue(pProperty, findNextInCollection(pProperty.getPossibleValues(), this.getValue(pProperty)));
    }

    protected static <T> T findNextInCollection(List<T> pPossibleValues, T pCurrentValue) {
        int i = pPossibleValues.indexOf(pCurrentValue) + 1;
        return i == pPossibleValues.size() ? pPossibleValues.getFirst() : pPossibleValues.get(i);
    }

    @Override
    public String toString() {
        StringBuilder stringbuilder = new StringBuilder();
        stringbuilder.append(this.owner);
        if (!this.getValues().isEmpty()) {
            stringbuilder.append('[');
            stringbuilder.append(this.getValues().entrySet().stream().map(PROPERTY_ENTRY_TO_STRING_FUNCTION).collect(Collectors.joining(",")));
            stringbuilder.append(']');
        }

        return stringbuilder.toString();
    }

    @Override
    public final boolean equals(Object pOther) {
        return super.equals(pOther);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    public Collection<Property<?>> getProperties() {
        return Collections.unmodifiableCollection(this.values.keySet());
    }

    public boolean hasProperty(Property<?> pProperty) {
        return this.values.containsKey(pProperty);
    }

    public <T extends Comparable<T>> T getValue(Property<T> pProperty) {
        Comparable<?> comparable = this.values.get(pProperty);
        if (comparable == null) {
            throw new IllegalArgumentException("Cannot get property " + pProperty + " as it does not exist in " + this.owner);
        } else {
            return pProperty.getValueClass().cast(comparable);
        }
    }

    public <T extends Comparable<T>> Optional<T> getOptionalValue(Property<T> pProperty) {
        return Optional.ofNullable(this.getNullableValue(pProperty));
    }

    public <T extends Comparable<T>> T getValueOrElse(Property<T> pProperty, T pDefaultValue) {
        return Objects.requireNonNullElse(this.getNullableValue(pProperty), pDefaultValue);
    }

    @Nullable
    private <T extends Comparable<T>> T getNullableValue(Property<T> pProperty) {
        Comparable<?> comparable = this.values.get(pProperty);
        return comparable == null ? null : pProperty.getValueClass().cast(comparable);
    }

    public <T extends Comparable<T>, V extends T> S setValue(Property<T> pProperty, V pValue) {
        Comparable<?> comparable = this.values.get(pProperty);
        if (comparable == null) {
            throw new IllegalArgumentException("Cannot set property " + pProperty + " as it does not exist in " + this.owner);
        } else {
            return this.setValueInternal(pProperty, pValue, comparable);
        }
    }

    public <T extends Comparable<T>, V extends T> S trySetValue(Property<T> pProperty, V pValue) {
        Comparable<?> comparable = this.values.get(pProperty);
        return (S)(comparable == null ? this : this.setValueInternal(pProperty, pValue, comparable));
    }

    private <T extends Comparable<T>, V extends T> S setValueInternal(Property<T> pProperty, V pValue, Comparable<?> pComparable) {
        if (pComparable.equals(pValue)) {
            return (S)this;
        } else {
            int i = pProperty.getInternalIndex((T)pValue);
            if (i < 0) {
                throw new IllegalArgumentException(
                    "Cannot set property " + pProperty + " to " + pValue + " on " + this.owner + ", it is not an allowed value"
                );
            } else {
                return (S)this.neighbours.get(pProperty)[i];
            }
        }
    }

    public void populateNeighbours(Map<Map<Property<?>, Comparable<?>>, S> pPossibleStateMap) {
        if (this.neighbours != null) {
            throw new IllegalStateException();
        } else {
            Map<Property<?>, S[]> map = new Reference2ObjectArrayMap<>(this.values.size());

            for (Entry<Property<?>, Comparable<?>> entry : this.values.entrySet()) {
                Property<?> property = entry.getKey();
                map.put(property, (S[])property.getPossibleValues().stream().map(p_360554_ -> pPossibleStateMap.get(this.makeNeighbourValues(property, p_360554_))).toArray());
            }

            this.neighbours = map;
        }
    }

    private Map<Property<?>, Comparable<?>> makeNeighbourValues(Property<?> pProperty, Comparable<?> pValue) {
        Map<Property<?>, Comparable<?>> map = new Reference2ObjectArrayMap<>(this.values);
        map.put(pProperty, pValue);
        return map;
    }

    public Map<Property<?>, Comparable<?>> getValues() {
        return this.values;
    }

    protected static <O, S extends StateHolder<O, S>> Codec<S> codec(Codec<O> pPropertyMap, Function<O, S> pHolderFunction) {
        return pPropertyMap.dispatch(
            "Name",
            p_61121_ -> p_61121_.owner,
            p_327407_ -> {
                S s = pHolderFunction.apply((O)p_327407_);
                return s.getValues().isEmpty()
                    ? MapCodec.unit(s)
                    : s.propertiesCodec.codec().lenientOptionalFieldOf("Properties").xmap(p_187544_ -> p_187544_.orElse(s), Optional::of);
            }
        );
    }
}