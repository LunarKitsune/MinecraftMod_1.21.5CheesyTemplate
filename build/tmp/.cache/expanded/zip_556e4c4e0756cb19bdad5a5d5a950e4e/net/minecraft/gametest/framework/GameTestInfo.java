package net.minecraft.gametest.framework;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Object2LongMap.Entry;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.TestInstanceBlockEntity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;

public class GameTestInfo {
    private final Holder.Reference<GameTestInstance> test;
    @Nullable
    private BlockPos testBlockPos;
    private final ServerLevel level;
    private final Collection<GameTestListener> listeners = Lists.newArrayList();
    private final int timeoutTicks;
    private final Collection<GameTestSequence> sequences = Lists.newCopyOnWriteArrayList();
    private final Object2LongMap<Runnable> runAtTickTimeMap = new Object2LongOpenHashMap<>();
    private boolean placedStructure;
    private boolean chunksLoaded;
    private int tickCount;
    private boolean started;
    private final RetryOptions retryOptions;
    private final Stopwatch timer = Stopwatch.createUnstarted();
    private boolean done;
    private final Rotation extraRotation;
    @Nullable
    private GameTestException error;
    @Nullable
    private TestInstanceBlockEntity testInstanceBlockEntity;

    public GameTestInfo(Holder.Reference<GameTestInstance> pTest, Rotation pRotation, ServerLevel pLevel, RetryOptions pRetryOptions) {
        this.test = pTest;
        this.level = pLevel;
        this.retryOptions = pRetryOptions;
        this.timeoutTicks = pTest.value().maxTicks();
        this.extraRotation = pRotation;
    }

    public void setTestBlockPos(@Nullable BlockPos pTestBlockPos) {
        this.testBlockPos = pTestBlockPos;
    }

    public GameTestInfo startExecution(int pDelay) {
        this.tickCount = -(this.test.value().setupTicks() + pDelay + 1);
        return this;
    }

    public void placeStructure() {
        if (!this.placedStructure) {
            TestInstanceBlockEntity testinstanceblockentity = this.getTestInstanceBlockEntity();
            if (!testinstanceblockentity.placeStructure()) {
                this.fail(Component.translatable("test.error.structure.failure", testinstanceblockentity.getTestName().getString()));
            }

            this.placedStructure = true;
            testinstanceblockentity.encaseStructure();
            BoundingBox boundingbox = testinstanceblockentity.getStructureBoundingBox();
            this.level.getBlockTicks().clearArea(boundingbox);
            this.level.clearBlockEvents(boundingbox);
            this.listeners.forEach(p_127630_ -> p_127630_.testStructureLoaded(this));
        }
    }

    public void tick(GameTestRunner pRunner) {
        if (!this.isDone()) {
            if (!this.placedStructure) {
                this.fail(Component.translatable("test.error.ticking_without_structure"));
            }

            if (this.testInstanceBlockEntity == null) {
                this.fail(Component.translatable("test.error.missing_block_entity"));
            }

            if (this.error != null) {
                this.finish();
            }

            if (this.chunksLoaded || this.testInstanceBlockEntity.getStructureBoundingBox().intersectingChunks().allMatch(this.level::areEntitiesActuallyLoadedAndTicking)) {
                this.chunksLoaded = true;
                this.tickInternal();
                if (this.isDone()) {
                    if (this.error != null) {
                        this.listeners.forEach(p_325940_ -> p_325940_.testFailed(this, pRunner));
                    } else {
                        this.listeners.forEach(p_325938_ -> p_325938_.testPassed(this, pRunner));
                    }
                }
            }
        }
    }

    private void tickInternal() {
        this.tickCount++;
        if (this.tickCount >= 0) {
            if (!this.started) {
                this.startTest();
            }

            ObjectIterator<Entry<Runnable>> objectiterator = this.runAtTickTimeMap.object2LongEntrySet().iterator();

            while (objectiterator.hasNext()) {
                Entry<Runnable> entry = objectiterator.next();
                if (entry.getLongValue() <= this.tickCount) {
                    try {
                        entry.getKey().run();
                    } catch (GameTestException gametestexception) {
                        this.fail(gametestexception);
                    } catch (Exception exception) {
                        this.fail(new UnknownGameTestException(exception));
                    }

                    objectiterator.remove();
                }
            }

            if (this.tickCount > this.timeoutTicks) {
                if (this.sequences.isEmpty()) {
                    this.fail(new GameTestTimeoutException(Component.translatable("test.error.timeout.no_result", this.test.value().maxTicks())));
                } else {
                    this.sequences.forEach(p_389764_ -> p_389764_.tickAndFailIfNotComplete(this.tickCount));
                    if (this.error == null) {
                        this.fail(
                            new GameTestTimeoutException(
                                Component.translatable("test.error.timeout.no_sequences_finished", this.test.value().maxTicks())
                            )
                        );
                    }
                }
            } else {
                this.sequences.forEach(p_389763_ -> p_389763_.tickAndContinue(this.tickCount));
            }
        }
    }

    private void startTest() {
        if (!this.started) {
            this.started = true;
            this.getTestInstanceBlockEntity().setRunning();

            try {
                this.test.value().run(new GameTestHelper(this));
            } catch (GameTestException gametestexception) {
                this.fail(gametestexception);
            } catch (Exception exception) {
                this.fail(new UnknownGameTestException(exception));
            }
        }
    }

    public void setRunAtTickTime(long pTickTime, Runnable pTask) {
        this.runAtTickTimeMap.put(pTask, pTickTime);
    }

    public ResourceLocation id() {
        return this.test.key().location();
    }

    @Nullable
    public BlockPos getTestBlockPos() {
        return this.testBlockPos;
    }

    public BlockPos getTestOrigin() {
        return this.testInstanceBlockEntity.getStartCorner();
    }

    public AABB getStructureBounds() {
        TestInstanceBlockEntity testinstanceblockentity = this.getTestInstanceBlockEntity();
        return testinstanceblockentity.getStructureBounds();
    }

    public TestInstanceBlockEntity getTestInstanceBlockEntity() {
        if (this.testInstanceBlockEntity == null) {
            if (this.testBlockPos == null) {
                throw new IllegalStateException("This GameTestInfo has no position");
            }

            if (this.level.getBlockEntity(this.testBlockPos) instanceof TestInstanceBlockEntity testinstanceblockentity) {
                this.testInstanceBlockEntity = testinstanceblockentity;
            }

            if (this.testInstanceBlockEntity == null) {
                throw new IllegalStateException("Could not find a test instance block entity at the given coordinate " + this.testBlockPos);
            }
        }

        return this.testInstanceBlockEntity;
    }

    public ServerLevel getLevel() {
        return this.level;
    }

    public boolean hasSucceeded() {
        return this.done && this.error == null;
    }

    public boolean hasFailed() {
        return this.error != null;
    }

    public boolean hasStarted() {
        return this.started;
    }

    public boolean isDone() {
        return this.done;
    }

    public long getRunTime() {
        return this.timer.elapsed(TimeUnit.MILLISECONDS);
    }

    private void finish() {
        if (!this.done) {
            this.done = true;
            if (this.timer.isRunning()) {
                this.timer.stop();
            }
        }
    }

    public void succeed() {
        if (this.error == null) {
            this.finish();
            AABB aabb = this.getStructureBounds();
            List<Entity> list = this.getLevel().getEntitiesOfClass(Entity.class, aabb.inflate(1.0), p_308532_ -> !(p_308532_ instanceof Player));
            list.forEach(p_308534_ -> p_308534_.remove(Entity.RemovalReason.DISCARDED));
        }
    }

    public void fail(Component pMessage) {
        this.fail(new GameTestAssertException(pMessage, this.tickCount));
    }

    public void fail(GameTestException pError) {
        this.error = pError;
    }

    @Nullable
    public GameTestException getError() {
        return this.error;
    }

    @Override
    public String toString() {
        return this.id().toString();
    }

    public void addListener(GameTestListener pListener) {
        this.listeners.add(pListener);
    }

    public GameTestInfo prepareTestStructure() {
        this.testInstanceBlockEntity = this.createTestInstanceBlock(Objects.requireNonNull(this.testBlockPos), this.extraRotation, this.level);
        this.placeStructure();
        return this;
    }

    private TestInstanceBlockEntity createTestInstanceBlock(BlockPos pPos, Rotation pRotation, ServerLevel pLevel) {
        pLevel.setBlockAndUpdate(pPos, Blocks.TEST_INSTANCE_BLOCK.defaultBlockState());
        TestInstanceBlockEntity testinstanceblockentity = Objects.requireNonNull((TestInstanceBlockEntity)pLevel.getBlockEntity(pPos));
        ResourceKey<GameTestInstance> resourcekey = this.getTestHolder().key();
        Vec3i vec3i = TestInstanceBlockEntity.getStructureSize(pLevel, resourcekey).orElse(new Vec3i(1, 1, 1));
        testinstanceblockentity.set(
            new TestInstanceBlockEntity.Data(Optional.of(resourcekey), vec3i, pRotation, false, TestInstanceBlockEntity.Status.CLEARED, Optional.empty())
        );
        return testinstanceblockentity;
    }

    int getTick() {
        return this.tickCount;
    }

    GameTestSequence createSequence() {
        GameTestSequence gametestsequence = new GameTestSequence(this);
        this.sequences.add(gametestsequence);
        return gametestsequence;
    }

    public boolean isRequired() {
        return this.test.value().required();
    }

    public boolean isOptional() {
        return !this.test.value().required();
    }

    public ResourceLocation getStructure() {
        return this.test.value().structure();
    }

    public Rotation getRotation() {
        return this.test.value().info().rotation().getRotated(this.extraRotation);
    }

    public GameTestInstance getTest() {
        return this.test.value();
    }

    public Holder.Reference<GameTestInstance> getTestHolder() {
        return this.test;
    }

    public int getTimeoutTicks() {
        return this.timeoutTicks;
    }

    public boolean isFlaky() {
        return this.test.value().maxAttempts() > 1;
    }

    public int maxAttempts() {
        return this.test.value().maxAttempts();
    }

    public int requiredSuccesses() {
        return this.test.value().requiredSuccesses();
    }

    public RetryOptions retryOptions() {
        return this.retryOptions;
    }

    public Stream<GameTestListener> getListeners() {
        return this.listeners.stream();
    }

    public GameTestInfo copyReset() {
        GameTestInfo gametestinfo = new GameTestInfo(this.test, this.extraRotation, this.level, this.retryOptions());
        if (this.testBlockPos != null) {
            gametestinfo.setTestBlockPos(this.testBlockPos);
        }

        return gametestinfo;
    }
}