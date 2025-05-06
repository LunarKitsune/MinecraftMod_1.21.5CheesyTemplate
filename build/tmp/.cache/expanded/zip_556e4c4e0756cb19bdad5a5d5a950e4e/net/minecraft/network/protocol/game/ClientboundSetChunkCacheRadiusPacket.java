package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ClientboundSetChunkCacheRadiusPacket implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ClientboundSetChunkCacheRadiusPacket> STREAM_CODEC = Packet.codec(
        ClientboundSetChunkCacheRadiusPacket::write, ClientboundSetChunkCacheRadiusPacket::new
    );
    private final int radius;

    public ClientboundSetChunkCacheRadiusPacket(int pRadius) {
        this.radius = pRadius;
    }

    private ClientboundSetChunkCacheRadiusPacket(FriendlyByteBuf pBuffer) {
        this.radius = pBuffer.readVarInt();
    }

    private void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeVarInt(this.radius);
    }

    @Override
    public PacketType<ClientboundSetChunkCacheRadiusPacket> type() {
        return GamePacketTypes.CLIENTBOUND_SET_CHUNK_CACHE_RADIUS;
    }

    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleSetChunkCacheRadius(this);
    }

    public int getRadius() {
        return this.radius;
    }
}