/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.minecraftforge.client.loading;

import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.util.Mth;
import net.minecraftforge.client.ForgeRenderTypes;
import net.minecraftforge.fml.StartupMessageManager;
import net.minecraftforge.fml.earlydisplay.DisplayWindow;
import net.minecraftforge.fml.loading.progress.ProgressMeter;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL30C;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * This is an implementation of the LoadingOverlay that calls back into the early window rendering, as part of the
 * game loading cycle. We completely replace the {@link #render(GuiGraphics, int, int, float)} call from the parent
 * with one of our own, that allows us to blend our early loading screen into the main window, in the same manner as
 * the Mojang screen. It also allows us to see and tick appropriately as the later stages of the loading system run.
 *
 * It is somewhat a copy of the superclass render method.
 */
public class ForgeLoadingOverlay extends LoadingOverlay {
    private static final boolean ENABLE = Boolean.parseBoolean("forge.enableForgeLoadingOverlay");
    private final Minecraft minecraft;
    private final ReloadInstance reload;
    private final DisplayWindow displayWindow;
    private final ProgressMeter progress;
    private final RenderType earlyBuffer;

    public ForgeLoadingOverlay(final Minecraft mc, final ReloadInstance reloader, final Consumer<Optional<Throwable>> errorConsumer, DisplayWindow displayWindow) {
        super(mc, reloader, errorConsumer, false);
        this.minecraft = mc;
        this.reload = reloader;
        this.displayWindow = displayWindow;
        var texture = mc.getTextureManager().getTexture(MOJANG_STUDIOS_LOGO_LOCATION);
        var glTexture = (GlTexture)texture.getTexture();
        displayWindow.addMojangTexture(glTexture.glId());
        this.progress = StartupMessageManager.prependProgressBar("Minecraft Progress", 100);
        this.earlyBuffer = ForgeRenderTypes.getLoadingOverlay(displayWindow);
    }

    public static Supplier<LoadingOverlay> newInstance(Supplier<Minecraft> mc, Supplier<ReloadInstance> ri, Consumer<Optional<Throwable>> handler, DisplayWindow window) {
        return ()->new ForgeLoadingOverlay(mc.get(), ri.get(), handler, window);
    }

    @Override
    protected boolean renderContents(GuiGraphics gui, float fade) {
        if (!ENABLE)
            return true;
        progress.setAbsolute(Mth.clamp((int)(this.reload.getActualProgress() * 100f), 0, 100));

        int alpha = (int)(fade * 255);
        this.displayWindow.render(alpha);

        int width = gui.guiWidth();
        int height = gui.guiHeight();

        var fbWidth = this.minecraft.getWindow().getWidth();
        var fbHeight = this.minecraft.getWindow().getHeight();
        GL30C.glViewport(0, 0, fbWidth, fbHeight);

        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, fade);
        Matrix4f pos = gui.pose().last().pose();
        /*
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlConst.GL_SRC_ALPHA, GlConst.GL_ONE_MINUS_SRC_ALPHA);
        RenderSystem.setShader(CoreShaders.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, displayWindow.getFramebufferTextureId());
        GL30C.glTexParameterIi(GlConst.GL_TEXTURE_2D, GlConst.GL_TEXTURE_MIN_FILTER, GlConst.GL_NEAREST);
        GL30C.glTexParameterIi(GlConst.GL_TEXTURE_2D, GlConst.GL_TEXTURE_MAG_FILTER, GlConst.GL_NEAREST);
        */

        var buf = gui.getBufferSource().getBuffer(earlyBuffer);
        buf.addVertex(pos, 0,     0,      0f).setUv(0, 0).setColor(1f, 1f, 1f, fade);
        buf.addVertex(pos, 0,     height, 0f).setUv(0, 1).setColor(1f, 1f, 1f, fade);
        buf.addVertex(pos, width, height, 0f).setUv(1, 1).setColor(1f, 1f, 1f, fade);
        buf.addVertex(pos, width, 0,      0f).setUv(1, 0).setColor(1f, 1f, 1f, fade);
        /*
        BufferUploader.drawWithShader(buf.buildOrThrow());

        // I dont know what exactly this does, but without it the screen flickers black.
        // So as a hack we just render the mojang logo as a 0x0 cube
        // TODO: Remove this when early screen is re-written to not be a texture based renderer
        var logo = gui.getBufferSource().getBuffer(RenderType.mojangLogo());
        logo.addVertex(pos, 0, 0, 0f).setUv(0, 0).setColor(1f, 1f, 1f, fade);
        logo.addVertex(pos, 0, 0, 0f).setUv(0, 1).setColor(1f, 1f, 1f, fade);
        logo.addVertex(pos, 0, 0, 0f).setUv(1, 1).setColor(1f, 1f, 1f, fade);
        logo.addVertex(pos, 0, 0, 0f).setUv(1, 0).setColor(1f, 1f, 1f, fade);


        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        */
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1f);

        return false;
    }
}
