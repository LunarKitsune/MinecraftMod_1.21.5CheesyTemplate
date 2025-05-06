package net.minecraft.network.protocol.common;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.resources.ResourceLocation;

public record ClientboundStoreCookiePacket(ResourceLocation key, byte[] payload) implements Packet<ClientCommonPacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ClientboundStoreCookiePacket> STREAM_CODEC = Packet.codec(
        ClientboundStoreCookiePacket::write, ClientboundStoreCookiePacket::new
    );
    private static final int MAX_PAYLOAD_SIZE = 5120;
    public static final StreamCodec<ByteBuf, byte[]> PAYLOAD_STREAM_CODEC = ByteBufCodecs.byteArray(5120);

    private ClientboundStoreCookiePacket(FriendlyByteBuf pBuffer) {
        this(pBuffer.readResourceLocation(), PAYLOAD_STREAM_CODEC.decode(pBuffer));
    }

    private void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeResourceLocation(this.key);
        PAYLOAD_STREAM_CODEC.encode(pBuffer, this.payload);
    }

    @Override
    public PacketType<ClientboundStoreCookiePacket> type() {
        return CommonPacketTypes.CLIENTBOUND_STORE_COOKIE;
    }

    public void handle(ClientCommonPacketListener p_334968_) {
        p_334968_.handleStoreCookie(this);
    }
}