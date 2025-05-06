package net.minecraft.network;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

public class LocalFrameEncoder extends ChannelOutboundHandlerAdapter {
    @Override
    public void write(ChannelHandlerContext pContext, Object pMsg, ChannelPromise pPromise) {
        pContext.write(HiddenByteBuf.pack(pMsg), pPromise);
    }
}