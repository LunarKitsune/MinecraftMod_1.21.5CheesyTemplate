package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.commands.arguments.blocks.BlockPredicateArgument;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public class FillCommand {
    private static final Dynamic2CommandExceptionType ERROR_AREA_TOO_LARGE = new Dynamic2CommandExceptionType(
        (p_308702_, p_308703_) -> Component.translatableEscape("commands.fill.toobig", p_308702_, p_308703_)
    );
    static final BlockInput HOLLOW_CORE = new BlockInput(Blocks.AIR.defaultBlockState(), Collections.emptySet(), null);
    private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.fill.failed"));

    public static void register(CommandDispatcher<CommandSourceStack> pDispatcher, CommandBuildContext pBuildContext) {
        pDispatcher.register(
            Commands.literal("fill")
                .requires(p_137384_ -> p_137384_.hasPermission(2))
                .then(
                    Commands.argument("from", BlockPosArgument.blockPos())
                        .then(
                            Commands.argument("to", BlockPosArgument.blockPos())
                                .then(
                                    wrapWithMode(
                                            pBuildContext,
                                            Commands.argument("block", BlockStateArgument.block(pBuildContext)),
                                            p_390046_ -> BlockPosArgument.getLoadedBlockPos(p_390046_, "from"),
                                            p_390024_ -> BlockPosArgument.getLoadedBlockPos(p_390024_, "to"),
                                            p_390018_ -> BlockStateArgument.getBlock(p_390018_, "block"),
                                            p_390033_ -> null
                                        )
                                        .then(
                                            Commands.literal("replace")
                                                .executes(
                                                    p_390025_ -> fillBlocks(
                                                        p_390025_.getSource(),
                                                        BoundingBox.fromCorners(
                                                            BlockPosArgument.getLoadedBlockPos(p_390025_, "from"), BlockPosArgument.getLoadedBlockPos(p_390025_, "to")
                                                        ),
                                                        BlockStateArgument.getBlock(p_390025_, "block"),
                                                        FillCommand.Mode.REPLACE,
                                                        null,
                                                        false
                                                    )
                                                )
                                                .then(
                                                    wrapWithMode(
                                                        pBuildContext,
                                                        Commands.argument("filter", BlockPredicateArgument.blockPredicate(pBuildContext)),
                                                        p_390027_ -> BlockPosArgument.getLoadedBlockPos(p_390027_, "from"),
                                                        p_390040_ -> BlockPosArgument.getLoadedBlockPos(p_390040_, "to"),
                                                        p_390047_ -> BlockStateArgument.getBlock(p_390047_, "block"),
                                                        p_390034_ -> BlockPredicateArgument.getBlockPredicate(p_390034_, "filter")
                                                    )
                                                )
                                        )
                                        .then(
                                            Commands.literal("keep")
                                                .executes(
                                                    p_390026_ -> fillBlocks(
                                                        p_390026_.getSource(),
                                                        BoundingBox.fromCorners(
                                                            BlockPosArgument.getLoadedBlockPos(p_390026_, "from"), BlockPosArgument.getLoadedBlockPos(p_390026_, "to")
                                                        ),
                                                        BlockStateArgument.getBlock(p_390026_, "block"),
                                                        FillCommand.Mode.REPLACE,
                                                        p_180225_ -> p_180225_.getLevel().isEmptyBlock(p_180225_.getPos()),
                                                        false
                                                    )
                                                )
                                        )
                                )
                        )
                )
        );
    }

    private static ArgumentBuilder<CommandSourceStack, ?> wrapWithMode(
        CommandBuildContext pBuildContext,
        ArgumentBuilder<CommandSourceStack, ?> pArgumentBuilder,
        InCommandFunction<CommandContext<CommandSourceStack>, BlockPos> pFrom,
        InCommandFunction<CommandContext<CommandSourceStack>, BlockPos> pTo,
        InCommandFunction<CommandContext<CommandSourceStack>, BlockInput> pBlock,
        FillCommand.NullableCommandFunction<CommandContext<CommandSourceStack>, Predicate<BlockInWorld>> pFilter
    ) {
        return pArgumentBuilder.executes(
                p_390039_ -> fillBlocks(
                    p_390039_.getSource(),
                    BoundingBox.fromCorners(pFrom.apply(p_390039_), pTo.apply(p_390039_)),
                    pBlock.apply(p_390039_),
                    FillCommand.Mode.REPLACE,
                    pFilter.apply(p_390039_),
                    false
                )
            )
            .then(
                Commands.literal("outline")
                    .executes(
                        p_390032_ -> fillBlocks(
                            p_390032_.getSource(),
                            BoundingBox.fromCorners(pFrom.apply(p_390032_), pTo.apply(p_390032_)),
                            pBlock.apply(p_390032_),
                            FillCommand.Mode.OUTLINE,
                            pFilter.apply(p_390032_),
                            false
                        )
                    )
            )
            .then(
                Commands.literal("hollow")
                    .executes(
                        p_390023_ -> fillBlocks(
                            p_390023_.getSource(),
                            BoundingBox.fromCorners(pFrom.apply(p_390023_), pTo.apply(p_390023_)),
                            pBlock.apply(p_390023_),
                            FillCommand.Mode.HOLLOW,
                            pFilter.apply(p_390023_),
                            false
                        )
                    )
            )
            .then(
                Commands.literal("destroy")
                    .executes(
                        p_390045_ -> fillBlocks(
                            p_390045_.getSource(),
                            BoundingBox.fromCorners(pFrom.apply(p_390045_), pTo.apply(p_390045_)),
                            pBlock.apply(p_390045_),
                            FillCommand.Mode.DESTROY,
                            pFilter.apply(p_390045_),
                            false
                        )
                    )
            )
            .then(
                Commands.literal("strict")
                    .executes(
                        p_390017_ -> fillBlocks(
                            p_390017_.getSource(),
                            BoundingBox.fromCorners(pFrom.apply(p_390017_), pTo.apply(p_390017_)),
                            pBlock.apply(p_390017_),
                            FillCommand.Mode.REPLACE,
                            pFilter.apply(p_390017_),
                            true
                        )
                    )
            );
    }

    private static int fillBlocks(
        CommandSourceStack pSource,
        BoundingBox pBox,
        BlockInput pBlock,
        FillCommand.Mode pMode,
        @Nullable Predicate<BlockInWorld> pFilter,
        boolean pStrict
    ) throws CommandSyntaxException {
        int i = pBox.getXSpan() * pBox.getYSpan() * pBox.getZSpan();
        int j = pSource.getLevel().getGameRules().getInt(GameRules.RULE_COMMAND_MODIFICATION_BLOCK_LIMIT);
        if (i > j) {
            throw ERROR_AREA_TOO_LARGE.create(j, i);
        } else {
            List<BlockPos> list = Lists.newArrayList();
            ServerLevel serverlevel = pSource.getLevel();
            if (serverlevel.isDebug()) {
                throw ERROR_FAILED.create();
            } else {
                int k = 0;

                for (BlockPos blockpos : BlockPos.betweenClosed(
                    pBox.minX(), pBox.minY(), pBox.minZ(), pBox.maxX(), pBox.maxY(), pBox.maxZ()
                )) {
                    if (pFilter == null || pFilter.test(new BlockInWorld(serverlevel, blockpos, true))) {
                        boolean flag = false;
                        if (pMode.affector.affect(serverlevel, blockpos)) {
                            flag = true;
                        }

                        BlockInput blockinput = pMode.filter.filter(pBox, blockpos, pBlock, serverlevel);
                        if (blockinput == null) {
                            if (flag) {
                                k++;
                            }
                        } else if (!blockinput.place(serverlevel, blockpos, 2 | (pStrict ? 816 : 256))) {
                            if (flag) {
                                k++;
                            }
                        } else {
                            if (!pStrict) {
                                list.add(blockpos.immutable());
                            }

                            k++;
                        }
                    }
                }

                for (BlockPos blockpos1 : list) {
                    Block block = serverlevel.getBlockState(blockpos1).getBlock();
                    serverlevel.updateNeighborsAt(blockpos1, block);
                }

                if (k == 0) {
                    throw ERROR_FAILED.create();
                } else {
                    int l = k;
                    pSource.sendSuccess(() -> Component.translatable("commands.fill.success", l), true);
                    return k;
                }
            }
        }
    }

    @FunctionalInterface
    public interface Affector {
        FillCommand.Affector NOOP = (p_397097_, p_396648_) -> false;

        boolean affect(ServerLevel pLevel, BlockPos pPos);
    }

    @FunctionalInterface
    public interface Filter {
        FillCommand.Filter NOOP = (p_393263_, p_397927_, p_393586_, p_393914_) -> p_393586_;

        @Nullable
        BlockInput filter(BoundingBox pBox, BlockPos pPos, BlockInput pBlock, ServerLevel pLevel);
    }

    static enum Mode {
        REPLACE(FillCommand.Affector.NOOP, FillCommand.Filter.NOOP),
        OUTLINE(
            FillCommand.Affector.NOOP,
            (p_137428_, p_137429_, p_137430_, p_137431_) -> p_137429_.getX() != p_137428_.minX()
                    && p_137429_.getX() != p_137428_.maxX()
                    && p_137429_.getY() != p_137428_.minY()
                    && p_137429_.getY() != p_137428_.maxY()
                    && p_137429_.getZ() != p_137428_.minZ()
                    && p_137429_.getZ() != p_137428_.maxZ()
                ? null
                : p_137430_
        ),
        HOLLOW(
            FillCommand.Affector.NOOP,
            (p_137423_, p_137424_, p_137425_, p_137426_) -> p_137424_.getX() != p_137423_.minX()
                    && p_137424_.getX() != p_137423_.maxX()
                    && p_137424_.getY() != p_137423_.minY()
                    && p_137424_.getY() != p_137423_.maxY()
                    && p_137424_.getZ() != p_137423_.minZ()
                    && p_137424_.getZ() != p_137423_.maxZ()
                ? FillCommand.HOLLOW_CORE
                : p_137425_
        ),
        DESTROY((p_390048_, p_390049_) -> p_390048_.destroyBlock(p_390049_, true), FillCommand.Filter.NOOP);

        public final FillCommand.Filter filter;
        public final FillCommand.Affector affector;

        private Mode(final FillCommand.Affector pAffector, final FillCommand.Filter pFilter) {
            this.affector = pAffector;
            this.filter = pFilter;
        }
    }

    @FunctionalInterface
    interface NullableCommandFunction<T, R> {
        @Nullable
        R apply(T pContext) throws CommandSyntaxException;
    }
}