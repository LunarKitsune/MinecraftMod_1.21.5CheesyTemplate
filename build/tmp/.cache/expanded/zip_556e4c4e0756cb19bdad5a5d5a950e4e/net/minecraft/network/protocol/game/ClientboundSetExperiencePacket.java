package net.minecraft.network.protocol.game;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ClientboundSetExperiencePacket implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ClientboundSetExperiencePacket> STREAM_CODEC = Packet.codec(
        ClientboundSetExperiencePacket::write, ClientboundSetExperiencePacket::new
    );
    private final float experienceProgress;
    private final int totalExperience;
    private final int experienceLevel;

    public ClientboundSetExperiencePacket(float pExperienceProgress, int pTotalExperience, int pExperienceLevel) {
        this.experienceProgress = pExperienceProgress;
        this.totalExperience = pTotalExperience;
        this.experienceLevel = pExperienceLevel;
    }

    private ClientboundSetExperiencePacket(FriendlyByteBuf pBuffer) {
        this.experienceProgress = pBuffer.readFloat();
        this.experienceLevel = pBuffer.readVarInt();
        this.totalExperience = pBuffer.readVarInt();
    }

    private void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeFloat(this.experienceProgress);
        pBuffer.writeVarInt(this.experienceLevel);
        pBuffer.writeVarInt(this.totalExperience);
    }

    @Override
    public PacketType<ClientboundSetExperiencePacket> type() {
        return GamePacketTypes.CLIENTBOUND_SET_EXPERIENCE;
    }

    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleSetExperience(this);
    }

    public float getExperienceProgress() {
        return this.experienceProgress;
    }

    public int getTotalExperience() {
        return this.totalExperience;
    }

    public int getExperienceLevel() {
        return this.experienceLevel;
    }
}