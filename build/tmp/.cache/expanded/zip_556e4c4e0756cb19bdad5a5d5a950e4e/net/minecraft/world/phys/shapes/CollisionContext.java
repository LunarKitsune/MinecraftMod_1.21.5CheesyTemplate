package net.minecraft.world.phys.shapes;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public interface CollisionContext {
    static CollisionContext empty() {
        return EntityCollisionContext.EMPTY;
    }

    static CollisionContext of(Entity pEntity) {
        return (CollisionContext)(switch (pEntity) {
            case AbstractMinecart abstractminecart -> AbstractMinecart.useExperimentalMovement(abstractminecart.level())
                ? new MinecartCollisionContext(abstractminecart, false)
                : new EntityCollisionContext(pEntity, false, false);
            default -> new EntityCollisionContext(pEntity, false, false);
        });
    }

    static CollisionContext of(Entity pEntity, boolean pCanStandOnFluid) {
        return new EntityCollisionContext(pEntity, pCanStandOnFluid, false);
    }

    static CollisionContext placementContext(@Nullable Entity pEntity) {
        return new EntityCollisionContext(
            pEntity != null ? pEntity.isDescending() : false,
            true,
            pEntity != null ? pEntity.getY() : -Double.MAX_VALUE,
            pEntity instanceof LivingEntity livingentity ? livingentity.getMainHandItem() : ItemStack.EMPTY,
            pEntity instanceof LivingEntity livingentity1 ? p_394132_ -> ((LivingEntity)pEntity).canStandOnFluid(p_394132_) : p_397477_ -> false,
            pEntity
        );
    }

    boolean isDescending();

    boolean isAbove(VoxelShape pShape, BlockPos pPos, boolean pCanAscend);

    boolean isHoldingItem(Item pItem);

    boolean canStandOnFluid(FluidState pFluid1, FluidState pFluid2);

    VoxelShape getCollisionShape(BlockState pState, CollisionGetter pCollisionGetter, BlockPos pPos);

    default boolean isPlacement() {
        return false;
    }
}