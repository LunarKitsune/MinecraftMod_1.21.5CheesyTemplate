package net.minecraft.world.level;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.ticks.LevelTickAccess;
import net.minecraft.world.ticks.ScheduledTick;
import net.minecraft.world.ticks.TickPriority;

public interface ScheduledTickAccess {
    <T> ScheduledTick<T> createTick(BlockPos pPos, T pType, int pDelay, TickPriority pPriority);

    <T> ScheduledTick<T> createTick(BlockPos pPos, T pType, int pDelay);

    LevelTickAccess<Block> getBlockTicks();

    default void scheduleTick(BlockPos pPos, Block pBlock, int pDelay, TickPriority pPriority) {
        this.getBlockTicks().schedule(this.createTick(pPos, pBlock, pDelay, pPriority));
    }

    default void scheduleTick(BlockPos pPos, Block pBlock, int pDelay) {
        this.getBlockTicks().schedule(this.createTick(pPos, pBlock, pDelay));
    }

    LevelTickAccess<Fluid> getFluidTicks();

    default void scheduleTick(BlockPos pPos, Fluid pFluid, int pDelay, TickPriority pPriority) {
        this.getFluidTicks().schedule(this.createTick(pPos, pFluid, pDelay, pPriority));
    }

    default void scheduleTick(BlockPos pPos, Fluid pFluid, int pDelay) {
        this.getFluidTicks().schedule(this.createTick(pPos, pFluid, pDelay));
    }
}