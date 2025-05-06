package net.minecraft.network.protocol.game;

import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.resources.ResourceLocation;

public class ClientboundUpdateAdvancementsPacket implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundUpdateAdvancementsPacket> STREAM_CODEC = Packet.codec(
        ClientboundUpdateAdvancementsPacket::write, ClientboundUpdateAdvancementsPacket::new
    );
    private final boolean reset;
    private final List<AdvancementHolder> added;
    private final Set<ResourceLocation> removed;
    private final Map<ResourceLocation, AdvancementProgress> progress;
    private final boolean showAdvancements;

    public ClientboundUpdateAdvancementsPacket(
        boolean pReset,
        Collection<AdvancementHolder> pAdded,
        Set<ResourceLocation> pRemoved,
        Map<ResourceLocation, AdvancementProgress> pProgress,
        boolean pShowAdvancements
    ) {
        this.reset = pReset;
        this.added = List.copyOf(pAdded);
        this.removed = Set.copyOf(pRemoved);
        this.progress = Map.copyOf(pProgress);
        this.showAdvancements = pShowAdvancements;
    }

    private ClientboundUpdateAdvancementsPacket(RegistryFriendlyByteBuf pBuffer) {
        this.reset = pBuffer.readBoolean();
        this.added = AdvancementHolder.LIST_STREAM_CODEC.decode(pBuffer);
        this.removed = pBuffer.readCollection(Sets::newLinkedHashSetWithExpectedSize, FriendlyByteBuf::readResourceLocation);
        this.progress = pBuffer.readMap(FriendlyByteBuf::readResourceLocation, AdvancementProgress::fromNetwork);
        this.showAdvancements = pBuffer.readBoolean();
    }

    private void write(RegistryFriendlyByteBuf pBuffer) {
        pBuffer.writeBoolean(this.reset);
        AdvancementHolder.LIST_STREAM_CODEC.encode(pBuffer, this.added);
        pBuffer.writeCollection(this.removed, FriendlyByteBuf::writeResourceLocation);
        pBuffer.writeMap(this.progress, FriendlyByteBuf::writeResourceLocation, (p_179444_, p_179445_) -> p_179445_.serializeToNetwork(p_179444_));
        pBuffer.writeBoolean(this.showAdvancements);
    }

    @Override
    public PacketType<ClientboundUpdateAdvancementsPacket> type() {
        return GamePacketTypes.CLIENTBOUND_UPDATE_ADVANCEMENTS;
    }

    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleUpdateAdvancementsPacket(this);
    }

    public List<AdvancementHolder> getAdded() {
        return this.added;
    }

    public Set<ResourceLocation> getRemoved() {
        return this.removed;
    }

    public Map<ResourceLocation, AdvancementProgress> getProgress() {
        return this.progress;
    }

    public boolean shouldReset() {
        return this.reset;
    }

    public boolean shouldShowAdvancements() {
        return this.showAdvancements;
    }
}