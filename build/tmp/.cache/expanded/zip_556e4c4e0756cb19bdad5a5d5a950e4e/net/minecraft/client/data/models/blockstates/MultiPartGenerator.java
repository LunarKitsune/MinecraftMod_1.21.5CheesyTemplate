package net.minecraft.client.data.models.blockstates;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.renderer.block.model.BlockModelDefinition;
import net.minecraft.client.renderer.block.model.multipart.Condition;
import net.minecraft.client.renderer.block.model.multipart.Selector;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MultiPartGenerator implements BlockModelDefinitionGenerator {
    private final Block block;
    private final List<MultiPartGenerator.Entry> parts = new ArrayList<>();

    private MultiPartGenerator(Block pBlock) {
        this.block = pBlock;
    }

    @Override
    public Block block() {
        return this.block;
    }

    public static MultiPartGenerator multiPart(Block pBlock) {
        return new MultiPartGenerator(pBlock);
    }

    public MultiPartGenerator with(MultiVariant pVariants) {
        this.parts.add(new MultiPartGenerator.Entry(Optional.empty(), pVariants));
        return this;
    }

    private void validateCondition(Condition pCondition) {
        pCondition.instantiate(this.block.getStateDefinition());
    }

    public MultiPartGenerator with(Condition pCondition, MultiVariant pVariants) {
        this.validateCondition(pCondition);
        this.parts.add(new MultiPartGenerator.Entry(Optional.of(pCondition), pVariants));
        return this;
    }

    public MultiPartGenerator with(ConditionBuilder pCondition, MultiVariant pVariants) {
        return this.with(pCondition.build(), pVariants);
    }

    @Override
    public BlockModelDefinition create() {
        return new BlockModelDefinition(
            Optional.empty(),
            Optional.of(new BlockModelDefinition.MultiPartDefinition(this.parts.stream().map(MultiPartGenerator.Entry::toUnbaked).toList()))
        );
    }

    @OnlyIn(Dist.CLIENT)
    record Entry(Optional<Condition> condition, MultiVariant variants) {
        public Selector toUnbaked() {
            return new Selector(this.condition, this.variants.toUnbaked());
        }
    }
}