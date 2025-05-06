package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootTable;

public class GiveGiftToHero extends Behavior<Villager> {
    private static final int THROW_GIFT_AT_DISTANCE = 5;
    private static final int MIN_TIME_BETWEEN_GIFTS = 600;
    private static final int MAX_TIME_BETWEEN_GIFTS = 6600;
    private static final int TIME_TO_DELAY_FOR_HEAD_TO_FINISH_TURNING = 20;
    private static final Map<ResourceKey<VillagerProfession>, ResourceKey<LootTable>> GIFTS = ImmutableMap.<ResourceKey<VillagerProfession>, ResourceKey<LootTable>>builder()
        .put(VillagerProfession.ARMORER, BuiltInLootTables.ARMORER_GIFT)
        .put(VillagerProfession.BUTCHER, BuiltInLootTables.BUTCHER_GIFT)
        .put(VillagerProfession.CARTOGRAPHER, BuiltInLootTables.CARTOGRAPHER_GIFT)
        .put(VillagerProfession.CLERIC, BuiltInLootTables.CLERIC_GIFT)
        .put(VillagerProfession.FARMER, BuiltInLootTables.FARMER_GIFT)
        .put(VillagerProfession.FISHERMAN, BuiltInLootTables.FISHERMAN_GIFT)
        .put(VillagerProfession.FLETCHER, BuiltInLootTables.FLETCHER_GIFT)
        .put(VillagerProfession.LEATHERWORKER, BuiltInLootTables.LEATHERWORKER_GIFT)
        .put(VillagerProfession.LIBRARIAN, BuiltInLootTables.LIBRARIAN_GIFT)
        .put(VillagerProfession.MASON, BuiltInLootTables.MASON_GIFT)
        .put(VillagerProfession.SHEPHERD, BuiltInLootTables.SHEPHERD_GIFT)
        .put(VillagerProfession.TOOLSMITH, BuiltInLootTables.TOOLSMITH_GIFT)
        .put(VillagerProfession.WEAPONSMITH, BuiltInLootTables.WEAPONSMITH_GIFT)
        .build();
    private static final float SPEED_MODIFIER = 0.5F;
    private int timeUntilNextGift = 600;
    private boolean giftGivenDuringThisRun;
    private long timeSinceStart;

    public GiveGiftToHero(int pDuration) {
        super(
            ImmutableMap.of(
                MemoryModuleType.WALK_TARGET,
                MemoryStatus.REGISTERED,
                MemoryModuleType.LOOK_TARGET,
                MemoryStatus.REGISTERED,
                MemoryModuleType.INTERACTION_TARGET,
                MemoryStatus.REGISTERED,
                MemoryModuleType.NEAREST_VISIBLE_PLAYER,
                MemoryStatus.VALUE_PRESENT
            ),
            pDuration
        );
    }

    protected boolean checkExtraStartConditions(ServerLevel pLevel, Villager pOwner) {
        if (!this.isHeroVisible(pOwner)) {
            return false;
        } else if (this.timeUntilNextGift > 0) {
            this.timeUntilNextGift--;
            return false;
        } else {
            return true;
        }
    }

    protected void start(ServerLevel pLevel, Villager pEntity, long pGameTime) {
        this.giftGivenDuringThisRun = false;
        this.timeSinceStart = pGameTime;
        Player player = this.getNearestTargetableHero(pEntity).get();
        pEntity.getBrain().setMemory(MemoryModuleType.INTERACTION_TARGET, player);
        BehaviorUtils.lookAtEntity(pEntity, player);
    }

    protected boolean canStillUse(ServerLevel pLevel, Villager pEntity, long pGameTime) {
        return this.isHeroVisible(pEntity) && !this.giftGivenDuringThisRun;
    }

    protected void tick(ServerLevel pLevel, Villager pOwner, long pGameTime) {
        Player player = this.getNearestTargetableHero(pOwner).get();
        BehaviorUtils.lookAtEntity(pOwner, player);
        if (this.isWithinThrowingDistance(pOwner, player)) {
            if (pGameTime - this.timeSinceStart > 20L) {
                this.throwGift(pLevel, pOwner, player);
                this.giftGivenDuringThisRun = true;
            }
        } else {
            BehaviorUtils.setWalkAndLookTargetMemories(pOwner, player, 0.5F, 5);
        }
    }

    protected void stop(ServerLevel pLevel, Villager pEntity, long pGameTime) {
        this.timeUntilNextGift = calculateTimeUntilNextGift(pLevel);
        pEntity.getBrain().eraseMemory(MemoryModuleType.INTERACTION_TARGET);
        pEntity.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        pEntity.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
    }

    private void throwGift(ServerLevel pLevel, Villager pVillager, LivingEntity pTarget) {
        pVillager.dropFromGiftLootTable(pLevel, getLootTableToThrow(pVillager), (p_374983_, p_374984_) -> BehaviorUtils.throwItem(pVillager, p_374984_, pTarget.position()));
    }

    private static ResourceKey<LootTable> getLootTableToThrow(Villager pVillager) {
        if (pVillager.isBaby()) {
            return BuiltInLootTables.BABY_VILLAGER_GIFT;
        } else {
            Optional<ResourceKey<VillagerProfession>> optional = pVillager.getVillagerData().profession().unwrapKey();
            return optional.isEmpty() ? BuiltInLootTables.UNEMPLOYED_GIFT : GIFTS.getOrDefault(optional.get(), BuiltInLootTables.UNEMPLOYED_GIFT);
        }
    }

    private boolean isHeroVisible(Villager pVillager) {
        return this.getNearestTargetableHero(pVillager).isPresent();
    }

    private Optional<Player> getNearestTargetableHero(Villager pVillager) {
        return pVillager.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_PLAYER).filter(this::isHero);
    }

    private boolean isHero(Player pPlayer) {
        return pPlayer.hasEffect(MobEffects.HERO_OF_THE_VILLAGE);
    }

    private boolean isWithinThrowingDistance(Villager pVillager, Player pHero) {
        BlockPos blockpos = pHero.blockPosition();
        BlockPos blockpos1 = pVillager.blockPosition();
        return blockpos1.closerThan(blockpos, 5.0);
    }

    private static int calculateTimeUntilNextGift(ServerLevel pLevel) {
        return 600 + pLevel.random.nextInt(6001);
    }
}