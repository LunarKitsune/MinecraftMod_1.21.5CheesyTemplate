package net.minecraft.client.model.geom.builders;

import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PartDefinition {
    private final List<CubeDefinition> cubes;
    private final PartPose partPose;
    private final Map<String, PartDefinition> children = Maps.newHashMap();

    PartDefinition(List<CubeDefinition> pCubes, PartPose pPartPose) {
        this.cubes = pCubes;
        this.partPose = pPartPose;
    }

    public PartDefinition addOrReplaceChild(String pName, CubeListBuilder pCubes, PartPose pPartPose) {
        PartDefinition partdefinition = new PartDefinition(pCubes.getCubes(), pPartPose);
        return this.addOrReplaceChild(pName, partdefinition);
    }

    public PartDefinition addOrReplaceChild(String pName, PartDefinition pChuld) {
        PartDefinition partdefinition = this.children.put(pName, pChuld);
        if (partdefinition != null) {
            pChuld.children.putAll(partdefinition.children);
        }

        return pChuld;
    }

    public PartDefinition clearChild(String pName) {
        PartDefinition partdefinition = this.children.get(pName);
        if (partdefinition == null) {
            throw new IllegalArgumentException("No child with name: " + pName);
        } else {
            return this.addOrReplaceChild(pName, CubeListBuilder.create(), partdefinition.partPose);
        }
    }

    public ModelPart bake(int pTexWidth, int pTexHeight) {
        Object2ObjectArrayMap<String, ModelPart> object2objectarraymap = this.children
            .entrySet()
            .stream()
            .collect(
                Collectors.toMap(
                    Entry::getKey,
                    p_171593_ -> ((PartDefinition)p_171593_.getValue()).bake(pTexWidth, pTexHeight),
                    (p_171595_, p_171596_) -> p_171595_,
                    Object2ObjectArrayMap::new
                )
            );
        List<ModelPart.Cube> list = this.cubes.stream().map(p_171589_ -> p_171589_.bake(pTexWidth, pTexHeight)).toList();
        ModelPart modelpart = new ModelPart(list, object2objectarraymap);
        modelpart.setInitialPose(this.partPose);
        modelpart.loadPose(this.partPose);
        return modelpart;
    }

    public PartDefinition getChild(String pName) {
        return this.children.get(pName);
    }

    public Set<Entry<String, PartDefinition>> getChildren() {
        return this.children.entrySet();
    }

    public PartDefinition transformed(UnaryOperator<PartPose> pTransformer) {
        PartDefinition partdefinition = new PartDefinition(this.cubes, pTransformer.apply(this.partPose));
        partdefinition.children.putAll(this.children);
        return partdefinition;
    }
}