package net.minecraft.network.protocol.game;

import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public class ClientboundSetCameraPacket implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ClientboundSetCameraPacket> STREAM_CODEC = Packet.codec(
        ClientboundSetCameraPacket::write, ClientboundSetCameraPacket::new
    );
    private final int cameraId;

    public ClientboundSetCameraPacket(Entity pCameraEntity) {
        this.cameraId = pCameraEntity.getId();
    }

    private ClientboundSetCameraPacket(FriendlyByteBuf pBuffer) {
        this.cameraId = pBuffer.readVarInt();
    }

    private void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeVarInt(this.cameraId);
    }

    @Override
    public PacketType<ClientboundSetCameraPacket> type() {
        return GamePacketTypes.CLIENTBOUND_SET_CAMERA;
    }

    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleSetCamera(this);
    }

    @Nullable
    public Entity getEntity(Level pLevel) {
        return pLevel.getEntity(this.cameraId);
    }
}