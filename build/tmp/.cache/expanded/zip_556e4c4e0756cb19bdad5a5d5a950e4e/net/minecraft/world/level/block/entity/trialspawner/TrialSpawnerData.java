package net.minecraft.world.level.block.entity.trialspawner;

import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

public class TrialSpawnerData {
    public static final String TAG_SPAWN_DATA = "spawn_data";
    private static final String TAG_NEXT_MOB_SPAWNS_AT = "next_mob_spawns_at";
    private static final int DELAY_BETWEEN_PLAYER_SCANS = 20;
    private static final int TRIAL_OMEN_PER_BAD_OMEN_LEVEL = 18000;
    public static MapCodec<TrialSpawnerData> MAP_CODEC = RecordCodecBuilder.mapCodec(
        p_313188_ -> p_313188_.group(
                UUIDUtil.CODEC_SET.lenientOptionalFieldOf("registered_players", Sets.newHashSet()).forGetter(p_309580_ -> p_309580_.detectedPlayers),
                UUIDUtil.CODEC_SET.lenientOptionalFieldOf("current_mobs", Sets.newHashSet()).forGetter(p_311034_ -> p_311034_.currentMobs),
                Codec.LONG.lenientOptionalFieldOf("cooldown_ends_at", 0L).forGetter(p_309685_ -> p_309685_.cooldownEndsAt),
                Codec.LONG.lenientOptionalFieldOf("next_mob_spawns_at", 0L).forGetter(p_310876_ -> p_310876_.nextMobSpawnsAt),
                Codec.intRange(0, Integer.MAX_VALUE).lenientOptionalFieldOf("total_mobs_spawned", 0).forGetter(p_309745_ -> p_309745_.totalMobsSpawned),
                SpawnData.CODEC.lenientOptionalFieldOf("spawn_data").forGetter(p_312904_ -> p_312904_.nextSpawnData),
                LootTable.KEY_CODEC.lenientOptionalFieldOf("ejecting_loot_table").forGetter(p_310765_ -> p_310765_.ejectingLootTable)
            )
            .apply(p_313188_, TrialSpawnerData::new)
    );
    protected final Set<UUID> detectedPlayers = new HashSet<>();
    protected final Set<UUID> currentMobs = new HashSet<>();
    protected long cooldownEndsAt;
    protected long nextMobSpawnsAt;
    protected int totalMobsSpawned;
    protected Optional<SpawnData> nextSpawnData;
    protected Optional<ResourceKey<LootTable>> ejectingLootTable;
    @Nullable
    protected Entity displayEntity;
    @Nullable
    private WeightedList<ItemStack> dispensing;
    protected double spin;
    protected double oSpin;

    public TrialSpawnerData() {
        this(Collections.emptySet(), Collections.emptySet(), 0L, 0L, 0, Optional.empty(), Optional.empty());
    }

    public TrialSpawnerData(
        Set<UUID> pDetectedPlayers,
        Set<UUID> pCurrentMobs,
        long pCooldownEndsAt,
        long pNextMobSpawnsAt,
        int pTotalMobsSpawned,
        Optional<SpawnData> pNextSpawnData,
        Optional<ResourceKey<LootTable>> pEjectingLootTable
    ) {
        this.detectedPlayers.addAll(pDetectedPlayers);
        this.currentMobs.addAll(pCurrentMobs);
        this.cooldownEndsAt = pCooldownEndsAt;
        this.nextMobSpawnsAt = pNextMobSpawnsAt;
        this.totalMobsSpawned = pTotalMobsSpawned;
        this.nextSpawnData = pNextSpawnData;
        this.ejectingLootTable = pEjectingLootTable;
    }

    public void reset() {
        this.currentMobs.clear();
        this.nextSpawnData = Optional.empty();
        this.resetStatistics();
    }

    public void resetStatistics() {
        this.detectedPlayers.clear();
        this.totalMobsSpawned = 0;
        this.nextMobSpawnsAt = 0L;
        this.cooldownEndsAt = 0L;
    }

    public boolean hasMobToSpawn(TrialSpawner pTrialSpawner, RandomSource pRandom) {
        boolean flag = this.getOrCreateNextSpawnData(pTrialSpawner, pRandom).getEntityToSpawn().getString("id").isPresent();
        return flag || !pTrialSpawner.getConfig().spawnPotentialsDefinition().isEmpty();
    }

    public boolean hasFinishedSpawningAllMobs(TrialSpawnerConfig pConfig, int pPlayers) {
        return this.totalMobsSpawned >= pConfig.calculateTargetTotalMobs(pPlayers);
    }

    public boolean haveAllCurrentMobsDied() {
        return this.currentMobs.isEmpty();
    }

    public boolean isReadyToSpawnNextMob(ServerLevel pLevel, TrialSpawnerConfig pConfig, int pPlayers) {
        return pLevel.getGameTime() >= this.nextMobSpawnsAt && this.currentMobs.size() < pConfig.calculateTargetSimultaneousMobs(pPlayers);
    }

    public int countAdditionalPlayers(BlockPos pPos) {
        if (this.detectedPlayers.isEmpty()) {
            Util.logAndPauseIfInIde("Trial Spawner at " + pPos + " has no detected players");
        }

        return Math.max(0, this.detectedPlayers.size() - 1);
    }

    public void tryDetectPlayers(ServerLevel pLevel, BlockPos pPos, TrialSpawner pSpawner) {
        boolean flag = (pPos.asLong() + pLevel.getGameTime()) % 20L != 0L;
        if (!flag) {
            if (!pSpawner.getState().equals(TrialSpawnerState.COOLDOWN) || !pSpawner.isOminous()) {
                List<UUID> list = pSpawner.getPlayerDetector().detect(pLevel, pSpawner.getEntitySelector(), pPos, pSpawner.getRequiredPlayerRange(), true);
                boolean flag1;
                if (!pSpawner.isOminous() && !list.isEmpty()) {
                    Optional<Pair<Player, Holder<MobEffect>>> optional = findPlayerWithOminousEffect(pLevel, list);
                    optional.ifPresent(p_390992_ -> {
                        Player player = p_390992_.getFirst();
                        if (p_390992_.getSecond() == MobEffects.BAD_OMEN) {
                            transformBadOmenIntoTrialOmen(player);
                        }

                        pLevel.levelEvent(3020, BlockPos.containing(player.getEyePosition()), 0);
                        pSpawner.applyOminous(pLevel, pPos);
                    });
                    flag1 = optional.isPresent();
                } else {
                    flag1 = false;
                }

                if (!pSpawner.getState().equals(TrialSpawnerState.COOLDOWN) || flag1) {
                    boolean flag2 = pSpawner.getData().detectedPlayers.isEmpty();
                    List<UUID> list1 = flag2
                        ? list
                        : pSpawner.getPlayerDetector().detect(pLevel, pSpawner.getEntitySelector(), pPos, pSpawner.getRequiredPlayerRange(), false);
                    if (this.detectedPlayers.addAll(list1)) {
                        this.nextMobSpawnsAt = Math.max(pLevel.getGameTime() + 40L, this.nextMobSpawnsAt);
                        if (!flag1) {
                            int i = pSpawner.isOminous() ? 3019 : 3013;
                            pLevel.levelEvent(i, pPos, this.detectedPlayers.size());
                        }
                    }
                }
            }
        }
    }

    private static Optional<Pair<Player, Holder<MobEffect>>> findPlayerWithOminousEffect(ServerLevel pLevel, List<UUID> pPlayers) {
        Player player = null;

        for (UUID uuid : pPlayers) {
            Player player1 = pLevel.getPlayerByUUID(uuid);
            if (player1 != null) {
                Holder<MobEffect> holder = MobEffects.TRIAL_OMEN;
                if (player1.hasEffect(holder)) {
                    return Optional.of(Pair.of(player1, holder));
                }

                if (player1.hasEffect(MobEffects.BAD_OMEN)) {
                    player = player1;
                }
            }
        }

        return Optional.ofNullable(player).map(p_341863_ -> Pair.of(p_341863_, MobEffects.BAD_OMEN));
    }

    public void resetAfterBecomingOminous(TrialSpawner pSpawner, ServerLevel pLevel) {
        this.currentMobs.stream().map(pLevel::getEntity).forEach(p_360528_ -> {
            if (p_360528_ != null) {
                pLevel.levelEvent(3012, p_360528_.blockPosition(), TrialSpawner.FlameParticle.NORMAL.encode());
                if (p_360528_ instanceof Mob mob) {
                    mob.dropPreservedEquipment(pLevel);
                }

                p_360528_.remove(Entity.RemovalReason.DISCARDED);
            }
        });
        if (!pSpawner.getOminousConfig().spawnPotentialsDefinition().isEmpty()) {
            this.nextSpawnData = Optional.empty();
        }

        this.totalMobsSpawned = 0;
        this.currentMobs.clear();
        this.nextMobSpawnsAt = pLevel.getGameTime() + pSpawner.getOminousConfig().ticksBetweenSpawn();
        pSpawner.markUpdated();
        this.cooldownEndsAt = pLevel.getGameTime() + pSpawner.getOminousConfig().ticksBetweenItemSpawners();
    }

    private static void transformBadOmenIntoTrialOmen(Player pPlayer) {
        MobEffectInstance mobeffectinstance = pPlayer.getEffect(MobEffects.BAD_OMEN);
        if (mobeffectinstance != null) {
            int i = mobeffectinstance.getAmplifier() + 1;
            int j = 18000 * i;
            pPlayer.removeEffect(MobEffects.BAD_OMEN);
            pPlayer.addEffect(new MobEffectInstance(MobEffects.TRIAL_OMEN, j, 0));
        }
    }

    public boolean isReadyToOpenShutter(ServerLevel pLevel, float pDelay, int pTargetCooldownLength) {
        long i = this.cooldownEndsAt - pTargetCooldownLength;
        return (float)pLevel.getGameTime() >= (float)i + pDelay;
    }

    public boolean isReadyToEjectItems(ServerLevel pLevel, float pDelay, int pTargetCooldownLength) {
        long i = this.cooldownEndsAt - pTargetCooldownLength;
        return (float)(pLevel.getGameTime() - i) % pDelay == 0.0F;
    }

    public boolean isCooldownFinished(ServerLevel pLevel) {
        return pLevel.getGameTime() >= this.cooldownEndsAt;
    }

    protected SpawnData getOrCreateNextSpawnData(TrialSpawner pSpawner, RandomSource pRandom) {
        if (this.nextSpawnData.isPresent()) {
            return this.nextSpawnData.get();
        } else {
            WeightedList<SpawnData> weightedlist = pSpawner.getConfig().spawnPotentialsDefinition();
            Optional<SpawnData> optional = weightedlist.isEmpty() ? this.nextSpawnData : weightedlist.getRandom(pRandom);
            this.nextSpawnData = Optional.of(optional.orElseGet(SpawnData::new));
            pSpawner.markUpdated();
            return this.nextSpawnData.get();
        }
    }

    @Nullable
    public Entity getOrCreateDisplayEntity(TrialSpawner pSpawner, Level pLevel, TrialSpawnerState pSpawnerState) {
        if (!pSpawnerState.hasSpinningMob()) {
            return null;
        } else {
            if (this.displayEntity == null) {
                CompoundTag compoundtag = this.getOrCreateNextSpawnData(pSpawner, pLevel.getRandom()).getEntityToSpawn();
                if (compoundtag.getString("id").isPresent()) {
                    this.displayEntity = EntityType.loadEntityRecursive(compoundtag, pLevel, EntitySpawnReason.TRIAL_SPAWNER, Function.identity());
                }
            }

            return this.displayEntity;
        }
    }

    public CompoundTag getUpdateTag(TrialSpawnerState pSpawnerState) {
        CompoundTag compoundtag = new CompoundTag();
        if (pSpawnerState == TrialSpawnerState.ACTIVE) {
            compoundtag.putLong("next_mob_spawns_at", this.nextMobSpawnsAt);
        }

        this.nextSpawnData.ifPresent(p_390988_ -> compoundtag.store("spawn_data", SpawnData.CODEC, p_390988_));
        return compoundtag;
    }

    public double getSpin() {
        return this.spin;
    }

    public double getOSpin() {
        return this.oSpin;
    }

    WeightedList<ItemStack> getDispensingItems(ServerLevel pLevel, TrialSpawnerConfig pConfig, BlockPos pPos) {
        if (this.dispensing != null) {
            return this.dispensing;
        } else {
            LootTable loottable = pLevel.getServer().reloadableRegistries().getLootTable(pConfig.itemsToDropWhenOminous());
            LootParams lootparams = new LootParams.Builder(pLevel).create(LootContextParamSets.EMPTY);
            long i = lowResolutionPosition(pLevel, pPos);
            ObjectArrayList<ItemStack> objectarraylist = loottable.getRandomItems(lootparams, i);
            if (objectarraylist.isEmpty()) {
                return WeightedList.of();
            } else {
                WeightedList.Builder<ItemStack> builder = WeightedList.builder();

                for (ItemStack itemstack : objectarraylist) {
                    builder.add(itemstack.copyWithCount(1), itemstack.getCount());
                }

                this.dispensing = builder.build();
                return this.dispensing;
            }
        }
    }

    private static long lowResolutionPosition(ServerLevel pLevel, BlockPos pPos) {
        BlockPos blockpos = new BlockPos(
            Mth.floor(pPos.getX() / 30.0F), Mth.floor(pPos.getY() / 20.0F), Mth.floor(pPos.getZ() / 30.0F)
        );
        return pLevel.getSeed() + blockpos.asLong();
    }
}