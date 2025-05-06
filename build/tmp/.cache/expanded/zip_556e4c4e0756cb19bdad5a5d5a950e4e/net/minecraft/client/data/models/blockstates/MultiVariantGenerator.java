package net.minecraft.client.data.models.blockstates;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.renderer.block.model.BlockModelDefinition;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.block.model.VariantMutator;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MultiVariantGenerator implements BlockModelDefinitionGenerator {
    private final Block block;
    private final List<MultiVariantGenerator.Entry> entries;
    private final Set<Property<?>> seenProperties;

    MultiVariantGenerator(Block pBlock, List<MultiVariantGenerator.Entry> pEntries, Set<Property<?>> pSeenProperties) {
        this.block = pBlock;
        this.entries = pEntries;
        this.seenProperties = pSeenProperties;
    }

    static Set<Property<?>> validateAndExpandProperties(Set<Property<?>> pSeenProperties, Block pBlock, PropertyDispatch<?> pPropertyDispatch) {
        List<Property<?>> list = pPropertyDispatch.getDefinedProperties();
        list.forEach(p_389280_ -> {
            if (pBlock.getStateDefinition().getProperty(p_389280_.getName()) != p_389280_) {
                throw new IllegalStateException("Property " + p_389280_ + " is not defined for block " + pBlock);
            } else if (pSeenProperties.contains(p_389280_)) {
                throw new IllegalStateException("Values of property " + p_389280_ + " already defined for block " + pBlock);
            }
        });
        Set<Property<?>> set = new HashSet<>(pSeenProperties);
        set.addAll(list);
        return set;
    }

    public MultiVariantGenerator with(PropertyDispatch<VariantMutator> pPropertyDispatch) {
        Set<Property<?>> set = validateAndExpandProperties(this.seenProperties, this.block, pPropertyDispatch);
        List<MultiVariantGenerator.Entry> list = this.entries.stream().flatMap(p_389282_ -> p_389282_.apply(pPropertyDispatch)).toList();
        return new MultiVariantGenerator(this.block, list, set);
    }

    public MultiVariantGenerator with(VariantMutator pMutator) {
        List<MultiVariantGenerator.Entry> list = this.entries.stream().flatMap(p_389284_ -> p_389284_.apply(pMutator)).toList();
        return new MultiVariantGenerator(this.block, list, this.seenProperties);
    }

    @Override
    public BlockModelDefinition create() {
        Map<String, BlockStateModel.Unbaked> map = new HashMap<>();

        for (MultiVariantGenerator.Entry multivariantgenerator$entry : this.entries) {
            map.put(multivariantgenerator$entry.properties.getKey(), multivariantgenerator$entry.variant.toUnbaked());
        }

        return new BlockModelDefinition(Optional.of(new BlockModelDefinition.SimpleModelSelectors(map)), Optional.empty());
    }

    @Override
    public Block block() {
        return this.block;
    }

    public static MultiVariantGenerator.Empty dispatch(Block pBlock) {
        return new MultiVariantGenerator.Empty(pBlock);
    }

    public static MultiVariantGenerator dispatch(Block pBlock, MultiVariant pVariants) {
        return new MultiVariantGenerator(pBlock, List.of(new MultiVariantGenerator.Entry(PropertyValueList.EMPTY, pVariants)), Set.of());
    }

    @OnlyIn(Dist.CLIENT)
    public static class Empty {
        private final Block block;

        public Empty(Block pBlock) {
            this.block = pBlock;
        }

        public MultiVariantGenerator with(PropertyDispatch<MultiVariant> pPropertyDispatch) {
            Set<Property<?>> set = MultiVariantGenerator.validateAndExpandProperties(Set.of(), this.block, pPropertyDispatch);
            List<MultiVariantGenerator.Entry> list = pPropertyDispatch.getEntries()
                .entrySet()
                .stream()
                .map(p_396238_ -> new MultiVariantGenerator.Entry(p_396238_.getKey(), p_396238_.getValue()))
                .toList();
            return new MultiVariantGenerator(this.block, list, set);
        }
    }

    @OnlyIn(Dist.CLIENT)
    record Entry(PropertyValueList properties, MultiVariant variant) {
        public Stream<MultiVariantGenerator.Entry> apply(PropertyDispatch<VariantMutator> pPropertyDispatch) {
            return pPropertyDispatch.getEntries().entrySet().stream().map(p_393452_ -> {
                PropertyValueList propertyvaluelist = this.properties.extend(p_393452_.getKey());
                MultiVariant multivariant = this.variant.with(p_393452_.getValue());
                return new MultiVariantGenerator.Entry(propertyvaluelist, multivariant);
            });
        }

        public Stream<MultiVariantGenerator.Entry> apply(VariantMutator pMutator) {
            return Stream.of(new MultiVariantGenerator.Entry(this.properties, this.variant.with(pMutator)));
        }
    }
}