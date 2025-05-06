package net.minecraft.world.level.block.state.properties;

import it.unimi.dsi.fastutil.ints.IntImmutableList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

public final class IntegerProperty extends Property<Integer> {
    private final IntImmutableList values;
    private final int min;
    private final int max;

    private IntegerProperty(String pName, int pMin, int pMax) {
        super(pName, Integer.class);
        if (pMin < 0) {
            throw new IllegalArgumentException("Min value of " + pName + " must be 0 or greater");
        } else if (pMax <= pMin) {
            throw new IllegalArgumentException("Max value of " + pName + " must be greater than min (" + pMin + ")");
        } else {
            this.min = pMin;
            this.max = pMax;
            this.values = IntImmutableList.toList(IntStream.range(pMin, pMax + 1));
        }
    }

    @Override
    public List<Integer> getPossibleValues() {
        return this.values;
    }

    @Override
    public boolean equals(Object p_61639_) {
        if (this == p_61639_) {
            return true;
        } else {
            return p_61639_ instanceof IntegerProperty integerproperty && super.equals(p_61639_) ? this.values.equals(integerproperty.values) : false;
        }
    }

    @Override
    public int generateHashCode() {
        return 31 * super.generateHashCode() + this.values.hashCode();
    }

    public static IntegerProperty create(String pName, int pMin, int pMax) {
        return new IntegerProperty(pName, pMin, pMax);
    }

    @Override
    public Optional<Integer> getValue(String pValue) {
        try {
            int i = Integer.parseInt(pValue);
            return i >= this.min && i <= this.max ? Optional.of(i) : Optional.empty();
        } catch (NumberFormatException numberformatexception) {
            return Optional.empty();
        }
    }

    public String getName(Integer pValue) {
        return pValue.toString();
    }

    public int getInternalIndex(Integer p_369529_) {
        return p_369529_ <= this.max ? p_369529_ - this.min : -1;
    }
}