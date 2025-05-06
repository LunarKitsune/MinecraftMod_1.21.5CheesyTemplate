package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public class ClientboundRotateHeadPacket implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ClientboundRotateHeadPacket> STREAM_CODEC = Packet.codec(
        ClientboundRotateHeadPacket::write, ClientboundRotateHeadPacket::new
    );
    private final int entityId;
    private final byte yHeadRot;

    public ClientboundRotateHeadPacket(Entity pEntity, byte pYHeadRot) {
        this.entityId = pEntity.getId();
        this.yHeadRot = pYHeadRot;
    }

    private ClientboundRotateHeadPacket(FriendlyByteBuf pBuffer) {
        this.entityId = pBuffer.readVarInt();
        this.yHeadRot = pBuffer.readByte();
    }

    private void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeVarInt(this.entityId);
        pBuffer.writeByte(this.yHeadRot);
    }

    @Override
    public PacketType<ClientboundRotateHeadPacket> type() {
        return GamePacketTypes.CLIENTBOUND_ROTATE_HEAD;
    }

    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleRotateMob(this);
    }

    public Entity getEntity(Level pLevel) {
        return pLevel.getEntity(this.entityId);
    }

    public float getYHeadRot() {
        return Mth.unpackDegrees(this.yHeadRot);
    }
}