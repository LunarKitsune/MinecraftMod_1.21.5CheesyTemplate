package com.mojang.blaze3d.resource;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface ResourceDescriptor<T> {
    T allocate();

    default void prepare(T pTarget) {
    }

    void free(T pTarget);

    default boolean canUsePhysicalResource(ResourceDescriptor<?> pDescriptor) {
        return this.equals(pDescriptor);
    }
}