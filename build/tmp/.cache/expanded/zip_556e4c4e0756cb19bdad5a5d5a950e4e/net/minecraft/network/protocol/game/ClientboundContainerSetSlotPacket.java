package net.minecraft.network.protocol.game;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.item.ItemStack;

public class ClientboundContainerSetSlotPacket implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundContainerSetSlotPacket> STREAM_CODEC = Packet.codec(
        ClientboundContainerSetSlotPacket::write, ClientboundContainerSetSlotPacket::new
    );
    private final int containerId;
    private final int stateId;
    private final int slot;
    private final ItemStack itemStack;

    public ClientboundContainerSetSlotPacket(int pContainerId, int pStateId, int pSlot, ItemStack pItemStack) {
        this.containerId = pContainerId;
        this.stateId = pStateId;
        this.slot = pSlot;
        this.itemStack = pItemStack.copy();
    }

    private ClientboundContainerSetSlotPacket(RegistryFriendlyByteBuf pBuffer) {
        this.containerId = pBuffer.readContainerId();
        this.stateId = pBuffer.readVarInt();
        this.slot = pBuffer.readShort();
        this.itemStack = ItemStack.OPTIONAL_STREAM_CODEC.decode(pBuffer);
    }

    private void write(RegistryFriendlyByteBuf pBuffer) {
        pBuffer.writeContainerId(this.containerId);
        pBuffer.writeVarInt(this.stateId);
        pBuffer.writeShort(this.slot);
        ItemStack.OPTIONAL_STREAM_CODEC.encode(pBuffer, this.itemStack);
    }

    @Override
    public PacketType<ClientboundContainerSetSlotPacket> type() {
        return GamePacketTypes.CLIENTBOUND_CONTAINER_SET_SLOT;
    }

    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleContainerSetSlot(this);
    }

    public int getContainerId() {
        return this.containerId;
    }

    public int getSlot() {
        return this.slot;
    }

    public ItemStack getItem() {
        return this.itemStack;
    }

    public int getStateId() {
        return this.stateId;
    }
}