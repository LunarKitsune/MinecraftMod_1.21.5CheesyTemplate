package net.minecraft.client.renderer;

import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record MaterialMapper(ResourceLocation sheet, String prefix) {
    public Material apply(ResourceLocation pName) {
        return new Material(this.sheet, pName.withPrefix(this.prefix + "/"));
    }

    public Material defaultNamespaceApply(String pName) {
        return this.apply(ResourceLocation.withDefaultNamespace(pName));
    }
}