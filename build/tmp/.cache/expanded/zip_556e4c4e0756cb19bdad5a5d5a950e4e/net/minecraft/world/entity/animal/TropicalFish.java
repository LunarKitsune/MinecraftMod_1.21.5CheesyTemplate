package net.minecraft.world.entity.animal;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;

public class TropicalFish extends AbstractSchoolingFish {
    public static final TropicalFish.Variant DEFAULT_VARIANT = new TropicalFish.Variant(TropicalFish.Pattern.KOB, DyeColor.WHITE, DyeColor.WHITE);
    private static final EntityDataAccessor<Integer> DATA_ID_TYPE_VARIANT = SynchedEntityData.defineId(TropicalFish.class, EntityDataSerializers.INT);
    public static final List<TropicalFish.Variant> COMMON_VARIANTS = List.of(
        new TropicalFish.Variant(TropicalFish.Pattern.STRIPEY, DyeColor.ORANGE, DyeColor.GRAY),
        new TropicalFish.Variant(TropicalFish.Pattern.FLOPPER, DyeColor.GRAY, DyeColor.GRAY),
        new TropicalFish.Variant(TropicalFish.Pattern.FLOPPER, DyeColor.GRAY, DyeColor.BLUE),
        new TropicalFish.Variant(TropicalFish.Pattern.CLAYFISH, DyeColor.WHITE, DyeColor.GRAY),
        new TropicalFish.Variant(TropicalFish.Pattern.SUNSTREAK, DyeColor.BLUE, DyeColor.GRAY),
        new TropicalFish.Variant(TropicalFish.Pattern.KOB, DyeColor.ORANGE, DyeColor.WHITE),
        new TropicalFish.Variant(TropicalFish.Pattern.SPOTTY, DyeColor.PINK, DyeColor.LIGHT_BLUE),
        new TropicalFish.Variant(TropicalFish.Pattern.BLOCKFISH, DyeColor.PURPLE, DyeColor.YELLOW),
        new TropicalFish.Variant(TropicalFish.Pattern.CLAYFISH, DyeColor.WHITE, DyeColor.RED),
        new TropicalFish.Variant(TropicalFish.Pattern.SPOTTY, DyeColor.WHITE, DyeColor.YELLOW),
        new TropicalFish.Variant(TropicalFish.Pattern.GLITTER, DyeColor.WHITE, DyeColor.GRAY),
        new TropicalFish.Variant(TropicalFish.Pattern.CLAYFISH, DyeColor.WHITE, DyeColor.ORANGE),
        new TropicalFish.Variant(TropicalFish.Pattern.DASHER, DyeColor.CYAN, DyeColor.PINK),
        new TropicalFish.Variant(TropicalFish.Pattern.BRINELY, DyeColor.LIME, DyeColor.LIGHT_BLUE),
        new TropicalFish.Variant(TropicalFish.Pattern.BETTY, DyeColor.RED, DyeColor.WHITE),
        new TropicalFish.Variant(TropicalFish.Pattern.SNOOPER, DyeColor.GRAY, DyeColor.RED),
        new TropicalFish.Variant(TropicalFish.Pattern.BLOCKFISH, DyeColor.RED, DyeColor.WHITE),
        new TropicalFish.Variant(TropicalFish.Pattern.FLOPPER, DyeColor.WHITE, DyeColor.YELLOW),
        new TropicalFish.Variant(TropicalFish.Pattern.KOB, DyeColor.RED, DyeColor.WHITE),
        new TropicalFish.Variant(TropicalFish.Pattern.SUNSTREAK, DyeColor.GRAY, DyeColor.WHITE),
        new TropicalFish.Variant(TropicalFish.Pattern.DASHER, DyeColor.CYAN, DyeColor.YELLOW),
        new TropicalFish.Variant(TropicalFish.Pattern.FLOPPER, DyeColor.YELLOW, DyeColor.YELLOW)
    );
    private boolean isSchool = true;

    public TropicalFish(EntityType<? extends TropicalFish> p_30015_, Level p_30016_) {
        super(p_30015_, p_30016_);
    }

    public static String getPredefinedName(int pVariantId) {
        return "entity.minecraft.tropical_fish.predefined." + pVariantId;
    }

    static int packVariant(TropicalFish.Pattern pPattern, DyeColor pBaseColor, DyeColor pPatternColor) {
        return pPattern.getPackedId() & 65535 | (pBaseColor.getId() & 0xFF) << 16 | (pPatternColor.getId() & 0xFF) << 24;
    }

    public static DyeColor getBaseColor(int pVariantId) {
        return DyeColor.byId(pVariantId >> 16 & 0xFF);
    }

    public static DyeColor getPatternColor(int pVariantId) {
        return DyeColor.byId(pVariantId >> 24 & 0xFF);
    }

    public static TropicalFish.Pattern getPattern(int pVariantId) {
        return TropicalFish.Pattern.byId(pVariantId & 65535);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder p_330187_) {
        super.defineSynchedData(p_330187_);
        p_330187_.define(DATA_ID_TYPE_VARIANT, DEFAULT_VARIANT.getPackedId());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.store("Variant", TropicalFish.Variant.CODEC, new TropicalFish.Variant(this.getPackedVariant()));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        TropicalFish.Variant tropicalfish$variant = pCompound.read("Variant", TropicalFish.Variant.CODEC).orElse(DEFAULT_VARIANT);
        this.setPackedVariant(tropicalfish$variant.getPackedId());
    }

    private void setPackedVariant(int pPackedVariant) {
        this.entityData.set(DATA_ID_TYPE_VARIANT, pPackedVariant);
    }

    @Override
    public boolean isMaxGroupSizeReached(int pSize) {
        return !this.isSchool;
    }

    private int getPackedVariant() {
        return this.entityData.get(DATA_ID_TYPE_VARIANT);
    }

    public DyeColor getBaseColor() {
        return getBaseColor(this.getPackedVariant());
    }

    public DyeColor getPatternColor() {
        return getPatternColor(this.getPackedVariant());
    }

    public TropicalFish.Pattern getPattern() {
        return getPattern(this.getPackedVariant());
    }

    private void setPattern(TropicalFish.Pattern pPattern) {
        int i = this.getPackedVariant();
        DyeColor dyecolor = getBaseColor(i);
        DyeColor dyecolor1 = getPatternColor(i);
        this.setPackedVariant(packVariant(pPattern, dyecolor, dyecolor1));
    }

    private void setBaseColor(DyeColor pBaseColor) {
        int i = this.getPackedVariant();
        TropicalFish.Pattern tropicalfish$pattern = getPattern(i);
        DyeColor dyecolor = getPatternColor(i);
        this.setPackedVariant(packVariant(tropicalfish$pattern, pBaseColor, dyecolor));
    }

    private void setPatternColor(DyeColor pPatternColor) {
        int i = this.getPackedVariant();
        TropicalFish.Pattern tropicalfish$pattern = getPattern(i);
        DyeColor dyecolor = getBaseColor(i);
        this.setPackedVariant(packVariant(tropicalfish$pattern, dyecolor, pPatternColor));
    }

    @Nullable
    @Override
    public <T> T get(DataComponentType<? extends T> p_396544_) {
        if (p_396544_ == DataComponents.TROPICAL_FISH_PATTERN) {
            return castComponentValue((DataComponentType<T>)p_396544_, this.getPattern());
        } else if (p_396544_ == DataComponents.TROPICAL_FISH_BASE_COLOR) {
            return castComponentValue((DataComponentType<T>)p_396544_, this.getBaseColor());
        } else {
            return p_396544_ == DataComponents.TROPICAL_FISH_PATTERN_COLOR ? castComponentValue((DataComponentType<T>)p_396544_, this.getPatternColor()) : super.get(p_396544_);
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter p_392532_) {
        this.applyImplicitComponentIfPresent(p_392532_, DataComponents.TROPICAL_FISH_PATTERN);
        this.applyImplicitComponentIfPresent(p_392532_, DataComponents.TROPICAL_FISH_BASE_COLOR);
        this.applyImplicitComponentIfPresent(p_392532_, DataComponents.TROPICAL_FISH_PATTERN_COLOR);
        super.applyImplicitComponents(p_392532_);
    }

    @Override
    protected <T> boolean applyImplicitComponent(DataComponentType<T> p_392959_, T p_397123_) {
        if (p_392959_ == DataComponents.TROPICAL_FISH_PATTERN) {
            this.setPattern(castComponentValue(DataComponents.TROPICAL_FISH_PATTERN, p_397123_));
            return true;
        } else if (p_392959_ == DataComponents.TROPICAL_FISH_BASE_COLOR) {
            this.setBaseColor(castComponentValue(DataComponents.TROPICAL_FISH_BASE_COLOR, p_397123_));
            return true;
        } else if (p_392959_ == DataComponents.TROPICAL_FISH_PATTERN_COLOR) {
            this.setPatternColor(castComponentValue(DataComponents.TROPICAL_FISH_PATTERN_COLOR, p_397123_));
            return true;
        } else {
            return super.applyImplicitComponent(p_392959_, p_397123_);
        }
    }

    @Override
    public void saveToBucketTag(ItemStack p_30049_) {
        super.saveToBucketTag(p_30049_);
        p_30049_.copyFrom(DataComponents.TROPICAL_FISH_PATTERN, this);
        p_30049_.copyFrom(DataComponents.TROPICAL_FISH_BASE_COLOR, this);
        p_30049_.copyFrom(DataComponents.TROPICAL_FISH_PATTERN_COLOR, this);
    }

    @Override
    public ItemStack getBucketItemStack() {
        return new ItemStack(Items.TROPICAL_FISH_BUCKET);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.TROPICAL_FISH_AMBIENT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.TROPICAL_FISH_DEATH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource pDamageSource) {
        return SoundEvents.TROPICAL_FISH_HURT;
    }

    @Override
    protected SoundEvent getFlopSound() {
        return SoundEvents.TROPICAL_FISH_FLOP;
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor p_30023_, DifficultyInstance p_30024_, EntitySpawnReason p_362994_, @Nullable SpawnGroupData p_30026_) {
        p_30026_ = super.finalizeSpawn(p_30023_, p_30024_, p_362994_, p_30026_);
        RandomSource randomsource = p_30023_.getRandom();
        TropicalFish.Variant tropicalfish$variant;
        if (p_30026_ instanceof TropicalFish.TropicalFishGroupData tropicalfish$tropicalfishgroupdata) {
            tropicalfish$variant = tropicalfish$tropicalfishgroupdata.variant;
        } else if (randomsource.nextFloat() < 0.9) {
            tropicalfish$variant = Util.getRandom(COMMON_VARIANTS, randomsource);
            p_30026_ = new TropicalFish.TropicalFishGroupData(this, tropicalfish$variant);
        } else {
            this.isSchool = false;
            TropicalFish.Pattern[] atropicalfish$pattern = TropicalFish.Pattern.values();
            DyeColor[] adyecolor = DyeColor.values();
            TropicalFish.Pattern tropicalfish$pattern = Util.getRandom(atropicalfish$pattern, randomsource);
            DyeColor dyecolor = Util.getRandom(adyecolor, randomsource);
            DyeColor dyecolor1 = Util.getRandom(adyecolor, randomsource);
            tropicalfish$variant = new TropicalFish.Variant(tropicalfish$pattern, dyecolor, dyecolor1);
        }

        this.setPackedVariant(tropicalfish$variant.getPackedId());
        return p_30026_;
    }

    public static boolean checkTropicalFishSpawnRules(
        EntityType<TropicalFish> pEntityType, LevelAccessor pLevel, EntitySpawnReason pSpawnReason, BlockPos pPos, RandomSource pRandom
    ) {
        return pLevel.getFluidState(pPos.below()).is(FluidTags.WATER)
            && pLevel.getBlockState(pPos.above()).is(Blocks.WATER)
            && (pLevel.getBiome(pPos).is(BiomeTags.ALLOWS_TROPICAL_FISH_SPAWNS_AT_ANY_HEIGHT) || WaterAnimal.checkSurfaceWaterAnimalSpawnRules(pEntityType, pLevel, pSpawnReason, pPos, pRandom));
    }

    public static enum Base {
        SMALL(0),
        LARGE(1);

        final int id;

        private Base(final int pId) {
            this.id = pId;
        }
    }

    public static enum Pattern implements StringRepresentable, TooltipProvider {
        KOB("kob", TropicalFish.Base.SMALL, 0),
        SUNSTREAK("sunstreak", TropicalFish.Base.SMALL, 1),
        SNOOPER("snooper", TropicalFish.Base.SMALL, 2),
        DASHER("dasher", TropicalFish.Base.SMALL, 3),
        BRINELY("brinely", TropicalFish.Base.SMALL, 4),
        SPOTTY("spotty", TropicalFish.Base.SMALL, 5),
        FLOPPER("flopper", TropicalFish.Base.LARGE, 0),
        STRIPEY("stripey", TropicalFish.Base.LARGE, 1),
        GLITTER("glitter", TropicalFish.Base.LARGE, 2),
        BLOCKFISH("blockfish", TropicalFish.Base.LARGE, 3),
        BETTY("betty", TropicalFish.Base.LARGE, 4),
        CLAYFISH("clayfish", TropicalFish.Base.LARGE, 5);

        public static final Codec<TropicalFish.Pattern> CODEC = StringRepresentable.fromEnum(TropicalFish.Pattern::values);
        private static final IntFunction<TropicalFish.Pattern> BY_ID = ByIdMap.sparse(TropicalFish.Pattern::getPackedId, values(), KOB);
        public static final StreamCodec<ByteBuf, TropicalFish.Pattern> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, TropicalFish.Pattern::getPackedId);
        private final String name;
        private final Component displayName;
        private final TropicalFish.Base base;
        private final int packedId;

        private Pattern(final String pName, final TropicalFish.Base pBase, final int pId) {
            this.name = pName;
            this.base = pBase;
            this.packedId = pBase.id | pId << 8;
            this.displayName = Component.translatable("entity.minecraft.tropical_fish.type." + this.name);
        }

        public static TropicalFish.Pattern byId(int pPackedId) {
            return BY_ID.apply(pPackedId);
        }

        public TropicalFish.Base base() {
            return this.base;
        }

        public int getPackedId() {
            return this.packedId;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        public Component displayName() {
            return this.displayName;
        }

        @Override
        public void addToTooltip(Item.TooltipContext p_394397_, Consumer<Component> p_395416_, TooltipFlag p_393156_, DataComponentGetter p_397784_) {
            DyeColor dyecolor = p_397784_.getOrDefault(DataComponents.TROPICAL_FISH_BASE_COLOR, TropicalFish.DEFAULT_VARIANT.baseColor());
            DyeColor dyecolor1 = p_397784_.getOrDefault(DataComponents.TROPICAL_FISH_PATTERN_COLOR, TropicalFish.DEFAULT_VARIANT.patternColor());
            ChatFormatting[] achatformatting = new ChatFormatting[]{ChatFormatting.ITALIC, ChatFormatting.GRAY};
            int i = TropicalFish.COMMON_VARIANTS.indexOf(new TropicalFish.Variant(this, dyecolor, dyecolor1));
            if (i != -1) {
                p_395416_.accept(Component.translatable(TropicalFish.getPredefinedName(i)).withStyle(achatformatting));
            } else {
                p_395416_.accept(this.displayName.plainCopy().withStyle(achatformatting));
                MutableComponent mutablecomponent = Component.translatable("color.minecraft." + dyecolor.getName());
                if (dyecolor != dyecolor1) {
                    mutablecomponent.append(", ").append(Component.translatable("color.minecraft." + dyecolor1.getName()));
                }

                mutablecomponent.withStyle(achatformatting);
                p_395416_.accept(mutablecomponent);
            }
        }
    }

    static class TropicalFishGroupData extends AbstractSchoolingFish.SchoolSpawnGroupData {
        final TropicalFish.Variant variant;

        TropicalFishGroupData(TropicalFish pLeader, TropicalFish.Variant pVariant) {
            super(pLeader);
            this.variant = pVariant;
        }
    }

    public record Variant(TropicalFish.Pattern pattern, DyeColor baseColor, DyeColor patternColor) {
        public static final Codec<TropicalFish.Variant> CODEC = Codec.INT.xmap(TropicalFish.Variant::new, TropicalFish.Variant::getPackedId);

        public Variant(int pId) {
            this(TropicalFish.getPattern(pId), TropicalFish.getBaseColor(pId), TropicalFish.getPatternColor(pId));
        }

        public int getPackedId() {
            return TropicalFish.packVariant(this.pattern, this.baseColor, this.patternColor);
        }
    }
}