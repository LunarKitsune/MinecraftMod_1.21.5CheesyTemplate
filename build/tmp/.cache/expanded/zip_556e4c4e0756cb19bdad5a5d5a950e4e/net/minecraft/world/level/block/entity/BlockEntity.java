package net.minecraft.world.level.block.entity;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.CrashReportCategory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.SectionPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

public abstract class BlockEntity extends net.minecraftforge.common.capabilities.CapabilityProvider<BlockEntity> implements net.minecraftforge.common.extensions.IForgeBlockEntity {
    private static final Codec<BlockEntityType<?>> TYPE_CODEC = BuiltInRegistries.BLOCK_ENTITY_TYPE.byNameCodec();
    private static final Logger LOGGER = LogUtils.getLogger();
    private final BlockEntityType<?> type;
    @Nullable
    protected Level level;
    protected final BlockPos worldPosition;
    protected boolean remove;
    private BlockState blockState;
    private DataComponentMap components = DataComponentMap.EMPTY;

    public BlockEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(BlockEntity.class);
        this.type = pType;
        this.worldPosition = pPos.immutable();
        this.validateBlockState(pBlockState);
        this.blockState = pBlockState;
        this.gatherCapabilities();
    }

    private void validateBlockState(BlockState pState) {
        if (!this.isValidBlockState(pState)) {
            throw new IllegalStateException("Invalid block entity " + this.getNameForReporting() + " state at " + this.worldPosition + ", got " + pState);
        }
    }

    public boolean isValidBlockState(BlockState pState) {
        return this.getType().isValid(pState);
    }

    public static BlockPos getPosFromTag(ChunkPos pChunkPos, CompoundTag pTag) {
        int i = pTag.getIntOr("x", 0);
        int j = pTag.getIntOr("y", 0);
        int k = pTag.getIntOr("z", 0);
        int l = SectionPos.blockToSectionCoord(i);
        int i1 = SectionPos.blockToSectionCoord(k);
        if (l != pChunkPos.x || i1 != pChunkPos.z) {
            LOGGER.warn("Block entity {} found in a wrong chunk, expected position from chunk {}", pTag, pChunkPos);
            i = pChunkPos.getBlockX(SectionPos.sectionRelative(i));
            k = pChunkPos.getBlockZ(SectionPos.sectionRelative(k));
        }

        return new BlockPos(i, j, k);
    }

    @Nullable
    public Level getLevel() {
        return this.level;
    }

    public void setLevel(Level pLevel) {
        this.level = pLevel;
    }

    public boolean hasLevel() {
        return this.level != null;
    }

    protected void loadAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        var caps = pTag.getCompound("ForgeCaps");
        if (getCapabilities() != null && caps.isPresent()) deserializeCaps(pRegistries, caps.get());
    }

    public final void loadWithComponents(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        this.loadAdditional(pTag, pRegistries);
        this.components = pTag.read(BlockEntity.ComponentHelper.COMPONENTS_CODEC, pRegistries.createSerializationContext(NbtOps.INSTANCE)).orElse(DataComponentMap.EMPTY);
    }

    public final void loadCustomOnly(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        this.loadAdditional(pTag, pRegistries);
    }

    protected void saveAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        if (getCapabilities() != null) pTag.put("ForgeCaps", serializeCaps(pRegistries));
    }

    public final CompoundTag saveWithFullMetadata(HolderLookup.Provider pRegistries) {
        CompoundTag compoundtag = this.saveWithoutMetadata(pRegistries);
        this.saveMetadata(compoundtag);
        return compoundtag;
    }

    public final CompoundTag saveWithId(HolderLookup.Provider pRegistries) {
        CompoundTag compoundtag = this.saveWithoutMetadata(pRegistries);
        this.saveId(compoundtag);
        return compoundtag;
    }

    public final CompoundTag saveWithoutMetadata(HolderLookup.Provider pRegistries) {
        CompoundTag compoundtag = new CompoundTag();
        this.saveAdditional(compoundtag, pRegistries);
        compoundtag.store(BlockEntity.ComponentHelper.COMPONENTS_CODEC, pRegistries.createSerializationContext(NbtOps.INSTANCE), this.components);
        return compoundtag;
    }

    public final CompoundTag saveCustomOnly(HolderLookup.Provider pRegistries) {
        CompoundTag compoundtag = new CompoundTag();
        this.saveAdditional(compoundtag, pRegistries);
        return compoundtag;
    }

    public final CompoundTag saveCustomAndMetadata(HolderLookup.Provider pRegistries) {
        CompoundTag compoundtag = this.saveCustomOnly(pRegistries);
        this.saveMetadata(compoundtag);
        return compoundtag;
    }

    private void saveId(CompoundTag pTag) {
        addEntityType(pTag, this.getType());
    }

    public static void addEntityType(CompoundTag pTag, BlockEntityType<?> pEntityType) {
        pTag.store("id", TYPE_CODEC, pEntityType);
    }

    private void saveMetadata(CompoundTag pTag) {
        this.saveId(pTag);
        pTag.putInt("x", this.worldPosition.getX());
        pTag.putInt("y", this.worldPosition.getY());
        pTag.putInt("z", this.worldPosition.getZ());
    }

    @Nullable
    public static BlockEntity loadStatic(BlockPos pPos, BlockState pState, CompoundTag pTag, HolderLookup.Provider pRegistries) {
        BlockEntityType<?> blockentitytype = pTag.read("id", TYPE_CODEC).orElse(null);
        if (blockentitytype == null) {
            LOGGER.error("Skipping block entity with invalid type: {}", pTag.get("id"));
            return null;
        } else {
            BlockEntity blockentity;
            try {
                blockentity = blockentitytype.create(pPos, pState);
            } catch (Throwable throwable1) {
                LOGGER.error("Failed to create block entity {} for block {} at position {} ", blockentitytype, pPos, pState, throwable1);
                return null;
            }

            try {
                blockentity.loadWithComponents(pTag, pRegistries);
                return blockentity;
            } catch (Throwable throwable) {
                LOGGER.error("Failed to load data for block entity {} for block {} at position {}", blockentitytype, pPos, pState, throwable);
                return null;
            }
        }
    }

    public void setChanged() {
        if (this.level != null) {
            setChanged(this.level, this.worldPosition, this.blockState);
        }
    }

    protected static void setChanged(Level pLevel, BlockPos pPos, BlockState pState) {
        pLevel.blockEntityChanged(pPos);
        if (!pState.isAir()) {
            pLevel.updateNeighbourForOutputSignal(pPos, pState.getBlock());
        }
    }

    public BlockPos getBlockPos() {
        return this.worldPosition;
    }

    public BlockState getBlockState() {
        return this.blockState;
    }

    @Nullable
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return null;
    }

    public CompoundTag getUpdateTag(HolderLookup.Provider pRegistries) {
        return new CompoundTag();
    }

    public boolean isRemoved() {
        return this.remove;
    }

    public void setRemoved() {
        this.remove = true;
        this.invalidateCaps();
        requestModelDataUpdate();
    }

    @Override
    public void onChunkUnloaded() {
        this.invalidateCaps();
    }

    public void clearRemoved() {
        this.remove = false;
    }

    public void preRemoveSideEffects(BlockPos pPos, BlockState pState) {
        if (this instanceof Container container && this.level != null) {
            Containers.dropContents(this.level, pPos, container);
        }
    }

    public boolean triggerEvent(int pId, int pType) {
        return false;
    }

    public void fillCrashReportCategory(CrashReportCategory pReportCategory) {
        pReportCategory.setDetail("Name", this::getNameForReporting);
        pReportCategory.setDetail("Cached block", this.getBlockState()::toString);
        if (this.level == null) {
            pReportCategory.setDetail("Block location", () -> this.worldPosition + " (world missing)");
        } else {
            pReportCategory.setDetail("Actual block", this.level.getBlockState(this.worldPosition)::toString);
            CrashReportCategory.populateBlockLocationDetails(pReportCategory, this.level, this.worldPosition);
        }
    }

    private String getNameForReporting() {
        return BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(this.getType()) + " // " + this.getClass().getCanonicalName();
    }

    public BlockEntityType<?> getType() {
        return this.type;
    }

    @Deprecated
    public void setBlockState(BlockState pBlockState) {
        this.validateBlockState(pBlockState);
        this.blockState = pBlockState;
    }

    protected void applyImplicitComponents(DataComponentGetter pComponentGetter) {
    }

    public final void applyComponentsFromItemStack(ItemStack pStack) {
        this.applyComponents(pStack.getPrototype(), pStack.getComponentsPatch());
    }

    public final void applyComponents(DataComponentMap pComponents, DataComponentPatch pPatch) {
        final Set<DataComponentType<?>> set = new HashSet<>();
        set.add(DataComponents.BLOCK_ENTITY_DATA);
        set.add(DataComponents.BLOCK_STATE);
        final DataComponentMap datacomponentmap = PatchedDataComponentMap.fromPatch(pComponents, pPatch);
        this.applyImplicitComponents(new DataComponentGetter() {
            @Nullable
            @Override
            public <T> T get(DataComponentType<? extends T> p_335233_) {
                set.add(p_335233_);
                return datacomponentmap.get(p_335233_);
            }

            @Override
            public <T> T getOrDefault(DataComponentType<? extends T> p_334887_, T p_333244_) {
                set.add(p_334887_);
                return datacomponentmap.getOrDefault(p_334887_, p_333244_);
            }
        });
        DataComponentPatch datacomponentpatch = pPatch.forget(set::contains);
        this.components = datacomponentpatch.split().added();
    }

    protected void collectImplicitComponents(DataComponentMap.Builder pComponents) {
    }

    @Deprecated
    public void removeComponentsFromTag(CompoundTag pTag) {
    }

    public final DataComponentMap collectComponents() {
        DataComponentMap.Builder datacomponentmap$builder = DataComponentMap.builder();
        datacomponentmap$builder.addAll(this.components);
        this.collectImplicitComponents(datacomponentmap$builder);
        return datacomponentmap$builder.build();
    }

    public DataComponentMap components() {
        return this.components;
    }

    public void setComponents(DataComponentMap pComponents) {
        this.components = pComponents;
    }

    @Nullable
    public static Component parseCustomNameSafe(@Nullable Tag pTag, HolderLookup.Provider pRegistries) {
        return pTag == null
            ? null
            : ComponentSerialization.CODEC
                .parse(pRegistries.createSerializationContext(NbtOps.INSTANCE), pTag)
                .resultOrPartial(p_327293_ -> LOGGER.warn("Failed to parse custom name, discarding: {}", p_327293_))
                .orElse(null);
    }

    static class ComponentHelper {
        public static final MapCodec<DataComponentMap> COMPONENTS_CODEC = DataComponentMap.CODEC.optionalFieldOf("components", DataComponentMap.EMPTY);

        private ComponentHelper() {
        }
    }
}
