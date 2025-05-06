package net.minecraft.network.protocol.game;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.item.trading.MerchantOffers;

public class ClientboundMerchantOffersPacket implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundMerchantOffersPacket> STREAM_CODEC = Packet.codec(
        ClientboundMerchantOffersPacket::write, ClientboundMerchantOffersPacket::new
    );
    private final int containerId;
    private final MerchantOffers offers;
    private final int villagerLevel;
    private final int villagerXp;
    private final boolean showProgress;
    private final boolean canRestock;

    public ClientboundMerchantOffersPacket(int pContainerId, MerchantOffers pOffers, int pVillagerLevel, int pVillagerXp, boolean pShowProgress, boolean pCanRestock) {
        this.containerId = pContainerId;
        this.offers = pOffers.copy();
        this.villagerLevel = pVillagerLevel;
        this.villagerXp = pVillagerXp;
        this.showProgress = pShowProgress;
        this.canRestock = pCanRestock;
    }

    private ClientboundMerchantOffersPacket(RegistryFriendlyByteBuf pBuffer) {
        this.containerId = pBuffer.readContainerId();
        this.offers = MerchantOffers.STREAM_CODEC.decode(pBuffer);
        this.villagerLevel = pBuffer.readVarInt();
        this.villagerXp = pBuffer.readVarInt();
        this.showProgress = pBuffer.readBoolean();
        this.canRestock = pBuffer.readBoolean();
    }

    private void write(RegistryFriendlyByteBuf pBuffer) {
        pBuffer.writeContainerId(this.containerId);
        MerchantOffers.STREAM_CODEC.encode(pBuffer, this.offers);
        pBuffer.writeVarInt(this.villagerLevel);
        pBuffer.writeVarInt(this.villagerXp);
        pBuffer.writeBoolean(this.showProgress);
        pBuffer.writeBoolean(this.canRestock);
    }

    @Override
    public PacketType<ClientboundMerchantOffersPacket> type() {
        return GamePacketTypes.CLIENTBOUND_MERCHANT_OFFERS;
    }

    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleMerchantOffers(this);
    }

    public int getContainerId() {
        return this.containerId;
    }

    public MerchantOffers getOffers() {
        return this.offers;
    }

    public int getVillagerLevel() {
        return this.villagerLevel;
    }

    public int getVillagerXp() {
        return this.villagerXp;
    }

    public boolean showProgress() {
        return this.showProgress;
    }

    public boolean canRestock() {
        return this.canRestock;
    }
}