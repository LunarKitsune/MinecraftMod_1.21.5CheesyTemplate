package net.minecraft.world.level.redstone;

import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.Level;

public class ExperimentalRedstoneUtils {
    @Nullable
    public static Orientation initialOrientation(Level pLevel, @Nullable Direction pFront, @Nullable Direction pUp) {
        if (pLevel.enabledFeatures().contains(FeatureFlags.REDSTONE_EXPERIMENTS)) {
            Orientation orientation = Orientation.random(pLevel.random).withSideBias(Orientation.SideBias.LEFT);
            if (pUp != null) {
                orientation = orientation.withUp(pUp);
            }

            if (pFront != null) {
                orientation = orientation.withFront(pFront);
            }

            return orientation;
        } else {
            return null;
        }
    }

    @Nullable
    public static Orientation withFront(@Nullable Orientation pOrientation, Direction pDirection) {
        return pOrientation == null ? null : pOrientation.withFront(pDirection);
    }
}