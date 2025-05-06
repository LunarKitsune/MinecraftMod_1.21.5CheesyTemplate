package net.minecraft.world.entity.monster.piglin;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BackUpIfTooClose;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.behavior.CopyMemoryWithExpiry;
import net.minecraft.world.entity.ai.behavior.CrossbowAttack;
import net.minecraft.world.entity.ai.behavior.DismountOrSkipMounting;
import net.minecraft.world.entity.ai.behavior.DoNothing;
import net.minecraft.world.entity.ai.behavior.EraseMemoryIf;
import net.minecraft.world.entity.ai.behavior.GoToTargetLocation;
import net.minecraft.world.entity.ai.behavior.GoToWantedItem;
import net.minecraft.world.entity.ai.behavior.InteractWith;
import net.minecraft.world.entity.ai.behavior.InteractWithDoor;
import net.minecraft.world.entity.ai.behavior.LookAtTargetSink;
import net.minecraft.world.entity.ai.behavior.MeleeAttack;
import net.minecraft.world.entity.ai.behavior.Mount;
import net.minecraft.world.entity.ai.behavior.MoveToTargetSink;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.world.entity.ai.behavior.RandomStroll;
import net.minecraft.world.entity.ai.behavior.RunOne;
import net.minecraft.world.entity.ai.behavior.SetEntityLookTarget;
import net.minecraft.world.entity.ai.behavior.SetEntityLookTargetSometimes;
import net.minecraft.world.entity.ai.behavior.SetLookAndInteract;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetAwayFrom;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromAttackTargetIfTargetOutOfReach;
import net.minecraft.world.entity.ai.behavior.SetWalkTargetFromLookTarget;
import net.minecraft.world.entity.ai.behavior.StartAttacking;
import net.minecraft.world.entity.ai.behavior.StartCelebratingIfTargetDead;
import net.minecraft.world.entity.ai.behavior.StopAttackingIfTargetInvalid;
import net.minecraft.world.entity.ai.behavior.StopBeingAngryIfTargetDead;
import net.minecraft.world.entity.ai.behavior.TriggerGate;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.declarative.Trigger;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

public class PiglinAi {
    public static final int REPELLENT_DETECTION_RANGE_HORIZONTAL = 8;
    public static final int REPELLENT_DETECTION_RANGE_VERTICAL = 4;
    public static final Item BARTERING_ITEM = Items.GOLD_INGOT;
    private static final int PLAYER_ANGER_RANGE = 16;
    private static final int ANGER_DURATION = 600;
    private static final int ADMIRE_DURATION = 119;
    private static final int MAX_DISTANCE_TO_WALK_TO_ITEM = 9;
    private static final int MAX_TIME_TO_WALK_TO_ITEM = 200;
    private static final int HOW_LONG_TIME_TO_DISABLE_ADMIRE_WALKING_IF_CANT_REACH_ITEM = 200;
    private static final int CELEBRATION_TIME = 300;
    protected static final UniformInt TIME_BETWEEN_HUNTS = TimeUtil.rangeOfSeconds(30, 120);
    private static final int BABY_FLEE_DURATION_AFTER_GETTING_HIT = 100;
    private static final int HIT_BY_PLAYER_MEMORY_TIMEOUT = 400;
    private static final int MAX_WALK_DISTANCE_TO_START_RIDING = 8;
    private static final UniformInt RIDE_START_INTERVAL = TimeUtil.rangeOfSeconds(10, 40);
    private static final UniformInt RIDE_DURATION = TimeUtil.rangeOfSeconds(10, 30);
    private static final UniformInt RETREAT_DURATION = TimeUtil.rangeOfSeconds(5, 20);
    private static final int MELEE_ATTACK_COOLDOWN = 20;
    private static final int EAT_COOLDOWN = 200;
    private static final int DESIRED_DISTANCE_FROM_ENTITY_WHEN_AVOIDING = 12;
    private static final int MAX_LOOK_DIST = 8;
    private static final int MAX_LOOK_DIST_FOR_PLAYER_HOLDING_LOVED_ITEM = 14;
    private static final int INTERACTION_RANGE = 8;
    private static final int MIN_DESIRED_DIST_FROM_TARGET_WHEN_HOLDING_CROSSBOW = 5;
    private static final float SPEED_WHEN_STRAFING_BACK_FROM_TARGET = 0.75F;
    private static final int DESIRED_DISTANCE_FROM_ZOMBIFIED = 6;
    private static final UniformInt AVOID_ZOMBIFIED_DURATION = TimeUtil.rangeOfSeconds(5, 7);
    private static final UniformInt BABY_AVOID_NEMESIS_DURATION = TimeUtil.rangeOfSeconds(5, 7);
    private static final float PROBABILITY_OF_CELEBRATION_DANCE = 0.1F;
    private static final float SPEED_MULTIPLIER_WHEN_AVOIDING = 1.0F;
    private static final float SPEED_MULTIPLIER_WHEN_RETREATING = 1.0F;
    private static final float SPEED_MULTIPLIER_WHEN_MOUNTING = 0.8F;
    private static final float SPEED_MULTIPLIER_WHEN_GOING_TO_WANTED_ITEM = 1.0F;
    private static final float SPEED_MULTIPLIER_WHEN_GOING_TO_CELEBRATE_LOCATION = 1.0F;
    private static final float SPEED_MULTIPLIER_WHEN_DANCING = 0.6F;
    private static final float SPEED_MULTIPLIER_WHEN_IDLING = 0.6F;

    protected static Brain<?> makeBrain(Piglin pPiglin, Brain<Piglin> pBrain) {
        initCoreActivity(pBrain);
        initIdleActivity(pBrain);
        initAdmireItemActivity(pBrain);
        initFightActivity(pPiglin, pBrain);
        initCelebrateActivity(pBrain);
        initRetreatActivity(pBrain);
        initRideHoglinActivity(pBrain);
        pBrain.setCoreActivities(ImmutableSet.of(Activity.CORE));
        pBrain.setDefaultActivity(Activity.IDLE);
        pBrain.useDefaultActivity();
        return pBrain;
    }

    protected static void initMemories(Piglin pPiglin, RandomSource pRandom) {
        int i = TIME_BETWEEN_HUNTS.sample(pRandom);
        pPiglin.getBrain().setMemoryWithExpiry(MemoryModuleType.HUNTED_RECENTLY, true, i);
    }

    private static void initCoreActivity(Brain<Piglin> pBrain) {
        pBrain.addActivity(
            Activity.CORE,
            0,
            ImmutableList.of(
                new LookAtTargetSink(45, 90),
                new MoveToTargetSink(),
                InteractWithDoor.create(),
                babyAvoidNemesis(),
                avoidZombified(),
                StopHoldingItemIfNoLongerAdmiring.create(),
                StartAdmiringItemIfSeen.create(119),
                StartCelebratingIfTargetDead.create(300, PiglinAi::wantsToDance),
                StopBeingAngryIfTargetDead.create()
            )
        );
    }

    private static void initIdleActivity(Brain<Piglin> pBrain) {
        pBrain.addActivity(
            Activity.IDLE,
            10,
            ImmutableList.of(
                SetEntityLookTarget.create(PiglinAi::isPlayerHoldingLovedItem, 14.0F),
                StartAttacking.<Piglin>create((p_390707_, p_390708_) -> p_390708_.isAdult(), PiglinAi::findNearestValidAttackTarget),
                BehaviorBuilder.triggerIf(Piglin::canHunt, StartHuntingHoglin.create()),
                avoidRepellent(),
                babySometimesRideBabyHoglin(),
                createIdleLookBehaviors(),
                createIdleMovementBehaviors(),
                SetLookAndInteract.create(EntityType.PLAYER, 4)
            )
        );
    }

    private static void initFightActivity(Piglin pPiglin, Brain<Piglin> pBrain) {
        pBrain.addActivityAndRemoveMemoryWhenStopped(
            Activity.FIGHT,
            10,
            ImmutableList.of(
                StopAttackingIfTargetInvalid.create((p_359284_, p_359285_) -> !isNearestValidAttackTarget(p_359284_, pPiglin, p_359285_)),
                BehaviorBuilder.triggerIf(PiglinAi::hasCrossbow, BackUpIfTooClose.create(5, 0.75F)),
                SetWalkTargetFromAttackTargetIfTargetOutOfReach.create(1.0F),
                MeleeAttack.create(20),
                new CrossbowAttack<>(),
                RememberIfHoglinWasKilled.create(),
                EraseMemoryIf.create(PiglinAi::isNearZombified, MemoryModuleType.ATTACK_TARGET)
            ),
            MemoryModuleType.ATTACK_TARGET
        );
    }

    private static void initCelebrateActivity(Brain<Piglin> pBrain) {
        pBrain.addActivityAndRemoveMemoryWhenStopped(
            Activity.CELEBRATE,
            10,
            ImmutableList.of(
                avoidRepellent(),
                SetEntityLookTarget.create(PiglinAi::isPlayerHoldingLovedItem, 14.0F),
                StartAttacking.<Piglin>create((p_390705_, p_390706_) -> p_390706_.isAdult(), PiglinAi::findNearestValidAttackTarget),
                BehaviorBuilder.triggerIf(p_34804_ -> !p_34804_.isDancing(), GoToTargetLocation.create(MemoryModuleType.CELEBRATE_LOCATION, 2, 1.0F)),
                BehaviorBuilder.triggerIf(Piglin::isDancing, GoToTargetLocation.create(MemoryModuleType.CELEBRATE_LOCATION, 4, 0.6F)),
                new RunOne<LivingEntity>(
                    ImmutableList.of(
                        Pair.of(SetEntityLookTarget.create(EntityType.PIGLIN, 8.0F), 1),
                        Pair.of((BehaviorControl<LivingEntity>)(BehaviorControl)RandomStroll.stroll(0.6F, 2, 1), 1),
                        Pair.of(new DoNothing(10, 20), 1)
                    )
                )
            ),
            MemoryModuleType.CELEBRATE_LOCATION
        );
    }

    private static void initAdmireItemActivity(Brain<Piglin> pBrain) {
        pBrain.addActivityAndRemoveMemoryWhenStopped(
            Activity.ADMIRE_ITEM,
            10,
            ImmutableList.of(
                GoToWantedItem.create(PiglinAi::isNotHoldingLovedItemInOffHand, 1.0F, true, 9),
                StopAdmiringIfItemTooFarAway.create(9),
                StopAdmiringIfTiredOfTryingToReachItem.create(200, 200)
            ),
            MemoryModuleType.ADMIRING_ITEM
        );
    }

    private static void initRetreatActivity(Brain<Piglin> pBrain) {
        pBrain.addActivityAndRemoveMemoryWhenStopped(
            Activity.AVOID,
            10,
            ImmutableList.of(
                SetWalkTargetAwayFrom.entity(MemoryModuleType.AVOID_TARGET, 1.0F, 12, true),
                createIdleLookBehaviors(),
                createIdleMovementBehaviors(),
                EraseMemoryIf.create(PiglinAi::wantsToStopFleeing, MemoryModuleType.AVOID_TARGET)
            ),
            MemoryModuleType.AVOID_TARGET
        );
    }

    private static void initRideHoglinActivity(Brain<Piglin> pBrain) {
        pBrain.addActivityAndRemoveMemoryWhenStopped(
            Activity.RIDE,
            10,
            ImmutableList.of(
                Mount.create(0.8F),
                SetEntityLookTarget.create(PiglinAi::isPlayerHoldingLovedItem, 8.0F),
                BehaviorBuilder.sequence(
                    BehaviorBuilder.triggerIf(Entity::isPassenger),
                    TriggerGate.triggerOneShuffled(
                        ImmutableList.<Pair<? extends Trigger<? super LivingEntity>, Integer>>builder()
                            .addAll(createLookBehaviors())
                            .add(Pair.of(BehaviorBuilder.triggerIf(p_258950_ -> true), 1))
                            .build()
                    )
                ),
                DismountOrSkipMounting.create(8, PiglinAi::wantsToStopRiding)
            ),
            MemoryModuleType.RIDE_TARGET
        );
    }

    private static ImmutableList<Pair<OneShot<LivingEntity>, Integer>> createLookBehaviors() {
        return ImmutableList.of(
            Pair.of(SetEntityLookTarget.create(EntityType.PLAYER, 8.0F), 1),
            Pair.of(SetEntityLookTarget.create(EntityType.PIGLIN, 8.0F), 1),
            Pair.of(SetEntityLookTarget.create(8.0F), 1)
        );
    }

    private static RunOne<LivingEntity> createIdleLookBehaviors() {
        return new RunOne<>(
            ImmutableList.<Pair<? extends BehaviorControl<? super LivingEntity>, Integer>>builder()
                .addAll(createLookBehaviors())
                .add(Pair.of(new DoNothing(30, 60), 1))
                .build()
        );
    }

    private static RunOne<Piglin> createIdleMovementBehaviors() {
        return new RunOne<>(
            ImmutableList.of(
                Pair.of(RandomStroll.stroll(0.6F), 2),
                Pair.of(InteractWith.of(EntityType.PIGLIN, 8, MemoryModuleType.INTERACTION_TARGET, 0.6F, 2), 2),
                Pair.of(BehaviorBuilder.triggerIf(PiglinAi::doesntSeeAnyPlayerHoldingLovedItem, SetWalkTargetFromLookTarget.create(0.6F, 3)), 2),
                Pair.of(new DoNothing(30, 60), 1)
            )
        );
    }

    private static BehaviorControl<PathfinderMob> avoidRepellent() {
        return SetWalkTargetAwayFrom.pos(MemoryModuleType.NEAREST_REPELLENT, 1.0F, 8, false);
    }

    private static BehaviorControl<Piglin> babyAvoidNemesis() {
        return CopyMemoryWithExpiry.create(Piglin::isBaby, MemoryModuleType.NEAREST_VISIBLE_NEMESIS, MemoryModuleType.AVOID_TARGET, BABY_AVOID_NEMESIS_DURATION);
    }

    private static BehaviorControl<Piglin> avoidZombified() {
        return CopyMemoryWithExpiry.create(PiglinAi::isNearZombified, MemoryModuleType.NEAREST_VISIBLE_ZOMBIFIED, MemoryModuleType.AVOID_TARGET, AVOID_ZOMBIFIED_DURATION);
    }

    protected static void updateActivity(Piglin pPiglin) {
        Brain<Piglin> brain = pPiglin.getBrain();
        Activity activity = brain.getActiveNonCoreActivity().orElse(null);
        brain.setActiveActivityToFirstValid(ImmutableList.of(Activity.ADMIRE_ITEM, Activity.FIGHT, Activity.AVOID, Activity.CELEBRATE, Activity.RIDE, Activity.IDLE));
        Activity activity1 = brain.getActiveNonCoreActivity().orElse(null);
        if (activity != activity1) {
            getSoundForCurrentActivity(pPiglin).ifPresent(pPiglin::makeSound);
        }

        pPiglin.setAggressive(brain.hasMemoryValue(MemoryModuleType.ATTACK_TARGET));
        if (!brain.hasMemoryValue(MemoryModuleType.RIDE_TARGET) && isBabyRidingBaby(pPiglin)) {
            pPiglin.stopRiding();
        }

        if (!brain.hasMemoryValue(MemoryModuleType.CELEBRATE_LOCATION)) {
            brain.eraseMemory(MemoryModuleType.DANCING);
        }

        pPiglin.setDancing(brain.hasMemoryValue(MemoryModuleType.DANCING));
    }

    private static boolean isBabyRidingBaby(Piglin pPassenger) {
        if (!pPassenger.isBaby()) {
            return false;
        } else {
            Entity entity = pPassenger.getVehicle();
            return entity instanceof Piglin && ((Piglin)entity).isBaby() || entity instanceof Hoglin && ((Hoglin)entity).isBaby();
        }
    }

    protected static void pickUpItem(ServerLevel pLevel, Piglin pPiglin, ItemEntity pItemEntity) {
        stopWalking(pPiglin);
        ItemStack itemstack;
        if (pItemEntity.getItem().is(Items.GOLD_NUGGET)) {
            pPiglin.take(pItemEntity, pItemEntity.getItem().getCount());
            itemstack = pItemEntity.getItem();
            pItemEntity.discard();
        } else {
            pPiglin.take(pItemEntity, 1);
            itemstack = removeOneItemFromItemEntity(pItemEntity);
        }

        if (isLovedItem(itemstack)) {
            pPiglin.getBrain().eraseMemory(MemoryModuleType.TIME_TRYING_TO_REACH_ADMIRE_ITEM);
            holdInOffhand(pLevel, pPiglin, itemstack);
            admireGoldItem(pPiglin);
        } else if (isFood(itemstack) && !hasEatenRecently(pPiglin)) {
            eat(pPiglin);
        } else {
            boolean flag = !pPiglin.equipItemIfPossible(pLevel, itemstack).equals(ItemStack.EMPTY);
            if (!flag) {
                putInInventory(pPiglin, itemstack);
            }
        }
    }

    private static void holdInOffhand(ServerLevel pLevel, Piglin pPiglin, ItemStack pStack) {
        if (isHoldingItemInOffHand(pPiglin)) {
            pPiglin.spawnAtLocation(pLevel, pPiglin.getItemInHand(InteractionHand.OFF_HAND));
        }

        pPiglin.holdInOffHand(pStack);
    }

    private static ItemStack removeOneItemFromItemEntity(ItemEntity pItemEntity) {
        ItemStack itemstack = pItemEntity.getItem();
        ItemStack itemstack1 = itemstack.split(1);
        if (itemstack.isEmpty()) {
            pItemEntity.discard();
        } else {
            pItemEntity.setItem(itemstack);
        }

        return itemstack1;
    }

    protected static void stopHoldingOffHandItem(ServerLevel pLevel, Piglin pPiglin, boolean pBarter) {
        ItemStack itemstack = pPiglin.getItemInHand(InteractionHand.OFF_HAND);
        pPiglin.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        if (pPiglin.isAdult()) {
            boolean flag = isBarterCurrency(itemstack);
            if (pBarter && flag) {
                throwItems(pPiglin, getBarterResponseItems(pPiglin));
            } else if (!flag) {
                boolean flag1 = !pPiglin.equipItemIfPossible(pLevel, itemstack).isEmpty();
                if (!flag1) {
                    putInInventory(pPiglin, itemstack);
                }
            }
        } else {
            boolean flag2 = !pPiglin.equipItemIfPossible(pLevel, itemstack).isEmpty();
            if (!flag2) {
                ItemStack itemstack1 = pPiglin.getMainHandItem();
                if (isLovedItem(itemstack1)) {
                    putInInventory(pPiglin, itemstack1);
                } else {
                    throwItems(pPiglin, Collections.singletonList(itemstack1));
                }

                pPiglin.holdInMainHand(itemstack);
            }
        }
    }

    protected static void cancelAdmiring(ServerLevel pLevel, Piglin pPiglin) {
        if (isAdmiringItem(pPiglin) && !pPiglin.getOffhandItem().isEmpty()) {
            pPiglin.spawnAtLocation(pLevel, pPiglin.getOffhandItem());
            pPiglin.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        }
    }

    private static void putInInventory(Piglin pPiglin, ItemStack pStack) {
        ItemStack itemstack = pPiglin.addToInventory(pStack);
        throwItemsTowardRandomPos(pPiglin, Collections.singletonList(itemstack));
    }

    private static void throwItems(Piglin pPilgin, List<ItemStack> pStacks) {
        Optional<Player> optional = pPilgin.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_PLAYER);
        if (optional.isPresent()) {
            throwItemsTowardPlayer(pPilgin, optional.get(), pStacks);
        } else {
            throwItemsTowardRandomPos(pPilgin, pStacks);
        }
    }

    private static void throwItemsTowardRandomPos(Piglin pPiglin, List<ItemStack> pStacks) {
        throwItemsTowardPos(pPiglin, pStacks, getRandomNearbyPos(pPiglin));
    }

    private static void throwItemsTowardPlayer(Piglin pPiglin, Player pPlayer, List<ItemStack> pStacks) {
        throwItemsTowardPos(pPiglin, pStacks, pPlayer.position());
    }

    private static void throwItemsTowardPos(Piglin pPiglin, List<ItemStack> pStacks, Vec3 pPos) {
        if (!pStacks.isEmpty()) {
            pPiglin.swing(InteractionHand.OFF_HAND);

            for (ItemStack itemstack : pStacks) {
                BehaviorUtils.throwItem(pPiglin, itemstack, pPos.add(0.0, 1.0, 0.0));
            }
        }
    }

    private static List<ItemStack> getBarterResponseItems(Piglin pPiglin) {
        LootTable loottable = pPiglin.level().getServer().reloadableRegistries().getLootTable(BuiltInLootTables.PIGLIN_BARTERING);
        List<ItemStack> list = loottable.getRandomItems(
            new LootParams.Builder((ServerLevel)pPiglin.level()).withParameter(LootContextParams.THIS_ENTITY, pPiglin).create(LootContextParamSets.PIGLIN_BARTER)
        );
        return list;
    }

    private static boolean wantsToDance(LivingEntity pPiglin, LivingEntity pTarget) {
        return pTarget.getType() != EntityType.HOGLIN ? false : RandomSource.create(pPiglin.level().getGameTime()).nextFloat() < 0.1F;
    }

    protected static boolean wantsToPickup(Piglin pPiglin, ItemStack pStack) {
        if (pPiglin.isBaby() && pStack.is(ItemTags.IGNORED_BY_PIGLIN_BABIES)) {
            return false;
        } else if (pStack.is(ItemTags.PIGLIN_REPELLENTS)) {
            return false;
        } else if (isAdmiringDisabled(pPiglin) && pPiglin.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET)) {
            return false;
        } else if (isBarterCurrency(pStack)) {
            return isNotHoldingLovedItemInOffHand(pPiglin);
        } else {
            boolean flag = pPiglin.canAddToInventory(pStack);
            if (pStack.is(Items.GOLD_NUGGET)) {
                return flag;
            } else if (isFood(pStack)) {
                return !hasEatenRecently(pPiglin) && flag;
            } else {
                return !isLovedItem(pStack) ? pPiglin.canReplaceCurrentItem(pStack) : isNotHoldingLovedItemInOffHand(pPiglin) && flag;
            }
        }
    }

    protected static boolean isLovedItem(ItemStack pItem) {
        return pItem.is(ItemTags.PIGLIN_LOVED);
    }

    private static boolean wantsToStopRiding(Piglin pPiglin, Entity pVehicle) {
        return !(pVehicle instanceof Mob mob)
            ? false
            : !mob.isBaby() || !mob.isAlive() || wasHurtRecently(pPiglin) || wasHurtRecently(mob) || mob instanceof Piglin && mob.getVehicle() == null;
    }

    private static boolean isNearestValidAttackTarget(ServerLevel pLevel, Piglin pPiglin, LivingEntity pTarget) {
        return findNearestValidAttackTarget(pLevel, pPiglin).filter(p_34887_ -> p_34887_ == pTarget).isPresent();
    }

    private static boolean isNearZombified(Piglin pPiglin) {
        Brain<Piglin> brain = pPiglin.getBrain();
        if (brain.hasMemoryValue(MemoryModuleType.NEAREST_VISIBLE_ZOMBIFIED)) {
            LivingEntity livingentity = brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_ZOMBIFIED).get();
            return pPiglin.closerThan(livingentity, 6.0);
        } else {
            return false;
        }
    }

    private static Optional<? extends LivingEntity> findNearestValidAttackTarget(ServerLevel pLevel, Piglin pPiglin) {
        Brain<Piglin> brain = pPiglin.getBrain();
        if (isNearZombified(pPiglin)) {
            return Optional.empty();
        } else {
            Optional<LivingEntity> optional = BehaviorUtils.getLivingEntityFromUUIDMemory(pPiglin, MemoryModuleType.ANGRY_AT);
            if (optional.isPresent() && Sensor.isEntityAttackableIgnoringLineOfSight(pLevel, pPiglin, optional.get())) {
                return optional;
            } else {
                if (brain.hasMemoryValue(MemoryModuleType.UNIVERSAL_ANGER)) {
                    Optional<Player> optional1 = brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER);
                    if (optional1.isPresent()) {
                        return optional1;
                    }
                }

                Optional<Mob> optional3 = brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_NEMESIS);
                if (optional3.isPresent()) {
                    return optional3;
                } else {
                    Optional<Player> optional2 = brain.getMemory(MemoryModuleType.NEAREST_TARGETABLE_PLAYER_NOT_WEARING_GOLD);
                    return optional2.isPresent() && Sensor.isEntityAttackable(pLevel, pPiglin, optional2.get()) ? optional2 : Optional.empty();
                }
            }
        }
    }

    public static void angerNearbyPiglins(ServerLevel pLevel, Player pPlayer, boolean pRequireLineOfSight) {
        List<Piglin> list = pPlayer.level().getEntitiesOfClass(Piglin.class, pPlayer.getBoundingBox().inflate(16.0));
        list.stream().filter(PiglinAi::isIdle).filter(p_34881_ -> !pRequireLineOfSight || BehaviorUtils.canSee(p_34881_, pPlayer)).forEach(p_359282_ -> {
            if (pLevel.getGameRules().getBoolean(GameRules.RULE_UNIVERSAL_ANGER)) {
                setAngerTargetToNearestTargetablePlayerIfFound(pLevel, p_359282_, pPlayer);
            } else {
                setAngerTarget(pLevel, p_359282_, pPlayer);
            }
        });
    }

    public static InteractionResult mobInteract(ServerLevel pLevel, Piglin pPiglin, Player pPlayer, InteractionHand pHand) {
        ItemStack itemstack = pPlayer.getItemInHand(pHand);
        if (canAdmire(pPiglin, itemstack)) {
            ItemStack itemstack1 = itemstack.consumeAndReturn(1, pPlayer);
            holdInOffhand(pLevel, pPiglin, itemstack1);
            admireGoldItem(pPiglin);
            stopWalking(pPiglin);
            return InteractionResult.SUCCESS;
        } else {
            return InteractionResult.PASS;
        }
    }

    protected static boolean canAdmire(Piglin pPiglin, ItemStack pStack) {
        return !isAdmiringDisabled(pPiglin) && !isAdmiringItem(pPiglin) && pPiglin.isAdult() && isBarterCurrency(pStack);
    }

    protected static void wasHurtBy(ServerLevel pLevel, Piglin pPiglin, LivingEntity pEntity) {
        if (!(pEntity instanceof Piglin)) {
            if (isHoldingItemInOffHand(pPiglin)) {
                stopHoldingOffHandItem(pLevel, pPiglin, false);
            }

            Brain<Piglin> brain = pPiglin.getBrain();
            brain.eraseMemory(MemoryModuleType.CELEBRATE_LOCATION);
            brain.eraseMemory(MemoryModuleType.DANCING);
            brain.eraseMemory(MemoryModuleType.ADMIRING_ITEM);
            if (pEntity instanceof Player) {
                brain.setMemoryWithExpiry(MemoryModuleType.ADMIRING_DISABLED, true, 400L);
            }

            getAvoidTarget(pPiglin).ifPresent(p_390701_ -> {
                if (p_390701_.getType() != pEntity.getType()) {
                    brain.eraseMemory(MemoryModuleType.AVOID_TARGET);
                }
            });
            if (pPiglin.isBaby()) {
                brain.setMemoryWithExpiry(MemoryModuleType.AVOID_TARGET, pEntity, 100L);
                if (Sensor.isEntityAttackableIgnoringLineOfSight(pLevel, pPiglin, pEntity)) {
                    broadcastAngerTarget(pLevel, pPiglin, pEntity);
                }
            } else if (pEntity.getType() == EntityType.HOGLIN && hoglinsOutnumberPiglins(pPiglin)) {
                setAvoidTargetAndDontHuntForAWhile(pPiglin, pEntity);
                broadcastRetreat(pPiglin, pEntity);
            } else {
                maybeRetaliate(pLevel, pPiglin, pEntity);
            }
        }
    }

    protected static void maybeRetaliate(ServerLevel pLevel, AbstractPiglin pPiglin, LivingEntity pEntity) {
        if (!pPiglin.getBrain().isActive(Activity.AVOID)) {
            if (Sensor.isEntityAttackableIgnoringLineOfSight(pLevel, pPiglin, pEntity)) {
                if (!BehaviorUtils.isOtherTargetMuchFurtherAwayThanCurrentAttackTarget(pPiglin, pEntity, 4.0)) {
                    if (pEntity.getType() == EntityType.PLAYER && pLevel.getGameRules().getBoolean(GameRules.RULE_UNIVERSAL_ANGER)) {
                        setAngerTargetToNearestTargetablePlayerIfFound(pLevel, pPiglin, pEntity);
                        broadcastUniversalAnger(pLevel, pPiglin);
                    } else {
                        setAngerTarget(pLevel, pPiglin, pEntity);
                        broadcastAngerTarget(pLevel, pPiglin, pEntity);
                    }
                }
            }
        }
    }

    public static Optional<SoundEvent> getSoundForCurrentActivity(Piglin pPiglin) {
        return pPiglin.getBrain().getActiveNonCoreActivity().map(p_34908_ -> getSoundForActivity(pPiglin, p_34908_));
    }

    private static SoundEvent getSoundForActivity(Piglin pPiglin, Activity pActivity) {
        if (pActivity == Activity.FIGHT) {
            return SoundEvents.PIGLIN_ANGRY;
        } else if (pPiglin.isConverting()) {
            return SoundEvents.PIGLIN_RETREAT;
        } else if (pActivity == Activity.AVOID && isNearAvoidTarget(pPiglin)) {
            return SoundEvents.PIGLIN_RETREAT;
        } else if (pActivity == Activity.ADMIRE_ITEM) {
            return SoundEvents.PIGLIN_ADMIRING_ITEM;
        } else if (pActivity == Activity.CELEBRATE) {
            return SoundEvents.PIGLIN_CELEBRATE;
        } else if (seesPlayerHoldingLovedItem(pPiglin)) {
            return SoundEvents.PIGLIN_JEALOUS;
        } else {
            return isNearRepellent(pPiglin) ? SoundEvents.PIGLIN_RETREAT : SoundEvents.PIGLIN_AMBIENT;
        }
    }

    private static boolean isNearAvoidTarget(Piglin pPiglin) {
        Brain<Piglin> brain = pPiglin.getBrain();
        return !brain.hasMemoryValue(MemoryModuleType.AVOID_TARGET) ? false : brain.getMemory(MemoryModuleType.AVOID_TARGET).get().closerThan(pPiglin, 12.0);
    }

    protected static List<AbstractPiglin> getVisibleAdultPiglins(Piglin pPiglin) {
        return pPiglin.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_ADULT_PIGLINS).orElse(ImmutableList.of());
    }

    private static List<AbstractPiglin> getAdultPiglins(AbstractPiglin pPiglin) {
        return pPiglin.getBrain().getMemory(MemoryModuleType.NEARBY_ADULT_PIGLINS).orElse(ImmutableList.of());
    }

    public static boolean isWearingSafeArmor(LivingEntity pEntity) {
        for (EquipmentSlot equipmentslot : EquipmentSlotGroup.ARMOR) {
            if (pEntity.getItemBySlot(equipmentslot).makesPiglinsNeutral(pEntity)) {
                return true;
            }
        }

        return false;
    }

    private static void stopWalking(Piglin pPiglin) {
        pPiglin.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        pPiglin.getNavigation().stop();
    }

    private static BehaviorControl<LivingEntity> babySometimesRideBabyHoglin() {
        SetEntityLookTargetSometimes.Ticker setentitylooktargetsometimes$ticker = new SetEntityLookTargetSometimes.Ticker(RIDE_START_INTERVAL);
        return CopyMemoryWithExpiry.create(
            p_375153_ -> p_375153_.isBaby() && setentitylooktargetsometimes$ticker.tickDownAndCheck(p_375153_.level().random),
            MemoryModuleType.NEAREST_VISIBLE_BABY_HOGLIN,
            MemoryModuleType.RIDE_TARGET,
            RIDE_DURATION
        );
    }

    protected static void broadcastAngerTarget(ServerLevel pLevel, AbstractPiglin pPiglin, LivingEntity pAngerTarget) {
        getAdultPiglins(pPiglin).forEach(p_390704_ -> {
            if (pAngerTarget.getType() != EntityType.HOGLIN || p_390704_.canHunt() && ((Hoglin)pAngerTarget).canBeHunted()) {
                setAngerTargetIfCloserThanCurrent(pLevel, p_390704_, pAngerTarget);
            }
        });
    }

    protected static void broadcastUniversalAnger(ServerLevel pLevel, AbstractPiglin pPiglin) {
        getAdultPiglins(pPiglin).forEach(p_359293_ -> getNearestVisibleTargetablePlayer(p_359293_).ifPresent(p_359276_ -> setAngerTarget(pLevel, p_359293_, p_359276_)));
    }

    protected static void setAngerTarget(ServerLevel pLevel, AbstractPiglin pPiglin, LivingEntity pAngerTarget) {
        if (Sensor.isEntityAttackableIgnoringLineOfSight(pLevel, pPiglin, pAngerTarget)) {
            pPiglin.getBrain().eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
            pPiglin.getBrain().setMemoryWithExpiry(MemoryModuleType.ANGRY_AT, pAngerTarget.getUUID(), 600L);
            if (pAngerTarget.getType() == EntityType.HOGLIN && pPiglin.canHunt()) {
                dontKillAnyMoreHoglinsForAWhile(pPiglin);
            }

            if (pAngerTarget.getType() == EntityType.PLAYER && pLevel.getGameRules().getBoolean(GameRules.RULE_UNIVERSAL_ANGER)) {
                pPiglin.getBrain().setMemoryWithExpiry(MemoryModuleType.UNIVERSAL_ANGER, true, 600L);
            }
        }
    }

    private static void setAngerTargetToNearestTargetablePlayerIfFound(ServerLevel pLevel, AbstractPiglin pPiglin, LivingEntity pEntity) {
        Optional<Player> optional = getNearestVisibleTargetablePlayer(pPiglin);
        if (optional.isPresent()) {
            setAngerTarget(pLevel, pPiglin, optional.get());
        } else {
            setAngerTarget(pLevel, pPiglin, pEntity);
        }
    }

    private static void setAngerTargetIfCloserThanCurrent(ServerLevel pLevel, AbstractPiglin pPiglin, LivingEntity pAngerTarget) {
        Optional<LivingEntity> optional = getAngerTarget(pPiglin);
        LivingEntity livingentity = BehaviorUtils.getNearestTarget(pPiglin, optional, pAngerTarget);
        if (!optional.isPresent() || optional.get() != livingentity) {
            setAngerTarget(pLevel, pPiglin, livingentity);
        }
    }

    private static Optional<LivingEntity> getAngerTarget(AbstractPiglin pPiglin) {
        return BehaviorUtils.getLivingEntityFromUUIDMemory(pPiglin, MemoryModuleType.ANGRY_AT);
    }

    public static Optional<LivingEntity> getAvoidTarget(Piglin pPiglin) {
        return pPiglin.getBrain().hasMemoryValue(MemoryModuleType.AVOID_TARGET) ? pPiglin.getBrain().getMemory(MemoryModuleType.AVOID_TARGET) : Optional.empty();
    }

    public static Optional<Player> getNearestVisibleTargetablePlayer(AbstractPiglin pPiglin) {
        return pPiglin.getBrain().hasMemoryValue(MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER) ? pPiglin.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER) : Optional.empty();
    }

    private static void broadcastRetreat(Piglin pPiglin, LivingEntity pTarget) {
        getVisibleAdultPiglins(pPiglin).stream().filter(p_34985_ -> p_34985_ instanceof Piglin).forEach(p_34819_ -> retreatFromNearestTarget((Piglin)p_34819_, pTarget));
    }

    private static void retreatFromNearestTarget(Piglin pPiglin, LivingEntity pTarget) {
        Brain<Piglin> brain = pPiglin.getBrain();
        LivingEntity $$3 = BehaviorUtils.getNearestTarget(pPiglin, brain.getMemory(MemoryModuleType.AVOID_TARGET), pTarget);
        $$3 = BehaviorUtils.getNearestTarget(pPiglin, brain.getMemory(MemoryModuleType.ATTACK_TARGET), $$3);
        setAvoidTargetAndDontHuntForAWhile(pPiglin, $$3);
    }

    private static boolean wantsToStopFleeing(Piglin pPiglin) {
        Brain<Piglin> brain = pPiglin.getBrain();
        if (!brain.hasMemoryValue(MemoryModuleType.AVOID_TARGET)) {
            return true;
        } else {
            LivingEntity livingentity = brain.getMemory(MemoryModuleType.AVOID_TARGET).get();
            EntityType<?> entitytype = livingentity.getType();
            if (entitytype == EntityType.HOGLIN) {
                return piglinsEqualOrOutnumberHoglins(pPiglin);
            } else {
                return isZombified(entitytype) ? !brain.isMemoryValue(MemoryModuleType.NEAREST_VISIBLE_ZOMBIFIED, livingentity) : false;
            }
        }
    }

    private static boolean piglinsEqualOrOutnumberHoglins(Piglin pPiglin) {
        return !hoglinsOutnumberPiglins(pPiglin);
    }

    private static boolean hoglinsOutnumberPiglins(Piglin pPiglin) {
        int i = pPiglin.getBrain().getMemory(MemoryModuleType.VISIBLE_ADULT_PIGLIN_COUNT).orElse(0) + 1;
        int j = pPiglin.getBrain().getMemory(MemoryModuleType.VISIBLE_ADULT_HOGLIN_COUNT).orElse(0);
        return j > i;
    }

    private static void setAvoidTargetAndDontHuntForAWhile(Piglin pPiglin, LivingEntity pTarget) {
        pPiglin.getBrain().eraseMemory(MemoryModuleType.ANGRY_AT);
        pPiglin.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        pPiglin.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        pPiglin.getBrain().setMemoryWithExpiry(MemoryModuleType.AVOID_TARGET, pTarget, RETREAT_DURATION.sample(pPiglin.level().random));
        dontKillAnyMoreHoglinsForAWhile(pPiglin);
    }

    protected static void dontKillAnyMoreHoglinsForAWhile(AbstractPiglin pPiglin) {
        pPiglin.getBrain().setMemoryWithExpiry(MemoryModuleType.HUNTED_RECENTLY, true, TIME_BETWEEN_HUNTS.sample(pPiglin.level().random));
    }

    private static void eat(Piglin pPiglin) {
        pPiglin.getBrain().setMemoryWithExpiry(MemoryModuleType.ATE_RECENTLY, true, 200L);
    }

    private static Vec3 getRandomNearbyPos(Piglin pPiglin) {
        Vec3 vec3 = LandRandomPos.getPos(pPiglin, 4, 2);
        return vec3 == null ? pPiglin.position() : vec3;
    }

    private static boolean hasEatenRecently(Piglin pPiglin) {
        return pPiglin.getBrain().hasMemoryValue(MemoryModuleType.ATE_RECENTLY);
    }

    protected static boolean isIdle(AbstractPiglin pPiglin) {
        return pPiglin.getBrain().isActive(Activity.IDLE);
    }

    private static boolean hasCrossbow(LivingEntity pPiglin) {
        return pPiglin.isHolding(is -> is.getItem() instanceof net.minecraft.world.item.CrossbowItem);
    }

    private static void admireGoldItem(LivingEntity pPiglin) {
        pPiglin.getBrain().setMemoryWithExpiry(MemoryModuleType.ADMIRING_ITEM, true, 119L);
    }

    private static boolean isAdmiringItem(Piglin pPiglin) {
        return pPiglin.getBrain().hasMemoryValue(MemoryModuleType.ADMIRING_ITEM);
    }

    private static boolean isBarterCurrency(ItemStack pStack) {
        return pStack.is(BARTERING_ITEM) || pStack.isPiglinCurrency();
    }

    private static boolean isFood(ItemStack pStack) {
        return pStack.is(ItemTags.PIGLIN_FOOD);
    }

    private static boolean isNearRepellent(Piglin pPiglin) {
        return pPiglin.getBrain().hasMemoryValue(MemoryModuleType.NEAREST_REPELLENT);
    }

    private static boolean seesPlayerHoldingLovedItem(LivingEntity pPiglin) {
        return pPiglin.getBrain().hasMemoryValue(MemoryModuleType.NEAREST_PLAYER_HOLDING_WANTED_ITEM);
    }

    private static boolean doesntSeeAnyPlayerHoldingLovedItem(LivingEntity pPiglin) {
        return !seesPlayerHoldingLovedItem(pPiglin);
    }

    public static boolean isPlayerHoldingLovedItem(LivingEntity pPlayer) {
        return pPlayer.getType() == EntityType.PLAYER && pPlayer.isHolding(PiglinAi::isLovedItem);
    }

    private static boolean isAdmiringDisabled(Piglin pPiglin) {
        return pPiglin.getBrain().hasMemoryValue(MemoryModuleType.ADMIRING_DISABLED);
    }

    private static boolean wasHurtRecently(LivingEntity pPiglin) {
        return pPiglin.getBrain().hasMemoryValue(MemoryModuleType.HURT_BY);
    }

    private static boolean isHoldingItemInOffHand(Piglin pPiglin) {
        return !pPiglin.getOffhandItem().isEmpty();
    }

    private static boolean isNotHoldingLovedItemInOffHand(Piglin pPiglin) {
        return pPiglin.getOffhandItem().isEmpty() || !isLovedItem(pPiglin.getOffhandItem());
    }

    public static boolean isZombified(EntityType<?> pEntityType) {
        return pEntityType == EntityType.ZOMBIFIED_PIGLIN || pEntityType == EntityType.ZOGLIN;
    }
}
