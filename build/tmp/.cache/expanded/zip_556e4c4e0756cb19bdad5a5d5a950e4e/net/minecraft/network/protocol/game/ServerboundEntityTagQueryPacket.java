package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ServerboundEntityTagQueryPacket implements Packet<ServerGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ServerboundEntityTagQueryPacket> STREAM_CODEC = Packet.codec(
        ServerboundEntityTagQueryPacket::write, ServerboundEntityTagQueryPacket::new
    );
    private final int transactionId;
    private final int entityId;

    public ServerboundEntityTagQueryPacket(int pTransactionId, int pEntityId) {
        this.transactionId = pTransactionId;
        this.entityId = pEntityId;
    }

    private ServerboundEntityTagQueryPacket(FriendlyByteBuf pBuffer) {
        this.transactionId = pBuffer.readVarInt();
        this.entityId = pBuffer.readVarInt();
    }

    private void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeVarInt(this.transactionId);
        pBuffer.writeVarInt(this.entityId);
    }

    @Override
    public PacketType<ServerboundEntityTagQueryPacket> type() {
        return GamePacketTypes.SERVERBOUND_ENTITY_TAG_QUERY;
    }

    public void handle(ServerGamePacketListener p_330266_) {
        p_330266_.handleEntityTagQuery(this);
    }

    public int getTransactionId() {
        return this.transactionId;
    }

    public int getEntityId() {
        return this.entityId;
    }
}