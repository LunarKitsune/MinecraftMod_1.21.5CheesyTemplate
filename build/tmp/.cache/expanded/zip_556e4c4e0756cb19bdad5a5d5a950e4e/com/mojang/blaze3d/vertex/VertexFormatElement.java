package com.mojang.blaze3d.vertex;

import com.mojang.blaze3d.DontObfuscate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@DontObfuscate
public record VertexFormatElement(int id, int index, VertexFormatElement.Type type, VertexFormatElement.Usage usage, int count) {
    public static final int MAX_COUNT = 32;
    private static final VertexFormatElement[] BY_ID = new VertexFormatElement[32];
    private static final List<VertexFormatElement> ELEMENTS = new ArrayList<>(32);
    public static final VertexFormatElement POSITION = register(0, 0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.POSITION, 3);
    public static final VertexFormatElement COLOR = register(1, 0, VertexFormatElement.Type.UBYTE, VertexFormatElement.Usage.COLOR, 4);
    public static final VertexFormatElement UV0 = register(2, 0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.UV, 2);
    public static final VertexFormatElement UV = UV0;
    public static final VertexFormatElement UV1 = register(3, 1, VertexFormatElement.Type.SHORT, VertexFormatElement.Usage.UV, 2);
    public static final VertexFormatElement UV2 = register(4, 2, VertexFormatElement.Type.SHORT, VertexFormatElement.Usage.UV, 2);
    public static final VertexFormatElement NORMAL = register(5, 0, VertexFormatElement.Type.BYTE, VertexFormatElement.Usage.NORMAL, 3);

    public VertexFormatElement(int id, int index, VertexFormatElement.Type type, VertexFormatElement.Usage usage, int count) {
        if (id < 0 || id >= BY_ID.length) {
            throw new IllegalArgumentException("Element ID must be in range [0; " + BY_ID.length + ")");
        } else if (!this.supportsUsage(index, usage)) {
            throw new IllegalStateException("Multiple vertex elements of the same type other than UVs are not supported");
        } else {
            this.id = id;
            this.index = index;
            this.type = type;
            this.usage = usage;
            this.count = count;
        }
    }

    public static VertexFormatElement register(
        int pId, int pIndex, VertexFormatElement.Type pType, VertexFormatElement.Usage pUsage, int pCount
    ) {
        VertexFormatElement vertexformatelement = new VertexFormatElement(pId, pIndex, pType, pUsage, pCount);
        if (BY_ID[pId] != null) {
            throw new IllegalArgumentException("Duplicate element registration for: " + pId);
        } else {
            BY_ID[pId] = vertexformatelement;
            ELEMENTS.add(vertexformatelement);
            return vertexformatelement;
        }
    }

    private boolean supportsUsage(int pIndex, VertexFormatElement.Usage pUsage) {
        return pIndex == 0 || pUsage == VertexFormatElement.Usage.UV;
    }

    @Override
    public String toString() {
        return this.count + "," + this.usage + "," + this.type + " (" + this.id + ")";
    }

    public int mask() {
        return 1 << this.id;
    }

    public int byteSize() {
        return this.type.size() * this.count;
    }

    @Nullable
    public static VertexFormatElement byId(int pId) {
        return BY_ID[pId];
    }

    public static Stream<VertexFormatElement> elementsFromMask(int pMask) {
        return ELEMENTS.stream().filter(p_345037_ -> p_345037_ != null && (pMask & p_345037_.mask()) != 0);
    }

    @OnlyIn(Dist.CLIENT)
    @DontObfuscate
    public static enum Type {
        FLOAT(4, "Float"),
        UBYTE(1, "Unsigned Byte"),
        BYTE(1, "Byte"),
        USHORT(2, "Unsigned Short"),
        SHORT(2, "Short"),
        UINT(4, "Unsigned Int"),
        INT(4, "Int");

        private final int size;
        private final String name;

        private Type(final int pSize, final String pName) {
            this.size = pSize;
            this.name = pName;
        }

        public int size() {
            return this.size;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

    @OnlyIn(Dist.CLIENT)
    @DontObfuscate
    public static enum Usage {
        POSITION("Position"),
        NORMAL("Normal"),
        COLOR("Vertex Color"),
        UV("UV"),
        GENERIC("Generic");

        private final String name;

        private Usage(final String pName) {
            this.name = pName;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }
}