package net.minecraft.client.resources.model;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface ResolvableModel {
    void resolveDependencies(ResolvableModel.Resolver pResolver);

    @OnlyIn(Dist.CLIENT)
    public interface Resolver {
        void markDependency(ResourceLocation pDependency);
    }
}