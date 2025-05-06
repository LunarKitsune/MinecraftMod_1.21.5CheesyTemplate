package net.minecraft.world.level.levelgen.feature.stateproviders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Collection;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;

public class RandomizedIntStateProvider extends BlockStateProvider {
    public static final MapCodec<RandomizedIntStateProvider> CODEC = RecordCodecBuilder.mapCodec(
        p_161576_ -> p_161576_.group(
                BlockStateProvider.CODEC.fieldOf("source").forGetter(p_161592_ -> p_161592_.source),
                Codec.STRING.fieldOf("property").forGetter(p_161590_ -> p_161590_.propertyName),
                IntProvider.CODEC.fieldOf("values").forGetter(p_161578_ -> p_161578_.values)
            )
            .apply(p_161576_, RandomizedIntStateProvider::new)
    );
    private final BlockStateProvider source;
    private final String propertyName;
    @Nullable
    private IntegerProperty property;
    private final IntProvider values;

    public RandomizedIntStateProvider(BlockStateProvider pSource, IntegerProperty pProperty, IntProvider pValues) {
        this.source = pSource;
        this.property = pProperty;
        this.propertyName = pProperty.getName();
        this.values = pValues;
        Collection<Integer> collection = pProperty.getPossibleValues();

        for (int i = pValues.getMinValue(); i <= pValues.getMaxValue(); i++) {
            if (!collection.contains(i)) {
                throw new IllegalArgumentException("Property value out of range: " + pProperty.getName() + ": " + i);
            }
        }
    }

    public RandomizedIntStateProvider(BlockStateProvider pSource, String pPropertyName, IntProvider pValues) {
        this.source = pSource;
        this.propertyName = pPropertyName;
        this.values = pValues;
    }

    @Override
    protected BlockStateProviderType<?> type() {
        return BlockStateProviderType.RANDOMIZED_INT_STATE_PROVIDER;
    }

    @Override
    public BlockState getState(RandomSource p_225919_, BlockPos p_225920_) {
        BlockState blockstate = this.source.getState(p_225919_, p_225920_);
        if (this.property == null || !blockstate.hasProperty(this.property)) {
            IntegerProperty integerproperty = findProperty(blockstate, this.propertyName);
            if (integerproperty == null) {
                return blockstate;
            }

            this.property = integerproperty;
        }

        return blockstate.setValue(this.property, this.values.sample(p_225919_));
    }

    @Nullable
    private static IntegerProperty findProperty(BlockState pState, String pPropertyName) {
        Collection<Property<?>> collection = pState.getProperties();
        Optional<IntegerProperty> optional = collection.stream()
            .filter(p_161583_ -> p_161583_.getName().equals(pPropertyName))
            .filter(p_161588_ -> p_161588_ instanceof IntegerProperty)
            .map(p_161574_ -> (IntegerProperty)p_161574_)
            .findAny();
        return optional.orElse(null);
    }
}