package net.minecraft.world.level.block.entity;

import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.ResourceLocationException;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.StructureBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.StructureMode;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockRotProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public class StructureBlockEntity extends BlockEntity implements BoundingBoxRenderable {
    private static final int SCAN_CORNER_BLOCKS_RANGE = 5;
    public static final int MAX_OFFSET_PER_AXIS = 48;
    public static final int MAX_SIZE_PER_AXIS = 48;
    public static final String AUTHOR_TAG = "author";
    private static final String DEFAULT_AUTHOR = "";
    private static final String DEFAULT_METADATA = "";
    private static final BlockPos DEFAULT_POS = new BlockPos(0, 1, 0);
    private static final Vec3i DEFAULT_SIZE = Vec3i.ZERO;
    private static final Rotation DEFAULT_ROTATION = Rotation.NONE;
    private static final Mirror DEFAULT_MIRROR = Mirror.NONE;
    private static final boolean DEFAULT_IGNORE_ENTITIES = true;
    private static final boolean DEFAULT_STRICT = false;
    private static final boolean DEFAULT_POWERED = false;
    private static final boolean DEFAULT_SHOW_AIR = false;
    private static final boolean DEFAULT_SHOW_BOUNDING_BOX = true;
    private static final float DEFAULT_INTEGRITY = 1.0F;
    private static final long DEFAULT_SEED = 0L;
    @Nullable
    private ResourceLocation structureName;
    private String author = "";
    private String metaData = "";
    private BlockPos structurePos = DEFAULT_POS;
    private Vec3i structureSize = DEFAULT_SIZE;
    private Mirror mirror = Mirror.NONE;
    private Rotation rotation = Rotation.NONE;
    private StructureMode mode;
    private boolean ignoreEntities = true;
    private boolean strict = false;
    private boolean powered = false;
    private boolean showAir = false;
    private boolean showBoundingBox = true;
    private float integrity = 1.0F;
    private long seed = 0L;

    public StructureBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(BlockEntityType.STRUCTURE_BLOCK, pPos, pBlockState);
        this.mode = pBlockState.getValue(StructureBlock.MODE);
    }

    @Override
    protected void saveAdditional(CompoundTag p_187524_, HolderLookup.Provider p_331199_) {
        super.saveAdditional(p_187524_, p_331199_);
        p_187524_.putString("name", this.getStructureName());
        p_187524_.putString("author", this.author);
        p_187524_.putString("metadata", this.metaData);
        p_187524_.putInt("posX", this.structurePos.getX());
        p_187524_.putInt("posY", this.structurePos.getY());
        p_187524_.putInt("posZ", this.structurePos.getZ());
        p_187524_.putInt("sizeX", this.structureSize.getX());
        p_187524_.putInt("sizeY", this.structureSize.getY());
        p_187524_.putInt("sizeZ", this.structureSize.getZ());
        p_187524_.store("rotation", Rotation.LEGACY_CODEC, this.rotation);
        p_187524_.store("mirror", Mirror.LEGACY_CODEC, this.mirror);
        p_187524_.store("mode", StructureMode.LEGACY_CODEC, this.mode);
        p_187524_.putBoolean("ignoreEntities", this.ignoreEntities);
        p_187524_.putBoolean("strict", this.strict);
        p_187524_.putBoolean("powered", this.powered);
        p_187524_.putBoolean("showair", this.showAir);
        p_187524_.putBoolean("showboundingbox", this.showBoundingBox);
        p_187524_.putFloat("integrity", this.integrity);
        p_187524_.putLong("seed", this.seed);
    }

    @Override
    protected void loadAdditional(CompoundTag p_332447_, HolderLookup.Provider p_334283_) {
        super.loadAdditional(p_332447_, p_334283_);
        this.setStructureName(p_332447_.getStringOr("name", ""));
        this.author = p_332447_.getStringOr("author", "");
        this.metaData = p_332447_.getStringOr("metadata", "");
        int i = Mth.clamp(p_332447_.getIntOr("posX", DEFAULT_POS.getX()), -48, 48);
        int j = Mth.clamp(p_332447_.getIntOr("posY", DEFAULT_POS.getY()), -48, 48);
        int k = Mth.clamp(p_332447_.getIntOr("posZ", DEFAULT_POS.getZ()), -48, 48);
        this.structurePos = new BlockPos(i, j, k);
        int l = Mth.clamp(p_332447_.getIntOr("sizeX", DEFAULT_SIZE.getX()), 0, 48);
        int i1 = Mth.clamp(p_332447_.getIntOr("sizeY", DEFAULT_SIZE.getY()), 0, 48);
        int j1 = Mth.clamp(p_332447_.getIntOr("sizeZ", DEFAULT_SIZE.getZ()), 0, 48);
        this.structureSize = new Vec3i(l, i1, j1);
        this.rotation = p_332447_.read("rotation", Rotation.LEGACY_CODEC).orElse(DEFAULT_ROTATION);
        this.mirror = p_332447_.read("mirror", Mirror.LEGACY_CODEC).orElse(DEFAULT_MIRROR);
        this.mode = p_332447_.read("mode", StructureMode.LEGACY_CODEC).orElse(StructureMode.DATA);
        this.ignoreEntities = p_332447_.getBooleanOr("ignoreEntities", true);
        this.strict = p_332447_.getBooleanOr("strict", false);
        this.powered = p_332447_.getBooleanOr("powered", false);
        this.showAir = p_332447_.getBooleanOr("showair", false);
        this.showBoundingBox = p_332447_.getBooleanOr("showboundingbox", true);
        this.integrity = p_332447_.getFloatOr("integrity", 1.0F);
        this.seed = p_332447_.getLongOr("seed", 0L);
        this.updateBlockState();
    }

    private void updateBlockState() {
        if (this.level != null) {
            BlockPos blockpos = this.getBlockPos();
            BlockState blockstate = this.level.getBlockState(blockpos);
            if (blockstate.is(Blocks.STRUCTURE_BLOCK)) {
                this.level.setBlock(blockpos, blockstate.setValue(StructureBlock.MODE, this.mode), 2);
            }
        }
    }

    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider p_327713_) {
        return this.saveCustomOnly(p_327713_);
    }

    public boolean usedBy(Player pPlayer) {
        if (!pPlayer.canUseGameMasterBlocks()) {
            return false;
        } else {
            if (pPlayer.getCommandSenderWorld().isClientSide) {
                pPlayer.openStructureBlock(this);
            }

            return true;
        }
    }

    public String getStructureName() {
        return this.structureName == null ? "" : this.structureName.toString();
    }

    public boolean hasStructureName() {
        return this.structureName != null;
    }

    public void setStructureName(@Nullable String pStructureName) {
        this.setStructureName(StringUtil.isNullOrEmpty(pStructureName) ? null : ResourceLocation.tryParse(pStructureName));
    }

    public void setStructureName(@Nullable ResourceLocation pStructureName) {
        this.structureName = pStructureName;
    }

    public void createdBy(LivingEntity pAuthor) {
        this.author = pAuthor.getName().getString();
    }

    public BlockPos getStructurePos() {
        return this.structurePos;
    }

    public void setStructurePos(BlockPos pStructurePos) {
        this.structurePos = pStructurePos;
    }

    public Vec3i getStructureSize() {
        return this.structureSize;
    }

    public void setStructureSize(Vec3i pStructureSize) {
        this.structureSize = pStructureSize;
    }

    public Mirror getMirror() {
        return this.mirror;
    }

    public void setMirror(Mirror pMirror) {
        this.mirror = pMirror;
    }

    public Rotation getRotation() {
        return this.rotation;
    }

    public void setRotation(Rotation pRotation) {
        this.rotation = pRotation;
    }

    public String getMetaData() {
        return this.metaData;
    }

    public void setMetaData(String pMetaData) {
        this.metaData = pMetaData;
    }

    public StructureMode getMode() {
        return this.mode;
    }

    public void setMode(StructureMode pMode) {
        this.mode = pMode;
        BlockState blockstate = this.level.getBlockState(this.getBlockPos());
        if (blockstate.is(Blocks.STRUCTURE_BLOCK)) {
            this.level.setBlock(this.getBlockPos(), blockstate.setValue(StructureBlock.MODE, pMode), 2);
        }
    }

    public boolean isIgnoreEntities() {
        return this.ignoreEntities;
    }

    public boolean isStrict() {
        return this.strict;
    }

    public void setIgnoreEntities(boolean pIgnoreEntities) {
        this.ignoreEntities = pIgnoreEntities;
    }

    public void setStrict(boolean pStrict) {
        this.strict = pStrict;
    }

    public float getIntegrity() {
        return this.integrity;
    }

    public void setIntegrity(float pIntegrity) {
        this.integrity = pIntegrity;
    }

    public long getSeed() {
        return this.seed;
    }

    public void setSeed(long pSeed) {
        this.seed = pSeed;
    }

    public boolean detectSize() {
        if (this.mode != StructureMode.SAVE) {
            return false;
        } else {
            BlockPos blockpos = this.getBlockPos();
            int i = 80;
            BlockPos blockpos1 = new BlockPos(blockpos.getX() - 80, this.level.getMinY(), blockpos.getZ() - 80);
            BlockPos blockpos2 = new BlockPos(blockpos.getX() + 80, this.level.getMaxY(), blockpos.getZ() + 80);
            Stream<BlockPos> stream = this.getRelatedCorners(blockpos1, blockpos2);
            return calculateEnclosingBoundingBox(blockpos, stream)
                .filter(
                    p_155790_ -> {
                        int j = p_155790_.maxX() - p_155790_.minX();
                        int k = p_155790_.maxY() - p_155790_.minY();
                        int l = p_155790_.maxZ() - p_155790_.minZ();
                        if (j > 1 && k > 1 && l > 1) {
                            this.structurePos = new BlockPos(
                                p_155790_.minX() - blockpos.getX() + 1,
                                p_155790_.minY() - blockpos.getY() + 1,
                                p_155790_.minZ() - blockpos.getZ() + 1
                            );
                            this.structureSize = new Vec3i(j - 1, k - 1, l - 1);
                            this.setChanged();
                            BlockState blockstate = this.level.getBlockState(blockpos);
                            this.level.sendBlockUpdated(blockpos, blockstate, blockstate, 3);
                            return true;
                        } else {
                            return false;
                        }
                    }
                )
                .isPresent();
        }
    }

    private Stream<BlockPos> getRelatedCorners(BlockPos pMinPos, BlockPos pMaxPos) {
        return BlockPos.betweenClosedStream(pMinPos, pMaxPos)
            .filter(p_272561_ -> this.level.getBlockState(p_272561_).is(Blocks.STRUCTURE_BLOCK))
            .map(this.level::getBlockEntity)
            .filter(p_155802_ -> p_155802_ instanceof StructureBlockEntity)
            .map(p_155785_ -> (StructureBlockEntity)p_155785_)
            .filter(p_155787_ -> p_155787_.mode == StructureMode.CORNER && Objects.equals(this.structureName, p_155787_.structureName))
            .map(BlockEntity::getBlockPos);
    }

    private static Optional<BoundingBox> calculateEnclosingBoundingBox(BlockPos pPos, Stream<BlockPos> pRelatedCorners) {
        Iterator<BlockPos> iterator = pRelatedCorners.iterator();
        if (!iterator.hasNext()) {
            return Optional.empty();
        } else {
            BlockPos blockpos = iterator.next();
            BoundingBox boundingbox = new BoundingBox(blockpos);
            if (iterator.hasNext()) {
                iterator.forEachRemaining(boundingbox::encapsulate);
            } else {
                boundingbox.encapsulate(pPos);
            }

            return Optional.of(boundingbox);
        }
    }

    public boolean saveStructure() {
        return this.mode != StructureMode.SAVE ? false : this.saveStructure(true);
    }

    public boolean saveStructure(boolean pWriteToDisk) {
        if (this.structureName != null && this.level instanceof ServerLevel serverlevel) {
            BlockPos blockpos = this.getBlockPos().offset(this.structurePos);
            return saveStructure(serverlevel, this.structureName, blockpos, this.structureSize, this.ignoreEntities, this.author, pWriteToDisk);
        } else {
            return false;
        }
    }

    public static boolean saveStructure(
        ServerLevel pLevel, ResourceLocation pName, BlockPos pPos, Vec3i pSize, boolean pIgnoreEntities, String pAuthor, boolean pWriteToDisk
    ) {
        StructureTemplateManager structuretemplatemanager = pLevel.getStructureManager();

        StructureTemplate structuretemplate;
        try {
            structuretemplate = structuretemplatemanager.getOrCreate(pName);
        } catch (ResourceLocationException resourcelocationexception1) {
            return false;
        }

        structuretemplate.fillFromWorld(pLevel, pPos, pSize, !pIgnoreEntities, Blocks.STRUCTURE_VOID);
        structuretemplate.setAuthor(pAuthor);
        if (pWriteToDisk) {
            try {
                return structuretemplatemanager.save(pName);
            } catch (ResourceLocationException resourcelocationexception) {
                return false;
            }
        } else {
            return true;
        }
    }

    public static RandomSource createRandom(long pSeed) {
        return pSeed == 0L ? RandomSource.create(Util.getMillis()) : RandomSource.create(pSeed);
    }

    public boolean placeStructureIfSameSize(ServerLevel pLevel) {
        if (this.mode == StructureMode.LOAD && this.structureName != null) {
            StructureTemplate structuretemplate = pLevel.getStructureManager().get(this.structureName).orElse(null);
            if (structuretemplate == null) {
                return false;
            } else if (structuretemplate.getSize().equals(this.structureSize)) {
                this.placeStructure(pLevel, structuretemplate);
                return true;
            } else {
                this.loadStructureInfo(structuretemplate);
                return false;
            }
        } else {
            return false;
        }
    }

    public boolean loadStructureInfo(ServerLevel pLevel) {
        StructureTemplate structuretemplate = this.getStructureTemplate(pLevel);
        if (structuretemplate == null) {
            return false;
        } else {
            this.loadStructureInfo(structuretemplate);
            return true;
        }
    }

    private void loadStructureInfo(StructureTemplate pStructureTemplate) {
        this.author = !StringUtil.isNullOrEmpty(pStructureTemplate.getAuthor()) ? pStructureTemplate.getAuthor() : "";
        this.structureSize = pStructureTemplate.getSize();
        this.setChanged();
    }

    public void placeStructure(ServerLevel pLevel) {
        StructureTemplate structuretemplate = this.getStructureTemplate(pLevel);
        if (structuretemplate != null) {
            this.placeStructure(pLevel, structuretemplate);
        }
    }

    @Nullable
    private StructureTemplate getStructureTemplate(ServerLevel pLevel) {
        return this.structureName == null ? null : pLevel.getStructureManager().get(this.structureName).orElse(null);
    }

    private void placeStructure(ServerLevel pLevel, StructureTemplate pStructureTemplate) {
        this.loadStructureInfo(pStructureTemplate);
        StructurePlaceSettings structureplacesettings = new StructurePlaceSettings()
            .setMirror(this.mirror)
            .setRotation(this.rotation)
            .setIgnoreEntities(this.ignoreEntities)
            .setKnownShape(this.strict);
        if (this.integrity < 1.0F) {
            structureplacesettings.clearProcessors().addProcessor(new BlockRotProcessor(Mth.clamp(this.integrity, 0.0F, 1.0F))).setRandom(createRandom(this.seed));
        }

        BlockPos blockpos = this.getBlockPos().offset(this.structurePos);
        pStructureTemplate.placeInWorld(pLevel, blockpos, blockpos, structureplacesettings, createRandom(this.seed), 2 | (this.strict ? 816 : 0));
    }

    public void unloadStructure() {
        if (this.structureName != null) {
            ServerLevel serverlevel = (ServerLevel)this.level;
            StructureTemplateManager structuretemplatemanager = serverlevel.getStructureManager();
            structuretemplatemanager.remove(this.structureName);
        }
    }

    public boolean isStructureLoadable() {
        if (this.mode == StructureMode.LOAD && !this.level.isClientSide && this.structureName != null) {
            ServerLevel serverlevel = (ServerLevel)this.level;
            StructureTemplateManager structuretemplatemanager = serverlevel.getStructureManager();

            try {
                return structuretemplatemanager.get(this.structureName).isPresent();
            } catch (ResourceLocationException resourcelocationexception) {
                return false;
            }
        } else {
            return false;
        }
    }

    public boolean isPowered() {
        return this.powered;
    }

    public void setPowered(boolean pPowered) {
        this.powered = pPowered;
    }

    public boolean getShowAir() {
        return this.showAir;
    }

    public void setShowAir(boolean pShowAir) {
        this.showAir = pShowAir;
    }

    public boolean getShowBoundingBox() {
        return this.showBoundingBox;
    }

    public void setShowBoundingBox(boolean pShowBoundingBox) {
        this.showBoundingBox = pShowBoundingBox;
    }

    @Override
    public BoundingBoxRenderable.Mode renderMode() {
        if (this.mode != StructureMode.SAVE && this.mode != StructureMode.LOAD) {
            return BoundingBoxRenderable.Mode.NONE;
        } else if (this.mode == StructureMode.SAVE && this.showAir) {
            return BoundingBoxRenderable.Mode.BOX_AND_INVISIBLE_BLOCKS;
        } else {
            return this.mode != StructureMode.SAVE && !this.showBoundingBox ? BoundingBoxRenderable.Mode.NONE : BoundingBoxRenderable.Mode.BOX;
        }
    }

    @Override
    public BoundingBoxRenderable.RenderableBox getRenderableBox() {
        BlockPos blockpos = this.getStructurePos();
        Vec3i vec3i = this.getStructureSize();
        int i = blockpos.getX();
        int j = blockpos.getZ();
        int j1 = blockpos.getY();
        int i2 = j1 + vec3i.getY();
        int k;
        int l;
        switch (this.mirror) {
            case LEFT_RIGHT:
                k = vec3i.getX();
                l = -vec3i.getZ();
                break;
            case FRONT_BACK:
                k = -vec3i.getX();
                l = vec3i.getZ();
                break;
            default:
                k = vec3i.getX();
                l = vec3i.getZ();
        }

        int i1;
        int k1;
        int l1;
        int j2;
        switch (this.rotation) {
            case CLOCKWISE_90:
                i1 = l < 0 ? i : i + 1;
                k1 = k < 0 ? j + 1 : j;
                l1 = i1 - l;
                j2 = k1 + k;
                break;
            case CLOCKWISE_180:
                i1 = k < 0 ? i : i + 1;
                k1 = l < 0 ? j : j + 1;
                l1 = i1 - k;
                j2 = k1 - l;
                break;
            case COUNTERCLOCKWISE_90:
                i1 = l < 0 ? i + 1 : i;
                k1 = k < 0 ? j : j + 1;
                l1 = i1 + l;
                j2 = k1 - k;
                break;
            default:
                i1 = k < 0 ? i + 1 : i;
                k1 = l < 0 ? j + 1 : j;
                l1 = i1 + k;
                j2 = k1 + l;
        }

        return BoundingBoxRenderable.RenderableBox.fromCorners(i1, j1, k1, l1, i2, j2);
    }

    public static enum UpdateType {
        UPDATE_DATA,
        SAVE_AREA,
        LOAD_AREA,
        SCAN_AREA;
    }
}