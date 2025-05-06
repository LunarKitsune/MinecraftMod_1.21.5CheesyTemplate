package net.minecraft.client.renderer.entity.state;

import javax.annotation.Nullable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record ServerHitboxesRenderState(
    boolean missing,
    double serverEntityX,
    double serverEntityY,
    double serverEntityZ,
    double deltaMovementX,
    double deltaMovementY,
    double deltaMovementZ,
    float eyeHeight,
    @Nullable HitboxesRenderState hitboxes
) {
    public ServerHitboxesRenderState(boolean pMissing) {
        this(pMissing, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0F, null);
    }
}