package net.minecraft.world.level.block.entity;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.Nameable;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AbstractBannerBlock;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.state.BlockState;

public class BannerBlockEntity extends BlockEntity implements Nameable {
    public static final int MAX_PATTERNS = 6;
    private static final String TAG_PATTERNS = "patterns";
    @Nullable
    private Component name;
    private final DyeColor baseColor;
    private BannerPatternLayers patterns = BannerPatternLayers.EMPTY;

    public BannerBlockEntity(BlockPos pPos, BlockState pBlockState) {
        this(pPos, pBlockState, ((AbstractBannerBlock)pBlockState.getBlock()).getColor());
    }

    public BannerBlockEntity(BlockPos pPos, BlockState pBlockState, DyeColor pBaseColor) {
        super(BlockEntityType.BANNER, pPos, pBlockState);
        this.baseColor = pBaseColor;
    }

    @Override
    public Component getName() {
        return (Component)(this.name != null ? this.name : Component.translatable("block.minecraft.banner"));
    }

    @Nullable
    @Override
    public Component getCustomName() {
        return this.name;
    }

    @Override
    protected void saveAdditional(CompoundTag p_187456_, HolderLookup.Provider p_329292_) {
        super.saveAdditional(p_187456_, p_329292_);
        RegistryOps<Tag> registryops = p_329292_.createSerializationContext(NbtOps.INSTANCE);
        if (!this.patterns.equals(BannerPatternLayers.EMPTY)) {
            p_187456_.store("patterns", BannerPatternLayers.CODEC, registryops, this.patterns);
        }

        p_187456_.storeNullable("CustomName", ComponentSerialization.CODEC, registryops, this.name);
    }

    @Override
    protected void loadAdditional(CompoundTag p_334165_, HolderLookup.Provider p_330621_) {
        super.loadAdditional(p_334165_, p_330621_);
        this.name = parseCustomNameSafe(p_334165_.get("CustomName"), p_330621_);
        RegistryOps<Tag> registryops = p_330621_.createSerializationContext(NbtOps.INSTANCE);
        this.patterns = p_334165_.read("patterns", BannerPatternLayers.CODEC, registryops).orElse(BannerPatternLayers.EMPTY);
    }

    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider p_335241_) {
        return this.saveWithoutMetadata(p_335241_);
    }

    public BannerPatternLayers getPatterns() {
        return this.patterns;
    }

    public ItemStack getItem() {
        ItemStack itemstack = new ItemStack(BannerBlock.byColor(this.baseColor));
        itemstack.applyComponents(this.collectComponents());
        return itemstack;
    }

    public DyeColor getBaseColor() {
        return this.baseColor;
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter p_396293_) {
        super.applyImplicitComponents(p_396293_);
        this.patterns = p_396293_.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY);
        this.name = p_396293_.get(DataComponents.CUSTOM_NAME);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder p_332512_) {
        super.collectImplicitComponents(p_332512_);
        p_332512_.set(DataComponents.BANNER_PATTERNS, this.patterns);
        p_332512_.set(DataComponents.CUSTOM_NAME, this.name);
    }

    @Override
    public void removeComponentsFromTag(CompoundTag p_336055_) {
        p_336055_.remove("patterns");
        p_336055_.remove("CustomName");
    }
}