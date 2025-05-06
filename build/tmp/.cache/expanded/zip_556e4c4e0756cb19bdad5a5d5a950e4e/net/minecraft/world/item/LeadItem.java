package net.minecraft.world.item;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;

public class LeadItem extends Item {
    public LeadItem(Item.Properties p_42828_) {
        super(p_42828_);
    }

    @Override
    public InteractionResult useOn(UseOnContext pContext) {
        Level level = pContext.getLevel();
        BlockPos blockpos = pContext.getClickedPos();
        BlockState blockstate = level.getBlockState(blockpos);
        if (blockstate.is(BlockTags.FENCES)) {
            Player player = pContext.getPlayer();
            if (!level.isClientSide && player != null) {
                return bindPlayerMobs(player, level, blockpos);
            }
        }

        return InteractionResult.PASS;
    }

    public static InteractionResult bindPlayerMobs(Player pPlayer, Level pLevel, BlockPos pPos) {
        LeashFenceKnotEntity leashfenceknotentity = null;
        List<Leashable> list = leashableInArea(pLevel, pPos, p_341570_ -> p_341570_.getLeashHolder() == pPlayer);

        for (Leashable leashable : list) {
            if (leashfenceknotentity == null) {
                leashfenceknotentity = LeashFenceKnotEntity.getOrCreateKnot(pLevel, pPos);
                leashfenceknotentity.playPlacementSound();
            }

            leashable.setLeashedTo(leashfenceknotentity, true);
        }

        if (!list.isEmpty()) {
            pLevel.gameEvent(GameEvent.BLOCK_ATTACH, pPos, GameEvent.Context.of(pPlayer));
            return InteractionResult.SUCCESS_SERVER;
        } else {
            return InteractionResult.PASS;
        }
    }

    public static List<Leashable> leashableInArea(Level pLevel, BlockPos pPos, Predicate<Leashable> pPredicate) {
        double d0 = 7.0;
        int i = pPos.getX();
        int j = pPos.getY();
        int k = pPos.getZ();
        AABB aabb = new AABB(i - 7.0, j - 7.0, k - 7.0, i + 7.0, j + 7.0, k + 7.0);
        return pLevel.getEntitiesOfClass(Entity.class, aabb, p_341568_ -> p_341568_ instanceof Leashable leashable && pPredicate.test(leashable))
            .stream()
            .map(Leashable.class::cast)
            .toList();
    }
}