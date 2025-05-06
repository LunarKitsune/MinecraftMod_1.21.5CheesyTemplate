package com.mojang.blaze3d.shaders;

import com.mojang.blaze3d.DontObfuscate;
import javax.annotation.Nullable;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
@DontObfuscate
public enum ShaderType {
    VERTEX("vertex", ".vsh"),
    FRAGMENT("fragment", ".fsh");

    private static final ShaderType[] TYPES = values();
    private final String name;
    private final String extension;

    private ShaderType(final String pName, final String pExtension) {
        this.name = pName;
        this.extension = pExtension;
    }

    @Nullable
    public static ShaderType byLocation(ResourceLocation pLocation) {
        for (ShaderType shadertype : TYPES) {
            if (pLocation.getPath().endsWith(shadertype.extension)) {
                return shadertype;
            }
        }

        return null;
    }

    public String getName() {
        return this.name;
    }

    public FileToIdConverter idConverter() {
        return new FileToIdConverter("shaders", this.extension);
    }
}