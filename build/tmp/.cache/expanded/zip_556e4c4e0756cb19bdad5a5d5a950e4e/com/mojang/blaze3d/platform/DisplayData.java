package com.mojang.blaze3d.platform;

import java.util.OptionalInt;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record DisplayData(int width, int height, OptionalInt fullscreenWidth, OptionalInt fullscreenHeight, boolean isFullscreen) {
    public DisplayData withSize(int pWidth, int pHeight) {
        return new DisplayData(pWidth, pHeight, this.fullscreenWidth, this.fullscreenHeight, this.isFullscreen);
    }

    public DisplayData withFullscreen(boolean pFullscreen) {
        return new DisplayData(this.width, this.height, this.fullscreenWidth, this.fullscreenHeight, pFullscreen);
    }
}