package net.minecraft.network.protocol.game;

import javax.annotation.Nullable;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;

public class ClientboundStopSoundPacket implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ClientboundStopSoundPacket> STREAM_CODEC = Packet.codec(
        ClientboundStopSoundPacket::write, ClientboundStopSoundPacket::new
    );
    private static final int HAS_SOURCE = 1;
    private static final int HAS_SOUND = 2;
    @Nullable
    private final ResourceLocation name;
    @Nullable
    private final SoundSource source;

    public ClientboundStopSoundPacket(@Nullable ResourceLocation pName, @Nullable SoundSource pSource) {
        this.name = pName;
        this.source = pSource;
    }

    private ClientboundStopSoundPacket(FriendlyByteBuf pBuffer) {
        int i = pBuffer.readByte();
        if ((i & 1) > 0) {
            this.source = pBuffer.readEnum(SoundSource.class);
        } else {
            this.source = null;
        }

        if ((i & 2) > 0) {
            this.name = pBuffer.readResourceLocation();
        } else {
            this.name = null;
        }
    }

    private void write(FriendlyByteBuf pBuffer) {
        if (this.source != null) {
            if (this.name != null) {
                pBuffer.writeByte(3);
                pBuffer.writeEnum(this.source);
                pBuffer.writeResourceLocation(this.name);
            } else {
                pBuffer.writeByte(1);
                pBuffer.writeEnum(this.source);
            }
        } else if (this.name != null) {
            pBuffer.writeByte(2);
            pBuffer.writeResourceLocation(this.name);
        } else {
            pBuffer.writeByte(0);
        }
    }

    @Override
    public PacketType<ClientboundStopSoundPacket> type() {
        return GamePacketTypes.CLIENTBOUND_STOP_SOUND;
    }

    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleStopSoundEvent(this);
    }

    @Nullable
    public ResourceLocation getName() {
        return this.name;
    }

    @Nullable
    public SoundSource getSource() {
        return this.source;
    }
}