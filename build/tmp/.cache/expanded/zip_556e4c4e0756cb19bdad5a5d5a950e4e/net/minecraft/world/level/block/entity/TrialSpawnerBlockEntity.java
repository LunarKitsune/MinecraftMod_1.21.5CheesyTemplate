package net.minecraft.world.level.block.entity;

import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Spawner;
import net.minecraft.world.level.block.TrialSpawnerBlock;
import net.minecraft.world.level.block.entity.trialspawner.PlayerDetector;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawner;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawnerState;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class TrialSpawnerBlockEntity extends BlockEntity implements Spawner, TrialSpawner.StateAccessor {
    private TrialSpawner trialSpawner = this.createDefaultSpawner();

    public TrialSpawnerBlockEntity(BlockPos pPos, BlockState pState) {
        super(BlockEntityType.TRIAL_SPAWNER, pPos, pState);
    }

    private TrialSpawner createDefaultSpawner() {
        PlayerDetector playerdetector = PlayerDetector.NO_CREATIVE_PLAYERS;
        PlayerDetector.EntitySelector playerdetector$entityselector = PlayerDetector.EntitySelector.SELECT_FROM_LEVEL;
        return new TrialSpawner(this, playerdetector, playerdetector$entityselector);
    }

    @Override
    protected void loadAdditional(CompoundTag p_330602_, HolderLookup.Provider p_329868_) {
        super.loadAdditional(p_330602_, p_329868_);
        this.trialSpawner = p_330602_.read(this.trialSpawner.codec(), p_329868_.createSerializationContext(NbtOps.INSTANCE)).orElseGet(this::createDefaultSpawner);
        if (this.level != null) {
            this.markUpdated();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag p_310285_, HolderLookup.Provider p_332039_) {
        super.saveAdditional(p_310285_, p_332039_);
        p_310285_.store(this.trialSpawner.codec(), p_332039_.createSerializationContext(NbtOps.INSTANCE), this.trialSpawner);
    }

    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider p_335483_) {
        return this.trialSpawner.getData().getUpdateTag(this.getBlockState().getValue(TrialSpawnerBlock.STATE));
    }

    @Override
    public void setEntityId(EntityType<?> p_312357_, RandomSource p_313173_) {
        if (this.level == null) {
            Util.logAndPauseIfInIde("Expected non-null level");
        } else {
            this.trialSpawner.overrideEntityToSpawn(p_312357_, this.level);
            this.setChanged();
        }
    }

    public TrialSpawner getTrialSpawner() {
        return this.trialSpawner;
    }

    @Override
    public TrialSpawnerState getState() {
        return !this.getBlockState().hasProperty(BlockStateProperties.TRIAL_SPAWNER_STATE)
            ? TrialSpawnerState.INACTIVE
            : this.getBlockState().getValue(BlockStateProperties.TRIAL_SPAWNER_STATE);
    }

    @Override
    public void setState(Level p_313150_, TrialSpawnerState p_310751_) {
        this.setChanged();
        p_313150_.setBlockAndUpdate(this.worldPosition, this.getBlockState().setValue(BlockStateProperties.TRIAL_SPAWNER_STATE, p_310751_));
    }

    @Override
    public void markUpdated() {
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }
}