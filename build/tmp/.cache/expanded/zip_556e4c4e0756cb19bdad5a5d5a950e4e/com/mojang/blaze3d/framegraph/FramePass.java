package com.mojang.blaze3d.framegraph;

import com.mojang.blaze3d.resource.ResourceDescriptor;
import com.mojang.blaze3d.resource.ResourceHandle;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface FramePass {
    <T> ResourceHandle<T> createsInternal(String pName, ResourceDescriptor<T> pDescriptor);

    <T> void reads(ResourceHandle<T> pHandle);

    <T> ResourceHandle<T> readsAndWrites(ResourceHandle<T> pHandle);

    void requires(FramePass pPass);

    void disableCulling();

    void executes(Runnable pTask);
}