package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ClientboundHorseScreenOpenPacket implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ClientboundHorseScreenOpenPacket> STREAM_CODEC = Packet.codec(
        ClientboundHorseScreenOpenPacket::write, ClientboundHorseScreenOpenPacket::new
    );
    private final int containerId;
    private final int inventoryColumns;
    private final int entityId;

    public ClientboundHorseScreenOpenPacket(int pContainerId, int pSize, int pEntityId) {
        this.containerId = pContainerId;
        this.inventoryColumns = pSize;
        this.entityId = pEntityId;
    }

    private ClientboundHorseScreenOpenPacket(FriendlyByteBuf pBuffer) {
        this.containerId = pBuffer.readContainerId();
        this.inventoryColumns = pBuffer.readVarInt();
        this.entityId = pBuffer.readInt();
    }

    private void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeContainerId(this.containerId);
        pBuffer.writeVarInt(this.inventoryColumns);
        pBuffer.writeInt(this.entityId);
    }

    @Override
    public PacketType<ClientboundHorseScreenOpenPacket> type() {
        return GamePacketTypes.CLIENTBOUND_HORSE_SCREEN_OPEN;
    }

    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleHorseScreenOpen(this);
    }

    public int getContainerId() {
        return this.containerId;
    }

    public int getInventoryColumns() {
        return this.inventoryColumns;
    }

    public int getEntityId() {
        return this.entityId;
    }
}