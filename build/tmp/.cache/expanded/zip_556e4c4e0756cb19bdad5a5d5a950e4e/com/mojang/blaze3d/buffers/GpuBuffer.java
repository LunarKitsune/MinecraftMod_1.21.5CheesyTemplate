package com.mojang.blaze3d.buffers;

import com.mojang.blaze3d.DontObfuscate;
import java.nio.ByteBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@DontObfuscate
public abstract class GpuBuffer implements AutoCloseable {
    private final BufferType type;
    private final BufferUsage usage;
    public int size;

    public GpuBuffer(BufferType pType, BufferUsage pUsage, int pSize) {
        this.type = pType;
        this.size = pSize;
        this.usage = pUsage;
    }

    public int size() {
        return this.size;
    }

    public BufferType type() {
        return this.type;
    }

    public BufferUsage usage() {
        return this.usage;
    }

    public abstract boolean isClosed();

    @Override
    public abstract void close();

    @OnlyIn(Dist.CLIENT)
    @DontObfuscate
    public interface ReadView extends AutoCloseable {
        ByteBuffer data();

        @Override
        void close();
    }
}