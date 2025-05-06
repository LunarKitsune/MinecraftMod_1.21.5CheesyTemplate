package net.minecraft.network.protocol.common.custom;

import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public record StructuresDebugPayload(ResourceKey<Level> dimension, BoundingBox mainBB, List<StructuresDebugPayload.PieceInfo> pieces)
    implements CustomPacketPayload {
    public static final StreamCodec<FriendlyByteBuf, StructuresDebugPayload> STREAM_CODEC = CustomPacketPayload.codec(
        StructuresDebugPayload::write, StructuresDebugPayload::new
    );
    public static final CustomPacketPayload.Type<StructuresDebugPayload> TYPE = CustomPacketPayload.createType("debug/structures");

    private StructuresDebugPayload(FriendlyByteBuf pBuffer) {
        this(pBuffer.readResourceKey(Registries.DIMENSION), readBoundingBox(pBuffer), pBuffer.readList(StructuresDebugPayload.PieceInfo::new));
    }

    private void write(FriendlyByteBuf pBuffer) {
        pBuffer.writeResourceKey(this.dimension);
        writeBoundingBox(pBuffer, this.mainBB);
        pBuffer.writeCollection(this.pieces, (p_300337_, p_299834_) -> p_299834_.write(pBuffer));
    }

    @Override
    public CustomPacketPayload.Type<StructuresDebugPayload> type() {
        return TYPE;
    }

    static BoundingBox readBoundingBox(FriendlyByteBuf pBuffer) {
        return new BoundingBox(pBuffer.readInt(), pBuffer.readInt(), pBuffer.readInt(), pBuffer.readInt(), pBuffer.readInt(), pBuffer.readInt());
    }

    static void writeBoundingBox(FriendlyByteBuf pBuffer, BoundingBox pBoundingBox) {
        pBuffer.writeInt(pBoundingBox.minX());
        pBuffer.writeInt(pBoundingBox.minY());
        pBuffer.writeInt(pBoundingBox.minZ());
        pBuffer.writeInt(pBoundingBox.maxX());
        pBuffer.writeInt(pBoundingBox.maxY());
        pBuffer.writeInt(pBoundingBox.maxZ());
    }

    public record PieceInfo(BoundingBox boundingBox, boolean isStart) {
        public PieceInfo(FriendlyByteBuf pBuffer) {
            this(StructuresDebugPayload.readBoundingBox(pBuffer), pBuffer.readBoolean());
        }

        public void write(FriendlyByteBuf pBuffer) {
            StructuresDebugPayload.writeBoundingBox(pBuffer, this.boundingBox);
            pBuffer.writeBoolean(this.isStart);
        }
    }
}