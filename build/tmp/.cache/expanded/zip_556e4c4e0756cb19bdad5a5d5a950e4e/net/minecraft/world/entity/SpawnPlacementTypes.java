package net.minecraft.world.entity;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;

public interface SpawnPlacementTypes {
    SpawnPlacementType NO_RESTRICTIONS = (p_332715_, p_333529_, p_334870_) -> true;
    SpawnPlacementType IN_WATER = (p_390529_, p_390530_, p_390531_) -> {
        if (p_390531_ != null && p_390529_.getWorldBorder().isWithinBounds(p_390530_)) {
            BlockPos blockpos = p_390530_.above();
            return p_390529_.getFluidState(p_390530_).is(FluidTags.WATER) && !p_390529_.getBlockState(blockpos).isRedstoneConductor(p_390529_, blockpos);
        } else {
            return false;
        }
    };
    SpawnPlacementType IN_LAVA = (p_390526_, p_390527_, p_390528_) -> p_390528_ != null && p_390526_.getWorldBorder().isWithinBounds(p_390527_)
        ? p_390526_.getFluidState(p_390527_).is(FluidTags.LAVA)
        : false;
    SpawnPlacementType ON_GROUND = new SpawnPlacementType() {
        @Override
        public boolean isSpawnPositionOk(LevelReader p_328923_, BlockPos p_332749_, @Nullable EntityType<?> p_334188_) {
            if (p_334188_ != null && p_328923_.getWorldBorder().isWithinBounds(p_332749_)) {
                BlockPos blockpos = p_332749_.above();
                BlockPos blockpos1 = p_332749_.below();
                BlockState blockstate = p_328923_.getBlockState(blockpos1);
                return !blockstate.isValidSpawn(p_328923_, blockpos1, this, p_334188_)
                    ? false
                    : this.isValidEmptySpawnBlock(p_328923_, p_332749_, p_334188_) && this.isValidEmptySpawnBlock(p_328923_, blockpos, p_334188_);
            } else {
                return false;
            }
        }

        private boolean isValidEmptySpawnBlock(LevelReader p_331376_, BlockPos p_333023_, EntityType<?> p_334970_) {
            BlockState blockstate = p_331376_.getBlockState(p_333023_);
            return NaturalSpawner.isValidEmptySpawnBlock(p_331376_, p_333023_, blockstate, blockstate.getFluidState(), p_334970_);
        }

        @Override
        public BlockPos adjustSpawnPosition(LevelReader p_333745_, BlockPos p_335214_) {
            BlockPos blockpos = p_335214_.below();
            return p_333745_.getBlockState(blockpos).isPathfindable(PathComputationType.LAND) ? blockpos : p_335214_;
        }
    };
}
