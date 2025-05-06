package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;

public class SetBlockCommand {
    private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.setblock.failed"));

    public static void register(CommandDispatcher<CommandSourceStack> pDispatcher, CommandBuildContext pBuildContext) {
        Predicate<BlockInWorld> predicate = p_180517_ -> p_180517_.getLevel().isEmptyBlock(p_180517_.getPos());
        pDispatcher.register(
            Commands.literal("setblock")
                .requires(p_138606_ -> p_138606_.hasPermission(2))
                .then(
                    Commands.argument("pos", BlockPosArgument.blockPos())
                        .then(
                            Commands.argument("block", BlockStateArgument.block(pBuildContext))
                                .executes(
                                    p_390090_ -> setBlock(
                                        p_390090_.getSource(),
                                        BlockPosArgument.getLoadedBlockPos(p_390090_, "pos"),
                                        BlockStateArgument.getBlock(p_390090_, "block"),
                                        SetBlockCommand.Mode.REPLACE,
                                        null,
                                        false
                                    )
                                )
                                .then(
                                    Commands.literal("destroy")
                                        .executes(
                                            p_390093_ -> setBlock(
                                                p_390093_.getSource(),
                                                BlockPosArgument.getLoadedBlockPos(p_390093_, "pos"),
                                                BlockStateArgument.getBlock(p_390093_, "block"),
                                                SetBlockCommand.Mode.DESTROY,
                                                null,
                                                false
                                            )
                                        )
                                )
                                .then(
                                    Commands.literal("keep")
                                        .executes(
                                            p_390095_ -> setBlock(
                                                p_390095_.getSource(),
                                                BlockPosArgument.getLoadedBlockPos(p_390095_, "pos"),
                                                BlockStateArgument.getBlock(p_390095_, "block"),
                                                SetBlockCommand.Mode.REPLACE,
                                                predicate,
                                                false
                                            )
                                        )
                                )
                                .then(
                                    Commands.literal("replace")
                                        .executes(
                                            p_390092_ -> setBlock(
                                                p_390092_.getSource(),
                                                BlockPosArgument.getLoadedBlockPos(p_390092_, "pos"),
                                                BlockStateArgument.getBlock(p_390092_, "block"),
                                                SetBlockCommand.Mode.REPLACE,
                                                null,
                                                false
                                            )
                                        )
                                )
                                .then(
                                    Commands.literal("strict")
                                        .executes(
                                            p_390091_ -> setBlock(
                                                p_390091_.getSource(),
                                                BlockPosArgument.getLoadedBlockPos(p_390091_, "pos"),
                                                BlockStateArgument.getBlock(p_390091_, "block"),
                                                SetBlockCommand.Mode.REPLACE,
                                                null,
                                                true
                                            )
                                        )
                                )
                        )
                )
        );
    }

    private static int setBlock(
        CommandSourceStack pSource,
        BlockPos pPos,
        BlockInput pBlock,
        SetBlockCommand.Mode pMode,
        @Nullable Predicate<BlockInWorld> pFilter,
        boolean pStrict
    ) throws CommandSyntaxException {
        ServerLevel serverlevel = pSource.getLevel();
        if (serverlevel.isDebug()) {
            throw ERROR_FAILED.create();
        } else if (pFilter != null && !pFilter.test(new BlockInWorld(serverlevel, pPos, true))) {
            throw ERROR_FAILED.create();
        } else {
            boolean flag;
            if (pMode == SetBlockCommand.Mode.DESTROY) {
                serverlevel.destroyBlock(pPos, true);
                flag = !pBlock.getState().isAir() || !serverlevel.getBlockState(pPos).isAir();
            } else {
                flag = true;
            }

            if (flag && !pBlock.place(serverlevel, pPos, 2 | (pStrict ? 816 : 256))) {
                throw ERROR_FAILED.create();
            } else {
                if (!pStrict) {
                    serverlevel.updateNeighborsAt(pPos, pBlock.getState().getBlock());
                }

                pSource.sendSuccess(
                    () -> Component.translatable("commands.setblock.success", pPos.getX(), pPos.getY(), pPos.getZ()), true
                );
                return 1;
            }
        }
    }

    public static enum Mode {
        REPLACE,
        DESTROY;
    }
}