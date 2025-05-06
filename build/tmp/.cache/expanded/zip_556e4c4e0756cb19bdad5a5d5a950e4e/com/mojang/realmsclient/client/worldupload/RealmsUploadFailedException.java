package com.mojang.realmsclient.client.worldupload;

import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RealmsUploadFailedException extends RealmsUploadException {
    private final Component errorMessage;

    public RealmsUploadFailedException(Component pErrorMessage) {
        this.errorMessage = pErrorMessage;
    }

    public RealmsUploadFailedException(String pErrorMessage) {
        this(Component.literal(pErrorMessage));
    }

    @Override
    public Component getStatusMessage() {
        return Component.translatable("mco.upload.failed", this.errorMessage);
    }
}