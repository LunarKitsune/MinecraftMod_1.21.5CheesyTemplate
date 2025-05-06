package net.minecraft.network.protocol.game;

import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

public class ClientboundUpdateMobEffectPacket implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundUpdateMobEffectPacket> STREAM_CODEC = Packet.codec(
        ClientboundUpdateMobEffectPacket::write, ClientboundUpdateMobEffectPacket::new
    );
    private static final int FLAG_AMBIENT = 1;
    private static final int FLAG_VISIBLE = 2;
    private static final int FLAG_SHOW_ICON = 4;
    private static final int FLAG_BLEND = 8;
    private final int entityId;
    private final Holder<MobEffect> effect;
    private final int effectAmplifier;
    private final int effectDurationTicks;
    private final byte flags;

    public ClientboundUpdateMobEffectPacket(int pEntityId, MobEffectInstance pEffect, boolean pBlend) {
        this.entityId = pEntityId;
        this.effect = pEffect.getEffect();
        this.effectAmplifier = pEffect.getAmplifier();
        this.effectDurationTicks = pEffect.getDuration();
        byte b0 = 0;
        if (pEffect.isAmbient()) {
            b0 = (byte)(b0 | 1);
        }

        if (pEffect.isVisible()) {
            b0 = (byte)(b0 | 2);
        }

        if (pEffect.showIcon()) {
            b0 = (byte)(b0 | 4);
        }

        if (pBlend) {
            b0 = (byte)(b0 | 8);
        }

        this.flags = b0;
    }

    private ClientboundUpdateMobEffectPacket(RegistryFriendlyByteBuf pBuffer) {
        this.entityId = pBuffer.readVarInt();
        this.effect = MobEffect.STREAM_CODEC.decode(pBuffer);
        this.effectAmplifier = pBuffer.readVarInt();
        this.effectDurationTicks = pBuffer.readVarInt();
        this.flags = pBuffer.readByte();
    }

    private void write(RegistryFriendlyByteBuf pBuffer) {
        pBuffer.writeVarInt(this.entityId);
        MobEffect.STREAM_CODEC.encode(pBuffer, this.effect);
        pBuffer.writeVarInt(this.effectAmplifier);
        pBuffer.writeVarInt(this.effectDurationTicks);
        pBuffer.writeByte(this.flags);
    }

    @Override
    public PacketType<ClientboundUpdateMobEffectPacket> type() {
        return GamePacketTypes.CLIENTBOUND_UPDATE_MOB_EFFECT;
    }

    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleUpdateMobEffect(this);
    }

    public int getEntityId() {
        return this.entityId;
    }

    public Holder<MobEffect> getEffect() {
        return this.effect;
    }

    public int getEffectAmplifier() {
        return this.effectAmplifier;
    }

    public int getEffectDurationTicks() {
        return this.effectDurationTicks;
    }

    public boolean isEffectVisible() {
        return (this.flags & 2) != 0;
    }

    public boolean isEffectAmbient() {
        return (this.flags & 1) != 0;
    }

    public boolean effectShowsIcon() {
        return (this.flags & 4) != 0;
    }

    public boolean shouldBlend() {
        return (this.flags & 8) != 0;
    }
}