package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.InteractionHand;

public class ServerboundUseItemPacket implements Packet<ServerGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ServerboundUseItemPacket> STREAM_CODEC = Packet.codec(
        ServerboundUseItemPacket::write, ServerboundUseItemPacket::new
    );
    private final InteractionHand hand;
    private final int sequence;
    private final float yRot;
    private final float xRot;

    public ServerboundUseItemPacket(InteractionHand pHand, int pSequence, float pYRot, float pXRot) {
        this.hand = pHand;
        this.sequence = pSequence;
        this.yRot = pYRot;
        this.xRot = pXRot;
    }

    private ServerboundUseItemPacket(FriendlyByteBuf pBuffer) {
        this.hand = pBuffer.readEnum(InteractionHand.class);
        this.sequence = pBuffer.readVarInt();
        this.yRot = pBuffer.readFloat();
        this.xRot = pBuffer.readFloat();
    }

    private void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeEnum(this.hand);
        pBuffer.writeVarInt(this.sequence);
        pBuffer.writeFloat(this.yRot);
        pBuffer.writeFloat(this.xRot);
    }

    @Override
    public PacketType<ServerboundUseItemPacket> type() {
        return GamePacketTypes.SERVERBOUND_USE_ITEM;
    }

    public void handle(ServerGamePacketListener pHandler) {
        pHandler.handleUseItem(this);
    }

    public InteractionHand getHand() {
        return this.hand;
    }

    public int getSequence() {
        return this.sequence;
    }

    public float getYRot() {
        return this.yRot;
    }

    public float getXRot() {
        return this.xRot;
    }
}