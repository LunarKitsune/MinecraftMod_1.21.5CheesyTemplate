package net.minecraft.network.protocol.game;

import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;

public class ClientboundLevelChunkPacketData {
    private static final StreamCodec<ByteBuf, Map<Heightmap.Types, long[]>> HEIGHTMAPS_STREAM_CODEC = ByteBufCodecs.map(
        p_389934_ -> new EnumMap<>(Heightmap.Types.class), Heightmap.Types.STREAM_CODEC, ByteBufCodecs.LONG_ARRAY
    );
    private static final int TWO_MEGABYTES = 2097152;
    private final Map<Heightmap.Types, long[]> heightmaps;
    private final byte[] buffer;
    private final List<ClientboundLevelChunkPacketData.BlockEntityInfo> blockEntitiesData;

    public ClientboundLevelChunkPacketData(LevelChunk pLevelChunk) {
        this.heightmaps = pLevelChunk.getHeightmaps()
            .stream()
            .filter(p_389936_ -> p_389936_.getKey().sendToClient())
            .collect(Collectors.toMap(Entry::getKey, p_389935_ -> (long[])p_389935_.getValue().getRawData().clone()));
        this.buffer = new byte[calculateChunkSize(pLevelChunk)];
        extractChunkData(new FriendlyByteBuf(this.getWriteBuffer()), pLevelChunk);
        this.blockEntitiesData = Lists.newArrayList();

        for (Entry<BlockPos, BlockEntity> entry : pLevelChunk.getBlockEntities().entrySet()) {
            this.blockEntitiesData.add(ClientboundLevelChunkPacketData.BlockEntityInfo.create(entry.getValue()));
        }
    }

    public ClientboundLevelChunkPacketData(RegistryFriendlyByteBuf pBuffer, int pX, int pZ) {
        this.heightmaps = HEIGHTMAPS_STREAM_CODEC.decode(pBuffer);
        int i = pBuffer.readVarInt();
        if (i > 2097152) {
            throw new RuntimeException("Chunk Packet trying to allocate too much memory on read.");
        } else {
            this.buffer = new byte[i];
            pBuffer.readBytes(this.buffer);
            this.blockEntitiesData = ClientboundLevelChunkPacketData.BlockEntityInfo.LIST_STREAM_CODEC.decode(pBuffer);
        }
    }

    public void write(RegistryFriendlyByteBuf pBuffer) {
        HEIGHTMAPS_STREAM_CODEC.encode(pBuffer, this.heightmaps);
        pBuffer.writeVarInt(this.buffer.length);
        pBuffer.writeBytes(this.buffer);
        ClientboundLevelChunkPacketData.BlockEntityInfo.LIST_STREAM_CODEC.encode(pBuffer, this.blockEntitiesData);
    }

    private static int calculateChunkSize(LevelChunk pChunk) {
        int i = 0;

        for (LevelChunkSection levelchunksection : pChunk.getSections()) {
            i += levelchunksection.getSerializedSize();
        }

        return i;
    }

    private ByteBuf getWriteBuffer() {
        ByteBuf bytebuf = Unpooled.wrappedBuffer(this.buffer);
        bytebuf.writerIndex(0);
        return bytebuf;
    }

    public static void extractChunkData(FriendlyByteBuf pBuffer, LevelChunk pChunk) {
        for (LevelChunkSection levelchunksection : pChunk.getSections()) {
            levelchunksection.write(pBuffer);
        }
    }

    public Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> getBlockEntitiesTagsConsumer(int pChunkX, int pChunkZ) {
        return p_195663_ -> this.getBlockEntitiesTags(p_195663_, pChunkX, pChunkZ);
    }

    private void getBlockEntitiesTags(ClientboundLevelChunkPacketData.BlockEntityTagOutput pOutput, int pChunkX, int pChunkZ) {
        int i = 16 * pChunkX;
        int j = 16 * pChunkZ;
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

        for (ClientboundLevelChunkPacketData.BlockEntityInfo clientboundlevelchunkpacketdata$blockentityinfo : this.blockEntitiesData) {
            int k = i + SectionPos.sectionRelative(clientboundlevelchunkpacketdata$blockentityinfo.packedXZ >> 4);
            int l = j + SectionPos.sectionRelative(clientboundlevelchunkpacketdata$blockentityinfo.packedXZ);
            blockpos$mutableblockpos.set(k, clientboundlevelchunkpacketdata$blockentityinfo.y, l);
            pOutput.accept(
                blockpos$mutableblockpos, clientboundlevelchunkpacketdata$blockentityinfo.type, clientboundlevelchunkpacketdata$blockentityinfo.tag
            );
        }
    }

    public FriendlyByteBuf getReadBuffer() {
        return new FriendlyByteBuf(Unpooled.wrappedBuffer(this.buffer));
    }

    public Map<Heightmap.Types, long[]> getHeightmaps() {
        return this.heightmaps;
    }

    static class BlockEntityInfo {
        public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundLevelChunkPacketData.BlockEntityInfo> STREAM_CODEC = StreamCodec.ofMember(
            ClientboundLevelChunkPacketData.BlockEntityInfo::write, ClientboundLevelChunkPacketData.BlockEntityInfo::new
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, List<ClientboundLevelChunkPacketData.BlockEntityInfo>> LIST_STREAM_CODEC = STREAM_CODEC.apply(
            ByteBufCodecs.list()
        );
        final int packedXZ;
        final int y;
        final BlockEntityType<?> type;
        @Nullable
        final CompoundTag tag;

        private BlockEntityInfo(int pPackedXZ, int pY, BlockEntityType<?> pType, @Nullable CompoundTag pTag) {
            this.packedXZ = pPackedXZ;
            this.y = pY;
            this.type = pType;
            this.tag = pTag;
        }

        private BlockEntityInfo(RegistryFriendlyByteBuf pBuffer) {
            this.packedXZ = pBuffer.readByte();
            this.y = pBuffer.readShort();
            this.type = ByteBufCodecs.registry(Registries.BLOCK_ENTITY_TYPE).decode(pBuffer);
            this.tag = pBuffer.readNbt();
        }

        private void write(RegistryFriendlyByteBuf pBuffer) {
            pBuffer.writeByte(this.packedXZ);
            pBuffer.writeShort(this.y);
            ByteBufCodecs.registry(Registries.BLOCK_ENTITY_TYPE).encode(pBuffer, this.type);
            pBuffer.writeNbt(this.tag);
        }

        static ClientboundLevelChunkPacketData.BlockEntityInfo create(BlockEntity pBlockEntity) {
            CompoundTag compoundtag = pBlockEntity.getUpdateTag(pBlockEntity.getLevel().registryAccess());
            BlockPos blockpos = pBlockEntity.getBlockPos();
            int i = SectionPos.sectionRelative(blockpos.getX()) << 4 | SectionPos.sectionRelative(blockpos.getZ());
            return new ClientboundLevelChunkPacketData.BlockEntityInfo(
                i, blockpos.getY(), pBlockEntity.getType(), compoundtag.isEmpty() ? null : compoundtag
            );
        }
    }

    @FunctionalInterface
    public interface BlockEntityTagOutput {
        void accept(BlockPos pPos, BlockEntityType<?> pType, @Nullable CompoundTag pNbt);
    }
}