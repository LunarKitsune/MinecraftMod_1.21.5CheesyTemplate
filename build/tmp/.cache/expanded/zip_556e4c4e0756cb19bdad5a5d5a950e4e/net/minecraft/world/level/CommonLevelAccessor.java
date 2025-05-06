package net.minecraft.world.level;

import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

public interface CommonLevelAccessor extends EntityGetter, LevelReader, LevelSimulatedRW {
    @Override
    default <T extends BlockEntity> Optional<T> getBlockEntity(BlockPos p_151452_, BlockEntityType<T> p_151453_) {
        return LevelReader.super.getBlockEntity(p_151452_, p_151453_);
    }

    @Override
    default List<VoxelShape> getEntityCollisions(@Nullable Entity p_186447_, AABB p_186448_) {
        return EntityGetter.super.getEntityCollisions(p_186447_, p_186448_);
    }

    @Override
    default boolean isUnobstructed(@Nullable Entity pEntity, VoxelShape pShape) {
        return EntityGetter.super.isUnobstructed(pEntity, pShape);
    }

    @Override
    default BlockPos getHeightmapPos(Heightmap.Types pHeightmapType, BlockPos pPos) {
        return LevelReader.super.getHeightmapPos(pHeightmapType, pPos);
    }
}