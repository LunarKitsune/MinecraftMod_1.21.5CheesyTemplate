package net.minecraft.world.entity.ai.control;

import net.minecraft.util.Mth;

public interface Control {
    default float rotateTowards(float pCurrent, float pWanted, float pMaxSpeed) {
        float f = Mth.degreesDifference(pCurrent, pWanted);
        float f1 = Mth.clamp(f, -pMaxSpeed, pMaxSpeed);
        return pCurrent + f1;
    }
}