package net.minecraft.network.protocol.common;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ServerboundPongPacket implements Packet<ServerCommonPacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ServerboundPongPacket> STREAM_CODEC = Packet.codec(
        ServerboundPongPacket::write, ServerboundPongPacket::new
    );
    private final int id;

    public ServerboundPongPacket(int pId) {
        this.id = pId;
    }

    private ServerboundPongPacket(FriendlyByteBuf pBuffer) {
        this.id = pBuffer.readInt();
    }

    private void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeInt(this.id);
    }

    @Override
    public PacketType<ServerboundPongPacket> type() {
        return CommonPacketTypes.SERVERBOUND_PONG;
    }

    public void handle(ServerCommonPacketListener p_298626_) {
        p_298626_.handlePong(this);
    }

    public int getId() {
        return this.id;
    }
}