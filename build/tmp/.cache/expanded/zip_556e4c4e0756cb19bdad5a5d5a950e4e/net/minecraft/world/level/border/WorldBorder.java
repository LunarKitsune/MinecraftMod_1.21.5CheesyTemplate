package net.minecraft.world.level.border;

import com.google.common.collect.Lists;
import com.mojang.serialization.DynamicLike;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class WorldBorder {
    public static final double MAX_SIZE = 5.999997E7F;
    public static final double MAX_CENTER_COORDINATE = 2.9999984E7;
    private final List<BorderChangeListener> listeners = Lists.newArrayList();
    private double damagePerBlock = 0.2;
    private double damageSafeZone = 5.0;
    private int warningTime = 15;
    private int warningBlocks = 5;
    private double centerX;
    private double centerZ;
    int absoluteMaxSize = 29999984;
    private WorldBorder.BorderExtent extent = new WorldBorder.StaticBorderExtent(5.999997E7F);
    public static final WorldBorder.Settings DEFAULT_SETTINGS = new WorldBorder.Settings(0.0, 0.0, 0.2, 5.0, 5, 15, 5.999997E7F, 0L, 0.0);

    public boolean isWithinBounds(BlockPos pPos) {
        return this.isWithinBounds(pPos.getX(), pPos.getZ());
    }

    public boolean isWithinBounds(Vec3 pPos) {
        return this.isWithinBounds(pPos.x, pPos.z);
    }

    public boolean isWithinBounds(ChunkPos pChunkPos) {
        return this.isWithinBounds(pChunkPos.getMinBlockX(), pChunkPos.getMinBlockZ()) && this.isWithinBounds(pChunkPos.getMaxBlockX(), pChunkPos.getMaxBlockZ());
    }

    public boolean isWithinBounds(AABB pBox) {
        return this.isWithinBounds(pBox.minX, pBox.minZ, pBox.maxX - 1.0E-5F, pBox.maxZ - 1.0E-5F);
    }

    private boolean isWithinBounds(double pX1, double pZ1, double pX2, double pZ2) {
        return this.isWithinBounds(pX1, pZ1) && this.isWithinBounds(pX2, pZ2);
    }

    public boolean isWithinBounds(double pX, double pZ) {
        return this.isWithinBounds(pX, pZ, 0.0);
    }

    public boolean isWithinBounds(double pX, double pZ, double pOffset) {
        return pX >= this.getMinX() - pOffset
            && pX < this.getMaxX() + pOffset
            && pZ >= this.getMinZ() - pOffset
            && pZ < this.getMaxZ() + pOffset;
    }

    public BlockPos clampToBounds(BlockPos pPos) {
        return this.clampToBounds(pPos.getX(), pPos.getY(), pPos.getZ());
    }

    public BlockPos clampToBounds(Vec3 pPos) {
        return this.clampToBounds(pPos.x(), pPos.y(), pPos.z());
    }

    public BlockPos clampToBounds(double pX, double pY, double pZ) {
        return BlockPos.containing(this.clampVec3ToBound(pX, pY, pZ));
    }

    public Vec3 clampVec3ToBound(Vec3 pVec3) {
        return this.clampVec3ToBound(pVec3.x, pVec3.y, pVec3.z);
    }

    public Vec3 clampVec3ToBound(double pX, double pY, double pZ) {
        return new Vec3(
            Mth.clamp(pX, this.getMinX(), this.getMaxX() - 1.0E-5F), pY, Mth.clamp(pZ, this.getMinZ(), this.getMaxZ() - 1.0E-5F)
        );
    }

    public double getDistanceToBorder(Entity pEntity) {
        return this.getDistanceToBorder(pEntity.getX(), pEntity.getZ());
    }

    public VoxelShape getCollisionShape() {
        return this.extent.getCollisionShape();
    }

    public double getDistanceToBorder(double pX, double pZ) {
        double d0 = pZ - this.getMinZ();
        double d1 = this.getMaxZ() - pZ;
        double d2 = pX - this.getMinX();
        double d3 = this.getMaxX() - pX;
        double d4 = Math.min(d2, d3);
        d4 = Math.min(d4, d0);
        return Math.min(d4, d1);
    }

    public List<WorldBorder.DistancePerDirection> closestBorder(double pX, double pZ) {
        WorldBorder.DistancePerDirection[] aworldborder$distanceperdirection = new WorldBorder.DistancePerDirection[]{
            new WorldBorder.DistancePerDirection(Direction.NORTH, pZ - this.getMinZ()),
            new WorldBorder.DistancePerDirection(Direction.SOUTH, this.getMaxZ() - pZ),
            new WorldBorder.DistancePerDirection(Direction.WEST, pX - this.getMinX()),
            new WorldBorder.DistancePerDirection(Direction.EAST, this.getMaxX() - pX)
        };
        return Arrays.stream(aworldborder$distanceperdirection).sorted(Comparator.comparingDouble(p_396970_ -> p_396970_.distance)).toList();
    }

    public boolean isInsideCloseToBorder(Entity pEntity, AABB pBounds) {
        double d0 = Math.max(Mth.absMax(pBounds.getXsize(), pBounds.getZsize()), 1.0);
        return this.getDistanceToBorder(pEntity) < d0 * 2.0 && this.isWithinBounds(pEntity.getX(), pEntity.getZ(), d0);
    }

    public BorderStatus getStatus() {
        return this.extent.getStatus();
    }

    public double getMinX() {
        return this.extent.getMinX();
    }

    public double getMinZ() {
        return this.extent.getMinZ();
    }

    public double getMaxX() {
        return this.extent.getMaxX();
    }

    public double getMaxZ() {
        return this.extent.getMaxZ();
    }

    public double getCenterX() {
        return this.centerX;
    }

    public double getCenterZ() {
        return this.centerZ;
    }

    public void setCenter(double pX, double pZ) {
        this.centerX = pX;
        this.centerZ = pZ;
        this.extent.onCenterChange();

        for (BorderChangeListener borderchangelistener : this.getListeners()) {
            borderchangelistener.onBorderCenterSet(this, pX, pZ);
        }
    }

    public double getSize() {
        return this.extent.getSize();
    }

    public long getLerpRemainingTime() {
        return this.extent.getLerpRemainingTime();
    }

    public double getLerpTarget() {
        return this.extent.getLerpTarget();
    }

    public void setSize(double pSize) {
        this.extent = new WorldBorder.StaticBorderExtent(pSize);

        for (BorderChangeListener borderchangelistener : this.getListeners()) {
            borderchangelistener.onBorderSizeSet(this, pSize);
        }
    }

    public void lerpSizeBetween(double pOldSize, double pNewSize, long pTime) {
        this.extent = (WorldBorder.BorderExtent)(pOldSize == pNewSize
            ? new WorldBorder.StaticBorderExtent(pNewSize)
            : new WorldBorder.MovingBorderExtent(pOldSize, pNewSize, pTime));

        for (BorderChangeListener borderchangelistener : this.getListeners()) {
            borderchangelistener.onBorderSizeLerping(this, pOldSize, pNewSize, pTime);
        }
    }

    protected List<BorderChangeListener> getListeners() {
        return Lists.newArrayList(this.listeners);
    }

    public void addListener(BorderChangeListener pListener) {
        this.listeners.add(pListener);
    }

    public void removeListener(BorderChangeListener pListener) {
        this.listeners.remove(pListener);
    }

    public void setAbsoluteMaxSize(int pSize) {
        this.absoluteMaxSize = pSize;
        this.extent.onAbsoluteMaxSizeChange();
    }

    public int getAbsoluteMaxSize() {
        return this.absoluteMaxSize;
    }

    public double getDamageSafeZone() {
        return this.damageSafeZone;
    }

    public void setDamageSafeZone(double pDamageSafeZone) {
        this.damageSafeZone = pDamageSafeZone;

        for (BorderChangeListener borderchangelistener : this.getListeners()) {
            borderchangelistener.onBorderSetDamageSafeZOne(this, pDamageSafeZone);
        }
    }

    public double getDamagePerBlock() {
        return this.damagePerBlock;
    }

    public void setDamagePerBlock(double pDamagePerBlock) {
        this.damagePerBlock = pDamagePerBlock;

        for (BorderChangeListener borderchangelistener : this.getListeners()) {
            borderchangelistener.onBorderSetDamagePerBlock(this, pDamagePerBlock);
        }
    }

    public double getLerpSpeed() {
        return this.extent.getLerpSpeed();
    }

    public int getWarningTime() {
        return this.warningTime;
    }

    public void setWarningTime(int pWarningTime) {
        this.warningTime = pWarningTime;

        for (BorderChangeListener borderchangelistener : this.getListeners()) {
            borderchangelistener.onBorderSetWarningTime(this, pWarningTime);
        }
    }

    public int getWarningBlocks() {
        return this.warningBlocks;
    }

    public void setWarningBlocks(int pWarningDistance) {
        this.warningBlocks = pWarningDistance;

        for (BorderChangeListener borderchangelistener : this.getListeners()) {
            borderchangelistener.onBorderSetWarningBlocks(this, pWarningDistance);
        }
    }

    public void tick() {
        this.extent = this.extent.update();
    }

    public WorldBorder.Settings createSettings() {
        return new WorldBorder.Settings(this);
    }

    public void applySettings(WorldBorder.Settings pSerializer) {
        this.setCenter(pSerializer.getCenterX(), pSerializer.getCenterZ());
        this.setDamagePerBlock(pSerializer.getDamagePerBlock());
        this.setDamageSafeZone(pSerializer.getSafeZone());
        this.setWarningBlocks(pSerializer.getWarningBlocks());
        this.setWarningTime(pSerializer.getWarningTime());
        if (pSerializer.getSizeLerpTime() > 0L) {
            this.lerpSizeBetween(pSerializer.getSize(), pSerializer.getSizeLerpTarget(), pSerializer.getSizeLerpTime());
        } else {
            this.setSize(pSerializer.getSize());
        }
    }

    interface BorderExtent {
        double getMinX();

        double getMaxX();

        double getMinZ();

        double getMaxZ();

        double getSize();

        double getLerpSpeed();

        long getLerpRemainingTime();

        double getLerpTarget();

        BorderStatus getStatus();

        void onAbsoluteMaxSizeChange();

        void onCenterChange();

        WorldBorder.BorderExtent update();

        VoxelShape getCollisionShape();
    }

    public record DistancePerDirection(Direction direction, double distance) {
    }

    class MovingBorderExtent implements WorldBorder.BorderExtent {
        private final double from;
        private final double to;
        private final long lerpEnd;
        private final long lerpBegin;
        private final double lerpDuration;

        MovingBorderExtent(final double pFrom, final double pTo, final long pLerpDuration) {
            this.from = pFrom;
            this.to = pTo;
            this.lerpDuration = pLerpDuration;
            this.lerpBegin = Util.getMillis();
            this.lerpEnd = this.lerpBegin + pLerpDuration;
        }

        @Override
        public double getMinX() {
            return Mth.clamp(WorldBorder.this.getCenterX() - this.getSize() / 2.0, -WorldBorder.this.absoluteMaxSize, WorldBorder.this.absoluteMaxSize);
        }

        @Override
        public double getMinZ() {
            return Mth.clamp(WorldBorder.this.getCenterZ() - this.getSize() / 2.0, -WorldBorder.this.absoluteMaxSize, WorldBorder.this.absoluteMaxSize);
        }

        @Override
        public double getMaxX() {
            return Mth.clamp(WorldBorder.this.getCenterX() + this.getSize() / 2.0, -WorldBorder.this.absoluteMaxSize, WorldBorder.this.absoluteMaxSize);
        }

        @Override
        public double getMaxZ() {
            return Mth.clamp(WorldBorder.this.getCenterZ() + this.getSize() / 2.0, -WorldBorder.this.absoluteMaxSize, WorldBorder.this.absoluteMaxSize);
        }

        @Override
        public double getSize() {
            double d0 = (Util.getMillis() - this.lerpBegin) / this.lerpDuration;
            return d0 < 1.0 ? Mth.lerp(d0, this.from, this.to) : this.to;
        }

        @Override
        public double getLerpSpeed() {
            return Math.abs(this.from - this.to) / (this.lerpEnd - this.lerpBegin);
        }

        @Override
        public long getLerpRemainingTime() {
            return this.lerpEnd - Util.getMillis();
        }

        @Override
        public double getLerpTarget() {
            return this.to;
        }

        @Override
        public BorderStatus getStatus() {
            return this.to < this.from ? BorderStatus.SHRINKING : BorderStatus.GROWING;
        }

        @Override
        public void onCenterChange() {
        }

        @Override
        public void onAbsoluteMaxSizeChange() {
        }

        @Override
        public WorldBorder.BorderExtent update() {
            return (WorldBorder.BorderExtent)(this.getLerpRemainingTime() <= 0L ? WorldBorder.this.new StaticBorderExtent(this.to) : this);
        }

        @Override
        public VoxelShape getCollisionShape() {
            return Shapes.join(
                Shapes.INFINITY,
                Shapes.box(
                    Math.floor(this.getMinX()),
                    Double.NEGATIVE_INFINITY,
                    Math.floor(this.getMinZ()),
                    Math.ceil(this.getMaxX()),
                    Double.POSITIVE_INFINITY,
                    Math.ceil(this.getMaxZ())
                ),
                BooleanOp.ONLY_FIRST
            );
        }
    }

    public static class Settings {
        private final double centerX;
        private final double centerZ;
        private final double damagePerBlock;
        private final double safeZone;
        private final int warningBlocks;
        private final int warningTime;
        private final double size;
        private final long sizeLerpTime;
        private final double sizeLerpTarget;

        Settings(
            double pCenterX, double pCenterZ, double pDamagePerBlock, double pSafeZone, int pWarningBlocks, int pWarningTime, double pSize, long pSizeLerpTime, double pSizeLerpTarget
        ) {
            this.centerX = pCenterX;
            this.centerZ = pCenterZ;
            this.damagePerBlock = pDamagePerBlock;
            this.safeZone = pSafeZone;
            this.warningBlocks = pWarningBlocks;
            this.warningTime = pWarningTime;
            this.size = pSize;
            this.sizeLerpTime = pSizeLerpTime;
            this.sizeLerpTarget = pSizeLerpTarget;
        }

        Settings(WorldBorder pBorder) {
            this.centerX = pBorder.getCenterX();
            this.centerZ = pBorder.getCenterZ();
            this.damagePerBlock = pBorder.getDamagePerBlock();
            this.safeZone = pBorder.getDamageSafeZone();
            this.warningBlocks = pBorder.getWarningBlocks();
            this.warningTime = pBorder.getWarningTime();
            this.size = pBorder.getSize();
            this.sizeLerpTime = pBorder.getLerpRemainingTime();
            this.sizeLerpTarget = pBorder.getLerpTarget();
        }

        public double getCenterX() {
            return this.centerX;
        }

        public double getCenterZ() {
            return this.centerZ;
        }

        public double getDamagePerBlock() {
            return this.damagePerBlock;
        }

        public double getSafeZone() {
            return this.safeZone;
        }

        public int getWarningBlocks() {
            return this.warningBlocks;
        }

        public int getWarningTime() {
            return this.warningTime;
        }

        public double getSize() {
            return this.size;
        }

        public long getSizeLerpTime() {
            return this.sizeLerpTime;
        }

        public double getSizeLerpTarget() {
            return this.sizeLerpTarget;
        }

        public static WorldBorder.Settings read(DynamicLike<?> pDynamic, WorldBorder.Settings pDefaultValue) {
            double d0 = Mth.clamp(pDynamic.get("BorderCenterX").asDouble(pDefaultValue.centerX), -2.9999984E7, 2.9999984E7);
            double d1 = Mth.clamp(pDynamic.get("BorderCenterZ").asDouble(pDefaultValue.centerZ), -2.9999984E7, 2.9999984E7);
            double d2 = pDynamic.get("BorderSize").asDouble(pDefaultValue.size);
            long i = pDynamic.get("BorderSizeLerpTime").asLong(pDefaultValue.sizeLerpTime);
            double d3 = pDynamic.get("BorderSizeLerpTarget").asDouble(pDefaultValue.sizeLerpTarget);
            double d4 = pDynamic.get("BorderSafeZone").asDouble(pDefaultValue.safeZone);
            double d5 = pDynamic.get("BorderDamagePerBlock").asDouble(pDefaultValue.damagePerBlock);
            int j = pDynamic.get("BorderWarningBlocks").asInt(pDefaultValue.warningBlocks);
            int k = pDynamic.get("BorderWarningTime").asInt(pDefaultValue.warningTime);
            return new WorldBorder.Settings(d0, d1, d5, d4, j, k, d2, i, d3);
        }

        public void write(CompoundTag pNbt) {
            pNbt.putDouble("BorderCenterX", this.centerX);
            pNbt.putDouble("BorderCenterZ", this.centerZ);
            pNbt.putDouble("BorderSize", this.size);
            pNbt.putLong("BorderSizeLerpTime", this.sizeLerpTime);
            pNbt.putDouble("BorderSafeZone", this.safeZone);
            pNbt.putDouble("BorderDamagePerBlock", this.damagePerBlock);
            pNbt.putDouble("BorderSizeLerpTarget", this.sizeLerpTarget);
            pNbt.putDouble("BorderWarningBlocks", this.warningBlocks);
            pNbt.putDouble("BorderWarningTime", this.warningTime);
        }
    }

    class StaticBorderExtent implements WorldBorder.BorderExtent {
        private final double size;
        private double minX;
        private double minZ;
        private double maxX;
        private double maxZ;
        private VoxelShape shape;

        public StaticBorderExtent(final double pSize) {
            this.size = pSize;
            this.updateBox();
        }

        @Override
        public double getMinX() {
            return this.minX;
        }

        @Override
        public double getMaxX() {
            return this.maxX;
        }

        @Override
        public double getMinZ() {
            return this.minZ;
        }

        @Override
        public double getMaxZ() {
            return this.maxZ;
        }

        @Override
        public double getSize() {
            return this.size;
        }

        @Override
        public BorderStatus getStatus() {
            return BorderStatus.STATIONARY;
        }

        @Override
        public double getLerpSpeed() {
            return 0.0;
        }

        @Override
        public long getLerpRemainingTime() {
            return 0L;
        }

        @Override
        public double getLerpTarget() {
            return this.size;
        }

        private void updateBox() {
            this.minX = Mth.clamp(WorldBorder.this.getCenterX() - this.size / 2.0, -WorldBorder.this.absoluteMaxSize, WorldBorder.this.absoluteMaxSize);
            this.minZ = Mth.clamp(WorldBorder.this.getCenterZ() - this.size / 2.0, -WorldBorder.this.absoluteMaxSize, WorldBorder.this.absoluteMaxSize);
            this.maxX = Mth.clamp(WorldBorder.this.getCenterX() + this.size / 2.0, -WorldBorder.this.absoluteMaxSize, WorldBorder.this.absoluteMaxSize);
            this.maxZ = Mth.clamp(WorldBorder.this.getCenterZ() + this.size / 2.0, -WorldBorder.this.absoluteMaxSize, WorldBorder.this.absoluteMaxSize);
            this.shape = Shapes.join(
                Shapes.INFINITY,
                Shapes.box(
                    Math.floor(this.getMinX()),
                    Double.NEGATIVE_INFINITY,
                    Math.floor(this.getMinZ()),
                    Math.ceil(this.getMaxX()),
                    Double.POSITIVE_INFINITY,
                    Math.ceil(this.getMaxZ())
                ),
                BooleanOp.ONLY_FIRST
            );
        }

        @Override
        public void onAbsoluteMaxSizeChange() {
            this.updateBox();
        }

        @Override
        public void onCenterChange() {
            this.updateBox();
        }

        @Override
        public WorldBorder.BorderExtent update() {
            return this;
        }

        @Override
        public VoxelShape getCollisionShape() {
            return this.shape;
        }
    }
}