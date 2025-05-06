package com.mojang.blaze3d.opengl;

import com.google.common.collect.EvictingQueue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.DebugMemoryUntracker;
import com.mojang.blaze3d.platform.GLX;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.opengl.ARBDebugOutput;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengl.GLDebugMessageARBCallback;
import org.lwjgl.opengl.GLDebugMessageCallback;
import org.lwjgl.opengl.KHRDebug;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class GlDebug {
    /**
     * TODO: [Forge][Rendering][VEN] Expose this in the configs
     * <p>
     * Will enable synchronous OpenGL debug logging, which means the message will be sent from the call that
     * would have caused the error. Additionally, enables printing a stacktrace when and where this occurs.
     */
    private static final boolean PRINT_STACKTRACE_ON_ERROR = Boolean.getBoolean("forge.printGLStackOnError");

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int CIRCULAR_LOG_SIZE = 10;
    private final Queue<GlDebug.LogEntry> MESSAGE_BUFFER = EvictingQueue.create(10);
    @Nullable
    private volatile GlDebug.LogEntry lastEntry;
    private static final List<Integer> DEBUG_LEVELS = ImmutableList.of(37190, 37191, 37192, 33387);
    private static final List<Integer> DEBUG_LEVELS_ARB = ImmutableList.of(37190, 37191, 37192);

    private static String printUnknownToken(int pToken) {
        return "Unknown (0x" + Integer.toHexString(pToken).toUpperCase() + ")";
    }

    public static String sourceToString(int pSource) {
        switch (pSource) {
            case 33350:
                return "API";
            case 33351:
                return "WINDOW SYSTEM";
            case 33352:
                return "SHADER COMPILER";
            case 33353:
                return "THIRD PARTY";
            case 33354:
                return "APPLICATION";
            case 33355:
                return "OTHER";
            default:
                return printUnknownToken(pSource);
        }
    }

    public static String typeToString(int pType) {
        switch (pType) {
            case 33356:
                return "ERROR";
            case 33357:
                return "DEPRECATED BEHAVIOR";
            case 33358:
                return "UNDEFINED BEHAVIOR";
            case 33359:
                return "PORTABILITY";
            case 33360:
                return "PERFORMANCE";
            case 33361:
                return "OTHER";
            case 33384:
                return "MARKER";
            default:
                return printUnknownToken(pType);
        }
    }

    public static String severityToString(int pType) {
        switch (pType) {
            case 33387:
                return "NOTIFICATION";
            case 37190:
                return "HIGH";
            case 37191:
                return "MEDIUM";
            case 37192:
                return "LOW";
            default:
                return printUnknownToken(pType);
        }
    }

    private void printDebugLog(int pSource, int pType, int pId, int pSeverity, int pLength, long pMessage, long pUserProgram) {
        String s = GLDebugMessageCallback.getMessage(pLength, pMessage);
        GlDebug.LogEntry gldebug$logentry;
        synchronized (this.MESSAGE_BUFFER) {
            gldebug$logentry = this.lastEntry;
            if (gldebug$logentry != null && gldebug$logentry.isSame(pSource, pType, pId, pSeverity, s)) {
                gldebug$logentry.count++;
            } else {
                gldebug$logentry = new GlDebug.LogEntry(pSource, pType, pId, pSeverity, s);
                this.MESSAGE_BUFFER.add(gldebug$logentry);
                this.lastEntry = gldebug$logentry;
            }
        }

        LOGGER.info("OpenGL debug message: {}", gldebug$logentry);
        // TODO: [VEN] Trim the stack trace
        if (PRINT_STACKTRACE_ON_ERROR) LOGGER.info("Trace: ", new Throwable("GlDebug"));
    }

    public List<String> getLastOpenGlDebugMessages() {
        synchronized (this.MESSAGE_BUFFER) {
            List<String> list = Lists.newArrayListWithCapacity(this.MESSAGE_BUFFER.size());

            for (GlDebug.LogEntry gldebug$logentry : this.MESSAGE_BUFFER) {
                list.add(gldebug$logentry + " x " + gldebug$logentry.count);
            }

            return list;
        }
    }

    @Nullable
    public static GlDebug enableDebugCallback(int pVebosity, boolean pSychronous, Set<String> pEnabledExtensions) {
        pSychronous |= PRINT_STACKTRACE_ON_ERROR;
        if (pVebosity <= 0) {
            return null;
        } else {
            GLCapabilities glcapabilities = GL.getCapabilities();
            if (glcapabilities.GL_KHR_debug && GlDevice.USE_GL_KHR_debug) {
                GlDebug gldebug1 = new GlDebug();
                pEnabledExtensions.add("GL_KHR_debug");
                GL11.glEnable(37600);
                if (pSychronous) {
                    GL11.glEnable(33346);
                }

                for (int j = 0; j < DEBUG_LEVELS.size(); j++) {
                    boolean flag1 = j < pVebosity;
                    KHRDebug.glDebugMessageControl(4352, 4352, DEBUG_LEVELS.get(j), (int[])null, flag1);
                }

                KHRDebug.glDebugMessageCallback(GLX.make(GLDebugMessageCallback.create(gldebug1::printDebugLog), DebugMemoryUntracker::untrack), 0L);
                return gldebug1;
            } else if (glcapabilities.GL_ARB_debug_output && GlDevice.USE_GL_ARB_debug_output) {
                GlDebug gldebug = new GlDebug();
                pEnabledExtensions.add("GL_ARB_debug_output");
                if (pSychronous) {
                    GL11.glEnable(33346);
                }

                for (int i = 0; i < DEBUG_LEVELS_ARB.size(); i++) {
                    boolean flag = i < pVebosity;
                    ARBDebugOutput.glDebugMessageControlARB(4352, 4352, DEBUG_LEVELS_ARB.get(i), (int[])null, flag);
                }

                ARBDebugOutput.glDebugMessageCallbackARB(GLX.make(GLDebugMessageARBCallback.create(gldebug::printDebugLog), DebugMemoryUntracker::untrack), 0L);
                return gldebug;
            } else {
                return null;
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class LogEntry {
        private final int id;
        private final int source;
        private final int type;
        private final int severity;
        private final String message;
        int count = 1;

        LogEntry(int pSource, int pType, int pId, int pSeverity, String pMessage) {
            this.id = pId;
            this.source = pSource;
            this.type = pType;
            this.severity = pSeverity;
            this.message = pMessage;
        }

        boolean isSame(int pSource, int pType, int pId, int pSeverity, String pMessage) {
            return pType == this.type
                && pSource == this.source
                && pId == this.id
                && pSeverity == this.severity
                && pMessage.equals(this.message);
        }

        @Override
        public String toString() {
            return "id="
                + this.id
                + ", source="
                + GlDebug.sourceToString(this.source)
                + ", type="
                + GlDebug.typeToString(this.type)
                + ", severity="
                + GlDebug.severityToString(this.severity)
                + ", message='"
                + this.message
                + "'";
        }
    }
}
