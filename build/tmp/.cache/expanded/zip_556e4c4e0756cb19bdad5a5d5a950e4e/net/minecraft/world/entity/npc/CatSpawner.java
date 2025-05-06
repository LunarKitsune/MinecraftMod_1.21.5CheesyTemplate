package net.minecraft.world.entity.npc;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.AABB;

public class CatSpawner implements CustomSpawner {
    private static final int TICK_DELAY = 1200;
    private int nextTick;

    @Override
    public void tick(ServerLevel p_35330_, boolean p_35331_, boolean p_35332_) {
        if (p_35332_ && p_35330_.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)) {
            this.nextTick--;
            if (this.nextTick <= 0) {
                this.nextTick = 1200;
                Player player = p_35330_.getRandomPlayer();
                if (player != null) {
                    RandomSource randomsource = p_35330_.random;
                    int i = (8 + randomsource.nextInt(24)) * (randomsource.nextBoolean() ? -1 : 1);
                    int j = (8 + randomsource.nextInt(24)) * (randomsource.nextBoolean() ? -1 : 1);
                    BlockPos blockpos = player.blockPosition().offset(i, 0, j);
                    int k = 10;
                    if (p_35330_.hasChunksAt(blockpos.getX() - 10, blockpos.getZ() - 10, blockpos.getX() + 10, blockpos.getZ() + 10)) {
                        if (SpawnPlacements.isSpawnPositionOk(EntityType.CAT, p_35330_, blockpos)) {
                            if (p_35330_.isCloseToVillage(blockpos, 2)) {
                                this.spawnInVillage(p_35330_, blockpos);
                            } else if (p_35330_.structureManager().getStructureWithPieceAt(blockpos, StructureTags.CATS_SPAWN_IN).isValid()) {
                                this.spawnInHut(p_35330_, blockpos);
                            }
                        }
                    }
                }
            }
        }
    }

    private void spawnInVillage(ServerLevel pLevel, BlockPos pPos) {
        int i = 48;
        if (pLevel.getPoiManager().getCountInRange(p_219610_ -> p_219610_.is(PoiTypes.HOME), pPos, 48, PoiManager.Occupancy.IS_OCCUPIED) > 4L) {
            List<Cat> list = pLevel.getEntitiesOfClass(Cat.class, new AABB(pPos).inflate(48.0, 8.0, 48.0));
            if (list.size() < 5) {
                this.spawnCat(pPos, pLevel, false);
            }
        }
    }

    private void spawnInHut(ServerLevel pLevel, BlockPos pPos) {
        int i = 16;
        List<Cat> list = pLevel.getEntitiesOfClass(Cat.class, new AABB(pPos).inflate(16.0, 8.0, 16.0));
        if (list.isEmpty()) {
            this.spawnCat(pPos, pLevel, true);
        }
    }

    private void spawnCat(BlockPos pPos, ServerLevel pLevel, boolean pPersistent) {
        Cat cat = EntityType.CAT.create(pLevel, EntitySpawnReason.NATURAL);
        if (cat != null) {
            cat.snapTo(pPos, 0.0F, 0.0F); // Fix MC-147659: Some witch huts spawn the incorrect cat
            cat.finalizeSpawn(pLevel, pLevel.getCurrentDifficultyAt(pPos), EntitySpawnReason.NATURAL, null);
            if (pPersistent) {
                cat.setPersistenceRequired();
            }

            pLevel.addFreshEntityWithPassengers(cat);
        }
    }
}
