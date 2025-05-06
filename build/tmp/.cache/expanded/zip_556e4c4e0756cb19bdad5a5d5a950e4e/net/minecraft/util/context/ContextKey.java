package net.minecraft.util.context;

import net.minecraft.resources.ResourceLocation;

public class ContextKey<T> {
    private final ResourceLocation name;

    public ContextKey(ResourceLocation pName) {
        this.name = pName;
    }

    public static <T> ContextKey<T> vanilla(String pName) {
        return new ContextKey<>(ResourceLocation.withDefaultNamespace(pName));
    }

    public ResourceLocation name() {
        return this.name;
    }

    @Override
    public String toString() {
        return "<parameter " + this.name + ">";
    }
}