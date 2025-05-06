package net.minecraft.network.protocol.common;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public record ClientboundTransferPacket(String host, int port) implements Packet<ClientCommonPacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ClientboundTransferPacket> STREAM_CODEC = Packet.codec(
        ClientboundTransferPacket::write, ClientboundTransferPacket::new
    );

    private ClientboundTransferPacket(FriendlyByteBuf pBuffer) {
        this(pBuffer.readUtf(), pBuffer.readVarInt());
    }

    private void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeUtf(this.host);
        pBuffer.writeVarInt(this.port);
    }

    @Override
    public PacketType<ClientboundTransferPacket> type() {
        return CommonPacketTypes.CLIENTBOUND_TRANSFER;
    }

    public void handle(ClientCommonPacketListener p_328535_) {
        p_328535_.handleTransfer(this);
    }
}