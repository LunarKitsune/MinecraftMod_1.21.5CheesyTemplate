package net.minecraft.world.entity.ai.navigation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.Vec3;

public class GroundPathNavigation extends PathNavigation {
    private boolean avoidSun;

    public GroundPathNavigation(Mob p_26448_, Level p_26449_) {
        super(p_26448_, p_26449_);
    }

    @Override
    protected PathFinder createPathFinder(int p_26453_) {
        this.nodeEvaluator = new WalkNodeEvaluator();
        return new PathFinder(this.nodeEvaluator, p_26453_);
    }

    @Override
    protected boolean canUpdatePath() {
        return this.mob.onGround() || this.mob.isInLiquid() || this.mob.isPassenger();
    }

    @Override
    protected Vec3 getTempMobPos() {
        return new Vec3(this.mob.getX(), this.getSurfaceY(), this.mob.getZ());
    }

    @Override
    public Path createPath(BlockPos pPos, int pAccuracy) {
        LevelChunk levelchunk = this.level.getChunkSource().getChunkNow(SectionPos.blockToSectionCoord(pPos.getX()), SectionPos.blockToSectionCoord(pPos.getZ()));
        if (levelchunk == null) {
            return null;
        } else {
            if (levelchunk.getBlockState(pPos).isAir()) {
                BlockPos.MutableBlockPos blockpos$mutableblockpos = pPos.mutable().move(Direction.DOWN);

                while (blockpos$mutableblockpos.getY() > this.level.getMinY() && levelchunk.getBlockState(blockpos$mutableblockpos).isAir()) {
                    blockpos$mutableblockpos.move(Direction.DOWN);
                }

                if (blockpos$mutableblockpos.getY() > this.level.getMinY()) {
                    return super.createPath(blockpos$mutableblockpos.above(), pAccuracy);
                }

                blockpos$mutableblockpos.setY(pPos.getY() + 1);

                while (blockpos$mutableblockpos.getY() <= this.level.getMaxY() && levelchunk.getBlockState(blockpos$mutableblockpos).isAir()) {
                    blockpos$mutableblockpos.move(Direction.UP);
                }

                pPos = blockpos$mutableblockpos;
            }

            if (!levelchunk.getBlockState(pPos).isSolid()) {
                return super.createPath(pPos, pAccuracy);
            } else {
                BlockPos.MutableBlockPos blockpos$mutableblockpos1 = pPos.mutable().move(Direction.UP);

                while (blockpos$mutableblockpos1.getY() <= this.level.getMaxY() && levelchunk.getBlockState(blockpos$mutableblockpos1).isSolid()) {
                    blockpos$mutableblockpos1.move(Direction.UP);
                }

                return super.createPath(blockpos$mutableblockpos1.immutable(), pAccuracy);
            }
        }
    }

    @Override
    public Path createPath(Entity pEntity, int p_26466_) {
        return this.createPath(pEntity.blockPosition(), p_26466_);
    }

    private int getSurfaceY() {
        if (this.mob.isInWater() && this.canFloat()) {
            int i = this.mob.getBlockY();
            BlockState blockstate = this.level.getBlockState(BlockPos.containing(this.mob.getX(), i, this.mob.getZ()));
            int j = 0;

            while (blockstate.is(Blocks.WATER)) {
                blockstate = this.level.getBlockState(BlockPos.containing(this.mob.getX(), ++i, this.mob.getZ()));
                if (++j > 16) {
                    return this.mob.getBlockY();
                }
            }

            return i;
        } else {
            return Mth.floor(this.mob.getY() + 0.5);
        }
    }

    @Override
    protected void trimPath() {
        super.trimPath();
        if (this.avoidSun) {
            if (this.level.canSeeSky(BlockPos.containing(this.mob.getX(), this.mob.getY() + 0.5, this.mob.getZ()))) {
                return;
            }

            for (int i = 0; i < this.path.getNodeCount(); i++) {
                Node node = this.path.getNode(i);
                if (this.level.canSeeSky(new BlockPos(node.x, node.y, node.z))) {
                    this.path.truncateNodes(i);
                    return;
                }
            }
        }
    }

    protected boolean hasValidPathType(PathType pPathType) {
        if (pPathType == PathType.WATER) {
            return false;
        } else {
            return pPathType == PathType.LAVA ? false : pPathType != PathType.OPEN;
        }
    }

    public void setCanOpenDoors(boolean pCanOpenDoors) {
        this.nodeEvaluator.setCanOpenDoors(pCanOpenDoors);
    }

    public void setAvoidSun(boolean pAvoidSun) {
        this.avoidSun = pAvoidSun;
    }

    public void setCanWalkOverFences(boolean pCanWalkOverFences) {
        this.nodeEvaluator.setCanWalkOverFences(pCanWalkOverFences);
    }
}