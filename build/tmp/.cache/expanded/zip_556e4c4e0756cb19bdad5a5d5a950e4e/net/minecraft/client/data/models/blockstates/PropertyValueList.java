package net.minecraft.client.data.models.blockstates;

import com.google.common.collect.ImmutableList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.Util;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record PropertyValueList(List<Property.Value<?>> values) {
    public static final PropertyValueList EMPTY = new PropertyValueList(List.of());
    private static final Comparator<Property.Value<?>> COMPARE_BY_NAME = Comparator.comparing(p_394475_ -> p_394475_.property().getName());

    public PropertyValueList extend(Property.Value<?> pValue) {
        return new PropertyValueList(Util.copyAndAdd(this.values, pValue));
    }

    public PropertyValueList extend(PropertyValueList pValues) {
        return new PropertyValueList(ImmutableList.<Property.Value<?>>builder().addAll(this.values).addAll(pValues.values).build());
    }

    public static PropertyValueList of(Property.Value<?>... pValues) {
        return new PropertyValueList(List.of(pValues));
    }

    public String getKey() {
        return this.values.stream().sorted(COMPARE_BY_NAME).map(Property.Value::toString).collect(Collectors.joining(","));
    }

    @Override
    public String toString() {
        return this.getKey();
    }
}