package net.minecraft.world.level.block;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringUtil;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BaseCommandBlock;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.slf4j.Logger;

public class CommandBlock extends BaseEntityBlock implements GameMasterBlock {
    public static final MapCodec<CommandBlock> CODEC = RecordCodecBuilder.mapCodec(
        p_360421_ -> p_360421_.group(Codec.BOOL.fieldOf("automatic").forGetter(p_311238_ -> p_311238_.automatic), propertiesCodec())
            .apply(p_360421_, CommandBlock::new)
    );
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final EnumProperty<Direction> FACING = DirectionalBlock.FACING;
    public static final BooleanProperty CONDITIONAL = BlockStateProperties.CONDITIONAL;
    private final boolean automatic;

    @Override
    public MapCodec<CommandBlock> codec() {
        return CODEC;
    }

    public CommandBlock(boolean pAutomatic, BlockBehaviour.Properties pProperties) {
        super(pProperties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(CONDITIONAL, false));
        this.automatic = pAutomatic;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos p_153083_, BlockState p_153084_) {
        CommandBlockEntity commandblockentity = new CommandBlockEntity(p_153083_, p_153084_);
        commandblockentity.setAutomatic(this.automatic);
        return commandblockentity;
    }

    @Override
    protected void neighborChanged(BlockState p_51838_, Level p_51839_, BlockPos p_51840_, Block p_51841_, @Nullable Orientation p_361911_, boolean p_51843_) {
        if (!p_51839_.isClientSide) {
            if (p_51839_.getBlockEntity(p_51840_) instanceof CommandBlockEntity commandblockentity) {
                this.setPoweredAndUpdate(p_51839_, p_51840_, commandblockentity, p_51839_.hasNeighborSignal(p_51840_));
            }
        }
    }

    private void setPoweredAndUpdate(Level pLevel, BlockPos pPos, CommandBlockEntity pBlockEntity, boolean pPowered) {
        boolean flag = pBlockEntity.isPowered();
        if (pPowered != flag) {
            pBlockEntity.setPowered(pPowered);
            if (pPowered) {
                if (pBlockEntity.isAutomatic() || pBlockEntity.getMode() == CommandBlockEntity.Mode.SEQUENCE) {
                    return;
                }

                pBlockEntity.markConditionMet();
                pLevel.scheduleTick(pPos, this, 1);
            }
        }
    }

    @Override
    protected void tick(BlockState p_221005_, ServerLevel p_221006_, BlockPos p_221007_, RandomSource p_221008_) {
        if (p_221006_.getBlockEntity(p_221007_) instanceof CommandBlockEntity commandblockentity) {
            BaseCommandBlock basecommandblock = commandblockentity.getCommandBlock();
            boolean flag = !StringUtil.isNullOrEmpty(basecommandblock.getCommand());
            CommandBlockEntity.Mode commandblockentity$mode = commandblockentity.getMode();
            boolean flag1 = commandblockentity.wasConditionMet();
            if (commandblockentity$mode == CommandBlockEntity.Mode.AUTO) {
                commandblockentity.markConditionMet();
                if (flag1) {
                    this.execute(p_221005_, p_221006_, p_221007_, basecommandblock, flag);
                } else if (commandblockentity.isConditional()) {
                    basecommandblock.setSuccessCount(0);
                }

                if (commandblockentity.isPowered() || commandblockentity.isAutomatic()) {
                    p_221006_.scheduleTick(p_221007_, this, 1);
                }
            } else if (commandblockentity$mode == CommandBlockEntity.Mode.REDSTONE) {
                if (flag1) {
                    this.execute(p_221005_, p_221006_, p_221007_, basecommandblock, flag);
                } else if (commandblockentity.isConditional()) {
                    basecommandblock.setSuccessCount(0);
                }
            }

            p_221006_.updateNeighbourForOutputSignal(p_221007_, this);
        }
    }

    private void execute(BlockState pState, ServerLevel pLevel, BlockPos pPos, BaseCommandBlock pLogic, boolean pCanTrigger) {
        if (pCanTrigger) {
            pLogic.performCommand(pLevel);
        } else {
            pLogic.setSuccessCount(0);
        }

        executeChain(pLevel, pPos, pState.getValue(FACING));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState p_51825_, Level p_51826_, BlockPos p_51827_, Player p_51828_, BlockHitResult p_51830_) {
        BlockEntity blockentity = p_51826_.getBlockEntity(p_51827_);
        if (blockentity instanceof CommandBlockEntity && p_51828_.canUseGameMasterBlocks()) {
            p_51828_.openCommandBlock((CommandBlockEntity)blockentity);
            return InteractionResult.SUCCESS;
        } else {
            return InteractionResult.PASS;
        }
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState pState) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState pBlockState, Level pLevel, BlockPos pPos) {
        BlockEntity blockentity = pLevel.getBlockEntity(pPos);
        return blockentity instanceof CommandBlockEntity ? ((CommandBlockEntity)blockentity).getCommandBlock().getSuccessCount() : 0;
    }

    @Override
    public void setPlacedBy(Level pLevel, BlockPos pPos, BlockState pState, LivingEntity pPlacer, ItemStack pStack) {
        if (pLevel.getBlockEntity(pPos) instanceof CommandBlockEntity commandblockentity) {
            BaseCommandBlock $$8 = commandblockentity.getCommandBlock();
            if (pLevel instanceof ServerLevel serverlevel) {
                if (!pStack.has(DataComponents.BLOCK_ENTITY_DATA)) {
                    $$8.setTrackOutput(serverlevel.getGameRules().getBoolean(GameRules.RULE_SENDCOMMANDFEEDBACK));
                    commandblockentity.setAutomatic(this.automatic);
                }

                boolean flag = pLevel.hasNeighborSignal(pPos);
                this.setPoweredAndUpdate(pLevel, pPos, commandblockentity, flag);
            }
        }
    }

    @Override
    protected BlockState rotate(BlockState pState, Rotation pRotation) {
        return pState.setValue(FACING, pRotation.rotate(pState.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState pState, Mirror pMirror) {
        return pState.rotate(pMirror.getRotation(pState.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        pBuilder.add(FACING, CONDITIONAL);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext pContext) {
        return this.defaultBlockState().setValue(FACING, pContext.getNearestLookingDirection().getOpposite());
    }

    private static void executeChain(ServerLevel pLevel, BlockPos pPos, Direction pDirection) {
        BlockPos.MutableBlockPos blockpos$mutableblockpos = pPos.mutable();
        GameRules gamerules = pLevel.getGameRules();
        int i = gamerules.getInt(GameRules.RULE_MAX_COMMAND_CHAIN_LENGTH);

        while (i-- > 0) {
            blockpos$mutableblockpos.move(pDirection);
            BlockState blockstate = pLevel.getBlockState(blockpos$mutableblockpos);
            Block block = blockstate.getBlock();
            if (!blockstate.is(Blocks.CHAIN_COMMAND_BLOCK)
                || !(pLevel.getBlockEntity(blockpos$mutableblockpos) instanceof CommandBlockEntity commandblockentity)
                || commandblockentity.getMode() != CommandBlockEntity.Mode.SEQUENCE) {
                break;
            }

            if (commandblockentity.isPowered() || commandblockentity.isAutomatic()) {
                BaseCommandBlock basecommandblock = commandblockentity.getCommandBlock();
                if (commandblockentity.markConditionMet()) {
                    if (!basecommandblock.performCommand(pLevel)) {
                        break;
                    }

                    pLevel.updateNeighbourForOutputSignal(blockpos$mutableblockpos, block);
                } else if (commandblockentity.isConditional()) {
                    basecommandblock.setSuccessCount(0);
                }
            }

            pDirection = blockstate.getValue(FACING);
        }

        if (i <= 0) {
            int j = Math.max(gamerules.getInt(GameRules.RULE_MAX_COMMAND_CHAIN_LENGTH), 0);
            LOGGER.warn("Command Block chain tried to execute more than {} steps!", j);
        }
    }
}