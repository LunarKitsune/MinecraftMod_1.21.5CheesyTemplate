package net.minecraft.network.protocol.game;

import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;

public class ClientboundSoundEntityPacket implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundSoundEntityPacket> STREAM_CODEC = Packet.codec(
        ClientboundSoundEntityPacket::write, ClientboundSoundEntityPacket::new
    );
    private final Holder<SoundEvent> sound;
    private final SoundSource source;
    private final int id;
    private final float volume;
    private final float pitch;
    private final long seed;

    public ClientboundSoundEntityPacket(Holder<SoundEvent> pSound, SoundSource pSource, Entity pEntity, float pVolume, float pPitch, long pSeed) {
        this.sound = pSound;
        this.source = pSource;
        this.id = pEntity.getId();
        this.volume = pVolume;
        this.pitch = pPitch;
        this.seed = pSeed;
    }

    private ClientboundSoundEntityPacket(RegistryFriendlyByteBuf pBuffer) {
        this.sound = SoundEvent.STREAM_CODEC.decode(pBuffer);
        this.source = pBuffer.readEnum(SoundSource.class);
        this.id = pBuffer.readVarInt();
        this.volume = pBuffer.readFloat();
        this.pitch = pBuffer.readFloat();
        this.seed = pBuffer.readLong();
    }

    private void write(RegistryFriendlyByteBuf pBuffer) {
        SoundEvent.STREAM_CODEC.encode(pBuffer, this.sound);
        pBuffer.writeEnum(this.source);
        pBuffer.writeVarInt(this.id);
        pBuffer.writeFloat(this.volume);
        pBuffer.writeFloat(this.pitch);
        pBuffer.writeLong(this.seed);
    }

    @Override
    public PacketType<ClientboundSoundEntityPacket> type() {
        return GamePacketTypes.CLIENTBOUND_SOUND_ENTITY;
    }

    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleSoundEntityEvent(this);
    }

    public Holder<SoundEvent> getSound() {
        return this.sound;
    }

    public SoundSource getSource() {
        return this.source;
    }

    public int getId() {
        return this.id;
    }

    public float getVolume() {
        return this.volume;
    }

    public float getPitch() {
        return this.pitch;
    }

    public long getSeed() {
        return this.seed;
    }
}