package net.minecraft.gametest.framework;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.ResourceSelectorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.commands.InCommandFunction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.TestInstanceBlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.BlockHitResult;
import org.apache.commons.lang3.mutable.MutableInt;

public class TestCommand {
    public static final int TEST_NEARBY_SEARCH_RADIUS = 15;
    public static final int TEST_FULL_SEARCH_RADIUS = 200;
    public static final int VERIFY_TEST_GRID_AXIS_SIZE = 10;
    public static final int VERIFY_TEST_BATCH_SIZE = 100;
    private static final int DEFAULT_CLEAR_RADIUS = 200;
    private static final int MAX_CLEAR_RADIUS = 1024;
    private static final int TEST_POS_Z_OFFSET_FROM_PLAYER = 3;
    private static final int SHOW_POS_DURATION_MS = 10000;
    private static final int DEFAULT_X_SIZE = 5;
    private static final int DEFAULT_Y_SIZE = 5;
    private static final int DEFAULT_Z_SIZE = 5;
    private static final SimpleCommandExceptionType CLEAR_NO_TESTS = new SimpleCommandExceptionType(Component.translatable("commands.test.clear.error.no_tests"));
    private static final SimpleCommandExceptionType RESET_NO_TESTS = new SimpleCommandExceptionType(Component.translatable("commands.test.reset.error.no_tests"));
    private static final SimpleCommandExceptionType TEST_INSTANCE_COULD_NOT_BE_FOUND = new SimpleCommandExceptionType(
        Component.translatable("commands.test.error.test_instance_not_found")
    );
    private static final SimpleCommandExceptionType NO_STRUCTURES_TO_EXPORT = new SimpleCommandExceptionType(Component.literal("Could not find any structures to export"));
    private static final SimpleCommandExceptionType NO_TEST_INSTANCES = new SimpleCommandExceptionType(Component.translatable("commands.test.error.no_test_instances"));
    private static final Dynamic3CommandExceptionType NO_TEST_CONTAINING = new Dynamic3CommandExceptionType(
        (p_389834_, p_389835_, p_389836_) -> Component.translatableEscape("commands.test.error.no_test_containing_pos", p_389834_, p_389835_, p_389836_)
    );
    private static final DynamicCommandExceptionType TOO_LARGE = new DynamicCommandExceptionType(
        p_389800_ -> Component.translatableEscape("commands.test.error.too_large", p_389800_)
    );

    private static int reset(TestFinder pTestFinder) throws CommandSyntaxException {
        stopTests();
        int i = toGameTestInfos(pTestFinder.source(), RetryOptions.noRetries(), pTestFinder)
            .map(p_389815_ -> resetGameTestInfo(pTestFinder.source(), p_389815_))
            .toList()
            .size();
        if (i == 0) {
            throw CLEAR_NO_TESTS.create();
        } else {
            pTestFinder.source().sendSuccess(() -> Component.translatable("commands.test.reset.success", i), true);
            return i;
        }
    }

    private static int clear(TestFinder pTestFinder) throws CommandSyntaxException {
        stopTests();
        CommandSourceStack commandsourcestack = pTestFinder.source();
        ServerLevel serverlevel = commandsourcestack.getLevel();
        GameTestRunner.clearMarkers(serverlevel);
        List<BoundingBox> list = pTestFinder.findTestPos()
            .flatMap(p_389831_ -> serverlevel.getBlockEntity(p_389831_, BlockEntityType.TEST_INSTANCE_BLOCK).stream())
            .map(TestInstanceBlockEntity::getStructureBoundingBox)
            .toList();
        list.forEach(p_389852_ -> StructureUtils.clearSpaceForStructure(p_389852_, serverlevel));
        if (list.isEmpty()) {
            throw CLEAR_NO_TESTS.create();
        } else {
            commandsourcestack.sendSuccess(() -> Component.translatable("commands.test.clear.success", list.size()), true);
            return list.size();
        }
    }

    private static int export(TestFinder pTestFinder) throws CommandSyntaxException {
        CommandSourceStack commandsourcestack = pTestFinder.source();
        ServerLevel serverlevel = commandsourcestack.getLevel();
        int i = 0;
        boolean flag = true;

        for (Iterator<BlockPos> iterator = pTestFinder.findTestPos().iterator(); iterator.hasNext(); i++) {
            BlockPos blockpos = iterator.next();
            if (!(serverlevel.getBlockEntity(blockpos) instanceof TestInstanceBlockEntity testinstanceblockentity)) {
                throw TEST_INSTANCE_COULD_NOT_BE_FOUND.create();
            }

            if (!testinstanceblockentity.exportTest(commandsourcestack::sendSystemMessage)) {
                flag = false;
            }
        }

        if (i == 0) {
            throw NO_STRUCTURES_TO_EXPORT.create();
        } else {
            String s = "Exported " + i + " structures";
            pTestFinder.source().sendSuccess(() -> Component.literal(s), true);
            return flag ? 0 : 1;
        }
    }

    private static int verify(TestFinder pTestFinder) {
        stopTests();
        CommandSourceStack commandsourcestack = pTestFinder.source();
        ServerLevel serverlevel = commandsourcestack.getLevel();
        BlockPos blockpos = createTestPositionAround(commandsourcestack);
        Collection<GameTestInfo> collection = Stream.concat(
                toGameTestInfos(commandsourcestack, RetryOptions.noRetries(), pTestFinder), toGameTestInfo(commandsourcestack, RetryOptions.noRetries(), pTestFinder, 0)
            )
            .toList();
        GameTestRunner.clearMarkers(serverlevel);
        FailedTestTracker.forgetFailedTests();
        Collection<GameTestBatch> collection1 = new ArrayList<>();

        for (GameTestInfo gametestinfo : collection) {
            for (Rotation rotation : Rotation.values()) {
                Collection<GameTestInfo> collection2 = new ArrayList<>();

                for (int i = 0; i < 100; i++) {
                    GameTestInfo gametestinfo1 = new GameTestInfo(gametestinfo.getTestHolder(), rotation, serverlevel, new RetryOptions(1, true));
                    gametestinfo1.setTestBlockPos(gametestinfo.getTestBlockPos());
                    collection2.add(gametestinfo1);
                }

                GameTestBatch gametestbatch = GameTestBatchFactory.toGameTestBatch(collection2, gametestinfo.getTest().batch(), rotation.ordinal());
                collection1.add(gametestbatch);
            }
        }

        StructureGridSpawner structuregridspawner = new StructureGridSpawner(blockpos, 10, true);
        GameTestRunner gametestrunner = GameTestRunner.Builder.fromBatches(collection1, serverlevel)
            .batcher(GameTestBatchFactory.fromGameTestInfo(100))
            .newStructureSpawner(structuregridspawner)
            .existingStructureSpawner(structuregridspawner)
            .haltOnError(true)
            .build();
        return trackAndStartRunner(commandsourcestack, gametestrunner);
    }

    private static int run(TestFinder pTestFinder, RetryOptions pRetryOptions, int pRotationSteps, int pTestsPerRow) {
        stopTests();
        CommandSourceStack commandsourcestack = pTestFinder.source();
        ServerLevel serverlevel = commandsourcestack.getLevel();
        BlockPos blockpos = createTestPositionAround(commandsourcestack);
        Collection<GameTestInfo> collection = Stream.concat(
                toGameTestInfos(commandsourcestack, pRetryOptions, pTestFinder), toGameTestInfo(commandsourcestack, pRetryOptions, pTestFinder, pRotationSteps)
            )
            .toList();
        if (collection.isEmpty()) {
            commandsourcestack.sendSuccess(() -> Component.translatable("commands.test.no_tests"), false);
            return 0;
        } else {
            GameTestRunner.clearMarkers(serverlevel);
            FailedTestTracker.forgetFailedTests();
            commandsourcestack.sendSuccess(() -> Component.translatable("commands.test.run.running", collection.size()), false);
            GameTestRunner gametestrunner = GameTestRunner.Builder.fromInfo(collection, serverlevel)
                .newStructureSpawner(new StructureGridSpawner(blockpos, pTestsPerRow, false))
                .build();
            return trackAndStartRunner(commandsourcestack, gametestrunner);
        }
    }

    private static int locate(TestFinder pTestFinder) throws CommandSyntaxException {
        pTestFinder.source().sendSystemMessage(Component.translatable("commands.test.locate.started"));
        MutableInt mutableint = new MutableInt(0);
        BlockPos blockpos = BlockPos.containing(pTestFinder.source().getPosition());
        pTestFinder.findTestPos()
            .forEach(
                p_389808_ -> {
                    if (pTestFinder.source().getLevel().getBlockEntity(p_389808_) instanceof TestInstanceBlockEntity testinstanceblockentity) {
                        Direction direction = testinstanceblockentity.getRotation().rotate(Direction.NORTH);
                        BlockPos $$8 = testinstanceblockentity.getBlockPos().relative(direction, 2);
                        int $$9 = (int)direction.getOpposite().toYRot();
                        String $$10 = String.format(Locale.ROOT, "/tp @s %d %d %d %d 0", $$8.getX(), $$8.getY(), $$8.getZ(), $$9);
                        int $$11 = blockpos.getX() - p_389808_.getX();
                        int $$12 = blockpos.getZ() - p_389808_.getZ();
                        int $$13 = Mth.floor(Mth.sqrt($$11 * $$11 + $$12 * $$12));
                        MutableComponent $$14 = ComponentUtils.wrapInSquareBrackets(
                                Component.translatable("chat.coordinates", p_389808_.getX(), p_389808_.getY(), p_389808_.getZ())
                            )
                            .withStyle(
                                p_389833_ -> p_389833_.withColor(ChatFormatting.GREEN)
                                    .withClickEvent(new ClickEvent.SuggestCommand($$10))
                                    .withHoverEvent(new HoverEvent.ShowText(Component.translatable("chat.coordinates.tooltip")))
                            );
                        pTestFinder.source().sendSuccess(() -> Component.translatable("commands.test.locate.found", $$14, $$13), false);
                        mutableint.increment();
                    }
                }
            );
        int i = mutableint.intValue();
        if (i == 0) {
            throw NO_TEST_INSTANCES.create();
        } else {
            pTestFinder.source().sendSuccess(() -> Component.translatable("commands.test.locate.done", i), true);
            return i;
        }
    }

    private static ArgumentBuilder<CommandSourceStack, ?> runWithRetryOptions(
        ArgumentBuilder<CommandSourceStack, ?> pArgumentBuilder,
        InCommandFunction<CommandContext<CommandSourceStack>, TestFinder> pFinderGetter,
        Function<ArgumentBuilder<CommandSourceStack, ?>, ArgumentBuilder<CommandSourceStack, ?>> pModifier
    ) {
        return pArgumentBuilder.executes(p_389857_ -> run(pFinderGetter.apply(p_389857_), RetryOptions.noRetries(), 0, 8))
            .then(
                Commands.argument("numberOfTimes", IntegerArgumentType.integer(0))
                    .executes(
                        p_389822_ -> run(
                            pFinderGetter.apply(p_389822_), new RetryOptions(IntegerArgumentType.getInteger(p_389822_, "numberOfTimes"), false), 0, 8
                        )
                    )
                    .then(
                        pModifier.apply(
                            Commands.argument("untilFailed", BoolArgumentType.bool())
                                .executes(
                                    p_389810_ -> run(
                                        pFinderGetter.apply(p_389810_),
                                        new RetryOptions(
                                            IntegerArgumentType.getInteger(p_389810_, "numberOfTimes"), BoolArgumentType.getBool(p_389810_, "untilFailed")
                                        ),
                                        0,
                                        8
                                    )
                                )
                        )
                    )
            );
    }

    private static ArgumentBuilder<CommandSourceStack, ?> runWithRetryOptions(
        ArgumentBuilder<CommandSourceStack, ?> pArgumentBuilder, InCommandFunction<CommandContext<CommandSourceStack>, TestFinder> pFinderGetter
    ) {
        return runWithRetryOptions(pArgumentBuilder, pFinderGetter, p_325997_ -> p_325997_);
    }

    private static ArgumentBuilder<CommandSourceStack, ?> runWithRetryOptionsAndBuildInfo(
        ArgumentBuilder<CommandSourceStack, ?> pArgumentBuilder, InCommandFunction<CommandContext<CommandSourceStack>, TestFinder> pFinderGetter
    ) {
        return runWithRetryOptions(
            pArgumentBuilder,
            pFinderGetter,
            p_325993_ -> p_325993_.then(
                Commands.argument("rotationSteps", IntegerArgumentType.integer())
                    .executes(
                        p_389839_ -> run(
                            pFinderGetter.apply(p_389839_),
                            new RetryOptions(IntegerArgumentType.getInteger(p_389839_, "numberOfTimes"), BoolArgumentType.getBool(p_389839_, "untilFailed")),
                            IntegerArgumentType.getInteger(p_389839_, "rotationSteps"),
                            8
                        )
                    )
                    .then(
                        Commands.argument("testsPerRow", IntegerArgumentType.integer())
                            .executes(
                                p_389844_ -> run(
                                    pFinderGetter.apply(p_389844_),
                                    new RetryOptions(
                                        IntegerArgumentType.getInteger(p_389844_, "numberOfTimes"), BoolArgumentType.getBool(p_389844_, "untilFailed")
                                    ),
                                    IntegerArgumentType.getInteger(p_389844_, "rotationSteps"),
                                    IntegerArgumentType.getInteger(p_389844_, "testsPerRow")
                                )
                            )
                    )
            )
        );
    }

    public static void register(CommandDispatcher<CommandSourceStack> pDispatcher, CommandBuildContext pBuildContext) {
        ArgumentBuilder<CommandSourceStack, ?> argumentbuilder = runWithRetryOptionsAndBuildInfo(
            Commands.argument("onlyRequiredTests", BoolArgumentType.bool()),
            p_389854_ -> TestFinder.builder().failedTests(p_389854_, BoolArgumentType.getBool(p_389854_, "onlyRequiredTests"))
        );
        LiteralArgumentBuilder<CommandSourceStack> literalargumentbuilder = Commands.literal("test")
            .requires(p_389812_ -> p_389812_.hasPermission(2))
            .then(
                Commands.literal("run")
                    .then(
                        runWithRetryOptionsAndBuildInfo(
                            Commands.argument("tests", ResourceSelectorArgument.resourceSelector(pBuildContext, Registries.TEST_INSTANCE)),
                            p_389797_ -> TestFinder.builder()
                                .byResourceSelection(p_389797_, ResourceSelectorArgument.getSelectedResources(p_389797_, "tests", Registries.TEST_INSTANCE))
                        )
                    )
            )
            .then(
                Commands.literal("runmultiple")
                    .then(
                        Commands.argument("tests", ResourceSelectorArgument.resourceSelector(pBuildContext, Registries.TEST_INSTANCE))
                            .executes(
                                p_389855_ -> run(
                                    TestFinder.builder().byResourceSelection(p_389855_, ResourceSelectorArgument.getSelectedResources(p_389855_, "tests", Registries.TEST_INSTANCE)),
                                    RetryOptions.noRetries(),
                                    0,
                                    8
                                )
                            )
                            .then(
                                Commands.argument("amount", IntegerArgumentType.integer())
                                    .executes(
                                        p_389823_ -> run(
                                            TestFinder.builder()
                                                .createMultipleCopies(IntegerArgumentType.getInteger(p_389823_, "amount"))
                                                .byResourceSelection(p_389823_, ResourceSelectorArgument.getSelectedResources(p_389823_, "tests", Registries.TEST_INSTANCE)),
                                            RetryOptions.noRetries(),
                                            0,
                                            8
                                        )
                                    )
                            )
                    )
            )
            .then(runWithRetryOptions(Commands.literal("runthese"), TestFinder.builder()::allNearby))
            .then(runWithRetryOptions(Commands.literal("runclosest"), TestFinder.builder()::nearest))
            .then(runWithRetryOptions(Commands.literal("runthat"), TestFinder.builder()::lookedAt))
            .then(runWithRetryOptionsAndBuildInfo(Commands.literal("runfailed").then(argumentbuilder), TestFinder.builder()::failedTests))
            .then(
                Commands.literal("verify")
                    .then(
                        Commands.argument("tests", ResourceSelectorArgument.resourceSelector(pBuildContext, Registries.TEST_INSTANCE))
                            .executes(
                                p_389819_ -> verify(
                                    TestFinder.builder().byResourceSelection(p_389819_, ResourceSelectorArgument.getSelectedResources(p_389819_, "tests", Registries.TEST_INSTANCE))
                                )
                            )
                    )
            )
            .then(
                Commands.literal("locate")
                    .then(
                        Commands.argument("tests", ResourceSelectorArgument.resourceSelector(pBuildContext, Registries.TEST_INSTANCE))
                            .executes(
                                p_389847_ -> locate(
                                    TestFinder.builder().byResourceSelection(p_389847_, ResourceSelectorArgument.getSelectedResources(p_389847_, "tests", Registries.TEST_INSTANCE))
                                )
                            )
                    )
            )
            .then(Commands.literal("resetclosest").executes(p_389837_ -> reset(TestFinder.builder().nearest(p_389837_))))
            .then(Commands.literal("resetthese").executes(p_389842_ -> reset(TestFinder.builder().allNearby(p_389842_))))
            .then(Commands.literal("resetthat").executes(p_389840_ -> reset(TestFinder.builder().lookedAt(p_389840_))))
            .then(Commands.literal("clearthat").executes(p_389813_ -> clear(TestFinder.builder().lookedAt(p_389813_))))
            .then(Commands.literal("clearthese").executes(p_389845_ -> clear(TestFinder.builder().allNearby(p_389845_))))
            .then(
                Commands.literal("clearall")
                    .executes(p_389796_ -> clear(TestFinder.builder().radius(p_389796_, 200)))
                    .then(
                        Commands.argument("radius", IntegerArgumentType.integer())
                            .executes(
                                p_389820_ -> clear(
                                    TestFinder.builder().radius(p_389820_, Mth.clamp(IntegerArgumentType.getInteger(p_389820_, "radius"), 0, 1024))
                                )
                            )
                    )
            )
            .then(Commands.literal("stop").executes(p_326006_ -> stopTests()))
            .then(
                Commands.literal("pos")
                    .executes(p_128023_ -> showPos(p_128023_.getSource(), "pos"))
                    .then(
                        Commands.argument("var", StringArgumentType.word())
                            .executes(p_128021_ -> showPos(p_128021_.getSource(), StringArgumentType.getString(p_128021_, "var")))
                    )
            )
            .then(
                Commands.literal("create")
                    .then(
                        Commands.argument("id", ResourceLocationArgument.id())
                            .suggests(TestCommand::suggestTestFunction)
                            .executes(p_389858_ -> createNewStructure(p_389858_.getSource(), ResourceLocationArgument.getId(p_389858_, "id"), 5, 5, 5))
                            .then(
                                Commands.argument("width", IntegerArgumentType.integer())
                                    .executes(
                                        p_389798_ -> createNewStructure(
                                            p_389798_.getSource(),
                                            ResourceLocationArgument.getId(p_389798_, "id"),
                                            IntegerArgumentType.getInteger(p_389798_, "width"),
                                            IntegerArgumentType.getInteger(p_389798_, "width"),
                                            IntegerArgumentType.getInteger(p_389798_, "width")
                                        )
                                    )
                                    .then(
                                        Commands.argument("height", IntegerArgumentType.integer())
                                            .then(
                                                Commands.argument("depth", IntegerArgumentType.integer())
                                                    .executes(
                                                        p_389811_ -> createNewStructure(
                                                            p_389811_.getSource(),
                                                            ResourceLocationArgument.getId(p_389811_, "id"),
                                                            IntegerArgumentType.getInteger(p_389811_, "width"),
                                                            IntegerArgumentType.getInteger(p_389811_, "height"),
                                                            IntegerArgumentType.getInteger(p_389811_, "depth")
                                                        )
                                                    )
                                            )
                                    )
                            )
                    )
            );
        if (SharedConstants.IS_RUNNING_IN_IDE) {
            literalargumentbuilder = literalargumentbuilder.then(
                    Commands.literal("export")
                        .then(
                            Commands.argument("test", ResourceArgument.resource(pBuildContext, Registries.TEST_INSTANCE))
                                .executes(p_389829_ -> exportTestStructure(p_389829_.getSource(), ResourceArgument.getResource(p_389829_, "test", Registries.TEST_INSTANCE)))
                        )
                )
                .then(Commands.literal("exportclosest").executes(p_389846_ -> export(TestFinder.builder().nearest(p_389846_))))
                .then(Commands.literal("exportthese").executes(p_389818_ -> export(TestFinder.builder().allNearby(p_389818_))))
                .then(Commands.literal("exportthat").executes(p_389802_ -> export(TestFinder.builder().lookedAt(p_389802_))));
        }

        pDispatcher.register(literalargumentbuilder);
    }

    public static CompletableFuture<Suggestions> suggestTestFunction(CommandContext<CommandSourceStack> pContext, SuggestionsBuilder pBuilder) {
        Stream<String> stream = pContext.getSource().registryAccess().lookupOrThrow(Registries.TEST_FUNCTION).listElements().map(Holder::getRegisteredName);
        return SharedSuggestionProvider.suggest(stream, pBuilder);
    }

    private static int resetGameTestInfo(CommandSourceStack pSource, GameTestInfo pTestInfo) {
        TestInstanceBlockEntity testinstanceblockentity = pTestInfo.getTestInstanceBlockEntity();
        testinstanceblockentity.resetTest(pSource::sendSystemMessage);
        return 1;
    }

    private static Stream<GameTestInfo> toGameTestInfos(CommandSourceStack pSource, RetryOptions pRetryOptions, TestPosFinder pPosFinder) {
        return pPosFinder.findTestPos().map(p_389850_ -> createGameTestInfo(p_389850_, pSource, pRetryOptions)).flatMap(Optional::stream);
    }

    private static Stream<GameTestInfo> toGameTestInfo(CommandSourceStack pSource, RetryOptions pRetryOptions, TestInstanceFinder pFinder, int pRotationSteps) {
        return pFinder.findTests()
            .filter(p_389817_ -> verifyStructureExists(pSource, p_389817_.value().structure()))
            .map(
                p_389828_ -> new GameTestInfo(
                    (Holder.Reference<GameTestInstance>)p_389828_, StructureUtils.getRotationForRotationSteps(pRotationSteps), pSource.getLevel(), pRetryOptions
                )
            );
    }

    private static Optional<GameTestInfo> createGameTestInfo(BlockPos pPos, CommandSourceStack pSource, RetryOptions pRetryOptions) {
        ServerLevel serverlevel = pSource.getLevel();
        if (serverlevel.getBlockEntity(pPos) instanceof TestInstanceBlockEntity testinstanceblockentity) {
            Optional<Holder.Reference<GameTestInstance>> optional = testinstanceblockentity.test()
                .flatMap(pSource.registryAccess().lookupOrThrow(Registries.TEST_INSTANCE)::get);
            if (optional.isEmpty()) {
                pSource.sendFailure(Component.translatable("commands.test.error.non_existant_test", testinstanceblockentity.getTestName()));
                return Optional.empty();
            } else {
                Holder.Reference<GameTestInstance> reference = optional.get();
                // Forge: The rotation is stored in the structure block, and added in the GameTestInfo constructor.
                // So reverse it to find the manually specified rotation so the test runs the same every time.
                var rotation = testinstanceblockentity.test().flatMap(testinstanceblockentity.getLevel().registryAccess()::get).map(Holder::value).map(GameTestInstance::rotation).orElse(Rotation.NONE);
                var steps = StructureUtils.getRotationStepsForRotation(testinstanceblockentity.getRotation()) - StructureUtils.getRotationStepsForRotation(rotation);
                if (steps < 0) steps += 4;
                GameTestInfo gametestinfo = new GameTestInfo(reference, StructureUtils.getRotationForRotationSteps(steps), serverlevel, pRetryOptions);
                gametestinfo.setTestBlockPos(pPos);
                return !verifyStructureExists(pSource, gametestinfo.getStructure()) ? Optional.empty() : Optional.of(gametestinfo);
            }
        } else {
            pSource.sendFailure(
                Component.translatable("commands.test.error.test_instance_not_found.position", pPos.getX(), pPos.getY(), pPos.getZ())
            );
            return Optional.empty();
        }
    }

    private static int createNewStructure(CommandSourceStack pSource, ResourceLocation pId, int pWidth, int pHeight, int pDepth) throws CommandSyntaxException {
        if (pWidth <= 48 && pHeight <= 48 && pDepth <= 48) {
            ServerLevel serverlevel = pSource.getLevel();
            BlockPos blockpos = createTestPositionAround(pSource);
            TestInstanceBlockEntity testinstanceblockentity = StructureUtils.createNewEmptyTest(
                pId, blockpos, new Vec3i(pWidth, pHeight, pDepth), Rotation.NONE, serverlevel
            );
            BlockPos blockpos1 = testinstanceblockentity.getStructurePos();
            BlockPos blockpos2 = blockpos1.offset(pWidth - 1, 0, pDepth - 1);
            BlockPos.betweenClosedStream(blockpos1, blockpos2).forEach(p_325982_ -> serverlevel.setBlockAndUpdate(p_325982_, Blocks.BEDROCK.defaultBlockState()));
            pSource.sendSuccess(() -> Component.translatable("commands.test.create.success", testinstanceblockentity.getTestName()), true);
            return 1;
        } else {
            throw TOO_LARGE.create(48);
        }
    }

    private static int showPos(CommandSourceStack pSource, String pVariableName) throws CommandSyntaxException {
        BlockHitResult blockhitresult = (BlockHitResult)pSource.getPlayerOrException().pick(10.0, 1.0F, false);
        BlockPos blockpos = blockhitresult.getBlockPos();
        ServerLevel serverlevel = pSource.getLevel();
        Optional<BlockPos> optional = StructureUtils.findTestContainingPos(blockpos, 15, serverlevel);
        if (optional.isEmpty()) {
            optional = StructureUtils.findTestContainingPos(blockpos, 200, serverlevel);
        }

        if (optional.isEmpty()) {
            throw NO_TEST_CONTAINING.create(blockpos.getX(), blockpos.getY(), blockpos.getZ());
        } else if (serverlevel.getBlockEntity(optional.get()) instanceof TestInstanceBlockEntity testinstanceblockentity) {
            BlockPos blockpos2 = testinstanceblockentity.getStructurePos();
            BlockPos blockpos1 = blockpos.subtract(blockpos2);
            String $$10 = blockpos1.getX() + ", " + blockpos1.getY() + ", " + blockpos1.getZ();
            String $$11 = testinstanceblockentity.getTestName().getString();
            MutableComponent $$12 = Component.translatable("commands.test.coordinates", blockpos1.getX(), blockpos1.getY(), blockpos1.getZ())
                .setStyle(
                    Style.EMPTY
                        .withBold(true)
                        .withColor(ChatFormatting.GREEN)
                        .withHoverEvent(new HoverEvent.ShowText(Component.translatable("commands.test.coordinates.copy")))
                        .withClickEvent(new ClickEvent.CopyToClipboard("final BlockPos " + pVariableName + " = new BlockPos(" + $$10 + ");"))
                );
            pSource.sendSuccess(() -> Component.translatable("commands.test.relative_position", $$11, $$12), false);
            DebugPackets.sendGameTestAddMarker(serverlevel, new BlockPos(blockpos), $$10, -2147418368, 10000);
            return 1;
        } else {
            throw TEST_INSTANCE_COULD_NOT_BE_FOUND.create();
        }
    }

    private static int stopTests() {
        GameTestTicker.SINGLETON.clear();
        return 1;
    }

    public static int trackAndStartRunner(CommandSourceStack pSource, GameTestRunner pTestRunner) {
        pTestRunner.addListener(new TestCommand.TestBatchSummaryDisplayer(pSource));
        MultipleTestTracker multipletesttracker = new MultipleTestTracker(pTestRunner.getTestInfos());
        multipletesttracker.addListener(new TestCommand.TestSummaryDisplayer(pSource, multipletesttracker));
        multipletesttracker.addFailureListener(p_389841_ -> FailedTestTracker.rememberFailedTest(p_389841_.getTestHolder()));
        pTestRunner.start();
        return 1;
    }

    private static int exportTestStructure(CommandSourceStack pSource, Holder<GameTestInstance> pTestInstance) {
        return !TestInstanceBlockEntity.export(pSource.getLevel(), pTestInstance.value().structure(), pSource::sendSystemMessage) ? 0 : 1;
    }

    private static boolean verifyStructureExists(CommandSourceStack pSource, ResourceLocation pStructure) {
        if (pSource.getLevel().getStructureManager().get(pStructure).isEmpty()) {
            pSource.sendFailure(Component.translatable("commands.test.error.structure_not_found", Component.translationArg(pStructure)));
            return false;
        } else {
            return true;
        }
    }

    private static BlockPos createTestPositionAround(CommandSourceStack pSource) {
        BlockPos blockpos = BlockPos.containing(pSource.getPosition());
        int i = pSource.getLevel().getHeightmapPos(Heightmap.Types.WORLD_SURFACE, blockpos).getY();
        return new BlockPos(blockpos.getX(), i, blockpos.getZ() + 3);
    }

    record TestBatchSummaryDisplayer(CommandSourceStack source) implements GameTestBatchListener {
        @Override
        public void testBatchStarting(GameTestBatch p_327831_) {
            this.source.sendSuccess(() -> Component.translatable("commands.test.batch.starting", p_327831_.environment().getRegisteredName(), p_327831_.index()), true);
        }

        @Override
        public void testBatchFinished(GameTestBatch p_335734_) {
        }
    }

    public record TestSummaryDisplayer(CommandSourceStack source, MultipleTestTracker tracker) implements GameTestListener {
        @Override
        public void testStructureLoaded(GameTestInfo p_128064_) {
        }

        @Override
        public void testPassed(GameTestInfo p_177797_, GameTestRunner p_333026_) {
            this.showTestSummaryIfAllDone();
        }

        @Override
        public void testFailed(GameTestInfo p_128066_, GameTestRunner p_333809_) {
            this.showTestSummaryIfAllDone();
        }

        @Override
        public void testAddedForRerun(GameTestInfo p_328539_, GameTestInfo p_335500_, GameTestRunner p_328503_) {
            this.tracker.addTestToTrack(p_335500_);
        }

        private void showTestSummaryIfAllDone() {
            if (this.tracker.isDone()) {
                this.source.sendSuccess(() -> Component.translatable("commands.test.summary", this.tracker.getTotalCount()).withStyle(ChatFormatting.WHITE), true);
                if (this.tracker.hasFailedRequired()) {
                    this.source.sendFailure(Component.translatable("commands.test.summary.failed", this.tracker.getFailedRequiredCount()));
                } else {
                    this.source.sendSuccess(() -> Component.translatable("commands.test.summary.all_required_passed").withStyle(ChatFormatting.GREEN), true);
                }

                if (this.tracker.hasFailedOptional()) {
                    this.source.sendSystemMessage(Component.translatable("commands.test.summary.optional_failed", this.tracker.getFailedOptionalCount()));
                }
            }
        }
    }
}
