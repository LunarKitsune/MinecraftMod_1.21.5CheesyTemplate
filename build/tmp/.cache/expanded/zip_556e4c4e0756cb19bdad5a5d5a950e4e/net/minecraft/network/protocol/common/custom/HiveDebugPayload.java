package net.minecraft.network.protocol.common.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record HiveDebugPayload(HiveDebugPayload.HiveInfo hiveInfo) implements CustomPacketPayload {
    public static final StreamCodec<FriendlyByteBuf, HiveDebugPayload> STREAM_CODEC = CustomPacketPayload.codec(
        HiveDebugPayload::write, HiveDebugPayload::new
    );
    public static final CustomPacketPayload.Type<HiveDebugPayload> TYPE = CustomPacketPayload.createType("debug/hive");

    private HiveDebugPayload(FriendlyByteBuf pBuffer) {
        this(new HiveDebugPayload.HiveInfo(pBuffer));
    }

    private void write(FriendlyByteBuf pBuffer) {
        this.hiveInfo.write(pBuffer);
    }

    @Override
    public CustomPacketPayload.Type<HiveDebugPayload> type() {
        return TYPE;
    }

    public record HiveInfo(BlockPos pos, String hiveType, int occupantCount, int honeyLevel, boolean sedated) {
        public HiveInfo(FriendlyByteBuf pBuffer) {
            this(pBuffer.readBlockPos(), pBuffer.readUtf(), pBuffer.readInt(), pBuffer.readInt(), pBuffer.readBoolean());
        }

        public void write(FriendlyByteBuf pBuffer) {
            pBuffer.writeBlockPos(this.pos);
            pBuffer.writeUtf(this.hiveType);
            pBuffer.writeInt(this.occupantCount);
            pBuffer.writeInt(this.honeyLevel);
            pBuffer.writeBoolean(this.sedated);
        }
    }
}