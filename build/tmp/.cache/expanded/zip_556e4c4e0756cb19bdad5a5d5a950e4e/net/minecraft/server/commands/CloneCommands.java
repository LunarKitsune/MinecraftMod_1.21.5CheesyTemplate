package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Deque;
import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.blocks.BlockPredicateArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public class CloneCommands {
    private static final SimpleCommandExceptionType ERROR_OVERLAP = new SimpleCommandExceptionType(Component.translatable("commands.clone.overlap"));
    private static final Dynamic2CommandExceptionType ERROR_AREA_TOO_LARGE = new Dynamic2CommandExceptionType(
        (p_308640_, p_308641_) -> Component.translatableEscape("commands.clone.toobig", p_308640_, p_308641_)
    );
    private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.clone.failed"));
    public static final Predicate<BlockInWorld> FILTER_AIR = p_358579_ -> !p_358579_.getState().isAir();

    public static void register(CommandDispatcher<CommandSourceStack> pDispatcher, CommandBuildContext pContext) {
        pDispatcher.register(
            Commands.literal("clone")
                .requires(p_136734_ -> p_136734_.hasPermission(2))
                .then(beginEndDestinationAndModeSuffix(pContext, p_264757_ -> p_264757_.getSource().getLevel()))
                .then(
                    Commands.literal("from")
                        .then(
                            Commands.argument("sourceDimension", DimensionArgument.dimension())
                                .then(beginEndDestinationAndModeSuffix(pContext, p_264743_ -> DimensionArgument.getDimension(p_264743_, "sourceDimension")))
                        )
                )
        );
    }

    private static ArgumentBuilder<CommandSourceStack, ?> beginEndDestinationAndModeSuffix(
        CommandBuildContext pBuildContext, InCommandFunction<CommandContext<CommandSourceStack>, ServerLevel> pLevelGetter
    ) {
        return Commands.argument("begin", BlockPosArgument.blockPos())
            .then(
                Commands.argument("end", BlockPosArgument.blockPos())
                    .then(destinationAndStrictSuffix(pBuildContext, pLevelGetter, p_264751_ -> p_264751_.getSource().getLevel()))
                    .then(
                        Commands.literal("to")
                            .then(
                                Commands.argument("targetDimension", DimensionArgument.dimension())
                                    .then(destinationAndStrictSuffix(pBuildContext, pLevelGetter, p_264756_ -> DimensionArgument.getDimension(p_264756_, "targetDimension")))
                            )
                    )
            );
    }

    private static CloneCommands.DimensionAndPosition getLoadedDimensionAndPosition(CommandContext<CommandSourceStack> pContext, ServerLevel pLevel, String pName) throws CommandSyntaxException {
        BlockPos blockpos = BlockPosArgument.getLoadedBlockPos(pContext, pLevel, pName);
        return new CloneCommands.DimensionAndPosition(pLevel, blockpos);
    }

    private static ArgumentBuilder<CommandSourceStack, ?> destinationAndStrictSuffix(
        CommandBuildContext pBuildContext,
        InCommandFunction<CommandContext<CommandSourceStack>, ServerLevel> pSourceLevelGetter,
        InCommandFunction<CommandContext<CommandSourceStack>, ServerLevel> pDestinationLevelGetter
    ) {
        InCommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> incommandfunction = p_389989_ -> getLoadedDimensionAndPosition(
            p_389989_, pSourceLevelGetter.apply(p_389989_), "begin"
        );
        InCommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> incommandfunction1 = p_389970_ -> getLoadedDimensionAndPosition(
            p_389970_, pSourceLevelGetter.apply(p_389970_), "end"
        );
        InCommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> incommandfunction2 = p_389991_ -> getLoadedDimensionAndPosition(
            p_389991_, pDestinationLevelGetter.apply(p_389991_), "destination"
        );
        return modeSuffix(
                pBuildContext, incommandfunction, incommandfunction1, incommandfunction2, false, Commands.argument("destination", BlockPosArgument.blockPos())
            )
            .then(modeSuffix(pBuildContext, incommandfunction, incommandfunction1, incommandfunction2, true, Commands.literal("strict")));
    }

    private static ArgumentBuilder<CommandSourceStack, ?> modeSuffix(
        CommandBuildContext pBuildContext,
        InCommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> pBegin,
        InCommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> pEnd,
        InCommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> pDestination,
        boolean pStrict,
        ArgumentBuilder<CommandSourceStack, ?> pArgumentBuilder
    ) {
        return pArgumentBuilder.executes(
                p_389981_ -> clone(
                    p_389981_.getSource(),
                    pBegin.apply(p_389981_),
                    pEnd.apply(p_389981_),
                    pDestination.apply(p_389981_),
                    p_180033_ -> true,
                    CloneCommands.Mode.NORMAL,
                    pStrict
                )
            )
            .then(wrapWithCloneMode(pBegin, pEnd, pDestination, p_264738_ -> p_180041_ -> true, pStrict, Commands.literal("replace")))
            .then(wrapWithCloneMode(pBegin, pEnd, pDestination, p_264744_ -> FILTER_AIR, pStrict, Commands.literal("masked")))
            .then(
                Commands.literal("filtered")
                    .then(
                        wrapWithCloneMode(
                            pBegin,
                            pEnd,
                            pDestination,
                            p_264745_ -> BlockPredicateArgument.getBlockPredicate(p_264745_, "filter"),
                            pStrict,
                            Commands.argument("filter", BlockPredicateArgument.blockPredicate(pBuildContext))
                        )
                    )
            );
    }

    private static ArgumentBuilder<CommandSourceStack, ?> wrapWithCloneMode(
        InCommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> pBegin,
        InCommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> pEnd,
        InCommandFunction<CommandContext<CommandSourceStack>, CloneCommands.DimensionAndPosition> pDestination,
        InCommandFunction<CommandContext<CommandSourceStack>, Predicate<BlockInWorld>> pFilter,
        boolean pStrict,
        ArgumentBuilder<CommandSourceStack, ?> pArgumentBuilder
    ) {
        return pArgumentBuilder.executes(
                p_389997_ -> clone(
                    p_389997_.getSource(),
                    pBegin.apply(p_389997_),
                    pEnd.apply(p_389997_),
                    pDestination.apply(p_389997_),
                    pFilter.apply(p_389997_),
                    CloneCommands.Mode.NORMAL,
                    pStrict
                )
            )
            .then(
                Commands.literal("force")
                    .executes(
                        p_389976_ -> clone(
                            p_389976_.getSource(),
                            pBegin.apply(p_389976_),
                            pEnd.apply(p_389976_),
                            pDestination.apply(p_389976_),
                            pFilter.apply(p_389976_),
                            CloneCommands.Mode.FORCE,
                            pStrict
                        )
                    )
            )
            .then(
                Commands.literal("move")
                    .executes(
                        p_389987_ -> clone(
                            p_389987_.getSource(),
                            pBegin.apply(p_389987_),
                            pEnd.apply(p_389987_),
                            pDestination.apply(p_389987_),
                            pFilter.apply(p_389987_),
                            CloneCommands.Mode.MOVE,
                            pStrict
                        )
                    )
            )
            .then(
                Commands.literal("normal")
                    .executes(
                        p_389968_ -> clone(
                            p_389968_.getSource(),
                            pBegin.apply(p_389968_),
                            pEnd.apply(p_389968_),
                            pDestination.apply(p_389968_),
                            pFilter.apply(p_389968_),
                            CloneCommands.Mode.NORMAL,
                            pStrict
                        )
                    )
            );
    }

    private static int clone(
        CommandSourceStack pSource,
        CloneCommands.DimensionAndPosition pBegin,
        CloneCommands.DimensionAndPosition pEnd,
        CloneCommands.DimensionAndPosition pDestination,
        Predicate<BlockInWorld> pFilter,
        CloneCommands.Mode pMode,
        boolean pStrict
    ) throws CommandSyntaxException {
        BlockPos blockpos = pBegin.position();
        BlockPos blockpos1 = pEnd.position();
        BoundingBox boundingbox = BoundingBox.fromCorners(blockpos, blockpos1);
        BlockPos blockpos2 = pDestination.position();
        BlockPos blockpos3 = blockpos2.offset(boundingbox.getLength());
        BoundingBox boundingbox1 = BoundingBox.fromCorners(blockpos2, blockpos3);
        ServerLevel serverlevel = pBegin.dimension();
        ServerLevel serverlevel1 = pDestination.dimension();
        if (!pMode.canOverlap() && serverlevel == serverlevel1 && boundingbox1.intersects(boundingbox)) {
            throw ERROR_OVERLAP.create();
        } else {
            int i = boundingbox.getXSpan() * boundingbox.getYSpan() * boundingbox.getZSpan();
            int j = pSource.getLevel().getGameRules().getInt(GameRules.RULE_COMMAND_MODIFICATION_BLOCK_LIMIT);
            if (i > j) {
                throw ERROR_AREA_TOO_LARGE.create(j, i);
            } else if (serverlevel.hasChunksAt(blockpos, blockpos1) && serverlevel1.hasChunksAt(blockpos2, blockpos3)) {
                if (serverlevel1.isDebug()) {
                    throw ERROR_FAILED.create();
                } else {
                    List<CloneCommands.CloneBlockInfo> list = Lists.newArrayList();
                    List<CloneCommands.CloneBlockInfo> list1 = Lists.newArrayList();
                    List<CloneCommands.CloneBlockInfo> list2 = Lists.newArrayList();
                    Deque<BlockPos> deque = Lists.newLinkedList();
                    BlockPos blockpos4 = new BlockPos(
                        boundingbox1.minX() - boundingbox.minX(),
                        boundingbox1.minY() - boundingbox.minY(),
                        boundingbox1.minZ() - boundingbox.minZ()
                    );

                    for (int k = boundingbox.minZ(); k <= boundingbox.maxZ(); k++) {
                        for (int l = boundingbox.minY(); l <= boundingbox.maxY(); l++) {
                            for (int i1 = boundingbox.minX(); i1 <= boundingbox.maxX(); i1++) {
                                BlockPos blockpos5 = new BlockPos(i1, l, k);
                                BlockPos blockpos6 = blockpos5.offset(blockpos4);
                                BlockInWorld blockinworld = new BlockInWorld(serverlevel, blockpos5, false);
                                BlockState blockstate = blockinworld.getState();
                                if (pFilter.test(blockinworld)) {
                                    BlockEntity blockentity = serverlevel.getBlockEntity(blockpos5);
                                    if (blockentity != null) {
                                        CloneCommands.CloneBlockEntityInfo clonecommands$cloneblockentityinfo = new CloneCommands.CloneBlockEntityInfo(
                                            blockentity.saveCustomOnly(pSource.registryAccess()), blockentity.components()
                                        );
                                        list1.add(new CloneCommands.CloneBlockInfo(blockpos6, blockstate, clonecommands$cloneblockentityinfo));
                                        deque.addLast(blockpos5);
                                    } else if (!blockstate.isSolidRender() && !blockstate.isCollisionShapeFullBlock(serverlevel, blockpos5)) {
                                        list2.add(new CloneCommands.CloneBlockInfo(blockpos6, blockstate, null));
                                        deque.addFirst(blockpos5);
                                    } else {
                                        list.add(new CloneCommands.CloneBlockInfo(blockpos6, blockstate, null));
                                        deque.addLast(blockpos5);
                                    }
                                }
                            }
                        }
                    }

                    int j1 = 2 | (pStrict ? 816 : 0);
                    if (pMode == CloneCommands.Mode.MOVE) {
                        for (BlockPos blockpos7 : deque) {
                            serverlevel.setBlock(blockpos7, Blocks.BARRIER.defaultBlockState(), j1 | 816);
                        }

                        int k1 = pStrict ? j1 : 3;

                        for (BlockPos blockpos8 : deque) {
                            serverlevel.setBlock(blockpos8, Blocks.AIR.defaultBlockState(), k1);
                        }
                    }

                    List<CloneCommands.CloneBlockInfo> list3 = Lists.newArrayList();
                    list3.addAll(list);
                    list3.addAll(list1);
                    list3.addAll(list2);
                    List<CloneCommands.CloneBlockInfo> list4 = Lists.reverse(list3);

                    for (CloneCommands.CloneBlockInfo clonecommands$cloneblockinfo : list4) {
                        serverlevel1.setBlock(clonecommands$cloneblockinfo.pos, Blocks.BARRIER.defaultBlockState(), j1 | 816);
                    }

                    int l1 = 0;

                    for (CloneCommands.CloneBlockInfo clonecommands$cloneblockinfo1 : list3) {
                        if (serverlevel1.setBlock(clonecommands$cloneblockinfo1.pos, clonecommands$cloneblockinfo1.state, j1)) {
                            l1++;
                        }
                    }

                    for (CloneCommands.CloneBlockInfo clonecommands$cloneblockinfo2 : list1) {
                        BlockEntity blockentity1 = serverlevel1.getBlockEntity(clonecommands$cloneblockinfo2.pos);
                        if (clonecommands$cloneblockinfo2.blockEntityInfo != null && blockentity1 != null) {
                            blockentity1.loadCustomOnly(clonecommands$cloneblockinfo2.blockEntityInfo.tag, serverlevel1.registryAccess());
                            blockentity1.setComponents(clonecommands$cloneblockinfo2.blockEntityInfo.components);
                            blockentity1.setChanged();
                        }

                        serverlevel1.setBlock(clonecommands$cloneblockinfo2.pos, clonecommands$cloneblockinfo2.state, j1);
                    }

                    if (!pStrict) {
                        for (CloneCommands.CloneBlockInfo clonecommands$cloneblockinfo3 : list4) {
                            serverlevel1.updateNeighborsAt(clonecommands$cloneblockinfo3.pos, clonecommands$cloneblockinfo3.state.getBlock());
                        }
                    }

                    serverlevel1.getBlockTicks().copyAreaFrom(serverlevel.getBlockTicks(), boundingbox, blockpos4);
                    if (l1 == 0) {
                        throw ERROR_FAILED.create();
                    } else {
                        int i2 = l1;
                        pSource.sendSuccess(() -> Component.translatable("commands.clone.success", i2), true);
                        return l1;
                    }
                }
            } else {
                throw BlockPosArgument.ERROR_NOT_LOADED.create();
            }
        }
    }

    record CloneBlockEntityInfo(CompoundTag tag, DataComponentMap components) {
    }

    record CloneBlockInfo(BlockPos pos, BlockState state, @Nullable CloneCommands.CloneBlockEntityInfo blockEntityInfo) {
    }

    record DimensionAndPosition(ServerLevel dimension, BlockPos position) {
    }

    static enum Mode {
        FORCE(true),
        MOVE(true),
        NORMAL(false);

        private final boolean canOverlap;

        private Mode(final boolean pCanOverlap) {
            this.canOverlap = pCanOverlap;
        }

        public boolean canOverlap() {
            return this.canOverlap;
        }
    }
}