package net.minecraft.network;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class LocalFrameDecoder extends ChannelInboundHandlerAdapter {
    @Override
    public void channelRead(ChannelHandlerContext pContext, Object pMsg) {
        pContext.fireChannelRead(HiddenByteBuf.unpack(pMsg));
    }
}