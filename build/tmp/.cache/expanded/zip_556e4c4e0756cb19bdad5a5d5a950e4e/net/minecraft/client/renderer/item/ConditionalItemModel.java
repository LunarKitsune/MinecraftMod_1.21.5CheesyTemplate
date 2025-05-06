package net.minecraft.client.renderer.item;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.CacheSlot;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperties;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.client.renderer.item.properties.conditional.ItemModelPropertyTest;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.util.RegistryContextSwapper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ConditionalItemModel implements ItemModel {
    private final ItemModelPropertyTest property;
    private final ItemModel onTrue;
    private final ItemModel onFalse;

    public ConditionalItemModel(ItemModelPropertyTest pProperty, ItemModel pOnTrue, ItemModel pOnFalse) {
        this.property = pProperty;
        this.onTrue = pOnTrue;
        this.onFalse = pOnFalse;
    }

    @Override
    public void update(
        ItemStackRenderState p_376429_,
        ItemStack p_375999_,
        ItemModelResolver p_375782_,
        ItemDisplayContext p_376170_,
        @Nullable ClientLevel p_378740_,
        @Nullable LivingEntity p_376673_,
        int p_376970_
    ) {
        (this.property.get(p_375999_, p_378740_, p_376673_, p_376970_, p_376170_) ? this.onTrue : this.onFalse)
            .update(p_376429_, p_375999_, p_375782_, p_376170_, p_378740_, p_376673_, p_376970_);
    }

    @OnlyIn(Dist.CLIENT)
    public record Unbaked(ConditionalItemModelProperty property, ItemModel.Unbaked onTrue, ItemModel.Unbaked onFalse) implements ItemModel.Unbaked {
        public static final MapCodec<ConditionalItemModel.Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
            p_375749_ -> p_375749_.group(
                    ConditionalItemModelProperties.MAP_CODEC.forGetter(ConditionalItemModel.Unbaked::property),
                    ItemModels.CODEC.fieldOf("on_true").forGetter(ConditionalItemModel.Unbaked::onTrue),
                    ItemModels.CODEC.fieldOf("on_false").forGetter(ConditionalItemModel.Unbaked::onFalse)
                )
                .apply(p_375749_, ConditionalItemModel.Unbaked::new)
        );

        @Override
        public MapCodec<ConditionalItemModel.Unbaked> type() {
            return MAP_CODEC;
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext p_376462_) {
            return new ConditionalItemModel(
                this.adaptProperty(this.property, p_376462_.contextSwapper()), this.onTrue.bake(p_376462_), this.onFalse.bake(p_376462_)
            );
        }

        private ItemModelPropertyTest adaptProperty(ConditionalItemModelProperty pProperty, @Nullable RegistryContextSwapper pContextSwapper) {
            if (pContextSwapper == null) {
                return pProperty;
            } else {
                CacheSlot<ClientLevel, ItemModelPropertyTest> cacheslot = new CacheSlot<>(p_389533_ -> swapContext(pProperty, pContextSwapper, p_389533_));
                return (p_389526_, p_389527_, p_389528_, p_389529_, p_389530_) -> {
                    ItemModelPropertyTest itemmodelpropertytest = (ItemModelPropertyTest)(p_389527_ == null ? pProperty : cacheslot.compute(p_389527_));
                    return itemmodelpropertytest.get(p_389526_, p_389527_, p_389528_, p_389529_, p_389530_);
                };
            }
        }

        private static <T extends ConditionalItemModelProperty> T swapContext(T pProperty, RegistryContextSwapper pContextSwapper, ClientLevel pLevel) {
            return (T)pContextSwapper.swapTo(((MapCodec<T>)pProperty.type()).codec(), pProperty, pLevel.registryAccess()).result().orElse(pProperty);
        }

        @Override
        public void resolveDependencies(ResolvableModel.Resolver p_376408_) {
            this.onTrue.resolveDependencies(p_376408_);
            this.onFalse.resolveDependencies(p_376408_);
        }
    }
}