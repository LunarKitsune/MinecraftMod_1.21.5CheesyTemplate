package net.minecraft.network.protocol.game;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ClientboundLevelEventPacket implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ClientboundLevelEventPacket> STREAM_CODEC = Packet.codec(
        ClientboundLevelEventPacket::write, ClientboundLevelEventPacket::new
    );
    private final int type;
    private final BlockPos pos;
    private final int data;
    private final boolean globalEvent;

    public ClientboundLevelEventPacket(int pType, BlockPos pPos, int pData, boolean pGlobalEvent) {
        this.type = pType;
        this.pos = pPos.immutable();
        this.data = pData;
        this.globalEvent = pGlobalEvent;
    }

    private ClientboundLevelEventPacket(FriendlyByteBuf pBuffer) {
        this.type = pBuffer.readInt();
        this.pos = pBuffer.readBlockPos();
        this.data = pBuffer.readInt();
        this.globalEvent = pBuffer.readBoolean();
    }

    private void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeInt(this.type);
        pBuffer.writeBlockPos(this.pos);
        pBuffer.writeInt(this.data);
        pBuffer.writeBoolean(this.globalEvent);
    }

    @Override
    public PacketType<ClientboundLevelEventPacket> type() {
        return GamePacketTypes.CLIENTBOUND_LEVEL_EVENT;
    }

    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleLevelEvent(this);
    }

    public boolean isGlobalEvent() {
        return this.globalEvent;
    }

    public int getType() {
        return this.type;
    }

    public int getData() {
        return this.data;
    }

    public BlockPos getPos() {
        return this.pos;
    }
}