package net.minecraft.gametest.framework;

import com.mojang.brigadier.context.CommandContext;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;

public class TestFinder implements TestInstanceFinder, TestPosFinder {
    static final TestInstanceFinder NO_FUNCTIONS = Stream::empty;
    static final TestPosFinder NO_STRUCTURES = Stream::empty;
    private final TestInstanceFinder testInstanceFinder;
    private final TestPosFinder testPosFinder;
    private final CommandSourceStack source;

    @Override
    public Stream<BlockPos> findTestPos() {
        return this.testPosFinder.findTestPos();
    }

    public static TestFinder.Builder builder() {
        return new TestFinder.Builder();
    }

    TestFinder(CommandSourceStack pSource, TestInstanceFinder pTestInstanceFinder, TestPosFinder pTestPosFinder) {
        this.source = pSource;
        this.testInstanceFinder = pTestInstanceFinder;
        this.testPosFinder = pTestPosFinder;
    }

    public CommandSourceStack source() {
        return this.source;
    }

    @Override
    public Stream<Holder.Reference<GameTestInstance>> findTests() {
        return this.testInstanceFinder.findTests();
    }

    public static class Builder {
        private final UnaryOperator<Supplier<Stream<Holder.Reference<GameTestInstance>>>> testFinderWrapper;
        private final UnaryOperator<Supplier<Stream<BlockPos>>> structureBlockPosFinderWrapper;

        public Builder() {
            this.testFinderWrapper = p_333647_ -> p_333647_;
            this.structureBlockPosFinderWrapper = p_327811_ -> p_327811_;
        }

        private Builder(UnaryOperator<Supplier<Stream<Holder.Reference<GameTestInstance>>>> pTestFinderWrapper, UnaryOperator<Supplier<Stream<BlockPos>>> pStructureBlockPosFinderWrapper) {
            this.testFinderWrapper = pTestFinderWrapper;
            this.structureBlockPosFinderWrapper = pStructureBlockPosFinderWrapper;
        }

        public TestFinder.Builder createMultipleCopies(int pCount) {
            return new TestFinder.Builder(createCopies(pCount), createCopies(pCount));
        }

        private static <Q> UnaryOperator<Supplier<Stream<Q>>> createCopies(int pCount) {
            return p_389860_ -> {
                List<Q> list = new LinkedList<>();
                List<Q> list1 = ((Stream)p_389860_.get()).toList();

                for (int i = 0; i < pCount; i++) {
                    list.addAll(list1);
                }

                return list::stream;
            };
        }

        private TestFinder build(CommandSourceStack pSource, TestInstanceFinder pInstanceFinder, TestPosFinder pPosFinder) {
            return new TestFinder(pSource, this.testFinderWrapper.apply(pInstanceFinder::findTests)::get, this.structureBlockPosFinderWrapper.apply(pPosFinder::findTestPos)::get);
        }

        public TestFinder radius(CommandContext<CommandSourceStack> pContext, int pRadius) {
            CommandSourceStack commandsourcestack = pContext.getSource();
            BlockPos blockpos = BlockPos.containing(commandsourcestack.getPosition());
            return this.build(commandsourcestack, TestFinder.NO_FUNCTIONS, () -> StructureUtils.findTestBlocks(blockpos, pRadius, commandsourcestack.getLevel()));
        }

        public TestFinder nearest(CommandContext<CommandSourceStack> pContext) {
            CommandSourceStack commandsourcestack = pContext.getSource();
            BlockPos blockpos = BlockPos.containing(commandsourcestack.getPosition());
            return this.build(
                commandsourcestack, TestFinder.NO_FUNCTIONS, () -> StructureUtils.findNearestTest(blockpos, 15, commandsourcestack.getLevel()).stream()
            );
        }

        public TestFinder allNearby(CommandContext<CommandSourceStack> pContext) {
            CommandSourceStack commandsourcestack = pContext.getSource();
            BlockPos blockpos = BlockPos.containing(commandsourcestack.getPosition());
            return this.build(commandsourcestack, TestFinder.NO_FUNCTIONS, () -> StructureUtils.findTestBlocks(blockpos, 200, commandsourcestack.getLevel()));
        }

        public TestFinder lookedAt(CommandContext<CommandSourceStack> pContext) {
            CommandSourceStack commandsourcestack = pContext.getSource();
            return this.build(
                commandsourcestack,
                TestFinder.NO_FUNCTIONS,
                () -> StructureUtils.lookedAtTestPos(
                    BlockPos.containing(commandsourcestack.getPosition()), commandsourcestack.getPlayer().getCamera(), commandsourcestack.getLevel()
                )
            );
        }

        public TestFinder failedTests(CommandContext<CommandSourceStack> pContext, boolean pOnlyRequired) {
            return this.build(
                pContext.getSource(),
                () -> FailedTestTracker.getLastFailedTests().filter(p_389864_ -> !pOnlyRequired || p_389864_.value().required()),
                TestFinder.NO_STRUCTURES
            );
        }

        public TestFinder byResourceSelection(CommandContext<CommandSourceStack> pContext, Collection<Holder.Reference<GameTestInstance>> pCollection) {
            return this.build(pContext.getSource(), pCollection::stream, TestFinder.NO_STRUCTURES);
        }

        public TestFinder failedTests(CommandContext<CommandSourceStack> pContext) {
            return this.failedTests(pContext, false);
        }
    }
}