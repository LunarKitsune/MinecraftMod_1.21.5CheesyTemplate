package net.minecraft.util;

public class BinaryAnimator {
    private final int animationLength;
    private final BinaryAnimator.EasingFunction easingFunction;
    private int ticks;
    private int ticksOld;

    public BinaryAnimator(int pAnimationLength, BinaryAnimator.EasingFunction pEasingFunction) {
        this.animationLength = pAnimationLength;
        this.easingFunction = pEasingFunction;
    }

    public BinaryAnimator(int pAnimationLength) {
        this(pAnimationLength, p_364253_ -> p_364253_);
    }

    public void tick(boolean pCondition) {
        this.ticksOld = this.ticks;
        if (pCondition) {
            if (this.ticks < this.animationLength) {
                this.ticks++;
            }
        } else if (this.ticks > 0) {
            this.ticks--;
        }
    }

    public float getFactor(float pPartialTick) {
        float f = Mth.lerp(pPartialTick, this.ticksOld, this.ticks) / this.animationLength;
        return this.easingFunction.apply(f);
    }

    public interface EasingFunction {
        float apply(float pTicks);
    }
}