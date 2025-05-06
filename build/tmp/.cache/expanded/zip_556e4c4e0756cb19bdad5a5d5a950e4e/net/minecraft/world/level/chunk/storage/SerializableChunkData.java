package net.minecraft.world.level.chunk.storage;

import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import it.unimi.dsi.fastutil.shorts.ShortList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.Optionull;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.NbtException;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerRO;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.status.ChunkType;
import net.minecraft.world.level.levelgen.BelowZeroRetrogen;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.ProtoChunkTicks;
import net.minecraft.world.ticks.SavedTick;
import org.slf4j.Logger;

public record SerializableChunkData(
    Registry<Biome> biomeRegistry,
    ChunkPos chunkPos,
    int minSectionY,
    long lastUpdateTime,
    long inhabitedTime,
    ChunkStatus chunkStatus,
    @Nullable BlendingData.Packed blendingData,
    @Nullable BelowZeroRetrogen belowZeroRetrogen,
    UpgradeData upgradeData,
    @Nullable long[] carvingMask,
    Map<Heightmap.Types, long[]> heightmaps,
    ChunkAccess.PackedTicks packedTicks,
    ShortList[] postProcessingSections,
    boolean lightCorrect,
    List<SerializableChunkData.SectionData> sectionData,
    List<CompoundTag> entities,
    List<CompoundTag> blockEntities,
    CompoundTag structureData
) {
    private static final Codec<PalettedContainer<BlockState>> BLOCK_STATE_CODEC = PalettedContainer.codecRW(
        Block.BLOCK_STATE_REGISTRY, BlockState.CODEC, PalettedContainer.Strategy.SECTION_STATES, Blocks.AIR.defaultBlockState()
    );
    private static final Codec<List<SavedTick<Block>>> BLOCK_TICKS_CODEC = SavedTick.codec(BuiltInRegistries.BLOCK.byNameCodec()).listOf();
    private static final Codec<List<SavedTick<Fluid>>> FLUID_TICKS_CODEC = SavedTick.codec(BuiltInRegistries.FLUID.byNameCodec()).listOf();
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TAG_UPGRADE_DATA = "UpgradeData";
    private static final String BLOCK_TICKS_TAG = "block_ticks";
    private static final String FLUID_TICKS_TAG = "fluid_ticks";
    public static final String X_POS_TAG = "xPos";
    public static final String Z_POS_TAG = "zPos";
    public static final String HEIGHTMAPS_TAG = "Heightmaps";
    public static final String IS_LIGHT_ON_TAG = "isLightOn";
    public static final String SECTIONS_TAG = "sections";
    public static final String BLOCK_LIGHT_TAG = "BlockLight";
    public static final String SKY_LIGHT_TAG = "SkyLight";

    @Nullable
    public static SerializableChunkData parse(LevelHeightAccessor pLevelHeightAccessor, RegistryAccess pRegistries, CompoundTag pTag) {
        if (pTag.getString("Status").isEmpty()) {
            return null;
        } else {
            ChunkPos chunkpos = new ChunkPos(pTag.getIntOr("xPos", 0), pTag.getIntOr("zPos", 0));
            long i = pTag.getLongOr("LastUpdate", 0L);
            long j = pTag.getLongOr("InhabitedTime", 0L);
            ChunkStatus chunkstatus = pTag.read("Status", ChunkStatus.CODEC).orElse(ChunkStatus.EMPTY);
            UpgradeData upgradedata = pTag.getCompound("UpgradeData").map(p_391014_ -> new UpgradeData(p_391014_, pLevelHeightAccessor)).orElse(UpgradeData.EMPTY);
            boolean flag = pTag.getBooleanOr("isLightOn", false);
            BlendingData.Packed blendingdata$packed = pTag.read("blending_data", BlendingData.Packed.CODEC).orElse(null);
            BelowZeroRetrogen belowzeroretrogen = pTag.read("below_zero_retrogen", BelowZeroRetrogen.CODEC).orElse(null);
            long[] along = pTag.getLongArray("carving_mask").orElse(null);
            Map<Heightmap.Types, long[]> map = new EnumMap<>(Heightmap.Types.class);
            pTag.getCompound("Heightmaps").ifPresent(p_391017_ -> {
                for (Heightmap.Types heightmap$types : chunkstatus.heightmapsAfter()) {
                    p_391017_.getLongArray(heightmap$types.getSerializationKey()).ifPresent(p_391011_ -> map.put(heightmap$types, p_391011_));
                }
            });
            List<SavedTick<Block>> list = SavedTick.filterTickListForChunk(pTag.read("block_ticks", BLOCK_TICKS_CODEC).orElse(List.of()), chunkpos);
            List<SavedTick<Fluid>> list1 = SavedTick.filterTickListForChunk(pTag.read("fluid_ticks", FLUID_TICKS_CODEC).orElse(List.of()), chunkpos);
            ChunkAccess.PackedTicks chunkaccess$packedticks = new ChunkAccess.PackedTicks(list, list1);
            ListTag listtag = pTag.getListOrEmpty("PostProcessing");
            ShortList[] ashortlist = new ShortList[listtag.size()];

            for (int k = 0; k < listtag.size(); k++) {
                ListTag listtag1 = listtag.getListOrEmpty(k);
                ShortList shortlist = new ShortArrayList(listtag1.size());

                for (int l = 0; l < listtag1.size(); l++) {
                    shortlist.add(listtag1.getShortOr(l, (short)0));
                }

                ashortlist[k] = shortlist;
            }

            List<CompoundTag> list3 = pTag.getList("entities").stream().flatMap(ListTag::compoundStream).toList();
            List<CompoundTag> list4 = pTag.getList("block_entities").stream().flatMap(ListTag::compoundStream).toList();
            CompoundTag compoundtag1 = pTag.getCompoundOrEmpty("structures");
            ListTag listtag2 = pTag.getListOrEmpty("sections");
            List<SerializableChunkData.SectionData> list2 = new ArrayList<>(listtag2.size());
            Registry<Biome> registry = pRegistries.lookupOrThrow(Registries.BIOME);
            Codec<PalettedContainerRO<Holder<Biome>>> codec = makeBiomeCodec(registry);

            for (int i1 = 0; i1 < listtag2.size(); i1++) {
                Optional<CompoundTag> optional = listtag2.getCompound(i1);
                if (!optional.isEmpty()) {
                    CompoundTag compoundtag = optional.get();
                    int j1 = compoundtag.getByteOr("Y", (byte)0);
                    LevelChunkSection levelchunksection;
                    if (j1 >= pLevelHeightAccessor.getMinSectionY() && j1 <= pLevelHeightAccessor.getMaxSectionY()) {
                        PalettedContainer<BlockState> palettedcontainer = compoundtag.getCompound("block_states")
                            .map(
                                p_391024_ -> BLOCK_STATE_CODEC.parse(NbtOps.INSTANCE, p_391024_)
                                    .promotePartial(p_362514_ -> logErrors(chunkpos, j1, p_362514_))
                                    .getOrThrow(SerializableChunkData.ChunkReadException::new)
                            )
                            .orElseGet(() -> new PalettedContainer<>(Block.BLOCK_STATE_REGISTRY, Blocks.AIR.defaultBlockState(), PalettedContainer.Strategy.SECTION_STATES));
                        PalettedContainerRO<Holder<Biome>> palettedcontainerro = compoundtag.getCompound("biomes")
                            .map(
                                p_391021_ -> codec.parse(NbtOps.INSTANCE, p_391021_)
                                    .promotePartial(p_362842_ -> logErrors(chunkpos, j1, p_362842_))
                                    .getOrThrow(SerializableChunkData.ChunkReadException::new)
                            )
                            .orElseGet(
                                () -> new PalettedContainer<>(registry.asHolderIdMap(), registry.getOrThrow(Biomes.PLAINS), PalettedContainer.Strategy.SECTION_BIOMES)
                            );
                        levelchunksection = new LevelChunkSection(palettedcontainer, palettedcontainerro);
                    } else {
                        levelchunksection = null;
                    }

                    DataLayer datalayer = compoundtag.getByteArray("BlockLight").map(DataLayer::new).orElse(null);
                    DataLayer datalayer1 = compoundtag.getByteArray("SkyLight").map(DataLayer::new).orElse(null);
                    list2.add(new SerializableChunkData.SectionData(j1, levelchunksection, datalayer, datalayer1));
                }
            }

            return new SerializableChunkData(
                registry,
                chunkpos,
                pLevelHeightAccessor.getMinSectionY(),
                i,
                j,
                chunkstatus,
                blendingdata$packed,
                belowzeroretrogen,
                upgradedata,
                along,
                map,
                chunkaccess$packedticks,
                ashortlist,
                flag,
                list2,
                list3,
                list4,
                compoundtag1
            );
        }
    }

    public ProtoChunk read(ServerLevel pLevel, PoiManager pPoiManager, RegionStorageInfo pRegionStorageInfo, ChunkPos pPos) {
        if (!Objects.equals(pPos, this.chunkPos)) {
            LOGGER.error("Chunk file at {} is in the wrong location; relocating. (Expected {}, got {})", pPos, pPos, this.chunkPos);
            pLevel.getServer().reportMisplacedChunk(this.chunkPos, pPos, pRegionStorageInfo);
        }

        int i = pLevel.getSectionsCount();
        LevelChunkSection[] alevelchunksection = new LevelChunkSection[i];
        boolean flag = pLevel.dimensionType().hasSkyLight();
        ChunkSource chunksource = pLevel.getChunkSource();
        LevelLightEngine levellightengine = chunksource.getLightEngine();
        Registry<Biome> registry = pLevel.registryAccess().lookupOrThrow(Registries.BIOME);
        boolean flag1 = false;

        for (SerializableChunkData.SectionData serializablechunkdata$sectiondata : this.sectionData) {
            SectionPos sectionpos = SectionPos.of(pPos, serializablechunkdata$sectiondata.y);
            if (serializablechunkdata$sectiondata.chunkSection != null) {
                alevelchunksection[pLevel.getSectionIndexFromSectionY(serializablechunkdata$sectiondata.y)] = serializablechunkdata$sectiondata.chunkSection;
                pPoiManager.checkConsistencyWithBlocks(sectionpos, serializablechunkdata$sectiondata.chunkSection);
            }

            boolean flag2 = serializablechunkdata$sectiondata.blockLight != null;
            boolean flag3 = flag && serializablechunkdata$sectiondata.skyLight != null;
            if (flag2 || flag3) {
                if (!flag1) {
                    levellightengine.retainData(pPos, true);
                    flag1 = true;
                }

                if (flag2) {
                    levellightengine.queueSectionData(LightLayer.BLOCK, sectionpos, serializablechunkdata$sectiondata.blockLight);
                }

                if (flag3) {
                    levellightengine.queueSectionData(LightLayer.SKY, sectionpos, serializablechunkdata$sectiondata.skyLight);
                }
            }
        }

        ChunkType chunktype = this.chunkStatus.getChunkType();
        ChunkAccess chunkaccess;
        if (chunktype == ChunkType.LEVELCHUNK) {
            LevelChunkTicks<Block> levelchunkticks = new LevelChunkTicks<>(this.packedTicks.blocks());
            LevelChunkTicks<Fluid> levelchunkticks1 = new LevelChunkTicks<>(this.packedTicks.fluids());
            chunkaccess = new LevelChunk(
                pLevel.getLevel(),
                pPos,
                this.upgradeData,
                levelchunkticks,
                levelchunkticks1,
                this.inhabitedTime,
                alevelchunksection,
                postLoadChunk(pLevel, this.entities, this.blockEntities),
                BlendingData.unpack(this.blendingData)
            );
        } else {
            ProtoChunkTicks<Block> protochunkticks = ProtoChunkTicks.load(this.packedTicks.blocks());
            ProtoChunkTicks<Fluid> protochunkticks1 = ProtoChunkTicks.load(this.packedTicks.fluids());
            ProtoChunk protochunk1 = new ProtoChunk(
                pPos, this.upgradeData, alevelchunksection, protochunkticks, protochunkticks1, pLevel, registry, BlendingData.unpack(this.blendingData)
            );
            chunkaccess = protochunk1;
            protochunk1.setInhabitedTime(this.inhabitedTime);
            if (this.belowZeroRetrogen != null) {
                protochunk1.setBelowZeroRetrogen(this.belowZeroRetrogen);
            }

            protochunk1.setPersistedStatus(this.chunkStatus);
            if (this.chunkStatus.isOrAfter(ChunkStatus.INITIALIZE_LIGHT)) {
                protochunk1.setLightEngine(levellightengine);
            }
        }

        chunkaccess.setLightCorrect(this.lightCorrect);
        EnumSet<Heightmap.Types> enumset = EnumSet.noneOf(Heightmap.Types.class);

        for (Heightmap.Types heightmap$types : chunkaccess.getPersistedStatus().heightmapsAfter()) {
            long[] along = this.heightmaps.get(heightmap$types);
            if (along != null) {
                chunkaccess.setHeightmap(heightmap$types, along);
            } else {
                enumset.add(heightmap$types);
            }
        }

        Heightmap.primeHeightmaps(chunkaccess, enumset);
        chunkaccess.setAllStarts(unpackStructureStart(StructurePieceSerializationContext.fromLevel(pLevel), this.structureData, pLevel.getSeed()));
        chunkaccess.setAllReferences(unpackStructureReferences(pLevel.registryAccess(), pPos, this.structureData));

        for (int j = 0; j < this.postProcessingSections.length; j++) {
            chunkaccess.addPackedPostProcess(this.postProcessingSections[j], j);
        }

        if (chunktype == ChunkType.LEVELCHUNK) {
            return new ImposterProtoChunk((LevelChunk)chunkaccess, false);
        } else {
            ProtoChunk protochunk = (ProtoChunk)chunkaccess;

            for (CompoundTag compoundtag : this.entities) {
                protochunk.addEntity(compoundtag);
            }

            for (CompoundTag compoundtag1 : this.blockEntities) {
                protochunk.setBlockEntityNbt(compoundtag1);
            }

            if (this.carvingMask != null) {
                protochunk.setCarvingMask(new CarvingMask(this.carvingMask, chunkaccess.getMinY()));
            }

            return protochunk;
        }
    }

    private static void logErrors(ChunkPos pChunkPos, int pSectionY, String pError) {
        LOGGER.error("Recoverable errors when loading section [{}, {}, {}]: {}", pChunkPos.x, pSectionY, pChunkPos.z, pError);
    }

    private static Codec<PalettedContainerRO<Holder<Biome>>> makeBiomeCodec(Registry<Biome> pBiomeRegistry) {
        return PalettedContainer.codecRO(
            pBiomeRegistry.asHolderIdMap(), pBiomeRegistry.holderByNameCodec(), PalettedContainer.Strategy.SECTION_BIOMES, pBiomeRegistry.getOrThrow(Biomes.PLAINS)
        );
    }

    public static SerializableChunkData copyOf(ServerLevel pLevel, ChunkAccess pChunk) {
        if (!pChunk.canBeSerialized()) {
            throw new IllegalArgumentException("Chunk can't be serialized: " + pChunk);
        } else {
            ChunkPos chunkpos = pChunk.getPos();
            List<SerializableChunkData.SectionData> list = new ArrayList<>();
            LevelChunkSection[] alevelchunksection = pChunk.getSections();
            LevelLightEngine levellightengine = pLevel.getChunkSource().getLightEngine();

            for (int i = levellightengine.getMinLightSection(); i < levellightengine.getMaxLightSection(); i++) {
                int j = pChunk.getSectionIndexFromSectionY(i);
                boolean flag = j >= 0 && j < alevelchunksection.length;
                DataLayer datalayer = levellightengine.getLayerListener(LightLayer.BLOCK).getDataLayerData(SectionPos.of(chunkpos, i));
                DataLayer datalayer1 = levellightengine.getLayerListener(LightLayer.SKY).getDataLayerData(SectionPos.of(chunkpos, i));
                DataLayer datalayer2 = datalayer != null && !datalayer.isEmpty() ? datalayer.copy() : null;
                DataLayer datalayer3 = datalayer1 != null && !datalayer1.isEmpty() ? datalayer1.copy() : null;
                if (flag || datalayer2 != null || datalayer3 != null) {
                    LevelChunkSection levelchunksection = flag ? alevelchunksection[j].copy() : null;
                    list.add(new SerializableChunkData.SectionData(i, levelchunksection, datalayer2, datalayer3));
                }
            }

            List<CompoundTag> list1 = new ArrayList<>(pChunk.getBlockEntitiesPos().size());

            for (BlockPos blockpos : pChunk.getBlockEntitiesPos()) {
                CompoundTag compoundtag = pChunk.getBlockEntityNbtForSaving(blockpos, pLevel.registryAccess());
                if (compoundtag != null) {
                    list1.add(compoundtag);
                }
            }

            List<CompoundTag> list2 = new ArrayList<>();
            long[] along = null;
            if (pChunk.getPersistedStatus().getChunkType() == ChunkType.PROTOCHUNK) {
                ProtoChunk protochunk = (ProtoChunk)pChunk;
                list2.addAll(protochunk.getEntities());
                CarvingMask carvingmask = protochunk.getCarvingMask();
                if (carvingmask != null) {
                    along = carvingmask.toArray();
                }
            }

            Map<Heightmap.Types, long[]> map = new EnumMap<>(Heightmap.Types.class);

            for (Entry<Heightmap.Types, Heightmap> entry : pChunk.getHeightmaps()) {
                if (pChunk.getPersistedStatus().heightmapsAfter().contains(entry.getKey())) {
                    long[] along1 = entry.getValue().getRawData();
                    map.put(entry.getKey(), (long[])along1.clone());
                }
            }

            ChunkAccess.PackedTicks chunkaccess$packedticks = pChunk.getTicksForSerialization(pLevel.getGameTime());
            ShortList[] ashortlist = Arrays.stream(pChunk.getPostProcessing())
                .map(p_366782_ -> p_366782_ != null ? new ShortArrayList(p_366782_) : null)
                .toArray(ShortList[]::new);
            CompoundTag compoundtag1 = packStructureData(StructurePieceSerializationContext.fromLevel(pLevel), chunkpos, pChunk.getAllStarts(), pChunk.getAllReferences());
            return new SerializableChunkData(
                pLevel.registryAccess().lookupOrThrow(Registries.BIOME),
                chunkpos,
                pChunk.getMinSectionY(),
                pLevel.getGameTime(),
                pChunk.getInhabitedTime(),
                pChunk.getPersistedStatus(),
                Optionull.map(pChunk.getBlendingData(), BlendingData::pack),
                pChunk.getBelowZeroRetrogen(),
                pChunk.getUpgradeData().copy(),
                along,
                map,
                chunkaccess$packedticks,
                ashortlist,
                pChunk.isLightCorrect(),
                list,
                list2,
                list1,
                compoundtag1
            );
        }
    }

    public CompoundTag write() {
        CompoundTag compoundtag = NbtUtils.addCurrentDataVersion(new CompoundTag());
        compoundtag.putInt("xPos", this.chunkPos.x);
        compoundtag.putInt("yPos", this.minSectionY);
        compoundtag.putInt("zPos", this.chunkPos.z);
        compoundtag.putLong("LastUpdate", this.lastUpdateTime);
        compoundtag.putLong("InhabitedTime", this.inhabitedTime);
        compoundtag.putString("Status", BuiltInRegistries.CHUNK_STATUS.getKey(this.chunkStatus).toString());
        compoundtag.storeNullable("blending_data", BlendingData.Packed.CODEC, this.blendingData);
        compoundtag.storeNullable("below_zero_retrogen", BelowZeroRetrogen.CODEC, this.belowZeroRetrogen);
        if (!this.upgradeData.isEmpty()) {
            compoundtag.put("UpgradeData", this.upgradeData.write());
        }

        ListTag listtag = new ListTag();
        Codec<PalettedContainerRO<Holder<Biome>>> codec = makeBiomeCodec(this.biomeRegistry);

        for (SerializableChunkData.SectionData serializablechunkdata$sectiondata : this.sectionData) {
            CompoundTag compoundtag1 = new CompoundTag();
            LevelChunkSection levelchunksection = serializablechunkdata$sectiondata.chunkSection;
            if (levelchunksection != null) {
                compoundtag1.store("block_states", BLOCK_STATE_CODEC, levelchunksection.getStates());
                compoundtag1.store("biomes", codec, levelchunksection.getBiomes());
            }

            if (serializablechunkdata$sectiondata.blockLight != null) {
                compoundtag1.putByteArray("BlockLight", serializablechunkdata$sectiondata.blockLight.getData());
            }

            if (serializablechunkdata$sectiondata.skyLight != null) {
                compoundtag1.putByteArray("SkyLight", serializablechunkdata$sectiondata.skyLight.getData());
            }

            if (!compoundtag1.isEmpty()) {
                compoundtag1.putByte("Y", (byte)serializablechunkdata$sectiondata.y);
                listtag.add(compoundtag1);
            }
        }

        compoundtag.put("sections", listtag);
        if (this.lightCorrect) {
            compoundtag.putBoolean("isLightOn", true);
        }

        ListTag listtag1 = new ListTag();
        listtag1.addAll(this.blockEntities);
        compoundtag.put("block_entities", listtag1);
        if (this.chunkStatus.getChunkType() == ChunkType.PROTOCHUNK) {
            ListTag listtag2 = new ListTag();
            listtag2.addAll(this.entities);
            compoundtag.put("entities", listtag2);
            if (this.carvingMask != null) {
                compoundtag.putLongArray("carving_mask", this.carvingMask);
            }
        }

        saveTicks(compoundtag, this.packedTicks);
        compoundtag.put("PostProcessing", packOffsets(this.postProcessingSections));
        CompoundTag compoundtag2 = new CompoundTag();
        this.heightmaps.forEach((p_369025_, p_369618_) -> compoundtag2.put(p_369025_.getSerializationKey(), new LongArrayTag(p_369618_)));
        compoundtag.put("Heightmaps", compoundtag2);
        compoundtag.put("structures", this.structureData);
        return compoundtag;
    }

    private static void saveTicks(CompoundTag pTag, ChunkAccess.PackedTicks pTicks) {
        pTag.store("block_ticks", BLOCK_TICKS_CODEC, pTicks.blocks());
        pTag.store("fluid_ticks", FLUID_TICKS_CODEC, pTicks.fluids());
    }

    public static ChunkStatus getChunkStatusFromTag(@Nullable CompoundTag pTag) {
        return pTag != null ? pTag.read("Status", ChunkStatus.CODEC).orElse(ChunkStatus.EMPTY) : ChunkStatus.EMPTY;
    }

    @Nullable
    private static LevelChunk.PostLoadProcessor postLoadChunk(ServerLevel pLevel, List<CompoundTag> pEntities, List<CompoundTag> pBlockEntities) {
        return pEntities.isEmpty() && pBlockEntities.isEmpty() ? null : p_361871_ -> {
            if (!pEntities.isEmpty()) {
                pLevel.addLegacyChunkEntities(EntityType.loadEntitiesRecursive(pEntities, pLevel, EntitySpawnReason.LOAD));
            }

            for (CompoundTag compoundtag : pBlockEntities) {
                boolean flag = compoundtag.getBooleanOr("keepPacked", false);
                if (flag) {
                    p_361871_.setBlockEntityNbt(compoundtag);
                } else {
                    BlockPos blockpos = BlockEntity.getPosFromTag(p_361871_.getPos(), compoundtag);
                    BlockEntity blockentity = BlockEntity.loadStatic(blockpos, p_361871_.getBlockState(blockpos), compoundtag, pLevel.registryAccess());
                    if (blockentity != null) {
                        p_361871_.setBlockEntity(blockentity);
                    }
                }
            }
        };
    }

    private static CompoundTag packStructureData(
        StructurePieceSerializationContext pContext, ChunkPos pPos, Map<Structure, StructureStart> pStructureStarts, Map<Structure, LongSet> pReferences
    ) {
        CompoundTag compoundtag = new CompoundTag();
        CompoundTag compoundtag1 = new CompoundTag();
        Registry<Structure> registry = pContext.registryAccess().lookupOrThrow(Registries.STRUCTURE);

        for (Entry<Structure, StructureStart> entry : pStructureStarts.entrySet()) {
            ResourceLocation resourcelocation = registry.getKey(entry.getKey());
            compoundtag1.put(resourcelocation.toString(), entry.getValue().createTag(pContext, pPos));
        }

        compoundtag.put("starts", compoundtag1);
        CompoundTag compoundtag2 = new CompoundTag();

        for (Entry<Structure, LongSet> entry1 : pReferences.entrySet()) {
            if (!entry1.getValue().isEmpty()) {
                ResourceLocation resourcelocation1 = registry.getKey(entry1.getKey());
                compoundtag2.putLongArray(resourcelocation1.toString(), entry1.getValue().toLongArray());
            }
        }

        compoundtag.put("References", compoundtag2);
        return compoundtag;
    }

    private static Map<Structure, StructureStart> unpackStructureStart(StructurePieceSerializationContext pContext, CompoundTag pTag, long pSeed) {
        Map<Structure, StructureStart> map = Maps.newHashMap();
        Registry<Structure> registry = pContext.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        CompoundTag compoundtag = pTag.getCompoundOrEmpty("starts");

        for (String s : compoundtag.keySet()) {
            ResourceLocation resourcelocation = ResourceLocation.tryParse(s);
            Structure structure = registry.getValue(resourcelocation);
            if (structure == null) {
                LOGGER.error("Unknown structure start: {}", resourcelocation);
            } else {
                StructureStart structurestart = StructureStart.loadStaticStart(pContext, compoundtag.getCompoundOrEmpty(s), pSeed);
                if (structurestart != null) {
                    map.put(structure, structurestart);
                }
            }
        }

        return map;
    }

    private static Map<Structure, LongSet> unpackStructureReferences(RegistryAccess pRegistries, ChunkPos pPos, CompoundTag pTag) {
        Map<Structure, LongSet> map = Maps.newHashMap();
        Registry<Structure> registry = pRegistries.lookupOrThrow(Registries.STRUCTURE);
        CompoundTag compoundtag = pTag.getCompoundOrEmpty("References");
        compoundtag.forEach((p_391028_, p_391029_) -> {
            ResourceLocation resourcelocation = ResourceLocation.tryParse(p_391028_);
            Structure structure = registry.getValue(resourcelocation);
            if (structure == null) {
                LOGGER.warn("Found reference to unknown structure '{}' in chunk {}, discarding", resourcelocation, pPos);
            } else {
                Optional<long[]> optional = p_391029_.asLongArray();
                if (!optional.isEmpty()) {
                    map.put(structure, new LongOpenHashSet(Arrays.stream(optional.get()).filter(p_365743_ -> {
                        ChunkPos chunkpos = new ChunkPos(p_365743_);
                        if (chunkpos.getChessboardDistance(pPos) > 8) {
                            LOGGER.warn("Found invalid structure reference [ {} @ {} ] for chunk {}.", resourcelocation, chunkpos, pPos);
                            return false;
                        } else {
                            return true;
                        }
                    }).toArray()));
                }
            }
        });
        return map;
    }

    private static ListTag packOffsets(ShortList[] pOffsets) {
        ListTag listtag = new ListTag();

        for (ShortList shortlist : pOffsets) {
            ListTag listtag1 = new ListTag();
            if (shortlist != null) {
                for (int i = 0; i < shortlist.size(); i++) {
                    listtag1.add(ShortTag.valueOf(shortlist.getShort(i)));
                }
            }

            listtag.add(listtag1);
        }

        return listtag;
    }

    public static class ChunkReadException extends NbtException {
        public ChunkReadException(String p_364016_) {
            super(p_364016_);
        }
    }

    public record SectionData(int y, @Nullable LevelChunkSection chunkSection, @Nullable DataLayer blockLight, @Nullable DataLayer skyLight) {
    }
}