package net.minecraft.network.protocol.game;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;

public class ServerboundJigsawGeneratePacket implements Packet<ServerGamePacketListener> {
    public static final StreamCodec<FriendlyByteBuf, ServerboundJigsawGeneratePacket> STREAM_CODEC = Packet.codec(
        ServerboundJigsawGeneratePacket::write, ServerboundJigsawGeneratePacket::new
    );
    private final BlockPos pos;
    private final int levels;
    private final boolean keepJigsaws;

    public ServerboundJigsawGeneratePacket(BlockPos pPos, int pLevels, boolean pKeepJigsaws) {
        this.pos = pPos;
        this.levels = pLevels;
        this.keepJigsaws = pKeepJigsaws;
    }

    private ServerboundJigsawGeneratePacket(FriendlyByteBuf pBuffer) {
        this.pos = pBuffer.readBlockPos();
        this.levels = pBuffer.readVarInt();
        this.keepJigsaws = pBuffer.readBoolean();
    }

    private void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeBlockPos(this.pos);
        pBuffer.writeVarInt(this.levels);
        pBuffer.writeBoolean(this.keepJigsaws);
    }

    @Override
    public PacketType<ServerboundJigsawGeneratePacket> type() {
        return GamePacketTypes.SERVERBOUND_JIGSAW_GENERATE;
    }

    public void handle(ServerGamePacketListener pHandler) {
        pHandler.handleJigsawGenerate(this);
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public int levels() {
        return this.levels;
    }

    public boolean keepJigsaws() {
        return this.keepJigsaws;
    }
}