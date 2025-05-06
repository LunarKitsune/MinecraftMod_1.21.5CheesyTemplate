package net.minecraft.world.phys;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public class BlockHitResult extends HitResult {
    private final Direction direction;
    private final BlockPos blockPos;
    private final boolean miss;
    private final boolean inside;
    private final boolean worldBorderHit;

    public static BlockHitResult miss(Vec3 pLocation, Direction pDirection, BlockPos pBlockPos) {
        return new BlockHitResult(true, pLocation, pDirection, pBlockPos, false, false);
    }

    public BlockHitResult(Vec3 pLocation, Direction pDirection, BlockPos pBlockPos, boolean pInside) {
        this(false, pLocation, pDirection, pBlockPos, pInside, false);
    }

    public BlockHitResult(Vec3 pLocation, Direction pDirection, BlockPos pBlockPos, boolean pInside, boolean pWorldBorderHit) {
        this(false, pLocation, pDirection, pBlockPos, pInside, pWorldBorderHit);
    }

    private BlockHitResult(boolean pMiss, Vec3 pLocation, Direction pDirection, BlockPos pBlockPos, boolean pInside, boolean pWorldBorderHit) {
        super(pLocation);
        this.miss = pMiss;
        this.direction = pDirection;
        this.blockPos = pBlockPos;
        this.inside = pInside;
        this.worldBorderHit = pWorldBorderHit;
    }

    public BlockHitResult withDirection(Direction pNewFace) {
        return new BlockHitResult(this.miss, this.location, pNewFace, this.blockPos, this.inside, this.worldBorderHit);
    }

    public BlockHitResult withPosition(BlockPos pPos) {
        return new BlockHitResult(this.miss, this.location, this.direction, pPos, this.inside, this.worldBorderHit);
    }

    public BlockHitResult hitBorder() {
        return new BlockHitResult(this.miss, this.location, this.direction, this.blockPos, this.inside, true);
    }

    public BlockPos getBlockPos() {
        return this.blockPos;
    }

    public Direction getDirection() {
        return this.direction;
    }

    @Override
    public HitResult.Type getType() {
        return this.miss ? HitResult.Type.MISS : HitResult.Type.BLOCK;
    }

    public boolean isInside() {
        return this.inside;
    }

    public boolean isWorldBorderHit() {
        return this.worldBorderHit;
    }
}