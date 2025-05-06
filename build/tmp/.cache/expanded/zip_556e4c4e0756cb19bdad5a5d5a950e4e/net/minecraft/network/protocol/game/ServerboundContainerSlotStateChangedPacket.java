package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public record ServerboundContainerSlotStateChangedPacket(int slotId, int containerId, boolean newState) implements Packet<ServerGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ServerboundContainerSlotStateChangedPacket> STREAM_CODEC = Packet.codec(
        ServerboundContainerSlotStateChangedPacket::write, ServerboundContainerSlotStateChangedPacket::new
    );

    private ServerboundContainerSlotStateChangedPacket(FriendlyByteBuf pBuffer) {
        this(pBuffer.readVarInt(), pBuffer.readContainerId(), pBuffer.readBoolean());
    }

    private void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeVarInt(this.slotId);
        pBuffer.writeContainerId(this.containerId);
        pBuffer.writeBoolean(this.newState);
    }

    @Override
    public PacketType<ServerboundContainerSlotStateChangedPacket> type() {
        return GamePacketTypes.SERVERBOUND_CONTAINER_SLOT_STATE_CHANGED;
    }

    public void handle(ServerGamePacketListener p_309835_) {
        p_309835_.handleContainerSlotStateChanged(this);
    }
}