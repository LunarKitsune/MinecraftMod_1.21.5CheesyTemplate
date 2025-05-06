package net.minecraft.world.entity;

import com.mojang.logging.LogUtils;
import com.mojang.math.Transformation;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.List;
import java.util.Optional;
import java.util.function.IntFunction;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ARGB;
import net.minecraft.util.Brightness;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.slf4j.Logger;

public abstract class Display extends Entity {
    static final Logger LOGGER = LogUtils.getLogger();
    public static final int NO_BRIGHTNESS_OVERRIDE = -1;
    private static final EntityDataAccessor<Integer> DATA_TRANSFORMATION_INTERPOLATION_START_DELTA_TICKS_ID = SynchedEntityData.defineId(Display.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TRANSFORMATION_INTERPOLATION_DURATION_ID = SynchedEntityData.defineId(Display.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_POS_ROT_INTERPOLATION_DURATION_ID = SynchedEntityData.defineId(Display.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Vector3f> DATA_TRANSLATION_ID = SynchedEntityData.defineId(Display.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Vector3f> DATA_SCALE_ID = SynchedEntityData.defineId(Display.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Quaternionf> DATA_LEFT_ROTATION_ID = SynchedEntityData.defineId(Display.class, EntityDataSerializers.QUATERNION);
    private static final EntityDataAccessor<Quaternionf> DATA_RIGHT_ROTATION_ID = SynchedEntityData.defineId(Display.class, EntityDataSerializers.QUATERNION);
    private static final EntityDataAccessor<Byte> DATA_BILLBOARD_RENDER_CONSTRAINTS_ID = SynchedEntityData.defineId(Display.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> DATA_BRIGHTNESS_OVERRIDE_ID = SynchedEntityData.defineId(Display.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_VIEW_RANGE_ID = SynchedEntityData.defineId(Display.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_SHADOW_RADIUS_ID = SynchedEntityData.defineId(Display.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_SHADOW_STRENGTH_ID = SynchedEntityData.defineId(Display.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_WIDTH_ID = SynchedEntityData.defineId(Display.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_HEIGHT_ID = SynchedEntityData.defineId(Display.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_GLOW_COLOR_OVERRIDE_ID = SynchedEntityData.defineId(Display.class, EntityDataSerializers.INT);
    private static final IntSet RENDER_STATE_IDS = IntSet.of(
        DATA_TRANSLATION_ID.id(),
        DATA_SCALE_ID.id(),
        DATA_LEFT_ROTATION_ID.id(),
        DATA_RIGHT_ROTATION_ID.id(),
        DATA_BILLBOARD_RENDER_CONSTRAINTS_ID.id(),
        DATA_BRIGHTNESS_OVERRIDE_ID.id(),
        DATA_SHADOW_RADIUS_ID.id(),
        DATA_SHADOW_STRENGTH_ID.id()
    );
    private static final int INITIAL_TRANSFORMATION_INTERPOLATION_DURATION = 0;
    private static final int INITIAL_TRANSFORMATION_START_INTERPOLATION = 0;
    private static final int INITIAL_POS_ROT_INTERPOLATION_DURATION = 0;
    private static final float INITIAL_SHADOW_RADIUS = 0.0F;
    private static final float INITIAL_SHADOW_STRENGTH = 1.0F;
    private static final float INITIAL_VIEW_RANGE = 1.0F;
    private static final float INITIAL_WIDTH = 0.0F;
    private static final float INITIAL_HEIGHT = 0.0F;
    private static final int NO_GLOW_COLOR_OVERRIDE = -1;
    public static final String TAG_POS_ROT_INTERPOLATION_DURATION = "teleport_duration";
    public static final String TAG_TRANSFORMATION_INTERPOLATION_DURATION = "interpolation_duration";
    public static final String TAG_TRANSFORMATION_START_INTERPOLATION = "start_interpolation";
    public static final String TAG_TRANSFORMATION = "transformation";
    public static final String TAG_BILLBOARD = "billboard";
    public static final String TAG_BRIGHTNESS = "brightness";
    public static final String TAG_VIEW_RANGE = "view_range";
    public static final String TAG_SHADOW_RADIUS = "shadow_radius";
    public static final String TAG_SHADOW_STRENGTH = "shadow_strength";
    public static final String TAG_WIDTH = "width";
    public static final String TAG_HEIGHT = "height";
    public static final String TAG_GLOW_COLOR_OVERRIDE = "glow_color_override";
    private long interpolationStartClientTick = -2147483648L;
    private int interpolationDuration;
    private float lastProgress;
    private AABB cullingBoundingBox;
    private boolean noCulling = true;
    protected boolean updateRenderState;
    private boolean updateStartTick;
    private boolean updateInterpolationDuration;
    @Nullable
    private Display.RenderState renderState;
    private final InterpolationHandler interpolation = new InterpolationHandler(this, 0);

    public Display(EntityType<?> p_270360_, Level p_270280_) {
        super(p_270360_, p_270280_);
        this.noPhysics = true;
        this.cullingBoundingBox = this.getBoundingBox();
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> p_270275_) {
        super.onSyncedDataUpdated(p_270275_);
        if (DATA_HEIGHT_ID.equals(p_270275_) || DATA_WIDTH_ID.equals(p_270275_)) {
            this.updateCulling();
        }

        if (DATA_TRANSFORMATION_INTERPOLATION_START_DELTA_TICKS_ID.equals(p_270275_)) {
            this.updateStartTick = true;
        }

        if (DATA_POS_ROT_INTERPOLATION_DURATION_ID.equals(p_270275_)) {
            this.interpolation.setInterpolationLength(this.getPosRotInterpolationDuration());
        }

        if (DATA_TRANSFORMATION_INTERPOLATION_DURATION_ID.equals(p_270275_)) {
            this.updateInterpolationDuration = true;
        }

        if (RENDER_STATE_IDS.contains(p_270275_.id())) {
            this.updateRenderState = true;
        }
    }

    @Override
    public final boolean hurtServer(ServerLevel p_365251_, DamageSource p_369787_, float p_367342_) {
        return false;
    }

    private static Transformation createTransformation(SynchedEntityData pSynchedEntityData) {
        Vector3f vector3f = pSynchedEntityData.get(DATA_TRANSLATION_ID);
        Quaternionf quaternionf = pSynchedEntityData.get(DATA_LEFT_ROTATION_ID);
        Vector3f vector3f1 = pSynchedEntityData.get(DATA_SCALE_ID);
        Quaternionf quaternionf1 = pSynchedEntityData.get(DATA_RIGHT_ROTATION_ID);
        return new Transformation(vector3f, quaternionf, vector3f1, quaternionf1);
    }

    @Override
    public void tick() {
        Entity entity = this.getVehicle();
        if (entity != null && entity.isRemoved()) {
            this.stopRiding();
        }

        if (this.level().isClientSide) {
            if (this.updateStartTick) {
                this.updateStartTick = false;
                int i = this.getTransformationInterpolationDelay();
                this.interpolationStartClientTick = this.tickCount + i;
            }

            if (this.updateInterpolationDuration) {
                this.updateInterpolationDuration = false;
                this.interpolationDuration = this.getTransformationInterpolationDuration();
            }

            if (this.updateRenderState) {
                this.updateRenderState = false;
                boolean flag = this.interpolationDuration != 0;
                if (flag && this.renderState != null) {
                    this.renderState = this.createInterpolatedRenderState(this.renderState, this.lastProgress);
                } else {
                    this.renderState = this.createFreshRenderState();
                }

                this.updateRenderSubState(flag, this.lastProgress);
            }

            this.interpolation.interpolate();
        }
    }

    @Override
    public InterpolationHandler getInterpolation() {
        return this.interpolation;
    }

    protected abstract void updateRenderSubState(boolean pInterpolate, float pPartialTick);

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder p_327982_) {
        p_327982_.define(DATA_POS_ROT_INTERPOLATION_DURATION_ID, 0);
        p_327982_.define(DATA_TRANSFORMATION_INTERPOLATION_START_DELTA_TICKS_ID, 0);
        p_327982_.define(DATA_TRANSFORMATION_INTERPOLATION_DURATION_ID, 0);
        p_327982_.define(DATA_TRANSLATION_ID, new Vector3f());
        p_327982_.define(DATA_SCALE_ID, new Vector3f(1.0F, 1.0F, 1.0F));
        p_327982_.define(DATA_RIGHT_ROTATION_ID, new Quaternionf());
        p_327982_.define(DATA_LEFT_ROTATION_ID, new Quaternionf());
        p_327982_.define(DATA_BILLBOARD_RENDER_CONSTRAINTS_ID, Display.BillboardConstraints.FIXED.getId());
        p_327982_.define(DATA_BRIGHTNESS_OVERRIDE_ID, -1);
        p_327982_.define(DATA_VIEW_RANGE_ID, 1.0F);
        p_327982_.define(DATA_SHADOW_RADIUS_ID, 0.0F);
        p_327982_.define(DATA_SHADOW_STRENGTH_ID, 1.0F);
        p_327982_.define(DATA_WIDTH_ID, 0.0F);
        p_327982_.define(DATA_HEIGHT_ID, 0.0F);
        p_327982_.define(DATA_GLOW_COLOR_OVERRIDE_ID, -1);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag p_270854_) {
        this.setTransformation(p_270854_.read("transformation", Transformation.EXTENDED_CODEC).orElse(Transformation.identity()));
        this.setTransformationInterpolationDuration(p_270854_.getIntOr("interpolation_duration", 0));
        this.setTransformationInterpolationDelay(p_270854_.getIntOr("start_interpolation", 0));
        int i = p_270854_.getIntOr("teleport_duration", 0);
        this.setPosRotInterpolationDuration(Mth.clamp(i, 0, 59));
        this.setBillboardConstraints(p_270854_.read("billboard", Display.BillboardConstraints.CODEC).orElse(Display.BillboardConstraints.FIXED));
        this.setViewRange(p_270854_.getFloatOr("view_range", 1.0F));
        this.setShadowRadius(p_270854_.getFloatOr("shadow_radius", 0.0F));
        this.setShadowStrength(p_270854_.getFloatOr("shadow_strength", 1.0F));
        this.setWidth(p_270854_.getFloatOr("width", 0.0F));
        this.setHeight(p_270854_.getFloatOr("height", 0.0F));
        this.setGlowColorOverride(p_270854_.getIntOr("glow_color_override", -1));
        this.setBrightnessOverride(p_270854_.read("brightness", Brightness.CODEC).orElse(null));
    }

    private void setTransformation(Transformation pTransformation) {
        this.entityData.set(DATA_TRANSLATION_ID, pTransformation.getTranslation());
        this.entityData.set(DATA_LEFT_ROTATION_ID, pTransformation.getLeftRotation());
        this.entityData.set(DATA_SCALE_ID, pTransformation.getScale());
        this.entityData.set(DATA_RIGHT_ROTATION_ID, pTransformation.getRightRotation());
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag p_270779_) {
        p_270779_.store("transformation", Transformation.EXTENDED_CODEC, createTransformation(this.entityData));
        p_270779_.store("billboard", Display.BillboardConstraints.CODEC, this.getBillboardConstraints());
        p_270779_.putInt("interpolation_duration", this.getTransformationInterpolationDuration());
        p_270779_.putInt("teleport_duration", this.getPosRotInterpolationDuration());
        p_270779_.putFloat("view_range", this.getViewRange());
        p_270779_.putFloat("shadow_radius", this.getShadowRadius());
        p_270779_.putFloat("shadow_strength", this.getShadowStrength());
        p_270779_.putFloat("width", this.getWidth());
        p_270779_.putFloat("height", this.getHeight());
        p_270779_.putInt("glow_color_override", this.getGlowColorOverride());
        p_270779_.storeNullable("brightness", Brightness.CODEC, this.getBrightnessOverride());
    }

    public AABB getBoundingBoxForCulling() {
        return this.cullingBoundingBox;
    }

    public boolean affectedByCulling() {
        return !this.noCulling;
    }

    @Override
    public PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE;
    }

    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }

    @Nullable
    public Display.RenderState renderState() {
        return this.renderState;
    }

    private void setTransformationInterpolationDuration(int pTransformationInterpolationDuration) {
        this.entityData.set(DATA_TRANSFORMATION_INTERPOLATION_DURATION_ID, pTransformationInterpolationDuration);
    }

    private int getTransformationInterpolationDuration() {
        return this.entityData.get(DATA_TRANSFORMATION_INTERPOLATION_DURATION_ID);
    }

    private void setTransformationInterpolationDelay(int pTransformationInterpolationDelay) {
        this.entityData.set(DATA_TRANSFORMATION_INTERPOLATION_START_DELTA_TICKS_ID, pTransformationInterpolationDelay, true);
    }

    private int getTransformationInterpolationDelay() {
        return this.entityData.get(DATA_TRANSFORMATION_INTERPOLATION_START_DELTA_TICKS_ID);
    }

    private void setPosRotInterpolationDuration(int pPosRotInterpolationDuration) {
        this.entityData.set(DATA_POS_ROT_INTERPOLATION_DURATION_ID, pPosRotInterpolationDuration);
    }

    private int getPosRotInterpolationDuration() {
        return this.entityData.get(DATA_POS_ROT_INTERPOLATION_DURATION_ID);
    }

    private void setBillboardConstraints(Display.BillboardConstraints pBillboardConstraints) {
        this.entityData.set(DATA_BILLBOARD_RENDER_CONSTRAINTS_ID, pBillboardConstraints.getId());
    }

    private Display.BillboardConstraints getBillboardConstraints() {
        return Display.BillboardConstraints.BY_ID.apply(this.entityData.get(DATA_BILLBOARD_RENDER_CONSTRAINTS_ID));
    }

    private void setBrightnessOverride(@Nullable Brightness pBrightnessOverride) {
        this.entityData.set(DATA_BRIGHTNESS_OVERRIDE_ID, pBrightnessOverride != null ? pBrightnessOverride.pack() : -1);
    }

    @Nullable
    private Brightness getBrightnessOverride() {
        int i = this.entityData.get(DATA_BRIGHTNESS_OVERRIDE_ID);
        return i != -1 ? Brightness.unpack(i) : null;
    }

    private int getPackedBrightnessOverride() {
        return this.entityData.get(DATA_BRIGHTNESS_OVERRIDE_ID);
    }

    private void setViewRange(float pViewRange) {
        this.entityData.set(DATA_VIEW_RANGE_ID, pViewRange);
    }

    private float getViewRange() {
        return this.entityData.get(DATA_VIEW_RANGE_ID);
    }

    private void setShadowRadius(float pShadowRadius) {
        this.entityData.set(DATA_SHADOW_RADIUS_ID, pShadowRadius);
    }

    private float getShadowRadius() {
        return this.entityData.get(DATA_SHADOW_RADIUS_ID);
    }

    private void setShadowStrength(float pShadowStrength) {
        this.entityData.set(DATA_SHADOW_STRENGTH_ID, pShadowStrength);
    }

    private float getShadowStrength() {
        return this.entityData.get(DATA_SHADOW_STRENGTH_ID);
    }

    private void setWidth(float pWidth) {
        this.entityData.set(DATA_WIDTH_ID, pWidth);
    }

    private float getWidth() {
        return this.entityData.get(DATA_WIDTH_ID);
    }

    private void setHeight(float pHeight) {
        this.entityData.set(DATA_HEIGHT_ID, pHeight);
    }

    private int getGlowColorOverride() {
        return this.entityData.get(DATA_GLOW_COLOR_OVERRIDE_ID);
    }

    private void setGlowColorOverride(int pGlowColorOverride) {
        this.entityData.set(DATA_GLOW_COLOR_OVERRIDE_ID, pGlowColorOverride);
    }

    public float calculateInterpolationProgress(float pPartialTick) {
        int i = this.interpolationDuration;
        if (i <= 0) {
            return 1.0F;
        } else {
            float f = (float)(this.tickCount - this.interpolationStartClientTick);
            float f1 = f + pPartialTick;
            float f2 = Mth.clamp(Mth.inverseLerp(f1, 0.0F, i), 0.0F, 1.0F);
            this.lastProgress = f2;
            return f2;
        }
    }

    private float getHeight() {
        return this.entityData.get(DATA_HEIGHT_ID);
    }

    @Override
    public void setPos(double p_270091_, double p_270983_, double p_270419_) {
        super.setPos(p_270091_, p_270983_, p_270419_);
        this.updateCulling();
    }

    private void updateCulling() {
        float f = this.getWidth();
        float f1 = this.getHeight();
        this.noCulling = f == 0.0F || f1 == 0.0F;
        float f2 = f / 2.0F;
        double d0 = this.getX();
        double d1 = this.getY();
        double d2 = this.getZ();
        this.cullingBoundingBox = new AABB(d0 - f2, d1, d2 - f2, d0 + f2, d1 + f1, d2 + f2);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double p_270991_) {
        return p_270991_ < Mth.square(this.getViewRange() * 64.0 * getViewScale());
    }

    @Override
    public int getTeamColor() {
        int i = this.getGlowColorOverride();
        return i != -1 ? i : super.getTeamColor();
    }

    private Display.RenderState createFreshRenderState() {
        return new Display.RenderState(
            Display.GenericInterpolator.constant(createTransformation(this.entityData)),
            this.getBillboardConstraints(),
            this.getPackedBrightnessOverride(),
            Display.FloatInterpolator.constant(this.getShadowRadius()),
            Display.FloatInterpolator.constant(this.getShadowStrength()),
            this.getGlowColorOverride()
        );
    }

    private Display.RenderState createInterpolatedRenderState(Display.RenderState pRenderState, float pPartialTick) {
        Transformation transformation = pRenderState.transformation.get(pPartialTick);
        float f = pRenderState.shadowRadius.get(pPartialTick);
        float f1 = pRenderState.shadowStrength.get(pPartialTick);
        return new Display.RenderState(
            new Display.TransformationInterpolator(transformation, createTransformation(this.entityData)),
            this.getBillboardConstraints(),
            this.getPackedBrightnessOverride(),
            new Display.LinearFloatInterpolator(f, this.getShadowRadius()),
            new Display.LinearFloatInterpolator(f1, this.getShadowStrength()),
            this.getGlowColorOverride()
        );
    }

    public static enum BillboardConstraints implements StringRepresentable {
        FIXED((byte)0, "fixed"),
        VERTICAL((byte)1, "vertical"),
        HORIZONTAL((byte)2, "horizontal"),
        CENTER((byte)3, "center");

        public static final Codec<Display.BillboardConstraints> CODEC = StringRepresentable.fromEnum(Display.BillboardConstraints::values);
        public static final IntFunction<Display.BillboardConstraints> BY_ID = ByIdMap.continuous(
            Display.BillboardConstraints::getId, values(), ByIdMap.OutOfBoundsStrategy.ZERO
        );
        private final byte id;
        private final String name;

        private BillboardConstraints(final byte pId, final String pName) {
            this.name = pName;
            this.id = pId;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        byte getId() {
            return this.id;
        }
    }

    public static class BlockDisplay extends Display {
        public static final String TAG_BLOCK_STATE = "block_state";
        private static final EntityDataAccessor<BlockState> DATA_BLOCK_STATE_ID = SynchedEntityData.defineId(Display.BlockDisplay.class, EntityDataSerializers.BLOCK_STATE);
        @Nullable
        private Display.BlockDisplay.BlockRenderState blockRenderState;

        public BlockDisplay(EntityType<?> p_271022_, Level p_270442_) {
            super(p_271022_, p_270442_);
        }

        @Override
        protected void defineSynchedData(SynchedEntityData.Builder p_329731_) {
            super.defineSynchedData(p_329731_);
            p_329731_.define(DATA_BLOCK_STATE_ID, Blocks.AIR.defaultBlockState());
        }

        @Override
        public void onSyncedDataUpdated(EntityDataAccessor<?> p_277476_) {
            super.onSyncedDataUpdated(p_277476_);
            if (p_277476_.equals(DATA_BLOCK_STATE_ID)) {
                this.updateRenderState = true;
            }
        }

        private BlockState getBlockState() {
            return this.entityData.get(DATA_BLOCK_STATE_ID);
        }

        private void setBlockState(BlockState pBlockState) {
            this.entityData.set(DATA_BLOCK_STATE_ID, pBlockState);
        }

        @Override
        protected void readAdditionalSaveData(CompoundTag p_270139_) {
            super.readAdditionalSaveData(p_270139_);
            RegistryOps<Tag> registryops = this.registryAccess().createSerializationContext(NbtOps.INSTANCE);
            this.setBlockState(p_270139_.read("block_state", BlockState.CODEC, registryops).orElse(Blocks.AIR.defaultBlockState()));
        }

        @Override
        protected void addAdditionalSaveData(CompoundTag p_270469_) {
            super.addAdditionalSaveData(p_270469_);
            RegistryOps<Tag> registryops = this.registryAccess().createSerializationContext(NbtOps.INSTANCE);
            p_270469_.store("block_state", BlockState.CODEC, registryops, this.getBlockState());
        }

        @Nullable
        public Display.BlockDisplay.BlockRenderState blockRenderState() {
            return this.blockRenderState;
        }

        @Override
        protected void updateRenderSubState(boolean p_277802_, float p_277688_) {
            this.blockRenderState = new Display.BlockDisplay.BlockRenderState(this.getBlockState());
        }

        public record BlockRenderState(BlockState blockState) {
        }
    }

    record ColorInterpolator(int previous, int current) implements Display.IntInterpolator {
        @Override
        public int get(float p_278012_) {
            return ARGB.lerp(p_278012_, this.previous, this.current);
        }
    }

    @FunctionalInterface
    public interface FloatInterpolator {
        static Display.FloatInterpolator constant(float pValue) {
            return p_278040_ -> pValue;
        }

        float get(float pPartialTick);
    }

    @FunctionalInterface
    public interface GenericInterpolator<T> {
        static <T> Display.GenericInterpolator<T> constant(T pValue) {
            return p_277907_ -> pValue;
        }

        T get(float pPartialTick);
    }

    @FunctionalInterface
    public interface IntInterpolator {
        static Display.IntInterpolator constant(int pValue) {
            return p_277356_ -> pValue;
        }

        int get(float pPartialTick);
    }

    public static class ItemDisplay extends Display {
        private static final String TAG_ITEM = "item";
        private static final String TAG_ITEM_DISPLAY = "item_display";
        private static final EntityDataAccessor<ItemStack> DATA_ITEM_STACK_ID = SynchedEntityData.defineId(Display.ItemDisplay.class, EntityDataSerializers.ITEM_STACK);
        private static final EntityDataAccessor<Byte> DATA_ITEM_DISPLAY_ID = SynchedEntityData.defineId(Display.ItemDisplay.class, EntityDataSerializers.BYTE);
        private final SlotAccess slot = SlotAccess.of(this::getItemStack, this::setItemStack);
        @Nullable
        private Display.ItemDisplay.ItemRenderState itemRenderState;

        public ItemDisplay(EntityType<?> p_270104_, Level p_270735_) {
            super(p_270104_, p_270735_);
        }

        @Override
        protected void defineSynchedData(SynchedEntityData.Builder p_332308_) {
            super.defineSynchedData(p_332308_);
            p_332308_.define(DATA_ITEM_STACK_ID, ItemStack.EMPTY);
            p_332308_.define(DATA_ITEM_DISPLAY_ID, ItemDisplayContext.NONE.getId());
        }

        @Override
        public void onSyncedDataUpdated(EntityDataAccessor<?> p_277793_) {
            super.onSyncedDataUpdated(p_277793_);
            if (DATA_ITEM_STACK_ID.equals(p_277793_) || DATA_ITEM_DISPLAY_ID.equals(p_277793_)) {
                this.updateRenderState = true;
            }
        }

        private ItemStack getItemStack() {
            return this.entityData.get(DATA_ITEM_STACK_ID);
        }

        private void setItemStack(ItemStack pItemStack) {
            this.entityData.set(DATA_ITEM_STACK_ID, pItemStack);
        }

        private void setItemTransform(ItemDisplayContext pItemTransform) {
            this.entityData.set(DATA_ITEM_DISPLAY_ID, pItemTransform.getId());
        }

        private ItemDisplayContext getItemTransform() {
            return ItemDisplayContext.BY_ID.apply(this.entityData.get(DATA_ITEM_DISPLAY_ID));
        }

        @Override
        protected void readAdditionalSaveData(CompoundTag p_270713_) {
            super.readAdditionalSaveData(p_270713_);
            RegistryOps<Tag> registryops = this.registryAccess().createSerializationContext(NbtOps.INSTANCE);
            this.setItemStack(p_270713_.read("item", ItemStack.CODEC, registryops).orElse(ItemStack.EMPTY));
            this.setItemTransform(p_270713_.read("item_display", ItemDisplayContext.CODEC).orElse(ItemDisplayContext.NONE));
        }

        @Override
        protected void addAdditionalSaveData(CompoundTag p_270669_) {
            super.addAdditionalSaveData(p_270669_);
            if (!this.getItemStack().isEmpty()) {
                p_270669_.store("item", ItemStack.CODEC, this.registryAccess().createSerializationContext(NbtOps.INSTANCE), this.getItemStack());
            }

            p_270669_.store("item_display", ItemDisplayContext.CODEC, this.getItemTransform());
        }

        @Override
        public SlotAccess getSlot(int p_270599_) {
            return p_270599_ == 0 ? this.slot : SlotAccess.NULL;
        }

        @Nullable
        public Display.ItemDisplay.ItemRenderState itemRenderState() {
            return this.itemRenderState;
        }

        @Override
        protected void updateRenderSubState(boolean p_277976_, float p_277708_) {
            ItemStack itemstack = this.getItemStack();
            itemstack.setEntityRepresentation(this);
            this.itemRenderState = new Display.ItemDisplay.ItemRenderState(itemstack, this.getItemTransform());
        }

        public record ItemRenderState(ItemStack itemStack, ItemDisplayContext itemTransform) {
        }
    }

    record LinearFloatInterpolator(float previous, float current) implements Display.FloatInterpolator {
        @Override
        public float get(float p_277511_) {
            return Mth.lerp(p_277511_, this.previous, this.current);
        }
    }

    record LinearIntInterpolator(int previous, int current) implements Display.IntInterpolator {
        @Override
        public int get(float p_277960_) {
            return Mth.lerpInt(p_277960_, this.previous, this.current);
        }
    }

    public record RenderState(
        Display.GenericInterpolator<Transformation> transformation,
        Display.BillboardConstraints billboardConstraints,
        int brightnessOverride,
        Display.FloatInterpolator shadowRadius,
        Display.FloatInterpolator shadowStrength,
        int glowColorOverride
    ) {
    }

    public static class TextDisplay extends Display {
        public static final String TAG_TEXT = "text";
        private static final String TAG_LINE_WIDTH = "line_width";
        private static final String TAG_TEXT_OPACITY = "text_opacity";
        private static final String TAG_BACKGROUND_COLOR = "background";
        private static final String TAG_SHADOW = "shadow";
        private static final String TAG_SEE_THROUGH = "see_through";
        private static final String TAG_USE_DEFAULT_BACKGROUND = "default_background";
        private static final String TAG_ALIGNMENT = "alignment";
        public static final byte FLAG_SHADOW = 1;
        public static final byte FLAG_SEE_THROUGH = 2;
        public static final byte FLAG_USE_DEFAULT_BACKGROUND = 4;
        public static final byte FLAG_ALIGN_LEFT = 8;
        public static final byte FLAG_ALIGN_RIGHT = 16;
        private static final byte INITIAL_TEXT_OPACITY = -1;
        public static final int INITIAL_BACKGROUND = 1073741824;
        private static final int INITIAL_LINE_WIDTH = 200;
        private static final EntityDataAccessor<Component> DATA_TEXT_ID = SynchedEntityData.defineId(Display.TextDisplay.class, EntityDataSerializers.COMPONENT);
        private static final EntityDataAccessor<Integer> DATA_LINE_WIDTH_ID = SynchedEntityData.defineId(Display.TextDisplay.class, EntityDataSerializers.INT);
        private static final EntityDataAccessor<Integer> DATA_BACKGROUND_COLOR_ID = SynchedEntityData.defineId(Display.TextDisplay.class, EntityDataSerializers.INT);
        private static final EntityDataAccessor<Byte> DATA_TEXT_OPACITY_ID = SynchedEntityData.defineId(Display.TextDisplay.class, EntityDataSerializers.BYTE);
        private static final EntityDataAccessor<Byte> DATA_STYLE_FLAGS_ID = SynchedEntityData.defineId(Display.TextDisplay.class, EntityDataSerializers.BYTE);
        private static final IntSet TEXT_RENDER_STATE_IDS = IntSet.of(
            DATA_TEXT_ID.id(), DATA_LINE_WIDTH_ID.id(), DATA_BACKGROUND_COLOR_ID.id(), DATA_TEXT_OPACITY_ID.id(), DATA_STYLE_FLAGS_ID.id()
        );
        @Nullable
        private Display.TextDisplay.CachedInfo clientDisplayCache;
        @Nullable
        private Display.TextDisplay.TextRenderState textRenderState;

        public TextDisplay(EntityType<?> p_270708_, Level p_270736_) {
            super(p_270708_, p_270736_);
        }

        @Override
        protected void defineSynchedData(SynchedEntityData.Builder p_330653_) {
            super.defineSynchedData(p_330653_);
            p_330653_.define(DATA_TEXT_ID, Component.empty());
            p_330653_.define(DATA_LINE_WIDTH_ID, 200);
            p_330653_.define(DATA_BACKGROUND_COLOR_ID, 1073741824);
            p_330653_.define(DATA_TEXT_OPACITY_ID, (byte)-1);
            p_330653_.define(DATA_STYLE_FLAGS_ID, (byte)0);
        }

        @Override
        public void onSyncedDataUpdated(EntityDataAccessor<?> p_270797_) {
            super.onSyncedDataUpdated(p_270797_);
            if (TEXT_RENDER_STATE_IDS.contains(p_270797_.id())) {
                this.updateRenderState = true;
            }
        }

        private Component getText() {
            return this.entityData.get(DATA_TEXT_ID);
        }

        private void setText(Component pText) {
            this.entityData.set(DATA_TEXT_ID, pText);
        }

        private int getLineWidth() {
            return this.entityData.get(DATA_LINE_WIDTH_ID);
        }

        private void setLineWidth(int pLineWidth) {
            this.entityData.set(DATA_LINE_WIDTH_ID, pLineWidth);
        }

        private byte getTextOpacity() {
            return this.entityData.get(DATA_TEXT_OPACITY_ID);
        }

        private void setTextOpacity(byte pTextOpacity) {
            this.entityData.set(DATA_TEXT_OPACITY_ID, pTextOpacity);
        }

        private int getBackgroundColor() {
            return this.entityData.get(DATA_BACKGROUND_COLOR_ID);
        }

        private void setBackgroundColor(int pBackgroundColor) {
            this.entityData.set(DATA_BACKGROUND_COLOR_ID, pBackgroundColor);
        }

        private byte getFlags() {
            return this.entityData.get(DATA_STYLE_FLAGS_ID);
        }

        private void setFlags(byte pFlags) {
            this.entityData.set(DATA_STYLE_FLAGS_ID, pFlags);
        }

        private static byte loadFlag(byte pCurrentValue, CompoundTag pTag, String pFlag, byte pMask) {
            return pTag.getBooleanOr(pFlag, false) ? (byte)(pCurrentValue | pMask) : pCurrentValue;
        }

        @Override
        protected void readAdditionalSaveData(CompoundTag p_270714_) {
            super.readAdditionalSaveData(p_270714_);
            this.setLineWidth(p_270714_.getIntOr("line_width", 200));
            this.setTextOpacity(p_270714_.getByteOr("text_opacity", (byte)-1));
            this.setBackgroundColor(p_270714_.getIntOr("background", 1073741824));
            byte b0 = loadFlag((byte)0, p_270714_, "shadow", (byte)1);
            b0 = loadFlag(b0, p_270714_, "see_through", (byte)2);
            b0 = loadFlag(b0, p_270714_, "default_background", (byte)4);
            Optional<Display.TextDisplay.Align> optional = p_270714_.read("alignment", Display.TextDisplay.Align.CODEC);
            if (optional.isPresent()) {
                b0 = switch ((Display.TextDisplay.Align)optional.get()) {
                    case CENTER -> b0;
                    case LEFT -> (byte)(b0 | 8);
                    case RIGHT -> (byte)(b0 | 16);
                };
            }

            this.setFlags(b0);
            Tag tag = p_270714_.get("text");
            if (tag != null) {
                RegistryOps<Tag> registryops = this.registryAccess().createSerializationContext(NbtOps.INSTANCE);

                try {
                    Component component = ComponentSerialization.CODEC.parse(registryops, tag).getOrThrow();
                    if (component != null && this.level() instanceof ServerLevel serverlevel) {
                        CommandSourceStack commandsourcestack = this.createCommandSourceStackForNameResolution(serverlevel).withPermission(2);
                        Component component1 = ComponentUtils.updateForEntity(commandsourcestack, component, this, 0);
                        this.setText(component1);
                    } else {
                        this.setText(Component.empty());
                    }
                } catch (Exception exception) {
                    Display.LOGGER.warn("Failed to parse display entity text {}", tag, exception);
                }
            }
        }

        private static void storeFlag(byte pCurrentValue, CompoundTag pTag, String pFlag, byte pMask) {
            pTag.putBoolean(pFlag, (pCurrentValue & pMask) != 0);
        }

        @Override
        protected void addAdditionalSaveData(CompoundTag p_270268_) {
            super.addAdditionalSaveData(p_270268_);
            RegistryOps<Tag> registryops = this.registryAccess().createSerializationContext(NbtOps.INSTANCE);
            p_270268_.store("text", ComponentSerialization.CODEC, registryops, this.getText());
            p_270268_.putInt("line_width", this.getLineWidth());
            p_270268_.putInt("background", this.getBackgroundColor());
            p_270268_.putByte("text_opacity", this.getTextOpacity());
            byte b0 = this.getFlags();
            storeFlag(b0, p_270268_, "shadow", (byte)1);
            storeFlag(b0, p_270268_, "see_through", (byte)2);
            storeFlag(b0, p_270268_, "default_background", (byte)4);
            p_270268_.store("alignment", Display.TextDisplay.Align.CODEC, getAlign(b0));
        }

        @Override
        protected void updateRenderSubState(boolean p_277565_, float p_277967_) {
            if (p_277565_ && this.textRenderState != null) {
                this.textRenderState = this.createInterpolatedTextRenderState(this.textRenderState, p_277967_);
            } else {
                this.textRenderState = this.createFreshTextRenderState();
            }

            this.clientDisplayCache = null;
        }

        @Nullable
        public Display.TextDisplay.TextRenderState textRenderState() {
            return this.textRenderState;
        }

        private Display.TextDisplay.TextRenderState createFreshTextRenderState() {
            return new Display.TextDisplay.TextRenderState(
                this.getText(),
                this.getLineWidth(),
                Display.IntInterpolator.constant(this.getTextOpacity()),
                Display.IntInterpolator.constant(this.getBackgroundColor()),
                this.getFlags()
            );
        }

        private Display.TextDisplay.TextRenderState createInterpolatedTextRenderState(Display.TextDisplay.TextRenderState pRenderState, float pPartialTick) {
            int i = pRenderState.backgroundColor.get(pPartialTick);
            int j = pRenderState.textOpacity.get(pPartialTick);
            return new Display.TextDisplay.TextRenderState(
                this.getText(),
                this.getLineWidth(),
                new Display.LinearIntInterpolator(j, this.getTextOpacity()),
                new Display.ColorInterpolator(i, this.getBackgroundColor()),
                this.getFlags()
            );
        }

        public Display.TextDisplay.CachedInfo cacheDisplay(Display.TextDisplay.LineSplitter pSplitter) {
            if (this.clientDisplayCache == null) {
                if (this.textRenderState != null) {
                    this.clientDisplayCache = pSplitter.split(this.textRenderState.text(), this.textRenderState.lineWidth());
                } else {
                    this.clientDisplayCache = new Display.TextDisplay.CachedInfo(List.of(), 0);
                }
            }

            return this.clientDisplayCache;
        }

        public static Display.TextDisplay.Align getAlign(byte pFlags) {
            if ((pFlags & 8) != 0) {
                return Display.TextDisplay.Align.LEFT;
            } else {
                return (pFlags & 16) != 0 ? Display.TextDisplay.Align.RIGHT : Display.TextDisplay.Align.CENTER;
            }
        }

        public static enum Align implements StringRepresentable {
            CENTER("center"),
            LEFT("left"),
            RIGHT("right");

            public static final Codec<Display.TextDisplay.Align> CODEC = StringRepresentable.fromEnum(Display.TextDisplay.Align::values);
            private final String name;

            private Align(final String pName) {
                this.name = pName;
            }

            @Override
            public String getSerializedName() {
                return this.name;
            }
        }

        public record CachedInfo(List<Display.TextDisplay.CachedLine> lines, int width) {
        }

        public record CachedLine(FormattedCharSequence contents, int width) {
        }

        @FunctionalInterface
        public interface LineSplitter {
            Display.TextDisplay.CachedInfo split(Component pText, int pMaxWidth);
        }

        public record TextRenderState(Component text, int lineWidth, Display.IntInterpolator textOpacity, Display.IntInterpolator backgroundColor, byte flags) {
        }
    }

    record TransformationInterpolator(Transformation previous, Transformation current) implements Display.GenericInterpolator<Transformation> {
        public Transformation get(float p_278027_) {
            return p_278027_ >= 1.0 ? this.current : this.previous.slerp(this.current, p_278027_);
        }
    }
}