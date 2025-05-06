package net.minecraft.client.renderer.block.model;

import com.mojang.math.Quadrant;
import java.util.function.UnaryOperator;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@FunctionalInterface
@OnlyIn(Dist.CLIENT)
public interface VariantMutator extends UnaryOperator<Variant> {
    VariantMutator.VariantProperty<Quadrant> X_ROT = Variant::withXRot;
    VariantMutator.VariantProperty<Quadrant> Y_ROT = Variant::withYRot;
    VariantMutator.VariantProperty<ResourceLocation> MODEL = Variant::withModel;
    VariantMutator.VariantProperty<Boolean> UV_LOCK = Variant::withUvLock;

    default VariantMutator then(VariantMutator pMutator) {
        return p_397265_ -> pMutator.apply(this.apply(p_397265_));
    }

    @FunctionalInterface
    @OnlyIn(Dist.CLIENT)
    public interface VariantProperty<T> {
        Variant apply(Variant pVariant, T pValue);

        default VariantMutator withValue(T pValue) {
            return p_393814_ -> this.apply(p_393814_, pValue);
        }
    }
}