package net.minecraft.client.resources.model;

import javax.annotation.Nullable;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface UnbakedModel {
    String PARTICLE_TEXTURE_REFERENCE = "particle";

    @Nullable
    default Boolean ambientOcclusion() {
        return null;
    }

    @Nullable
    default UnbakedModel.GuiLight guiLight() {
        return null;
    }

    @Nullable
    default ItemTransforms transforms() {
        return null;
    }

    default TextureSlots.Data textureSlots() {
        return TextureSlots.Data.EMPTY;
    }

    @Nullable
    default UnbakedGeometry geometry() {
        return null;
    }

    @Nullable
    default ResourceLocation parent() {
        return null;
    }

    @OnlyIn(Dist.CLIENT)
    public static enum GuiLight {
        FRONT("front"),
        SIDE("side");

        private final String name;

        private GuiLight(final String pName) {
            this.name = pName;
        }

        public static UnbakedModel.GuiLight getByName(String pName) {
            for (UnbakedModel.GuiLight unbakedmodel$guilight : values()) {
                if (unbakedmodel$guilight.name.equals(pName)) {
                    return unbakedmodel$guilight;
                }
            }

            throw new IllegalArgumentException("Invalid gui light: " + pName);
        }

        public boolean lightLikeBlock() {
            return this == SIDE;
        }

        public String getSerializedName() {
            return name;
        }
    }
}
