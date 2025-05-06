package net.minecraft.world.phys.shapes;

import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public class EntityCollisionContext implements CollisionContext {
    protected static final CollisionContext EMPTY = new EntityCollisionContext(false, false, -Double.MAX_VALUE, ItemStack.EMPTY, p_205118_ -> false, null) {
        @Override
        public boolean isAbove(VoxelShape p_82898_, BlockPos p_82899_, boolean p_82900_) {
            return p_82900_;
        }
    };
    private final boolean descending;
    private final double entityBottom;
    private final boolean placement;
    private final ItemStack heldItem;
    private final Predicate<FluidState> canStandOnFluid;
    @Nullable
    private final Entity entity;

    protected EntityCollisionContext(
        boolean pDescending, boolean pPlacement, double pEntityBottom, ItemStack pHeldItem, Predicate<FluidState> pCanStandOnFluid, @Nullable Entity pEntity
    ) {
        this.descending = pDescending;
        this.placement = pPlacement;
        this.entityBottom = pEntityBottom;
        this.heldItem = pHeldItem;
        this.canStandOnFluid = pCanStandOnFluid;
        this.entity = pEntity;
    }

    @Deprecated
    protected EntityCollisionContext(Entity pEntity, boolean pCanStandOnFluid, boolean pPlacement) {
        this(
            pEntity.isDescending(),
            pPlacement,
            pEntity.getY(),
            pEntity instanceof LivingEntity livingentity ? livingentity.getMainHandItem() : ItemStack.EMPTY,
            pCanStandOnFluid
                ? p_360701_ -> true
                : (pEntity instanceof LivingEntity ? p_391140_ -> ((LivingEntity)pEntity).canStandOnFluid(p_391140_) : p_205113_ -> false),
            pEntity
        );
    }

    @Override
    public boolean isHoldingItem(Item pItem) {
        return this.heldItem.is(pItem);
    }

    @Override
    public boolean canStandOnFluid(FluidState p_205115_, FluidState p_205116_) {
        return this.canStandOnFluid.test(p_205116_) && !p_205115_.getType().isSame(p_205116_.getType());
    }

    @Override
    public VoxelShape getCollisionShape(BlockState p_367344_, CollisionGetter p_362064_, BlockPos p_364238_) {
        return p_367344_.getCollisionShape(p_362064_, p_364238_, this);
    }

    @Override
    public boolean isDescending() {
        return this.descending;
    }

    @Override
    public boolean isAbove(VoxelShape p_82886_, BlockPos p_82887_, boolean p_82888_) {
        return this.entityBottom > p_82887_.getY() + p_82886_.max(Direction.Axis.Y) - 1.0E-5F;
    }

    @Nullable
    public Entity getEntity() {
        return this.entity;
    }

    @Override
    public boolean isPlacement() {
        return this.placement;
    }
}