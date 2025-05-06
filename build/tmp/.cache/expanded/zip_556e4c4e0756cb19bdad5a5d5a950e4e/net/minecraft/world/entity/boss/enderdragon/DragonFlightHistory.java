package net.minecraft.world.entity.boss.enderdragon;

import java.util.Arrays;
import net.minecraft.util.Mth;

public class DragonFlightHistory {
    public static final int LENGTH = 64;
    private static final int MASK = 63;
    private final DragonFlightHistory.Sample[] samples = new DragonFlightHistory.Sample[64];
    private int head = -1;

    public DragonFlightHistory() {
        Arrays.fill(this.samples, new DragonFlightHistory.Sample(0.0, 0.0F));
    }

    public void copyFrom(DragonFlightHistory pOther) {
        System.arraycopy(pOther.samples, 0, this.samples, 0, 64);
        this.head = pOther.head;
    }

    public void record(double pY, float pYRot) {
        DragonFlightHistory.Sample dragonflighthistory$sample = new DragonFlightHistory.Sample(pY, pYRot);
        if (this.head < 0) {
            Arrays.fill(this.samples, dragonflighthistory$sample);
        }

        if (++this.head == 64) {
            this.head = 0;
        }

        this.samples[this.head] = dragonflighthistory$sample;
    }

    public DragonFlightHistory.Sample get(int pIndex) {
        return this.samples[this.head - pIndex & 63];
    }

    public DragonFlightHistory.Sample get(int pIndex, float pPartialTick) {
        DragonFlightHistory.Sample dragonflighthistory$sample = this.get(pIndex);
        DragonFlightHistory.Sample dragonflighthistory$sample1 = this.get(pIndex + 1);
        return new DragonFlightHistory.Sample(
            Mth.lerp(pPartialTick, dragonflighthistory$sample1.y, dragonflighthistory$sample.y),
            Mth.rotLerp(pPartialTick, dragonflighthistory$sample1.yRot, dragonflighthistory$sample.yRot)
        );
    }

    public record Sample(double y, float yRot) {
    }
}