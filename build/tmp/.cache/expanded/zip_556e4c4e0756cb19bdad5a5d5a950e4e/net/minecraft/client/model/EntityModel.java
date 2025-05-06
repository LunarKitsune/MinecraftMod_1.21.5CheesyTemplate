package net.minecraft.client.model;

import java.util.function.Function;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class EntityModel<T extends EntityRenderState> extends Model {
    public static final float MODEL_Y_OFFSET = -1.501F;

    protected EntityModel(ModelPart pRoot) {
        this(pRoot, RenderType::entityCutoutNoCull);
    }

    protected EntityModel(ModelPart p_367878_, Function<ResourceLocation, RenderType> p_102613_) {
        super(p_367878_, p_102613_);
    }

    public void setupAnim(T pRenderState) {
        this.resetPose();
    }
}