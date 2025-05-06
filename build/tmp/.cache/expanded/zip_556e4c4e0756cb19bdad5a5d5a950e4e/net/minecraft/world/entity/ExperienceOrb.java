package net.minecraft.world.entity;

import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ExperienceOrb extends Entity {
    protected static final EntityDataAccessor<Integer> DATA_VALUE = SynchedEntityData.defineId(ExperienceOrb.class, EntityDataSerializers.INT);
    private static final int LIFETIME = 6000;
    private static final int ENTITY_SCAN_PERIOD = 20;
    private static final int MAX_FOLLOW_DIST = 8;
    private static final int ORB_GROUPS_PER_AREA = 40;
    private static final double ORB_MERGE_DISTANCE = 0.5;
    private static final short DEFAULT_HEALTH = 5;
    private static final short DEFAULT_AGE = 0;
    private static final short DEFAULT_VALUE = 0;
    private static final int DEFAULT_COUNT = 1;
    private int age = 0;
    private int health = 5;
    private int count = 1;
    @Nullable
    private Player followingPlayer;
    private final InterpolationHandler interpolation = new InterpolationHandler(this);

    public ExperienceOrb(Level pLevel, double pX, double pY, double pZ, int pValue) {
        this(EntityType.EXPERIENCE_ORB, pLevel);
        this.setPos(pX, pY, pZ);
        if (!this.level().isClientSide) {
            this.setYRot((float)(this.random.nextDouble() * 360.0));
            this.setDeltaMovement(
                (this.random.nextDouble() * 0.2F - 0.1F) * 2.0, this.random.nextDouble() * 0.2 * 2.0, (this.random.nextDouble() * 0.2F - 0.1F) * 2.0
            );
        }

        this.setValue(pValue);
    }

    public ExperienceOrb(EntityType<? extends ExperienceOrb> p_20773_, Level p_20774_) {
        super(p_20773_, p_20774_);
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.NONE;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder p_329424_) {
        p_329424_.define(DATA_VALUE, 0);
    }

    @Override
    protected double getDefaultGravity() {
        return 0.03;
    }

    @Override
    public void tick() {
        this.interpolation.interpolate();
        if (this.firstTick && this.level().isClientSide) {
            this.firstTick = false;
        } else {
            super.tick();
            boolean flag = !this.level().noCollision(this.getBoundingBox());
            if (this.isEyeInFluid(FluidTags.WATER)) {
                this.setUnderwaterMovement();
            } else if (!flag) {
                this.applyGravity();
            }

            if (this.level().getFluidState(this.blockPosition()).is(FluidTags.LAVA)) {
                this.setDeltaMovement(
                    (this.random.nextFloat() - this.random.nextFloat()) * 0.2F, 0.2F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F
                );
            }

            if (this.tickCount % 20 == 1) {
                this.scanForMerges();
            }

            this.followNearbyPlayer();
            if (this.followingPlayer == null && !this.level().isClientSide && flag) {
                this.moveTowardsClosestSpace(this.getX(), (this.getBoundingBox().minY + this.getBoundingBox().maxY) / 2.0, this.getZ());
                this.hasImpulse = true;
            }

            double d0 = this.getDeltaMovement().y;
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.applyEffectsFromBlocks();
            float f = 0.98F;
            if (this.onGround()) {
                BlockPos pos = getBlockPosBelowThatAffectsMyMovement();
                f = this.level().getBlockState(pos).getFriction(this.level(), pos, this) * 0.98F;
            }

            this.setDeltaMovement(this.getDeltaMovement().scale(f));
            if (this.verticalCollisionBelow && d0 < -this.getGravity()) {
                this.setDeltaMovement(new Vec3(this.getDeltaMovement().x, -d0 * 0.4, this.getDeltaMovement().z));
            }

            this.age++;
            if (this.age >= 6000) {
                this.discard();
            }
        }
    }

    private void followNearbyPlayer() {
        if (this.followingPlayer == null || this.followingPlayer.isSpectator() || this.followingPlayer.distanceToSqr(this) > 64.0) {
            Player player = this.level().getNearestPlayer(this, 8.0);
            if (player != null && !player.isSpectator() && !player.isDeadOrDying()) {
                this.followingPlayer = player;
            } else {
                this.followingPlayer = null;
            }
        }

        if (this.followingPlayer != null) {
            Vec3 vec3 = new Vec3(
                this.followingPlayer.getX() - this.getX(),
                this.followingPlayer.getY() + this.followingPlayer.getEyeHeight() / 2.0 - this.getY(),
                this.followingPlayer.getZ() - this.getZ()
            );
            double d0 = vec3.lengthSqr();
            double d1 = 1.0 - Math.sqrt(d0) / 8.0;
            this.setDeltaMovement(this.getDeltaMovement().add(vec3.normalize().scale(d1 * d1 * 0.1)));
        }
    }

    @Override
    public BlockPos getBlockPosBelowThatAffectsMyMovement() {
        return this.getOnPos(0.999999F);
    }

    private void scanForMerges() {
        if (this.level() instanceof ServerLevel) {
            for (ExperienceOrb experienceorb : this.level()
                .getEntities(EntityTypeTest.forClass(ExperienceOrb.class), this.getBoundingBox().inflate(0.5), this::canMerge)) {
                this.merge(experienceorb);
            }
        }
    }

    public static void award(ServerLevel pLevel, Vec3 pPos, int pAmount) {
        while (pAmount > 0) {
            int i = getExperienceValue(pAmount);
            pAmount -= i;
            if (!tryMergeToExisting(pLevel, pPos, i)) {
                pLevel.addFreshEntity(new ExperienceOrb(pLevel, pPos.x(), pPos.y(), pPos.z(), i));
            }
        }
    }

    private static boolean tryMergeToExisting(ServerLevel pLevel, Vec3 pPos, int pAmount) {
        AABB aabb = AABB.ofSize(pPos, 1.0, 1.0, 1.0);
        int i = pLevel.getRandom().nextInt(40);
        List<ExperienceOrb> list = pLevel.getEntities(EntityTypeTest.forClass(ExperienceOrb.class), aabb, p_147081_ -> canMerge(p_147081_, i, pAmount));
        if (!list.isEmpty()) {
            ExperienceOrb experienceorb = list.get(0);
            experienceorb.count++;
            experienceorb.age = 0;
            return true;
        } else {
            return false;
        }
    }

    private boolean canMerge(ExperienceOrb pOrb) {
        return pOrb != this && canMerge(pOrb, this.getId(), this.getValue());
    }

    private static boolean canMerge(ExperienceOrb pOrb, int pAmount, int pOther) {
        return !pOrb.isRemoved() && (pOrb.getId() - pAmount) % 40 == 0 && pOrb.getValue() == pOther;
    }

    private void merge(ExperienceOrb pOrb) {
        this.count = this.count + pOrb.count;
        this.age = Math.min(this.age, pOrb.age);
        pOrb.discard();
    }

    private void setUnderwaterMovement() {
        Vec3 vec3 = this.getDeltaMovement();
        this.setDeltaMovement(vec3.x * 0.99F, Math.min(vec3.y + 5.0E-4F, 0.06F), vec3.z * 0.99F);
    }

    @Override
    protected void doWaterSplashEffect() {
    }

    @Override
    public final boolean hurtClient(DamageSource p_369585_) {
        return !this.isInvulnerableToBase(p_369585_);
    }

    @Override
    public final boolean hurtServer(ServerLevel p_365476_, DamageSource p_362340_, float p_369855_) {
        if (this.isInvulnerableToBase(p_362340_)) {
            return false;
        } else {
            this.markHurt();
            this.health = (int)(this.health - p_369855_);
            if (this.health <= 0) {
                this.discard();
            }

            return true;
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        pCompound.putShort("Health", (short)this.health);
        pCompound.putShort("Age", (short)this.age);
        pCompound.putShort("Value", (short)this.getValue());
        pCompound.putInt("Count", this.count);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        this.health = pCompound.getShortOr("Health", (short)5);
        this.age = pCompound.getShortOr("Age", (short)0);
        this.setValue(pCompound.getShortOr("Value", (short)0));
        this.count = pCompound.read("Count", ExtraCodecs.POSITIVE_INT).orElse(1);
    }

    @Override
    public void playerTouch(Player pEntity) {
        if (pEntity instanceof ServerPlayer serverplayer) {
            if (pEntity.takeXpDelay == 0) {
                if (net.minecraftforge.event.ForgeEventFactory.onPlayerPickupXp(pEntity, this)) return;
                pEntity.takeXpDelay = 2;
                pEntity.take(this, 1);
                int i = this.repairPlayerItems(serverplayer, this.getValue());
                if (i > 0) {
                    pEntity.giveExperiencePoints(i);
                }

                this.count--;
                if (this.count == 0) {
                    this.discard();
                }
            }
        }
    }

    private int repairPlayerItems(ServerPlayer pPlayer, int pValue) {
        Optional<EnchantedItemInUse> optional = EnchantmentHelper.getRandomItemWith(EnchantmentEffectComponents.REPAIR_WITH_XP, pPlayer, ItemStack::isDamaged);
        if (optional.isPresent()) {
            ItemStack itemstack = optional.get().itemStack();
            int i = EnchantmentHelper.modifyDurabilityToRepairFromXp(pPlayer.serverLevel(), itemstack, pValue);
            int j = Math.min(i, itemstack.getDamageValue());
            itemstack.setDamageValue(itemstack.getDamageValue() - j);
            if (j > 0) {
                int k = pValue - j * pValue / i;
                if (k > 0) {
                    return this.repairPlayerItems(pPlayer, k);
                }
            }

            return 0;
        } else {
            return pValue;
        }
    }

    public int getValue() {
        return this.entityData.get(DATA_VALUE);
    }

    private void setValue(int pValue) {
        this.entityData.set(DATA_VALUE, pValue);
    }

    public int getIcon() {
        int i = this.getValue();
        if (i >= 2477) {
            return 10;
        } else if (i >= 1237) {
            return 9;
        } else if (i >= 617) {
            return 8;
        } else if (i >= 307) {
            return 7;
        } else if (i >= 149) {
            return 6;
        } else if (i >= 73) {
            return 5;
        } else if (i >= 37) {
            return 4;
        } else if (i >= 17) {
            return 3;
        } else if (i >= 7) {
            return 2;
        } else {
            return i >= 3 ? 1 : 0;
        }
    }

    public static int getExperienceValue(int pExpValue) {
        if (pExpValue >= 2477) {
            return 2477;
        } else if (pExpValue >= 1237) {
            return 1237;
        } else if (pExpValue >= 617) {
            return 617;
        } else if (pExpValue >= 307) {
            return 307;
        } else if (pExpValue >= 149) {
            return 149;
        } else if (pExpValue >= 73) {
            return 73;
        } else if (pExpValue >= 37) {
            return 37;
        } else if (pExpValue >= 17) {
            return 17;
        } else if (pExpValue >= 7) {
            return 7;
        } else {
            return pExpValue >= 3 ? 3 : 1;
        }
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.AMBIENT;
    }

    @Override
    public InterpolationHandler getInterpolation() {
        return this.interpolation;
    }
}
