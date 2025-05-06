package net.minecraft.world.level.block.entity;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.Pools;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public class JigsawBlockEntity extends BlockEntity {
    public static final Codec<ResourceKey<StructureTemplatePool>> POOL_CODEC = ResourceKey.codec(Registries.TEMPLATE_POOL);
    public static final ResourceLocation EMPTY_ID = ResourceLocation.withDefaultNamespace("empty");
    private static final int DEFAULT_PLACEMENT_PRIORITY = 0;
    private static final int DEFAULT_SELECTION_PRIORITY = 0;
    public static final String TARGET = "target";
    public static final String POOL = "pool";
    public static final String JOINT = "joint";
    public static final String PLACEMENT_PRIORITY = "placement_priority";
    public static final String SELECTION_PRIORITY = "selection_priority";
    public static final String NAME = "name";
    public static final String FINAL_STATE = "final_state";
    public static final String DEFAULT_FINAL_STATE = "minecraft:air";
    private ResourceLocation name = EMPTY_ID;
    private ResourceLocation target = EMPTY_ID;
    private ResourceKey<StructureTemplatePool> pool = Pools.EMPTY;
    private JigsawBlockEntity.JointType joint = JigsawBlockEntity.JointType.ROLLABLE;
    private String finalState = "minecraft:air";
    private int placementPriority = 0;
    private int selectionPriority = 0;

    public JigsawBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(BlockEntityType.JIGSAW, pPos, pBlockState);
    }

    public ResourceLocation getName() {
        return this.name;
    }

    public ResourceLocation getTarget() {
        return this.target;
    }

    public ResourceKey<StructureTemplatePool> getPool() {
        return this.pool;
    }

    public String getFinalState() {
        return this.finalState;
    }

    public JigsawBlockEntity.JointType getJoint() {
        return this.joint;
    }

    public int getPlacementPriority() {
        return this.placementPriority;
    }

    public int getSelectionPriority() {
        return this.selectionPriority;
    }

    public void setName(ResourceLocation pName) {
        this.name = pName;
    }

    public void setTarget(ResourceLocation pTarget) {
        this.target = pTarget;
    }

    public void setPool(ResourceKey<StructureTemplatePool> pPool) {
        this.pool = pPool;
    }

    public void setFinalState(String pFinalState) {
        this.finalState = pFinalState;
    }

    public void setJoint(JigsawBlockEntity.JointType pJoint) {
        this.joint = pJoint;
    }

    public void setPlacementPriority(int pPlacementPriority) {
        this.placementPriority = pPlacementPriority;
    }

    public void setSelectionPriority(int pSelectionPriority) {
        this.selectionPriority = pSelectionPriority;
    }

    @Override
    protected void saveAdditional(CompoundTag p_187504_, HolderLookup.Provider p_335581_) {
        super.saveAdditional(p_187504_, p_335581_);
        p_187504_.store("name", ResourceLocation.CODEC, this.name);
        p_187504_.store("target", ResourceLocation.CODEC, this.target);
        p_187504_.store("pool", POOL_CODEC, this.pool);
        p_187504_.putString("final_state", this.finalState);
        p_187504_.store("joint", JigsawBlockEntity.JointType.CODEC, this.joint);
        p_187504_.putInt("placement_priority", this.placementPriority);
        p_187504_.putInt("selection_priority", this.selectionPriority);
    }

    @Override
    protected void loadAdditional(CompoundTag p_331375_, HolderLookup.Provider p_332374_) {
        super.loadAdditional(p_331375_, p_332374_);
        this.name = p_331375_.read("name", ResourceLocation.CODEC).orElse(EMPTY_ID);
        this.target = p_331375_.read("target", ResourceLocation.CODEC).orElse(EMPTY_ID);
        this.pool = p_331375_.read("pool", POOL_CODEC).orElse(Pools.EMPTY);
        this.finalState = p_331375_.getStringOr("final_state", "minecraft:air");
        this.joint = p_331375_.read("joint", JigsawBlockEntity.JointType.CODEC).orElseGet(() -> StructureTemplate.getDefaultJointType(this.getBlockState()));
        this.placementPriority = p_331375_.getIntOr("placement_priority", 0);
        this.selectionPriority = p_331375_.getIntOr("selection_priority", 0);
    }

    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider p_333585_) {
        return this.saveCustomOnly(p_333585_);
    }

    public void generate(ServerLevel pLevel, int pMaxDepth, boolean pKeepJigsaws) {
        BlockPos blockpos = this.getBlockPos().relative(this.getBlockState().getValue(JigsawBlock.ORIENTATION).front());
        Registry<StructureTemplatePool> registry = pLevel.registryAccess().lookupOrThrow(Registries.TEMPLATE_POOL);
        Holder<StructureTemplatePool> holder = registry.getOrThrow(this.pool);
        JigsawPlacement.generateJigsaw(pLevel, holder, this.target, pMaxDepth, blockpos, pKeepJigsaws);
    }

    public static enum JointType implements StringRepresentable {
        ROLLABLE("rollable"),
        ALIGNED("aligned");

        public static final StringRepresentable.EnumCodec<JigsawBlockEntity.JointType> CODEC = StringRepresentable.fromEnum(
            JigsawBlockEntity.JointType::values
        );
        private final String name;

        private JointType(final String pName) {
            this.name = pName;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        public Component getTranslatedName() {
            return Component.translatable("jigsaw_block.joint." + this.name);
        }
    }
}