package com.mojang.blaze3d.platform;

import com.mojang.jtracy.MemoryPool;
import com.mojang.jtracy.TracyClient;
import com.mojang.logging.LogUtils;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.IntUnaryOperator;
import javax.annotation.Nullable;
import net.minecraft.client.gui.font.providers.FreeTypeUtil;
import net.minecraft.util.ARGB;
import net.minecraft.util.PngInfo;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.io.IOUtils;
import org.lwjgl.stb.STBIWriteCallback;
import org.lwjgl.stb.STBImage;
import org.lwjgl.stb.STBImageResize;
import org.lwjgl.stb.STBImageWrite;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.freetype.FT_Bitmap;
import org.lwjgl.util.freetype.FT_Face;
import org.lwjgl.util.freetype.FT_GlyphSlot;
import org.lwjgl.util.freetype.FreeType;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public final class NativeImage implements AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final MemoryPool MEMORY_POOL = TracyClient.createMemoryPool("NativeImage");
    private static final Set<StandardOpenOption> OPEN_OPTIONS = EnumSet.of(
        StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING
    );
    private final NativeImage.Format format;
    private final int width;
    private final int height;
    private final boolean useStbFree;
    private long pixels;
    private final long size;

    public NativeImage(int pWidth, int pHeight, boolean pUseCalloc) {
        this(NativeImage.Format.RGBA, pWidth, pHeight, pUseCalloc);
    }

    public NativeImage(NativeImage.Format pFormat, int pWidth, int pHeight, boolean pUseCalloc) {
        if (pWidth > 0 && pHeight > 0) {
            this.format = pFormat;
            this.width = pWidth;
            this.height = pHeight;
            this.size = (long)pWidth * pHeight * pFormat.components();
            this.useStbFree = false;
            if (pUseCalloc) {
                this.pixels = MemoryUtil.nmemCalloc(1L, this.size);
            } else {
                this.pixels = MemoryUtil.nmemAlloc(this.size);
            }

            MEMORY_POOL.malloc(this.pixels, (int)this.size);
            if (this.pixels == 0L) {
                throw new IllegalStateException("Unable to allocate texture of size " + pWidth + "x" + pHeight + " (" + pFormat.components() + " channels)");
            }
        } else {
            throw new IllegalArgumentException("Invalid texture size: " + pWidth + "x" + pHeight);
        }
    }

    public NativeImage(NativeImage.Format pFormat, int pWidth, int pHeight, boolean pUseStbFree, long pPixels) {
        if (pWidth > 0 && pHeight > 0) {
            this.format = pFormat;
            this.width = pWidth;
            this.height = pHeight;
            this.useStbFree = pUseStbFree;
            this.pixels = pPixels;
            this.size = (long)pWidth * pHeight * pFormat.components();
        } else {
            throw new IllegalArgumentException("Invalid texture size: " + pWidth + "x" + pHeight);
        }
    }

    @Override
    public String toString() {
        return "NativeImage[" + this.format + " " + this.width + "x" + this.height + "@" + this.pixels + (this.useStbFree ? "S" : "N") + "]";
    }

    private boolean isOutsideBounds(int pX, int pY) {
        return pX < 0 || pX >= this.width || pY < 0 || pY >= this.height;
    }

    public static NativeImage read(InputStream pTextureStream) throws IOException {
        return read(NativeImage.Format.RGBA, pTextureStream);
    }

    public static NativeImage read(@Nullable NativeImage.Format pFormat, InputStream pTextureStream) throws IOException {
        ByteBuffer bytebuffer = null;

        NativeImage nativeimage;
        try {
            bytebuffer = TextureUtil.readResource(pTextureStream);
            bytebuffer.rewind();
            nativeimage = read(pFormat, bytebuffer);
        } finally {
            MemoryUtil.memFree(bytebuffer);
            IOUtils.closeQuietly(pTextureStream);
        }

        return nativeimage;
    }

    public static NativeImage read(ByteBuffer pTextureData) throws IOException {
        return read(NativeImage.Format.RGBA, pTextureData);
    }

    public static NativeImage read(byte[] pBytes) throws IOException {
        MemoryStack memorystack = MemoryStack.stackGet();
        int i = memorystack.getPointer();
        if (i < pBytes.length) {
            ByteBuffer bytebuffer1 = MemoryUtil.memAlloc(pBytes.length);

            NativeImage nativeimage1;
            try {
                nativeimage1 = putAndRead(bytebuffer1, pBytes);
            } finally {
                MemoryUtil.memFree(bytebuffer1);
            }

            return nativeimage1;
        } else {
            NativeImage nativeimage;
            try (MemoryStack memorystack1 = MemoryStack.stackPush()) {
                ByteBuffer bytebuffer = memorystack1.malloc(pBytes.length);
                nativeimage = putAndRead(bytebuffer, pBytes);
            }

            return nativeimage;
        }
    }

    private static NativeImage putAndRead(ByteBuffer pBuffer, byte[] pBytes) throws IOException {
        pBuffer.put(pBytes);
        pBuffer.rewind();
        return read(pBuffer);
    }

    public static NativeImage read(@Nullable NativeImage.Format pFormat, ByteBuffer pTextureData) throws IOException {
        if (pFormat != null && !pFormat.supportedByStb()) {
            throw new UnsupportedOperationException("Don't know how to read format " + pFormat);
        } else if (MemoryUtil.memAddress(pTextureData) == 0L) {
            throw new IllegalArgumentException("Invalid buffer");
        } else {
            PngInfo.validateHeader(pTextureData);

            NativeImage nativeimage;
            try (MemoryStack memorystack = MemoryStack.stackPush()) {
                IntBuffer intbuffer = memorystack.mallocInt(1);
                IntBuffer intbuffer1 = memorystack.mallocInt(1);
                IntBuffer intbuffer2 = memorystack.mallocInt(1);
                ByteBuffer bytebuffer = STBImage.stbi_load_from_memory(pTextureData, intbuffer, intbuffer1, intbuffer2, pFormat == null ? 0 : pFormat.components);
                if (bytebuffer == null) {
                    throw new IOException("Could not load image: " + STBImage.stbi_failure_reason());
                }

                long i = MemoryUtil.memAddress(bytebuffer);
                MEMORY_POOL.malloc(i, bytebuffer.limit());
                nativeimage = new NativeImage(
                    pFormat == null ? NativeImage.Format.getStbFormat(intbuffer2.get(0)) : pFormat, intbuffer.get(0), intbuffer1.get(0), true, i
                );
            }

            return nativeimage;
        }
    }

    private void checkAllocated() {
        if (this.pixels == 0L) {
            throw new IllegalStateException("Image is not allocated.");
        }
    }

    @Override
    public void close() {
        if (this.pixels != 0L) {
            if (this.useStbFree) {
                STBImage.nstbi_image_free(this.pixels);
            } else {
                MemoryUtil.nmemFree(this.pixels);
            }

            MEMORY_POOL.free(this.pixels);
        }

        this.pixels = 0L;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public NativeImage.Format format() {
        return this.format;
    }

    private int getPixelABGR(int pX, int pY) {
        if (this.format != NativeImage.Format.RGBA) {
            throw new IllegalArgumentException(String.format(Locale.ROOT, "getPixelRGBA only works on RGBA images; have %s", this.format));
        } else if (this.isOutsideBounds(pX, pY)) {
            throw new IllegalArgumentException(
                String.format(Locale.ROOT, "(%s, %s) outside of image bounds (%s, %s)", pX, pY, this.width, this.height)
            );
        } else {
            this.checkAllocated();
            long i = (pX + (long)pY * this.width) * 4L;
            return MemoryUtil.memGetInt(this.pixels + i);
        }
    }

    public int getPixel(int pX, int pY) {
        return ARGB.fromABGR(this.getPixelABGR(pX, pY));
    }

    public void setPixelABGR(int pX, int pY, int pColor) {
        if (this.format != NativeImage.Format.RGBA) {
            throw new IllegalArgumentException(String.format(Locale.ROOT, "setPixelRGBA only works on RGBA images; have %s", this.format));
        } else if (this.isOutsideBounds(pX, pY)) {
            throw new IllegalArgumentException(
                String.format(Locale.ROOT, "(%s, %s) outside of image bounds (%s, %s)", pX, pY, this.width, this.height)
            );
        } else {
            this.checkAllocated();
            long i = (pX + (long)pY * this.width) * 4L;
            MemoryUtil.memPutInt(this.pixels + i, pColor);
        }
    }

    public void setPixel(int pX, int pY, int pColor) {
        this.setPixelABGR(pX, pY, ARGB.toABGR(pColor));
    }

    public NativeImage mappedCopy(IntUnaryOperator pFunction) {
        if (this.format != NativeImage.Format.RGBA) {
            throw new IllegalArgumentException(String.format(Locale.ROOT, "function application only works on RGBA images; have %s", this.format));
        } else {
            this.checkAllocated();
            NativeImage nativeimage = new NativeImage(this.width, this.height, false);
            int i = this.width * this.height;
            IntBuffer intbuffer = MemoryUtil.memIntBuffer(this.pixels, i);
            IntBuffer intbuffer1 = MemoryUtil.memIntBuffer(nativeimage.pixels, i);

            for (int j = 0; j < i; j++) {
                int k = ARGB.fromABGR(intbuffer.get(j));
                int l = pFunction.applyAsInt(k);
                intbuffer1.put(j, ARGB.toABGR(l));
            }

            return nativeimage;
        }
    }

    public int[] getPixelsABGR() {
        if (this.format != NativeImage.Format.RGBA) {
            throw new IllegalArgumentException(String.format(Locale.ROOT, "getPixels only works on RGBA images; have %s", this.format));
        } else {
            this.checkAllocated();
            int[] aint = new int[this.width * this.height];
            MemoryUtil.memIntBuffer(this.pixels, this.width * this.height).get(aint);
            return aint;
        }
    }

    public int[] getPixels() {
        int[] aint = this.getPixelsABGR();

        for (int i = 0; i < aint.length; i++) {
            aint[i] = ARGB.fromABGR(aint[i]);
        }

        return aint;
    }

    public byte getLuminanceOrAlpha(int pX, int pY) {
        if (!this.format.hasLuminanceOrAlpha()) {
            throw new IllegalArgumentException(String.format(Locale.ROOT, "no luminance or alpha in %s", this.format));
        } else if (this.isOutsideBounds(pX, pY)) {
            throw new IllegalArgumentException(
                String.format(Locale.ROOT, "(%s, %s) outside of image bounds (%s, %s)", pX, pY, this.width, this.height)
            );
        } else {
            int i = (pX + pY * this.width) * this.format.components() + this.format.luminanceOrAlphaOffset() / 8;
            return MemoryUtil.memGetByte(this.pixels + i);
        }
    }

    @Deprecated
    public int[] makePixelArray() {
        if (this.format != NativeImage.Format.RGBA) {
            throw new UnsupportedOperationException("can only call makePixelArray for RGBA images.");
        } else {
            this.checkAllocated();
            int[] aint = new int[this.getWidth() * this.getHeight()];

            for (int i = 0; i < this.getHeight(); i++) {
                for (int j = 0; j < this.getWidth(); j++) {
                    aint[j + i * this.getWidth()] = this.getPixel(j, i);
                }
            }

            return aint;
        }
    }

    public void writeToFile(File pFile) throws IOException {
        this.writeToFile(pFile.toPath());
    }

    public boolean copyFromFont(FT_Face pFace, int pIndex) {
        if (this.format.components() != 1) {
            throw new IllegalArgumentException("Can only write fonts into 1-component images.");
        } else if (FreeTypeUtil.checkError(FreeType.FT_Load_Glyph(pFace, pIndex, 4), "Loading glyph")) {
            return false;
        } else {
            FT_GlyphSlot ft_glyphslot = Objects.requireNonNull(pFace.glyph(), "Glyph not initialized");
            FT_Bitmap ft_bitmap = ft_glyphslot.bitmap();
            if (ft_bitmap.pixel_mode() != 2) {
                throw new IllegalStateException("Rendered glyph was not 8-bit grayscale");
            } else if (ft_bitmap.width() == this.getWidth() && ft_bitmap.rows() == this.getHeight()) {
                int i = ft_bitmap.width() * ft_bitmap.rows();
                ByteBuffer bytebuffer = Objects.requireNonNull(ft_bitmap.buffer(i), "Glyph has no bitmap");
                MemoryUtil.memCopy(MemoryUtil.memAddress(bytebuffer), this.pixels, i);
                return true;
            } else {
                throw new IllegalArgumentException(
                    String.format(
                        Locale.ROOT,
                        "Glyph bitmap of size %sx%s does not match image of size: %sx%s",
                        ft_bitmap.width(),
                        ft_bitmap.rows(),
                        this.getWidth(),
                        this.getHeight()
                    )
                );
            }
        }
    }

    public void writeToFile(Path pPath) throws IOException {
        if (!this.format.supportedByStb()) {
            throw new UnsupportedOperationException("Don't know how to write format " + this.format);
        } else {
            this.checkAllocated();

            try (WritableByteChannel writablebytechannel = Files.newByteChannel(pPath, OPEN_OPTIONS)) {
                if (!this.writeToChannel(writablebytechannel)) {
                    throw new IOException("Could not write image to the PNG file \"" + pPath.toAbsolutePath() + "\": " + STBImage.stbi_failure_reason());
                }
            }
        }
    }

    private boolean writeToChannel(WritableByteChannel pChannel) throws IOException {
        NativeImage.WriteCallback nativeimage$writecallback = new NativeImage.WriteCallback(pChannel);

        boolean flag;
        try {
            int i = Math.min(this.getHeight(), Integer.MAX_VALUE / this.getWidth() / this.format.components());
            if (i < this.getHeight()) {
                LOGGER.warn("Dropping image height from {} to {} to fit the size into 32-bit signed int", this.getHeight(), i);
            }

            if (STBImageWrite.nstbi_write_png_to_func(nativeimage$writecallback.address(), 0L, this.getWidth(), i, this.format.components(), this.pixels, 0)
                != 0) {
                nativeimage$writecallback.throwIfException();
                return true;
            }

            flag = false;
        } finally {
            nativeimage$writecallback.free();
        }

        return flag;
    }

    public void copyFrom(NativeImage pOther) {
        if (pOther.format() != this.format) {
            throw new UnsupportedOperationException("Image formats don't match.");
        } else {
            int i = this.format.components();
            this.checkAllocated();
            pOther.checkAllocated();
            if (this.width == pOther.width) {
                MemoryUtil.memCopy(pOther.pixels, this.pixels, Math.min(this.size, pOther.size));
            } else {
                int j = Math.min(this.getWidth(), pOther.getWidth());
                int k = Math.min(this.getHeight(), pOther.getHeight());

                for (int l = 0; l < k; l++) {
                    int i1 = l * pOther.getWidth() * i;
                    int j1 = l * this.getWidth() * i;
                    MemoryUtil.memCopy(pOther.pixels + i1, this.pixels + j1, j);
                }
            }
        }
    }

    public void fillRect(int pX, int pY, int pWidth, int pHeight, int pValue) {
        for (int i = pY; i < pY + pHeight; i++) {
            for (int j = pX; j < pX + pWidth; j++) {
                this.setPixel(j, i, pValue);
            }
        }
    }

    public void copyRect(int pXFrom, int pYFrom, int pXToDelta, int pYToDelta, int pWidth, int pHeight, boolean pMirrorX, boolean pMirrorY) {
        this.copyRect(this, pXFrom, pYFrom, pXFrom + pXToDelta, pYFrom + pYToDelta, pWidth, pHeight, pMirrorX, pMirrorY);
    }

    public void copyRect(
        NativeImage pSource, int pXFrom, int pYFrom, int pXTo, int pYTo, int pWidth, int pHeight, boolean pMirrorX, boolean pMirrorY
    ) {
        for (int i = 0; i < pHeight; i++) {
            for (int j = 0; j < pWidth; j++) {
                int k = pMirrorX ? pWidth - 1 - j : j;
                int l = pMirrorY ? pHeight - 1 - i : i;
                int i1 = this.getPixelABGR(pXFrom + j, pYFrom + i);
                pSource.setPixelABGR(pXTo + k, pYTo + l, i1);
            }
        }
    }

    public void resizeSubRectTo(int pX, int pY, int pWidth, int pHeight, NativeImage pImage) {
        this.checkAllocated();
        if (pImage.format() != this.format) {
            throw new UnsupportedOperationException("resizeSubRectTo only works for images of the same format.");
        } else {
            int i = this.format.components();
            STBImageResize.nstbir_resize_uint8(
                this.pixels + (pX + pY * this.getWidth()) * i,
                pWidth,
                pHeight,
                this.getWidth() * i,
                pImage.pixels,
                pImage.getWidth(),
                pImage.getHeight(),
                0,
                i
            );
        }
    }

    public void untrack() {
        DebugMemoryUntracker.untrack(this.pixels);
    }

    public long getPointer() {
        return this.pixels;
    }

    @OnlyIn(Dist.CLIENT)
    public static enum Format {
        RGBA(4, true, true, true, false, true, 0, 8, 16, 255, 24, true),
        RGB(3, true, true, true, false, false, 0, 8, 16, 255, 255, true),
        LUMINANCE_ALPHA(2, false, false, false, true, true, 255, 255, 255, 0, 8, true),
        LUMINANCE(1, false, false, false, true, false, 0, 0, 0, 0, 255, true);

        final int components;
        private final boolean hasRed;
        private final boolean hasGreen;
        private final boolean hasBlue;
        private final boolean hasLuminance;
        private final boolean hasAlpha;
        private final int redOffset;
        private final int greenOffset;
        private final int blueOffset;
        private final int luminanceOffset;
        private final int alphaOffset;
        private final boolean supportedByStb;

        private Format(
            final int pComponents,
            final boolean pHasRed,
            final boolean pHasGreen,
            final boolean pHasBlue,
            final boolean pHasLuminance,
            final boolean pHasAlpha,
            final int pRedOffset,
            final int pGreenOffset,
            final int pBlueOffset,
            final int pLuminanceOffset,
            final int pAlphaOffset,
            final boolean pSupportedByStb
        ) {
            this.components = pComponents;
            this.hasRed = pHasRed;
            this.hasGreen = pHasGreen;
            this.hasBlue = pHasBlue;
            this.hasLuminance = pHasLuminance;
            this.hasAlpha = pHasAlpha;
            this.redOffset = pRedOffset;
            this.greenOffset = pGreenOffset;
            this.blueOffset = pBlueOffset;
            this.luminanceOffset = pLuminanceOffset;
            this.alphaOffset = pAlphaOffset;
            this.supportedByStb = pSupportedByStb;
        }

        public int components() {
            return this.components;
        }

        public boolean hasRed() {
            return this.hasRed;
        }

        public boolean hasGreen() {
            return this.hasGreen;
        }

        public boolean hasBlue() {
            return this.hasBlue;
        }

        public boolean hasLuminance() {
            return this.hasLuminance;
        }

        public boolean hasAlpha() {
            return this.hasAlpha;
        }

        public int redOffset() {
            return this.redOffset;
        }

        public int greenOffset() {
            return this.greenOffset;
        }

        public int blueOffset() {
            return this.blueOffset;
        }

        public int luminanceOffset() {
            return this.luminanceOffset;
        }

        public int alphaOffset() {
            return this.alphaOffset;
        }

        public boolean hasLuminanceOrRed() {
            return this.hasLuminance || this.hasRed;
        }

        public boolean hasLuminanceOrGreen() {
            return this.hasLuminance || this.hasGreen;
        }

        public boolean hasLuminanceOrBlue() {
            return this.hasLuminance || this.hasBlue;
        }

        public boolean hasLuminanceOrAlpha() {
            return this.hasLuminance || this.hasAlpha;
        }

        public int luminanceOrRedOffset() {
            return this.hasLuminance ? this.luminanceOffset : this.redOffset;
        }

        public int luminanceOrGreenOffset() {
            return this.hasLuminance ? this.luminanceOffset : this.greenOffset;
        }

        public int luminanceOrBlueOffset() {
            return this.hasLuminance ? this.luminanceOffset : this.blueOffset;
        }

        public int luminanceOrAlphaOffset() {
            return this.hasLuminance ? this.luminanceOffset : this.alphaOffset;
        }

        public boolean supportedByStb() {
            return this.supportedByStb;
        }

        static NativeImage.Format getStbFormat(int pChannels) {
            switch (pChannels) {
                case 1:
                    return LUMINANCE;
                case 2:
                    return LUMINANCE_ALPHA;
                case 3:
                    return RGB;
                case 4:
                default:
                    return RGBA;
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    static class WriteCallback extends STBIWriteCallback {
        private final WritableByteChannel output;
        @Nullable
        private IOException exception;

        WriteCallback(WritableByteChannel pOutput) {
            this.output = pOutput;
        }

        @Override
        public void invoke(long pContext, long pData, int pSize) {
            ByteBuffer bytebuffer = getData(pData, pSize);

            try {
                this.output.write(bytebuffer);
            } catch (IOException ioexception) {
                this.exception = ioexception;
            }
        }

        public void throwIfException() throws IOException {
            if (this.exception != null) {
                throw this.exception;
            }
        }
    }
}