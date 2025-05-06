package net.minecraft.world.item;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Spawner;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class SpawnEggItem extends Item {
    private static final Map<EntityType<? extends Mob>, SpawnEggItem> BY_ID = Maps.newIdentityHashMap();
    private final EntityType<?> defaultType;

    public SpawnEggItem(EntityType<? extends Mob> pDefaultType, Item.Properties pProperties) {
        super(pProperties);
        this.defaultType = pDefaultType;
        BY_ID.put(pDefaultType, this);
    }

    @Override
    public InteractionResult useOn(UseOnContext pContext) {
        Level level = pContext.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        } else {
            ItemStack itemstack = pContext.getItemInHand();
            BlockPos blockpos = pContext.getClickedPos();
            Direction direction = pContext.getClickedFace();
            BlockState blockstate = level.getBlockState(blockpos);
            if (level.getBlockEntity(blockpos) instanceof Spawner spawner) {
                EntityType<?> entitytype1 = this.getType(level.registryAccess(), itemstack);
                spawner.setEntityId(entitytype1, level.getRandom());
                level.sendBlockUpdated(blockpos, blockstate, blockstate, 3);
                level.gameEvent(pContext.getPlayer(), GameEvent.BLOCK_CHANGE, blockpos);
                itemstack.shrink(1);
                return InteractionResult.SUCCESS;
            } else {
                BlockPos blockpos1;
                if (blockstate.getCollisionShape(level, blockpos).isEmpty()) {
                    blockpos1 = blockpos;
                } else {
                    blockpos1 = blockpos.relative(direction);
                }

                EntityType<?> entitytype = this.getType(level.registryAccess(), itemstack);
                if (entitytype.spawn(
                        (ServerLevel)level,
                        itemstack,
                        pContext.getPlayer(),
                        blockpos1,
                        EntitySpawnReason.SPAWN_ITEM_USE,
                        true,
                        !Objects.equals(blockpos, blockpos1) && direction == Direction.UP
                    )
                    != null) {
                    itemstack.shrink(1);
                    level.gameEvent(pContext.getPlayer(), GameEvent.ENTITY_PLACE, blockpos);
                }

                return InteractionResult.SUCCESS;
            }
        }
    }

    @Override
    public InteractionResult use(Level p_43225_, Player p_43226_, InteractionHand p_43227_) {
        ItemStack itemstack = p_43226_.getItemInHand(p_43227_);
        BlockHitResult blockhitresult = getPlayerPOVHitResult(p_43225_, p_43226_, ClipContext.Fluid.SOURCE_ONLY);
        if (blockhitresult.getType() != HitResult.Type.BLOCK) {
            return InteractionResult.PASS;
        } else if (p_43225_ instanceof ServerLevel serverlevel) {
            BlockPos $$8 = blockhitresult.getBlockPos();
            if (!(p_43225_.getBlockState($$8).getBlock() instanceof LiquidBlock)) {
                return InteractionResult.PASS;
            } else if (p_43225_.mayInteract(p_43226_, $$8) && p_43226_.mayUseItemAt($$8, blockhitresult.getDirection(), itemstack)) {
                EntityType<?> entitytype = this.getType(serverlevel.registryAccess(), itemstack);
                Entity entity = entitytype.spawn(serverlevel, itemstack, p_43226_, $$8, EntitySpawnReason.SPAWN_ITEM_USE, false, false);
                if (entity == null) {
                    return InteractionResult.PASS;
                } else {
                    itemstack.consume(1, p_43226_);
                    p_43226_.awardStat(Stats.ITEM_USED.get(this));
                    p_43225_.gameEvent(p_43226_, GameEvent.ENTITY_PLACE, entity.position());
                    return InteractionResult.SUCCESS;
                }
            } else {
                return InteractionResult.FAIL;
            }
        } else {
            return InteractionResult.SUCCESS;
        }
    }

    public boolean spawnsEntity(HolderLookup.Provider pRegistries, ItemStack pStack, EntityType<?> pEntityType) {
        return Objects.equals(this.getType(pRegistries, pStack), pEntityType);
    }

    @Nullable
    public static SpawnEggItem byId(@Nullable EntityType<?> pType) {
        return BY_ID.get(pType);
    }

    public static Iterable<SpawnEggItem> eggs() {
        return Iterables.unmodifiableIterable(BY_ID.values());
    }

    public EntityType<?> getType(HolderLookup.Provider pRegistries, ItemStack pProvider) {
        CustomData customdata = pProvider.getOrDefault(DataComponents.ENTITY_DATA, CustomData.EMPTY);
        if (!customdata.isEmpty()) {
            EntityType<?> entitytype = customdata.parseEntityType(pRegistries, Registries.ENTITY_TYPE);
            if (entitytype != null) {
                return entitytype;
            }
        }

        return this.defaultType;
    }

    public EntityType<?> getDefaultType() {
        return this.defaultType;
    }

    @Override
    public FeatureFlagSet requiredFeatures() {
        return this.defaultType.requiredFeatures();
    }

    public Optional<Mob> spawnOffspringFromSpawnEgg(Player pPlayer, Mob pMob, EntityType<? extends Mob> pEntityType, ServerLevel pServerLevel, Vec3 pPos, ItemStack pStack) {
        if (!this.spawnsEntity(pServerLevel.registryAccess(), pStack, pEntityType)) {
            return Optional.empty();
        } else {
            Mob mob;
            if (pMob instanceof AgeableMob) {
                mob = ((AgeableMob)pMob).getBreedOffspring(pServerLevel, (AgeableMob)pMob);
            } else {
                mob = pEntityType.create(pServerLevel, EntitySpawnReason.SPAWN_ITEM_USE);
            }

            if (mob == null) {
                return Optional.empty();
            } else {
                mob.setBaby(true);
                if (!mob.isBaby()) {
                    return Optional.empty();
                } else {
                    mob.snapTo(pPos.x(), pPos.y(), pPos.z(), 0.0F, 0.0F);
                    mob.applyComponentsFromItemStack(pStack);
                    pServerLevel.addFreshEntityWithPassengers(mob);
                    pStack.consume(1, pPlayer);
                    return Optional.of(mob);
                }
            }
        }
    }

    @Override
    public boolean shouldPrintOpWarning(ItemStack p_378492_, @Nullable Player p_377094_) {
        if (p_377094_ != null && p_377094_.getPermissionLevel() >= 2) {
            CustomData customdata = p_378492_.get(DataComponents.ENTITY_DATA);
            if (customdata != null) {
                EntityType<?> entitytype = customdata.parseEntityType(p_377094_.level().registryAccess(), Registries.ENTITY_TYPE);
                return entitytype != null && entitytype.onlyOpCanSetNbt();
            }
        }

        return false;
    }
}
