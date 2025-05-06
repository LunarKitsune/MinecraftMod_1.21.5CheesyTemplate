package net.minecraft.world.entity;

import java.util.function.Consumer;

public class AnimationState {
    private static final int STOPPED = Integer.MIN_VALUE;
    private int startTick = Integer.MIN_VALUE;

    public void start(int pTickCount) {
        this.startTick = pTickCount;
    }

    public void startIfStopped(int pTickCount) {
        if (!this.isStarted()) {
            this.start(pTickCount);
        }
    }

    public void animateWhen(boolean pCondition, int pTickCount) {
        if (pCondition) {
            this.startIfStopped(pTickCount);
        } else {
            this.stop();
        }
    }

    public void stop() {
        this.startTick = Integer.MIN_VALUE;
    }

    public void ifStarted(Consumer<AnimationState> pAction) {
        if (this.isStarted()) {
            pAction.accept(this);
        }
    }

    public void fastForward(int pDuration, float pSpeed) {
        if (this.isStarted()) {
            this.startTick -= (int)(pDuration * pSpeed);
        }
    }

    public long getTimeInMillis(float pGmaetime) {
        float f = pGmaetime - this.startTick;
        return (long)(f * 50.0F);
    }

    public boolean isStarted() {
        return this.startTick != Integer.MIN_VALUE;
    }

    public void copyFrom(AnimationState pOther) {
        this.startTick = pOther.startTick;
    }
}