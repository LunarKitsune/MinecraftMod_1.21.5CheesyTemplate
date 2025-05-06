package net.minecraft.world.entity.vehicle;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class MinecartSpawner extends AbstractMinecart {
    private final BaseSpawner spawner = new BaseSpawner() {
        @Override
        public void broadcastEvent(Level p_150342_, BlockPos p_150343_, int p_150344_) {
            p_150342_.broadcastEntityEvent(MinecartSpawner.this, (byte)p_150344_);
        }

        @Override
        @org.jetbrains.annotations.Nullable
        public net.minecraft.world.entity.Entity getSpawnerEntity() {
            return MinecartSpawner.this;
        }
    };
    private final Runnable ticker;

    public MinecartSpawner(EntityType<? extends MinecartSpawner> p_38623_, Level p_38624_) {
        super(p_38623_, p_38624_);
        this.ticker = this.createTicker(p_38624_);
    }

    @Override
    protected Item getDropItem() {
        return Items.MINECART;
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(Items.MINECART);
    }

    private Runnable createTicker(Level pLevel) {
        return pLevel instanceof ServerLevel
            ? () -> this.spawner.serverTick((ServerLevel)pLevel, this.blockPosition())
            : () -> this.spawner.clientTick(pLevel, this.blockPosition());
    }

    @Override
    public BlockState getDefaultDisplayBlockState() {
        return Blocks.SPAWNER.defaultBlockState();
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        this.spawner.load(this.level(), this.blockPosition(), pCompound);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        this.spawner.save(pCompound);
    }

    @Override
    public void handleEntityEvent(byte p_38631_) {
        this.spawner.onEventTriggered(this.level(), p_38631_);
    }

    @Override
    public void tick() {
        super.tick();
        this.ticker.run();
    }

    public BaseSpawner getSpawner() {
        return this.spawner;
    }
}
