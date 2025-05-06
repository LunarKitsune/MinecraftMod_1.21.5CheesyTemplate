package net.minecraft.client.model;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BannerFlagModel extends Model {
    private final ModelPart flag;

    public BannerFlagModel(ModelPart pRoot) {
        super(pRoot, RenderType::entitySolid);
        this.flag = pRoot.getChild("flag");
    }

    public static LayerDefinition createFlagLayer(boolean pIsStanding) {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        partdefinition.addOrReplaceChild(
            "flag",
            CubeListBuilder.create().texOffs(0, 0).addBox(-10.0F, 0.0F, -2.0F, 20.0F, 40.0F, 1.0F),
            PartPose.offset(0.0F, pIsStanding ? -44.0F : -20.5F, pIsStanding ? 0.0F : 10.5F)
        );
        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    public void setupAnim(float pAngle) {
        this.flag.xRot = (-0.0125F + 0.01F * Mth.cos((float) (Math.PI * 2) * pAngle)) * (float) Math.PI;
    }
}