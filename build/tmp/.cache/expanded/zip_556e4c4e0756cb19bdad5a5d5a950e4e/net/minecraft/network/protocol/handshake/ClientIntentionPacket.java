package net.minecraft.network.protocol.handshake;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public record ClientIntentionPacket(int protocolVersion, String hostName, int port, ClientIntent intention) implements Packet<ServerHandshakePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ClientIntentionPacket> STREAM_CODEC = Packet.codec(
        ClientIntentionPacket::write, ClientIntentionPacket::new
    );
    private static final int MAX_HOST_LENGTH = 255;

    @Deprecated
    public ClientIntentionPacket(int protocolVersion, String hostName, int port, ClientIntent intention) {
        this.protocolVersion = protocolVersion;
        this.hostName = hostName;
        this.port = port;
        this.intention = intention;
    }

    private ClientIntentionPacket(FriendlyByteBuf pBuffer) {
        this(pBuffer.readVarInt(), pBuffer.readUtf(255), pBuffer.readUnsignedShort(), ClientIntent.byId(pBuffer.readVarInt()));
    }

    private void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeVarInt(this.protocolVersion);
        pBuffer.writeUtf(this.hostName);
        pBuffer.writeShort(this.port);
        pBuffer.writeVarInt(this.intention.id());
    }

    @Override
    public PacketType<ClientIntentionPacket> type() {
        return HandshakePacketTypes.CLIENT_INTENTION;
    }

    public void handle(ServerHandshakePacketListener pHandler) {
        pHandler.handleIntention(this);
    }

    @Override
    public boolean isTerminal() {
        return true;
    }
}