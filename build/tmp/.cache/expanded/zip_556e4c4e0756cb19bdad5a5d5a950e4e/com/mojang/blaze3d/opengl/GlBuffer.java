package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.buffers.BufferType;
import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.jtracy.MemoryPool;
import com.mojang.jtracy.TracyClient;
import java.nio.ByteBuffer;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GlBuffer extends GpuBuffer {
    protected static final MemoryPool MEMORY_POOl = TracyClient.createMemoryPool("GPU Buffers");
    protected boolean closed;
    protected boolean initialized = false;
    @Nullable
    protected final Supplier<String> label;
    protected final int handle;

    protected GlBuffer(GlDebugLabel pDebugLabel, @Nullable Supplier<String> pLabel, BufferType pType, BufferUsage pUsage, int pSize, int pHandle) {
        super(pType, pUsage, pSize);
        this.label = pLabel;
        this.handle = pHandle;
        if (pUsage.isReadable()) {
            GlStateManager._glBindBuffer(GlConst.toGl(pType), pHandle);
            GlStateManager._glBufferData(GlConst.toGl(pType), pSize, GlConst.toGl(pUsage));
            MEMORY_POOl.malloc(pHandle, pSize);
            this.initialized = true;
            pDebugLabel.applyLabel(this);
        }
    }

    protected void ensureBufferExists() {
        if (!this.initialized) {
            GlStateManager._glBindBuffer(GlConst.toGl(this.type()), this.handle);
            GlStateManager._glBindBuffer(GlConst.toGl(this.type()), 0);
        }
    }

    @Override
    public boolean isClosed() {
        return this.closed;
    }

    @Override
    public void close() {
        if (!this.closed) {
            this.closed = true;
            GlStateManager._glDeleteBuffers(this.handle);
            if (this.initialized) {
                MEMORY_POOl.free(this.handle);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class ReadView implements GpuBuffer.ReadView {
        private final int target;
        private final ByteBuffer data;

        protected ReadView(int pTarget, ByteBuffer pData) {
            this.target = pTarget;
            this.data = pData;
        }

        @Override
        public ByteBuffer data() {
            return this.data;
        }

        @Override
        public void close() {
            GlStateManager._glUnmapBuffer(this.target);
        }
    }
}