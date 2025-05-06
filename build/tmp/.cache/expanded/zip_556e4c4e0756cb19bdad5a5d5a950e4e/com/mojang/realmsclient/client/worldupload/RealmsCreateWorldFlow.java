package com.mojang.realmsclient.client.worldupload;

import com.mojang.logging.LogUtils;
import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.RealmsWorldOptions;
import com.mojang.realmsclient.gui.screens.RealmsConfigureWorldScreen;
import com.mojang.realmsclient.gui.screens.RealmsGenericErrorScreen;
import com.mojang.realmsclient.util.task.RealmCreationTask;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletionException;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.RegistryLayer;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class RealmsCreateWorldFlow {
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void createWorld(
        Minecraft pMinecraft, Screen pLastScreen, Screen pResetWorldScreen, int pSlot, RealmsServer pServer, @Nullable RealmCreationTask pRealmCreationTask
    ) {
        CreateWorldScreen.openFresh(
            pMinecraft,
            pLastScreen,
            (p_389101_, p_389102_, p_389103_, p_389104_) -> {
                Path path;
                try {
                    path = createTemporaryWorldFolder(p_389102_, p_389103_, p_389104_);
                } catch (IOException ioexception) {
                    LOGGER.warn("Failed to create temporary world folder.");
                    pMinecraft.setScreen(new RealmsGenericErrorScreen(Component.translatable("mco.create.world.failed"), pResetWorldScreen));
                    return true;
                }

                RealmsWorldOptions realmsworldoptions = RealmsWorldOptions.createFromSettings(
                    p_389103_.getLevelSettings(), p_389103_.getLevelSettings().allowCommands(), SharedConstants.getCurrentVersion().getName()
                );
                RealmsWorldUpload realmsworldupload = new RealmsWorldUpload(
                    path, realmsworldoptions, pMinecraft.getUser(), pServer.id, pSlot, RealmsWorldUploadStatusTracker.noOp()
                );
                pMinecraft.forceSetScreen(
                    new AlertScreen(
                        realmsworldupload::cancel,
                        Component.translatable("mco.create.world.reset.title"),
                        Component.empty(),
                        CommonComponents.GUI_CANCEL,
                        false
                    )
                );
                if (pRealmCreationTask != null) {
                    pRealmCreationTask.run();
                }

                realmsworldupload.packAndUpload().handleAsync((p_366683_, p_363012_) -> {
                    if (p_363012_ != null) {
                        if (p_363012_ instanceof CompletionException completionexception) {
                            p_363012_ = completionexception.getCause();
                        }

                        if (p_363012_ instanceof RealmsUploadCanceledException) {
                            pMinecraft.forceSetScreen(pResetWorldScreen);
                        } else {
                            if (p_363012_ instanceof RealmsUploadFailedException realmsuploadfailedexception) {
                                LOGGER.warn("Failed to create realms world {}", realmsuploadfailedexception.getStatusMessage());
                            } else {
                                LOGGER.warn("Failed to create realms world {}", p_363012_.getMessage());
                            }

                            pMinecraft.forceSetScreen(new RealmsGenericErrorScreen(Component.translatable("mco.create.world.failed"), pResetWorldScreen));
                        }
                    } else {
                        if (pLastScreen instanceof RealmsConfigureWorldScreen realmsconfigureworldscreen) {
                            realmsconfigureworldscreen.fetchServerData(pServer.id);
                        }

                        if (pRealmCreationTask != null) {
                            RealmsMainScreen.play(pServer, pLastScreen, true);
                        } else {
                            pMinecraft.forceSetScreen(pLastScreen);
                        }

                        RealmsMainScreen.refreshServerList();
                    }

                    return null;
                }, pMinecraft);
                return true;
            }
        );
    }

    private static Path createTemporaryWorldFolder(LayeredRegistryAccess<RegistryLayer> pRegistryAccess, PrimaryLevelData pLevelData, @Nullable Path pTempDatapackDir) throws IOException {
        Path path = Files.createTempDirectory("minecraft_realms_world_upload");
        if (pTempDatapackDir != null) {
            Files.move(pTempDatapackDir, path.resolve("datapacks"));
        }

        CompoundTag compoundtag = pLevelData.createTag(pRegistryAccess.compositeAccess(), null);
        CompoundTag compoundtag1 = new CompoundTag();
        compoundtag1.put("Data", compoundtag);
        Path path1 = Files.createFile(path.resolve("level.dat"));
        NbtIo.writeCompressed(compoundtag1, path1);
        return path;
    }
}