package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ServerboundClientCommandPacket implements Packet<ServerGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ServerboundClientCommandPacket> STREAM_CODEC = Packet.codec(
        ServerboundClientCommandPacket::write, ServerboundClientCommandPacket::new
    );
    private final ServerboundClientCommandPacket.Action action;

    public ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action pAction) {
        this.action = pAction;
    }

    private ServerboundClientCommandPacket(FriendlyByteBuf pBuffer) {
        this.action = pBuffer.readEnum(ServerboundClientCommandPacket.Action.class);
    }

    private void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeEnum(this.action);
    }

    @Override
    public PacketType<ServerboundClientCommandPacket> type() {
        return GamePacketTypes.SERVERBOUND_CLIENT_COMMAND;
    }

    public void handle(ServerGamePacketListener pHandler) {
        pHandler.handleClientCommand(this);
    }

    public ServerboundClientCommandPacket.Action getAction() {
        return this.action;
    }

    public static enum Action {
        PERFORM_RESPAWN,
        REQUEST_STATS;
    }
}