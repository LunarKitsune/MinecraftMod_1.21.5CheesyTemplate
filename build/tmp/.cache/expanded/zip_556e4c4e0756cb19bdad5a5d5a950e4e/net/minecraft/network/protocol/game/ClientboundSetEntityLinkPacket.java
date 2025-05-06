package net.minecraft.network.protocol.game;

import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.entity.Entity;

public class ClientboundSetEntityLinkPacket implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ClientboundSetEntityLinkPacket> STREAM_CODEC = Packet.codec(
        ClientboundSetEntityLinkPacket::write, ClientboundSetEntityLinkPacket::new
    );
    private final int sourceId;
    private final int destId;

    public ClientboundSetEntityLinkPacket(Entity pSource, @Nullable Entity pDestination) {
        this.sourceId = pSource.getId();
        this.destId = pDestination != null ? pDestination.getId() : 0;
    }

    private ClientboundSetEntityLinkPacket(FriendlyByteBuf pBuffer) {
        this.sourceId = pBuffer.readInt();
        this.destId = pBuffer.readInt();
    }

    private void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeInt(this.sourceId);
        pBuffer.writeInt(this.destId);
    }

    @Override
    public PacketType<ClientboundSetEntityLinkPacket> type() {
        return GamePacketTypes.CLIENTBOUND_SET_ENTITY_LINK;
    }

    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleEntityLinkPacket(this);
    }

    public int getSourceId() {
        return this.sourceId;
    }

    public int getDestId() {
        return this.destId;
    }
}