package net.minecraft.client.data.models.blockstates;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.client.renderer.block.model.multipart.Condition;
import net.minecraft.client.renderer.block.model.multipart.KeyValueCondition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ConditionBuilder {
    private final Builder<String, KeyValueCondition.Terms> terms = ImmutableMap.builder();

    private <T extends Comparable<T>> void putValue(Property<T> pProperty, KeyValueCondition.Terms pTerms) {
        this.terms.put(pProperty.getName(), pTerms);
    }

    public final <T extends Comparable<T>> ConditionBuilder term(Property<T> pProperty, T pValue) {
        this.putValue(pProperty, new KeyValueCondition.Terms(List.of(new KeyValueCondition.Term(pProperty.getName(pValue), false))));
        return this;
    }

    @SafeVarargs
    public final <T extends Comparable<T>> ConditionBuilder term(Property<T> pProperty, T pValue, T... pOtherValues) {
        List<KeyValueCondition.Term> list = Stream.concat(Stream.of(pValue), Stream.of(pOtherValues))
            .map(pProperty::getName)
            .sorted()
            .distinct()
            .map(p_393302_ -> new KeyValueCondition.Term(p_393302_, false))
            .toList();
        this.putValue(pProperty, new KeyValueCondition.Terms(list));
        return this;
    }

    public final <T extends Comparable<T>> ConditionBuilder negatedTerm(Property<T> pProperty, T pValue) {
        this.putValue(pProperty, new KeyValueCondition.Terms(List.of(new KeyValueCondition.Term(pProperty.getName(pValue), true))));
        return this;
    }

    public Condition build() {
        return new KeyValueCondition(this.terms.buildOrThrow());
    }
}