package net.minecraft.network.protocol.game;

import java.time.Instant;
import net.minecraft.commands.arguments.ArgumentSignatures;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.LastSeenMessages;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public record ServerboundChatCommandSignedPacket(
    String command, Instant timeStamp, long salt, ArgumentSignatures argumentSignatures, LastSeenMessages.Update lastSeenMessages
) implements Packet<ServerGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ServerboundChatCommandSignedPacket> STREAM_CODEC = Packet.codec(
        ServerboundChatCommandSignedPacket::write, ServerboundChatCommandSignedPacket::new
    );

    private ServerboundChatCommandSignedPacket(FriendlyByteBuf pBuffer) {
        this(pBuffer.readUtf(), pBuffer.readInstant(), pBuffer.readLong(), new ArgumentSignatures(pBuffer), new LastSeenMessages.Update(pBuffer));
    }

    private void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeUtf(this.command);
        pBuffer.writeInstant(this.timeStamp);
        pBuffer.writeLong(this.salt);
        this.argumentSignatures.write(pBuffer);
        this.lastSeenMessages.write(pBuffer);
    }

    @Override
    public PacketType<ServerboundChatCommandSignedPacket> type() {
        return GamePacketTypes.SERVERBOUND_CHAT_COMMAND_SIGNED;
    }

    public void handle(ServerGamePacketListener p_329693_) {
        p_329693_.handleSignedChatCommand(this);
    }
}