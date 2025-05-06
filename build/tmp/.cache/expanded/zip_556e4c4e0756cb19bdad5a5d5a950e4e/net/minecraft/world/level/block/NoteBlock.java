package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;

public class NoteBlock extends Block {
    public static final MapCodec<NoteBlock> CODEC = simpleCodec(NoteBlock::new);
    public static final EnumProperty<NoteBlockInstrument> INSTRUMENT = BlockStateProperties.NOTEBLOCK_INSTRUMENT;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final IntegerProperty NOTE = BlockStateProperties.NOTE;
    public static final int NOTE_VOLUME = 3;

    @Override
    public MapCodec<NoteBlock> codec() {
        return CODEC;
    }

    public NoteBlock(BlockBehaviour.Properties p_55016_) {
        super(p_55016_);
        this.registerDefaultState(this.stateDefinition.any().setValue(INSTRUMENT, NoteBlockInstrument.HARP).setValue(NOTE, 0).setValue(POWERED, false));
    }

    private BlockState setInstrument(LevelReader pLevel, BlockPos pPos, BlockState pState) {
        NoteBlockInstrument noteblockinstrument = pLevel.getBlockState(pPos.above()).instrument();
        if (noteblockinstrument.worksAboveNoteBlock()) {
            return pState.setValue(INSTRUMENT, noteblockinstrument);
        } else {
            NoteBlockInstrument noteblockinstrument1 = pLevel.getBlockState(pPos.below()).instrument();
            NoteBlockInstrument noteblockinstrument2 = noteblockinstrument1.worksAboveNoteBlock() ? NoteBlockInstrument.HARP : noteblockinstrument1;
            return pState.setValue(INSTRUMENT, noteblockinstrument2);
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        return this.setInstrument(pContext.getLevel(), pContext.getClickedPos(), this.defaultBlockState());
    }

    @Override
    protected BlockState updateShape(
        BlockState p_55048_,
        LevelReader p_368598_,
        ScheduledTickAccess p_363136_,
        BlockPos p_55052_,
        Direction p_55049_,
        BlockPos p_55053_,
        BlockState p_55050_,
        RandomSource p_367019_
    ) {
        boolean flag = p_55049_.getAxis() == Direction.Axis.Y;
        return flag
            ? this.setInstrument(p_368598_, p_55052_, p_55048_)
            : super.updateShape(p_55048_, p_368598_, p_363136_, p_55052_, p_55049_, p_55053_, p_55050_, p_367019_);
    }

    @Override
    protected void neighborChanged(BlockState p_55041_, Level p_55042_, BlockPos p_55043_, Block p_55044_, @Nullable Orientation p_369340_, boolean p_55046_) {
        boolean flag = p_55042_.hasNeighborSignal(p_55043_);
        if (flag != p_55041_.getValue(POWERED)) {
            if (flag) {
                this.playNote(null, p_55041_, p_55042_, p_55043_);
            }

            p_55042_.setBlock(p_55043_, p_55041_.setValue(POWERED, flag), 3);
        }
    }

    private void playNote(@Nullable Entity pEntity, BlockState pState, Level pLevel, BlockPos pPos) {
        if (pState.getValue(INSTRUMENT).worksAboveNoteBlock() || pLevel.getBlockState(pPos.above()).isAir()) {
            pLevel.blockEvent(pPos, this, 0, 0);
            pLevel.gameEvent(pEntity, GameEvent.NOTE_BLOCK_PLAY, pPos);
        }
    }

    @Override
    protected InteractionResult useItemOn(
        ItemStack p_330444_, BlockState p_329477_, Level p_331069_, BlockPos p_335878_, Player p_329474_, InteractionHand p_328196_, BlockHitResult p_334403_
    ) {
        return (InteractionResult)(p_330444_.is(ItemTags.NOTE_BLOCK_TOP_INSTRUMENTS) && p_334403_.getDirection() == Direction.UP
            ? InteractionResult.PASS
            : super.useItemOn(p_330444_, p_329477_, p_331069_, p_335878_, p_329474_, p_328196_, p_334403_));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState p_331116_, Level p_332131_, BlockPos p_333586_, Player p_329332_, BlockHitResult p_331978_) {
        if (!p_332131_.isClientSide) {
            int _new = net.minecraftforge.common.ForgeHooks.onNoteChange(p_332131_, p_333586_, p_331116_, p_331116_.getValue(NOTE), p_331116_.cycle(NOTE).getValue(NOTE));
            if (_new == -1) return InteractionResult.FAIL;
            p_331116_ = p_331116_.setValue(NOTE, _new);
            p_332131_.setBlock(p_333586_, p_331116_, 3);
            this.playNote(p_329332_, p_331116_, p_332131_, p_333586_);
            p_329332_.awardStat(Stats.TUNE_NOTEBLOCK);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    protected void attack(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer) {
        if (!pLevel.isClientSide) {
            this.playNote(pPlayer, pState, pLevel, pPos);
            pPlayer.awardStat(Stats.PLAY_NOTEBLOCK);
        }
    }

    public static float getPitchFromNote(int pNote) {
        return (float)Math.pow(2.0, (pNote - 12) / 12.0);
    }

    @Override
    protected boolean triggerEvent(BlockState pState, Level pLevel, BlockPos pPos, int pId, int pParam) {
        var event = net.minecraftforge.event.ForgeEventFactory.onNotePlay(pLevel, pPos, pState, pState.getValue(NOTE), pState.getValue(INSTRUMENT));
        if (event.isCanceled()) return false;
        pState = pState.setValue(NOTE, event.getVanillaNoteId()).setValue(INSTRUMENT, event.getInstrument());
        NoteBlockInstrument noteblockinstrument = pState.getValue(INSTRUMENT);
        float f;
        if (noteblockinstrument.isTunable()) {
            int i = pState.getValue(NOTE);
            f = getPitchFromNote(i);
            pLevel.addParticle(ParticleTypes.NOTE, pPos.getX() + 0.5, pPos.getY() + 1.2, pPos.getZ() + 0.5, i / 24.0, 0.0, 0.0);
        } else {
            f = 1.0F;
        }

        Holder<SoundEvent> holder;
        if (noteblockinstrument.hasCustomSound()) {
            ResourceLocation resourcelocation = this.getCustomSoundId(pLevel, pPos);
            if (resourcelocation == null) {
                return false;
            }

            holder = Holder.direct(SoundEvent.createVariableRangeEvent(resourcelocation));
        } else {
            holder = noteblockinstrument.getSoundEvent();
        }

        pLevel.playSeededSound(
            null,
            pPos.getX() + 0.5,
            pPos.getY() + 0.5,
            pPos.getZ() + 0.5,
            holder,
            SoundSource.RECORDS,
            3.0F,
            f,
            pLevel.random.nextLong()
        );
        return true;
    }

    @Nullable
    private ResourceLocation getCustomSoundId(Level pLevel, BlockPos pPos) {
        return pLevel.getBlockEntity(pPos.above()) instanceof SkullBlockEntity skullblockentity ? skullblockentity.getNoteBlockSound() : null;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(INSTRUMENT, POWERED, NOTE);
    }
}
