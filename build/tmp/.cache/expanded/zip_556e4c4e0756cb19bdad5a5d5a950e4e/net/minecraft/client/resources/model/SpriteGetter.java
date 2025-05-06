package net.minecraft.client.resources.model;

import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface SpriteGetter {
    TextureAtlasSprite get(Material pMaterial, ModelDebugName pDebugName);

    TextureAtlasSprite reportMissingReference(String pName, ModelDebugName pDebugName);

    default TextureAtlasSprite resolveSlot(TextureSlots pTextureSlots, String pName, ModelDebugName pModelDebugName) {
        Material material = pTextureSlots.getMaterial(pName);
        return material != null ? this.get(material, pModelDebugName) : this.reportMissingReference(pName, pModelDebugName);
    }
}