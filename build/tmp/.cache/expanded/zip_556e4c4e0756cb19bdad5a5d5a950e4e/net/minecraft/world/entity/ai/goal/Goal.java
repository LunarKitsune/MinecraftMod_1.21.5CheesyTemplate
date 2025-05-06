package net.minecraft.world.entity.ai.goal;

import java.util.EnumSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public abstract class Goal {
    private final EnumSet<Goal.Flag> flags = EnumSet.noneOf(Goal.Flag.class);

    public abstract boolean canUse();

    public boolean canContinueToUse() {
        return this.canUse();
    }

    public boolean isInterruptable() {
        return true;
    }

    public void start() {
    }

    public void stop() {
    }

    public boolean requiresUpdateEveryTick() {
        return false;
    }

    public void tick() {
    }

    public void setFlags(EnumSet<Goal.Flag> pFlagSet) {
        this.flags.clear();
        this.flags.addAll(pFlagSet);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    public EnumSet<Goal.Flag> getFlags() {
        return this.flags;
    }

    protected int adjustedTickDelay(int pAdjustment) {
        return this.requiresUpdateEveryTick() ? pAdjustment : reducedTickDelay(pAdjustment);
    }

    protected static int reducedTickDelay(int pReduction) {
        return Mth.positiveCeilDiv(pReduction, 2);
    }

    protected static ServerLevel getServerLevel(Entity pEntity) {
        return (ServerLevel)pEntity.level();
    }

    protected static ServerLevel getServerLevel(Level pLevel) {
        return (ServerLevel)pLevel;
    }

    public static enum Flag {
        MOVE,
        LOOK,
        JUMP,
        TARGET;
    }
}