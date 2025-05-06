package com.mojang.blaze3d.opengl;

import com.mojang.blaze3d.DontObfuscate;
import com.mojang.blaze3d.buffers.BufferType;
import com.mojang.blaze3d.buffers.BufferUsage;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.PolygonMode;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.shaders.ShaderType;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@DontObfuscate
public class GlConst {
    public static final int GL_READ_FRAMEBUFFER = 36008;
    public static final int GL_DRAW_FRAMEBUFFER = 36009;
    public static final int GL_TRUE = 1;
    public static final int GL_FALSE = 0;
    public static final int GL_NONE = 0;
    public static final int GL_LINES = 1;
    public static final int GL_LINE_STRIP = 3;
    public static final int GL_TRIANGLE_STRIP = 5;
    public static final int GL_TRIANGLE_FAN = 6;
    public static final int GL_TRIANGLES = 4;
    public static final int GL_WRITE_ONLY = 35001;
    public static final int GL_READ_ONLY = 35000;
    public static final int GL_MAP_READ_BIT = 1;
    public static final int GL_EQUAL = 514;
    public static final int GL_LEQUAL = 515;
    public static final int GL_LESS = 513;
    public static final int GL_GREATER = 516;
    public static final int GL_GEQUAL = 518;
    public static final int GL_ALWAYS = 519;
    public static final int GL_TEXTURE_MAG_FILTER = 10240;
    public static final int GL_TEXTURE_MIN_FILTER = 10241;
    public static final int GL_TEXTURE_WRAP_S = 10242;
    public static final int GL_TEXTURE_WRAP_T = 10243;
    public static final int GL_NEAREST = 9728;
    public static final int GL_LINEAR = 9729;
    public static final int GL_NEAREST_MIPMAP_LINEAR = 9986;
    public static final int GL_LINEAR_MIPMAP_LINEAR = 9987;
    public static final int GL_CLAMP_TO_EDGE = 33071;
    public static final int GL_REPEAT = 10497;
    public static final int GL_FRONT = 1028;
    public static final int GL_FRONT_AND_BACK = 1032;
    public static final int GL_LINE = 6913;
    public static final int GL_FILL = 6914;
    public static final int GL_BYTE = 5120;
    public static final int GL_UNSIGNED_BYTE = 5121;
    public static final int GL_SHORT = 5122;
    public static final int GL_UNSIGNED_SHORT = 5123;
    public static final int GL_INT = 5124;
    public static final int GL_UNSIGNED_INT = 5125;
    public static final int GL_FLOAT = 5126;
    public static final int GL_ZERO = 0;
    public static final int GL_ONE = 1;
    public static final int GL_SRC_COLOR = 768;
    public static final int GL_ONE_MINUS_SRC_COLOR = 769;
    public static final int GL_SRC_ALPHA = 770;
    public static final int GL_ONE_MINUS_SRC_ALPHA = 771;
    public static final int GL_DST_ALPHA = 772;
    public static final int GL_ONE_MINUS_DST_ALPHA = 773;
    public static final int GL_DST_COLOR = 774;
    public static final int GL_ONE_MINUS_DST_COLOR = 775;
    public static final int GL_REPLACE = 7681;
    public static final int GL_DEPTH_BUFFER_BIT = 256;
    public static final int GL_COLOR_BUFFER_BIT = 16384;
    public static final int GL_RGBA8 = 32856;
    public static final int GL_PROXY_TEXTURE_2D = 32868;
    public static final int GL_RGBA = 6408;
    public static final int GL_TEXTURE_WIDTH = 4096;
    public static final int GL_BGR = 32992;
    public static final int GL_FUNC_ADD = 32774;
    public static final int GL_MIN = 32775;
    public static final int GL_MAX = 32776;
    public static final int GL_FUNC_SUBTRACT = 32778;
    public static final int GL_FUNC_REVERSE_SUBTRACT = 32779;
    public static final int GL_DEPTH_COMPONENT24 = 33190;
    public static final int GL_STATIC_DRAW = 35044;
    public static final int GL_DYNAMIC_DRAW = 35048;
    public static final int GL_STREAM_DRAW = 35040;
    public static final int GL_STATIC_READ = 35045;
    public static final int GL_DYNAMIC_READ = 35049;
    public static final int GL_STREAM_READ = 35041;
    public static final int GL_STATIC_COPY = 35046;
    public static final int GL_DYNAMIC_COPY = 35050;
    public static final int GL_STREAM_COPY = 35042;
    public static final int GL_SYNC_GPU_COMMANDS_COMPLETE = 37143;
    public static final int GL_TIMEOUT_EXPIRED = 37147;
    public static final int GL_WAIT_FAILED = 37149;
    public static final int GL_UNPACK_SWAP_BYTES = 3312;
    public static final int GL_UNPACK_LSB_FIRST = 3313;
    public static final int GL_UNPACK_ROW_LENGTH = 3314;
    public static final int GL_UNPACK_SKIP_ROWS = 3315;
    public static final int GL_UNPACK_SKIP_PIXELS = 3316;
    public static final int GL_UNPACK_ALIGNMENT = 3317;
    public static final int GL_PACK_ALIGNMENT = 3333;
    public static final int GL_PACK_ROW_LENGTH = 3330;
    public static final int GL_MAX_TEXTURE_SIZE = 3379;
    public static final int GL_TEXTURE_2D = 3553;
    public static final int GL_DEPTH_COMPONENT = 6402;
    public static final int GL_DEPTH_COMPONENT32 = 33191;
    public static final int GL_FRAMEBUFFER = 36160;
    public static final int GL_RENDERBUFFER = 36161;
    public static final int GL_COLOR_ATTACHMENT0 = 36064;
    public static final int GL_DEPTH_ATTACHMENT = 36096;
    public static final int GL_FRAMEBUFFER_COMPLETE = 36053;
    public static final int GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT = 36054;
    public static final int GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT = 36055;
    public static final int GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER = 36059;
    public static final int GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER = 36060;
    public static final int GL_FRAMEBUFFER_UNSUPPORTED = 36061;
    public static final int GL_LINK_STATUS = 35714;
    public static final int GL_COMPILE_STATUS = 35713;
    public static final int GL_VERTEX_SHADER = 35633;
    public static final int GL_FRAGMENT_SHADER = 35632;
    public static final int GL_TEXTURE0 = 33984;
    public static final int GL_TEXTURE1 = 33985;
    public static final int GL_TEXTURE2 = 33986;
    public static final int GL_DEPTH_TEXTURE_MODE = 34891;
    public static final int GL_TEXTURE_COMPARE_MODE = 34892;
    public static final int GL_ARRAY_BUFFER = 34962;
    public static final int GL_ELEMENT_ARRAY_BUFFER = 34963;
    public static final int GL_PIXEL_PACK_BUFFER = 35051;
    public static final int GL_COPY_READ_BUFFER = 36662;
    public static final int GL_COPY_WRITE_BUFFER = 36663;
    public static final int GL_PIXEL_UNPACK_BUFFER = 35052;
    public static final int GL_UNIFORM_BUFFER = 35345;
    public static final int GL_ALPHA_BIAS = 3357;
    public static final int GL_RGB = 6407;
    public static final int GL_RG = 33319;
    public static final int GL_R8 = 33321;
    public static final int GL_RED = 6403;
    public static final int GL_OUT_OF_MEMORY = 1285;

    public static int toGl(DepthTestFunction pDepthTestFunction) {
        return switch (pDepthTestFunction) {
            case NO_DEPTH_TEST -> 519;
            case EQUAL_DEPTH_TEST -> 514;
            case LESS_DEPTH_TEST -> 513;
            case GREATER_DEPTH_TEST -> 516;
            default -> 515;
        };
    }

    public static int toGl(PolygonMode pPolygonMode) {
        return switch (pPolygonMode) {
            case WIREFRAME -> 6913;
            default -> 6914;
        };
    }

    public static int toGl(DestFactor pDestFactor) {
        return switch (pDestFactor) {
            case CONSTANT_ALPHA -> 32771;
            case CONSTANT_COLOR -> 32769;
            case DST_ALPHA -> 772;
            case DST_COLOR -> 774;
            case ONE -> 1;
            case ONE_MINUS_CONSTANT_ALPHA -> 32772;
            case ONE_MINUS_CONSTANT_COLOR -> 32770;
            case ONE_MINUS_DST_ALPHA -> 773;
            case ONE_MINUS_DST_COLOR -> 775;
            case ONE_MINUS_SRC_ALPHA -> 771;
            case ONE_MINUS_SRC_COLOR -> 769;
            case SRC_ALPHA -> 770;
            case SRC_COLOR -> 768;
            case ZERO -> 0;
        };
    }

    public static int toGl(SourceFactor pSourceFactor) {
        return switch (pSourceFactor) {
            case CONSTANT_ALPHA -> 32771;
            case CONSTANT_COLOR -> 32769;
            case DST_ALPHA -> 772;
            case DST_COLOR -> 774;
            case ONE -> 1;
            case ONE_MINUS_CONSTANT_ALPHA -> 32772;
            case ONE_MINUS_CONSTANT_COLOR -> 32770;
            case ONE_MINUS_DST_ALPHA -> 773;
            case ONE_MINUS_DST_COLOR -> 775;
            case ONE_MINUS_SRC_ALPHA -> 771;
            case ONE_MINUS_SRC_COLOR -> 769;
            case SRC_ALPHA -> 770;
            case SRC_ALPHA_SATURATE -> 776;
            case SRC_COLOR -> 768;
            case ZERO -> 0;
        };
    }

    public static int toGl(BufferType pBufferType) {
        return switch (pBufferType) {
            case VERTICES -> 34962;
            case INDICES -> 34963;
            case PIXEL_PACK -> 35051;
            case COPY_READ -> 36662;
            case COPY_WRITE -> 36663;
            case PIXEL_UNPACK -> 35052;
            case UNIFORM -> 35345;
        };
    }

    public static int toGl(VertexFormat.Mode pMode) {
        return switch (pMode) {
            case LINES -> 4;
            case LINE_STRIP -> 5;
            case DEBUG_LINES -> 1;
            case DEBUG_LINE_STRIP -> 3;
            case TRIANGLES -> 4;
            case TRIANGLE_STRIP -> 5;
            case TRIANGLE_FAN -> 6;
            case QUADS -> 4;
        };
    }

    public static int toGl(VertexFormat.IndexType pIndexType) {
        return switch (pIndexType) {
            case SHORT -> 5123;
            case INT -> 5125;
        };
    }

    public static int toGl(NativeImage.Format pFormat) {
        return switch (pFormat) {
            case RGBA -> 6408;
            case RGB -> 6407;
            case LUMINANCE_ALPHA -> 33319;
            case LUMINANCE -> 6403;
        };
    }

    public static int toGl(BufferUsage pBufferUsage) {
        return switch (pBufferUsage) {
            case DYNAMIC_WRITE -> 35048;
            case STATIC_WRITE -> 35044;
            case STREAM_WRITE -> 35040;
            case STATIC_READ -> 35045;
            case DYNAMIC_READ -> 35049;
            case STREAM_READ -> 35041;
            case DYNAMIC_COPY -> 35050;
            case STATIC_COPY -> 35046;
            case STREAM_COPY -> 35042;
        };
    }

    public static int toGl(AddressMode pAddressMode) {
        return switch (pAddressMode) {
            case REPEAT -> 10497;
            case CLAMP_TO_EDGE -> 33071;
        };
    }

    public static int toGl(VertexFormatElement.Type pType) {
        return switch (pType) {
            case FLOAT -> 5126;
            case UBYTE -> 5121;
            case BYTE -> 5120;
            case USHORT -> 5123;
            case SHORT -> 5122;
            case UINT -> 5125;
            case INT -> 5124;
        };
    }

    public static int toGlInternalId(TextureFormat pTextureFormat) {
        return switch (pTextureFormat) {
            case RGBA8 -> 32856;
            case RED8 -> 33321;
            case DEPTH32 -> 33191;
        };
    }

    public static int toGlExternalId(TextureFormat pTextureFormat) {
        return switch (pTextureFormat) {
            case RGBA8 -> 6408;
            case RED8 -> 6403;
            case DEPTH32 -> 6402;
        };
    }

    public static int toGlType(TextureFormat pTextureFormat) {
        return switch (pTextureFormat) {
            case RGBA8 -> 5121;
            case RED8 -> 5121;
            case DEPTH32 -> 5126;
        };
    }

    public static int toGl(ShaderType pShaderType) {
        return switch (pShaderType) {
            case VERTEX -> 35633;
            case FRAGMENT -> 35632;
        };
    }
}