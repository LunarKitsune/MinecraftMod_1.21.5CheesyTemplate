package net.minecraft.world.level.levelgen;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.BitStorage;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.Mth;
import net.minecraft.util.SimpleBitStorage;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.slf4j.Logger;

public class Heightmap {
    private static final Logger LOGGER = LogUtils.getLogger();
    static final Predicate<BlockState> NOT_AIR = p_360584_ -> !p_360584_.isAir();
    static final Predicate<BlockState> MATERIAL_MOTION_BLOCKING = BlockBehaviour.BlockStateBase::blocksMotion;
    private final BitStorage data;
    private final Predicate<BlockState> isOpaque;
    private final ChunkAccess chunk;

    public Heightmap(ChunkAccess pChunk, Heightmap.Types pType) {
        this.isOpaque = pType.isOpaque();
        this.chunk = pChunk;
        int i = Mth.ceillog2(pChunk.getHeight() + 1);
        this.data = new SimpleBitStorage(i, 256);
    }

    public static void primeHeightmaps(ChunkAccess pChunk, Set<Heightmap.Types> pTypes) {
        if (!pTypes.isEmpty()) {
            int i = pTypes.size();
            ObjectList<Heightmap> objectlist = new ObjectArrayList<>(i);
            ObjectListIterator<Heightmap> objectlistiterator = objectlist.iterator();
            int j = pChunk.getHighestSectionPosition() + 16;
            BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

            for (int k = 0; k < 16; k++) {
                for (int l = 0; l < 16; l++) {
                    for (Heightmap.Types heightmap$types : pTypes) {
                        objectlist.add(pChunk.getOrCreateHeightmapUnprimed(heightmap$types));
                    }

                    for (int i1 = j - 1; i1 >= pChunk.getMinY(); i1--) {
                        blockpos$mutableblockpos.set(k, i1, l);
                        BlockState blockstate = pChunk.getBlockState(blockpos$mutableblockpos);
                        if (!blockstate.is(Blocks.AIR)) {
                            while (objectlistiterator.hasNext()) {
                                Heightmap heightmap = objectlistiterator.next();
                                if (heightmap.isOpaque.test(blockstate)) {
                                    heightmap.setHeight(k, l, i1 + 1);
                                    objectlistiterator.remove();
                                }
                            }

                            if (objectlist.isEmpty()) {
                                break;
                            }

                            objectlistiterator.back(i);
                        }
                    }
                }
            }
        }
    }

    public boolean update(int pX, int pY, int pZ, BlockState pState) {
        int i = this.getFirstAvailable(pX, pZ);
        if (pY <= i - 2) {
            return false;
        } else {
            if (this.isOpaque.test(pState)) {
                if (pY >= i) {
                    this.setHeight(pX, pZ, pY + 1);
                    return true;
                }
            } else if (i - 1 == pY) {
                BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

                for (int j = pY - 1; j >= this.chunk.getMinY(); j--) {
                    blockpos$mutableblockpos.set(pX, j, pZ);
                    if (this.isOpaque.test(this.chunk.getBlockState(blockpos$mutableblockpos))) {
                        this.setHeight(pX, pZ, j + 1);
                        return true;
                    }
                }

                this.setHeight(pX, pZ, this.chunk.getMinY());
                return true;
            }

            return false;
        }
    }

    public int getFirstAvailable(int pX, int pZ) {
        return this.getFirstAvailable(getIndex(pX, pZ));
    }

    public int getHighestTaken(int pX, int pZ) {
        return this.getFirstAvailable(getIndex(pX, pZ)) - 1;
    }

    private int getFirstAvailable(int pIndex) {
        return this.data.get(pIndex) + this.chunk.getMinY();
    }

    private void setHeight(int pX, int pZ, int pValue) {
        this.data.set(getIndex(pX, pZ), pValue - this.chunk.getMinY());
    }

    public void setRawData(ChunkAccess pChunk, Heightmap.Types pType, long[] pData) {
        long[] along = this.data.getRaw();
        if (along.length == pData.length) {
            System.arraycopy(pData, 0, along, 0, pData.length);
        } else {
            LOGGER.warn(
                "Ignoring heightmap data for chunk " + pChunk.getPos() + ", size does not match; expected: " + along.length + ", got: " + pData.length
            );
            primeHeightmaps(pChunk, EnumSet.of(pType));
        }
    }

    public long[] getRawData() {
        return this.data.getRaw();
    }

    private static int getIndex(int pX, int pZ) {
        return pX + pZ * 16;
    }

    public static enum Types implements StringRepresentable {
        WORLD_SURFACE_WG(0, "WORLD_SURFACE_WG", Heightmap.Usage.WORLDGEN, Heightmap.NOT_AIR),
        WORLD_SURFACE(1, "WORLD_SURFACE", Heightmap.Usage.CLIENT, Heightmap.NOT_AIR),
        OCEAN_FLOOR_WG(2, "OCEAN_FLOOR_WG", Heightmap.Usage.WORLDGEN, Heightmap.MATERIAL_MOTION_BLOCKING),
        OCEAN_FLOOR(3, "OCEAN_FLOOR", Heightmap.Usage.LIVE_WORLD, Heightmap.MATERIAL_MOTION_BLOCKING),
        MOTION_BLOCKING(4, "MOTION_BLOCKING", Heightmap.Usage.CLIENT, p_360586_ -> p_360586_.blocksMotion() || !p_360586_.getFluidState().isEmpty()),
        MOTION_BLOCKING_NO_LEAVES(
            5,
            "MOTION_BLOCKING_NO_LEAVES",
            Heightmap.Usage.CLIENT,
            p_360585_ -> (p_360585_.blocksMotion() || !p_360585_.getFluidState().isEmpty()) && !(p_360585_.getBlock() instanceof LeavesBlock)
        );

        public static final Codec<Heightmap.Types> CODEC = StringRepresentable.fromEnum(Heightmap.Types::values);
        private static final IntFunction<Heightmap.Types> BY_ID = ByIdMap.continuous(
            p_391036_ -> p_391036_.id, values(), ByIdMap.OutOfBoundsStrategy.ZERO
        );
        public static final StreamCodec<ByteBuf, Heightmap.Types> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, p_391037_ -> p_391037_.id);
        private final int id;
        private final String serializationKey;
        private final Heightmap.Usage usage;
        private final Predicate<BlockState> isOpaque;

        private Types(final int pId, final String pSerializationKey, final Heightmap.Usage pUsage, final Predicate<BlockState> pIsOpaque) {
            this.id = pId;
            this.serializationKey = pSerializationKey;
            this.usage = pUsage;
            this.isOpaque = pIsOpaque;
        }

        public String getSerializationKey() {
            return this.serializationKey;
        }

        public boolean sendToClient() {
            return this.usage == Heightmap.Usage.CLIENT;
        }

        public boolean keepAfterWorldgen() {
            return this.usage != Heightmap.Usage.WORLDGEN;
        }

        public Predicate<BlockState> isOpaque() {
            return this.isOpaque;
        }

        @Override
        public String getSerializedName() {
            return this.serializationKey;
        }
    }

    public static enum Usage {
        WORLDGEN,
        LIVE_WORLD,
        CLIENT;
    }
}