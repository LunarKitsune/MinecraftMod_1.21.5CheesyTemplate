package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.util.ReferenceCounted;

public record HiddenByteBuf(ByteBuf contents) implements ReferenceCounted {
    public HiddenByteBuf(final ByteBuf contents) {
        this.contents = ByteBufUtil.ensureAccessible(contents);
    }

    public static Object pack(Object pObject) {
        return pObject instanceof ByteBuf bytebuf ? new HiddenByteBuf(bytebuf) : pObject;
    }

    public static Object unpack(Object pObject) {
        return pObject instanceof HiddenByteBuf hiddenbytebuf ? ByteBufUtil.ensureAccessible(hiddenbytebuf.contents) : pObject;
    }

    @Override
    public int refCnt() {
        return this.contents.refCnt();
    }

    public HiddenByteBuf retain() {
        this.contents.retain();
        return this;
    }

    public HiddenByteBuf retain(int p_377803_) {
        this.contents.retain(p_377803_);
        return this;
    }

    public HiddenByteBuf touch() {
        this.contents.touch();
        return this;
    }

    public HiddenByteBuf touch(Object p_376790_) {
        this.contents.touch(p_376790_);
        return this;
    }

    @Override
    public boolean release() {
        return this.contents.release();
    }

    @Override
    public boolean release(int pDecrement) {
        return this.contents.release(pDecrement);
    }
}