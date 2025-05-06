package com.mojang.blaze3d.platform;

import com.mojang.blaze3d.TracyFrameCapture;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.main.SilentInitException;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWWindowCloseCallback;
import org.lwjgl.glfw.GLFWImage.Buffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public final class Window implements AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final int BASE_WIDTH = 320;
    public static final int BASE_HEIGHT = 240;
    private final GLFWErrorCallback defaultErrorCallback = GLFWErrorCallback.create(this::defaultErrorCallback);
    private final WindowEventHandler eventHandler;
    private final ScreenManager screenManager;
    private final long window;
    private int windowedX;
    private int windowedY;
    private int windowedWidth;
    private int windowedHeight;
    private Optional<VideoMode> preferredFullscreenVideoMode;
    private boolean fullscreen;
    private boolean actuallyFullscreen;
    private int x;
    private int y;
    private int width;
    private int height;
    private int framebufferWidth;
    private int framebufferHeight;
    private int guiScaledWidth;
    private int guiScaledHeight;
    private double guiScale;
    private String errorSection = "";
    private boolean dirty;
    private boolean vsync;
    private boolean iconified;
    private boolean minimized;

    public Window(WindowEventHandler pEventHandler, ScreenManager pScreenManager, DisplayData pDisplayData, @Nullable String pPreferredFullscreenVideoMode, String pTitle) {
        this.screenManager = pScreenManager;
        this.setBootErrorCallback();
        this.setErrorSection("Pre startup");
        this.eventHandler = pEventHandler;
        Optional<VideoMode> optional = VideoMode.read(pPreferredFullscreenVideoMode);
        if (optional.isPresent()) {
            this.preferredFullscreenVideoMode = optional;
        } else if (pDisplayData.fullscreenWidth().isPresent() && pDisplayData.fullscreenHeight().isPresent()) {
            this.preferredFullscreenVideoMode = Optional.of(new VideoMode(pDisplayData.fullscreenWidth().getAsInt(), pDisplayData.fullscreenHeight().getAsInt(), 8, 8, 8, 60));
        } else {
            this.preferredFullscreenVideoMode = Optional.empty();
        }

        this.actuallyFullscreen = this.fullscreen = pDisplayData.isFullscreen();
        Monitor monitor = pScreenManager.getMonitor(GLFW.glfwGetPrimaryMonitor());
        this.windowedWidth = this.width = Math.max(pDisplayData.width(), 1);
        this.windowedHeight = this.height = Math.max(pDisplayData.height(), 1);
        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(139265, 196609);
        GLFW.glfwWindowHint(139275, 221185);
        GLFW.glfwWindowHint(139266, 3);
        GLFW.glfwWindowHint(139267, 2);
        GLFW.glfwWindowHint(139272, 204801);
        GLFW.glfwWindowHint(139270, 1);
        this.window = net.minecraftforge.fml.loading.ImmediateWindowHandler.setupMinecraftWindow(()->this.width, ()->this.height, ()->pTitle, ()->this.fullscreen && monitor != null ? monitor.getMonitor() : 0L);
        if (!net.minecraftforge.fml.loading.ImmediateWindowHandler.positionWindow(Optional.ofNullable(monitor), w->this.width = this.windowedWidth = w, h->this.height = this.windowedHeight = h, x->this.x = this.windowedX = x, y->this.y = this.windowedY = y)) {
        if (monitor != null) {
            VideoMode videomode = monitor.getPreferredVidMode(this.fullscreen ? this.preferredFullscreenVideoMode : Optional.empty());
            this.windowedX = this.x = monitor.getX() + videomode.getWidth() / 2 - this.width / 2;
            this.windowedY = this.y = monitor.getY() + videomode.getHeight() / 2 - this.height / 2;
        } else {
            int[] aint1 = new int[1];
            int[] aint = new int[1];
            GLFW.glfwGetWindowPos(this.window, aint1, aint);
            this.windowedX = this.x = aint1[0];
            this.windowedY = this.y = aint[0];
        }
        }

        this.setMode();
        this.refreshFramebufferSize();
        GLFW.glfwSetFramebufferSizeCallback(this.window, this::onFramebufferResize);
        GLFW.glfwSetWindowPosCallback(this.window, this::onMove);
        GLFW.glfwSetWindowSizeCallback(this.window, this::onResize);
        GLFW.glfwSetWindowFocusCallback(this.window, this::onFocus);
        GLFW.glfwSetCursorEnterCallback(this.window, this::onEnter);
        GLFW.glfwSetWindowIconifyCallback(this.window, this::onIconify);
    }

    public static String getPlatform() {
        int i = GLFW.glfwGetPlatform();

        return switch (i) {
            case 0 -> "<error>";
            case 393217 -> "win32";
            case 393218 -> "cocoa";
            case 393219 -> "wayland";
            case 393220 -> "x11";
            case 393221 -> "null";
            default -> String.format(Locale.ROOT, "unknown (%08X)", i);
        };
    }

    public int getRefreshRate() {
        RenderSystem.assertOnRenderThread();
        return GLX._getRefreshRate(this);
    }

    public boolean shouldClose() {
        return GLX._shouldClose(this);
    }

    public static void checkGlfwError(BiConsumer<Integer, String> pErrorConsumer) {
        try (MemoryStack memorystack = MemoryStack.stackPush()) {
            PointerBuffer pointerbuffer = memorystack.mallocPointer(1);
            int i = GLFW.glfwGetError(pointerbuffer);
            if (i != 0) {
                long j = pointerbuffer.get();
                String s = j == 0L ? "" : MemoryUtil.memUTF8(j);
                pErrorConsumer.accept(i, s);
            }
        }
    }

    public void setIcon(PackResources pPackResources, IconSet pIconSet) throws IOException {
        int i = GLFW.glfwGetPlatform();
        switch (i) {
            case 393217:
            case 393220:
                List<IoSupplier<InputStream>> list = pIconSet.getStandardIcons(pPackResources);
                List<ByteBuffer> list1 = new ArrayList<>(list.size());

                try (MemoryStack memorystack = MemoryStack.stackPush()) {
                    Buffer buffer = GLFWImage.malloc(list.size(), memorystack);

                    for (int j = 0; j < list.size(); j++) {
                        try (NativeImage nativeimage = NativeImage.read(list.get(j).get())) {
                            ByteBuffer bytebuffer = MemoryUtil.memAlloc(nativeimage.getWidth() * nativeimage.getHeight() * 4);
                            list1.add(bytebuffer);
                            bytebuffer.asIntBuffer().put(nativeimage.getPixelsABGR());
                            buffer.position(j);
                            buffer.width(nativeimage.getWidth());
                            buffer.height(nativeimage.getHeight());
                            buffer.pixels(bytebuffer);
                        }
                    }

                    GLFW.glfwSetWindowIcon(this.window, buffer.position(0));
                    break;
                } finally {
                    list1.forEach(MemoryUtil::memFree);
                }
            case 393218:
                MacosUtil.loadIcon(pIconSet.getMacIcon(pPackResources));
            case 393219:
            case 393221:
                break;
            default:
                LOGGER.warn("Not setting icon for unrecognized platform: {}", i);
        }
    }

    public void setErrorSection(String pErrorSection) {
        this.errorSection = pErrorSection;
    }

    private void setBootErrorCallback() {
        GLFW.glfwSetErrorCallback(Window::bootCrash);
    }

    private static void bootCrash(int pError, long pDescription) {
        String s = "GLFW error " + pError + ": " + MemoryUtil.memUTF8(pDescription);
        TinyFileDialogs.tinyfd_messageBox(
            "Minecraft", s + ".\n\nPlease make sure you have up-to-date drivers (see aka.ms/mcdriver for instructions).", "ok", "error", false
        );
        throw new Window.WindowInitFailed(s);
    }

    public void defaultErrorCallback(int pError, long pDescription) {
        RenderSystem.assertOnRenderThread();
        String s = MemoryUtil.memUTF8(pDescription);
        LOGGER.error("########## GL ERROR ##########");
        LOGGER.error("@ {}", this.errorSection);
        LOGGER.error("{}: {}", pError, s);
    }

    public void setDefaultErrorCallback() {
        GLFWErrorCallback glfwerrorcallback = GLFW.glfwSetErrorCallback(this.defaultErrorCallback);
        if (glfwerrorcallback != null) {
            glfwerrorcallback.free();
        }
    }

    public void updateVsync(boolean pVsync) {
        RenderSystem.assertOnRenderThread();
        this.vsync = pVsync;
        GLFW.glfwSwapInterval(pVsync ? 1 : 0);
    }

    @Override
    public void close() {
        RenderSystem.assertOnRenderThread();
        Callbacks.glfwFreeCallbacks(this.window);
        this.defaultErrorCallback.close();
        GLFW.glfwDestroyWindow(this.window);
        GLFW.glfwTerminate();
    }

    private void onMove(long pWindow, int pX, int pY) {
        this.x = pX;
        this.y = pY;
    }

    private void onFramebufferResize(long pWindow, int pFramebufferWidth, int pFramebufferHeight) {
        if (pWindow == this.window) {
            int i = this.getWidth();
            int j = this.getHeight();
            if (pFramebufferWidth != 0 && pFramebufferHeight != 0) {
                this.minimized = false;
                this.framebufferWidth = pFramebufferWidth;
                this.framebufferHeight = pFramebufferHeight;
                if (this.getWidth() != i || this.getHeight() != j) {
                    try {
                        this.eventHandler.resizeDisplay();
                    } catch (Exception exception) {
                        CrashReport crashreport = CrashReport.forThrowable(exception, "Window resize");
                        CrashReportCategory crashreportcategory = crashreport.addCategory("Window Dimensions");
                        crashreportcategory.setDetail("Old", i + "x" + j);
                        crashreportcategory.setDetail("New", pFramebufferWidth + "x" + pFramebufferHeight);
                        throw new ReportedException(crashreport);
                    }
                }
            } else {
                this.minimized = true;
            }
        }
    }

    private void refreshFramebufferSize() {
        int[] aint = new int[1];
        int[] aint1 = new int[1];
        GLFW.glfwGetFramebufferSize(this.window, aint, aint1);
        this.framebufferWidth = aint[0] > 0 ? aint[0] : 1;
        this.framebufferHeight = aint1[0] > 0 ? aint1[0] : 1;
        if (this.framebufferHeight == 0 || this.framebufferWidth == 0) {
            net.minecraftforge.fml.loading.ImmediateWindowHandler.updateFBSize(w -> this.framebufferWidth = w, h -> this.framebufferHeight = h);
        }
    }

    private void onResize(long pWindow, int pWidth, int pHeight) {
        this.width = pWidth;
        this.height = pHeight;
    }

    private void onFocus(long pWindow, boolean pHasFocus) {
        if (pWindow == this.window) {
            this.eventHandler.setWindowActive(pHasFocus);
        }
    }

    private void onEnter(long pWindow, boolean pCursorEntered) {
        if (pCursorEntered) {
            this.eventHandler.cursorEntered();
        }
    }

    private void onIconify(long pWindow, boolean pIconified) {
        this.iconified = pIconified;
    }

    public void updateDisplay(@Nullable TracyFrameCapture pTracyFrameCapture) {
        RenderSystem.flipFrame(this.window, pTracyFrameCapture);
        if (this.fullscreen != this.actuallyFullscreen) {
            this.actuallyFullscreen = this.fullscreen;
            this.updateFullscreen(this.vsync, pTracyFrameCapture);
        }
    }

    public Optional<VideoMode> getPreferredFullscreenVideoMode() {
        return this.preferredFullscreenVideoMode;
    }

    public void setPreferredFullscreenVideoMode(Optional<VideoMode> pPreferredFullscreenVideoMode) {
        boolean flag = !pPreferredFullscreenVideoMode.equals(this.preferredFullscreenVideoMode);
        this.preferredFullscreenVideoMode = pPreferredFullscreenVideoMode;
        if (flag) {
            this.dirty = true;
        }
    }

    public void changeFullscreenVideoMode() {
        if (this.fullscreen && this.dirty) {
            this.dirty = false;
            this.setMode();
            this.eventHandler.resizeDisplay();
        }
    }

    private void setMode() {
        boolean flag = GLFW.glfwGetWindowMonitor(this.window) != 0L;
        if (this.fullscreen) {
            Monitor monitor = this.screenManager.findBestMonitor(this);
            if (monitor == null) {
                LOGGER.warn("Failed to find suitable monitor for fullscreen mode");
                this.fullscreen = false;
            } else {
                if (MacosUtil.IS_MACOS) {
                    MacosUtil.exitNativeFullscreen(this.window);
                }

                VideoMode videomode = monitor.getPreferredVidMode(this.preferredFullscreenVideoMode);
                if (!flag) {
                    this.windowedX = this.x;
                    this.windowedY = this.y;
                    this.windowedWidth = this.width;
                    this.windowedHeight = this.height;
                }

                this.x = 0;
                this.y = 0;
                this.width = videomode.getWidth();
                this.height = videomode.getHeight();
                GLFW.glfwSetWindowMonitor(this.window, monitor.getMonitor(), this.x, this.y, this.width, this.height, videomode.getRefreshRate());
                if (MacosUtil.IS_MACOS) {
                    MacosUtil.clearResizableBit(this.window);
                }
            }
        } else {
            this.x = this.windowedX;
            this.y = this.windowedY;
            this.width = this.windowedWidth;
            this.height = this.windowedHeight;
            GLFW.glfwSetWindowMonitor(this.window, 0L, this.x, this.y, this.width, this.height, -1);
        }
    }

    public void toggleFullScreen() {
        this.fullscreen = !this.fullscreen;
    }

    public void setWindowed(int pWindowedWidth, int pWindowedHeight) {
        this.windowedWidth = pWindowedWidth;
        this.windowedHeight = pWindowedHeight;
        this.fullscreen = false;
        this.setMode();
    }

    private void updateFullscreen(boolean pVsyncEnabled, @Nullable TracyFrameCapture pTracyFrameCapture) {
        RenderSystem.assertOnRenderThread();

        try {
            this.setMode();
            this.eventHandler.resizeDisplay();
            this.updateVsync(pVsyncEnabled);
            this.updateDisplay(pTracyFrameCapture);
        } catch (Exception exception) {
            LOGGER.error("Couldn't toggle fullscreen", (Throwable)exception);
        }
    }

    public int calculateScale(int pGuiScale, boolean pForceUnicode) {
        int i = 1;

        while (i != pGuiScale && i < this.framebufferWidth && i < this.framebufferHeight && this.framebufferWidth / (i + 1) >= 320 && this.framebufferHeight / (i + 1) >= 240) {
            i++;
        }

        if (pForceUnicode && i % 2 != 0) {
            i++;
        }

        return i;
    }

    public void setGuiScale(double pScaleFactor) {
        this.guiScale = pScaleFactor;
        int i = (int)(this.framebufferWidth / pScaleFactor);
        this.guiScaledWidth = this.framebufferWidth / pScaleFactor > i ? i + 1 : i;
        int j = (int)(this.framebufferHeight / pScaleFactor);
        this.guiScaledHeight = this.framebufferHeight / pScaleFactor > j ? j + 1 : j;
    }

    public void setTitle(String pTitle) {
        GLFW.glfwSetWindowTitle(this.window, pTitle);
    }

    public long getWindow() {
        return this.window;
    }

    public boolean isFullscreen() {
        return this.fullscreen;
    }

    public boolean isIconified() {
        return this.iconified;
    }

    public int getWidth() {
        return this.framebufferWidth;
    }

    public int getHeight() {
        return this.framebufferHeight;
    }

    public void setWidth(int pFramebufferWidth) {
        this.framebufferWidth = pFramebufferWidth;
    }

    public void setHeight(int pFramebufferHeight) {
        this.framebufferHeight = pFramebufferHeight;
    }

    public int getScreenWidth() {
        return this.width;
    }

    public int getScreenHeight() {
        return this.height;
    }

    public int getGuiScaledWidth() {
        return this.guiScaledWidth;
    }

    public int getGuiScaledHeight() {
        return this.guiScaledHeight;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public double getGuiScale() {
        return this.guiScale;
    }

    @Nullable
    public Monitor findBestMonitor() {
        return this.screenManager.findBestMonitor(this);
    }

    public void updateRawMouseInput(boolean pEnableRawMouseMotion) {
        InputConstants.updateRawMouseInput(this.window, pEnableRawMouseMotion);
    }

    public void setWindowCloseCallback(Runnable pWindowCloseCallback) {
        GLFWWindowCloseCallback glfwwindowclosecallback = GLFW.glfwSetWindowCloseCallback(this.window, p_361636_ -> pWindowCloseCallback.run());
        if (glfwwindowclosecallback != null) {
            glfwwindowclosecallback.free();
        }
    }

    public boolean isMinimized() {
        return this.minimized;
    }

    @OnlyIn(Dist.CLIENT)
    public static class WindowInitFailed extends SilentInitException {
        WindowInitFailed(String p_85455_) {
            super(p_85455_);
        }
    }
}
