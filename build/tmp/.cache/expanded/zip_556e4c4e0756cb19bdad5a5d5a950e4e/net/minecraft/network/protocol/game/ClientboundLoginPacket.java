package net.minecraft.network.protocol.game;

import com.google.common.collect.Sets;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public record ClientboundLoginPacket(
    int playerId,
    boolean hardcore,
    Set<ResourceKey<Level>> levels,
    int maxPlayers,
    int chunkRadius,
    int simulationDistance,
    boolean reducedDebugInfo,
    boolean showDeathScreen,
    boolean doLimitedCrafting,
    CommonPlayerSpawnInfo commonPlayerSpawnInfo,
    boolean enforcesSecureChat
) implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundLoginPacket> STREAM_CODEC = Packet.codec(
        ClientboundLoginPacket::write, ClientboundLoginPacket::new
    );

    private ClientboundLoginPacket(RegistryFriendlyByteBuf pBuffer) {
        this(
            pBuffer.readInt(),
            pBuffer.readBoolean(),
            pBuffer.readCollection(Sets::newHashSetWithExpectedSize, p_258210_ -> p_258210_.readResourceKey(Registries.DIMENSION)),
            pBuffer.readVarInt(),
            pBuffer.readVarInt(),
            pBuffer.readVarInt(),
            pBuffer.readBoolean(),
            pBuffer.readBoolean(),
            pBuffer.readBoolean(),
            new CommonPlayerSpawnInfo(pBuffer),
            pBuffer.readBoolean()
        );
    }

    private void write(RegistryFriendlyByteBuf pBuffer) {
        pBuffer.writeInt(this.playerId);
        pBuffer.writeBoolean(this.hardcore);
        pBuffer.writeCollection(this.levels, FriendlyByteBuf::writeResourceKey);
        pBuffer.writeVarInt(this.maxPlayers);
        pBuffer.writeVarInt(this.chunkRadius);
        pBuffer.writeVarInt(this.simulationDistance);
        pBuffer.writeBoolean(this.reducedDebugInfo);
        pBuffer.writeBoolean(this.showDeathScreen);
        pBuffer.writeBoolean(this.doLimitedCrafting);
        this.commonPlayerSpawnInfo.write(pBuffer);
        pBuffer.writeBoolean(this.enforcesSecureChat);
    }

    @Override
    public PacketType<ClientboundLoginPacket> type() {
        return GamePacketTypes.CLIENTBOUND_LOGIN;
    }

    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleLogin(this);
    }
}