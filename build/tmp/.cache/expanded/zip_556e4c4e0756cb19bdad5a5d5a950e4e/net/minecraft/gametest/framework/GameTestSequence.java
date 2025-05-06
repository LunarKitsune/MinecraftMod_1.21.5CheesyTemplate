package net.minecraft.gametest.framework;

import com.google.common.collect.Lists;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;

public class GameTestSequence {
    final GameTestInfo parent;
    private final List<GameTestEvent> events = Lists.newArrayList();
    private int lastTick;

    GameTestSequence(GameTestInfo pParent) {
        this.parent = pParent;
        this.lastTick = pParent.getTick();
    }

    public GameTestSequence thenWaitUntil(Runnable pTask) {
        this.events.add(GameTestEvent.create(pTask));
        return this;
    }

    public GameTestSequence thenWaitUntil(long pExpectedDelay, Runnable pTask) {
        this.events.add(GameTestEvent.create(pExpectedDelay, pTask));
        return this;
    }

    public GameTestSequence thenIdle(int pTick) {
        return this.thenExecuteAfter(pTick, () -> {});
    }

    public GameTestSequence thenExecute(Runnable pTask) {
        this.events.add(GameTestEvent.create(() -> this.executeWithoutFail(pTask)));
        return this;
    }

    public GameTestSequence thenExecuteAfter(int pTick, Runnable pTask) {
        this.events.add(GameTestEvent.create(() -> {
            if (this.parent.getTick() < this.lastTick + pTick) {
                throw new GameTestAssertException(Component.translatable("test.error.sequence.not_completed"), this.parent.getTick());
            } else {
                this.executeWithoutFail(pTask);
            }
        }));
        return this;
    }

    public GameTestSequence thenExecuteFor(int pTick, Runnable pTask) {
        this.events.add(GameTestEvent.create(() -> {
            if (this.parent.getTick() < this.lastTick + pTick) {
                this.executeWithoutFail(pTask);
                throw new GameTestAssertException(Component.translatable("test.error.sequence.not_completed"), this.parent.getTick());
            }
        }));
        return this;
    }

    public void thenSucceed() {
        this.events.add(GameTestEvent.create(this.parent::succeed));
    }

    public void thenFail(Supplier<GameTestException> pException) {
        this.events.add(GameTestEvent.create(() -> this.parent.fail(pException.get())));
    }

    public GameTestSequence.Condition thenTrigger() {
        GameTestSequence.Condition gametestsequence$condition = new GameTestSequence.Condition();
        this.events.add(GameTestEvent.create(() -> gametestsequence$condition.trigger(this.parent.getTick())));
        return gametestsequence$condition;
    }

    public void tickAndContinue(int pTickCount) {
        try {
            this.tick(pTickCount);
        } catch (GameTestAssertException gametestassertexception) {
        }
    }

    public void tickAndFailIfNotComplete(int pTickCount) {
        try {
            this.tick(pTickCount);
        } catch (GameTestAssertException gametestassertexception) {
            this.parent.fail(gametestassertexception);
        }
    }

    private void executeWithoutFail(Runnable pTask) {
        try {
            pTask.run();
        } catch (GameTestAssertException gametestassertexception) {
            this.parent.fail(gametestassertexception);
        }
    }

    private void tick(int pTickCount) {
        Iterator<GameTestEvent> iterator = this.events.iterator();

        while (iterator.hasNext()) {
            GameTestEvent gametestevent = iterator.next();
            gametestevent.assertion.run();
            iterator.remove();
            int i = pTickCount - this.lastTick;
            int j = this.lastTick;
            this.lastTick = pTickCount;
            if (gametestevent.expectedDelay != null && gametestevent.expectedDelay != i) {
                this.parent
                    .fail(new GameTestAssertException(Component.translatable("test.error.sequence.invalid_tick", j + gametestevent.expectedDelay), pTickCount));
                break;
            }
        }
    }

    public class Condition {
        private static final int NOT_TRIGGERED = -1;
        private int triggerTime = -1;

        void trigger(int pTriggerTime) {
            if (this.triggerTime != -1) {
                throw new IllegalStateException("Condition already triggered at " + this.triggerTime);
            } else {
                this.triggerTime = pTriggerTime;
            }
        }

        public void assertTriggeredThisTick() {
            int i = GameTestSequence.this.parent.getTick();
            if (this.triggerTime != i) {
                if (this.triggerTime == -1) {
                    throw new GameTestAssertException(Component.translatable("test.error.sequence.condition_not_triggered"), i);
                } else {
                    throw new GameTestAssertException(Component.translatable("test.error.sequence.condition_already_triggered", this.triggerTime), i);
                }
            }
        }
    }
}