package net.minecraft.world.entity.boss.enderdragon;

import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.dimension.end.EndDragonFight;

public class EndCrystal extends Entity {
    private static final EntityDataAccessor<Optional<BlockPos>> DATA_BEAM_TARGET = SynchedEntityData.defineId(EndCrystal.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final EntityDataAccessor<Boolean> DATA_SHOW_BOTTOM = SynchedEntityData.defineId(EndCrystal.class, EntityDataSerializers.BOOLEAN);
    private static final boolean DEFAULT_SHOW_BOTTOM = true;
    public int time;

    public EndCrystal(EntityType<? extends EndCrystal> p_31037_, Level p_31038_) {
        super(p_31037_, p_31038_);
        this.blocksBuilding = true;
        this.time = this.random.nextInt(100000);
    }

    public EndCrystal(Level pLevel, double pX, double pY, double pZ) {
        this(EntityType.END_CRYSTAL, pLevel);
        this.setPos(pX, pY, pZ);
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.NONE;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder p_331044_) {
        p_331044_.define(DATA_BEAM_TARGET, Optional.empty());
        p_331044_.define(DATA_SHOW_BOTTOM, true);
    }

    @Override
    public void tick() {
        this.time++;
        this.applyEffectsFromBlocks();
        this.handlePortal();
        if (this.level() instanceof ServerLevel) {
            BlockPos blockpos = this.blockPosition();
            if (((ServerLevel)this.level()).getDragonFight() != null && this.level().getBlockState(blockpos).isAir()) {
                this.level().setBlockAndUpdate(blockpos, BaseFireBlock.getState(this.level(), blockpos));
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag pCompound) {
        pCompound.storeNullable("beam_target", BlockPos.CODEC, this.getBeamTarget());
        pCompound.putBoolean("ShowBottom", this.showsBottom());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag pCompound) {
        this.setBeamTarget(pCompound.read("beam_target", BlockPos.CODEC).orElse(null));
        this.setShowBottom(pCompound.getBooleanOr("ShowBottom", true));
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public final boolean hurtClient(DamageSource p_368846_) {
        return this.isInvulnerableToBase(p_368846_) ? false : !(p_368846_.getEntity() instanceof EnderDragon);
    }

    @Override
    public final boolean hurtServer(ServerLevel p_363851_, DamageSource p_362125_, float p_364288_) {
        if (this.isInvulnerableToBase(p_362125_)) {
            return false;
        } else if (p_362125_.getEntity() instanceof EnderDragon) {
            return false;
        } else {
            if (!this.isRemoved()) {
                this.remove(Entity.RemovalReason.KILLED);
                if (!p_362125_.is(DamageTypeTags.IS_EXPLOSION)) {
                    DamageSource damagesource = p_362125_.getEntity() != null ? this.damageSources().explosion(this, p_362125_.getEntity()) : null;
                    p_363851_.explode(
                        this, damagesource, null, this.getX(), this.getY(), this.getZ(), 6.0F, false, Level.ExplosionInteraction.BLOCK
                    );
                }

                this.onDestroyedBy(p_363851_, p_362125_);
            }

            return true;
        }
    }

    @Override
    public void kill(ServerLevel p_366543_) {
        this.onDestroyedBy(p_366543_, this.damageSources().generic());
        super.kill(p_366543_);
    }

    private void onDestroyedBy(ServerLevel pLevel, DamageSource pDamageSource) {
        EndDragonFight enddragonfight = pLevel.getDragonFight();
        if (enddragonfight != null) {
            enddragonfight.onCrystalDestroyed(this, pDamageSource);
        }
    }

    public void setBeamTarget(@Nullable BlockPos pBeamTarget) {
        this.getEntityData().set(DATA_BEAM_TARGET, Optional.ofNullable(pBeamTarget));
    }

    @Nullable
    public BlockPos getBeamTarget() {
        return this.getEntityData().get(DATA_BEAM_TARGET).orElse(null);
    }

    public void setShowBottom(boolean pShowBottom) {
        this.getEntityData().set(DATA_SHOW_BOTTOM, pShowBottom);
    }

    public boolean showsBottom() {
        return this.getEntityData().get(DATA_SHOW_BOTTOM);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double pDistance) {
        return super.shouldRenderAtSqrDistance(pDistance) || this.getBeamTarget() != null;
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(Items.END_CRYSTAL);
    }
}