package net.minecraft.client.gui;

import java.util.Set;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.TextureAtlasHolder;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.gui.GuiMetadataSection;
import net.minecraft.client.resources.metadata.gui.GuiSpriteScaling;
import net.minecraft.client.resources.model.AtlasIds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GuiSpriteManager extends TextureAtlasHolder {
    private static final Set<MetadataSectionType<?>> METADATA_SECTIONS = Set.of(AnimationMetadataSection.TYPE, GuiMetadataSection.TYPE);

    public GuiSpriteManager(TextureManager pTextureManager) {
        super(pTextureManager, ResourceLocation.withDefaultNamespace("textures/atlas/gui.png"), AtlasIds.GUI, METADATA_SECTIONS);
    }

    @Override
    public TextureAtlasSprite getSprite(ResourceLocation p_298308_) {
        return super.getSprite(p_298308_);
    }

    public GuiSpriteScaling getSpriteScaling(TextureAtlasSprite pSprite) {
        return this.getMetadata(pSprite).scaling();
    }

    private GuiMetadataSection getMetadata(TextureAtlasSprite pSprite) {
        return pSprite.contents().metadata().getSection(GuiMetadataSection.TYPE).orElse(GuiMetadataSection.DEFAULT);
    }
}