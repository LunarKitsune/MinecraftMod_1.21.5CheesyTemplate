package net.minecraft.network.protocol.game;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.FilterMask;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.SignedMessageBody;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public record ClientboundPlayerChatPacket(
    int globalIndex,
    UUID sender,
    int index,
    @Nullable MessageSignature signature,
    SignedMessageBody.Packed body,
    @Nullable Component unsignedContent,
    FilterMask filterMask,
    ChatType.Bound chatType
) implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundPlayerChatPacket> STREAM_CODEC = Packet.codec(
        ClientboundPlayerChatPacket::write, ClientboundPlayerChatPacket::new
    );

    private ClientboundPlayerChatPacket(RegistryFriendlyByteBuf pBuffer) {
        this(
            pBuffer.readVarInt(),
            pBuffer.readUUID(),
            pBuffer.readVarInt(),
            pBuffer.readNullable(MessageSignature::read),
            new SignedMessageBody.Packed(pBuffer),
            FriendlyByteBuf.readNullable(pBuffer, ComponentSerialization.TRUSTED_STREAM_CODEC),
            FilterMask.read(pBuffer),
            ChatType.Bound.STREAM_CODEC.decode(pBuffer)
        );
    }

    private void write(RegistryFriendlyByteBuf pBuffer) {
        pBuffer.writeVarInt(this.globalIndex);
        pBuffer.writeUUID(this.sender);
        pBuffer.writeVarInt(this.index);
        pBuffer.writeNullable(this.signature, MessageSignature::write);
        this.body.write(pBuffer);
        FriendlyByteBuf.writeNullable(pBuffer, this.unsignedContent, ComponentSerialization.TRUSTED_STREAM_CODEC);
        FilterMask.write(pBuffer, this.filterMask);
        ChatType.Bound.STREAM_CODEC.encode(pBuffer, this.chatType);
    }

    @Override
    public PacketType<ClientboundPlayerChatPacket> type() {
        return GamePacketTypes.CLIENTBOUND_PLAYER_CHAT;
    }

    public void handle(ClientGamePacketListener p_237759_) {
        p_237759_.handlePlayerChat(this);
    }

    @Override
    public boolean isSkippable() {
        return true;
    }
}