package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ServerboundContainerClosePacket implements Packet<ServerGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ServerboundContainerClosePacket> STREAM_CODEC = Packet.codec(
        ServerboundContainerClosePacket::write, ServerboundContainerClosePacket::new
    );
    private final int containerId;

    public ServerboundContainerClosePacket(int pContainerId) {
        this.containerId = pContainerId;
    }

    private ServerboundContainerClosePacket(FriendlyByteBuf pBuffer) {
        this.containerId = pBuffer.readContainerId();
    }

    private void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeContainerId(this.containerId);
    }

    @Override
    public PacketType<ServerboundContainerClosePacket> type() {
        return GamePacketTypes.SERVERBOUND_CONTAINER_CLOSE;
    }

    public void handle(ServerGamePacketListener pHandler) {
        pHandler.handleContainerClose(this);
    }

    public int getContainerId() {
        return this.containerId;
    }
}