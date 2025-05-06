package net.minecraft.network.protocol.game;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.level.block.Block;

/**
 * Triggers a block event on the client.
 * 
 * @see Block#triggerEvent
 * @see Level#blockEvent
 */
public class ClientboundBlockEventPacket implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundBlockEventPacket> STREAM_CODEC = Packet.codec(
        ClientboundBlockEventPacket::write, ClientboundBlockEventPacket::new
    );
    private final BlockPos pos;
    private final int b0;
    private final int b1;
    private final Block block;

    public ClientboundBlockEventPacket(BlockPos pPos, Block pBlock, int pB0, int pB1) {
        this.pos = pPos;
        this.block = pBlock;
        this.b0 = pB0;
        this.b1 = pB1;
    }

    private ClientboundBlockEventPacket(RegistryFriendlyByteBuf pBuffer) {
        this.pos = pBuffer.readBlockPos();
        this.b0 = pBuffer.readUnsignedByte();
        this.b1 = pBuffer.readUnsignedByte();
        this.block = ByteBufCodecs.registry(Registries.BLOCK).decode(pBuffer);
    }

    private void write(RegistryFriendlyByteBuf pBuffer) {
        pBuffer.writeBlockPos(this.pos);
        pBuffer.writeByte(this.b0);
        pBuffer.writeByte(this.b1);
        ByteBufCodecs.registry(Registries.BLOCK).encode(pBuffer, this.block);
    }

    @Override
    public PacketType<ClientboundBlockEventPacket> type() {
        return GamePacketTypes.CLIENTBOUND_BLOCK_EVENT;
    }

    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleBlockEvent(this);
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public int getB0() {
        return this.b0;
    }

    public int getB1() {
        return this.b1;
    }

    public Block getBlock() {
        return this.block;
    }
}