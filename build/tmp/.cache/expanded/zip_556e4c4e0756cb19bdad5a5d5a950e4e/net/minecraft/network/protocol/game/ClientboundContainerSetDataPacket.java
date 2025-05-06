package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ClientboundContainerSetDataPacket implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ClientboundContainerSetDataPacket> STREAM_CODEC = Packet.codec(
        ClientboundContainerSetDataPacket::write, ClientboundContainerSetDataPacket::new
    );
    private final int containerId;
    private final int id;
    private final int value;

    public ClientboundContainerSetDataPacket(int pContainerId, int pId, int pValue) {
        this.containerId = pContainerId;
        this.id = pId;
        this.value = pValue;
    }

    private ClientboundContainerSetDataPacket(FriendlyByteBuf pBuffer) {
        this.containerId = pBuffer.readContainerId();
        this.id = pBuffer.readShort();
        this.value = pBuffer.readShort();
    }

    private void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeContainerId(this.containerId);
        pBuffer.writeShort(this.id);
        pBuffer.writeShort(this.value);
    }

    @Override
    public PacketType<ClientboundContainerSetDataPacket> type() {
        return GamePacketTypes.CLIENTBOUND_CONTAINER_SET_DATA;
    }

    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleContainerSetData(this);
    }

    public int getContainerId() {
        return this.containerId;
    }

    public int getId() {
        return this.id;
    }

    public int getValue() {
        return this.value;
    }
}