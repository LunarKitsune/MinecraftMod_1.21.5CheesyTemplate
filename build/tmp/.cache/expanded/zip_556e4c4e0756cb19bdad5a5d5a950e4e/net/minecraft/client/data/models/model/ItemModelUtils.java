package net.minecraft.client.data.models.model;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import net.minecraft.client.color.item.Constant;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.renderer.item.BlockModelWrapper;
import net.minecraft.client.renderer.item.CompositeModel;
import net.minecraft.client.renderer.item.ConditionalItemModel;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.RangeSelectItemModel;
import net.minecraft.client.renderer.item.SelectItemModel;
import net.minecraft.client.renderer.item.SpecialModelWrapper;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.client.renderer.item.properties.conditional.HasComponent;
import net.minecraft.client.renderer.item.properties.conditional.IsUsingItem;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.client.renderer.item.properties.select.ContextDimension;
import net.minecraft.client.renderer.item.properties.select.ItemBlockState;
import net.minecraft.client.renderer.item.properties.select.LocalTime;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ItemModelUtils {
    public static ItemModel.Unbaked plainModel(ResourceLocation pModel) {
        return new BlockModelWrapper.Unbaked(pModel, List.of());
    }

    public static ItemModel.Unbaked tintedModel(ResourceLocation pModel, ItemTintSource... pTintSources) {
        return new BlockModelWrapper.Unbaked(pModel, List.of(pTintSources));
    }

    public static ItemTintSource constantTint(int pValue) {
        return new Constant(pValue);
    }

    public static ItemModel.Unbaked composite(ItemModel.Unbaked... pModels) {
        return new CompositeModel.Unbaked(List.of(pModels));
    }

    public static ItemModel.Unbaked specialModel(ResourceLocation pBase, SpecialModelRenderer.Unbaked pSpecialModel) {
        return new SpecialModelWrapper.Unbaked(pBase, pSpecialModel);
    }

    public static RangeSelectItemModel.Entry override(ItemModel.Unbaked pThreshold, float pModel) {
        return new RangeSelectItemModel.Entry(pModel, pThreshold);
    }

    public static ItemModel.Unbaked rangeSelect(RangeSelectItemModelProperty pProperty, ItemModel.Unbaked pFallback, RangeSelectItemModel.Entry... pEntries) {
        return new RangeSelectItemModel.Unbaked(pProperty, 1.0F, List.of(pEntries), Optional.of(pFallback));
    }

    public static ItemModel.Unbaked rangeSelect(
        RangeSelectItemModelProperty pProperty, float pScale, ItemModel.Unbaked pFallback, RangeSelectItemModel.Entry... pEntries
    ) {
        return new RangeSelectItemModel.Unbaked(pProperty, pScale, List.of(pEntries), Optional.of(pFallback));
    }

    public static ItemModel.Unbaked rangeSelect(RangeSelectItemModelProperty pProperty, ItemModel.Unbaked pFallback, List<RangeSelectItemModel.Entry> pEntries) {
        return new RangeSelectItemModel.Unbaked(pProperty, 1.0F, pEntries, Optional.of(pFallback));
    }

    public static ItemModel.Unbaked rangeSelect(RangeSelectItemModelProperty pProperty, List<RangeSelectItemModel.Entry> pEntries) {
        return new RangeSelectItemModel.Unbaked(pProperty, 1.0F, pEntries, Optional.empty());
    }

    public static ItemModel.Unbaked rangeSelect(RangeSelectItemModelProperty pProperty, float pScale, List<RangeSelectItemModel.Entry> pEntries) {
        return new RangeSelectItemModel.Unbaked(pProperty, pScale, pEntries, Optional.empty());
    }

    public static ItemModel.Unbaked conditional(ConditionalItemModelProperty pProperty, ItemModel.Unbaked pOnTrue, ItemModel.Unbaked pOnFalse) {
        return new ConditionalItemModel.Unbaked(pProperty, pOnTrue, pOnFalse);
    }

    public static <T> SelectItemModel.SwitchCase<T> when(T pValue, ItemModel.Unbaked pModel) {
        return new SelectItemModel.SwitchCase<>(List.of(pValue), pModel);
    }

    public static <T> SelectItemModel.SwitchCase<T> when(List<T> pValues, ItemModel.Unbaked pModel) {
        return new SelectItemModel.SwitchCase<>(pValues, pModel);
    }

    @SafeVarargs
    public static <T> ItemModel.Unbaked select(SelectItemModelProperty<T> pProperty, ItemModel.Unbaked pFallback, SelectItemModel.SwitchCase<T>... pCases) {
        return select(pProperty, pFallback, List.of(pCases));
    }

    public static <T> ItemModel.Unbaked select(
        SelectItemModelProperty<T> pProperty, ItemModel.Unbaked pFallback, List<SelectItemModel.SwitchCase<T>> pCases
    ) {
        return new SelectItemModel.Unbaked(new SelectItemModel.UnbakedSwitch<>(pProperty, pCases), Optional.of(pFallback));
    }

    @SafeVarargs
    public static <T> ItemModel.Unbaked select(SelectItemModelProperty<T> pProperty, SelectItemModel.SwitchCase<T>... pCases) {
        return select(pProperty, List.of(pCases));
    }

    public static <T> ItemModel.Unbaked select(SelectItemModelProperty<T> pProperty, List<SelectItemModel.SwitchCase<T>> pCases) {
        return new SelectItemModel.Unbaked(new SelectItemModel.UnbakedSwitch<>(pProperty, pCases), Optional.empty());
    }

    public static ConditionalItemModelProperty isUsingItem() {
        return new IsUsingItem();
    }

    public static ConditionalItemModelProperty hasComponent(DataComponentType<?> pComponentType) {
        return new HasComponent(pComponentType, false);
    }

    public static ItemModel.Unbaked inOverworld(ItemModel.Unbaked pInOverworldModel, ItemModel.Unbaked pNotInOverworldModel) {
        return select(new ContextDimension(), pNotInOverworldModel, when(Level.OVERWORLD, pInOverworldModel));
    }

    public static <T extends Comparable<T>> ItemModel.Unbaked selectBlockItemProperty(Property<T> pProperty, ItemModel.Unbaked pFallback, Map<T, ItemModel.Unbaked> pModelMap) {
        List<SelectItemModel.SwitchCase<String>> list = pModelMap.entrySet().stream().sorted(Entry.comparingByKey()).map(p_375487_ -> {
            String s = pProperty.getName(p_375487_.getKey());
            return new SelectItemModel.SwitchCase<>(List.of(s), p_375487_.getValue());
        }).toList();
        return select(new ItemBlockState(pProperty.getName()), pFallback, list);
    }

    public static ItemModel.Unbaked isXmas(ItemModel.Unbaked pXmasModel, ItemModel.Unbaked pNormalModel) {
        return select(LocalTime.create("MM-dd", "", Optional.empty()), pNormalModel, List.of(when(List.of("12-24", "12-25", "12-26"), pXmasModel)));
    }
}