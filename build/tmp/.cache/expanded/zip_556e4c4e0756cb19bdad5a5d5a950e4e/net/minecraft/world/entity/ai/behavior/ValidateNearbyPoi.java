package net.minecraft.world.entity.ai.behavior;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.declarative.MemoryAccessor;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class ValidateNearbyPoi {
    private static final int MAX_DISTANCE = 16;

    public static BehaviorControl<LivingEntity> create(Predicate<Holder<PoiType>> pPoiValidator, MemoryModuleType<GlobalPos> pPoiPosMemory) {
        return BehaviorBuilder.create(
            p_259215_ -> p_259215_.group(p_259215_.present(pPoiPosMemory)).apply(p_259215_, p_259498_ -> (p_375053_, p_375054_, p_375055_) -> {
                GlobalPos globalpos = p_259215_.get(p_259498_);
                BlockPos blockpos = globalpos.pos();
                if (p_375053_.dimension() == globalpos.dimension() && blockpos.closerToCenterThan(p_375054_.position(), 16.0)) {
                    ServerLevel serverlevel = p_375053_.getServer().getLevel(globalpos.dimension());
                    if (serverlevel == null || !serverlevel.getPoiManager().exists(blockpos, pPoiValidator)) {
                        p_259498_.erase();
                    } else if (bedIsOccupied(serverlevel, blockpos, p_375054_)) {
                        p_259498_.erase();
                        if (!bedIsOccupiedByVillager(serverlevel, blockpos)) {
                            p_375053_.getPoiManager().release(blockpos);
                            DebugPackets.sendPoiTicketCountPacket(p_375053_, blockpos);
                        }
                    }

                    return true;
                } else {
                    return false;
                }
            })
        );
    }

    private static boolean bedIsOccupied(ServerLevel pLevel, BlockPos pPos, LivingEntity pEntity) {
        BlockState blockstate = pLevel.getBlockState(pPos);
        return blockstate.is(BlockTags.BEDS) && blockstate.getValue(BedBlock.OCCUPIED) && !pEntity.isSleeping();
    }

    private static boolean bedIsOccupiedByVillager(ServerLevel pLevel, BlockPos pPos) {
        List<Villager> list = pLevel.getEntitiesOfClass(Villager.class, new AABB(pPos), LivingEntity::isSleeping);
        return !list.isEmpty();
    }
}