package net.minecraft.client.renderer.entity.state;

import javax.annotation.Nullable;
import net.minecraft.world.entity.boss.enderdragon.DragonFlightHistory;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EnderDragonRenderState extends EntityRenderState {
    public float flapTime;
    public float deathTime;
    public boolean hasRedOverlay;
    @Nullable
    public Vec3 beamOffset;
    public boolean isLandingOrTakingOff;
    public boolean isSitting;
    public double distanceToEgg;
    public float partialTicks;
    public final DragonFlightHistory flightHistory = new DragonFlightHistory();

    public DragonFlightHistory.Sample getHistoricalPos(int pIndex) {
        return this.flightHistory.get(pIndex, this.partialTicks);
    }

    public float getHeadPartYOffset(int pPart, DragonFlightHistory.Sample pStart, DragonFlightHistory.Sample pCurrent) {
        double d0;
        if (this.isLandingOrTakingOff) {
            d0 = pPart / Math.max(this.distanceToEgg / 4.0, 1.0);
        } else if (this.isSitting) {
            d0 = pPart;
        } else if (pPart == 6) {
            d0 = 0.0;
        } else {
            d0 = pCurrent.y() - pStart.y();
        }

        return (float)d0;
    }
}