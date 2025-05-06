package net.minecraft.network.protocol.common.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record WorldGenAttemptDebugPayload(BlockPos pos, float scale, float red, float green, float blue, float alpha)
    implements CustomPacketPayload {
    public static final StreamCodec<FriendlyByteBuf, WorldGenAttemptDebugPayload> STREAM_CODEC = CustomPacketPayload.codec(
        WorldGenAttemptDebugPayload::write, WorldGenAttemptDebugPayload::new
    );
    public static final CustomPacketPayload.Type<WorldGenAttemptDebugPayload> TYPE = CustomPacketPayload.createType("debug/worldgen_attempt");

    private WorldGenAttemptDebugPayload(FriendlyByteBuf pBuffer) {
        this(pBuffer.readBlockPos(), pBuffer.readFloat(), pBuffer.readFloat(), pBuffer.readFloat(), pBuffer.readFloat(), pBuffer.readFloat());
    }

    private void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeBlockPos(this.pos);
        pBuffer.writeFloat(this.scale);
        pBuffer.writeFloat(this.red);
        pBuffer.writeFloat(this.green);
        pBuffer.writeFloat(this.blue);
        pBuffer.writeFloat(this.alpha);
    }

    @Override
    public CustomPacketPayload.Type<WorldGenAttemptDebugPayload> type() {
        return TYPE;
    }
}