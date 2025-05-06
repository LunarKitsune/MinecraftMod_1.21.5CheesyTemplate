package com.mojang.blaze3d.shaders;

import net.minecraft.util.StringRepresentable;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public enum UniformType implements StringRepresentable {
    INT(1, "int"),
    IVEC3(3, "ivec3"),
    FLOAT(1, "float"),
    VEC2(2, "vec2"),
    VEC3(3, "vec3"),
    VEC4(4, "vec4"),
    MATRIX4X4(16, "matrix4x4");

    public static final StringRepresentable.EnumCodec<UniformType> CODEC = StringRepresentable.fromEnum(UniformType::values);
    final int count;
    final String name;

    private UniformType(final int pCount, final String pName) {
        this.count = pCount;
        this.name = pName;
    }

    public int count() {
        return this.count;
    }

    public boolean isIntStorage() {
        return this == INT || this == IVEC3;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    public int getCount() {
        return this.count;
    }
}