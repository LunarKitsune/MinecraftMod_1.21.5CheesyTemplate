package net.minecraft.world.entity.projectile;

import com.google.common.base.MoreObjects;
import it.unimi.dsi.fastutil.doubles.DoubleDoubleImmutablePair;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public abstract class Projectile extends Entity implements TraceableEntity {
    private static final boolean DEFAULT_LEFT_OWNER = false;
    private static final boolean DEFAULT_HAS_BEEN_SHOT = false;
    @Nullable
    private UUID ownerUUID;
    @Nullable
    private Entity cachedOwner;
    private boolean leftOwner = false;
    private boolean hasBeenShot = false;
    @Nullable
    private Entity lastDeflectedBy;

    protected Projectile(EntityType<? extends Projectile> p_37248_, Level p_37249_) {
        super(p_37248_, p_37249_);
    }

    public void setOwner(@Nullable Entity pOwner) {
        if (pOwner != null) {
            this.ownerUUID = pOwner.getUUID();
            this.cachedOwner = pOwner;
        }
    }

    @Nullable
    @Override
    public Entity getOwner() {
        if (this.cachedOwner != null && !this.cachedOwner.isRemoved()) {
            return this.cachedOwner;
        } else if (this.ownerUUID != null) {
            this.cachedOwner = this.findOwner(this.ownerUUID);
            return this.cachedOwner;
        } else {
            return null;
        }
    }

    @Nullable
    protected Entity findOwner(UUID pEntityUuid) {
        return this.level() instanceof ServerLevel serverlevel ? serverlevel.getEntity(pEntityUuid) : null;
    }

    public Entity getEffectSource() {
        return MoreObjects.firstNonNull(this.getOwner(), this);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag pCompound) {
        pCompound.storeNullable("Owner", UUIDUtil.CODEC, this.ownerUUID);
        if (this.leftOwner) {
            pCompound.putBoolean("LeftOwner", true);
        }

        pCompound.putBoolean("HasBeenShot", this.hasBeenShot);
    }

    protected boolean ownedBy(Entity pEntity) {
        return pEntity.getUUID().equals(this.ownerUUID);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag pCompound) {
        this.setOwnerThroughUUID(pCompound.read("Owner", UUIDUtil.CODEC).orElse(null));
        this.leftOwner = pCompound.getBooleanOr("LeftOwner", false);
        this.hasBeenShot = pCompound.getBooleanOr("HasBeenShot", false);
    }

    protected void setOwnerThroughUUID(@Nullable UUID pUuid) {
        if (!Objects.equals(this.ownerUUID, pUuid)) {
            this.ownerUUID = pUuid;
            this.cachedOwner = pUuid != null ? this.findOwner(pUuid) : null;
        }
    }

    @Override
    public void restoreFrom(Entity p_310133_) {
        super.restoreFrom(p_310133_);
        if (p_310133_ instanceof Projectile projectile) {
            this.ownerUUID = projectile.ownerUUID;
            this.cachedOwner = projectile.cachedOwner;
        }
    }

    @Override
    public void tick() {
        if (!this.hasBeenShot) {
            this.gameEvent(GameEvent.PROJECTILE_SHOOT, this.getOwner());
            this.hasBeenShot = true;
        }

        if (!this.leftOwner) {
            this.leftOwner = this.checkLeftOwner();
        }

        super.tick();
    }

    private boolean checkLeftOwner() {
        Entity entity = this.getOwner();
        if (entity != null) {
            AABB aabb = this.getBoundingBox().expandTowards(this.getDeltaMovement()).inflate(1.0);
            return entity.getRootVehicle().getSelfAndPassengers().filter(EntitySelector.CAN_BE_PICKED).noneMatch(p_359340_ -> aabb.intersects(p_359340_.getBoundingBox()));
        } else {
            return true;
        }
    }

    public Vec3 getMovementToShoot(double pX, double pY, double pZ, float pVelocity, float pInaccuracy) {
        return new Vec3(pX, pY, pZ)
            .normalize()
            .add(
                this.random.triangle(0.0, 0.0172275 * pInaccuracy),
                this.random.triangle(0.0, 0.0172275 * pInaccuracy),
                this.random.triangle(0.0, 0.0172275 * pInaccuracy)
            )
            .scale(pVelocity);
    }

    public void shoot(double pX, double pY, double pZ, float pVelocity, float pInaccuracy) {
        Vec3 vec3 = this.getMovementToShoot(pX, pY, pZ, pVelocity, pInaccuracy);
        this.setDeltaMovement(vec3);
        this.hasImpulse = true;
        double d0 = vec3.horizontalDistance();
        this.setYRot((float)(Mth.atan2(vec3.x, vec3.z) * 180.0F / (float)Math.PI));
        this.setXRot((float)(Mth.atan2(vec3.y, d0) * 180.0F / (float)Math.PI));
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }

    public void shootFromRotation(Entity pShooter, float pX, float pY, float pZ, float pVelocity, float pInaccuracy) {
        float f = -Mth.sin(pY * (float) (Math.PI / 180.0)) * Mth.cos(pX * (float) (Math.PI / 180.0));
        float f1 = -Mth.sin((pX + pZ) * (float) (Math.PI / 180.0));
        float f2 = Mth.cos(pY * (float) (Math.PI / 180.0)) * Mth.cos(pX * (float) (Math.PI / 180.0));
        this.shoot(f, f1, f2, pVelocity, pInaccuracy);
        Vec3 vec3 = pShooter.getKnownMovement();
        this.setDeltaMovement(this.getDeltaMovement().add(vec3.x, pShooter.onGround() ? 0.0 : vec3.y, vec3.z));
    }

    @Override
    public void onAboveBubbleColumn(boolean p_395187_, BlockPos p_397623_) {
        double d0 = p_395187_ ? -0.03 : 0.1;
        this.setDeltaMovement(this.getDeltaMovement().add(0.0, d0, 0.0));
        sendBubbleColumnParticles(this.level(), p_397623_);
    }

    @Override
    public void onInsideBubbleColumn(boolean p_395217_) {
        double d0 = p_395217_ ? -0.03 : 0.06;
        this.setDeltaMovement(this.getDeltaMovement().add(0.0, d0, 0.0));
        this.resetFallDistance();
    }

    public static <T extends Projectile> T spawnProjectileFromRotation(
        Projectile.ProjectileFactory<T> pFactory,
        ServerLevel pLevel,
        ItemStack pSpawnedFrom,
        LivingEntity pOwner,
        float pZ,
        float pVelocity,
        float pInnaccuracy
    ) {
        return spawnProjectile(
            pFactory.create(pLevel, pOwner, pSpawnedFrom),
            pLevel,
            pSpawnedFrom,
            p_375173_ -> p_375173_.shootFromRotation(pOwner, pOwner.getXRot(), pOwner.getYRot(), pZ, pVelocity, pInnaccuracy)
        );
    }

    public static <T extends Projectile> T spawnProjectileUsingShoot(
        Projectile.ProjectileFactory<T> pFactory,
        ServerLevel pLevel,
        ItemStack pSpawnedFrom,
        LivingEntity pOwner,
        double pX,
        double pY,
        double pZ,
        float pVelocity,
        float pInaccuracy
    ) {
        return spawnProjectile(
            pFactory.create(pLevel, pOwner, pSpawnedFrom),
            pLevel,
            pSpawnedFrom,
            p_359337_ -> p_359337_.shoot(pX, pY, pZ, pVelocity, pInaccuracy)
        );
    }

    public static <T extends Projectile> T spawnProjectileUsingShoot(
        T pProjectile, ServerLevel pLevel, ItemStack pSpawnedFrom, double pX, double pY, double pZ, float pVelocity, float pInaccuracy
    ) {
        return spawnProjectile(pProjectile, pLevel, pSpawnedFrom, p_359347_ -> pProjectile.shoot(pX, pY, pZ, pVelocity, pInaccuracy));
    }

    public static <T extends Projectile> T spawnProjectile(T pProjectile, ServerLevel pLevel, ItemStack pSpawnedFrom) {
        return spawnProjectile(pProjectile, pLevel, pSpawnedFrom, p_359326_ -> {});
    }

    public static <T extends Projectile> T spawnProjectile(T pProjectile, ServerLevel pLevel, ItemStack pStack, Consumer<T> pAdapter) {
        pAdapter.accept(pProjectile);
        pLevel.addFreshEntity(pProjectile);
        pProjectile.applyOnProjectileSpawned(pLevel, pStack);
        return pProjectile;
    }

    public void applyOnProjectileSpawned(ServerLevel pLevel, ItemStack pSpawnedFrom) {
        EnchantmentHelper.onProjectileSpawned(pLevel, pSpawnedFrom, this, p_359338_ -> {});
        if (this instanceof AbstractArrow abstractarrow) {
            ItemStack itemstack = abstractarrow.getWeaponItem();
            if (itemstack != null && !itemstack.isEmpty() && !pSpawnedFrom.getItem().equals(itemstack.getItem())) {
                EnchantmentHelper.onProjectileSpawned(pLevel, itemstack, this, abstractarrow::onItemBreak);
            }
        }
    }

    protected ProjectileDeflection hitTargetOrDeflectSelf(HitResult pHitResult) {
        if (pHitResult.getType() == HitResult.Type.ENTITY) {
            EntityHitResult entityhitresult = (EntityHitResult)pHitResult;
            Entity entity = entityhitresult.getEntity();
            ProjectileDeflection projectiledeflection = entity.deflection(this);
            if (projectiledeflection != ProjectileDeflection.NONE) {
                if (entity != this.lastDeflectedBy && this.deflect(projectiledeflection, entity, this.getOwner(), false)) {
                    this.lastDeflectedBy = entity;
                }

                return projectiledeflection;
            }
        } else if (this.shouldBounceOnWorldBorder() && pHitResult instanceof BlockHitResult blockhitresult && blockhitresult.isWorldBorderHit()) {
            ProjectileDeflection projectiledeflection1 = ProjectileDeflection.REVERSE;
            if (this.deflect(projectiledeflection1, null, this.getOwner(), false)) {
                this.setDeltaMovement(this.getDeltaMovement().scale(0.2));
                return projectiledeflection1;
            }
        }

        this.onHit(pHitResult);
        return ProjectileDeflection.NONE;
    }

    protected boolean shouldBounceOnWorldBorder() {
        return false;
    }

    public boolean deflect(ProjectileDeflection pDeflection, @Nullable Entity pEntity, @Nullable Entity pOwner, boolean pDeflectedByPlayer) {
        pDeflection.deflect(this, pEntity, this.random);
        if (!this.level().isClientSide) {
            this.setOwner(pOwner);
            this.onDeflection(pEntity, pDeflectedByPlayer);
        }

        return true;
    }

    protected void onDeflection(@Nullable Entity pEntity, boolean pDeflectedByPlayer) {
    }

    protected void onItemBreak(Item pItem) {
    }

    protected void onHit(HitResult pResult) {
        HitResult.Type hitresult$type = pResult.getType();
        if (hitresult$type == HitResult.Type.ENTITY) {
            EntityHitResult entityhitresult = (EntityHitResult)pResult;
            Entity entity = entityhitresult.getEntity();
            if (entity.getType().is(EntityTypeTags.REDIRECTABLE_PROJECTILE) && entity instanceof Projectile projectile) {
                projectile.deflect(ProjectileDeflection.AIM_DEFLECT, this.getOwner(), this.getOwner(), true);
            }

            this.onHitEntity(entityhitresult);
            this.level().gameEvent(GameEvent.PROJECTILE_LAND, pResult.getLocation(), GameEvent.Context.of(this, null));
        } else if (hitresult$type == HitResult.Type.BLOCK) {
            BlockHitResult blockhitresult = (BlockHitResult)pResult;
            this.onHitBlock(blockhitresult);
            BlockPos blockpos = blockhitresult.getBlockPos();
            this.level().gameEvent(GameEvent.PROJECTILE_LAND, blockpos, GameEvent.Context.of(this, this.level().getBlockState(blockpos)));
        }
    }

    protected void onHitEntity(EntityHitResult pResult) {
    }

    protected void onHitBlock(BlockHitResult pResult) {
        BlockState blockstate = this.level().getBlockState(pResult.getBlockPos());
        blockstate.onProjectileHit(this.level(), blockstate, pResult, this);
    }

    protected boolean canHitEntity(Entity pTarget) {
        if (!pTarget.canBeHitByProjectile()) {
            return false;
        } else {
            Entity entity = this.getOwner();
            return entity == null || this.leftOwner || !entity.isPassengerOfSameVehicle(pTarget);
        }
    }

    protected void updateRotation() {
        Vec3 vec3 = this.getDeltaMovement();
        double d0 = vec3.horizontalDistance();
        this.setXRot(lerpRotation(this.xRotO, (float)(Mth.atan2(vec3.y, d0) * 180.0F / (float)Math.PI)));
        this.setYRot(lerpRotation(this.yRotO, (float)(Mth.atan2(vec3.x, vec3.z) * 180.0F / (float)Math.PI)));
    }

    protected static float lerpRotation(float pCurrentRotation, float pTargetRotation) {
        while (pTargetRotation - pCurrentRotation < -180.0F) {
            pCurrentRotation -= 360.0F;
        }

        while (pTargetRotation - pCurrentRotation >= 180.0F) {
            pCurrentRotation += 360.0F;
        }

        return Mth.lerp(0.2F, pCurrentRotation, pTargetRotation);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity p_345233_) {
        Entity entity = this.getOwner();
        return new ClientboundAddEntityPacket(this, p_345233_, entity == null ? 0 : entity.getId());
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket p_150170_) {
        super.recreateFromPacket(p_150170_);
        Entity entity = this.level().getEntity(p_150170_.getData());
        if (entity != null) {
            this.setOwner(entity);
        }
    }

    @Override
    public boolean mayInteract(ServerLevel p_364907_, BlockPos p_150168_) {
        Entity entity = this.getOwner();
        return entity instanceof Player ? entity.mayInteract(p_364907_, p_150168_) : entity == null || net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(p_364907_, entity);
    }

    public boolean mayBreak(ServerLevel pLevel) {
        return this.getType().is(EntityTypeTags.IMPACT_PROJECTILES) && pLevel.getGameRules().getBoolean(GameRules.RULE_PROJECTILESCANBREAKBLOCKS);
    }

    @Override
    public boolean isPickable() {
        return this.getType().is(EntityTypeTags.REDIRECTABLE_PROJECTILE);
    }

    @Override
    public float getPickRadius() {
        return this.isPickable() ? 1.0F : 0.0F;
    }

    public DoubleDoubleImmutablePair calculateHorizontalHurtKnockbackDirection(LivingEntity pEntity, DamageSource pDamageSource) {
        double d0 = this.getDeltaMovement().x;
        double d1 = this.getDeltaMovement().z;
        return DoubleDoubleImmutablePair.of(d0, d1);
    }

    @Override
    public int getDimensionChangingDelay() {
        return 2;
    }

    @Override
    public boolean hurtServer(ServerLevel p_367356_, DamageSource p_368526_, float p_366624_) {
        if (!this.isInvulnerableToBase(p_368526_)) {
            this.markHurt();
        }

        return false;
    }

    @FunctionalInterface
    public interface ProjectileFactory<T extends Projectile> {
        T create(ServerLevel pLevel, LivingEntity pOwner, ItemStack pSpawnedFrom);
    }
}
