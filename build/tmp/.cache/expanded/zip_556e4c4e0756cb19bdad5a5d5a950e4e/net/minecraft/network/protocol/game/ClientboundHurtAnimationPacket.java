package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.entity.LivingEntity;

public record ClientboundHurtAnimationPacket(int id, float yaw) implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ClientboundHurtAnimationPacket> STREAM_CODEC = Packet.codec(
        ClientboundHurtAnimationPacket::write, ClientboundHurtAnimationPacket::new
    );

    public ClientboundHurtAnimationPacket(LivingEntity pEntity) {
        this(pEntity.getId(), pEntity.getHurtDir());
    }

    private ClientboundHurtAnimationPacket(FriendlyByteBuf pBuffer) {
        this(pBuffer.readVarInt(), pBuffer.readFloat());
    }

    private void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeVarInt(this.id);
        pBuffer.writeFloat(this.yaw);
    }

    @Override
    public PacketType<ClientboundHurtAnimationPacket> type() {
        return GamePacketTypes.CLIENTBOUND_HURT_ANIMATION;
    }

    public void handle(ClientGamePacketListener p_265654_) {
        p_265654_.handleHurtAnimation(this);
    }
}