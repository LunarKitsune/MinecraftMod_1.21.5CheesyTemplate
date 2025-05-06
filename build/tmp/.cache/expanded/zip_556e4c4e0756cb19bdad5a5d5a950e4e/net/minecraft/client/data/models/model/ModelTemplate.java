package net.minecraft.client.data.models.model;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import com.google.gson.JsonObject;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ModelTemplate {
    private final Optional<ResourceLocation> model;
    private final Set<TextureSlot> requiredSlots;
    private final Optional<String> suffix;

    public ModelTemplate(Optional<ResourceLocation> pModel, Optional<String> pSuffix, TextureSlot... pRequiredSlots) {
        this.model = pModel;
        this.suffix = pSuffix;
        this.requiredSlots = ImmutableSet.copyOf(pRequiredSlots);
    }

    public ResourceLocation getDefaultModelLocation(Block pBlock) {
        return ModelLocationUtils.getModelLocation(pBlock, this.suffix.orElse(""));
    }

    public ResourceLocation create(Block pBlock, TextureMapping pTextureMapping, BiConsumer<ResourceLocation, ModelInstance> pOutput) {
        return this.create(ModelLocationUtils.getModelLocation(pBlock, this.suffix.orElse("")), pTextureMapping, pOutput);
    }

    public ResourceLocation createWithSuffix(Block pBlock, String pSuffix, TextureMapping pTextureMapping, BiConsumer<ResourceLocation, ModelInstance> pOutput) {
        return this.create(ModelLocationUtils.getModelLocation(pBlock, pSuffix + this.suffix.orElse("")), pTextureMapping, pOutput);
    }

    public ResourceLocation createWithOverride(Block pBlock, String pSuffix, TextureMapping pTextureMapping, BiConsumer<ResourceLocation, ModelInstance> pOutput) {
        return this.create(ModelLocationUtils.getModelLocation(pBlock, pSuffix), pTextureMapping, pOutput);
    }

    public ResourceLocation create(Item pItem, TextureMapping pTextureMapping, BiConsumer<ResourceLocation, ModelInstance> pOutput) {
        return this.create(ModelLocationUtils.getModelLocation(pItem, this.suffix.orElse("")), pTextureMapping, pOutput);
    }

    public ResourceLocation create(ResourceLocation pModelLocation, TextureMapping pTextureMapping, BiConsumer<ResourceLocation, ModelInstance> pOutput) {
        Map<TextureSlot, ResourceLocation> map = this.createMap(pTextureMapping);
        pOutput.accept(pModelLocation, () -> {
            JsonObject jsonobject = new JsonObject();
            this.model.ifPresent(p_376687_ -> jsonobject.addProperty("parent", p_376687_.toString()));
            if (!map.isEmpty()) {
                JsonObject jsonobject1 = new JsonObject();
                map.forEach((p_375899_, p_377821_) -> jsonobject1.addProperty(p_375899_.getId(), p_377821_.toString()));
                jsonobject.add("textures", jsonobject1);
            }

            return jsonobject;
        });
        return pModelLocation;
    }

    private Map<TextureSlot, ResourceLocation> createMap(TextureMapping pTextureMapping) {
        return Streams.concat(this.requiredSlots.stream(), pTextureMapping.getForced()).collect(ImmutableMap.toImmutableMap(Function.identity(), pTextureMapping::get));
    }
}