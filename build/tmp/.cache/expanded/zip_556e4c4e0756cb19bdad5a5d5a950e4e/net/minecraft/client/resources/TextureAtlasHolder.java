package net.minecraft.client.resources;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.Zone;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class TextureAtlasHolder implements PreparableReloadListener, AutoCloseable {
    protected final TextureAtlas textureAtlas;
    private final ResourceLocation atlasInfoLocation;
    private final Set<MetadataSectionType<?>> metadataSections;

    public TextureAtlasHolder(TextureManager pTextureManager, ResourceLocation pTextureAtlasLocation, ResourceLocation pAtlasInfoLocation) {
        this(pTextureManager, pTextureAtlasLocation, pAtlasInfoLocation, SpriteLoader.DEFAULT_METADATA_SECTIONS);
    }

    public TextureAtlasHolder(TextureManager pTextureManager, ResourceLocation pTextureAtlasLocation, ResourceLocation pAtlasInfoLocation, Set<MetadataSectionType<?>> pMetadataSections) {
        this.atlasInfoLocation = pAtlasInfoLocation;
        this.textureAtlas = new TextureAtlas(pTextureAtlasLocation);
        pTextureManager.register(this.textureAtlas.location(), this.textureAtlas);
        this.metadataSections = pMetadataSections;
    }

    protected TextureAtlasSprite getSprite(ResourceLocation pLocation) {
        return this.textureAtlas.getSprite(pLocation);
    }

    @Override
    public final CompletableFuture<Void> reload(
        PreparableReloadListener.PreparationBarrier p_249641_, ResourceManager p_250036_, Executor p_249427_, Executor p_250510_
    ) {
        return SpriteLoader.create(this.textureAtlas)
            .loadAndStitch(p_250036_, this.atlasInfoLocation, 0, p_249427_, this.metadataSections)
            .thenCompose(SpriteLoader.Preparations::waitForUpload)
            .thenCompose(p_249641_::wait)
            .thenAcceptAsync(this::apply, p_250510_);
    }

    private void apply(SpriteLoader.Preparations pPreperations) {
        try (Zone zone = Profiler.get().zone("upload")) {
            this.textureAtlas.upload(pPreperations);
        }
    }

    @Override
    public void close() {
        this.textureAtlas.clearTextureData();
    }
}