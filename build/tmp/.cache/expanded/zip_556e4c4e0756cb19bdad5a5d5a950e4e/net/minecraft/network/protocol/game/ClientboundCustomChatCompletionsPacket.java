package net.minecraft.network.protocol.game;

import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public record ClientboundCustomChatCompletionsPacket(ClientboundCustomChatCompletionsPacket.Action action, List<String> entries)
    implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ClientboundCustomChatCompletionsPacket> STREAM_CODEC = Packet.codec(
        ClientboundCustomChatCompletionsPacket::write, ClientboundCustomChatCompletionsPacket::new
    );

    private ClientboundCustomChatCompletionsPacket(FriendlyByteBuf pBuffer) {
        this(pBuffer.readEnum(ClientboundCustomChatCompletionsPacket.Action.class), pBuffer.readList(FriendlyByteBuf::readUtf));
    }

    private void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeEnum(this.action);
        pBuffer.writeCollection(this.entries, FriendlyByteBuf::writeUtf);
    }

    @Override
    public PacketType<ClientboundCustomChatCompletionsPacket> type() {
        return GamePacketTypes.CLIENTBOUND_CUSTOM_CHAT_COMPLETIONS;
    }

    public void handle(ClientGamePacketListener p_240794_) {
        p_240794_.handleCustomChatCompletions(this);
    }

    public static enum Action {
        ADD,
        REMOVE,
        SET;
    }
}