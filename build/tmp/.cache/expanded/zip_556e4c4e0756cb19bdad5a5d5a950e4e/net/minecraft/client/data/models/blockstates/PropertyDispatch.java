package net.minecraft.client.data.models.blockstates;

import com.mojang.datafixers.util.Function3;
import com.mojang.datafixers.util.Function4;
import com.mojang.datafixers.util.Function5;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.renderer.block.model.VariantMutator;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class PropertyDispatch<V> {
    private final Map<PropertyValueList, V> values = new HashMap<>();

    protected void putValue(PropertyValueList pProperties, V pValue) {
        V v = this.values.put(pProperties, pValue);
        if (v != null) {
            throw new IllegalStateException("Value " + pProperties + " is already defined");
        }
    }

    Map<PropertyValueList, V> getEntries() {
        this.verifyComplete();
        return Map.copyOf(this.values);
    }

    private void verifyComplete() {
        List<Property<?>> list = this.getDefinedProperties();
        Stream<PropertyValueList> stream = Stream.of(PropertyValueList.EMPTY);

        for (Property<?> property : list) {
            stream = stream.flatMap(p_396264_ -> property.getAllValues().map(p_396264_::extend));
        }

        List<PropertyValueList> list1 = stream.filter(p_394003_ -> !this.values.containsKey(p_394003_)).toList();
        if (!list1.isEmpty()) {
            throw new IllegalStateException("Missing definition for properties: " + list1);
        }
    }

    abstract List<Property<?>> getDefinedProperties();

    public static <T1 extends Comparable<T1>> PropertyDispatch.C1<MultiVariant, T1> initial(Property<T1> pProperty) {
        return new PropertyDispatch.C1<>(pProperty);
    }

    public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>> PropertyDispatch.C2<MultiVariant, T1, T2> initial(
        Property<T1> pProperty1, Property<T2> pProperty2
    ) {
        return new PropertyDispatch.C2<>(pProperty1, pProperty2);
    }

    public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>> PropertyDispatch.C3<MultiVariant, T1, T2, T3> initial(
        Property<T1> pProperty1, Property<T2> pProperty2, Property<T3> pProperty3
    ) {
        return new PropertyDispatch.C3<>(pProperty1, pProperty2, pProperty3);
    }

    public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>> PropertyDispatch.C4<MultiVariant, T1, T2, T3, T4> initial(
        Property<T1> pProperty1, Property<T2> pProperty2, Property<T3> pProperty3, Property<T4> pProperty4
    ) {
        return new PropertyDispatch.C4<>(pProperty1, pProperty2, pProperty3, pProperty4);
    }

    public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>, T5 extends Comparable<T5>> PropertyDispatch.C5<MultiVariant, T1, T2, T3, T4, T5> initial(
        Property<T1> pProperty1, Property<T2> pProperty2, Property<T3> pProperty3, Property<T4> pProperty4, Property<T5> pProperty5
    ) {
        return new PropertyDispatch.C5<>(pProperty1, pProperty2, pProperty3, pProperty4, pProperty5);
    }

    public static <T1 extends Comparable<T1>> PropertyDispatch.C1<VariantMutator, T1> modify(Property<T1> pProperty) {
        return new PropertyDispatch.C1<>(pProperty);
    }

    public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>> PropertyDispatch.C2<VariantMutator, T1, T2> modify(
        Property<T1> pProperty1, Property<T2> pProperty2
    ) {
        return new PropertyDispatch.C2<>(pProperty1, pProperty2);
    }

    public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>> PropertyDispatch.C3<VariantMutator, T1, T2, T3> modify(
        Property<T1> pProperty1, Property<T2> pProperty2, Property<T3> pProperty3
    ) {
        return new PropertyDispatch.C3<>(pProperty1, pProperty2, pProperty3);
    }

    public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>> PropertyDispatch.C4<VariantMutator, T1, T2, T3, T4> modify(
        Property<T1> pProperty1, Property<T2> pProperty2, Property<T3> pProperty3, Property<T4> pProperty4
    ) {
        return new PropertyDispatch.C4<>(pProperty1, pProperty2, pProperty3, pProperty4);
    }

    public static <T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>, T5 extends Comparable<T5>> PropertyDispatch.C5<VariantMutator, T1, T2, T3, T4, T5> modify(
        Property<T1> pProperty1, Property<T2> pProperty2, Property<T3> pProperty3, Property<T4> pProperty4, Property<T5> pProperty5
    ) {
        return new PropertyDispatch.C5<>(pProperty1, pProperty2, pProperty3, pProperty4, pProperty5);
    }

    @OnlyIn(Dist.CLIENT)
    public static class C1<V, T1 extends Comparable<T1>> extends PropertyDispatch<V> {
        private final Property<T1> property1;

        C1(Property<T1> pProperty1) {
            this.property1 = pProperty1;
        }

        @Override
        public List<Property<?>> getDefinedProperties() {
            return List.of(this.property1);
        }

        public PropertyDispatch.C1<V, T1> select(T1 pProperty, V pValue) {
            PropertyValueList propertyvaluelist = PropertyValueList.of(this.property1.value(pProperty));
            this.putValue(propertyvaluelist, pValue);
            return this;
        }

        public PropertyDispatch<V> generate(Function<T1, V> pGenerator) {
            this.property1.getPossibleValues().forEach(p_389286_ -> this.select((T1)p_389286_, pGenerator.apply((T1)p_389286_)));
            return this;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class C2<V, T1 extends Comparable<T1>, T2 extends Comparable<T2>> extends PropertyDispatch<V> {
        private final Property<T1> property1;
        private final Property<T2> property2;

        C2(Property<T1> pProperty1, Property<T2> pProperty2) {
            this.property1 = pProperty1;
            this.property2 = pProperty2;
        }

        @Override
        public List<Property<?>> getDefinedProperties() {
            return List.of(this.property1, this.property2);
        }

        public PropertyDispatch.C2<V, T1, T2> select(T1 pProperty1, T2 pProperty2, V pValue) {
            PropertyValueList propertyvaluelist = PropertyValueList.of(this.property1.value(pProperty1), this.property2.value(pProperty2));
            this.putValue(propertyvaluelist, pValue);
            return this;
        }

        public PropertyDispatch<V> generate(BiFunction<T1, T2, V> pGenerator) {
            this.property1
                .getPossibleValues()
                .forEach(
                    p_377154_ -> this.property2
                        .getPossibleValues()
                        .forEach(p_389289_ -> this.select((T1)p_377154_, (T2)p_389289_, pGenerator.apply((T1)p_377154_, (T2)p_389289_)))
                );
            return this;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class C3<V, T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>> extends PropertyDispatch<V> {
        private final Property<T1> property1;
        private final Property<T2> property2;
        private final Property<T3> property3;

        C3(Property<T1> pProperty1, Property<T2> pProperty2, Property<T3> pProperty3) {
            this.property1 = pProperty1;
            this.property2 = pProperty2;
            this.property3 = pProperty3;
        }

        @Override
        public List<Property<?>> getDefinedProperties() {
            return List.of(this.property1, this.property2, this.property3);
        }

        public PropertyDispatch.C3<V, T1, T2, T3> select(T1 pProperty1, T2 pProperty2, T3 pProperty3, V pValue) {
            PropertyValueList propertyvaluelist = PropertyValueList.of(
                this.property1.value(pProperty1), this.property2.value(pProperty2), this.property3.value(pProperty3)
            );
            this.putValue(propertyvaluelist, pValue);
            return this;
        }

        public PropertyDispatch<V> generate(Function3<T1, T2, T3, V> pGenerator) {
            this.property1
                .getPossibleValues()
                .forEach(
                    p_377047_ -> this.property2
                        .getPossibleValues()
                        .forEach(
                            p_377231_ -> this.property3
                                .getPossibleValues()
                                .forEach(
                                    p_389293_ -> this.select(
                                        (T1)p_377047_, (T2)p_377231_, (T3)p_389293_, pGenerator.apply((T1)p_377047_, (T2)p_377231_, (T3)p_389293_)
                                    )
                                )
                        )
                );
            return this;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class C4<V, T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>>
        extends PropertyDispatch<V> {
        private final Property<T1> property1;
        private final Property<T2> property2;
        private final Property<T3> property3;
        private final Property<T4> property4;

        C4(Property<T1> pProperty1, Property<T2> pProperty2, Property<T3> pProperty3, Property<T4> pProperty4) {
            this.property1 = pProperty1;
            this.property2 = pProperty2;
            this.property3 = pProperty3;
            this.property4 = pProperty4;
        }

        @Override
        public List<Property<?>> getDefinedProperties() {
            return List.of(this.property1, this.property2, this.property3, this.property4);
        }

        public PropertyDispatch.C4<V, T1, T2, T3, T4> select(T1 pProperty1, T2 pProperty2, T3 pProperty3, T4 pProperty4, V pValue) {
            PropertyValueList propertyvaluelist = PropertyValueList.of(
                this.property1.value(pProperty1), this.property2.value(pProperty2), this.property3.value(pProperty3), this.property4.value(pProperty4)
            );
            this.putValue(propertyvaluelist, pValue);
            return this;
        }

        public PropertyDispatch<V> generate(Function4<T1, T2, T3, T4, V> pGenerator) {
            this.property1
                .getPossibleValues()
                .forEach(
                    p_376254_ -> this.property2
                        .getPossibleValues()
                        .forEach(
                            p_375541_ -> this.property3
                                .getPossibleValues()
                                .forEach(
                                    p_376281_ -> this.property4
                                        .getPossibleValues()
                                        .forEach(
                                            p_389298_ -> this.select(
                                                (T1)p_376254_,
                                                (T2)p_375541_,
                                                (T3)p_376281_,
                                                (T4)p_389298_,
                                                pGenerator.apply((T1)p_376254_, (T2)p_375541_, (T3)p_376281_, (T4)p_389298_)
                                            )
                                        )
                                )
                        )
                );
            return this;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class C5<V, T1 extends Comparable<T1>, T2 extends Comparable<T2>, T3 extends Comparable<T3>, T4 extends Comparable<T4>, T5 extends Comparable<T5>>
        extends PropertyDispatch<V> {
        private final Property<T1> property1;
        private final Property<T2> property2;
        private final Property<T3> property3;
        private final Property<T4> property4;
        private final Property<T5> property5;

        C5(Property<T1> pProperty1, Property<T2> pProperty2, Property<T3> pProperty3, Property<T4> pProperty4, Property<T5> pProperty5) {
            this.property1 = pProperty1;
            this.property2 = pProperty2;
            this.property3 = pProperty3;
            this.property4 = pProperty4;
            this.property5 = pProperty5;
        }

        @Override
        public List<Property<?>> getDefinedProperties() {
            return List.of(this.property1, this.property2, this.property3, this.property4, this.property5);
        }

        public PropertyDispatch.C5<V, T1, T2, T3, T4, T5> select(T1 pProperty1, T2 pProperty2, T3 pProperty3, T4 pProperty4, T5 pProperty5, V pValue) {
            PropertyValueList propertyvaluelist = PropertyValueList.of(
                this.property1.value(pProperty1),
                this.property2.value(pProperty2),
                this.property3.value(pProperty3),
                this.property4.value(pProperty4),
                this.property5.value(pProperty5)
            );
            this.putValue(propertyvaluelist, pValue);
            return this;
        }

        public PropertyDispatch<V> generate(Function5<T1, T2, T3, T4, T5, V> pGenerator) {
            this.property1
                .getPossibleValues()
                .forEach(
                    p_376257_ -> this.property2
                        .getPossibleValues()
                        .forEach(
                            p_378211_ -> this.property3
                                .getPossibleValues()
                                .forEach(
                                    p_376810_ -> this.property4
                                        .getPossibleValues()
                                        .forEach(
                                            p_378107_ -> this.property5
                                                .getPossibleValues()
                                                .forEach(
                                                    p_389304_ -> this.select(
                                                        (T1)p_376257_,
                                                        (T2)p_378211_,
                                                        (T3)p_376810_,
                                                        (T4)p_378107_,
                                                        (T5)p_389304_,
                                                        pGenerator.apply((T1)p_376257_, (T2)p_378211_, (T3)p_376810_, (T4)p_378107_, (T5)p_389304_)
                                                    )
                                                )
                                        )
                                )
                        )
                );
            return this;
        }
    }
}