package com.mojang.blaze3d.buffers;

import com.mojang.blaze3d.DontObfuscate;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@DontObfuscate
public enum BufferType {
    VERTICES,
    INDICES,
    PIXEL_PACK,
    COPY_READ,
    COPY_WRITE,
    PIXEL_UNPACK,
    UNIFORM;
}