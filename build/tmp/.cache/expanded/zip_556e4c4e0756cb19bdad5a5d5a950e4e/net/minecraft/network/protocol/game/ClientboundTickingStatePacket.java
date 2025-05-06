package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.TickRateManager;

public record ClientboundTickingStatePacket(float tickRate, boolean isFrozen) implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ClientboundTickingStatePacket> STREAM_CODEC = Packet.codec(
        ClientboundTickingStatePacket::write, ClientboundTickingStatePacket::new
    );

    private ClientboundTickingStatePacket(FriendlyByteBuf pBuffer) {
        this(pBuffer.readFloat(), pBuffer.readBoolean());
    }

    public static ClientboundTickingStatePacket from(TickRateManager pTickRateManager) {
        return new ClientboundTickingStatePacket(pTickRateManager.tickrate(), pTickRateManager.isFrozen());
    }

    private void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeFloat(this.tickRate);
        pBuffer.writeBoolean(this.isFrozen);
    }

    @Override
    public PacketType<ClientboundTickingStatePacket> type() {
        return GamePacketTypes.CLIENTBOUND_TICKING_STATE;
    }

    public void handle(ClientGamePacketListener p_311641_) {
        p_311641_.handleTickingState(this);
    }
}