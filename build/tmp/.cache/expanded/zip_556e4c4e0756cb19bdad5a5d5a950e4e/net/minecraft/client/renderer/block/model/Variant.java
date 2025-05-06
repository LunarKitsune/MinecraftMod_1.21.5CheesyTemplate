package net.minecraft.client.renderer.block.model;

import com.mojang.math.Quadrant;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record Variant(ResourceLocation modelLocation, Variant.SimpleModelState modelState) implements BlockModelPart.Unbaked {
    public static final MapCodec<Variant> MAP_CODEC = RecordCodecBuilder.mapCodec(
        p_394819_ -> p_394819_.group(
                ResourceLocation.CODEC.fieldOf("model").forGetter(Variant::modelLocation), Variant.SimpleModelState.MAP_CODEC.forGetter(Variant::modelState)
            )
            .apply(p_394819_, Variant::new)
    );
    public static final Codec<Variant> CODEC = MAP_CODEC.codec();

    public Variant(ResourceLocation pModelLocation) {
        this(pModelLocation, Variant.SimpleModelState.DEFAULT);
    }

    public Variant withXRot(Quadrant pXRot) {
        return this.withState(this.modelState.withX(pXRot));
    }

    public Variant withYRot(Quadrant pYRot) {
        return this.withState(this.modelState.withY(pYRot));
    }

    public Variant withUvLock(boolean pUvLock) {
        return this.withState(this.modelState.withUvLock(pUvLock));
    }

    public Variant withModel(ResourceLocation pModelLocation) {
        return new Variant(pModelLocation, this.modelState);
    }

    public Variant withState(Variant.SimpleModelState pModelState) {
        return new Variant(this.modelLocation, pModelState);
    }

    public Variant with(VariantMutator pMutator) {
        return pMutator.apply(this);
    }

    @Override
    public BlockModelPart bake(ModelBaker p_397047_) {
        return SimpleModelWrapper.bake(p_397047_, this.modelLocation, this.modelState.asModelState());
    }

    @Override
    public void resolveDependencies(ResolvableModel.Resolver p_391294_) {
        p_391294_.markDependency(this.modelLocation);
    }

    @OnlyIn(Dist.CLIENT)
    public record SimpleModelState(Quadrant x, Quadrant y, boolean uvLock) {
        public static final MapCodec<Variant.SimpleModelState> MAP_CODEC = RecordCodecBuilder.mapCodec(
            p_391692_ -> p_391692_.group(
                    Quadrant.CODEC.optionalFieldOf("x", Quadrant.R0).forGetter(Variant.SimpleModelState::x),
                    Quadrant.CODEC.optionalFieldOf("y", Quadrant.R0).forGetter(Variant.SimpleModelState::y),
                    Codec.BOOL.optionalFieldOf("uvlock", false).forGetter(Variant.SimpleModelState::uvLock)
                )
                .apply(p_391692_, Variant.SimpleModelState::new)
        );
        public static final Variant.SimpleModelState DEFAULT = new Variant.SimpleModelState(Quadrant.R0, Quadrant.R0, false);

        public ModelState asModelState() {
            BlockModelRotation blockmodelrotation = BlockModelRotation.by(this.x, this.y);
            return (ModelState)(this.uvLock ? blockmodelrotation.withUvLock() : blockmodelrotation);
        }

        public Variant.SimpleModelState withX(Quadrant pXRot) {
            return new Variant.SimpleModelState(pXRot, this.y, this.uvLock);
        }

        public Variant.SimpleModelState withY(Quadrant pYRot) {
            return new Variant.SimpleModelState(this.x, pYRot, this.uvLock);
        }

        public Variant.SimpleModelState withUvLock(boolean pUvLock) {
            return new Variant.SimpleModelState(this.x, this.y, pUvLock);
        }
    }
}