package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.level.border.WorldBorder;

public class ClientboundSetBorderLerpSizePacket implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ClientboundSetBorderLerpSizePacket> STREAM_CODEC = Packet.codec(
        ClientboundSetBorderLerpSizePacket::write, ClientboundSetBorderLerpSizePacket::new
    );
    private final double oldSize;
    private final double newSize;
    private final long lerpTime;

    public ClientboundSetBorderLerpSizePacket(WorldBorder pWorldBorder) {
        this.oldSize = pWorldBorder.getSize();
        this.newSize = pWorldBorder.getLerpTarget();
        this.lerpTime = pWorldBorder.getLerpRemainingTime();
    }

    private ClientboundSetBorderLerpSizePacket(FriendlyByteBuf pBuffer) {
        this.oldSize = pBuffer.readDouble();
        this.newSize = pBuffer.readDouble();
        this.lerpTime = pBuffer.readVarLong();
    }

    private void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeDouble(this.oldSize);
        pBuffer.writeDouble(this.newSize);
        pBuffer.writeVarLong(this.lerpTime);
    }

    @Override
    public PacketType<ClientboundSetBorderLerpSizePacket> type() {
        return GamePacketTypes.CLIENTBOUND_SET_BORDER_LERP_SIZE;
    }

    public void handle(ClientGamePacketListener p_179237_) {
        p_179237_.handleSetBorderLerpSize(this);
    }

    public double getOldSize() {
        return this.oldSize;
    }

    public double getNewSize() {
        return this.newSize;
    }

    public long getLerpTime() {
        return this.lerpTime;
    }
}