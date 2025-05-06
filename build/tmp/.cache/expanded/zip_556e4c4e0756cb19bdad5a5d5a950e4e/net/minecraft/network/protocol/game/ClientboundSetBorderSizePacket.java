package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.level.border.WorldBorder;

public class ClientboundSetBorderSizePacket implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ClientboundSetBorderSizePacket> STREAM_CODEC = Packet.codec(
        ClientboundSetBorderSizePacket::write, ClientboundSetBorderSizePacket::new
    );
    private final double size;

    public ClientboundSetBorderSizePacket(WorldBorder pWorldBorder) {
        this.size = pWorldBorder.getLerpTarget();
    }

    private ClientboundSetBorderSizePacket(FriendlyByteBuf pBuffer) {
        this.size = pBuffer.readDouble();
    }

    private void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeDouble(this.size);
    }

    @Override
    public PacketType<ClientboundSetBorderSizePacket> type() {
        return GamePacketTypes.CLIENTBOUND_SET_BORDER_SIZE;
    }

    public void handle(ClientGamePacketListener p_179251_) {
        p_179251_.handleSetBorderSize(this);
    }

    public double getSize() {
        return this.size;
    }
}