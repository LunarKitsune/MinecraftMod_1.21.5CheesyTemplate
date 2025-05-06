package net.minecraft.world.level;

import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public abstract class BaseSpawner {
    public static final String SPAWN_DATA_TAG = "SpawnData";
    private static final int EVENT_SPAWN = 1;
    private static final int DEFAULT_SPAWN_DELAY = 20;
    private static final int DEFAULT_MIN_SPAWN_DELAY = 200;
    private static final int DEFAULT_MAX_SPAWN_DELAY = 800;
    private static final int DEFAULT_SPAWN_COUNT = 4;
    private static final int DEFAULT_MAX_NEARBY_ENTITIES = 6;
    private static final int DEFAULT_REQUIRED_PLAYER_RANGE = 16;
    private static final int DEFAULT_SPAWN_RANGE = 4;
    private int spawnDelay = 20;
    private WeightedList<SpawnData> spawnPotentials = WeightedList.of();
    @Nullable
    private SpawnData nextSpawnData;
    private double spin;
    private double oSpin;
    private int minSpawnDelay = 200;
    private int maxSpawnDelay = 800;
    private int spawnCount = 4;
    @Nullable
    private Entity displayEntity;
    private int maxNearbyEntities = 6;
    private int requiredPlayerRange = 16;
    private int spawnRange = 4;

    public void setEntityId(EntityType<?> pType, @Nullable Level pLevel, RandomSource pRandom, BlockPos pPos) {
        this.getOrCreateNextSpawnData(pLevel, pRandom, pPos).getEntityToSpawn().putString("id", BuiltInRegistries.ENTITY_TYPE.getKey(pType).toString());
    }

    private boolean isNearPlayer(Level pLevel, BlockPos pPos) {
        return pLevel.hasNearbyAlivePlayer(pPos.getX() + 0.5, pPos.getY() + 0.5, pPos.getZ() + 0.5, this.requiredPlayerRange);
    }

    public void clientTick(Level pLevel, BlockPos pPos) {
        if (!this.isNearPlayer(pLevel, pPos)) {
            this.oSpin = this.spin;
        } else if (this.displayEntity != null) {
            RandomSource randomsource = pLevel.getRandom();
            double d0 = pPos.getX() + randomsource.nextDouble();
            double d1 = pPos.getY() + randomsource.nextDouble();
            double d2 = pPos.getZ() + randomsource.nextDouble();
            pLevel.addParticle(ParticleTypes.SMOKE, d0, d1, d2, 0.0, 0.0, 0.0);
            pLevel.addParticle(ParticleTypes.FLAME, d0, d1, d2, 0.0, 0.0, 0.0);
            if (this.spawnDelay > 0) {
                this.spawnDelay--;
            }

            this.oSpin = this.spin;
            this.spin = (this.spin + 1000.0F / (this.spawnDelay + 200.0F)) % 360.0;
        }
    }

    public void serverTick(ServerLevel pServerLevel, BlockPos pPos) {
        if (this.isNearPlayer(pServerLevel, pPos)) {
            if (this.spawnDelay == -1) {
                this.delay(pServerLevel, pPos);
            }

            if (this.spawnDelay > 0) {
                this.spawnDelay--;
            } else {
                boolean flag = false;
                RandomSource randomsource = pServerLevel.getRandom();
                SpawnData spawndata = this.getOrCreateNextSpawnData(pServerLevel, randomsource, pPos);

                for (int i = 0; i < this.spawnCount; i++) {
                    CompoundTag compoundtag = spawndata.getEntityToSpawn();
                    Optional<EntityType<?>> optional = EntityType.by(compoundtag);
                    if (optional.isEmpty()) {
                        this.delay(pServerLevel, pPos);
                        return;
                    }

                    Vec3 vec3 = compoundtag.read("Pos", Vec3.CODEC)
                        .orElseGet(
                            () -> new Vec3(
                                pPos.getX() + (randomsource.nextDouble() - randomsource.nextDouble()) * this.spawnRange + 0.5,
                                pPos.getY() + randomsource.nextInt(3) - 1,
                                pPos.getZ() + (randomsource.nextDouble() - randomsource.nextDouble()) * this.spawnRange + 0.5
                            )
                        );
                    if (pServerLevel.noCollision(optional.get().getSpawnAABB(vec3.x, vec3.y, vec3.z))) {
                        BlockPos blockpos = BlockPos.containing(vec3);
                        if (spawndata.getCustomSpawnRules().isPresent()) {
                            if (!optional.get().getCategory().isFriendly() && pServerLevel.getDifficulty() == Difficulty.PEACEFUL) {
                                continue;
                            }

                            SpawnData.CustomSpawnRules spawndata$customspawnrules = spawndata.getCustomSpawnRules().get();
                            if (!spawndata$customspawnrules.isValidPosition(blockpos, pServerLevel)) {
                                continue;
                            }
                        } else if (!SpawnPlacements.checkSpawnRules(optional.get(), pServerLevel, EntitySpawnReason.SPAWNER, blockpos, pServerLevel.getRandom())) {
                            continue;
                        }

                        Entity entity = EntityType.loadEntityRecursive(compoundtag, pServerLevel, EntitySpawnReason.SPAWNER, p_390874_ -> {
                            p_390874_.snapTo(vec3.x, vec3.y, vec3.z, p_390874_.getYRot(), p_390874_.getXRot());
                            return p_390874_;
                        });
                        if (entity == null) {
                            this.delay(pServerLevel, pPos);
                            return;
                        }

                        int j = pServerLevel.getEntities(
                                EntityTypeTest.forExactClass(entity.getClass()),
                                new AABB(
                                        pPos.getX(),
                                        pPos.getY(),
                                        pPos.getZ(),
                                        pPos.getX() + 1,
                                        pPos.getY() + 1,
                                        pPos.getZ() + 1
                                    )
                                    .inflate(this.spawnRange),
                                EntitySelector.NO_SPECTATORS
                            )
                            .size();
                        if (j >= this.maxNearbyEntities) {
                            this.delay(pServerLevel, pPos);
                            return;
                        }

                        entity.snapTo(entity.getX(), entity.getY(), entity.getZ(), randomsource.nextFloat() * 360.0F, 0.0F);
                        if (entity instanceof Mob mob) {
                            if (!net.minecraftforge.event.ForgeEventFactory.checkSpawnPositionSpawner(mob, pServerLevel, EntitySpawnReason.SPAWNER, spawndata, this)) {
                                continue;
                            }

                            boolean flag1 = spawndata.getEntityToSpawn().size() == 1 && spawndata.getEntityToSpawn().getString("id").isPresent();
                            // Forge: Patch in FinalizeSpawn for spawners so it may be fired unconditionally, instead of only when vanilla normally would trigger it.
                            var event = net.minecraftforge.event.ForgeEventFactory.onFinalizeSpawnSpawner(mob, pServerLevel, pServerLevel.getCurrentDifficultyAt(entity.blockPosition()), null, compoundtag, this);
                            if (!event.isCanceled() && flag1) {
                                ((Mob)entity).finalizeSpawn(pServerLevel, event.getDifficulty(), EntitySpawnReason.SPAWNER, null);
                            }

                            spawndata.getEquipment().ifPresent(mob::equip);
                        }

                        if (!pServerLevel.tryAddFreshEntityWithPassengers(entity)) {
                            this.delay(pServerLevel, pPos);
                            return;
                        }

                        pServerLevel.levelEvent(2004, pPos, 0);
                        pServerLevel.gameEvent(entity, GameEvent.ENTITY_PLACE, blockpos);
                        if (entity instanceof Mob) {
                            ((Mob)entity).spawnAnim();
                        }

                        flag = true;
                    }
                }

                if (flag) {
                    this.delay(pServerLevel, pPos);
                }
            }
        }
    }

    private void delay(Level pLevel, BlockPos pPos) {
        RandomSource randomsource = pLevel.random;
        if (this.maxSpawnDelay <= this.minSpawnDelay) {
            this.spawnDelay = this.minSpawnDelay;
        } else {
            this.spawnDelay = this.minSpawnDelay + randomsource.nextInt(this.maxSpawnDelay - this.minSpawnDelay);
        }

        this.spawnPotentials.getRandom(randomsource).ifPresent(p_390869_ -> this.setNextSpawnData(pLevel, pPos, p_390869_));
        this.broadcastEvent(pLevel, pPos, 1);
    }

    public void load(@Nullable Level pLevel, BlockPos pPos, CompoundTag pTag) {
        this.spawnDelay = pTag.getShortOr("Delay", (short)20);
        pTag.read("SpawnData", SpawnData.CODEC).ifPresent(p_390872_ -> this.setNextSpawnData(pLevel, pPos, p_390872_));
        this.spawnPotentials = pTag.read("SpawnPotentials", SpawnData.LIST_CODEC)
            .orElseGet(() -> WeightedList.of(this.nextSpawnData != null ? this.nextSpawnData : new SpawnData()));
        this.minSpawnDelay = pTag.getIntOr("MinSpawnDelay", 200);
        this.maxSpawnDelay = pTag.getIntOr("MaxSpawnDelay", 800);
        this.spawnCount = pTag.getIntOr("SpawnCount", 4);
        this.maxNearbyEntities = pTag.getIntOr("MaxNearbyEntities", 6);
        this.requiredPlayerRange = pTag.getIntOr("RequiredPlayerRange", 16);
        this.spawnRange = pTag.getIntOr("SpawnRange", 4);
        this.displayEntity = null;
    }

    public CompoundTag save(CompoundTag pTag) {
        pTag.putShort("Delay", (short)this.spawnDelay);
        pTag.putShort("MinSpawnDelay", (short)this.minSpawnDelay);
        pTag.putShort("MaxSpawnDelay", (short)this.maxSpawnDelay);
        pTag.putShort("SpawnCount", (short)this.spawnCount);
        pTag.putShort("MaxNearbyEntities", (short)this.maxNearbyEntities);
        pTag.putShort("RequiredPlayerRange", (short)this.requiredPlayerRange);
        pTag.putShort("SpawnRange", (short)this.spawnRange);
        pTag.storeNullable("SpawnData", SpawnData.CODEC, this.nextSpawnData);
        pTag.store("SpawnPotentials", SpawnData.LIST_CODEC, this.spawnPotentials);
        return pTag;
    }

    @Nullable
    public Entity getOrCreateDisplayEntity(Level pLevel, BlockPos pPos) {
        if (this.displayEntity == null) {
            CompoundTag compoundtag = this.getOrCreateNextSpawnData(pLevel, pLevel.getRandom(), pPos).getEntityToSpawn();
            if (compoundtag.getString("id").isEmpty()) {
                return null;
            }

            this.displayEntity = EntityType.loadEntityRecursive(compoundtag, pLevel, EntitySpawnReason.SPAWNER, Function.identity());
            if (compoundtag.size() == 1 && this.displayEntity instanceof Mob) {
            }
        }

        return this.displayEntity;
    }

    public boolean onEventTriggered(Level pLevel, int pId) {
        if (pId == 1) {
            if (pLevel.isClientSide) {
                this.spawnDelay = this.minSpawnDelay;
            }

            return true;
        } else {
            return false;
        }
    }

    protected void setNextSpawnData(@Nullable Level pLevel, BlockPos pPos, SpawnData pNextSpawnData) {
        this.nextSpawnData = pNextSpawnData;
    }

    private SpawnData getOrCreateNextSpawnData(@Nullable Level pLevel, RandomSource pRandom, BlockPos pPos) {
        if (this.nextSpawnData != null) {
            return this.nextSpawnData;
        } else {
            this.setNextSpawnData(pLevel, pPos, this.spawnPotentials.getRandom(pRandom).orElseGet(SpawnData::new));
            return this.nextSpawnData;
        }
    }

    public abstract void broadcastEvent(Level pLevel, BlockPos pPos, int pEventId);

    public double getSpin() {
        return this.spin;
    }

    public double getoSpin() {
        return this.oSpin;
    }

    @Nullable
    public Entity getSpawnerEntity() {
       return null;
    }

    @Nullable
    public net.minecraft.world.level.block.entity.BlockEntity getSpawnerBlockEntity() {
        return null;
    }
}
