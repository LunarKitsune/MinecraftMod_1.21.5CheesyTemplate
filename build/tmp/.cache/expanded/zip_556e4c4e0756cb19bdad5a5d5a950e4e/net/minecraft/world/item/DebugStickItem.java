package net.minecraft.world.item;

import java.util.Collection;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.DebugStickState;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;

public class DebugStickItem extends Item {
    public DebugStickItem(Item.Properties p_40948_) {
        super(p_40948_);
    }

    @Override
    public boolean canDestroyBlock(ItemStack p_391172_, BlockState p_391714_, Level p_391767_, BlockPos p_397694_, LivingEntity p_395284_) {
        if (!p_391767_.isClientSide && p_395284_ instanceof Player player) {
            this.handleInteraction(player, p_391714_, p_391767_, p_397694_, false, p_391172_);
        }

        return false;
    }

    @Override
    public InteractionResult useOn(UseOnContext pContext) {
        Player player = pContext.getPlayer();
        Level level = pContext.getLevel();
        if (!level.isClientSide && player != null) {
            BlockPos blockpos = pContext.getClickedPos();
            if (!this.handleInteraction(player, level.getBlockState(blockpos), level, blockpos, true, pContext.getItemInHand())) {
                return InteractionResult.FAIL;
            }
        }

        return InteractionResult.SUCCESS;
    }

    private boolean handleInteraction(Player pPlayer, BlockState pStateClicked, LevelAccessor pAccessor, BlockPos pPos, boolean pShouldCycleState, ItemStack pDebugStack) {
        if (!pPlayer.canUseGameMasterBlocks()) {
            return false;
        } else {
            Holder<Block> holder = pStateClicked.getBlockHolder();
            StateDefinition<Block, BlockState> statedefinition = holder.value().getStateDefinition();
            Collection<Property<?>> collection = statedefinition.getProperties();
            if (collection.isEmpty()) {
                message(pPlayer, Component.translatable(this.descriptionId + ".empty", holder.getRegisteredName()));
                return false;
            } else {
                DebugStickState debugstickstate = pDebugStack.get(DataComponents.DEBUG_STICK_STATE);
                if (debugstickstate == null) {
                    return false;
                } else {
                    Property<?> property = debugstickstate.properties().get(holder);
                    if (pShouldCycleState) {
                        if (property == null) {
                            property = collection.iterator().next();
                        }

                        BlockState blockstate = cycleState(pStateClicked, property, pPlayer.isSecondaryUseActive());
                        pAccessor.setBlock(pPos, blockstate, 18);
                        message(pPlayer, Component.translatable(this.descriptionId + ".update", property.getName(), getNameHelper(blockstate, property)));
                    } else {
                        property = getRelative(collection, property, pPlayer.isSecondaryUseActive());
                        pDebugStack.set(DataComponents.DEBUG_STICK_STATE, debugstickstate.withProperty(holder, property));
                        message(pPlayer, Component.translatable(this.descriptionId + ".select", property.getName(), getNameHelper(pStateClicked, property)));
                    }

                    return true;
                }
            }
        }
    }

    private static <T extends Comparable<T>> BlockState cycleState(BlockState pState, Property<T> pProperty, boolean pBackwards) {
        return pState.setValue(pProperty, getRelative(pProperty.getPossibleValues(), pState.getValue(pProperty), pBackwards));
    }

    private static <T> T getRelative(Iterable<T> pAllowedValues, @Nullable T pCurrentValue, boolean pBackwards) {
        return pBackwards ? Util.findPreviousInIterable(pAllowedValues, pCurrentValue) : Util.findNextInIterable(pAllowedValues, pCurrentValue);
    }

    private static void message(Player pPlayer, Component pMessageComponent) {
        ((ServerPlayer)pPlayer).sendSystemMessage(pMessageComponent, true);
    }

    private static <T extends Comparable<T>> String getNameHelper(BlockState pState, Property<T> pProperty) {
        return pProperty.getName(pState.getValue(pProperty));
    }
}