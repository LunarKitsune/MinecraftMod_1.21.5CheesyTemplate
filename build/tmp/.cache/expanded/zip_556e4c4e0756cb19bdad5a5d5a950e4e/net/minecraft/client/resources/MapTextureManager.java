package net.minecraft.client.resources;

import com.mojang.blaze3d.platform.NativeImage;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MapTextureManager implements AutoCloseable {
    private final Int2ObjectMap<MapTextureManager.MapInstance> maps = new Int2ObjectOpenHashMap<>();
    final TextureManager textureManager;

    public MapTextureManager(TextureManager pTextureManager) {
        this.textureManager = pTextureManager;
    }

    public void update(MapId pId, MapItemSavedData pData) {
        this.getOrCreateMapInstance(pId, pData).forceUpload();
    }

    public ResourceLocation prepareMapTexture(MapId pId, MapItemSavedData pData) {
        MapTextureManager.MapInstance maptexturemanager$mapinstance = this.getOrCreateMapInstance(pId, pData);
        maptexturemanager$mapinstance.updateTextureIfNeeded();
        return maptexturemanager$mapinstance.location;
    }

    public void resetData() {
        for (MapTextureManager.MapInstance maptexturemanager$mapinstance : this.maps.values()) {
            maptexturemanager$mapinstance.close();
        }

        this.maps.clear();
    }

    private MapTextureManager.MapInstance getOrCreateMapInstance(MapId pId, MapItemSavedData pData) {
        return this.maps.compute(pId.id(), (p_366926_, p_369937_) -> {
            if (p_369937_ == null) {
                return new MapTextureManager.MapInstance(p_366926_, pData);
            } else {
                p_369937_.replaceMapData(pData);
                return (MapTextureManager.MapInstance)p_369937_;
            }
        });
    }

    @Override
    public void close() {
        this.resetData();
    }

    @OnlyIn(Dist.CLIENT)
    class MapInstance implements AutoCloseable {
        private MapItemSavedData data;
        private final DynamicTexture texture;
        private boolean requiresUpload = true;
        final ResourceLocation location;

        MapInstance(final int pId, final MapItemSavedData pData) {
            this.data = pData;
            this.texture = new DynamicTexture(() -> "Map " + pId, 128, 128, true);
            this.location = ResourceLocation.withDefaultNamespace("map/" + pId);
            MapTextureManager.this.textureManager.register(this.location, this.texture);
        }

        void replaceMapData(MapItemSavedData pData) {
            boolean flag = this.data != pData;
            this.data = pData;
            this.requiresUpload |= flag;
        }

        public void forceUpload() {
            this.requiresUpload = true;
        }

        void updateTextureIfNeeded() {
            if (this.requiresUpload) {
                NativeImage nativeimage = this.texture.getPixels();
                if (nativeimage != null) {
                    for (int i = 0; i < 128; i++) {
                        for (int j = 0; j < 128; j++) {
                            int k = j + i * 128;
                            nativeimage.setPixel(j, i, MapColor.getColorFromPackedId(this.data.colors[k]));
                        }
                    }
                }

                this.texture.upload();
                this.requiresUpload = false;
            }
        }

        @Override
        public void close() {
            this.texture.close();
        }
    }
}