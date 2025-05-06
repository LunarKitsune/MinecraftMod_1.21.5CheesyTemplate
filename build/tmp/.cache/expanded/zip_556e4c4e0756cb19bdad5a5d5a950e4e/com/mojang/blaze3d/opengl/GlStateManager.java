package com.mojang.blaze3d.opengl;

import com.google.common.base.Charsets;
import com.mojang.blaze3d.DontObfuscate;
import com.mojang.blaze3d.platform.MacosUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.jtracy.Plot;
import com.mojang.jtracy.TracyClient;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL32C;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

@OnlyIn(Dist.CLIENT)
@DontObfuscate
public class GlStateManager {
    private static final boolean ON_LINUX = Util.getPlatform() == Util.OS.LINUX;
    private static final Plot PLOT_TEXTURES = TracyClient.createPlot("GPU Textures");
    private static int numTextures = 0;
    private static final Plot PLOT_BUFFERS = TracyClient.createPlot("GPU Buffers");
    private static int numBuffers = 0;
    private static final GlStateManager.BlendState BLEND = new GlStateManager.BlendState();
    private static final GlStateManager.DepthState DEPTH = new GlStateManager.DepthState();
    private static final GlStateManager.CullState CULL = new GlStateManager.CullState();
    private static final GlStateManager.PolygonOffsetState POLY_OFFSET = new GlStateManager.PolygonOffsetState();
    private static final GlStateManager.ColorLogicState COLOR_LOGIC = new GlStateManager.ColorLogicState();
    private static final GlStateManager.ScissorState SCISSOR = new GlStateManager.ScissorState();
    private static int activeTexture;
    private static final GlStateManager.TextureState[] TEXTURES = IntStream.range(0, 12)
        .mapToObj(p_397300_ -> new GlStateManager.TextureState())
        .toArray(GlStateManager.TextureState[]::new);
    private static final GlStateManager.ColorMask COLOR_MASK = new GlStateManager.ColorMask();
    private static int readFbo;
    private static int writeFbo;

    public static void _disableScissorTest() {
        RenderSystem.assertOnRenderThread();
        SCISSOR.mode.disable();
    }

    public static void _enableScissorTest() {
        RenderSystem.assertOnRenderThread();
        SCISSOR.mode.enable();
    }

    public static void _scissorBox(int pX, int pY, int pWidth, int pHeight) {
        RenderSystem.assertOnRenderThread();
        GL20.glScissor(pX, pY, pWidth, pHeight);
    }

    public static void _disableDepthTest() {
        RenderSystem.assertOnRenderThread();
        DEPTH.mode.disable();
    }

    public static void _enableDepthTest() {
        RenderSystem.assertOnRenderThread();
        DEPTH.mode.enable();
    }

    public static void _depthFunc(int pDepthFunc) {
        RenderSystem.assertOnRenderThread();
        if (pDepthFunc != DEPTH.func) {
            DEPTH.func = pDepthFunc;
            GL11.glDepthFunc(pDepthFunc);
        }
    }

    public static void _depthMask(boolean pFlag) {
        RenderSystem.assertOnRenderThread();
        if (pFlag != DEPTH.mask) {
            DEPTH.mask = pFlag;
            GL11.glDepthMask(pFlag);
        }
    }

    public static boolean _isBlendEnabled() {
        RenderSystem.assertOnRenderThread();
        return BLEND.mode.enabled;
    }

    public static void _disableBlend() {
        RenderSystem.assertOnRenderThread();
        BLEND.mode.disable();
    }

    public static void _enableBlend() {
        RenderSystem.assertOnRenderThread();
        BLEND.mode.enable();
    }

    public static void _blendFuncSeparate(int pSrcFactor, int pDstFactor, int pSrcFactorAlpha, int pDstFactorAlpha) {
        RenderSystem.assertOnRenderThread();
        if (pSrcFactor != BLEND.srcRgb || pDstFactor != BLEND.dstRgb || pSrcFactorAlpha != BLEND.srcAlpha || pDstFactorAlpha != BLEND.dstAlpha) {
            BLEND.srcRgb = pSrcFactor;
            BLEND.dstRgb = pDstFactor;
            BLEND.srcAlpha = pSrcFactorAlpha;
            BLEND.dstAlpha = pDstFactorAlpha;
            glBlendFuncSeparate(pSrcFactor, pDstFactor, pSrcFactorAlpha, pDstFactorAlpha);
        }
    }

    public static int glGetProgrami(int pProgram, int pPname) {
        RenderSystem.assertOnRenderThread();
        return GL20.glGetProgrami(pProgram, pPname);
    }

    public static void glAttachShader(int pProgram, int pShader) {
        RenderSystem.assertOnRenderThread();
        GL20.glAttachShader(pProgram, pShader);
    }

    public static void glDeleteShader(int pShader) {
        RenderSystem.assertOnRenderThread();
        GL20.glDeleteShader(pShader);
    }

    public static int glCreateShader(int pType) {
        RenderSystem.assertOnRenderThread();
        return GL20.glCreateShader(pType);
    }

    public static void glShaderSource(int pType, String pSource) {
        RenderSystem.assertOnRenderThread();
        byte[] abyte = pSource.getBytes(Charsets.UTF_8);
        ByteBuffer bytebuffer = MemoryUtil.memAlloc(abyte.length + 1);
        bytebuffer.put(abyte);
        bytebuffer.put((byte)0);
        bytebuffer.flip();

        try (MemoryStack memorystack = MemoryStack.stackPush()) {
            PointerBuffer pointerbuffer = memorystack.mallocPointer(1);
            pointerbuffer.put(bytebuffer);
            GL20C.nglShaderSource(pType, 1, pointerbuffer.address0(), 0L);
        } finally {
            MemoryUtil.memFree(bytebuffer);
        }
    }

    public static void glCompileShader(int pShader) {
        RenderSystem.assertOnRenderThread();
        GL20.glCompileShader(pShader);
    }

    public static int glGetShaderi(int pShader, int pPname) {
        RenderSystem.assertOnRenderThread();
        return GL20.glGetShaderi(pShader, pPname);
    }

    public static void _glUseProgram(int pProgram) {
        RenderSystem.assertOnRenderThread();
        GL20.glUseProgram(pProgram);
    }

    public static int glCreateProgram() {
        RenderSystem.assertOnRenderThread();
        return GL20.glCreateProgram();
    }

    public static void glDeleteProgram(int pProgram) {
        RenderSystem.assertOnRenderThread();
        GL20.glDeleteProgram(pProgram);
    }

    public static void glLinkProgram(int pProgram) {
        RenderSystem.assertOnRenderThread();
        GL20.glLinkProgram(pProgram);
    }

    public static int _glGetUniformLocation(int pProgram, CharSequence pName) {
        RenderSystem.assertOnRenderThread();
        return GL20.glGetUniformLocation(pProgram, pName);
    }

    public static void _glUniform1(int pLocation, IntBuffer pValue) {
        RenderSystem.assertOnRenderThread();
        GL20.glUniform1iv(pLocation, pValue);
    }

    public static void _glUniform1i(int pLocation, int pValue) {
        RenderSystem.assertOnRenderThread();
        GL20.glUniform1i(pLocation, pValue);
    }

    public static void _glUniform1(int pLocation, FloatBuffer pValue) {
        RenderSystem.assertOnRenderThread();
        GL20.glUniform1fv(pLocation, pValue);
    }

    public static void _glUniform2(int pLocation, FloatBuffer pValue) {
        RenderSystem.assertOnRenderThread();
        GL20.glUniform2fv(pLocation, pValue);
    }

    public static void _glUniform3(int pLocation, IntBuffer pValue) {
        RenderSystem.assertOnRenderThread();
        GL20.glUniform3iv(pLocation, pValue);
    }

    public static void _glUniform3(int pLocation, FloatBuffer pValue) {
        RenderSystem.assertOnRenderThread();
        GL20.glUniform3fv(pLocation, pValue);
    }

    public static void _glUniform4(int pLocation, FloatBuffer pValue) {
        RenderSystem.assertOnRenderThread();
        GL20.glUniform4fv(pLocation, pValue);
    }

    public static void _glUniformMatrix4(int pLocation, FloatBuffer pValue) {
        RenderSystem.assertOnRenderThread();
        GL20.glUniformMatrix4fv(pLocation, false, pValue);
    }

    public static void _glBindAttribLocation(int pProgram, int pIndex, CharSequence pName) {
        RenderSystem.assertOnRenderThread();
        GL20.glBindAttribLocation(pProgram, pIndex, pName);
    }

    public static int _glGenBuffers() {
        RenderSystem.assertOnRenderThread();
        numBuffers++;
        PLOT_BUFFERS.setValue(numBuffers);
        return GL15.glGenBuffers();
    }

    public static int _glGenVertexArrays() {
        RenderSystem.assertOnRenderThread();
        return GL30.glGenVertexArrays();
    }

    public static void _glBindBuffer(int pTarget, int pBuffer) {
        RenderSystem.assertOnRenderThread();
        GL15.glBindBuffer(pTarget, pBuffer);
    }

    public static void _glBindVertexArray(int pArray) {
        RenderSystem.assertOnRenderThread();
        GL30.glBindVertexArray(pArray);
    }

    public static void _glBufferData(int pTarget, ByteBuffer pData, int pUsage) {
        RenderSystem.assertOnRenderThread();
        GL15.glBufferData(pTarget, pData, pUsage);
    }

    public static void _glBufferSubData(int pTarget, int pOffset, ByteBuffer pData) {
        RenderSystem.assertOnRenderThread();
        GL15.glBufferSubData(pTarget, (long)pOffset, pData);
    }

    public static void _glBufferData(int pTarget, long pSize, int pUsage) {
        RenderSystem.assertOnRenderThread();
        GL15.glBufferData(pTarget, pSize, pUsage);
    }

    @Nullable
    public static ByteBuffer _glMapBufferRange(int pTarget, int pOffset, int pLength, int pAccess) {
        RenderSystem.assertOnRenderThread();
        return GL30.glMapBufferRange(pTarget, pOffset, pLength, pAccess);
    }

    public static void _glUnmapBuffer(int pTarget) {
        RenderSystem.assertOnRenderThread();
        GL15.glUnmapBuffer(pTarget);
    }

    public static void _glDeleteBuffers(int pBuffer) {
        RenderSystem.assertOnRenderThread();
        if (ON_LINUX) {
            GL32C.glBindBuffer(34962, pBuffer);
            GL32C.glBufferData(34962, 0L, 35048);
            GL32C.glBindBuffer(34962, 0);
        }

        numBuffers--;
        PLOT_BUFFERS.setValue(numBuffers);
        GL15.glDeleteBuffers(pBuffer);
    }

    public static void _glBindFramebuffer(int pTarget, int pFrameBuffer) {
        if ((pTarget == 36008 || pTarget == 36160) && readFbo != pFrameBuffer) {
            GL30.glBindFramebuffer(36008, pFrameBuffer);
            readFbo = pFrameBuffer;
        }

        if ((pTarget == 36009 || pTarget == 36160) && writeFbo != pFrameBuffer) {
            GL30.glBindFramebuffer(36009, pFrameBuffer);
            writeFbo = pFrameBuffer;
        }
    }

    public static int getFrameBuffer(int pFrameBuffer) {
        if (pFrameBuffer == 36008) {
            return readFbo;
        } else {
            return pFrameBuffer == 36009 ? writeFbo : 0;
        }
    }

    public static void _glBlitFrameBuffer(
        int pSrcX0, int pSrcY0, int pSrcX1, int pSrcY1, int pDstX0, int pDstY0, int pDstX1, int pDstY1, int pMask, int pFilter
    ) {
        RenderSystem.assertOnRenderThread();
        GL30.glBlitFramebuffer(pSrcX0, pSrcY0, pSrcX1, pSrcY1, pDstX0, pDstY0, pDstX1, pDstY1, pMask, pFilter);
    }

    public static void _glDeleteFramebuffers(int pFrameBuffer) {
        RenderSystem.assertOnRenderThread();
        GL30.glDeleteFramebuffers(pFrameBuffer);
    }

    public static int glGenFramebuffers() {
        RenderSystem.assertOnRenderThread();
        return GL30.glGenFramebuffers();
    }

    public static void _glFramebufferTexture2D(int pTarget, int pAttachment, int pTexTarget, int pTexture, int pLevel) {
        RenderSystem.assertOnRenderThread();
        GL30.glFramebufferTexture2D(pTarget, pAttachment, pTexTarget, pTexture, pLevel);
    }

    public static void glActiveTexture(int pTexture) {
        RenderSystem.assertOnRenderThread();
        GL13.glActiveTexture(pTexture);
    }

    public static void glBlendFuncSeparate(int pSFactorRGB, int pDFactorRGB, int pSFactorAlpha, int pDFactorAlpha) {
        RenderSystem.assertOnRenderThread();
        GL14.glBlendFuncSeparate(pSFactorRGB, pDFactorRGB, pSFactorAlpha, pDFactorAlpha);
    }

    public static String glGetShaderInfoLog(int pShader, int pMaxLength) {
        RenderSystem.assertOnRenderThread();
        return GL20.glGetShaderInfoLog(pShader, pMaxLength);
    }

    public static String glGetProgramInfoLog(int pProgram, int pMaxLength) {
        RenderSystem.assertOnRenderThread();
        return GL20.glGetProgramInfoLog(pProgram, pMaxLength);
    }

    public static void _enableCull() {
        RenderSystem.assertOnRenderThread();
        CULL.enable.enable();
    }

    public static void _disableCull() {
        RenderSystem.assertOnRenderThread();
        CULL.enable.disable();
    }

    public static void _polygonMode(int pFace, int pMode) {
        RenderSystem.assertOnRenderThread();
        GL11.glPolygonMode(pFace, pMode);
    }

    public static void _enablePolygonOffset() {
        RenderSystem.assertOnRenderThread();
        POLY_OFFSET.fill.enable();
    }

    public static void _disablePolygonOffset() {
        RenderSystem.assertOnRenderThread();
        POLY_OFFSET.fill.disable();
    }

    public static void _polygonOffset(float pFactor, float pUnits) {
        RenderSystem.assertOnRenderThread();
        if (pFactor != POLY_OFFSET.factor || pUnits != POLY_OFFSET.units) {
            POLY_OFFSET.factor = pFactor;
            POLY_OFFSET.units = pUnits;
            GL11.glPolygonOffset(pFactor, pUnits);
        }
    }

    public static void _enableColorLogicOp() {
        RenderSystem.assertOnRenderThread();
        COLOR_LOGIC.enable.enable();
    }

    public static void _disableColorLogicOp() {
        RenderSystem.assertOnRenderThread();
        COLOR_LOGIC.enable.disable();
    }

    public static void _logicOp(int pLogicOperation) {
        RenderSystem.assertOnRenderThread();
        if (pLogicOperation != COLOR_LOGIC.op) {
            COLOR_LOGIC.op = pLogicOperation;
            GL11.glLogicOp(pLogicOperation);
        }
    }

    public static void _activeTexture(int pTexture) {
        RenderSystem.assertOnRenderThread();
        if (activeTexture != pTexture - 33984) {
            activeTexture = pTexture - 33984;
            glActiveTexture(pTexture);
        }
    }

    /* Stores the last values sent into glMultiTexCoord2f */
    public static float lastBrightnessX = 0.0f;
    public static float lastBrightnessY = 0.0f;

    public static void _texParameter(int pTarget, int pParameterName, int pParameter) {
        RenderSystem.assertOnRenderThread();
        GL11.glTexParameteri(pTarget, pParameterName, pParameter);
        if (pTarget == GL13.GL_TEXTURE1) {
            lastBrightnessX = pParameterName;
            lastBrightnessY = pParameter;
        }
    }

    public static int _getTexLevelParameter(int pTarget, int pLevel, int pParameterName) {
        return GL11.glGetTexLevelParameteri(pTarget, pLevel, pParameterName);
    }

    public static int _genTexture() {
        RenderSystem.assertOnRenderThread();
        numTextures++;
        PLOT_TEXTURES.setValue(numTextures);
        return GL11.glGenTextures();
    }

    public static void _deleteTexture(int pTexture) {
        RenderSystem.assertOnRenderThread();
        GL11.glDeleteTextures(pTexture);

        for (GlStateManager.TextureState glstatemanager$texturestate : TEXTURES) {
            if (glstatemanager$texturestate.binding == pTexture) {
                glstatemanager$texturestate.binding = -1;
            }
        }

        numTextures--;
        PLOT_TEXTURES.setValue(numTextures);
    }

    public static void _bindTexture(int pTexture) {
        RenderSystem.assertOnRenderThread();
        if (pTexture != TEXTURES[activeTexture].binding) {
            TEXTURES[activeTexture].binding = pTexture;
            GL11.glBindTexture(3553, pTexture);
        }
    }

    public static int _getActiveTexture() {
        return activeTexture + 33984;
    }

    public static void _texImage2D(
        int pTarget, int pLevel, int pInternalFormat, int pWidth, int pHeight, int pBorder, int pFormat, int pType, @Nullable IntBuffer pPixels
    ) {
        RenderSystem.assertOnRenderThread();
        GL11.glTexImage2D(pTarget, pLevel, pInternalFormat, pWidth, pHeight, pBorder, pFormat, pType, pPixels);
    }

    public static void _texSubImage2D(
        int pTarget, int pLevel, int pXOffset, int pYOffset, int pWidth, int pHeight, int pFormat, int pType, long pPixels
    ) {
        RenderSystem.assertOnRenderThread();
        GL11.glTexSubImage2D(pTarget, pLevel, pXOffset, pYOffset, pWidth, pHeight, pFormat, pType, pPixels);
    }

    public static void _texSubImage2D(
        int pTarget, int pLevel, int pXOffset, int pYOffset, int pWidth, int pHeight, int pFormat, int pType, IntBuffer pPixels
    ) {
        RenderSystem.assertOnRenderThread();
        GL11.glTexSubImage2D(pTarget, pLevel, pXOffset, pYOffset, pWidth, pHeight, pFormat, pType, pPixels);
    }

    public static void _viewport(int pX, int pY, int pWidth, int pHeight) {
        GL11.glViewport(pX, pY, pWidth, pHeight);
    }

    public static void _colorMask(boolean pRed, boolean pGreen, boolean pBlue, boolean pAlpha) {
        RenderSystem.assertOnRenderThread();
        if (pRed != COLOR_MASK.red || pGreen != COLOR_MASK.green || pBlue != COLOR_MASK.blue || pAlpha != COLOR_MASK.alpha) {
            COLOR_MASK.red = pRed;
            COLOR_MASK.green = pGreen;
            COLOR_MASK.blue = pBlue;
            COLOR_MASK.alpha = pAlpha;
            GL11.glColorMask(pRed, pGreen, pBlue, pAlpha);
        }
    }

    public static void _clear(int pMask) {
        RenderSystem.assertOnRenderThread();
        GL11.glClear(pMask);
        if (MacosUtil.IS_MACOS) {
            _getError();
        }
    }

    public static void _vertexAttribPointer(int pIndex, int pSize, int pType, boolean pNormalized, int pStride, long pPointer) {
        RenderSystem.assertOnRenderThread();
        GL20.glVertexAttribPointer(pIndex, pSize, pType, pNormalized, pStride, pPointer);
    }

    public static void _vertexAttribIPointer(int pIndex, int pSize, int pType, int pStride, long pPointer) {
        RenderSystem.assertOnRenderThread();
        GL30.glVertexAttribIPointer(pIndex, pSize, pType, pStride, pPointer);
    }

    public static void _enableVertexAttribArray(int pIndex) {
        RenderSystem.assertOnRenderThread();
        GL20.glEnableVertexAttribArray(pIndex);
    }

    public static void _drawElements(int pMode, int pCount, int pType, long pIndices) {
        RenderSystem.assertOnRenderThread();
        GL11.glDrawElements(pMode, pCount, pType, pIndices);
    }

    public static void _drawArrays(int pMode, int pFirst, int pCount) {
        RenderSystem.assertOnRenderThread();
        GL11.glDrawArrays(pMode, pFirst, pCount);
    }

    public static void _pixelStore(int pParameterName, int pParam) {
        RenderSystem.assertOnRenderThread();
        GL11.glPixelStorei(pParameterName, pParam);
    }

    public static void _readPixels(int pX, int pY, int pWidth, int pHeight, int pFormat, int pType, long pPixels) {
        RenderSystem.assertOnRenderThread();
        GL11.glReadPixels(pX, pY, pWidth, pHeight, pFormat, pType, pPixels);
    }

    public static int _getError() {
        RenderSystem.assertOnRenderThread();
        return GL11.glGetError();
    }

    public static void clearGlErrors() {
        RenderSystem.assertOnRenderThread();

        while (GL11.glGetError() != 0) {
        }
    }

    public static String _getString(int pName) {
        RenderSystem.assertOnRenderThread();
        return GL11.glGetString(pName);
    }

    public static int _getInteger(int pPname) {
        RenderSystem.assertOnRenderThread();
        return GL11.glGetInteger(pPname);
    }

    public static long _glFenceSync(int pCondition, int pFlags) {
        RenderSystem.assertOnRenderThread();
        return GL32.glFenceSync(pCondition, pFlags);
    }

    public static int _glClientWaitSync(long pSync, int pFlags, long pTimeout) {
        RenderSystem.assertOnRenderThread();
        return GL32.glClientWaitSync(pSync, pFlags, pTimeout);
    }

    public static void _glDeleteSync(long pSync) {
        RenderSystem.assertOnRenderThread();
        GL32.glDeleteSync(pSync);
    }

    @OnlyIn(Dist.CLIENT)
    static class BlendState {
        public final GlStateManager.BooleanState mode = new GlStateManager.BooleanState(3042);
        public int srcRgb = 1;
        public int dstRgb = 0;
        public int srcAlpha = 1;
        public int dstAlpha = 0;
    }

    @OnlyIn(Dist.CLIENT)
    static class BooleanState {
        private final int state;
        private boolean enabled;

        public BooleanState(int pState) {
            this.state = pState;
        }

        public void disable() {
            this.setEnabled(false);
        }

        public void enable() {
            this.setEnabled(true);
        }

        public void setEnabled(boolean pEnabled) {
            RenderSystem.assertOnRenderThread();
            if (pEnabled != this.enabled) {
                this.enabled = pEnabled;
                if (pEnabled) {
                    GL11.glEnable(this.state);
                } else {
                    GL11.glDisable(this.state);
                }
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class ColorLogicState {
        public final GlStateManager.BooleanState enable = new GlStateManager.BooleanState(3058);
        public int op = 5379;
    }

    @OnlyIn(Dist.CLIENT)
    static class ColorMask {
        public boolean red = true;
        public boolean green = true;
        public boolean blue = true;
        public boolean alpha = true;
    }

    @OnlyIn(Dist.CLIENT)
    static class CullState {
        public final GlStateManager.BooleanState enable = new GlStateManager.BooleanState(2884);
    }

    @OnlyIn(Dist.CLIENT)
    static class DepthState {
        public final GlStateManager.BooleanState mode = new GlStateManager.BooleanState(2929);
        public boolean mask = true;
        public int func = 513;
    }

    @OnlyIn(Dist.CLIENT)
    static class PolygonOffsetState {
        public final GlStateManager.BooleanState fill = new GlStateManager.BooleanState(32823);
        public float factor;
        public float units;
    }

    @OnlyIn(Dist.CLIENT)
    static class ScissorState {
        public final GlStateManager.BooleanState mode = new GlStateManager.BooleanState(3089);
    }

    @OnlyIn(Dist.CLIENT)
    static class TextureState {
        public int binding;
    }
}
