package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class MonitoredLocalFrameDecoder extends ChannelInboundHandlerAdapter {
    private final BandwidthDebugMonitor monitor;

    public MonitoredLocalFrameDecoder(BandwidthDebugMonitor pMonitor) {
        this.monitor = pMonitor;
    }

    @Override
    public void channelRead(ChannelHandlerContext pContext, Object pMsg) {
        pMsg = HiddenByteBuf.unpack(pMsg);
        if (pMsg instanceof ByteBuf bytebuf) {
            this.monitor.onReceive(bytebuf.readableBytes());
        }

        pContext.fireChannelRead(pMsg);
    }
}