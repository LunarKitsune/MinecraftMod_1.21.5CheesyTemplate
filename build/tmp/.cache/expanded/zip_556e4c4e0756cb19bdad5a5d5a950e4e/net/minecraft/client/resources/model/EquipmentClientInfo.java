package net.minecraft.client.resources.model;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record EquipmentClientInfo(Map<EquipmentClientInfo.LayerType, List<EquipmentClientInfo.Layer>> layers) {
    private static final Codec<List<EquipmentClientInfo.Layer>> LAYER_LIST_CODEC = ExtraCodecs.nonEmptyList(EquipmentClientInfo.Layer.CODEC.listOf());
    public static final Codec<EquipmentClientInfo> CODEC = RecordCodecBuilder.create(
        p_376111_ -> p_376111_.group(
                ExtraCodecs.nonEmptyMap(Codec.unboundedMap(EquipmentClientInfo.LayerType.CODEC, LAYER_LIST_CODEC))
                    .fieldOf("layers")
                    .forGetter(EquipmentClientInfo::layers)
            )
            .apply(p_376111_, EquipmentClientInfo::new)
    );

    public static EquipmentClientInfo.Builder builder() {
        return new EquipmentClientInfo.Builder();
    }

    public List<EquipmentClientInfo.Layer> getLayers(EquipmentClientInfo.LayerType pType) {
        return this.layers.getOrDefault(pType, List.of());
    }

    @OnlyIn(Dist.CLIENT)
    public static class Builder {
        private final Map<EquipmentClientInfo.LayerType, List<EquipmentClientInfo.Layer>> layersByType = new EnumMap<>(EquipmentClientInfo.LayerType.class);

        Builder() {
        }

        public EquipmentClientInfo.Builder addHumanoidLayers(ResourceLocation pTextureId) {
            return this.addHumanoidLayers(pTextureId, false);
        }

        public EquipmentClientInfo.Builder addHumanoidLayers(ResourceLocation pTextureId, boolean pDyeable) {
            this.addLayers(EquipmentClientInfo.LayerType.HUMANOID_LEGGINGS, EquipmentClientInfo.Layer.leatherDyeable(pTextureId, pDyeable));
            this.addMainHumanoidLayer(pTextureId, pDyeable);
            return this;
        }

        public EquipmentClientInfo.Builder addMainHumanoidLayer(ResourceLocation pTextureId, boolean pDyeable) {
            return this.addLayers(EquipmentClientInfo.LayerType.HUMANOID, EquipmentClientInfo.Layer.leatherDyeable(pTextureId, pDyeable));
        }

        public EquipmentClientInfo.Builder addLayers(EquipmentClientInfo.LayerType pType, EquipmentClientInfo.Layer... pLayers) {
            Collections.addAll(this.layersByType.computeIfAbsent(pType, p_376726_ -> new ArrayList<>()), pLayers);
            return this;
        }

        public EquipmentClientInfo build() {
            return new EquipmentClientInfo(
                this.layersByType.entrySet().stream().collect(ImmutableMap.toImmutableMap(Entry::getKey, p_378270_ -> List.copyOf(p_378270_.getValue())))
            );
        }
    }

    @OnlyIn(Dist.CLIENT)
    public record Dyeable(Optional<Integer> colorWhenUndyed) {
        public static final Codec<EquipmentClientInfo.Dyeable> CODEC = RecordCodecBuilder.create(
            p_377022_ -> p_377022_.group(ExtraCodecs.RGB_COLOR_CODEC.optionalFieldOf("color_when_undyed").forGetter(EquipmentClientInfo.Dyeable::colorWhenUndyed))
                .apply(p_377022_, EquipmentClientInfo.Dyeable::new)
        );
    }

    @OnlyIn(Dist.CLIENT)
    public record Layer(ResourceLocation textureId, Optional<EquipmentClientInfo.Dyeable> dyeable, boolean usePlayerTexture) {
        public static final Codec<EquipmentClientInfo.Layer> CODEC = RecordCodecBuilder.create(
            p_377965_ -> p_377965_.group(
                    ResourceLocation.CODEC.fieldOf("texture").forGetter(EquipmentClientInfo.Layer::textureId),
                    EquipmentClientInfo.Dyeable.CODEC.optionalFieldOf("dyeable").forGetter(EquipmentClientInfo.Layer::dyeable),
                    Codec.BOOL.optionalFieldOf("use_player_texture", false).forGetter(EquipmentClientInfo.Layer::usePlayerTexture)
                )
                .apply(p_377965_, EquipmentClientInfo.Layer::new)
        );

        public Layer(ResourceLocation pTextureId) {
            this(pTextureId, Optional.empty(), false);
        }

        public static EquipmentClientInfo.Layer leatherDyeable(ResourceLocation pTextureId, boolean pDyeable) {
            return new EquipmentClientInfo.Layer(
                pTextureId, pDyeable ? Optional.of(new EquipmentClientInfo.Dyeable(Optional.of(-6265536))) : Optional.empty(), false
            );
        }

        public static EquipmentClientInfo.Layer onlyIfDyed(ResourceLocation pTextureId, boolean pDyeable) {
            return new EquipmentClientInfo.Layer(
                pTextureId, pDyeable ? Optional.of(new EquipmentClientInfo.Dyeable(Optional.empty())) : Optional.empty(), false
            );
        }

        public ResourceLocation getTextureLocation(EquipmentClientInfo.LayerType pType) {
            return this.textureId.withPath(p_376895_ -> "textures/entity/equipment/" + pType.getSerializedName() + "/" + p_376895_ + ".png");
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static enum LayerType implements StringRepresentable {
        HUMANOID("humanoid"),
        HUMANOID_LEGGINGS("humanoid_leggings"),
        WINGS("wings"),
        WOLF_BODY("wolf_body"),
        HORSE_BODY("horse_body"),
        LLAMA_BODY("llama_body"),
        PIG_SADDLE("pig_saddle"),
        STRIDER_SADDLE("strider_saddle"),
        CAMEL_SADDLE("camel_saddle"),
        HORSE_SADDLE("horse_saddle"),
        DONKEY_SADDLE("donkey_saddle"),
        MULE_SADDLE("mule_saddle"),
        ZOMBIE_HORSE_SADDLE("zombie_horse_saddle"),
        SKELETON_HORSE_SADDLE("skeleton_horse_saddle");

        public static final Codec<EquipmentClientInfo.LayerType> CODEC = StringRepresentable.fromEnum(EquipmentClientInfo.LayerType::values);
        private final String id;

        private LayerType(final String pId) {
            this.id = pId;
        }

        @Override
        public String getSerializedName() {
            return this.id;
        }

        public String trimAssetPrefix() {
            return "trims/entity/" + this.id;
        }
    }
}