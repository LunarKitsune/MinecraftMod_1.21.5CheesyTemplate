package net.minecraft.client.renderer.entity;

import net.minecraft.client.gui.Font;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MapRenderer;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.resources.model.EquipmentAssetManager;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@FunctionalInterface
@OnlyIn(Dist.CLIENT)
public interface EntityRendererProvider<T extends Entity> {
    EntityRenderer<T, ?> create(EntityRendererProvider.Context pContext);

    @OnlyIn(Dist.CLIENT)
    public static class Context {
        private final EntityRenderDispatcher entityRenderDispatcher;
        private final ItemModelResolver itemModelResolver;
        private final MapRenderer mapRenderer;
        private final BlockRenderDispatcher blockRenderDispatcher;
        private final ResourceManager resourceManager;
        private final EntityModelSet modelSet;
        private final EquipmentAssetManager equipmentAssets;
        private final Font font;
        private final EquipmentLayerRenderer equipmentRenderer;

        public Context(
            EntityRenderDispatcher pEntityRenderDispatcher,
            ItemModelResolver pItemModelResolver,
            MapRenderer pMapRenderer,
            BlockRenderDispatcher pBlockRenderDispatcher,
            ResourceManager pResourceManager,
            EntityModelSet pModelSet,
            EquipmentAssetManager pEquipmentAssets,
            Font pFont
        ) {
            this.entityRenderDispatcher = pEntityRenderDispatcher;
            this.itemModelResolver = pItemModelResolver;
            this.mapRenderer = pMapRenderer;
            this.blockRenderDispatcher = pBlockRenderDispatcher;
            this.resourceManager = pResourceManager;
            this.modelSet = pModelSet;
            this.equipmentAssets = pEquipmentAssets;
            this.font = pFont;
            this.equipmentRenderer = new EquipmentLayerRenderer(pEquipmentAssets, this.getModelManager().getAtlas(Sheets.ARMOR_TRIMS_SHEET));
        }

        public EntityRenderDispatcher getEntityRenderDispatcher() {
            return this.entityRenderDispatcher;
        }

        public ItemModelResolver getItemModelResolver() {
            return this.itemModelResolver;
        }

        public MapRenderer getMapRenderer() {
            return this.mapRenderer;
        }

        public BlockRenderDispatcher getBlockRenderDispatcher() {
            return this.blockRenderDispatcher;
        }

        public ResourceManager getResourceManager() {
            return this.resourceManager;
        }

        public EntityModelSet getModelSet() {
            return this.modelSet;
        }

        public EquipmentAssetManager getEquipmentAssets() {
            return this.equipmentAssets;
        }

        public EquipmentLayerRenderer getEquipmentRenderer() {
            return this.equipmentRenderer;
        }

        public ModelManager getModelManager() {
            return this.blockRenderDispatcher.getBlockModelShaper().getModelManager();
        }

        public ModelPart bakeLayer(ModelLayerLocation pLayer) {
            return this.modelSet.bakeLayer(pLayer);
        }

        public Font getFont() {
            return this.font;
        }
    }
}