package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.gui.screens.AddRealmPopupScreen;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class TextureManager implements PreparableReloadListener, Tickable, AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final ResourceLocation INTENTIONAL_MISSING_TEXTURE = ResourceLocation.withDefaultNamespace("");
    private final Map<ResourceLocation, AbstractTexture> byPath = new HashMap<>();
    private final Set<Tickable> tickableTextures = new HashSet<>();
    private final ResourceManager resourceManager;

    public TextureManager(ResourceManager pResourceManager) {
        this.resourceManager = pResourceManager;
        NativeImage nativeimage = MissingTextureAtlasSprite.generateMissingImage();
        this.register(MissingTextureAtlasSprite.getLocation(), new DynamicTexture(() -> "(intentionally-)Missing Texture", nativeimage));
    }

    public void registerAndLoad(ResourceLocation pTextureId, ReloadableTexture pTexture) {
        try {
            pTexture.apply(this.loadContentsSafe(pTextureId, pTexture));
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Uploading texture");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Uploaded texture");
            crashreportcategory.setDetail("Resource location", pTexture.resourceId());
            crashreportcategory.setDetail("Texture id", pTextureId);
            throw new ReportedException(crashreport);
        }

        this.register(pTextureId, pTexture);
    }

    private TextureContents loadContentsSafe(ResourceLocation pTextureId, ReloadableTexture pTexture) {
        try {
            return loadContents(this.resourceManager, pTextureId, pTexture);
        } catch (Exception exception) {
            LOGGER.error("Failed to load texture {} into slot {}", pTexture.resourceId(), pTextureId, exception);
            return TextureContents.createMissing();
        }
    }

    public void registerForNextReload(ResourceLocation pTextureId) {
        this.register(pTextureId, new SimpleTexture(pTextureId));
    }

    public void register(ResourceLocation pPath, AbstractTexture pTexture) {
        AbstractTexture abstracttexture = this.byPath.put(pPath, pTexture);
        if (abstracttexture != pTexture) {
            if (abstracttexture != null) {
                this.safeClose(pPath, abstracttexture);
            }

            if (pTexture instanceof Tickable tickable) {
                this.tickableTextures.add(tickable);
            }
        }
    }

    private void safeClose(ResourceLocation pPath, AbstractTexture pTexture) {
        this.tickableTextures.remove(pTexture);

        try {
            pTexture.close();
        } catch (Exception exception) {
            LOGGER.warn("Failed to close texture {}", pPath, exception);
        }
    }

    public AbstractTexture getTexture(ResourceLocation pPath) {
        AbstractTexture abstracttexture = this.byPath.get(pPath);
        if (abstracttexture != null) {
            return abstracttexture;
        } else {
            SimpleTexture simpletexture = new SimpleTexture(pPath);
            this.registerAndLoad(pPath, simpletexture);
            return simpletexture;
        }
    }

    @Override
    public void tick() {
        for (Tickable tickable : this.tickableTextures) {
            tickable.tick();
        }
    }

    public void release(ResourceLocation pPath) {
        AbstractTexture abstracttexture = this.byPath.remove(pPath);
        if (abstracttexture != null) {
            this.safeClose(pPath, abstracttexture);
        }
    }

    @Override
    public void close() {
        this.byPath.forEach(this::safeClose);
        this.byPath.clear();
        this.tickableTextures.clear();
    }

    @Override
    public CompletableFuture<Void> reload(
        PreparableReloadListener.PreparationBarrier p_118476_, ResourceManager p_118477_, Executor p_118480_, Executor p_118481_
    ) {
        List<TextureManager.PendingReload> list = new ArrayList<>();
        this.byPath.forEach((p_374670_, p_374671_) -> {
            if (p_374671_ instanceof ReloadableTexture reloadabletexture) {
                list.add(scheduleLoad(p_118477_, p_374670_, reloadabletexture, p_118480_));
            }
        });
        return CompletableFuture.allOf(list.stream().map(TextureManager.PendingReload::newContents).toArray(CompletableFuture[]::new))
            .thenCompose(p_118476_::wait)
            .thenAcceptAsync(p_374677_ -> {
                AddRealmPopupScreen.updateCarouselImages(this.resourceManager);

                for (TextureManager.PendingReload texturemanager$pendingreload : list) {
                    texturemanager$pendingreload.texture.apply(texturemanager$pendingreload.newContents.join());
                }
            }, p_118481_);
    }

    public void dumpAllSheets(Path pPath) {
        try {
            Files.createDirectories(pPath);
        } catch (IOException ioexception) {
            LOGGER.error("Failed to create directory {}", pPath, ioexception);
            return;
        }

        this.byPath.forEach((p_276101_, p_276102_) -> {
            if (p_276102_ instanceof Dumpable dumpable) {
                try {
                    dumpable.dumpContents(p_276101_, pPath);
                } catch (IOException ioexception1) {
                    LOGGER.error("Failed to dump texture {}", p_276101_, ioexception1);
                }
            }
        });
    }

    private static TextureContents loadContents(ResourceManager pResourceManager, ResourceLocation pTextureId, ReloadableTexture pTexture) throws IOException {
        try {
            return pTexture.loadContents(pResourceManager);
        } catch (FileNotFoundException filenotfoundexception) {
            if (pTextureId != INTENTIONAL_MISSING_TEXTURE) {
                LOGGER.warn("Missing resource {} referenced from {}", pTexture.resourceId(), pTextureId);
            }

            return TextureContents.createMissing();
        }
    }

    private static TextureManager.PendingReload scheduleLoad(
        ResourceManager pResourceManager, ResourceLocation pTextureId, ReloadableTexture pTexture, Executor pExecutor
    ) {
        return new TextureManager.PendingReload(pTexture, CompletableFuture.supplyAsync(() -> {
            try {
                return loadContents(pResourceManager, pTextureId, pTexture);
            } catch (IOException ioexception) {
                throw new UncheckedIOException(ioexception);
            }
        }, pExecutor));
    }

    @OnlyIn(Dist.CLIENT)
    record PendingReload(ReloadableTexture texture, CompletableFuture<TextureContents> newContents) {
    }
}