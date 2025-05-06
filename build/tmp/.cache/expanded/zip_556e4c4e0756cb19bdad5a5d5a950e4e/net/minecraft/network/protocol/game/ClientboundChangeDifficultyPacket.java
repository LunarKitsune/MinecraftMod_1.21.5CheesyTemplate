package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.Difficulty;

public class ClientboundChangeDifficultyPacket implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ClientboundChangeDifficultyPacket> STREAM_CODEC = Packet.codec(
        ClientboundChangeDifficultyPacket::write, ClientboundChangeDifficultyPacket::new
    );
    private final Difficulty difficulty;
    private final boolean locked;

    public ClientboundChangeDifficultyPacket(Difficulty pDifficulty, boolean pLocked) {
        this.difficulty = pDifficulty;
        this.locked = pLocked;
    }

    private ClientboundChangeDifficultyPacket(FriendlyByteBuf pBuffer) {
        this.difficulty = Difficulty.byId(pBuffer.readUnsignedByte());
        this.locked = pBuffer.readBoolean();
    }

    private void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeByte(this.difficulty.getId());
        pBuffer.writeBoolean(this.locked);
    }

    @Override
    public PacketType<ClientboundChangeDifficultyPacket> type() {
        return GamePacketTypes.CLIENTBOUND_CHANGE_DIFFICULTY;
    }

    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleChangeDifficulty(this);
    }

    public boolean isLocked() {
        return this.locked;
    }

    public Difficulty getDifficulty() {
        return this.difficulty;
    }
}