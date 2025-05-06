package net.minecraft.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface VillagerLikeModel {
    void hatVisible(boolean pHatVisible);

    void translateToArms(PoseStack pPoseStack);
}