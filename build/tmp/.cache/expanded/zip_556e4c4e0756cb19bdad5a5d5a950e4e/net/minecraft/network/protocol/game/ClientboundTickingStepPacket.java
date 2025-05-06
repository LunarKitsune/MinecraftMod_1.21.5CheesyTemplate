package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.TickRateManager;

public record ClientboundTickingStepPacket(int tickSteps) implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ClientboundTickingStepPacket> STREAM_CODEC = Packet.codec(
        ClientboundTickingStepPacket::write, ClientboundTickingStepPacket::new
    );

    private ClientboundTickingStepPacket(FriendlyByteBuf pBuffer) {
        this(pBuffer.readVarInt());
    }

    public static ClientboundTickingStepPacket from(TickRateManager pTickRateManager) {
        return new ClientboundTickingStepPacket(pTickRateManager.frozenTicksToRun());
    }

    private void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeVarInt(this.tickSteps);
    }

    @Override
    public PacketType<ClientboundTickingStepPacket> type() {
        return GamePacketTypes.CLIENTBOUND_TICKING_STEP;
    }

    public void handle(ClientGamePacketListener p_309817_) {
        p_309817_.handleTickingStep(this);
    }
}