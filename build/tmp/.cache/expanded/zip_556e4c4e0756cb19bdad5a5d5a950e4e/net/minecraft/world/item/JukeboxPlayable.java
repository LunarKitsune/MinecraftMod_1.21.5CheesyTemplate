package net.minecraft.world.item;

import com.mojang.serialization.Codec;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.TooltipProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.entity.JukeboxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

public record JukeboxPlayable(EitherHolder<JukeboxSong> song) implements TooltipProvider {
    public static final Codec<JukeboxPlayable> CODEC = EitherHolder.codec(Registries.JUKEBOX_SONG, JukeboxSong.CODEC)
        .xmap(JukeboxPlayable::new, JukeboxPlayable::song);
    public static final StreamCodec<RegistryFriendlyByteBuf, JukeboxPlayable> STREAM_CODEC = StreamCodec.composite(
        EitherHolder.streamCodec(Registries.JUKEBOX_SONG, JukeboxSong.STREAM_CODEC), JukeboxPlayable::song, JukeboxPlayable::new
    );

    @Override
    public void addToTooltip(Item.TooltipContext p_343529_, Consumer<Component> p_344027_, TooltipFlag p_344530_, DataComponentGetter p_392270_) {
        HolderLookup.Provider holderlookup$provider = p_343529_.registries();
        if (holderlookup$provider != null) {
            this.song.unwrap(holderlookup$provider).ifPresent(p_343443_ -> {
                MutableComponent mutablecomponent = p_343443_.value().description().copy();
                ComponentUtils.mergeStyles(mutablecomponent, Style.EMPTY.withColor(ChatFormatting.GRAY));
                p_344027_.accept(mutablecomponent);
            });
        }
    }

    public static InteractionResult tryInsertIntoJukebox(Level pLevel, BlockPos pPos, ItemStack pStack, Player pPlayer) {
        JukeboxPlayable jukeboxplayable = pStack.get(DataComponents.JUKEBOX_PLAYABLE);
        if (jukeboxplayable == null) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        } else {
            BlockState blockstate = pLevel.getBlockState(pPos);
            if (blockstate.is(Blocks.JUKEBOX) && !blockstate.getValue(JukeboxBlock.HAS_RECORD)) {
                if (!pLevel.isClientSide) {
                    ItemStack itemstack = pStack.consumeAndReturn(1, pPlayer);
                    if (pLevel.getBlockEntity(pPos) instanceof JukeboxBlockEntity jukeboxblockentity) {
                        jukeboxblockentity.setTheItem(itemstack);
                        pLevel.gameEvent(GameEvent.BLOCK_CHANGE, pPos, GameEvent.Context.of(pPlayer, blockstate));
                    }

                    pPlayer.awardStat(Stats.PLAY_RECORD);
                }

                return InteractionResult.SUCCESS;
            } else {
                return InteractionResult.TRY_WITH_EMPTY_HAND;
            }
        }
    }
}