package net.minecraft.network.protocol.game;

import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

public class ClientboundSoundPacket implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundSoundPacket> STREAM_CODEC = Packet.codec(
        ClientboundSoundPacket::write, ClientboundSoundPacket::new
    );
    public static final float LOCATION_ACCURACY = 8.0F;
    private final Holder<SoundEvent> sound;
    private final SoundSource source;
    private final int x;
    private final int y;
    private final int z;
    private final float volume;
    private final float pitch;
    private final long seed;

    public ClientboundSoundPacket(
        Holder<SoundEvent> pSound,
        SoundSource pSource,
        double pX,
        double pY,
        double pZ,
        float pVolume,
        float pPitch,
        long pSeed
    ) {
        this.sound = pSound;
        this.source = pSource;
        this.x = (int)(pX * 8.0);
        this.y = (int)(pY * 8.0);
        this.z = (int)(pZ * 8.0);
        this.volume = pVolume;
        this.pitch = pPitch;
        this.seed = pSeed;
    }

    private ClientboundSoundPacket(RegistryFriendlyByteBuf pBuffer) {
        this.sound = SoundEvent.STREAM_CODEC.decode(pBuffer);
        this.source = pBuffer.readEnum(SoundSource.class);
        this.x = pBuffer.readInt();
        this.y = pBuffer.readInt();
        this.z = pBuffer.readInt();
        this.volume = pBuffer.readFloat();
        this.pitch = pBuffer.readFloat();
        this.seed = pBuffer.readLong();
    }

    private void write(RegistryFriendlyByteBuf pBuffer) {
        SoundEvent.STREAM_CODEC.encode(pBuffer, this.sound);
        pBuffer.writeEnum(this.source);
        pBuffer.writeInt(this.x);
        pBuffer.writeInt(this.y);
        pBuffer.writeInt(this.z);
        pBuffer.writeFloat(this.volume);
        pBuffer.writeFloat(this.pitch);
        pBuffer.writeLong(this.seed);
    }

    @Override
    public PacketType<ClientboundSoundPacket> type() {
        return GamePacketTypes.CLIENTBOUND_SOUND;
    }

    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleSoundEvent(this);
    }

    public Holder<SoundEvent> getSound() {
        return this.sound;
    }

    public SoundSource getSource() {
        return this.source;
    }

    public double getX() {
        return this.x / 8.0F;
    }

    public double getY() {
        return this.y / 8.0F;
    }

    public double getZ() {
        return this.z / 8.0F;
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